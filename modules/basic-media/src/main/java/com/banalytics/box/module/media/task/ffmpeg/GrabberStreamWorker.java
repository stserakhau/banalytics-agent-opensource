package com.banalytics.box.module.media.task.ffmpeg;

import com.banalytics.box.module.ExecutionContext;
import com.banalytics.box.module.media.preprocessor.BanalyticsWatermarkPreprocessor;
import com.banalytics.box.module.media.task.AbstractStreamingMediaTask;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import static com.banalytics.box.BanalyticsBoxInstanceState.getInstance;
import static com.banalytics.box.module.ExecutionContext.GlobalVariables.*;
import static com.banalytics.box.module.State.RUN;
import static com.banalytics.box.service.SystemThreadsService.SYSTEM_TIMER;

@Slf4j
public class GrabberStreamWorker implements Runnable {
    private final AbstractStreamingMediaTask<?> task;
    private final String rotateImage;
    private final FFmpegFrameGrabber grabber;

    long videoFrameCounter = 0;
    long audioFrameCounter = 0;

    private final AtomicLong lastFrameReceivedTime = new AtomicLong(0);

    final long fpsDelay;
    final boolean filePlay;

    final boolean audioDisabled;

    private final BanalyticsWatermarkPreprocessor banalyticsWatermarkPreprocessor = new BanalyticsWatermarkPreprocessor(null, null);

    final double fps;

    public GrabberStreamWorker(AbstractStreamingMediaTask<?> task, FFmpegFrameGrabber grabber, boolean filePlay, double fps, String rotateImage) {
        this.task = task;
        this.grabber = grabber;
        this.rotateImage = rotateImage;
        this.filePlay = filePlay;
        this.fps = fps;
        this.fpsDelay = fps > 0 ? (long) (1000 / fps) : 0;
        this.audioDisabled = "disabled".equals(grabber.getMetadata("audio"));
    }

    private final ExecutionContext context = new ExecutionContext();

    int previousFrameWidth = -1;

    double[] realFrameRateMeasurements = new double[10];
    int pos = 0;

    private void initRTFps(double initVal) {
        Arrays.fill(realFrameRateMeasurements, initVal);
    }

    private void pushRTFpsVal(double value) {
        realFrameRateMeasurements[pos] = value;
        pos++;
        if (pos >= realFrameRateMeasurements.length) {
            pos = 0;
        }
    }

    double lastCalculated;
    long counter = 0;

    private double avgFps() {
        if (fps > 0) {
            return fps;
        }
        double sum = 0;
        for (double realFrameRateMeasurement : realFrameRateMeasurements) {
            sum += realFrameRateMeasurement;
        }
        if (lastCalculated == 0 || counter % realFrameRateMeasurements.length == 0) {
            lastCalculated = sum / realFrameRateMeasurements.length;
        }
        counter++;
        return lastCalculated;
    }

    long measurementStartTime;


    private static FFmpegFrameFilter getRotationFilter(FFmpegFrameGrabber grabber, String rotate) {
        if (rotate == null) {
            return null;
        }
        FFmpegFrameFilter frameFilter = switch (rotate) {
            case "90" -> new FFmpegFrameFilter("transpose=clock", grabber.getImageWidth(), grabber.getImageHeight());
            case "180" -> new FFmpegFrameFilter("rotate=PI", grabber.getImageWidth(), grabber.getImageHeight());
            case "270" -> new FFmpegFrameFilter("transpose=cclock", grabber.getImageWidth(), grabber.getImageHeight());
            default -> null;
        };
        if (frameFilter != null) {
            frameFilter.setPixelFormat(grabber.getPixelFormat());
            frameFilter.setFrameRate(grabber.getFrameRate());
        }
        return frameFilter;
    }

    public final Collection<ContextPreProcessor> contextPreProcessor = new ArrayList<>();

    public interface ContextPreProcessor {
        void preProcess(AbstractStreamingMediaTask<?> task, ExecutionContext context);
    }

