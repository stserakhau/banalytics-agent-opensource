package com.banalytics.box.module.usecase;

import com.banalytics.box.module.*;
import com.banalytics.box.module.constants.MediaUrlSchema;
import com.banalytics.box.module.constants.RestartOnFailure;
import com.banalytics.box.module.events.EventManagerThing;
import com.banalytics.box.module.media.task.ffmpeg.SimpleRTSPGrabberTask;
import com.banalytics.box.module.media.task.motion.detector.MotionDetectionTask;
import com.banalytics.box.module.media.task.motion.storage.MotionImageShotTask;
import com.banalytics.box.module.media.task.motion.storage.MotionVideoRecordingTask;
import com.banalytics.box.module.media.task.storage.ContinousVideoRecordingTask;
import com.banalytics.box.module.media.task.storage.ContinuousImageShotTask;
import com.banalytics.box.module.media.task.watermark.WatermarkTask;
import com.banalytics.box.module.media.thing.UrlMediaStreamThing;
import com.banalytics.box.module.onvif.task.ffmpeg.OnvifGrabberTask;
import com.banalytics.box.module.onvif.thing.OnvifThing;
import com.banalytics.box.module.storage.filestorage.FileStorageThing;
import com.banalytics.box.module.system.ExecuteActionGroupAction;
import com.banalytics.box.module.system.TaskActionAction;
import com.banalytics.box.module.system.TaskActionActionConfiguration;
import org.onvif.ver10.schema.Profile;
import org.onvif.ver10.schema.VideoEncoderConfiguration;
import org.onvif.ver10.schema.VideoEncoding;
import org.onvif.ver10.schema.VideoResolution;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.banalytics.box.api.integration.webrtc.channel.environment.ThingApiCallReq.PARAM_METHOD;
import static com.banalytics.box.module.events.EventManagerThing.*;

public class OnvifMonitoringCase extends AbstractUseCase<OnvifMonitoringCaseConfiguration> {
    Instance instance;

    public OnvifMonitoringCase(BoxEngine engine) {
        super(engine);
    }

