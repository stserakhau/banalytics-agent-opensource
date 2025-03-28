package com.banalytics.box.module.usecase;

import com.banalytics.box.api.integration.utils.CommonUtils;
import com.banalytics.box.module.AbstractAction;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.Instance;
import com.banalytics.box.module.Thing;
import com.banalytics.box.module.email.EmailServerConnectorThing;
import com.banalytics.box.module.email.SendMailMessageAction;
import com.banalytics.box.module.events.EventManagerThing;
import com.banalytics.box.module.events.FileCreatedEvent;

import java.time.LocalDateTime;
import java.util.*;

import static com.banalytics.box.api.integration.webrtc.channel.environment.ThingApiCallReq.PARAM_METHOD;
import static com.banalytics.box.module.ConverterTypes.TYPE_SET_STRING;
import static com.banalytics.box.module.events.EventManagerThing.*;
import static com.cronutils.utils.StringUtils.isEmpty;

public class EmailNewContentNotificationCase extends AbstractUseCase<EmailNewContentNotificationCaseConfiguration> {
    Instance instance;

    public EmailNewContentNotificationCase(BoxEngine engine) {
        super(engine);
    }

    @Override
    public void create() throws Exception {
        instance = engine.getPrimaryInstance();

        EmailServerConnectorThing emailServerConnectorThing = null;

        switch (configuration.configurationType) {// get bot
            case CREATE_NEW_MAIL_SERVER -> {
                emailServerConnectorThing = new EmailServerConnectorThing(engine);
                emailServerConnectorThing.configuration.title = "Mail server - " + LocalDateTime.now();
                emailServerConnectorThing.configuration.username = configuration.username;
                emailServerConnectorThing.configuration.password = configuration.password;
                emailServerConnectorThing.configuration.properties = configuration.properties;

                emailServerConnectorThing = engine.saveOrUpdateThing(emailServerConnectorThing, true, null, true);
            }
            case USE_EXISTED_MAIL_SERVER -> {
                emailServerConnectorThing = findLocalMediaThing(configuration.mailServerUUID);
            }
        }

        if (emailServerConnectorThing == null) {
            throw new RuntimeException("Mail server was removed by another user");
        }
        {//get forward action
            SendMailMessageAction sendMailMessageAction = findSendMailMessageAction(
                    emailServerConnectorThing.getUuid(),
                    configuration.toEmails
            );
            if (sendMailMessageAction == null) {
                sendMailMessageAction = new SendMailMessageAction(engine, instance);
                sendMailMessageAction.configuration.title = "Send mail: " + configuration.toEmails;
                sendMailMessageAction.configuration.emailServerConnectorThingUuid = emailServerConnectorThing.getUuid();
                sendMailMessageAction.configuration.toEmails = configuration.toEmails;
                sendMailMessageAction.configuration.subject = configuration.subject;
                sendMailMessageAction.configuration.messageTemplate = configuration.messageTemplate;
                sendMailMessageAction.configuration.messageBatchSize = 10;
                sendMailMessageAction.configuration.messageBatchTimeoutSec = 2;
                sendMailMessageAction = engine.saveOrUpdateTask(sendMailMessageAction, true, null, true, instance);
            }

            {// get create event manager
                EventManagerThing emt = new EventManagerThing(engine);
                emt.configuration.title = "Mail server: new content notifications - " + LocalDateTime.now();
                emt = engine.saveOrUpdateThing(emt, true, null, true);
                Set<String> fileCreatedEventSources = CommonUtils.DEFAULT_OBJECT_MAPPER.readValue(configuration.fileCreatedEventSources, TYPE_SET_STRING);
                createAction(emt, "On file created", fileCreatedEventSources, sendMailMessageAction);
            }
        }
    }

    @Override
    public String groupCode() {
        return "MESSAGE_DELIVERY";
    }

    private void createAction(EventManagerThing eventManagerThing, String ruleTitle, Set<String> fileCreatedEventSources, AbstractAction<?> action) throws Exception {
        UUID ruleUuid = (UUID) eventManagerThing.call(Map.of(
                PARAM_METHOD, "updateCreateRule",
                PARAM_RULE_TITLE, ruleTitle
        ));
        eventManagerThing.call(Map.of(
                PARAM_METHOD, "updateAddEventSourceNode",
                PARAM_RULE_UUID, ruleUuid.toString(),
                PARAM_RULE_NODE_UUID, String.join(";", fileCreatedEventSources)
        ));
        eventManagerThing.call(Map.of(
                PARAM_METHOD, "updateAddEventTypeClass",
                PARAM_RULE_UUID, ruleUuid.toString(),
                PARAM_RULE_CLAZZ, FileCreatedEvent.class.getName(),
                PARAM_RULE_CLAZZ_CONFIGURATION, Map.of()
        ));
        eventManagerThing.call(Map.of(
                PARAM_METHOD, "updateAddActionTask",
                PARAM_RULE_UUID, ruleUuid.toString(),
                PARAM_RULE_ACTION_UUID, action.getUuid().toString()
        ));
        eventManagerThing.call(Map.of(
                PARAM_METHOD, "updateEnableRule",
                PARAM_RULE_UUID, ruleUuid.toString()
        ));
    }

    private SendMailMessageAction findSendMailMessageAction(UUID mailServerUuid, String toEmails) {
        Collection<AbstractAction<?>> actions = engine.findActionTasks();
        for (AbstractAction<?> action : actions) {
            if (action instanceof SendMailMessageAction a) {
                if (a.configuration.emailServerConnectorThingUuid.equals(mailServerUuid) && (
                        isEmpty(a.configuration.toEmails) == isEmpty(toEmails)
                                || a.configuration.toEmails != null && a.configuration.toEmails.equals(toEmails)
                )
                ) {
                    return a;
                }
            }
        }
        return null;
    }


    private EmailServerConnectorThing findLocalMediaThing(UUID uuid) throws Exception {
        List<Thing<?>> fsList = engine.findThings(EmailServerConnectorThing.class);
        for (Thing<?> fs : fsList) {
            if (fs instanceof EmailServerConnectorThing ms) {
                if (ms.getConfiguration().uuid.equals(uuid)) {
                    return ms;
                }
            }
        }
        return null;
    }
}
