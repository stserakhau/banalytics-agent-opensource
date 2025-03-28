package com.banalytics.box.module.webrtc.client.channel;

import com.banalytics.box.api.integration.MessageType;
import com.banalytics.box.api.integration.webrtc.channel.AbstractChannelMessage;
import com.banalytics.box.api.integration.webrtc.channel.ChannelMessage;
import com.banalytics.box.api.integration.webrtc.channel.environment.EmptyRes;
import com.banalytics.box.api.integration.webrtc.channel.environment.UseCaseCreateReq;
import com.banalytics.box.api.integration.webrtc.channel.environment.UseCaseListReq;
import com.banalytics.box.api.integration.webrtc.channel.environment.UseCaseListRes;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.utils.DataHolder;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class UseCaseCreateReqHandler implements ChannelRequestHandler {
    private final BoxEngine engine;

    @Override
    public ChannelMessage handle(ChannelMessage request) throws Exception {
        if (request instanceof UseCaseCreateReq req) {
            String useCaseClass = req.getUseCaseClass();
            Map<String, Object> configuration = req.getConfiguration();

            engine.buildUseCase(useCaseClass, configuration);
            EmptyRes res = new EmptyRes();
            res.setRequestId(req.getRequestId());
            return res;
        }
        return null;
    }
}