    @Override
    public void create() throws Exception {
        instance = engine.getPrimaryInstance();
        if (
                configuration.enableContinuousRecording &&
                        configuration.enableMotionRecording &&
                        configuration.continuousRecordingUri.equals(configuration.motionRecordingUri)) {
            throw new Exception("Continuous & Motion Recording storage URI must be different.");
        }
        double grabberMaxFps = 0;
        OnvifThing camera = findOnvifThing(configuration.host);
        if (camera == null) {
            camera = new OnvifThing(engine);
            camera.configuration.title = "Camera " + configuration.host;
            camera.configuration.host = configuration.host;
            camera.configuration.port = configuration.port;
            camera.configuration.username = configuration.username;
            camera.configuration.password = configuration.password;

            camera.getConfiguration().restartOnFailure = RestartOnFailure.RESTART_10_SECONDS;
            camera = engine.saveOrUpdateThing(camera, true, null, true);
        }
//        waiting device initialization
        while (camera.getState() != State.RUN) {
            Thread.sleep(1000);
            if (camera.getState() == State.ERROR || camera.getState() == State.INIT_ERROR) {
                engine.deleteThing(camera.getUuid());
                Thread.sleep(1000);
                throw new Exception(camera.getStateDescription());
            }
        }
        {
            List<OnvifGrabberTask> grabberTasks = camera.findSubscriber(OnvifGrabberTask.class);
            OnvifGrabberTask grabberTask = grabberTasks.isEmpty() ? null : grabberTasks.get(0);
            if (grabberTask == null) {
                grabberTask = new OnvifGrabberTask(engine, instance);
                grabberTask.configuration.deviceUuid = camera.getUuid();
                grabberTask.configuration.maxFps = grabberMaxFps;

                String minResolutionToken = null;
                int minResolution = 10000;
                for (Map.Entry<String, Profile> entry : camera.profiles().entrySet()) {
                    String token = entry.getKey();
                    Profile profile = entry.getValue();
                    VideoEncoderConfiguration vec = profile.getVideoEncoderConfiguration();
                    VideoResolution vr = vec.getResolution();
                    int width = vr.getWidth();
                    // int fps = vec.getRateControl().getFrameRateLimit();
                    if (minResolutionToken == null || width < minResolution) {
                        minResolutionToken = token;
                        minResolution = width;
                    }
                }
                grabberTask.configuration.deviceProfile = minResolutionToken;

                engine.saveOrUpdateTask(grabberTask, true, null, true, instance);
            }

            {
                List<WatermarkTask> watermarkTasks = grabberTask.findSubTask(WatermarkTask.class);
                WatermarkTask watermarkTask = watermarkTasks.isEmpty() ? null : watermarkTasks.get(0);
                if (watermarkTask == null) {
                    watermarkTask = new WatermarkTask(engine, grabberTask);
                    engine.saveOrUpdateTask(watermarkTask, true, null, true, grabberTask);
                }
            }

            TaskActionAction stopCVR = null;
            TaskActionAction startCVR = null;

            TaskActionAction stopMDT = null;
            TaskActionAction startMDT = null;
            TaskActionAction stopMRT = null;
            TaskActionAction startMRT = null;
            TaskActionAction stopMPST = null;
            TaskActionAction startMPST = null;

            if (configuration.enableContinuousRecording) {
                final AbstractTask<?> recordingTask;

                FileStorageThing continuousFSThing = findFileStorage(configuration.continuousRecordingUri);
                if (continuousFSThing == null) {
                    continuousFSThing = new FileStorageThing(engine);
                    continuousFSThing.getConfiguration().destinationUri = configuration.continuousRecordingUri;
                    continuousFSThing = engine.saveOrUpdateThing(continuousFSThing, true, null, true);
                }

                switch (configuration.continuousRecordingType) {
                    case VIDEO -> {
                        List<ContinousVideoRecordingTask> continuousVideoRecordingTasks = grabberTask.findSubTask(ContinousVideoRecordingTask.class);
                        if (continuousVideoRecordingTasks.isEmpty()) {
                            {
                                ContinousVideoRecordingTask _recordingTask = new ContinousVideoRecordingTask(engine, grabberTask);
                                _recordingTask.configuration.storageUuid = continuousFSThing.getUuid();
                                engine.saveOrUpdateTask(_recordingTask, true, null, false, grabberTask);
                                recordingTask = _recordingTask;
                            }
                        } else {
                            recordingTask = continuousVideoRecordingTasks.get(0);
                        }
                    }
                    case PHOTO -> {
                        List<ContinuousImageShotTask> continuousImageShotTasks = grabberTask.findSubTask(ContinuousImageShotTask.class);
                        if (continuousImageShotTasks.isEmpty()) {
                            {
                                ContinuousImageShotTask _recordingTask = new ContinuousImageShotTask(engine, grabberTask);
                                _recordingTask.configuration.storageUuid = continuousFSThing.getUuid();
                                _recordingTask.configuration.photoIntervalMillis = configuration.continuousPhotoIntervalSec * 1000;
                                engine.saveOrUpdateTask(_recordingTask, true, null, false, grabberTask);
                                recordingTask = _recordingTask;
                            }
                        } else {
                            recordingTask = continuousImageShotTasks.get(0);
                        }
                    }
                    default -> {
                        throw new RuntimeException(configuration.continuousRecordingType + " not supports.");
                    }
                }

                List<TaskActionAction> actions = instance.findSubTask(TaskActionAction.class);
                for (TaskActionAction action : actions) {
                    if (action.configuration.targetTask.equals(recordingTask.getUuid())) {
                        if (action.configuration.action == TaskActionActionConfiguration.Action.STOP) {
                            stopCVR = action;
                        } else if (action.configuration.action == TaskActionActionConfiguration.Action.START) {
                            startCVR = action;
                        }
                    }
                }
                if (stopCVR == null) {
                    stopCVR = createTaskActionFor(recordingTask, TaskActionActionConfiguration.Action.STOP);
                }
                if (startCVR == null) {
                    startCVR = createTaskActionFor(recordingTask, TaskActionActionConfiguration.Action.START);
                }
            }
            List<TaskActionAction> actions = instance.findSubTask(TaskActionAction.class);
            if (configuration.enableMotionDetection) {
                List<MotionDetectionTask> motionDetectionTasks = grabberTask.findSubTask(MotionDetectionTask.class);
                MotionDetectionTask motionDetectionTask = motionDetectionTasks.isEmpty() ? null : motionDetectionTasks.get(0);
                if (motionDetectionTask == null) {
                    motionDetectionTask = new MotionDetectionTask(engine, grabberTask);
                    engine.saveOrUpdateTask(motionDetectionTask, true, null, false, grabberTask);
                }
                for (TaskActionAction action : actions) {
                    if (action.configuration.targetTask.equals(motionDetectionTask.getUuid())) {
                        if (action.configuration.action == TaskActionActionConfiguration.Action.STOP) {
                            stopMDT = action;
                        } else if (action.configuration.action == TaskActionActionConfiguration.Action.START) {
                            startMDT = action;
                        }
                    }
                }
                if (stopMDT == null) {
                    stopMDT = createTaskActionFor(motionDetectionTask, TaskActionActionConfiguration.Action.STOP);
                }
                if (startMDT == null) {
                    startMDT = createTaskActionFor(motionDetectionTask, TaskActionActionConfiguration.Action.START);
                }

                if (configuration.enableMotionRecording) {
                    FileStorageThing motionFSThing = findFileStorage(configuration.motionRecordingUri);
                    if (motionFSThing == null) {
                        motionFSThing = new FileStorageThing(engine);
                        motionFSThing.getConfiguration().destinationUri = configuration.motionRecordingUri;
                        motionFSThing = engine.saveOrUpdateThing(motionFSThing, true, null, true);
                    }

                    final MotionVideoRecordingTask recordingTask;
                    List<MotionVideoRecordingTask> motionVideoRecordingTasks = grabberTask.findSubTask(MotionVideoRecordingTask.class);
                    if (motionVideoRecordingTasks.isEmpty()) {
                        recordingTask = new MotionVideoRecordingTask(engine, grabberTask);
                        recordingTask.configuration.storageUuid = motionFSThing.getUuid();
                        engine.saveOrUpdateTask(recordingTask, true, null, false, grabberTask);
                    } else {
                        recordingTask = motionVideoRecordingTasks.get(0);
                    }

                    for (TaskActionAction action : actions) {
                        if (action.configuration.targetTask.equals(recordingTask.getUuid())) {
                            if (action.configuration.action == TaskActionActionConfiguration.Action.STOP) {
                                stopMRT = action;
                            } else if (action.configuration.action == TaskActionActionConfiguration.Action.START) {
                                startMRT = action;
                            }
                        }
                    }
                    if (stopMRT == null) {
                        stopMRT = createTaskActionFor(recordingTask, TaskActionActionConfiguration.Action.STOP);
                    }
                    if (startMRT == null) {
                        startMRT = createTaskActionFor(recordingTask, TaskActionActionConfiguration.Action.START);
                    }
                }
                if (configuration.enableMotionPhotoShotRecording) {
                    FileStorageThing motionFSThing = findFileStorage(configuration.motionPhotoShotRecordingUri);
                    if (motionFSThing == null) {
                        motionFSThing = new FileStorageThing(engine);
                        motionFSThing.getConfiguration().destinationUri = configuration.motionRecordingUri;
                        motionFSThing = engine.saveOrUpdateThing(motionFSThing, true, null, true);
                    }
                    final MotionImageShotTask recordingTask;
                    List<MotionImageShotTask> motionImageShotTasks = grabberTask.findSubTask(MotionImageShotTask.class);
                    if (motionImageShotTasks.isEmpty()) {
                        recordingTask = new MotionImageShotTask(engine, grabberTask);
                        recordingTask.configuration.storageUuid = motionFSThing.getUuid();
                        recordingTask.configuration.photoIntervalMillis = configuration.motionPhotoShotIntervalSec * 1000;
                        engine.saveOrUpdateTask(recordingTask, true, null, false, grabberTask);
                    } else {
                        recordingTask = motionImageShotTasks.get(0);
                    }

                    for (TaskActionAction action : actions) {
                        if (action.configuration.targetTask.equals(recordingTask.getUuid())) {
                            if (action.configuration.action == TaskActionActionConfiguration.Action.STOP) {
                                stopMPST = action;
                            } else if (action.configuration.action == TaskActionActionConfiguration.Action.START) {
                                startMPST = action;
                            }
                        }
                    }
                    if (stopMPST == null) {
                        stopMPST = createTaskActionFor(recordingTask, TaskActionActionConfiguration.Action.STOP);
                    }
                    if (startMPST == null) {
                        startMPST = createTaskActionFor(recordingTask, TaskActionActionConfiguration.Action.START);
                    }
                }
            }

            if (configuration.enableMotionRecording || configuration.enableMotionPhotoShotRecording || configuration.enableContinuousRecording) {
                EventManagerThing eventManagerThing = new EventManagerThing(engine);
                eventManagerThing.configuration.title = "USB Monitoring Rules";
                eventManagerThing = engine.saveOrUpdateThing(eventManagerThing, true, null, true);

                if ((configuration.enableMotionRecording || configuration.enableMotionPhotoShotRecording) && configuration.enableContinuousRecording) {
                    final ExecuteActionGroupAction enableDayMode;
                    final ExecuteActionGroupAction enableNightMode;
                    if (configuration.enableMotionRecording && configuration.enableMotionPhotoShotRecording) {
                        enableDayMode = createExecuteActionGroupActionFor("Day mode", stopMDT, stopMRT, stopMPST, startCVR);
                        enableNightMode = createExecuteActionGroupActionFor("Night mode", stopCVR, startMDT, startMRT, startMPST);
                    } else if (configuration.enableMotionRecording) {
                        enableDayMode = createExecuteActionGroupActionFor("Day mode", stopMDT, stopMRT, startCVR);
                        enableNightMode = createExecuteActionGroupActionFor("Night mode", stopCVR, startMDT, startMRT);
                    } else {/*if(configuration.enableMotionPhotoShotRecording)*/
                        enableDayMode = createExecuteActionGroupActionFor("Day mode", stopMDT, stopMPST, startCVR);
                        enableNightMode = createExecuteActionGroupActionFor("Night mode", stopCVR, startMDT, startMPST);
                    }
                    scheduleAction(eventManagerThing, "Enable day mode", enableDayMode, configuration.continuousFromTime);
                    scheduleAction(eventManagerThing, "Enable night mode", enableNightMode, configuration.motionDetectionFromTime);

                    ExecutionContext ctx = new ExecutionContext();
                    ctx.setVar(IAction.MANUAL_RUN, IAction.MANUAL_RUN);
                    enableDayMode.action(ctx);
                } else if (configuration.enableContinuousRecording) {//create continuous action group and schedule
                    scheduleAction(eventManagerThing, "Start continuous recording", startCVR, configuration.continuousFromTime);
                    scheduleAction(eventManagerThing, "Stop continuous recording", stopCVR, configuration.continuousToTime);

                    ExecutionContext ctx = new ExecutionContext();
                    ctx.setVar(IAction.MANUAL_RUN, IAction.MANUAL_RUN);
                    startCVR.action(ctx);
                } else if (configuration.enableMotionRecording || configuration.enableMotionPhotoShotRecording) {
                    final ExecuteActionGroupAction startMD;
                    final ExecuteActionGroupAction stopMD;

                    if (configuration.enableMotionRecording && configuration.enableMotionPhotoShotRecording) {
                        startMD = createExecuteActionGroupActionFor("Start motion recording", startMDT, startMRT, startMPST);
                        stopMD = createExecuteActionGroupActionFor("Stop motion recording", stopMDT, stopMRT, stopMPST);
                    } else if (configuration.enableMotionRecording) {
                        startMD = createExecuteActionGroupActionFor("Start motion recording", startMDT, startMRT);
                        stopMD = createExecuteActionGroupActionFor("Stop motion recording", stopMDT, stopMRT);
                    } else {/*if(configuration.enableMotionPhotoShotRecording)*/
                        startMD = createExecuteActionGroupActionFor("Start motion recording", startMDT, startMPST);
                        stopMD = createExecuteActionGroupActionFor("Stop motion recording", stopMDT, stopMPST);
                    }

                    scheduleAction(eventManagerThing, "Start motion recording", startMD, configuration.motionDetectionFromTime);
                    scheduleAction(eventManagerThing, "Stop motion recording", stopMD, configuration.motionDetectionToTime);

                    ExecutionContext ctx = new ExecutionContext();
                    ctx.setVar(IAction.MANUAL_RUN, IAction.MANUAL_RUN);
                    startMD.action(ctx);
                }
            }
        }
    }

