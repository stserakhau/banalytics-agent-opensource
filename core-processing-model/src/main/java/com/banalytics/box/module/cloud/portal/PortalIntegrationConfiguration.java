package com.banalytics.box.module.cloud.portal;

import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.module.AbstractConfiguration;
import com.banalytics.box.module.constants.RestartOnFailure;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import static com.banalytics.box.api.integration.form.ComponentType.*;

@Getter
@Setter
public class PortalIntegrationConfiguration extends AbstractConfiguration {
    public static UUID THING_UUID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    public PortalIntegrationConfiguration() {
        restartOnFailure = RestartOnFailure.RESTART_30_SECONDS;
    }

    @UIComponent(
            index = 10, type = drop_down, required = true, restartOnChange = true,
            backendConfig = {
                    @UIComponent.BackendConfig(values = {
                            "wss://router.banalytics.live"
                    })
            }
    )
    public String portalUrl = "wss://router.banalytics.live";


    @UIComponent(index = 30, type = text_input_readonly)
    public UUID environmentUUID;

    @UIComponent(index = 40, type = int_input, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "min", value = "10"),
                    @UIComponent.UIConfig(name = "max", value = "60")
            }, restartOnChange = true)
    public int pingTimeoutSeconds = 30;

    //    @UIComponent(index = 40, type = int_input, required = true,
//            uiConfig = {
//                    @UIComponent.UIConfig(name = "min", value = "1"),
//                    @UIComponent.UIConfig(name = "max", value = "20")
//            }, restartOnChange = true)
//    public int connectionExpirationTime = 5;

    @Override
    public UUID getUuid() {
        return THING_UUID;
    }
}
