package com.banalytics.box.service.windows;

import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.model.discovery.AudioDevice;
import com.banalytics.box.module.model.discovery.VideoDevice;
import com.banalytics.box.service.AbstractLocalMediaDeviceDiscovery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.LogCallback;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.bytedeco.ffmpeg.global.avdevice.avdevice_register_all;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;

@Slf4j
@Profile({"windows"})
@Service
public class LocalMediaDeviceDiscoveryService extends AbstractLocalMediaDeviceDiscovery {
    public LocalMediaDeviceDiscoveryService(BoxEngine engine) {
        super(engine);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            avdevice_register_all();
            avutil.av_log_set_level(avutil.AV_LOG_FATAL);

            super.afterPropertiesSet();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    public synchronized boolean scanLocalDevices() {
        boolean deviceListChanged = false;
        synchronized (org.bytedeco.ffmpeg.global.avcodec.class) {
            List<String> result = Collections.synchronizedList(new ArrayList<>());
            avutil.av_log_set_level(avutil.AV_LOG_INFO);
            avutil.setLogCallback(new LogCallback() {
                StringBuilder line = new StringBuilder();

                @Override
                public void call(int i, BytePointer bytePointer) {
                    try {
                        String message = bytePointer.getString("UTF-8");
                        if (message.isEmpty()) {
                            return;
                        }
                        line.append(message);
                        boolean newLine = message.indexOf('\n') > -1;
                        if (newLine) {
                            result.add(line.toString().trim());
                            line = new StringBuilder();
                        }
                    } catch (Throwable e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });

            DeviceInfoAccumulator deviceInfoAccumulator = new DeviceInfoAccumulator();
            try {
                result.clear();
                execute("dshow", "list_devices", "dummy");
                deviceInfoAccumulator.process(new ArrayList<>(result));

                Set<String> newAudioDevices = new HashSet<>();
                Set<String> presentAudioDevices = new HashSet<>();
                Set<String> removedAudioDevices = new HashSet<>(audioDevices.keySet());
                for (Map.Entry<String, AudioDevice> entry : deviceInfoAccumulator.audioDevices.entrySet()) {
                    String key = entry.getKey();
                    removedAudioDevices.remove(key);
                    AudioDevice value = entry.getValue();
                    if (audioDevices.containsKey(key)) {
                        presentAudioDevices.add(key);
                        continue;
                    }
                    deviceListChanged = true;
                    newAudioDevices.add(key);
                    audioDevices.put(key, value);
                }
                removedAudioDevices.forEach(key -> audioDevices.remove(key));

                Set<String> newVideoDevices = new HashSet<>();
                Set<String> presentVideoDevices = new HashSet<>();
                Set<String> removedVideoDevices = new HashSet<>(videoDevices.keySet());
                for (Map.Entry<String, VideoDevice> entry : deviceInfoAccumulator.videoDevices.entrySet()) {
                    String key = entry.getKey();
                    removedVideoDevices.remove(key);
                    VideoDevice val = entry.getValue();
                    if (videoDevices.containsKey(key)) {
                        presentVideoDevices.add(key);
                        continue;
                    }
                    deviceListChanged = true;
                    newVideoDevices.add(key);
                    videoDevices.put(key, val);
                }
                removedVideoDevices.forEach(key -> videoDevices.remove(key));

            } catch (Throwable e) {
                log.info(e.getMessage(), e);
            }
            for (VideoDevice videoDevice : videoDevices.values()) {
                if (!videoDevice.videoProperties.resolutionFpsCases.isEmpty()) {
                    continue;
                }
                VideoDevicePropertyAccumulator vda = new VideoDevicePropertyAccumulator();
                try {
                    result.clear();
                    execute("dshow", "list_options", "video=" + videoDevice.alternativeName);
                    vda.process(new ArrayList<>(result));
                } catch (Throwable e) {
                    log.info(e.getMessage(), e);
                } finally {
                    videoDevice.videoProperties = vda.videoProperties;
                }
            }
            for (AudioDevice audioDevice : audioDevices.values()) {
                if (!audioDevice.audioProperties.audioCases.isEmpty()) {
                    continue;
                }
                AudioDevicePropertyAccumulator ada = new AudioDevicePropertyAccumulator();
                try {
                    result.clear();
                    execute("dshow", "list_options", "audio=" + audioDevice.alternativeName);
                    ada.process(new ArrayList<>(result));
                } catch (Throwable e) {
                    log.info(e.getMessage(), e);
                } finally {
                    audioDevice.audioProperties = ada.audioProperties;
                }
            }
            try {
                Thread.sleep(500);
                log.info("============Scan done");
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
            avutil.av_log_set_level(avutil.AV_LOG_FATAL);
            FFmpegLogCallback.set();
            log.info("============Revert to default logger");
        }

        return deviceListChanged;
    }

    private static void execute(String format, String option, String device) {
        log.info("Execute: {}, {}, {}", format, option, device);
        AVFormatContext context = avformat_alloc_context();
        AVDictionary options = new AVDictionary(null);
        try {
            AVInputFormat inputFormat = avformat.av_find_input_format(format);
            context.iformat(inputFormat);
            int res = av_dict_set(options, option, "true", 0);//list_options or list_formats
            if (res == 0) {
//                System.out.println("=============== " + context + " / " + device + " / " + inputFormat + " / " + options);
                res = avformat_open_input(context, device, inputFormat, options);
                if (!context.isNull()) {
                    avformat_close_input(context);
                }
            }
            Thread.sleep(300);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        } finally {
            av_dict_free(options);
            avformat_free_context(context);

            log.info("Executed");
        }
    }
}