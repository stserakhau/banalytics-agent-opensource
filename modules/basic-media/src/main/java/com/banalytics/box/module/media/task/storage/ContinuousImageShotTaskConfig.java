package com.banalytics.box.module.media.task.storage;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.module.AbstractConfiguration;
import com.banalytics.box.module.constants.TimestampFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import static com.banalytics.box.api.integration.form.ComponentType.int_input;

@Getter
@Setter
public class ContinuousImageShotTaskConfig extends AbstractConfiguration {

    @UIComponent(
            index = 20,
            type = ComponentType.drop_down,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(bean = "taskService", method = "findActiveByStandard", params = {"com.banalytics.box.module.standard.FileStorage"})//<< todo security
            },
            restartOnChange = true
    )
    public UUID storageUuid;

    @UIComponent(index = 30, type = ComponentType.drop_down, required = true, restartOnChange = true)
    public TimestampFormat pathPattern = TimestampFormat.yyyyMMdd_hh_mmss;

    @UIComponent(
            index = 40,
            type = int_input,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "min", value = "50"),
                    @UIComponent.UIConfig(name = "max", value = "3600000")
            }
    )
    public int photoIntervalMillis = 3000;

    @UIComponent(
            index = 60,
            type = int_input,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "step", value = "0.05"),
                    @UIComponent.UIConfig(name = "min", value = "0.1"),
                    @UIComponent.UIConfig(name = "max", value = "1")
            }
    )
    public float compressionRate = 0.75f;
}
