package com.banalytics.box.module.telegram;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.api.integration.form.annotation.UIDoc;
import com.banalytics.box.module.AbstractConfiguration;
import com.banalytics.box.module.ITitle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@UIDoc(href = "https://www.banalytics.live/knowledge-base/components/telegram-bot-for-receiving-videos-and-camera-snapshots")
public class TelegramBotConfiguration extends AbstractConfiguration implements ITitle {

    @UIComponent(index = 10, type = ComponentType.text_input, required = true)
    public String title;

    @UIComponent(index = 20, type = ComponentType.int_input, restartOnChange = true, uiConfig = {
            @UIComponent.UIConfig(name = "min", value = "500"),
            @UIComponent.UIConfig(name = "max", value = "10000")
    })
    public int checkMessagesTimeoutMillis = 3000;

    @UIComponent(index = 30, type = ComponentType.password_input, restartOnChange = true)
    public String botToken;

    @UIComponent(index = 40, type = ComponentType.password_input, required = true)
    public String pinCode = "";
}
