package com.banalytics.box.module.onvif.task.ffmpeg;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.module.AbstractConfiguration;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
public class OnvifGrabberTaskConfiguration extends AbstractConfiguration {
    @NotNull
    @UIComponent(
            index = 10,
            type = ComponentType.drop_down,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(bean = "taskService", method = "findActiveByStandard", params = {"com.banalytics.box.module.standard.Onvif"})
            },
            restartOnChange = true
    )
    public UUID deviceUuid;

    @UIComponent(
            index = 20,
            type = ComponentType.drop_down,
            required = true,
            dependsOn = {"deviceUuid"},
            uiConfig = {
                    @UIComponent.UIConfig(name = "api-uuid", value = "deviceUuid"),
                    @UIComponent.UIConfig(name = "api-method", value = "readProfilesList")
            },
            restartOnChange = true
    )
    public String deviceProfile;

    @UIComponent(
            index = 30,
            type = ComponentType.drop_down,
            required = true,
            backendConfig = {
                    @UIComponent.BackendConfig(values = {"0", "90", "180", "270"})
            },
            restartOnChange = true
    )
    public String rotateImage = "0";

    @UIComponent(index = 40,
            type = ComponentType.checkbox,
            required = true,
            restartOnChange = true
    )
    public boolean disableAudioRecording = true;

    @UIComponent(
            index = 50,
            type = ComponentType.int_input,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "min", value = "1"),
                    @UIComponent.UIConfig(name = "max", value = "100")
            }, restartOnChange = true
    )
    public int rtBufferSizeMb = 40;

    @UIComponent(index = 60,
            type = ComponentType.int_input,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "step", value = "0.01"),
                    @UIComponent.UIConfig(name = "min", value = "0"),
                    @UIComponent.UIConfig(name = "max", value = "30")
            }, restartOnChange = true
    )
    public double maxFps = 0;

}