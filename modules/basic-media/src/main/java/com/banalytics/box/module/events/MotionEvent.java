package com.banalytics.box.module.events;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.api.integration.webrtc.channel.NodeDescriptor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.text.StringSubstitutor;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@ToString(callSuper = true)
public class MotionEvent extends AbstractEvent {
    public UUID sourceThingUuid;

    @UIComponent(
            index = 0, type = ComponentType.multi_select,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc"),
                    @UIComponent.UIConfig(name = "show-empty", value = "false")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(
                            bean = "taskService",
                            method = "listPossibleConfigValues",
                            params = {
                                    "triggerAreas"
                            }
                    )
            }
    )
    public String[] zones;

    @UIComponent(
            index = 0, type = ComponentType.multi_select,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc"),
                    @UIComponent.UIConfig(name = "show-empty", value = "false")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(
                            bean = "taskService",
                            method = "listPossibleConfigValues",
                            params = {
                                    "classes"
                            }
                    )
            }
    )
    public String[] classes;

    public double relativeOverlapArea;

    @Override
    public String textView() {
        return super.textView() + '\n' + StringSubstitutor.replace(
                """
                        Motion detected: ${source}
                        *Areas:* ${area}
                        *Classes:* ${classes}
                        """,
                Map.of(
                        "source", this.getNodeTitle(),
                        "area", Arrays.toString(this.zones),
                        "classes", Arrays.toString(classes)
                )
        );
    }

    public MotionEvent() {
        super("EVT_MOTION");
    }

    public MotionEvent(NodeDescriptor.NodeType nodeType, UUID nodeUuid, String nodeClassName, String nodeTitle, UUID sourceThingUuid, String[] zones, String[] classes, double relativeOverlapArea) {
        super("EVT_MOTION", nodeType, nodeUuid, nodeClassName, nodeTitle);
        this.sourceThingUuid = sourceThingUuid;
        this.zones = zones;
        this.classes = classes;
        this.relativeOverlapArea = relativeOverlapArea;
    }
}
