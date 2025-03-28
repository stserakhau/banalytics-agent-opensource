package com.banalytics.box.module.events;

import com.banalytics.box.api.integration.AbstractMessage;
import com.banalytics.box.api.integration.webrtc.channel.NodeDescriptor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.text.StringSubstitutor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.banalytics.box.api.integration.utils.TimeUtil.currentTimeInServerTz;

@Getter
@Setter
@ToString(callSuper = true)
public abstract class AbstractEvent extends AbstractMessage {
    protected UUID messageUuid = UUID.randomUUID();
    protected UUID environmentUuid;
    protected NodeDescriptor.NodeType nodeType;
    protected UUID nodeUuid;
    protected LocalDateTime dateTime = currentTimeInServerTz();
    protected String nodeClassName;
    protected String nodeTitle;
    protected Map<String, Object> metaInfo = Map.of();

    /**
     * Bold	                * x *	    We'll see you at *4PM*.	txtFormatting_01_200px.png
     * Italic	                _ x _	    Your driver has been _delayed_ until 6PM.	txtFormatting_02_200px.png
     * Strike-through	        ~ x ~	    We expect to see you at ~4PM~ 6PM.	txtFormatting_03_200px.png
     * Pre-formatted (Code)	``` x ```	Use the ```Message``` API to notify users.
     */
    public String textView() {
        return StringSubstitutor.replace(
                "*${type}* ${nodeTitle}",
                Map.of(
                        "type", this.type,
                        "nodeTitle", this.nodeTitle
                )
        );
    }

    public AbstractEvent(String type) {
        super(type);
    }

    public AbstractEvent(String type, NodeDescriptor.NodeType nodeType, UUID nodeUuid, String nodeClassName, String nodeTitle) {
        super(type);
        this.nodeType = nodeType;
        this.nodeUuid = nodeUuid;
        this.nodeClassName = nodeClassName;
        this.nodeTitle = nodeTitle;
    }
}
