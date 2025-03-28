package com.banalytics.box.module.media.task.motion.detector;

import com.banalytics.box.api.integration.model.SubItem;
import com.banalytics.box.module.events.AbstractEvent;
import com.banalytics.box.module.events.MotionEvent;
import com.banalytics.box.module.*;
import com.banalytics.box.module.media.ImageClassifier;
import com.banalytics.box.module.media.ImageClassifier.ClassificationResult;
import com.banalytics.box.module.media.task.AbstractMediaGrabberTask;
import com.banalytics.box.module.media.task.AbstractStreamingMediaTask;
import com.banalytics.box.module.media.task.motion.storage.MotionVideoRecordingTask;
import com.banalytics.box.module.media.utils.ZonePainter;
import com.banalytics.box.service.SystemThreadsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;

import java.util.Arrays;
import java.util.*;
import java.util.stream.Collectors;

import static com.banalytics.box.module.ExecutionContext.GlobalVariables.*;
import static com.banalytics.box.module.media.task.motion.detector.MatrixSizeType.zero;
import static com.banalytics.box.module.media.task.motion.detector.MotionDetectionConfig.MotionTriggerMode.MOTION_AND_CLASSIFIER;
import static com.banalytics.box.module.media.task.motion.detector.MotionDetectionConfig.MotionTriggerMode.MOTION_ONLY_ADD_CLASSES;
import static com.banalytics.box.module.utils.Utils.nodeType;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.opencv.core.Core.BORDER_DEFAULT;

/**
 * https://learnopencv.com/background-subtraction-with-opencv-and-bgs-libraries/
 * https://docs.opencv.org/3.4/d1/dc5/tutorial_background_subtraction.html
 */
@Slf4j
@SubItem(of = {AbstractMediaGrabberTask.class}, group = "media-motion-processing")
public class MotionDetectionTask extends AbstractStreamingMediaTask<MotionDetectionConfig> implements PropertyValuesProvider {
    public MotionDetectionTask(BoxEngine metricDeliveryService, AbstractListOfTask<?> parent) {
        super(metricDeliveryService, parent);
    }

    @Override
    public Map<String, Class<?>> inSpec() {
        return Map.of(Frame.class.getName(), Frame.class);
    }

    OpenCVFrameConverter.ToMat converter;

    Size blurSize;

    UMat dilateKernel;

    private final ZonePainter zonePainter = new ZonePainter();

    private ImageClassifier<UMat> imageClassifier;

    @Override
    public void doInit() throws Exception {
        if (configuration.motionTriggerMode == MOTION_AND_CLASSIFIER && configuration.imageClassifierThingUuid != null) {
            imageClassifier = engine.getThingAndSubscribe(configuration.imageClassifierThingUuid, this);
        }
        super.doInit();
    }

    boolean insideClassificator;
    long classificationExpirationTimeout;

    long turnOnDelayTimeout;

    @Override
    public void doStart(boolean ignoreAutostartProperty, boolean startChildren) throws Exception {
        log.info("Initialization started: {}", configuration);
//        if (configuration.autoCalibration) {
//            configuration.backgroundHistoryDistThreshold = 0;
//            configuration.blurSize = MatrixSizeType.s7x7;
//            configuration.backgroundHistorySize = 3;
//        }
        if (configuration.blurSize != zero) {
            this.blurSize = new Size(configuration.blurSize.width, configuration.blurSize.height);
        } else {
            this.blurSize = null;
        }

//        this.backgroundSubtractor = opencv_video.createBackgroundSubtractorMOG2(configuration.backgroundHistorySize, configuration.backgroundHistoryDistThreshold, false);
        this.converter = new OpenCVFrameConverter.ToMat();
        if (configuration.dilateSize != zero) {
            this.dilateKernel = getStructuringElement(MorphType.MORPH_RECT.index, new Size(configuration.dilateSize.width, configuration.dilateSize.height)).getUMat(ACCESS_READ).clone();//todo memory leak
        } else {
            this.dilateKernel = null;
        }

        this.motionStunTimeout = 0;
        this.frameCounter = 0;
        this.insensitiveMask = null;
        this.insensitiveMaskRGB = null;

        this.currentGrayFrame = new UMat();
        this.blurredGrayFrame = new UMat();
        this.dilatedFrame = new UMat();

        this.contours = new MatVector();
        this.hierarchy = new UMat();
        this.fgMask = new UMat();

        insideClassificator = false;
        relativeOverlapArea = 0;
        triggeredRegions.clear();
        classificationResults.clear();

        reloadConfig();

        log.info("Initialization finished");
        super.doStart(ignoreAutostartProperty, startChildren);
        this.turnOnDelayTimeout = System.currentTimeMillis() + configuration.turnOnDelaySec * 1000L;
    }

