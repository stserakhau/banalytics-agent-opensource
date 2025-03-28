package com.banalytics.box.module.events;

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
public class SoundEvent extends AbstractEvent {
    public UUID sourceThingUuid;
    public boolean debug;
    public double[] magnitude;
    public double[] magnitudeAverage;

    public SoundEvent() {
        super("EVT_SOUND");
    }

    public SoundEvent(NodeDescriptor.NodeType nodeType, UUID nodeUuid, String nodeClassName, String nodeTitle,
                      UUID sourceThingUuid, boolean debug, double[] magnitude, double[] magnitudeAverage) {
        super("EVT_SOUND", nodeType, nodeUuid, nodeClassName, nodeTitle);
        this.sourceThingUuid = sourceThingUuid;
        this.debug = debug;
        this.magnitude = magnitude;
        this.magnitudeAverage = magnitudeAverage;
    }

    public String textView() {
        return StringSubstitutor.replace(
                """
                        *${type}* ${nodeTitle}
                        mag: ${mag}
                        avg mag: ${avgMag}
                        """,
                Map.of(
                        "type", this.type,
                        "nodeTitle", this.nodeTitle,
                        "mag", Arrays.toString(this.magnitude),
                        "avgMag", Arrays.toString(this.magnitudeAverage)
                )
        );
    }
}
