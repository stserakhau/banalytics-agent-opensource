package com.banalytics.box.module.system.cmd;

import com.banalytics.box.module.events.AbstractEvent;
import com.banalytics.box.module.events.ActionEvent;
import com.banalytics.box.module.AbstractAction;
import com.banalytics.box.module.AbstractListOfTask;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CmdAction extends AbstractAction<CmdActionConfiguration> {

    public CmdAction(BoxEngine engine, AbstractListOfTask<?> parent) {
        super(engine, parent);
    }

    private String[] commandLines;

    private final StringBuilder response = new StringBuilder(10000);

    @Override
    public void doInit() throws Exception {
        this.commandLines = configuration.commandLine.split("\n");
    }

    @Override
    protected boolean isFireActionEvent() {
        return true;
    }

    @Override
    public Object uniqueness() {
        return configuration.title;
    }

    @Override
    public String doAction(ExecutionContext ctx) throws Exception {
        response.setLength(0);

        ProcessBuilder processBuilder = new ProcessBuilder();

        for (String commandLine : commandLines) {
            commandLine = commandLine.trim();
            if (commandLine.isEmpty()) {
                response.append("\n");
                continue;
            }
            response.append("Command:\n").append(commandLine).append("\n");
            log.info("Executing command:\n" + response);
            String[] cmd = commandLine.split(" ");
            try {
                Process process = processBuilder.command(cmd).start();
                try {
                    process.waitFor(configuration.waitTimeoutSec, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    response.append("Exit by timeout\n");
                }
                try (InputStream is = process.getInputStream()) {
                    String result = IOUtils.toString(is, StandardCharsets.UTF_8);
                    response.append("Result:\n").append(result).append("\n");
                }
                try (InputStream is = process.getErrorStream()) {
                    String result = IOUtils.toString(is, StandardCharsets.UTF_8);
                    response.append("Error:\n").append(result);
                }
                response.append("Exit code: ").append(process.exitValue());
            } catch (Throwable e){
                response.append("Error: ").append(e.getMessage());
            }
        }
        log.info(response.toString());

        return response.toString();
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
}