    @Override
    public void run() {
        long frameRateControlTime = 0;
        FFmpegFrameFilter frameFilter = null;
        try {
            int counter = 100;
            while (task.state != RUN) {//10 seconds to get RUN state
                Thread.sleep(100); // wait transition to RUN state
                counter--;
                if (counter == 0) {
                    throw new Exception("Task can't transit to RUN state.");
                }
            }
            log.info("{}: Starting grabber...", task.getUuid());
//            grabber.start();
            grabber.startUnsafe(true);
            double realFrameRate = grabber.getFrameRate();
            initRTFps(fps > 0 ? fps : realFrameRate);
            if (filePlay) { //file play case
                long sleepTime = (long) (1000 / realFrameRate);
                if (sleepTime == 0) {
                    sleepTime = 100;
                }
                frameRateControlTime = sleepTime;
            }
            frameFilter = getRotationFilter(grabber, rotateImage);
            if (frameFilter != null) {
                frameFilter.start();
            }
        } catch (Throwable e) {
            task.onProcessingException(e);
            return;
        }
        int fpsMeasurementCounter = 0;
        long previousFrameTimestamp = 0;
        long nextImageFrameTimeout = 0;
        TimerTask streamControlJob = createStreamControlJob();
        try {
            SYSTEM_TIMER.schedule(streamControlJob, 10000, 5000);
            log.info("{}: Capture started. Task state: {}", task.getTitle(), task.state);
            Frame frame = null;
            try {
                boolean wasKeyFrame = false;
                while (task.state == RUN) {
                    long now = System.currentTimeMillis();
                    context.clear();
                    for (ContextPreProcessor preProcessor : contextPreProcessor) {
                        preProcessor.preProcess(task, context);
                    }
                    if (frameRateControlTime > 0) {//only for file play case
                        Thread.sleep(frameRateControlTime);
                    }
                    int counter = 0;
                    do {
                        if (audioDisabled) {
                            frame = grabber.grabImage();
                        } else {
                            frame = grabber.grabFrame();
                        }
                        if (frame == null) {
                            if (counter > 0) {
                                throw new Exception("Second null frame received. Media stream stopped. Restarting task '" + task.getTitle() + "' via " + task.configuration.restartOnFailure);
                            }
                            log.info("Null frame received. Restarting the grabber.");
                            Thread.sleep(200);
                            counter++;
                            grabber.restart();
                        }
                    } while (frame == null);

                    frame.timestamp = grabber.getTimestamp();
                    if (previousFrameTimestamp == frame.timestamp) {
                        task.sendTaskState("Frozen frame detected");
                    }
                    previousFrameTimestamp = frame.timestamp;

                    int imageWidth = grabber.getImageWidth();
                    if (imageWidth == 0) {
                        continue;
                    }
                    if (previousFrameWidth != -1) {
                        if (imageWidth != previousFrameWidth) {
                            throw new Exception("Frame size changed " + previousFrameWidth + "-> " + imageWidth + " pix. Restarting job: " + task.getTitle());
                        }
                    }

                    if (getInstance().isShowBanalyticsWatermark()) {
                        banalyticsWatermarkPreprocessor.preProcess(frame);
                    }

                    previousFrameWidth = imageWidth;
                    lastFrameReceivedTime.set(System.currentTimeMillis());

                    boolean videoFrame = frame.getTypes().contains(Frame.Type.VIDEO);
                    boolean videoKeyFrame = frame.keyFrame && videoFrame;

                    if (this.fpsDelay > 0 && videoFrame) {
                        if (now > nextImageFrameTimeout) {
                            nextImageFrameTimeout = now + this.fpsDelay;
                            if (wasKeyFrame) {
                                videoKeyFrame = true;
                            }
                        } else {
                            wasKeyFrame |= videoKeyFrame;
                            continue;
                        }
                    }

                    final Frame grabbedFrame;
                    if (videoFrame && frame.image != null) {
                        if (fpsMeasurementCounter > 3) {
                            long measurementEndTime = System.currentTimeMillis();
                            double realFrameRate = fpsMeasurementCounter / ((measurementEndTime - measurementStartTime) / 1000.0);
                            pushRTFpsVal(realFrameRate);
                            fpsMeasurementCounter = 0;
                            measurementStartTime = measurementEndTime;
                        }

                        videoFrameCounter++;
                        fpsMeasurementCounter++;

                        if (frameFilter != null) {
                            frameFilter.push(frame);
                            grabbedFrame = frameFilter.pull();
                        } else {
                            grabbedFrame = frame;
                        }
                    } else {
                        grabbedFrame = frame;
                        audioFrameCounter++;
                    }

                    context.setVar(Frame.class, grabbedFrame);
                    context.setVar(FrameGrabber.class, grabber);
                    context.setVar(VIDEO_KEY_FRAME, videoKeyFrame);
                    context.setVar(SOURCE_TASK_UUID, task.getUuid());
                    context.setVar(CALCULATED_FRAME_RATE, avgFps());

                    task.onFrameReceived(grabbedFrame, videoKeyFrame, avgFps());

                    task.process(context);

                    wasKeyFrame = false;
                }
                log.info("{}: Capture stopped.", task.getTitle());
            } finally {
                if (frame != null) {
                    frame.close();
                }
            }
        } catch (Throwable e) {
            log.error("{}: Capture stopped with error: {}", task.getTitle(), e.getMessage());
            task.onProcessingException(e);
        } finally {
            streamControlJob.cancel();
        }
        try {

            grabber.close();
            if (frameFilter != null) {
                frameFilter.close();
            }
            log.info("{}: Grabber stopped.", task.getTitle());
        } catch (Throwable e) {
            log.error("{}: Grabber stopped with error: {}", task.getTitle(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private TimerTask createStreamControlJob() {
        return new TimerTask() {
            long prevVideoFrameCounter = 0;
            long prevAudioFrameCounter = 0;

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (task.state == RUN) {
                    if (now - lastFrameReceivedTime.get() > 15000) {//if no frame grabbed in last 10 seconds, the restart
                        cancel();// self cancel
                        // and fire restart via exception
                        task.onProcessingException(new Exception("Frozen Media Stream. No frames received. Restarting the task: '" + task.getTitle() + "' via " + task.configuration.restartOnFailure));
                    } else {
//                        if (
//                                (grabber.hasVideo() && videoFrameCounter == prevVideoFrameCounter)
//                                        || (grabber.hasAudio() && audioFrameCounter == prevAudioFrameCounter)
//                        ) {
//                            cancel();// self cancel
//                            // and fire restart via exception
//                            task.onProcessingException(new Exception("Frozen Media Stream. Restarting the task: '" + task.getTitle() + "' via " + task.configuration.restartOnFailure));
//                        }
                    }
                    prevVideoFrameCounter = videoFrameCounter;
                    prevAudioFrameCounter = audioFrameCounter;
                } else {
                    cancel();
                }
            }
        };
    }
}