package com.banalytics.box.module.usecase;

import com.banalytics.box.api.integration.utils.CommonUtils;
import com.banalytics.box.module.AbstractAction;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.Instance;
import com.banalytics.box.module.Thing;
import com.banalytics.box.module.events.EventManagerThing;
import com.banalytics.box.module.events.FileCreatedEvent;
import com.banalytics.box.module.system.ForwardEventToConsumerAction;
import com.banalytics.box.module.telegram.TelegramBotThing;

import java.time.LocalDateTime;
import java.util.*;

import static com.banalytics.box.api.integration.webrtc.channel.environment.ThingApiCallReq.PARAM_METHOD;
import static com.banalytics.box.module.ConverterTypes.TYPE_SET_STRING;
import static com.banalytics.box.module.events.EventManagerThing.*;
import static com.cronutils.utils.StringUtils.isEmpty;

public class TelegramNewContentNotificationCase extends AbstractUseCase<TelegramNewContentNotificationCaseConfiguration> {
    Instance instance;

    public TelegramNewContentNotificationCase(BoxEngine engine) {
        super(engine);
    }

    @Override
    public void create() throws Exception {
        instance = engine.getPrimaryInstance();

        TelegramBotThing telegramBotThing = null;

        switch (configuration.configurationType) {// get bot
            case CREATE_NEW_TELEGRAM_BOT -> {
                telegramBotThing = new TelegramBotThing(engine);
                telegramBotThing.configuration.title = "Telegram bot - " + LocalDateTime.now();
                telegramBotThing.configuration.botToken = configuration.botToken;
                telegramBotThing.configuration.pinCode = configuration.pinCode;
                telegramBotThing.configuration.checkMessagesTimeoutMillis = 1500;

                telegramBotThing = engine.saveOrUpdateThing(telegramBotThing, true, null, true);
            }
            case USE_EXISTED_TELEGRAM_BOT -> {
                telegramBotThing = findLocalMediaThing(configuration.telegramBotUUID);
            }
        }

        if (telegramBotThing == null) {
            throw new RuntimeException("Telegram bot was removed by another user");
        }
        {//get forward action
            ForwardEventToConsumerAction forwardEventToConsumerAction = findForwardEventToConsumerAction(
                    telegramBotThing.getUuid(),
                    configuration.forwardToAccounts
            );
            if (forwardEventToConsumerAction == null) {
                forwardEventToConsumerAction = new ForwardEventToConsumerAction(engine, instance);
                forwardEventToConsumerAction.configuration.eventConsumerThing = telegramBotThing.getUuid();
                forwardEventToConsumerAction.configuration.forwardToAccounts = configuration.forwardToAccounts;
                forwardEventToConsumerAction = engine.saveOrUpdateTask(forwardEventToConsumerAction, true, null, true, instance);
            }

            {// get create event manager
                EventManagerThing emt = new EventManagerThing(engine);
                emt.configuration.title = "Telegram: new content notifications - " + LocalDateTime.now();
                emt = engine.saveOrUpdateThing(emt, true, null, true);
                Set<String> fileCreatedEventSources = CommonUtils.DEFAULT_OBJECT_MAPPER.readValue(configuration.fileCreatedEventSources, TYPE_SET_STRING);
                createAction(emt, "On file created", fileCreatedEventSources, forwardEventToConsumerAction);
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

    private ForwardEventToConsumerAction findForwardEventToConsumerAction(UUID consumerUUID, String forwardToAccounts) {
        Collection<AbstractAction<?>> actions = engine.findActionTasks();
        for (AbstractAction<?> action : actions) {
            if (action instanceof ForwardEventToConsumerAction a) {
                if (a.configuration.eventConsumerThing.equals(consumerUUID) && (
                        isEmpty(a.configuration.forwardToAccounts) == isEmpty(forwardToAccounts)
                                || a.configuration.forwardToAccounts != null && a.configuration.forwardToAccounts.equals(forwardToAccounts)
                )
                ) {
                    return a;
                }
            }
        }
        return null;
    }


    private TelegramBotThing findLocalMediaThing(UUID uuid) throws Exception {
        List<Thing<?>> fsList = engine.findThings(TelegramBotThing.class);
        for (Thing<?> fs : fsList) {
            if (fs instanceof TelegramBotThing bot) {
                if (bot.getConfiguration().uuid.equals(uuid)) {
                    return bot;
                }
            }
        }
        return null;
    }
}
