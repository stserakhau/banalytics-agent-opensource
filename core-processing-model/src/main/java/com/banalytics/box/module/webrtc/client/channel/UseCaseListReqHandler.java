package com.banalytics.box.module.webrtc.client.channel;

import com.banalytics.box.api.integration.webrtc.channel.ChannelMessage;
import com.banalytics.box.api.integration.webrtc.channel.environment.UseCaseListReq;
import com.banalytics.box.api.integration.webrtc.channel.environment.UseCaseListRes;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.usecase.AbstractUseCase;
import com.banalytics.box.module.usecase.UseCase;
import com.banalytics.box.module.utils.DataHolder;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class UseCaseListReqHandler implements ChannelRequestHandler {
    private final BoxEngine engine;

    @Override
    public ChannelMessage handle(ChannelMessage request) throws Exception {
        if (request instanceof UseCaseListReq req) {
            UseCaseListRes res = new UseCaseListRes();

            res.setRequestId(req.getRequestId());


            Map<String, String> useCaseGroupMap = new HashMap<>();

            for (Class<? extends AbstractUseCase> useCase : DataHolder.useCases()) {
                String key = useCase.getName();

                AbstractUseCase uc = UseCase.blankOf(useCase, engine);
                String group = uc.groupCode();

                useCaseGroupMap.put(key, group);
            }

            res.setUseCaseGroupMap(useCaseGroupMap);


            return res;
        }
        return null;
    }
}
