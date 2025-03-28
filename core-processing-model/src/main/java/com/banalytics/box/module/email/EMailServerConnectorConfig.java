package com.banalytics.box.module.email;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.module.AbstractConfiguration;
import com.banalytics.box.module.ITitle;
import com.banalytics.box.module.constants.CleanupInterval;
import com.banalytics.box.module.storage.filestorage.AccessType;
import com.banalytics.box.module.storage.filestorage.LimitType;
import com.banalytics.box.module.storage.filesystem.ServerLocalFileSystemNavigatorConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import static com.banalytics.box.api.integration.form.ComponentType.*;

@Getter
@Setter
public class EMailServerConnectorConfig extends AbstractConfiguration implements ITitle {
    @UIComponent(index = 10, type = ComponentType.text_input, required = true)
    public String title;

    @UIComponent(index = 20, type = text_input, required = true)
    public String username;

    @UIComponent(index = 30, type = password_input, required = true)
    public String password;

    @UIComponent(index = 40, type = text_area, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "rows", value = "10"),
            })
    public String properties =
            "mail.smtp.host=smtp.gmail.com\n" +
                    "mail.smtp.port=465\n" +
                    "mail.smtp.auth=true\n" +
                    "mail.smtp.ssl.enable=true\n" +
                    "mail.smtp.starttls.enable=false\n" +
                    "mail.debug=true";
}