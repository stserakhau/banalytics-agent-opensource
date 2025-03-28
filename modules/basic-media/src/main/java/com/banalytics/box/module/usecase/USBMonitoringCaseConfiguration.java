package com.banalytics.box.module.usecase;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.module.storage.filesystem.ServerLocalFileSystemNavigatorConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import static com.banalytics.box.api.integration.form.ComponentType.int_input;

@Getter
@Setter
public class USBMonitoringCaseConfiguration {
    @UIComponent(index = 10, type = ComponentType.drop_down, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(bean = "localMediaDeviceDiscoveryService", method = "cameras")
            },
            restartOnChange = true
    )
    public String camera;

    @UIComponent(index = 20, type = ComponentType.drop_down, required = true,
            dependsOn = {"camera"},
            uiConfig = {//
                    @UIComponent.UIConfig(name = "enableCondition", value = "form.camera !== ''"),
                    @UIComponent.UIConfig(name = "api-uuid", value = "00000000-0000-0000-0000-000000000005"),//portal integration thing
                    @UIComponent.UIConfig(name = "api-method", value = "bean:localMediaDeviceDiscoveryService:cameraResolutionsFps"),
                    @UIComponent.UIConfig(name = "api-params", value = "camera")
            }, restartOnChange = true
    )
    public String resolution;

    @UIComponent(index = 30, type = ComponentType.int_input, required = true,
            dependsOn = {"camera"},
            uiConfig = {
                    @UIComponent.UIConfig(name = "enableCondition", value = "form.camera !== ''"),
                    @UIComponent.UIConfig(name = "min", value = "1"),
                    @UIComponent.UIConfig(name = "max", value = "50")
            }, restartOnChange = true
    )
    public double fps = 10;

    @UIComponent(
            index = 40,
            type = ComponentType.drop_down,
            required = true, restartOnChange = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "show-empty", value = "false"),
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(bean = "taskService", method = "findActiveByStandard", params = {"com.banalytics.box.module.storage.FileSystem"})
            }
    )
    public UUID fileSystemUuid = ServerLocalFileSystemNavigatorConfig.SERVER_LOCAL_FS_NAVIGATOR_UUID;

    @UIComponent(index = 50,
            type = ComponentType.checkbox,
            required = true
    )
    public boolean enableContinuousRecording = false;

    @UIComponent(
            index = 60,
            type = ComponentType.folder_chooser,
            required = true, restartOnChange = true,
            dependsOn = {"enableContinuousRecording", "fileSystemUuid"},
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableContinuousRecording === true"),
                    @UIComponent.UIConfig(name = "api-uuid", value = "fileSystemUuid"),
                    @UIComponent.UIConfig(name = "enableFolderSelection", value = "true"),
                    @UIComponent.UIConfig(name = "enableFileSelection", value = "false")
            })
    public String continuousRecordingUri;

    @UIComponent(index = 65,
            type = ComponentType.drop_down,
            dependsOn = "enableContinuousRecording",
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableContinuousRecording === true")
            },
            required = true
    )
    public RecordingType continuousRecordingType = RecordingType.VIDEO;

    @UIComponent(index = 66,
            type = int_input,
            dependsOn = {"enableContinuousRecording", "continuousRecordingType"},
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableContinuousRecording === true && form.continuousRecordingType === 'PHOTO'"),
                    @UIComponent.UIConfig(name = "min", value = "1"),
                    @UIComponent.UIConfig(name = "max", value = "3600")
            }
    )
    public int continuousPhotoIntervalSec = 5;

    @UIComponent(index = 70,
            type = ComponentType.date_time,
            dependsOn = "enableContinuousRecording",
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableContinuousRecording === true"),
                    @UIComponent.UIConfig(name = "type", value = "time")
            },
            required = true
    )
    public String continuousFromTime = "08:00";

    @UIComponent(index = 80,
            type = ComponentType.date_time,
            dependsOn = {"enableContinuousRecording", "enableMotionRecording"},
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition",
                            value = "form.enableContinuousRecording === true && form.enableMotionRecording !== true"),
                    @UIComponent.UIConfig(name = "type", value = "time")
            },
            required = true
    )
    public String continuousToTime = "17:00";


    @UIComponent(index = 90,
            type = ComponentType.checkbox,
            required = true
    )
    public boolean enableMotionDetection = false;

    @UIComponent(index = 95,
            type = ComponentType.checkbox,
            required = true,
            dependsOn = "enableMotionDetection",
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableMotionDetection === true")
            }
    )
    public boolean enableMotionRecording = false;

    @UIComponent(
            index = 100,
            type = ComponentType.folder_chooser,
            required = true, restartOnChange = true,
            dependsOn = {"enableMotionDetection", "enableMotionRecording", "fileSystemUuid"},
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableMotionDetection === true && form.enableMotionRecording === true"),
                    @UIComponent.UIConfig(name = "api-uuid", value = "fileSystemUuid"),
                    @UIComponent.UIConfig(name = "enableFolderSelection", value = "true"),
                    @UIComponent.UIConfig(name = "enableFileSelection", value = "false")
            })
    public String motionRecordingUri;

    @UIComponent(index = 110,
            type = ComponentType.date_time,
            dependsOn = {"enableMotionDetection", "enableMotionRecording"},
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableMotionDetection === true && form.enableMotionRecording === true"),
                    @UIComponent.UIConfig(name = "type", value = "time")
            },
            required = true
    )
    public String motionDetectionFromTime = "17:00";

    @UIComponent(index = 120,
            type = ComponentType.date_time,
            dependsOn = {"enableMotionDetection", "enableMotionRecording", "enableContinuousRecording"},
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition",
                            value = "form.enableMotionDetection === true && form.enableMotionRecording === true && form.enableContinuousRecording !== true"),
                    @UIComponent.UIConfig(name = "type", value = "time")
            },
            required = true
    )
    public String motionDetectionToTime = "08:00";

    @UIComponent(index = 130,
            type = ComponentType.checkbox,
            required = true,
            dependsOn = "enableMotionDetection",
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableMotionDetection === true")
            }
    )
    public boolean enableMotionPhotoShotRecording = false;

    @UIComponent(
            index = 140,
            type = ComponentType.folder_chooser,
            required = true, restartOnChange = true,
            dependsOn = {"enableMotionDetection", "enableMotionPhotoShotRecording", "fileSystemUuid"},
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableMotionDetection === true && form.enableMotionPhotoShotRecording === true"),
                    @UIComponent.UIConfig(name = "api-uuid", value = "fileSystemUuid"),
                    @UIComponent.UIConfig(name = "enableFolderSelection", value = "true"),
                    @UIComponent.UIConfig(name = "enableFileSelection", value = "false")
            })
    public String motionPhotoShotRecordingUri;

    @UIComponent(index = 150,
            type = int_input,
            dependsOn = {"enableMotionDetection", "enableMotionPhotoShotRecording"},
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableMotionDetection === true && form.enableMotionPhotoShotRecording === true"),
                    @UIComponent.UIConfig(name = "min", value = "1"),
                    @UIComponent.UIConfig(name = "max", value = "10")
            }
    )
    public int motionPhotoShotIntervalSec = 1000;


    public enum RecordingType {
        VIDEO, PHOTO
    }
}
