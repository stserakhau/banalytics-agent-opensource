package com.banalytics.box.module.webrtc.client.channel;

import com.banalytics.box.api.integration.form.FormModel;
import com.banalytics.box.api.integration.webrtc.channel.ChannelMessage;
import com.banalytics.box.api.integration.webrtc.channel.environment.TaskConfigurationDescriptorReq;
import com.banalytics.box.api.integration.webrtc.channel.environment.ThingConfigurationDescriptorRes;
import com.banalytics.box.api.integration.webrtc.channel.environment.UseCaseConfigurationDescriptorReq;
import com.banalytics.box.api.integration.webrtc.channel.environment.UseCaseConfigurationDescriptorRes;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.ITask;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class UseCaseConfigurationDescriptorReqHandler implements ChannelRequestHandler {
    private final BoxEngine engine;

    @Override
    public ChannelMessage handle(ChannelMessage request) throws Exception {
        if (request instanceof UseCaseConfigurationDescriptorReq req) {
            String className = req.getClassName();
            UseCaseConfigurationDescriptorRes res = new UseCaseConfigurationDescriptorRes();

            FormModel formModel = engine.describeClass(className);
            res.setFormModel(formModel);
            res.setRequestId(req.getRequestId());
            return res;
        }
        return null;
    }
}
