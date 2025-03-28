package com.banalytics.box.module.events;

import com.banalytics.box.api.integration.webrtc.channel.environment.ThingApiCallReq;
import com.banalytics.box.model.task.EnvironmentNode;
import com.banalytics.box.module.AbstractThing;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.Singleton;
import com.banalytics.box.module.Thing;
import com.banalytics.box.module.events.jpa.EventStore;
import com.banalytics.box.module.standard.EventConsumer;
import com.banalytics.box.module.utils.DataHolder;
import com.banalytics.box.service.JpaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.banalytics.box.api.integration.utils.TimeUtil.currentTimeInServerTz;
import static com.banalytics.box.module.State.RUN;
import static com.banalytics.box.service.SystemThreadsService.SYSTEM_TIMER;

@Slf4j
@Order(Thing.StarUpOrder.CORE)
public class EventHistoryThing extends AbstractThing<EventHistoryThingConfig> implements EventConsumer, Singleton {
    private ExecutorService persistenceExecutor;

    public EventHistoryThing(BoxEngine engine) {
        super(engine);
    }

    private TimerTask cleanupHistoryTask;

    @Override
    protected void doInit() throws Exception {
    }

    @Override
    protected void doStart() throws Exception {
        persistenceExecutor = Executors.newSingleThreadExecutor();

        if (configuration.cleanUpTimePeriod.intervalMillis > 0) {
            cleanupHistoryTask = new TimerTask() {
                @Override
                public void run() {
                    JpaService jpaService = engine.getJpaService();
                    long historyLengthInDays = configuration.historyLengthInDays * 24 * 60 * 60 * 1000L;
                    LocalDateTime expiredFrom = currentTimeInServerTz()
                            .minus(historyLengthInDays, ChronoUnit.MILLIS);

                    jpaService.cleanUpEventHistory(expiredFrom);
                }
            };
            SYSTEM_TIMER.schedule(cleanupHistoryTask, 5000, configuration.cleanUpTimePeriod.intervalMillis);
        } else {
            cleanupHistoryTask = null;
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (cleanupHistoryTask != null) {
            cleanupHistoryTask.cancel();
            cleanupHistoryTask = null;
        }
        persistenceExecutor.shutdown();
        persistenceExecutor = null;
    }

    @Override
    public Set<String> accountNames(Set<String> accountIds) {
        return Set.of();
    }

    @Override
    public void consume(Recipient target, AbstractEvent event) {
        JpaService jpaService = engine.getJpaService();
        persistenceExecutor.submit(() -> {
            if (jpaService.isOpen()) {
                EventStore es = new EventStore();
                es.setNodeUuid(event.getNodeUuid());
                es.setDateTime(event.getDateTime());
                es.setMessageType(event.getType());
                es.setEvent(event);
                jpaService.persistEntity(es);
            }
        });
    }

    @Override
    public Set<String> apiMethodsPermissions() {
        return Set.of(PERMISSION_READ);
    }

    @Override
    public Object call(Map<String, Object> params) throws Exception {
        if (getState() != RUN) {
            throw new Exception("error.thing.notInitialized");
        }
        String method = (String) params.get(ThingApiCallReq.PARAM_METHOD);
        switch (method) {
            case "readTaskHierarchy" -> {
                return EnvironmentNode.build(engine.getPrimaryInstance());
            }
            case "readEventTypes" -> {
                Set<String> eventTypes = new HashSet<>();
                Collection<Class<? extends AbstractEvent>> evts = DataHolder.eventTypeClasses();
                for (Class<? extends AbstractEvent> evt : evts) {
                    AbstractEvent e = evt.getDeclaredConstructor().newInstance();
                    String type = e.getType();
                    eventTypes.add(type);
                }
                return eventTypes;
            }
            case "readHistory" -> {
                String query = (String) params.get("query");
                int pageNum;
                try {
                    pageNum = (Integer) params.get("pageNum");
                } catch (NumberFormatException e) {
                    pageNum = 0;
                }
                int pageSize;
                try {
                    pageSize = (Integer) params.get("pageSize");
                } catch (NumberFormatException e) {
                    pageSize = 10;
                }
                Map<String, String> orderSpec = (Map<String, String>) params.get("orderSpec");

                JpaService jpaService = engine.getJpaService();
                return jpaService.expressionQuery(pageNum, pageSize, query, orderSpec, EventStore.class);
            }
            default -> throw new Exception("Method not supported: " + method);
        }
    }
}
