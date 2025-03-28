package com.banalytics.box.module.usecase;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.module.storage.filesystem.ServerLocalFileSystemNavigatorConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class IPMonitoringCaseConfiguration {
    @UIComponent(index = 10, type = ComponentType.text_input_suggestion,
            uiConfig = {//
                    @UIComponent.UIConfig(name = "suggestionAPI", value = "https://console.banalytics.live/api/public/camera/producers?producer=${form.camProducer}:*"),
            },
            required = true)
    public String camProducer;

    @UIComponent(index = 20, type = ComponentType.drop_down,
            dependsOn = {"camProducer"},
            uiConfig = {//
                    @UIComponent.UIConfig(name = "enableCondition", value = "form.camProducer != ''"),
                    @UIComponent.UIConfig(name = "restAPI",
                            value = "https://console.banalytics.live/api/public/camera/models?producer=${form.camProducer}"),
                    @UIComponent.UIConfig(name = "labelFormatter", value = "${item}"),
                    @UIComponent.UIConfig(name = "valueFormatter", value = "${item}")
            },
            required = true)
    public String camModel;

    @UIComponent(index = 30, type = ComponentType.drop_down,
            dependsOn = {"camModel"},
            uiConfig = {//
                    @UIComponent.UIConfig(name = "enableCondition", value = "form.camModel != ''"),
                    @UIComponent.UIConfig(name = "restAPI",
                            value = "https://console.banalytics.live/api/public/camera/descriptor?producer=${form.camProducer}&model=${form.camModel}&types=VLC&types=MJPEG&types=FFMPEG"),
                    @UIComponent.UIConfig(name = "labelFormatter", value = "${item.url}"),
                    @UIComponent.UIConfig(name = "valueFormatter", value = "${item.url}")
//                    @UIComponent.UIConfig(name = "onchange", value = "formElement['streamUri'].value = self.value")
            },
            required = true)
    public String streamUri;

    @UIComponent(index = 31, type = ComponentType.text_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{username}') > -1 ")},
            required = true)
    public String username;
    @UIComponent(index = 32, type = ComponentType.text_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{password}') > -1 ")},
            required = true)
    public String password;
    @UIComponent(index = 33, type = ComponentType.text_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{host}') > -1 ")},
            required = true)
    public String host;

    @UIComponent(index = 34, type = ComponentType.text_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{CHANNEL}') > -1 ")},
            required = true)
    public String channel;
    @UIComponent(index = 35, type = ComponentType.int_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{WIDTH}') > -1 ")},
            required = true)
    public int width;
    @UIComponent(index = 36, type = ComponentType.int_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{HEIGHT}') > -1 ")},
            required = true)
    public int height;
    @UIComponent(index = 37, type = ComponentType.text_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{AUTH}') > -1 ")},
            required = true)
    public String auth;
    @UIComponent(index = 38, type = ComponentType.text_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{TOKEN}') > -1 ")},
            required = true)
    public String token;
    @UIComponent(index = 39, type = ComponentType.text_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{CODE}') > -1 ")},
            required = true)
    public String code;
    @UIComponent(index = 40, type = ComponentType.text_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{x0}') > -1 ")},
            required = true)
    public int x0;
    @UIComponent(index = 41, type = ComponentType.text_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{y0}') > -1 ")},
            required = true)
    public int y0;
    @UIComponent(index = 42, type = ComponentType.text_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{x1}') > -1 ")},
            required = true)
    public int x1;
    @UIComponent(index = 43, type = ComponentType.text_input,
            dependsOn = {"streamUri"},
            uiConfig = {@UIComponent.UIConfig(name = "visibleCondition", value = "form.streamUri.indexOf('{y1}') > -1 ")},
            required = true)
    public int y1;


    @UIComponent(
            index = 50,
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

    @UIComponent(index = 60,
            type = ComponentType.checkbox,
            required = true
    )
    public boolean enableContinuousRecording = false;

    @UIComponent(
            index = 65,
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

    @UIComponent(index = 66,
            type = ComponentType.drop_down,
            dependsOn = "enableContinuousRecording",
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableContinuousRecording === true")
            },
            required = true
    )
    public RecordingType continuousRecordingType = RecordingType.VIDEO;

    @UIComponent(index = 67,
            type = ComponentType.int_input,
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
            type = ComponentType.int_input,
            dependsOn = {"enableMotionDetection", "enableMotionPhotoShotRecording"},
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.enableMotionDetection === true && form.enableMotionPhotoShotRecording === true"),
                    @UIComponent.UIConfig(name = "min", value = "1"),
                    @UIComponent.UIConfig(name = "max", value = "10")
            }
    )
    public int motionPhotoShotIntervalSec = 1;


    public enum RecordingType {
        VIDEO, PHOTO
    }
}
