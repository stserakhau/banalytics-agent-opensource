package com.banalytics.box.module.events;

import com.banalytics.box.api.integration.webrtc.channel.NodeDescriptor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@ToString(callSuper = true)
public class FileCreatedEvent extends AbstractEvent {
    UUID storageUuid;
    String contextPath;
    Map<String, Object> options = new HashMap<>(4);

    public FileCreatedEvent() {
        super("EVT_FILE_CRT");
    }

    public FileCreatedEvent(NodeDescriptor.NodeType nodeType, UUID nodeUuid, String nodeClassName, String nodeTitle, UUID storageUuid, String contextPath) {
        super("EVT_FILE_CRT", nodeType, nodeUuid, nodeClassName, nodeTitle);
        this.storageUuid = storageUuid;
        this.contextPath = contextPath;
    }

    public String textView() {
        return StringSubstitutor.replace(
                """
                        *${type}* ${nodeTitle}: ${storageUuid}
                        Path: ${contextPath}
                        Options: ${opts}
                        """,
                Map.of(
                        "type", this.type,
                        "nodeTitle", this.nodeTitle,
                        "storageUuid", this.storageUuid,
                        "contextPath", this.contextPath,
                        "opts", this.options
                )
        );
    }

    public void option(String name, Object value) {
        options.put(name, value);
    }
    public <T> T option(String name) {
        return (T) options.get(name);
    }
}
