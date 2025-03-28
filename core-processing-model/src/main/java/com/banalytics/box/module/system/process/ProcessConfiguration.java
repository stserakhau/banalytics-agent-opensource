package com.banalytics.box.module.system.process;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.module.AbstractConfiguration;
import com.banalytics.box.module.constants.RestartOnFailure;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessConfiguration extends AbstractConfiguration {
    public ProcessConfiguration() {
        restartOnFailure = RestartOnFailure.STOP_ON_FAILURE;
        autostart = false;
    }

    @UIComponent(
            index = 10,
            type = ComponentType.text_input,
            required = true
    )
    public String title;

    @UIComponent(
            index = 20, type = ComponentType.text_area, required = true,
            restartOnChange = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "10"),
            }
    )
    public String commandLine = "cmd /C chcp 65001 & cmd";

    @UIComponent(
            index = 30,
            type = ComponentType.int_input,
            required = true,
            restartOnChange = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "min", value = "0")
            }
    )
    public int waitTimeoutSec;

    @UIComponent(
            index = 40, type = ComponentType.int_input, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "min", value = "10"),
                    @UIComponent.UIConfig(name = "max", value = "10000")
            }
    )
    public int historyLines = 100;
}
