package com.banalytics.box.module.events;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.api.integration.webrtc.channel.NodeDescriptor;
import com.banalytics.box.api.integration.webrtc.channel.NodeState;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.text.StringSubstitutor;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@ToString(callSuper = true)
public class StatusEvent extends AbstractEvent {
    @UIComponent(
            index = 0, type = ComponentType.multi_select, required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            }
    )
    public NodeState state;

    String message;

    @Override
    public String textView() {
        return StringSubstitutor.replace(
                "*${type}*: *${state}: ${nodeTitle}* ${nodeClass} ```${message}```",
                Map.of(
                        "type", this.type,
                        "nodeType", this.nodeType,
                        "nodeTitle", this.nodeTitle,
                        "nodeClass", this.nodeClassName,
                        "state", this.state,
                        "message", this.message
                )
        );
    }

    public StatusEvent() {
        super("EVT_STATUS");
    }

    public StatusEvent(NodeDescriptor.NodeType nodeType, UUID nodeUuid, String nodeClassName, String nodeTitle, NodeState state, String message) {
        super("EVT_STATUS", nodeType, nodeUuid, nodeClassName, nodeTitle);
        this.state = state;
        this.message = message;
    }
}
