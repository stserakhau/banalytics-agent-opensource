package com.banalytics.box.module.webrtc.client.channel;

import com.banalytics.box.api.integration.model.Share;
import com.banalytics.box.api.integration.model.SharePermission;
import com.banalytics.box.api.integration.webrtc.channel.AbstractChannelMessage;
import com.banalytics.box.api.integration.webrtc.channel.ChannelMessage;
import com.banalytics.box.api.integration.webrtc.channel.NodeDescriptor;
import com.banalytics.box.api.integration.webrtc.channel.NodeState;
import com.banalytics.box.api.integration.webrtc.channel.environment.IssuesReq;
import com.banalytics.box.api.integration.webrtc.channel.environment.SubTasksRes;
import com.banalytics.box.module.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.banalytics.box.module.Thing.PERMISSION_READ;
import static com.banalytics.box.module.utils.Utils.nodeType;
import static com.banalytics.box.module.webrtc.client.channel.Constants.ALWAYS_REQUIRED_THINGS_UUID_SET;

public class IssuesReqHandler implements ChannelRequestHandler {

    final BoxEngine engine;
    final Share share;

    public IssuesReqHandler(BoxEngine engine, Share share) {
        this.engine = engine;
        this.share = share;
    }

    @Override
    public ChannelMessage handle(ChannelMessage req) throws Exception {
        if (req instanceof IssuesReq ir) {
            SubTasksRes res = new SubTasksRes();
            res.setRequestId(req.getRequestId());

            List<NodeDescriptor> result = new ArrayList<>();

            Instance instance = engine.getPrimaryInstance();
            collectErrors(instance.getSubTasks(), result);
            res.setSubTasks(result);
            return res;
        }
        return null;
    }

    private void collectErrors(List<AbstractTask<?>> subTasks, List<NodeDescriptor> result) {
        Map<UUID, SharePermission> clientPermissions = share != null ? share.getSharePermissions() : null;
        boolean isMy = clientPermissions == null;

        for (ITask<?> t : subTasks) {
            if (share != null && !share.isSuperUser()) {
                boolean shareAllowed = false;
                if (!isMy) {
                    UUID sourceThingUuid = t.getSourceThingUuid();
                    if (sourceThingUuid != null) {
                        SharePermission perm = clientPermissions.get(sourceThingUuid);
                        shareAllowed = perm != null && perm.generalPermissions.contains(PERMISSION_READ);
                    }
                }
                boolean allowed = isMy || shareAllowed;

                if (!allowed) {
                    continue;
                }
            }
            if (t.getState() == State.ERROR || t.getState() == State.INIT_ERROR) {
                NodeDescriptor nd = new NodeDescriptor(
                        t.getUuid(),
                        t.getSelfClassName(),
                        t.getTitle(),
                        t.getUuid(),
                        nodeType(t.getClass()),
                        false,
                        t instanceof AbstractListOfTask sts && !sts.getSubTasks().isEmpty(),
                        false,
                        NodeState.valueOf(t.getState().name()),
                        t.getStateDescription(),
                        false,
                        t instanceof Singleton
                );
                nd.setRemovable(!ALWAYS_REQUIRED_THINGS_UUID_SET.contains(t.getUuid()));
                result.add(nd);

                if (t instanceof AbstractListOfTask<?> sts) {
                    collectErrors(sts.getSubTasks(), result);
                }
            }
        }
    }
}