    @Override
    public String groupCode() {
        return "VIDEO_SURVEILLANCE";
    }

    private void scheduleAction(EventManagerThing eventManagerThing, String ruleTitle, AbstractAction<?> action, String actionTime) throws Exception {
        String[] parts = actionTime.split(":");
        String hour = parts[0];
        String minute = parts[1];
        UUID ruleDayUuid = (UUID) eventManagerThing.call(Map.of(
                PARAM_METHOD, "updateCreateRule",
                PARAM_RULE_TITLE, ruleTitle
        ));
        eventManagerThing.call(Map.of(
                PARAM_METHOD, "updateCronExpressions",
                PARAM_RULE_UUID, ruleDayUuid.toString(),
                PARAM_RULE_CRON_EXPRS, List.of(
                        "0 " + minute + " " + hour + " ? * MON *",
                        "0 " + minute + " " + hour + " ? * TUE *",
                        "0 " + minute + " " + hour + " ? * WED *",
                        "0 " + minute + " " + hour + " ? * THU *",
                        "0 " + minute + " " + hour + " ? * FRI *",
                        "0 " + minute + " " + hour + " ? * SAT *",
                        "0 " + minute + " " + hour + " ? * SUN *"
                )
        ));
        eventManagerThing.call(Map.of(
                PARAM_METHOD, "updateAddActionTask",
                PARAM_RULE_UUID, ruleDayUuid.toString(),
                PARAM_RULE_ACTION_UUID, action.getUuid().toString()
        ));
        eventManagerThing.call(Map.of(
                PARAM_METHOD, "updateEnableRule",
                PARAM_RULE_UUID, ruleDayUuid.toString()
        ));
    }

