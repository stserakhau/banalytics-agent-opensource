package com.banalytics.box.module.audio;

import com.banalytics.box.module.AbstractThing;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.standard.AudioPlayer;
import com.banalytics.box.service.LocalAudioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.banalytics.box.module.Thing.StarUpOrder.DATA_EXCHANGE;

/**
 * For playing multiple files in parallel Ubuntu needs
 * 1. sudo apt install paprefs
 * 2. pulseaudio -k
 * 3. systemctl --user restart pulseaudio.service
 */
@Slf4j
@Order(DATA_EXCHANGE + 300)
public class LocalAudioPlayerThing extends AbstractThing<LocalAudioPlayerThingConfiguration> implements AudioPlayer {
    public LocalAudioPlayerThing(BoxEngine engine) {
        super(engine);
    }

    @Override
    public String getTitle() {
        return configuration.audioDevice;
    }

    @Override
    public Object uniqueness() {
        return configuration.audioDevice;
    }

    @Override
    public Set<String> generalPermissions() {
        return super.generalPermissions();
    }

    @Override
    public Set<String> apiMethodsPermissions() {
        return Set.of();
    }

    @Override
    protected void doInit() throws Exception {
    }

    @Override
    protected void doStart() throws Exception {
//        this.clip.addLineListener(event -> {
//            OPEN / CLOSE / START/ STOP
//        });
    }

    public static void main(String[] args) throws Exception {
        LocalAudioPlayerThing t = new LocalAudioPlayerThing(new BoxEngine() {
        });
        List<String> list = new ArrayList<>(LocalAudioService.supportedAudioPlayers());
        try {
            t.configuration.audioDevice = list.get(0);
            t.start(false, true);
            t.play(new File("e:\\out\\signal.wav"));
            t.play(new File("e:\\out\\SysAlert.wav"));
            Thread.sleep(2000);
            t.play(new File("e:\\out\\SysAlert.wav"));
            Thread.sleep(5000);
            t.stop();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    Map<File, Clip> fileClipMap = new ConcurrentHashMap<>();

    public void play(File file) throws Exception {
        //todo close line before play
        //todo or check mixer availability and if ok then play
        //todo or convert audio to supported format
//        String fName = file.getName();
//        if (fName.endsWith(".oga") || fName.endsWith(".ogg")) {
//            try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
//                AudioFormat format = stream.getFormat();
//                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16,
//                        format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
//                // Get AudioInputStream that will be decoded by underlying
//                // VorbisSPI
//                AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, stream);
//
//                DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
//
//                try (Line res = AudioSystem.getLine(info); SourceDataLine line = (SourceDataLine) res) {
//                    if (line != null) {
//                        line.open(decodedFormat);
//                        try {
//                            byte[] data = new byte[16];
//                            // Start
//                            line.start();
//                            int nBytesRead = 0;
//                            while (nBytesRead != -1) {
//                                nBytesRead = decodedStream.read(data, 0, data.length);
//                                if (nBytesRead != -1) {
//                                    line.write(data, 0, nBytesRead);
//                                }
//                            }
//                            // Stop
//                            line.stop();
//                        } catch (IOException io) {
//                            // Do nothing
//                        } finally {
//                            // Stop
//                            line.stop();
//                        }
//                    }
//                } catch (LineUnavailableException lue) {
//                    // Do nothing
//                }
//            }
//        } else {
            Clip clip = fileClipMap.computeIfAbsent(file, f -> {
                try {
                    Mixer mixer = LocalAudioService.getAudioPlayer(configuration.getAudioDevice());
                    AudioInputStream audio = AudioSystem.getAudioInputStream(f);
                    DataLine.Info info = new DataLine.Info(Clip.class, audio.getFormat());
                    Clip c = (Clip) mixer.getLine(info);
                    c.open(audio);
                    return c;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });

            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
//        }
    }

    @Override
    protected void doStop() throws Exception {
        fileClipMap.values().forEach(Line::close);
        fileClipMap.clear();
    }
}
