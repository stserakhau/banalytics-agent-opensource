package com.banalytics.box.module.media.task.motion.storage;

import com.banalytics.box.api.integration.utils.TimeUtil;
import com.banalytics.box.api.integration.model.SubItem;
import com.banalytics.box.api.integration.webrtc.channel.NodeState;
import com.banalytics.box.module.events.AbstractEvent;
import com.banalytics.box.module.events.FileCreatedEvent;
import com.banalytics.box.module.*;
import com.banalytics.box.module.constants.MediaFormat;
import com.banalytics.box.module.events.StatusEvent;
import com.banalytics.box.module.media.task.AbstractMediaGrabberTask;
import com.banalytics.box.module.media.task.Utils;
import com.banalytics.box.module.media.task.motion.detector.MotionDetectionTask;
import com.banalytics.box.module.media.task.sound.SoundDetectionTask;
import com.banalytics.box.module.standard.FileStorage;
import com.banalytics.box.service.SystemThreadsService;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static com.banalytics.box.api.integration.utils.TimeUtil.currentTimeInServerTz;
import static com.banalytics.box.module.ExecutionContext.GlobalVariables.*;
import static com.banalytics.box.module.constants.VideoPreBufferTime.OFF;
import static com.banalytics.box.module.utils.Utils.nodeType;

@Slf4j
@SubItem(of = {AbstractMediaGrabberTask.class}, group = "media-motion-processing")
public final class MotionVideoRecordingTask extends AbstractTask<MotionVideoRecordingConfig> implements MediaCaptureCallbackSupport, FileStorageSupport {
    public MotionVideoRecordingTask(BoxEngine metricDeliveryService, AbstractListOfTask<?> parent) {
        super(metricDeliveryService, parent);
    }

    @Override
    public Map<String, Class<?>> inSpec() {
        return Map.of(
                FrameGrabber.class.getName(), FrameGrabber.class,
                Frame.class.getName(), Frame.class,
                SOURCE_TASK_UUID.name(), UUID.class
        );
    }


    private FileStorage fileStorage;
    private DateTimeFormatter dateTimeFormat;

    private final LinkedList<Frame> preBuffer = new LinkedList<>();

    @Override
    public FileStorage fileStorage() {
        return fileStorage;
    }

    @Override
    public void doInit() throws Exception {
        if (this.fileStorage != null) {
            ((Thing<?>) this.fileStorage).unSubscribe(this);
        }
        Thing<?> fileStorageThing = engine.getThingAndSubscribe(configuration.storageUuid, this);
        this.fileStorage = (FileStorage) fileStorageThing;
    }

    private long noMotionTime;

    @Override
    public void doStart(boolean ignoreAutostartProperty, boolean startChildren) throws Exception {
        clearPreBuffer();

        this.dateTimeFormat = DateTimeFormatter.ofPattern(configuration.pathPattern.format);
        shutdown = false;
        this.noMotionTime = this.configuration.preBufferSeconds.intervalMillis
                + this.configuration.recordingOnMotionDisappearedTimoutMillis;
    }

    private String fileName;
    private String thumbnailFileName;
    private File recordingFile;
    private File thumbnailFile;
    private FFmpegFrameRecorder recorder;
    private long recordingTimeout;
    private long flushTimeout;

    boolean shutdown;

    LocalDateTime motionDetectedTimestamp;
    LocalDateTime lastMotionTimestamp;

    int width;
    int height;

    private Queue<Frame> framesQueue;
    private boolean needCommit;

