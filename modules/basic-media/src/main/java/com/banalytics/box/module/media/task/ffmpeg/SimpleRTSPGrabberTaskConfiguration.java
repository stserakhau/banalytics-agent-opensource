package com.banalytics.box.module.media.task.ffmpeg;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.api.integration.form.annotation.UIDoc;
import com.banalytics.box.module.AbstractConfiguration;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@UIDoc(href = "https://www.banalytics.live/knowledge-base/tasks/network-capture-video-of-a-network-camera")
public class SimpleRTSPGrabberTaskConfiguration extends AbstractConfiguration {

    @UIComponent(
            index = 10,
            type = ComponentType.drop_down,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(bean = "taskService", method = "findActiveByStandard", params = {"com.banalytics.box.module.standard.UrlMediaStream"})
            },
            restartOnChange = true
    )
    public UUID urlUuid;

    @UIComponent(index = 15,
            type = ComponentType.checkbox,
            required = true,
            restartOnChange = true
    )
    public boolean disableAudioRecording = true;

    @UIComponent(
            index = 20,
            type = ComponentType.int_input,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "min", value = "1"),
                    @UIComponent.UIConfig(name = "max", value = "100")
            }, restartOnChange = true
    )
    public int rtBufferSizeMb = 40;

    @UIComponent(index = 30,
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
