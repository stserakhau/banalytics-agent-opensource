package com.banalytics.box.module.system;

import com.banalytics.box.module.events.AbstractEvent;
import com.banalytics.box.module.events.ActionEvent;
import com.banalytics.box.module.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.banalytics.box.service.SystemThreadsService.SYSTEM_TIMER;

@Slf4j
public class TimeoutAction extends AbstractAction<TimeoutActionConfiguration> {
    public TimeoutAction(BoxEngine engine, AbstractListOfTask<?> parent) {
        super(engine, parent);
    }

    public IAction targetAction;

    @Override
    protected boolean isFireActionEvent() {
        return true;
    }

    private long executionTime = 0;

    private TimerTask timeoutTaskExecutor;

    @Override
    protected synchronized boolean doProcess(ExecutionContext executionContext) throws Exception {
        long now = System.currentTimeMillis();
        if (timeoutTaskExecutor == null) {
            this.executionTime = now + configuration.timeoutMillis;
            long checkTimeInterval = configuration.timeoutMillis / 10;
            if (checkTimeInterval < 10) {
                checkTimeInterval = 10;
            }
            timeoutTaskExecutor = new TimerTask() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    if (now < executionTime) {
                        return;
                    }
                    try {
                        ExecutionContext ctx = new ExecutionContext();
                        ctx.setVar(IAction.SCHEDULED_RUN, IAction.SCHEDULED_RUN);
                        targetAction.action(ctx);
                    } catch (Exception e) {
                        TimeoutAction.this.onException(e);
                    } finally {
                        timeoutTaskExecutor = null;
                        cancel();
                    }
                }
            };
            SYSTEM_TIMER.schedule(timeoutTaskExecutor, 0, checkTimeInterval);
        } else {
            if(configuration.prolongationEnabled) {
                this.executionTime = now + configuration.timeoutMillis;
            }
        }

        return false;
    }

    @Override
    public Object uniqueness() {
        return configuration.getTargetAction();
    }

    @Override
    public String doAction(ExecutionContext ctx) throws Exception {
        this.process(ctx);
        return null;
    }

    @Override
    public Set<Class<? extends AbstractEvent>> produceEvents() {
        Set<Class<? extends AbstractEvent>> events = new HashSet<>(super.produceEvents());
        events.add(ActionEvent.class);
        return events;
    }

    @Override
    public String getTitle() {
        return configuration.title;
    }

    @Override
    public Map<String, Object> uiDetails() {
        if (targetAction == null) {
            super.uiDetails();
        }
        return Map.of(
                TARGET_OBJECT_TITLE, targetAction.getTitle(),
                TARGET_OBJECT_CLASS, targetAction.getClass()
        );
    }

    @Override
    public UUID getSourceThingUuid() {
        if (targetAction == null) {
            return null;
        }
        return targetAction.getUuid();
    }

    @Override
    public void doInit() throws Exception {
        targetAction = engine.findTask(configuration.targetAction);
        if (targetAction == null) {
            throw new Exception("Action was removed.");
        }
    }
}