    public synchronized void reloadConfig() {
        this.zonePainter.clear();
        if (StringUtils.isNotEmpty(configuration.detectionAreas)) {
            zonePainter.init(configuration.detectionAreas);
        }

        targetClasses.clear();
        if (StringUtils.isNotEmpty(configuration.targetClasses)) {
            String[] classes = configuration.targetClasses.replaceAll("[\\[\\]\"]", "").split(",");
            if (classes.length != 1 || !StringUtils.isEmpty(classes[0])) {
                this.targetClasses.addAll(Arrays.asList(classes));
            }
        }
    }


    //    BackgroundSubtractor backgroundSubtractor;
    UMat fgMask;

    UMat currentGrayFrame;
    UMat blurredGrayFrame;
    UMat dilatedFrame;

    MatVector contours;
    UMat hierarchy;

    private final static Point DILATE_POINT = new Point(-1, -1);
    private final static Scalar DILATE_BORDER_ONE = new Scalar(1);

//    private final List<UMat> detectedObjects = new ArrayList<>(50);
    private double relativeOverlapArea = 0;
    private final Set<String> triggeredRegions = new HashSet<>(5);

    private final Set<String> targetClasses = new HashSet<>();
    private final List<ClassificationResult> classificationResults = new ArrayList<>(20);

    private FrameGrabber grabber;

    @Override
    protected FrameGrabber getGrabber() {
        return grabber;
    }

    @Override
    protected int getAudioChannels() {
        return grabber.getAudioChannels();
    }

    int frameCounter = 0;

    private UMat insensitiveMask;
    private UMat insensitiveMaskRGB;

    public static final Set<String> DEFAULT_ALL_ZONES = Set.of("*");
    private Set<String> lastTriggeredZones = Set.of();
    private long motionStunTimeout = 0;


    @Override
    public void doStop() throws Exception {
        close(currentGrayFrame);
        close(blurredGrayFrame);
        close(dilatedFrame);

        close(contours);
        close(hierarchy);
        close(fgMask);

        close(blurSize);
        close(dilateKernel);
//        close(backgroundSubtractor);
        close(insensitiveMask);
        close(insensitiveMaskRGB);

        super.doStop();
    }

    @Override
    public void destroy() {
        if (imageClassifier != null) {
            ((Thing<?>) imageClassifier).unSubscribe(this);
        }
        super.destroy();
    }

    private void close(Pointer mat) {
        if (mat != null) {
            mat.close();
        }
    }

    UMat targetGrayFrameUnManagedRef;

    private final UMat previousFrame = new UMat();


    private record MotionRect(double size, Rect rect, String title) {
        public void close() {
            rect.close();
        }
    }

    private long motionDetectionTimeout = 0;
    private long autoBreakMotionTimeout = 0;
    private final List<MotionRect> motionRects = new ArrayList<>();

    private final Point pos1 = new Point(0, 0);
    private final Point pos2 = new Point(0, 0);

