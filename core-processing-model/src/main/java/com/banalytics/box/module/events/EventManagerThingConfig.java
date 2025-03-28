package com.banalytics.box.module.events;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.api.integration.form.annotation.UIDoc;
import com.banalytics.box.module.AbstractConfiguration;
import com.banalytics.box.module.ITitle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@UIDoc(href = "https://www.banalytics.live/knowledge-base/components/event-manager")
public class EventManagerThingConfig extends AbstractConfiguration implements ITitle {
    @UIComponent(index = 10, type = ComponentType.text_input, required = true)
    public String title;
}