    @Override
    protected synchronized boolean doProcess(ExecutionContext executionContext) throws Exception {
        if (shutdown) {
            return true;
        }

        UUID dataSourceUuid = executionContext.getVar(SOURCE_TASK_UUID);
        FrameGrabber frameGrabber = executionContext.getVar(FrameGrabber.class);
        Frame frame = executionContext.getVar(Frame.class);
        if (configuration.disableAudioRecording && frame.type == Frame.Type.AUDIO) {
            return true;
        }
        Boolean videoMotionDetected = executionContext.getVar(VIDEO_MOTION_DETECTED);
        Boolean audioMotionDetected = executionContext.getVar(AUDIO_MOTION_DETECTED);
        boolean motionDetected = Boolean.TRUE.equals(videoMotionDetected)
                || Boolean.TRUE.equals(audioMotionDetected);

        long now = System.currentTimeMillis();
        if (motionDetected) {
            this.recordingTimeout = now + this.configuration.recordingOnMotionDisappearedTimoutMillis;
            this.lastMotionTimestamp = currentTimeInServerTz();
//            log.info("Motion detected");
        }

        if (recorder == null && motionDetected) {
            needCommit = false;
            this.motionDetectedTimestamp = this.lastMotionTimestamp = currentTimeInServerTz();
            LocalDateTime currentTime = TimeUtil.currentTimeInServerTz();

            this.fileName = '/' + dataSourceUuid.toString() + '/' + this.dateTimeFormat.format(currentTime) + "." + MediaFormat.mp4.name();

            this.recordingFile = this.fileStorage.startOutputTransaction(this.fileName);

            width = frame.imageWidth;
            height = frame.imageHeight;

            log.info("Recording started:\n\tfile: {}", this.fileName);

            this.framesQueue = new LinkedBlockingQueue<>();
            this.recorder = createRecorder(frameGrabber, recordingFile, executionContext);
            this.recorder.startUnsafe();
            this.flushTimeout = now + this.configuration.splitTimeout.intervalMillis;

            SystemThreadsService.execute(this, () -> {
//                System.out.println("Queue: started ===============================");
                boolean thumbnailCreated = false;
                Queue<Frame> frameQueue = framesQueue;
                a:
                while (frameQueue == framesQueue) {
                    while (frameQueue.isEmpty()) {
                        if (needCommit || state != State.RUN) {
                            log.info("Need commit");
                            break a;
                        }
                        try {
//                            System.out.println("Queue: wait frame");
                            int delay = (int)((1 / currentFrameRate) / 3 * 1000);
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            onProcessingException(e);
                        }
                    }
                    while (!frameQueue.isEmpty()) {
//                        System.out.println("Queue: poll frame & write");
                        try (Frame f = frameQueue.poll()) {
                            try {
                                if (!thumbnailCreated && f.getTypes().contains(Frame.Type.VIDEO)) {
                                    thumbnailCreated = true;
                                    int fNameIndex = this.fileName.lastIndexOf('/');
                                    String fPath = this.fileName.substring(0, fNameIndex);
                                    String fName = this.fileName.substring(fNameIndex);
                                    this.thumbnailFileName = fPath + "/thumbnails" + fName + ".jpg";
                                    this.thumbnailFile = this.fileStorage.startOutputTransaction(this.thumbnailFileName);
                                    Utils.saveFrameToFile(f, this.thumbnailFile, 0.8f, 300);
                                    log.info("\tthumb: {}", this.thumbnailFileName);
                                }
                            } catch (Exception e) {
                                log.error("Can't create thumbnail: {}", e.getMessage());
                            }
//                            log.info("Recording in progress");
                            recorder.record(f);
                        } catch (FFmpegFrameRecorder.Exception e) {
                            onProcessingException(e);
                            Duration motionTime = Duration.between(this.motionDetectedTimestamp, this.lastMotionTimestamp);
                            try {
                                commit(this.motionDetectedTimestamp, currentTimeInServerTz(), motionTime);
                            } catch (Exception ex) {
                                log.error(ex.getMessage(), ex);
                            }
                        }
                        if (this.shutdown) {
                            break a;
                        }
                    }
                }

                try {
                    Duration motionTime = Duration.between(this.motionDetectedTimestamp, this.lastMotionTimestamp);
                    System.out.println("Queue: Commit");
                    commit(this.motionDetectedTimestamp, currentTimeInServerTz(), motionTime);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }

        boolean isWriting = recorder != null;
        if (isWriting) {
            if (!this.preBuffer.isEmpty()) {
                try {//flush buffer
                    for (Frame preBufferedFrame : this.preBuffer) {
                        long fts = preBufferedFrame.timestamp;
                        long rts = recorder.getTimestamp();
                        if (fts < rts) {
                            preBufferedFrame.timestamp = rts + 10;
                        }
                        System.out.println("Queue: add pre-buffered");
                        framesQueue.add(preBufferedFrame);
                    }
                    preBuffer.clear();
                } finally {// and clear
                    clearPreBuffer();
                }
            }

            long fts = frame.timestamp;
            long rts = recorder.getTimestamp();
            if (fts < rts) {
                frame.timestamp = rts + 10;
            }

            if (framesQueue.size() < 20) {
                framesQueue.add(frame.clone());
            } else {
                engine.fireEvent(new StatusEvent(
                        nodeType(this.getClass()),
                        this.getUuid(),
                        getSelfClassName(),
                        getTitle(),
                        NodeState.valueOf(getState().name()),
                        "Low hardware performance. Frame skipped."
                ));
            }



            boolean videoKeyFrame = executionContext.getVar(VIDEO_KEY_FRAME) == null || (Boolean) executionContext.getVar(VIDEO_KEY_FRAME);
            boolean timeoutTriggered = now > this.flushTimeout || now >= this.recordingTimeout;

//            log.info("Queue frame added. MD: {}, VKF: {}, TT: {}", motionDetected, videoKeyFrame, timeoutTriggered);

            if (recorder != null && timeoutTriggered && videoKeyFrame) {
                needCommit = true;
            }
        } else {//if not writing then pre-buffer
            if (this.configuration.preBufferSeconds != OFF) {
                preBufferFrame(frame.clone());
            }
        }

        return true;
    }

    @Override
    public void onException(Throwable e) {
        super.onException(e);
        this.stop();
    }

    @Override
    public void doStop() throws Exception {
        this.shutdown = true;
        Thread.sleep(500);// wait flushing frame from main thread
        clearPreBuffer();
    }

    private void clearPreBuffer() {
        preBuffer.forEach(Frame::close);
        preBuffer.clear();
    }

    private void commit(LocalDateTime tsStart, LocalDateTime tsEnd) throws Exception {
        commit(tsStart, tsEnd, Duration.between(tsStart, tsEnd));
    }

    private void commit(LocalDateTime tsStart, LocalDateTime tsEnd, Duration motionTime) throws Exception {
        if (this.recorder == null) {
            return;
        }
        long duration = Duration.between(tsStart, tsEnd).toMillis();
        try {
            log.info("Flushing record");
            this.recorder.stop();
        } finally {
            this.recorder = null;
            this.motionDetectedTimestamp = null;
            String fileName = this.fileName;
            String thumbnailFileName = this.thumbnailFileName;
            long recordingSize = this.recordingFile.length();
            long minRecordingSize = configuration.minRecordingSizeKb * 1024;
            SystemThreadsService.execute(this, () -> {
                try {
                    log.info("Motion time(millis)/size(bytes): {}/{}  (min Time filer = {}; min size = {})", motionTime, recordingSize, configuration.minMotionTimeFilterMillis, minRecordingSize);
                    Thread.sleep(100);// wait to stop recorder
                    if (motionTime.toMillis() < configuration.minMotionTimeFilterMillis || recordingSize < minRecordingSize) {
                        log.info("Recording skipped");
                        this.fileStorage.rollbackOutputTransaction(fileName, (contextPath) -> {
                            log.info("Temporary recording file removed");
                        });
                        this.fileStorage.rollbackOutputTransaction(this.thumbnailFileName, (contextPath) -> {
                            log.info("Temporary recording file thumbnail removed");
                        });
                    } else {
                        this.fileStorage.commitOutputTransaction(fileName, (dataPair) -> {
                            try {
                                if (thumbnailFile != null) {
                                    this.fileStorage.commitOutputTransaction(thumbnailFileName, (dpTh) -> {
                                        FileCreatedEvent evt = new FileCreatedEvent(
                                                nodeType(this.getClass()),
                                                this.getUuid(),
                                                getSelfClassName(),
                                                getTitle(),
                                                configuration.storageUuid,
                                                dataPair.getKey()
                                        );
                                        evt.option("width", width);
                                        evt.option("height", height);
                                        evt.option("duration", (int) duration);
                                        evt.option("tsStart", tsStart);
                                        evt.option("tsEnd", tsEnd);
                                        engine.fireEvent(evt);
                                        log.info("Recording committed: {}", new Date());
                                    });
                                }
                            } catch (Throwable e) {
                                log.error("Thumbnail commit failed.", e);
                                onProcessingException(e);
                            }
                        });
                    }
                } catch (Throwable e) {
                    log.error("Recording commit failed.", e);
                    onProcessingException(e);
                }
            });
            needCommit = false;
        }
    }

    private double currentFrameRate;

    private FFmpegFrameRecorder createRecorder(FrameGrabber grabber, File outputFile, ExecutionContext executionContext) throws Exception {
        final FFmpegFrameRecorder recorder;

        double frameRate = grabber.getFrameRate();
        int videoBitrate = grabber.getVideoBitrate();
        int audioChannels = grabber.getAudioChannels();
        boolean isVideoExists = frameRate > 0 || videoBitrate > 0;
        boolean isAudioExists = audioChannels > 0;

        if (isVideoExists) {
            double useFrameRate;
            switch (configuration.useFrameRate) {
                case GRABBER_FRAME_RATE -> useFrameRate = grabber.getFrameRate();
                case CALCULATED_FRAME_RATE -> useFrameRate = executionContext.getVar(CALCULATED_FRAME_RATE);
                default -> useFrameRate = executionContext.getVar(CALCULATED_FRAME_RATE);
            }

            frameRate = useFrameRate;
            currentFrameRate = frameRate;
        }

        if (isVideoExists && isAudioExists) {
            recorder = new FFmpegFrameRecorder(
                    outputFile,
                    grabber.getImageWidth(), grabber.getImageHeight(),
                    grabber.getAudioChannels()
            );
        } else if (isVideoExists) {
            recorder = new FFmpegFrameRecorder(
                    outputFile, grabber.getImageWidth(), grabber.getImageHeight()
            );
        } else if (isAudioExists) {
            recorder = new FFmpegFrameRecorder(
                    outputFile, audioChannels
            );
        } else {
            throw new Exception("Grabber doesn't provides video or audio data");
        }

        recorder.setFormat(MediaFormat.mp4.name());
        recorder.setOption("threads", "4");
        int bitRate = configuration.videoBitRate.bitrate;
        if (isVideoExists) {
            if (bitRate == -1) {
                recorder.setVideoBitrate(videoBitrate);
            } else {
//            recorder.setInterleaved(true);
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setVideoBitrate(configuration.videoBitRate.bitrate);
                recorder.setAspectRatio(grabber.getAspectRatio());


                recorder.setFrameRate(frameRate);
                recorder.setGopSize(configuration.getGop());
            }
        }
        if (isAudioExists) {
            if (bitRate == -1) {
                recorder.setSampleRate(grabber.getSampleRate());
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            } else {
                recorder.setAudioOption("crf", "0");
                recorder.setAudioQuality(0);
                recorder.setSampleRate(grabber.getSampleRate());
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            }
        }

        return recorder;
    }

    //            recorder.setOption("hwaccel", "videotoolbox");
//            recorder.setVideoCodecName("h264_videotoolbox");
//            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

    private void preBufferFrame(Frame frame) throws Exception {
        this.preBuffer.addLast(frame);

        Frame first = this.preBuffer.getFirst();
        Frame last = this.preBuffer.getLast();
        while (last.timestamp - first.timestamp > configuration.preBufferSeconds.intervalMillis) {
            Frame removed = this.preBuffer.removeFirst();
            removed.close();
            first = this.preBuffer.getFirst();
        }
    }

    @Override
    public void destroy() {
        if (this.fileStorage != null) {
            ((Thing<?>) this.fileStorage).unSubscribe(this);
        }
    }

    @Override
    public Set<Class<? extends AbstractEvent>> produceEvents() {
        Set<Class<? extends AbstractEvent>> events = new HashSet<>(super.produceEvents());
        events.add(FileCreatedEvent.class);
        return events;
    }

    public Set<Class<? extends AbstractTask<?>>> shouldAddAfter() {
        return Set.of(MotionDetectionTask.class, SoundDetectionTask.class);
    }
}