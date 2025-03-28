package com.banalytics.box.module.system.process;

import com.banalytics.box.api.integration.webrtc.channel.environment.ThingApiCallReq;
import com.banalytics.box.module.AbstractThing;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.State;
import com.banalytics.box.module.Thing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Order(Thing.StarUpOrder.INTEGRATION)
public class ProcessThing extends AbstractThing<ProcessConfiguration> {

    private Process targetProcess;
    private BufferedWriter processInputWriter;
    private InputStreamReader processOutputReader;
    private InputStreamReader processErrorReader;
    private ExecutorService executorService;

    private int counter = 0;
    private final LinkedList<LogItem> historyLog = new LinkedList<>();

    public ProcessThing(BoxEngine metricDeliveryService) {
        super(metricDeliveryService);
    }

    @Override
    public String getTitle() {
        return configuration.title;
    }

    @Override
    public Object uniqueness() {
        return configuration.title;
    }

    @Override
    protected void doInit() throws Exception {
    }

    private synchronized void addToHistory(String line, boolean blink, boolean isError) {
        if (historyLog == null) {
            return;
        }
        if (isError) {
            log.error(line);
        } else {
            log.info(line);
        }
        historyLog.addLast(new LogItem(counter, blink, isError, line));
        counter++;
        if (historyLog.size() > configuration.getHistoryLines()) {
            historyLog.removeFirst();
        }
    }

    @Override
    public void doStart() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(configuration.commandLine.split(" "));
        targetProcess = processBuilder.start();

        processInputWriter = new BufferedWriter(new OutputStreamWriter(targetProcess.getOutputStream()));
        processOutputReader = new InputStreamReader(targetProcess.getInputStream());
        processErrorReader = new InputStreamReader(targetProcess.getErrorStream());

        executorService = Executors.newFixedThreadPool(3);
        executorService.submit(() -> {
            captureProcessOutput(processOutputReader, false);
        });
        executorService.submit(() -> {
            captureProcessOutput(processErrorReader, true);
        });
        executorService.submit(() -> {
            try {
                Thread.sleep(1000);
                log.info("======================= Started");
                while (true) {
                    Thread.sleep(500);
                    if(!flushed && System.currentTimeMillis() > flushTimeout) {
                        addToHistory(line.toString(), true, false);
                        flushed = true;
                    }
                }
            } catch (InterruptedException e) {
                log.info("======================= Stopped");
                log.warn(e.getMessage());
            }
        });
    }
    private boolean flushed = true;
    private long flushTimeout;

    private final char[] buffer = new char[1024];
    private final StringBuilder line = new StringBuilder(1024);

    private void captureProcessOutput(Reader reader, boolean isErrorStream) {
        try {
            int read;
            while ((read = reader.read(buffer)) != -1) {
                if (read == 0) {
                    addToHistory(line.toString(), false, isErrorStream);
                } else {
                    flushTimeout = System.currentTimeMillis() + 500;
                    for (int i = 0; i < read; i++) {
                        char c = buffer[i];
                        if (c == '\n') {
                            addToHistory(line.toString(), false, isErrorStream);
                            line.setLength(0);
                            flushed = true;
                            continue;
                        }
                        line.append(c);
                        flushed = false;
                    }
                }
            }
            log.info("Process finished with output: {}", targetProcess.exitValue());
            stop();
        } catch (Exception e) {
            addToHistory(e.getMessage(), false, true);
            onProcessingException(e);
        }
    }

    private synchronized void sendCommand(String command) throws IOException {
        if ("\u0003".equals(command)) {
            log.info("Command: Ctrl + C");
            processInputWriter.write(3);
            processInputWriter.flush();
        } else {
            synchronized (historyLog) {
                if(!historyLog.isEmpty()) {
                    LogItem lastItem = historyLog.getLast();
                    if (lastItem != null && lastItem.blinking) {
                        historyLog.removeLast();
                    }
                }
            }
            log.info("Command: {}", command);
            processInputWriter.write(command);
            processInputWriter.newLine();
            processInputWriter.flush();
        }
    }

    @Override
    public void doStop() throws Exception {
        targetProcess.destroyForcibly();
        executorService.shutdownNow();
    }

    @Override
    public Set<String> apiMethodsPermissions() {
        return Set.of();
    }

    @Override
    public Object call(Map<String, Object> params) throws Exception {
        String method = (String) params.get(ThingApiCallReq.PARAM_METHOD);
        switch (method) {
            case "history" -> {
                Number fromLine = (Number) params.get("fromLine");
                if (fromLine == null) {
                    return historyLog;
                }
                LinkedList<LogItem> lastAddedLineHistory = new LinkedList<>();

                Iterator<LogItem> it = historyLog.descendingIterator();
                while (it.hasNext()) {
                    LogItem lastItem = it.next();
                    if (lastItem.lineNumber == fromLine.intValue()) {
                        break;
                    }
                    lastAddedLineHistory.addFirst(lastItem);
                }

                return lastAddedLineHistory;
            }
            case "command" -> {
                sendCommand((String) params.get("command"));
                return Collections.emptyMap();
            }
            default -> {
                throw new Exception("Method not supported: " + method);
            }
        }
    }

    public record LogItem(int lineNumber, boolean blinking, boolean error, String message) {
    }
}
