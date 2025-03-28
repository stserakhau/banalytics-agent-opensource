package com.banalytics.box.module;

import com.banalytics.box.module.events.ActionEvent;
import lombok.extern.slf4j.Slf4j;

import static com.banalytics.box.module.utils.Utils.nodeType;

@Slf4j
public abstract class AbstractAction<CONFIGURATION extends IConfiguration> extends AbstractTask<CONFIGURATION> implements IAction {
    public AbstractAction(BoxEngine engine, AbstractListOfTask<?> parent) {
        super(engine, parent);
    }

    public abstract String doAction(ExecutionContext ctx) throws Exception;

    protected abstract boolean isFireActionEvent();

    @Override
    public boolean canStart() {
        return true;
    }

    @Override
    public void action(ExecutionContext ctx) throws Exception {
        if (this.state != State.RUN) {
            return;
        }
        if (isFireActionEvent()) {
            engine.fireEvent(new ActionEvent(
                    nodeType(this.getClass()),
                    this.getUuid(),
                    getSelfClassName(),
                    getTitle(),
                    ActionEvent.ActionState.STARTING,
                    ""
            ));
        }
        String operationResult = doAction(ctx);
        if (isFireActionEvent()) {
            engine.fireEvent(new ActionEvent(
                    nodeType(this.getClass()),
                    this.getUuid(),
                    getSelfClassName(),
                    getTitle(),
                    ActionEvent.ActionState.COMPLETED,
                    operationResult
            ));
        }
    }

    @Override
    public void onException(Throwable e) {
        log.error("Operation failed {} ({}): {}", getSelfClassName(), getTitle(), e.getMessage());
        log.error("Exception details", e);
        this.stateDescription = e.getMessage();
        sendTaskState(e.getMessage());
    }

    @Override
    public void onProcessingException(Throwable e) {
        this.onException(e);
    }
}
