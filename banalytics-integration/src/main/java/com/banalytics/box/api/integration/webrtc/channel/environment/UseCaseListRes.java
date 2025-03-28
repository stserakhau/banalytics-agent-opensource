package com.banalytics.box.api.integration.webrtc.channel.environment;

import com.banalytics.box.api.integration.MessageType;
import com.banalytics.box.api.integration.webrtc.channel.AbstractChannelMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString(callSuper = true)
public class UseCaseListRes extends AbstractChannelMessage {
    Map<String, String> useCaseGroupMap;

    public UseCaseListRes() {
        super(MessageType.UC_LIST_RES);
    }
}
