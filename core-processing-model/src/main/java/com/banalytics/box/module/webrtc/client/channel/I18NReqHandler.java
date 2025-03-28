package com.banalytics.box.module.webrtc.client.channel;

import com.banalytics.box.api.integration.webrtc.channel.ChannelMessage;
import com.banalytics.box.api.integration.webrtc.channel.environment.I18NReq;
import com.banalytics.box.api.integration.webrtc.channel.environment.I18NRes;
import com.banalytics.box.module.AbstractListOfTask;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.ITask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class I18NReqHandler implements ChannelRequestHandler {
    final BoxEngine engine;

    @Override
    public ChannelMessage handle(ChannelMessage request) throws Exception {
        if (request instanceof I18NReq req) {
            I18NRes res = new I18NRes();
            res.setRequestId(req.getRequestId());

            {
                Map<UUID, Map<String, String>> titlesMap = new HashMap<>(50);
                engine.findThings().forEach(t -> {
                    titlesMap.put(
                            t.getUuid(),
                            Map.of(
                                    "className", t.getSelfClassName(),
                                    "title", t.getTitle()
                            )
                    );
                });

                engine.instances().forEach(i -> {
                    processTasksTree(i, titlesMap);
                });
                res.setUuidClassTitleMap(titlesMap);
            }

            res.setI18n(engine.i18n());

            return res;
        }
        return null;
    }

    private void processTasksTree(ITask<?> task, Map<UUID, Map<String, String>> titlesMap) {
        titlesMap.put(task.getUuid(), Map.of(
                "className", task.getSelfClassName(),
                "title", task.getTitle()
        ));

        if (task instanceof AbstractListOfTask<?> parent) {
            parent.getSubTasks().forEach(t -> {
                processTasksTree(t, titlesMap);
            });
        }
    }
}
