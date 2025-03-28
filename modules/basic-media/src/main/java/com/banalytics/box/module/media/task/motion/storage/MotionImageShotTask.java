package com.banalytics.box.module.media.task.motion.storage;

import com.banalytics.box.api.integration.model.SubItem;
import com.banalytics.box.api.integration.utils.TimeUtil;
import com.banalytics.box.module.*;
import com.banalytics.box.module.constants.MediaFormat;
import com.banalytics.box.module.events.AbstractEvent;
import com.banalytics.box.module.events.FileCreatedEvent;
import com.banalytics.box.module.media.task.AbstractMediaGrabberTask;
import com.banalytics.box.module.media.task.Utils;
import com.banalytics.box.module.media.task.motion.detector.MotionDetectionTask;
import com.banalytics.box.module.media.task.sound.SoundDetectionTask;
import com.banalytics.box.module.standard.FileStorage;
import com.banalytics.box.service.SystemThreadsService;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.banalytics.box.api.integration.utils.TimeUtil.currentTimeInServerTz;
import static com.banalytics.box.module.ExecutionContext.GlobalVariables.*;
import static com.banalytics.box.module.utils.Utils.nodeType;

@Slf4j
@SubItem(of = {AbstractMediaGrabberTask.class}, group = "media-motion-processing")
public final class MotionImageShotTask extends AbstractTask<MotionImageShotTaskConfig> implements FileStorageSupport {
    public MotionImageShotTask(BoxEngine metricDeliveryService, AbstractListOfTask<?> parent) {
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
        this.dateTimeFormat = DateTimeFormatter.ofPattern(configuration.pathPattern.format);
    }

    LocalDateTime motionDetectedTimestamp;

    private int photoCounter = 0;
    private long nextPhotoTimeout;

    @Override
    protected synchronized boolean doProcess(ExecutionContext executionContext) throws Exception {
        UUID dataSourceUuid = executionContext.getVar(SOURCE_TASK_UUID);
        Frame frame = executionContext.getVar(Frame.class);
        if (!frame.getTypes().contains(Frame.Type.VIDEO)) {
            return true;
        }
        Boolean videoMotionDetected = executionContext.getVar(VIDEO_MOTION_DETECTED);
        Boolean audioMotionDetected = executionContext.getVar(AUDIO_MOTION_DETECTED);
        boolean motionDetected = Boolean.TRUE.equals(videoMotionDetected) || Boolean.TRUE.equals(audioMotionDetected);

        long now = System.currentTimeMillis();
        if (motionDetected) {
            this.motionDetectedTimestamp = currentTimeInServerTz();
            this.photoCounter = configuration.photosInSeries;
        }

        boolean createImageShot = photoCounter > 0 && now >= nextPhotoTimeout;

        if (createImageShot) {
            photoCounter--;
            nextPhotoTimeout = now + configuration.photoIntervalMillis;
            final Frame imageShot = frame.clone();
            SystemThreadsService.execute(this, () -> {
                try (imageShot) {
                    LocalDateTime currentTime = TimeUtil.currentTimeInServerTz();
                    String fileName = '/' + dataSourceUuid.toString() + '/' + this.dateTimeFormat.format(currentTime) + ".jpg";
                    File file = this.fileStorage.startOutputTransaction(fileName);
                    Utils.saveFrameToFile(imageShot, file, configuration.compressionRate, -1);
                    this.fileStorage.commitOutputTransaction(fileName, (dataPair) -> {
                        FileCreatedEvent evt = new FileCreatedEvent(
                                nodeType(this.getClass()),
                                this.getUuid(),
                                getSelfClassName(),
                                getTitle(),
                                configuration.storageUuid,
                                dataPair.getKey()
                        );
                        evt.option("width", imageShot.imageWidth);
                        evt.option("height", imageShot.imageHeight);
                        evt.option("duration", 10);
                        evt.option("tsStart", this.motionDetectedTimestamp);
                        evt.option("tsEnd", this.motionDetectedTimestamp.plus(10, ChronoUnit.MILLIS));
                        engine.fireEvent(evt);
                        log.info("Recording committed: {}", new Date());
                    });

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
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