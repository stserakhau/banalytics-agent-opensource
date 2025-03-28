package com.banalytics.box.module.audio;

import com.banalytics.box.api.integration.model.SubItem;
import com.banalytics.box.module.*;
import com.banalytics.box.module.standard.AudioPlayer;
import com.banalytics.box.module.storage.FileSystem;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Map;
import java.util.UUID;

@Slf4j
@SubItem(of = LocalAudioPlayerThing.class, group = "media-player")
public class PlayAudioAction extends AbstractAction<PlayAudioActionConfiguration> {
    @Override
    protected boolean isFireActionEvent() {
        return true;
    }

    @Override
    public String getTitle() {
        String filePath = configuration.playAudioFile;
        int fNameStart = filePath.lastIndexOf("/");
        String fileName = fNameStart > -1 ? filePath.substring(fNameStart + 1) : filePath;
        return fileName;
    }

    public PlayAudioAction(BoxEngine metricDeliveryService, AbstractListOfTask<?> parent) {
        super(metricDeliveryService, parent);
    }

    private AudioPlayer audioPlayer;
    private FileSystem fileSystem;

    private File audioFile;

    private long executionTimeout;

    @Override
    public UUID getSourceThingUuid() {
        if (audioPlayer == null) {
            return null;
        }
        return ((Thing<?>) audioPlayer).getUuid();
    }

    @Override
    public Object uniqueness() {
        return configuration.fileSystemUuid + "/" + configuration.playAudioFile + "->" + configuration.audioPlayerUuid;
    }

    @Override
    public void doInit() throws Exception {
        if (audioPlayer != null) {
            ((Thing<?>) audioPlayer).unSubscribe(this);
        }
        audioPlayer = engine.getThingAndSubscribe(configuration.audioPlayerUuid, this);

        this.fileSystem = engine.getThing(configuration.fileSystemUuid);

        this.audioFile = fileSystem.getLocalFile(configuration.playAudioFile);
    }

    @Override
    public void doStart(boolean ignoreAutostartProperty, boolean startChildren) throws Exception {
        long currentTime = System.currentTimeMillis();
        executionTimeout = currentTime;
    }

    @Override
    protected boolean doProcess(ExecutionContext executionContext) throws Exception {
        long currentTime = System.currentTimeMillis();
        if (currentTime < executionTimeout) {
            return true;
        }
        try {
            executionTimeout = currentTime + configuration.waitBeforeNextExecution.intervalMillis;

//            AbstractEvent evt = executionContext.getVar(AbstractEvent.class);
//            if(evt instanceof FileCreatedEvent fce) {
//                try {
//                    String dataUri = fce.getContextPath();
//                    URL url = new URL(dataUri);
//                    String filePath = url.getFile();
//                    String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
//                    File src = File.createTempFile("telegram_download", fileName);
//                    IOUtils.copy(url, src);
//                    audioPlayer.play(src);
//                } catch (Exception e) {
//                    log.error(e.getMessage(), e);
//                }
//
//            } else {
                audioPlayer.play(this.audioFile);
//            }
        } catch (Throwable e) {
            onProcessingException(e);
        }
        return true;
    }

    @Override
    public String doAction(ExecutionContext ctx) throws Exception {
        this.process(ctx);
        return null;
    }

    @Override
    public void doStop() throws Exception {
    }

    @Override
    public Map<String, Object> uiDetails() {
        if (audioPlayer == null) {
            super.uiDetails();
        }
        return Map.of(TARGET_OBJECT_TITLE, audioPlayer.getTitle());
    }

    @Override
    public void destroy() {
        if (audioPlayer != null) {
            ((Thing<?>) audioPlayer).unSubscribe(this);
            audioPlayer = null;
        }
    }
}
