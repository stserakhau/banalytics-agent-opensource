package com.banalytics.box.module.email;

import com.banalytics.box.api.integration.webrtc.channel.NodeState;
import com.banalytics.box.module.*;
import com.banalytics.box.module.events.AbstractEvent;
import com.banalytics.box.module.events.FileCreatedEvent;
import com.banalytics.box.module.events.StatusEvent;
import com.banalytics.box.module.storage.filestorage.FileStorageThing;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.util.*;

import static com.banalytics.box.module.utils.Utils.nodeType;
import static com.banalytics.box.service.SystemThreadsService.SYSTEM_TIMER;

@Slf4j
public class SendMailMessageAction extends AbstractAction<SendMailMessageActionConfiguration> {
    public SendMailMessageAction(BoxEngine engine, AbstractListOfTask<?> parent) {
        super(engine, parent);
    }

    private EmailServerConnectorThing emailServerConnectorThing;


    @Override
    public Object uniqueness() {
        return configuration.emailServerConnectorThingUuid + "/" + configuration.title;
    }

    @Override
    protected boolean isFireActionEvent() {
        return true;
    }

    private long eventsBatchTimeout = 0;
    private List<AbstractEvent> eventsBatch = new ArrayList<>();

    @Override
    protected synchronized boolean doProcess(ExecutionContext executionContext) throws Exception {
        if (emailServerConnectorThing == null) {
            onException(new Exception("error.mailServer.removed"));
        }
        AbstractEvent evt = executionContext.getVar(AbstractEvent.class);
        eventsBatch.add(evt);

        if (eventsBatch.size() >= configuration.messageBatchSize) {
            flushBatch();
        }

        return false;
    }

    private volatile boolean flushing;

    private void flushBatch() {
        if(this.eventsBatch.isEmpty()) {
            return;
        }
        if (flushing) {
            log.info("Flushing in progress");
            return;
        }
        try {
            flushing = true;
            List<AbstractEvent> eventsBatch = this.eventsBatch;
            this.eventsBatch = new ArrayList<>(configuration.messageBatchSize);
            log.info("Flushing {} events", eventsBatch.size());
            StringBuilder msg = new StringBuilder(10000);
            List<File> attachments = new ArrayList<>();
            for (int i = 0; i < eventsBatch.size(); i++) {
                AbstractEvent evt = eventsBatch.get(i);
                String errorMsg = "";
                try {
                    if (evt instanceof FileCreatedEvent fce) {
                        FileStorageThing fileStorageThing = engine.getThing(fce.getStorageUuid());
                        String contextPath = fce.getContextPath();
                        attachments.add(fileStorageThing.file(contextPath));
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    errorMsg = e.getMessage();
                }

                String message = StringSubstitutor.replace(
                        configuration.messageTemplate, Map.of(
                                "index", i,
                                "event", evt.textView(),
                                "error", errorMsg
                        ));
                msg.append(message);
            }

            emailServerConnectorThing.sendMessage(
                    configuration.fromEmails,
                    configuration.toEmails,
                    configuration.ccEmails,
                    configuration.bccEmails,
                    configuration.subject,
                    msg.toString(),
                    attachments.toArray(new File[0])
            );
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        } finally {
            long now = System.currentTimeMillis();
            eventsBatchTimeout = now + configuration.messageBatchTimeoutSec * 1000L;
            log.info("Flushing done");
            flushing = false;
        }
    }

    @Override
    public String doAction(ExecutionContext ctx) throws Exception {
        if (ctx.getVar(IAction.MANUAL_RUN) != null) {
            ctx.setVar(AbstractEvent.class, new StatusEvent(
                    nodeType(this.getClass()),
                    this.getUuid(),
                    getSelfClassName(),
                    getTitle(),
                    NodeState.valueOf(getState().name()),
                    "Manual"
            ));
        } else if (ctx.getVar(IAction.MANUAL_RUN) != null) {
            ctx.setVar(AbstractEvent.class, new StatusEvent(
                    nodeType(this.getClass()),
                    this.getUuid(),
                    getSelfClassName(),
                    getTitle(),
                    NodeState.valueOf(getState().name()),
                    "Scheduled"
            ));
        }
        this.process(ctx);

        return null;
    }

    @Override
    public String getTitle() {
        return configuration.title;
    }

    @Override
    public UUID getSourceThingUuid() {
        if (emailServerConnectorThing == null) {
            return null;
        }
        return emailServerConnectorThing.getUuid();
    }

    @Override
    public void doInit() throws Exception {
        emailServerConnectorThing = engine.getThingAndSubscribe(configuration.emailServerConnectorThingUuid, this);
    }

    private TimerTask flushBatchByTimeout;

    @Override
    public void doStart(boolean ignoreAutostartProperty, boolean startChildren) throws Exception {
        if (this.configuration.messageBatchSize > 1) {
            flushBatchByTimeout = new TimerTask() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    if (now > eventsBatchTimeout) {
                        flushBatch();
                    }
                }
            };
            SYSTEM_TIMER.schedule(flushBatchByTimeout, 5000, configuration.messageBatchTimeoutSec * 1000L / 4);
        }
    }

    @Override
    public void doStop() throws Exception {
        if (flushBatchByTimeout != null) {
            flushBatchByTimeout.cancel();
            flushBatchByTimeout = null;
        }
    }

    @Override
    public void destroy() {
        if (emailServerConnectorThing != null) {
            ((Thing<?>) emailServerConnectorThing).unSubscribe(this);
            emailServerConnectorThing = null;
        }
    }
}
