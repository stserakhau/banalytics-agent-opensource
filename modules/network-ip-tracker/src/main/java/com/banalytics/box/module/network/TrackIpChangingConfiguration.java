package com.banalytics.box.module.network;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.api.integration.form.annotation.UIDoc;
import com.banalytics.box.module.AbstractConfiguration;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
@UIDoc(href = "https://www.banalytics.live/knowledge-base/components/device-ip-tracker")
public class TrackIpChangingConfiguration extends AbstractConfiguration {
    @UIComponent(
            index = 10,
            type = ComponentType.drop_down,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(bean = "deviceDiscoveryService", method = "availableSubnetsForSelect")
            },
            restartOnChange = true
    )
    public String networkInterface;

    @UIComponent(
            index = 20,
            type = ComponentType.int_input,
            uiConfig = {
                    @UIComponent.UIConfig(name = "min", value = "30"),
            },
            required = true,
            restartOnChange = true
    )
    public int scanTimeSec = 60;

    @UIComponent(
            index = 30,
            type = ComponentType.int_input,
            uiConfig = {
                    @UIComponent.UIConfig(name = "min", value = "1"),
                    @UIComponent.UIConfig(name = "max", value = "10"),
            },
            required = true,
            restartOnChange = true
    )
    public int hostReachableTimeoutSec = 3;

}