    /**
     * TODO https://github.com/bytedeco/javacpp-presets/issues/644   UMAT CRASH ON JVM GC !!!
     */
    @Override
    protected boolean doProcess(ExecutionContext executionContext) throws Exception {
        boolean targetMotionDetected = false;
        this.grabber = executionContext.getVar(FrameGrabber.class);
        Frame frame = executionContext.getVar(Frame.class);
        try {
            long now = System.currentTimeMillis();

            if (now > autoBreakMotionTimeout) {
                for (MotionRect motionRect : motionRects) {
                    motionRect.close();
                }
                relativeOverlapArea = 0;
                motionRects.clear();
            }

            if (frame != null && frame.image != null) {
                frameCounter++;
//                detectedObjects.clear();
                triggeredRegions.clear();
//                boolean backgroundReady = frameCounter > configuration.backgroundHistorySize;
                boolean backgroundReady = frameCounter > 2; // >2 - need to initialize previousFrame and create fgMask
                Mat streamColorFrame = converter.convert(frame);

                try (UMat colorFrame = streamColorFrame.getUMat(ACCESS_READ)) {
                    boolean doMotionDetection = now > motionDetectionTimeout;

                    if (!backgroundReady || doMotionDetection) {
                        motionDetectionTimeout = now + configuration.motionDetectionTimeoutMillis;
                        cvtColor(colorFrame, currentGrayFrame, COLOR_BGR2GRAY);

                        if (configuration.blurSize != zero) {
                            GaussianBlur(currentGrayFrame, blurredGrayFrame, blurSize, 0);
                            targetGrayFrameUnManagedRef = blurredGrayFrame;
                        } else {
                            targetGrayFrameUnManagedRef = currentGrayFrame;
                        }
                        if (this.insensitiveMask == null) {//build exclusion mask
                            if (!zonePainter.insensitiveAreas.isEmpty()) {
                                this.insensitiveMask = zonePainter.insensitiveMask(targetGrayFrameUnManagedRef);
                                this.insensitiveMaskRGB = zonePainter.insensitiveMask(colorFrame);
                            }
                        }
                        if (this.insensitiveMask != null) {
                            bitwise_and(targetGrayFrameUnManagedRef, insensitiveMask, targetGrayFrameUnManagedRef);
                        }
//                        backgroundSubtractor.apply(targetGrayFrameUnManagedRef, fgMask);
                        if (!backgroundReady) {//if background not ready accumulate data and skip motion detection
                            targetGrayFrameUnManagedRef.copyTo(previousFrame);
                            return true;
                        }
                        absdiff(previousFrame, targetGrayFrameUnManagedRef, fgMask);

                        targetGrayFrameUnManagedRef.copyTo(previousFrame);
//                      todo use instead default  adaptiveThreshold(fgMask, fgMask, configuration.backgroundHistoryDistThreshold, 255, THRESH_BINARY);
                        threshold(fgMask, fgMask, configuration.backgroundHistoryDistThreshold, 255, THRESH_BINARY);

                        UMat matToContour;
                        if (dilateKernel != null) {
                            dilate(fgMask, dilatedFrame, dilateKernel, DILATE_POINT, 2, BORDER_DEFAULT, DILATE_BORDER_ONE);
                            matToContour = dilatedFrame;
                        } else {
                            matToContour = fgMask;
                        }
//                      todo instread of threshold  Canny(matToContour, matToContour, configuration.backgroundHistoryDistThreshold, 255);
                        {
                            for (int i = 0; i < contours.size(); i++) {// extract detected motion areas
                                contours.get(i).close();
                            }
                        }
                        contours.clear();
                        {
                            for (MotionRect motionRect : motionRects) {
                                motionRect.rect.close();
                            }
                            motionRects.clear();
                            autoBreakMotionTimeout = now + 15000;
                        }
                        findContours(matToContour, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

                        if (contours.size() > 0) {
//                            log.info("Found {} contours", contours.size());
                            double totalAreaSize = 0;
                            for (int i = 0; i < contours.size(); i++) {// extract detected motion areas
                                Mat contour = contours.get(i);
                                double contourAreaSize = contourArea(contour);
                                totalAreaSize += contourAreaSize;
                                boolean triggeredAreaSize = isTriggeredAreaSize(contourAreaSize);
                                if (triggeredAreaSize) {
                                    Rect rect = boundingRect(contour);//closing rect resource - 4 lines above
                                    zonePainter.checkObjectInZones(rect.x(), rect.y(), rect.width(), rect.height(), triggeredRegions);
                                    if (!zonePainter.hasDetectionAreas() || !triggeredRegions.isEmpty()) {
                                        MotionRect motionRect = new MotionRect(contourAreaSize, rect, Double.toString(contourAreaSize));
                                        motionRects.add(motionRect);

//                                if (extractDetectedObjects) {
//                                    UMat detectedObject = colorFrame.apply(rect).clone();//todo memory leak
//                                    detectedObjects.add(detectedObject);
//                                }
                                    }
                                }
                            }
                            relativeOverlapArea = totalAreaSize / (frame.imageWidth * frame.imageHeight);
//                            log.info("Found triggered areas: {}", motionRects.size());
                        }
                    }
                    if (!motionRects.isEmpty()) {
                        targetMotionDetected = true;

                        if (configuration.drawDetections) {
                            for (MotionRect motionRect : motionRects) {
                                rectangle(streamColorFrame, motionRect.rect, Scalar.RED, 1, LINE_4, 0);
                                putText(streamColorFrame, motionRect.title, motionRect.rect.tl(),
                                        FONT_HERSHEY_SIMPLEX, configuration.fontScale,
                                        Scalar.RED, 4, 0, false);
                            }
                        }

                        if (configuration.drawNoises) {
                            drawContours(streamColorFrame, contours, -1, Scalar.YELLOW);
                        }
                    }
                    if (now > classificationExpirationTimeout) {
                        if (targetMotionDetected && imageClassifier != null && !insideClassificator && (
                                configuration.motionTriggerMode == MOTION_AND_CLASSIFIER || configuration.motionTriggerMode == MOTION_ONLY_ADD_CLASSES
                        )) {
                            insideClassificator = true;
                            final UMat clonedColorMat = colorFrame.clone();
                            try {
                                SystemThreadsService.execute(this, () -> {
                                    List<ClassificationResult> results;
                                    try {
                                        if (this.insensitiveMaskRGB != null) {
                                            bitwise_and(clonedColorMat, insensitiveMaskRGB, clonedColorMat);
                                        }
                                        results = imageClassifier.predict(this.getUuid(), Collections.singletonList(clonedColorMat), (float) configuration.confidenceThreshold, (float) configuration.nmsThreshold);
                                        classificationExpirationTimeout = System.currentTimeMillis() + configuration.classificationDelay;
                                    } catch (Throwable e) {
                                        log.error(e.getMessage(), e);
                                        sendTaskState(e.getMessage());
                                        return;
                                    } finally {
                                        clonedColorMat.close();
                                        insideClassificator = false;
                                    }

                                    synchronized (this.classificationResults) {
                                        this.classificationResults.clear();
                                        triggeredRegions.clear();
                                        for (ClassificationResult dr : results) {
                                            boolean targetClassDetected = targetClasses.isEmpty() || targetClasses.contains(dr.className());
                                            boolean inTargetZone = zonePainter.checkObjectInZones(dr.x(), dr.y(), dr.width(), dr.height(), triggeredRegions);
                                            if (targetClassDetected && inTargetZone) {
                                                this.classificationResults.add(dr);
                                            }
                                        }
                                    }
                                });
                            } catch (Throwable e) {
                                clonedColorMat.close();
                            }
                        } else {
                            if (now > classificationExpirationTimeout + 1000) {
                                synchronized (this.classificationResults) {
                                    this.classificationResults.clear();
                                }
                            }
                        }
                    }
                    if (configuration.drawClasses) {
                        synchronized (this.classificationResults) {
                            for (ClassificationResult cr : this.classificationResults) {
                                pos1.x(cr.x());
                                pos1.y(cr.y());
                                pos2.x(cr.x() + cr.width());
                                pos2.y(cr.y() + cr.height());

                                rectangle(streamColorFrame, pos1, pos2, Scalar.BLUE, 2, LINE_8, 0);
                                putText(streamColorFrame, cr.className() + ": " + (int) (cr.confidence() * 100) + "%", pos1, FONT_HERSHEY_SIMPLEX, configuration.fontScale, Scalar.MAGENTA, 2, 0, false);
                            }
                        }
                    }

                }
                boolean videoKeyFrame = executionContext.getVar(VIDEO_KEY_FRAME, false);
                double frameRate = executionContext.getVar(CALCULATED_FRAME_RATE);
                frameRate = frameRate == 0 ? 10 : frameRate;
                switch (configuration.debug) {
                    case OFF -> onFrameReceived(frame, videoKeyFrame, frameRate);
                    case TARGET_FRAME -> {
                        try (UMat debugImg = targetGrayFrameUnManagedRef.clone(); Mat debugMat = debugImg.getMat(ACCESS_READ); Frame debugFrame = converter.convert(debugMat)) {
                            onFrameReceived(debugFrame, videoKeyFrame, frameRate);
                        }
                    }
                    case BG_SUBSTRACTOR -> {
                        try (UMat debugImg = fgMask.clone();
                             Mat debugMat = debugImg.getMat(ACCESS_READ)) {
                            if (!debugMat.empty()) {
                                try (Frame debugFrame = converter.convert(debugMat)) {
                                    onFrameReceived(debugFrame, videoKeyFrame, frameRate);
                                }
                            }
                        }
                    }
                }

                boolean classDetected = !this.classificationResults.isEmpty();
                final boolean resultDetection = switch (configuration.motionTriggerMode) {
                    case MOTION_ONLY -> targetMotionDetected;
                    case MOTION_AND_CLASSIFIER -> targetMotionDetected && classDetected;
                    case MOTION_ONLY_ADD_CLASSES -> targetMotionDetected;
                };

                executionContext.setVar(VIDEO_MOTION_DETECTED, resultDetection);
/*                executionContext.setVar(
                        LIST_OF_MAT,
                        detectedObjects
                );*/
                if (resultDetection) {// if motion detected
                    MotionEvent evt = null;
                    Set<String> triggeredZones = null;
                    Set<String> classes;
                    synchronized (this.classificationResults) {
                        classes = classDetected ? this.classificationResults.stream().map(ClassificationResult::className).collect(Collectors.toSet()) : Set.of();
                    }
                    String[] clss = classes.isEmpty() ? new String[]{"*"} : classes.toArray(new String[0]);
                    if (zonePainter.hasDetectionAreas()) {
                        if (!triggeredRegions.isEmpty()) { // and is in triggered areas, then produce event
                            triggeredZones = triggeredRegions;
                            evt = new MotionEvent(nodeType(this.getClass()), this.getUuid(), getSelfClassName(), getTitle(), getSourceThingUuid(), triggeredZones.toArray(new String[0]), clss, relativeOverlapArea);
                        }
                    } else { // and triggered areas not configured produce general event
                        triggeredZones = DEFAULT_ALL_ZONES;
                        evt = new MotionEvent(nodeType(this.getClass()), this.getUuid(), getSelfClassName(), getTitle(), getSourceThingUuid(), triggeredZones.toArray(new String[0]), clss, relativeOverlapArea);
                    }
                    if (evt != null) {
                        if ((now > motionStunTimeout && now > turnOnDelayTimeout) || lastTriggeredZones.size() != triggeredZones.size() || !lastTriggeredZones.containsAll(triggeredZones)) {
                            lastTriggeredZones = new HashSet<>(triggeredZones);
                            engine.fireEvent(evt);
                            motionStunTimeout = now + configuration.eventStunTimeMillis;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            onException(e);
        }
        return true;// continue processing without body execution
    }

    @Override
    public void onFrameReceived(Frame frame, boolean videoKeyFrame, double frameRate, Object... args) throws Exception {
        super.onFrameReceived(frame, videoKeyFrame, frameRate, args);
        mediaStreamToClient(frame, frameRate);
    }

    private boolean isTriggeredAreaSize(double areaSize) {
        return areaSize > configuration.triggeredAreaSize;
    }

    @Override
    public Set<String> provideValues(String propertyName) {
        reloadConfig();
        switch (propertyName) {
            case "classes" -> {
                return targetClasses;
            }
            case "triggerAreas" -> {
                return zonePainter.areasNames(ZonePainter.AreaType.trigger);
            }
            default -> {
                return null;
            }
        }
    }

    @Override
    public Set<Class<? extends AbstractEvent>> produceEvents() {
        Set<Class<? extends AbstractEvent>> events = new HashSet<>(super.produceEvents());
        events.add(MotionEvent.class);
        return events;
    }

    @Override
    public Set<Class<? extends AbstractTask<?>>> shouldAddBefore() {
        return Set.of(MotionVideoRecordingTask.class);
    }
}