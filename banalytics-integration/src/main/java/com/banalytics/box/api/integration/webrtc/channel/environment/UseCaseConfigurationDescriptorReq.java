package com.banalytics.box.api.integration.webrtc.channel.environment;

import com.banalytics.box.api.integration.MessageType;
import com.banalytics.box.api.integration.webrtc.channel.AbstractChannelMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class UseCaseConfigurationDescriptorReq extends AbstractChannelMessage {
    private String className;

    public UseCaseConfigurationDescriptorReq() {
        super(MessageType.TSK_CNF_DSCR_REQ);
    }
}
