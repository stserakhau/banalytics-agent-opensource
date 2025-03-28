package com.banalytics.box.module.system.cmd;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.module.AbstractConfiguration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CmdActionConfiguration extends AbstractConfiguration {
    @UIComponent(
            index = 10,
            type = ComponentType.text_input,
            required = true
    )
    public String title;

    @UIComponent(
            index = 20,
            type = ComponentType.text_area,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "10"),
            }
    )
    public String commandLine;

    @UIComponent(
            index = 30,
            type = ComponentType.int_input,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "min", value = "1"),
                    @UIComponent.UIConfig(name = "max", value = "300")
            }
    )
    public int waitTimeoutSec = 10;
}
