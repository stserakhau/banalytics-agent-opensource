package com.banalytics.box.module.usecase;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class EmailNewContentNotificationCaseConfiguration {

    @UIComponent(
            index = 10, type = ComponentType.multi_select,
            restartOnChange = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(bean = "taskService", method = "findEventProducersByEventType", params = {"com.banalytics.box.module.events.FileCreatedEvent"})
            }
    )
    public String fileCreatedEventSources;

    @UIComponent(index = 20, type = ComponentType.drop_down, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            }
    )
    public ConfigurationType configurationType;

    @UIComponent(
            index = 30, required = true, type = ComponentType.text_input,
            dependsOn = "configurationType",
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.configurationType === 'CREATE_NEW_MAIL_SERVER'")
            }
    )
    public String username;

    @UIComponent(
            index = 40, required = true, type = ComponentType.text_input,
            dependsOn = "configurationType",
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.configurationType === 'CREATE_NEW_MAIL_SERVER'")
            }
    )
    public String password;

    @UIComponent(
            index = 50, required = true, type = ComponentType.text_area,
            dependsOn = "configurationType",
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.configurationType === 'CREATE_NEW_MAIL_SERVER'"),
                    @UIComponent.UIConfig(name = "sort", value = "7")
            }
    )
    public String properties =
            "mail.smtp.host=smtp.gmail.com\n" +
                    "mail.smtp.port=465\n" +
                    "mail.smtp.auth=true\n" +
                    "mail.smtp.ssl.enable=true\n" +
                    "mail.smtp.starttls.enable=false\n" +
                    "mail.debug=true";

    @UIComponent(
            index = 60, required = true, type = ComponentType.drop_down,
            dependsOn = "configurationType",
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.configurationType === 'USE_EXISTED_MAIL_SERVER'"),
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(bean = "taskService", method = "findActiveByStandard", params = {"com.banalytics.box.module.email.EmailServerConnectorThing"})
            }
    )
    public UUID mailServerUUID;

    @UIComponent(
            index = 80, type = ComponentType.text_area, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "5")
            }
    )
    public String toEmails;

    @UIComponent(
            index = 90, type = ComponentType.text_area, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "5")
            }
    )
    public String subject;

    @UIComponent(
            index = 100, type = ComponentType.text_area, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "15")
            }
    )
    public String messageTemplate = "${index}: ${event}<br>${error}<hr>";

    public enum ConfigurationType {
        CREATE_NEW_MAIL_SERVER, USE_EXISTED_MAIL_SERVER
    }
}
