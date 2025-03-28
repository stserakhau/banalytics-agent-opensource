package com.banalytics.box.api.integration.webrtc.channel.environment;

import com.banalytics.box.api.integration.MessageType;
import com.banalytics.box.api.integration.webrtc.channel.AbstractChannelMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString(callSuper = true)
public class UseCaseCreateReq extends AbstractChannelMessage {
    private String useCaseClass;
    private Map<String, Object> configuration;

    public UseCaseCreateReq() {
        super(MessageType.UC_CRT_REQ);
    }
}
