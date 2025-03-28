package com.banalytics.box.module.system;

import com.banalytics.box.module.events.AbstractEvent;
import com.banalytics.box.module.events.ActionEvent;
import com.banalytics.box.module.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class TaskActionAction extends AbstractAction<TaskActionActionConfiguration> {
    public TaskActionAction(BoxEngine engine, AbstractListOfTask<?> parent) {
        super(engine, parent);
    }

    public ITask<?> targetTask;

    @Override
    protected boolean isFireActionEvent() {
        return true;
    }

    @Override
    protected boolean doProcess(ExecutionContext executionContext) throws Exception {
        if (configuration.affectAutostartState) {
            log.info("Execution (affect autostart): {} over {} ({})", configuration.action, targetTask.getSelfClassName(), targetTask.getTitle());
            switch (configuration.action) {
                case START -> engine.startTask(targetTask.getUuid());
                case RESTART -> targetTask.restart();
                case STOP -> engine.stopTask(targetTask.getUuid());
            }
        } else {
            log.info("Execution: {} over {} ({})", configuration.action, targetTask.getSelfClassName(), targetTask.getTitle());
            switch (configuration.action) {
                case START -> targetTask.start(configuration.ignoreAutostartState, true);
                case RESTART -> targetTask.restart();
                case STOP -> targetTask.stop();
            }
        }
        return false;
    }

    @Override
    public Object uniqueness() {
        return configuration.targetTask + ":" + configuration.affectAutostartState + ":" + configuration.action;
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
        if (targetTask == null) {
            return "";
        }
        Map<String, String> locale = engine.i18n().get("en");
        String title = locale.get(targetTask.getSelfClassName()) + ": " + targetTask.getTitle();
        return configuration.action
                + (configuration.affectAutostartState ? " (change autostart)" : "")
                + ": " + title;
    }

    @Override
    public Map<String, Object> uiDetails() {
        if (targetTask == null) {
            return super.uiDetails();
        }
        return Map.of(
                TARGET_OBJECT_TITLE, targetTask.getTitle(),
                TARGET_OBJECT_CLASS, targetTask.getSelfClassName()
        );
    }

    @Override
    public UUID getSourceThingUuid() {
        if (targetTask == null) {
            return null;
        }
        return targetTask.getSourceThingUuid();
    }

    @Override
    public void doInit() throws Exception {
        targetTask = engine.findTask(configuration.targetTask);
        if (targetTask == null) {
            throw new Exception("Task was removed");
        }
    }
}
