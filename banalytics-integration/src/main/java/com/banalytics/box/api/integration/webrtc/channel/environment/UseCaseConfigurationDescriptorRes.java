package com.banalytics.box.api.integration.webrtc.channel.environment;

import com.banalytics.box.api.integration.MessageType;
import com.banalytics.box.api.integration.form.FormModel;
import com.banalytics.box.api.integration.webrtc.channel.AbstractChannelMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class UseCaseConfigurationDescriptorRes extends AbstractChannelMessage {
    Object configuration;
    FormModel formModel;

    public UseCaseConfigurationDescriptorRes() {
        super(MessageType.TSK_CNF_DSCR_RES);
    }
}
