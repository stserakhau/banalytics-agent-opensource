package com.banalytics.box.module.system;

import com.banalytics.box.module.*;
import lombok.extern.slf4j.Slf4j;

import java.util.TimerTask;
import java.util.UUID;

import static com.banalytics.box.service.SystemThreadsService.SYSTEM_TIMER;

@Slf4j
public class RebootAgentAction extends AbstractAction<RebootAgentActionConfiguration> {
    public RebootAgentAction(BoxEngine engine, AbstractListOfTask<?> parent) {
        super(engine, parent);
    }

    @Override
    protected boolean isFireActionEvent() {
        return true;
    }

    @Override
    public String getTitle() {
        return getSelfClassName();
    }

    @Override
    public Object uniqueness() {
        return "reboot";
    }

    @Override
    protected boolean doProcess(ExecutionContext executionContext) throws Exception {
        SYSTEM_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                engine.reboot();
            }
        }, configuration.delayMillis);
        return false;
    }

    @Override
    public String doAction(ExecutionContext ctx) throws Exception {
        this.process(ctx);

        return null;
    }

    @Override
    public UUID getSourceThingUuid() {
        return null;
    }
}
