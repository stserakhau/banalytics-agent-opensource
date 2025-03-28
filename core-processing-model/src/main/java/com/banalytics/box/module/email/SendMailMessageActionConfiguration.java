package com.banalytics.box.module.email;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.module.AbstractConfiguration;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SendMailMessageActionConfiguration extends AbstractConfiguration {
    @UIComponent(index = 10, type = ComponentType.text_input, required = true)
    public String title;

    @UIComponent(
            index = 20,
            type = ComponentType.drop_down,
            required = true, restartOnChange = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(bean = "taskService", method = "findActiveByStandard", params = {"com.banalytics.box.module.email.EmailServerConnectorThing"})
            }
    )
    public UUID emailServerConnectorThingUuid;

    @UIComponent(
            index = 30, type = ComponentType.text_area, required = false,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "5")
            }
    )
    public String fromEmails;

    @UIComponent(
            index = 40, type = ComponentType.text_area, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "5")
            }
    )
    public String toEmails;

    @UIComponent(
            index = 50, type = ComponentType.text_area,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "5")
            }
    )
    public String ccEmails;

    @UIComponent(
            index = 60, type = ComponentType.text_area,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "5")
            }
    )
    public String bccEmails;

    @UIComponent(
            index = 70, type = ComponentType.text_area, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "5")
            }
    )
    public String subject;

    @UIComponent(
            index = 80, type = ComponentType.text_area, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "15")
            }
    )
    public String messageTemplate = "${index}: ${event}<br>${error}<hr>";

    @UIComponent(
            index = 90, type = ComponentType.int_input, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "min", value = "1"),
                    @UIComponent.UIConfig(name = "max", value = "100")
            }
    )
    public int messageBatchSize = 1;

    @UIComponent(
            index = 100, type = ComponentType.int_input, required = true,
            dependsOn = "messageBatchSize",
            uiConfig = {
                    @UIComponent.UIConfig(name = "enableCondition", value = "form.messageBatchSize > 1 "),
                    @UIComponent.UIConfig(name = "min", value = "1"),
                    @UIComponent.UIConfig(name = "max", value = "60")
            }
    )
    public int messageBatchTimeoutSec = 1;
}
