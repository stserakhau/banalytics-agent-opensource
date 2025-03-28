package com.banalytics.box.module.usecase;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TelegramNewContentNotificationCaseConfiguration {

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
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.configurationType === 'CREATE_NEW_TELEGRAM_BOT'")
            }
    )
    public String botToken;

    @UIComponent(
            index = 40, required = true, type = ComponentType.text_input,
            dependsOn = "configurationType",
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.configurationType === 'CREATE_NEW_TELEGRAM_BOT'")
            }
    )
    public String pinCode;


    @UIComponent(
            index = 50, required = true, type = ComponentType.drop_down,
            dependsOn = "configurationType",
            uiConfig = {
                    @UIComponent.UIConfig(name = "visibleCondition", value = "form.configurationType === 'USE_EXISTED_TELEGRAM_BOT'"),
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(bean = "taskService", method = "findActiveByStandard", params = {"com.banalytics.box.module.telegram.TelegramBotThing"})
            }
    )
    public UUID telegramBotUUID;

    @UIComponent(
            index = 60, type = ComponentType.multi_select,
            uiConfig = {
                    @UIComponent.UIConfig(name = "enableCondition", value = "form.telegramBotUUID !== ''"),
                    @UIComponent.UIConfig(name = "sort", value = "asc"),
                    @UIComponent.UIConfig(name = "api-uuid", value = "telegramBotUUID"),
                    @UIComponent.UIConfig(name = "api-method", value = "readAccounts")
            },
            dependsOn = {"telegramBotUUID"}
    )
    public String forwardToAccounts;

    public enum ConfigurationType {
        CREATE_NEW_TELEGRAM_BOT, USE_EXISTED_TELEGRAM_BOT
    }
}