    private ExecuteActionGroupAction createExecuteActionGroupActionFor(String title, AbstractAction<?>... actions) throws Exception {
        ExecuteActionGroupAction actionGroup = new ExecuteActionGroupAction(engine, instance);
        actionGroup.configuration.title = title;
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append('\"').append(actions[0].getUuid()).append('\"');
        for (int i = 1; i < actions.length; i++) {
            AbstractAction<?> action = actions[i];
            sb.append(",\"").append(action.getUuid()).append('\"');
        }
        sb.append("]");
        actionGroup.configuration.fireActionsUuids = sb.toString();
        actionGroup.configuration.executionDelayMillis = 100;
        actionGroup.configuration.parallelExecution = false;

        return engine.saveOrUpdateTask(actionGroup, true, null, true, instance);
    }

    private TaskActionAction createTaskActionFor(ITask<?> task, TaskActionActionConfiguration.Action action) throws Exception {
        TaskActionAction taskAction = new TaskActionAction(engine, instance);
        taskAction.configuration.action = action;
        taskAction.configuration.affectAutostartState = true;
        taskAction.configuration.ignoreAutostartState = true;
        taskAction.configuration.targetTask = task.getUuid();
        return engine.saveOrUpdateTask(taskAction, true, null, true, instance);
    }

    private OnvifThing findOnvifThing(String host) throws Exception {
        List<Thing<?>> fsList = engine.findThings(UrlMediaStreamThing.class);
        for (Thing<?> fs : fsList) {
            if (fs instanceof OnvifThing lmt) {
                if (lmt.getConfiguration().host.equals(host)) {
                    return lmt;
                }
            }
        }
        return null;
    }

    private FileStorageThing findFileStorage(String uri) throws Exception {
        List<Thing<?>> fsList = engine.findThings(FileStorageThing.class);
        for (Thing<?> fs : fsList) {
            if (fs instanceof FileStorageThing fst) {
                if (fst.getConfiguration().getDestinationUri().equals(uri)) {
                    return fst;
                }
            }
        }
        return null;
    }
}
