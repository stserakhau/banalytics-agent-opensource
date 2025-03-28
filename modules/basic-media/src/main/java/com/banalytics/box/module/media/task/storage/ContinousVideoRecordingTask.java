package com.banalytics.box.module.media.task.storage;

import com.banalytics.box.api.integration.model.SubItem;
import com.banalytics.box.api.integration.webrtc.channel.NodeState;
import com.banalytics.box.module.events.AbstractEvent;
import com.banalytics.box.module.events.FileCreatedEvent;
import com.banalytics.box.module.*;
import com.banalytics.box.module.constants.MediaFormat;
import com.banalytics.box.module.constants.SplitTimeInterval;
import com.banalytics.box.module.events.StatusEvent;
import com.banalytics.box.module.media.task.AbstractMediaGrabberTask;
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
import static com.banalytics.box.module.utils.Utils.nodeType;

@Slf4j
@SubItem(of = {AbstractMediaGrabberTask.class}, group = "media-recorders")
public final class ContinousVideoRecordingTask extends AbstractTask<ContinousVideoRecordingConfig> implements MediaCaptureCallbackSupport, FileStorageSupport {
    public ContinousVideoRecordingTask(BoxEngine metricDeliveryService, AbstractListOfTask<?> parent) {
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

    @Override
    public FileStorage fileStorage() {
        return fileStorage;
    }

    private DateTimeFormatter dateTimeFormat;

    @Override
    public Thing<?> getSourceThing() {
        if (parent == null) {
            return null;
        }
        return parent.getSourceThing();
    }

    @Override
    public Object uniqueness() {
        return configuration.storageUuid;
    }

    @Override
    public void doInit() throws Exception {
        if (this.fileStorage != null) {
            ((Thing<?>) this.fileStorage).unSubscribe(this);
        }
        Thing<?> fileStorageThing = engine.getThingAndSubscribe(configuration.storageUuid, this);
        this.fileStorage = (FileStorage) fileStorageThing;
        this.dateTimeFormat = DateTimeFormatter.ofPattern(configuration.pathPattern.format);
    }

    @Override
    public void doStart(boolean ignoreAutostartProperty, boolean startChildren) throws Exception {
        shutdown = false;
    }

    private String fileName;
    private FFmpegFrameRecorder recorder;
    private LocalDateTime flushTimeout;
    private LocalDateTime recordingStarted;

    private boolean shutdown;

    int width, height;

    private Queue<Frame> framesQueue;
    private boolean needCommit;

    @Override
    protected synchronized boolean doProcess(ExecutionContext executionContext) throws Exception {
        if (shutdown) {
            return true;
        }
        LocalDateTime now = currentTimeInServerTz();
        UUID dataSourceUuid = executionContext.getVar(SOURCE_TASK_UUID);
        FrameGrabber frameGrabber = executionContext.getVar(FrameGrabber.class);
        Frame frame = executionContext.getVar(Frame.class);
        if (configuration.disableAudioRecording && frame.type == Frame.Type.AUDIO) {
            return true;
        }
        boolean videoKeyFrame = executionContext.getVar(VIDEO_KEY_FRAME) == null || (Boolean) executionContext.getVar(VIDEO_KEY_FRAME);
        boolean timeoutTriggered = flushTimeout != null && now.isAfter(flushTimeout);

        if (recorder != null && timeoutTriggered && videoKeyFrame) {
            needCommit = true;
            while (needCommit) {
                Thread.sleep(50);
            }
        }

        if (recorder == null) {
            needCommit = false;
            recordingStarted = currentTimeInServerTz();
            LocalDateTime fileTime = SplitTimeInterval.ceilTimeout(recordingStarted, configuration.splitTimeout);
            this.flushTimeout = SplitTimeInterval.floorTimeout(recordingStarted, configuration.splitTimeout);

            final MediaFormat mf = MediaFormat.mp4;

            String fileNamePart = dateTimeFormat.format(fileTime);
            this.fileName = '/' + dataSourceUuid.toString() + '/' + fileNamePart + "." + mf.name();

            log.info("Start recording.\nFileName: {}\nRecording length: {} ms",
                    fileNamePart,
                    Duration.between(recordingStarted, this.flushTimeout)
            );
            File file = fileStorage.startOutputTransaction(fileName);

            width = frame.imageWidth;
            height = frame.imageHeight;

            this.framesQueue = new LinkedBlockingQueue<>();
            this.recorder = createRecorder(frameGrabber, mf, file, getUuid(), executionContext);
//            this.recorder.start();
            this.recorder.startUnsafe();

            SystemThreadsService.execute(this, () -> {
//                System.out.println("Queue: started ===============================");
                Queue<Frame> frameQueue = framesQueue;
                a:
                while (frameQueue == framesQueue) {
                    while (frameQueue.isEmpty()) {
                        if (needCommit || state != State.RUN) {
//                            System.out.println("Queue: need commit");
                            break a;
                        }
                        try {
//                            System.out.println("Queue: wait frame");
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            onProcessingException(e);
                        }
                    }
                    while (!frameQueue.isEmpty()) {
//                        System.out.println("Queue: poll frame & write");
                        try (Frame f = frameQueue.poll()) {
                            recorder.record(f);
                        } catch (FFmpegFrameRecorder.Exception e) {
                            onProcessingException(e);
                            try {
                                commit(recordingStarted, currentTimeInServerTz());
                            } catch (Exception ex) {
                                log.error(ex.getMessage(), ex);
                            }
                        }
                    }
                }
                try {
//                    System.out.println("Queue: Commit");
                    commit(recordingStarted, currentTimeInServerTz());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }

        long fts = frame.timestamp;
        long rts = recorder.getTimestamp();
        if (fts < rts) {
            frame.timestamp = rts + 10;
        }
//        System.out.println("Queue: add frame");
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

        return true;
    }


    @Override
    public void doStop() throws Exception {
        shutdown = true;
        Thread.sleep(500);// wait flushing frame from main thread
        commit(recordingStarted, currentTimeInServerTz());
    }

    private void commit(LocalDateTime tsStart, LocalDateTime tsEnd) throws Exception {
        if (recorder == null) {
            return;
        }
        long duration = Duration.between(tsStart, tsEnd).toMillis();
        try {
            this.recorder.stop();
        } finally {
            this.recorder = null;
            String fileName = this.fileName;
            SystemThreadsService.execute(this, () -> {
                try {
                    Thread.sleep(500);// wait to stop recorder
                    fileStorage.commitOutputTransaction(fileName, (pair) -> {
                        FileCreatedEvent evt = new FileCreatedEvent(
                                nodeType(this.getClass()),
                                this.getUuid(),
                                getSelfClassName(),
                                getTitle(),
                                configuration.storageUuid,
                                pair.getKey()
                        );
                        evt.option("width", width);
                        evt.option("height", height);
                        evt.option("duration", duration);
                        evt.option("tsStart", tsStart);
                        evt.option("tsEnd", tsEnd);
                        engine.fireEvent(evt);
                    });
//                    log.info("Recording committed: {}", new Date());
                } catch (Throwable e) {
                    log.error("Recording commit failed.", e);
                }
            });
            needCommit = false;
        }
    }

    public FFmpegFrameRecorder createRecorder(FrameGrabber grabber, MediaFormat mediaFormat, File outputFile, UUID uuid, ExecutionContext executionContext) throws Exception {
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
        recorder.setOption("threads", "4");
        recorder.setFormat(mediaFormat.name());
        int bitRate = configuration.videoBitRate.bitrate;
        if (isVideoExists) {
            recorder.setInterleaved(true);
            recorder.setFrameRate(frameRate);

//            if (!"cpu".equals(configuration.hwAccel)) {
//                recorder.setOption("hwaccel", configuration.hwAccel);
//            }
//            if (!"default".equals(configuration.encoder)) {
//                recorder.setOption("c:v", configuration.encoder);
//                recorder.setOption("vcodec", configuration.encoder);
//                recorder.setVideoCodecName(configuration.encoder);
//                recorder.setOption("gpu", "1");
//                if ("h264_amf".equals(configuration.encoder)) {
//                    recorder.setVideoOption("usage", "transcoding");
//                    recorder.setVideoOption("profile", "high");
//                }
//            } else {
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
//            recorder.setVideoCodecName("h264");
//            recorder.setPixelFormat(AV_PIX_FMT_NV12);
//            recorder.setOption("hwaccel", "dxva2");
//            recorder.setMaxBFrames(0);
//            }


            if (bitRate == -1) {
                recorder.setVideoBitrate(videoBitrate);
            } else {
//                recorder.setVideoOption("tune", "zerolatency");
//                recorder.setVideoOption("preset", "ultrafast");
//                recorder.setVideoOption("crf", "28");

                recorder.setVideoBitrate(configuration.videoBitRate.bitrate);
                recorder.setAspectRatio(grabber.getAspectRatio());

                recorder.setGopSize(configuration.gop);
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

    @Override
    public void destroy() {
        if (this.fileStorage != null) {
            ((Thing<?>) this.fileStorage).unSubscribe(this);
            log.debug("{}: unsubscribed", getUuid());
        }
    }

    @Override
    public Set<Class<? extends AbstractEvent>> produceEvents() {
        Set<Class<? extends AbstractEvent>> events = new HashSet<>(super.produceEvents());
        events.add(FileCreatedEvent.class);
        return events;
    }
}
