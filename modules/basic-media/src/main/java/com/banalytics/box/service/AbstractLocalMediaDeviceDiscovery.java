package com.banalytics.box.service;

import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.model.discovery.AudioDevice;
import com.banalytics.box.module.model.discovery.AudioProperties;
import com.banalytics.box.module.model.discovery.VideoDevice;
import com.banalytics.box.module.model.discovery.VideoProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecHWConfig;
import org.bytedeco.ffmpeg.avformat.AVOutputFormat;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.javacpp.Pointer;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.banalytics.box.api.integration.utils.CommonUtils.DEFAULT_OBJECT_MAPPER;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractLocalMediaDeviceDiscovery implements InitializingBean {
    private final static long REFRESH_TIMEOUT = 60000;

    final BoxEngine engine;

    private final TypeReference<Map<String, AudioDevice>> TYPE_AUDIO_MAP = new TypeReference<>() {
    };

    private final TypeReference<Map<String, VideoDevice>> TYPE_VIDEO_MAP = new TypeReference<>() {
    };

    protected Map<String, AudioDevice> audioDevices = new HashMap<>();
    protected Map<String, VideoDevice> videoDevices = new HashMap<>();

    private File localAudioDevicesFile;
    private File localVideoDevicesFile;

    @Override
    public void afterPropertiesSet() throws Exception {
        File applicationConfigFolder = engine.applicationConfigFolder();
        File instanceFolder = new File(applicationConfigFolder, "instances");
        this.localAudioDevicesFile = new File(instanceFolder, "local-audio-devices.json");
        this.localVideoDevicesFile = new File(instanceFolder, "local-video-devices.json");
        if (!localAudioDevicesFile.exists() || !localVideoDevicesFile.exists()) {
            if (scanLocalDevices()) {
                persistData();
            }
        } else {
            final File anyDataFile = localAudioDevicesFile.exists() ? localAudioDevicesFile : localVideoDevicesFile;
            long now = System.currentTimeMillis();
            long lastModified = anyDataFile.lastModified();
            if ((lastModified - now) > REFRESH_TIMEOUT && scanLocalDevices()) {
                persistData();
            } else {
                loadData();
            }
        }
    }

    private void persistData() throws IOException {
        DEFAULT_OBJECT_MAPPER.writeValue(localAudioDevicesFile, this.audioDevices);
        DEFAULT_OBJECT_MAPPER.writeValue(localVideoDevicesFile, this.videoDevices);
    }

    private void loadData() throws IOException {
        audioDevices = DEFAULT_OBJECT_MAPPER.readValue(localAudioDevicesFile, TYPE_AUDIO_MAP);
        videoDevices = DEFAULT_OBJECT_MAPPER.readValue(localVideoDevicesFile, TYPE_VIDEO_MAP);
    }

    /**
     * returns true is device list was changed
     *
     * @return
     */
    public abstract boolean scanLocalDevices();

    public List<String> microphones() {
        return audioDevices.values().stream()
                .sorted(Comparator.comparing(a -> a.name))
                .map(d -> d.alternativeName + "~" + d.name)
                .collect(Collectors.toList());
    }

    public List<String> cameras() {
        return videoDevices.values().stream()
                .sorted(Comparator.comparing(a -> a.name))
                .map(d -> d.alternativeName + "~" + d.name)
                .collect(Collectors.toList());
    }

    public List<String> cameraResolutionsFps(String camera) {
        for (VideoDevice vd : videoDevices.values()) {
            if (vd.alternativeName.equals(camera)) {
                TreeSet<VideoProperties.ResolutionFpsCase> cases = vd.videoProperties.resolutionFpsCases;
                List<String> result = new ArrayList<>();
                for (VideoProperties.ResolutionFpsCase rfc : cases) {
                    String key = rfc.getWidth() + "x" + rfc.getHeight() + "/" + rfc.getMinFps() + "-" + rfc.getMaxFps();
                    String value = key;
                    if (rfc.getMinFps() != rfc.getMinRecommendedFps() || rfc.getMaxFps() != rfc.getMaxRecommendedFps()) {
                        value += " (" + rfc.getMinRecommendedFps() + "-" + rfc.getMaxRecommendedFps() + ")";
                    }
                    result.add(key + "~" + value);
                }
                return result;
            }
        }
        return Collections.emptyList();
    }

    public Set<String> audioSupportedSampleRates(String audio) {
        AudioDevice audioDevice = audioDevices.get(audio);
        if (audioDevice == null) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        AudioProperties ap = audioDevice.audioProperties;

        for (AudioProperties.AudioCase ac : ap.audioCases) {
            result.add("" + ac.getRate());
        }

        return result;
    }

    /*todo below experiments */
    private final Map<String, String> videoEncoders = new TreeMap<>();
    private final Map<String, String> videoDecoders = new TreeMap<>();

    private void loadCodecs() {
        Pointer i = new Pointer((Pointer) null);

        AVOutputFormat format = avformat.av_guess_format("mp4", null, null);

        AVCodec codec = null;
        AVCodec testCodec;
        while ((testCodec = av_codec_iterate(i)) != null) {
            if (testCodec.type() == AVMEDIA_TYPE_VIDEO) {
                if (avformat.avformat_query_codec(format, testCodec.id(), FF_COMPLIANCE_STRICT) > 0) {
                    AVCodecHWConfig hwConfig = avcodec.avcodec_get_hw_config(testCodec, 0);
                    if (hwConfig == null) {
                        continue;
                    }
                    for (int j = 0; ; j++) {
                        hwConfig = avcodec.avcodec_get_hw_config(testCodec, j);
                        if (hwConfig == null) {
                            break;
                        }
                        if ((hwConfig.methods() & AV_CODEC_HW_CONFIG_METHOD_HW_DEVICE_CTX) > 0) {
                            log.info(testCodec.name().getString() + " supports device: " + hwConfig.device_type());
                        }
                        //AV_HWDEVICE_TYPE_DXVA2
                    }

                    if (av_codec_is_encoder(testCodec) > 0) {
                        videoEncoders.put(testCodec.name().getString(), testCodec.long_name().getString());
                    }
                    if (av_codec_is_decoder(testCodec) > 0) {
                        videoDecoders.put(testCodec.name().getString(), testCodec.long_name().getString());
                    }
                }
            }
        }
    }

    public Set<String> accelerators() {
        return Set.of(
                "cuvid", "d3d11va", "dxva2", "libmfx", "qsv", "vaapi", "vdpau"
        );
    }

    public Map<String, String> accelerationDecoders() {
        if (videoDecoders.isEmpty()) {
            loadCodecs();
        }

        return videoDecoders;
    }

    public Map<String, String> accelerationEncoders() {
        if (videoEncoders.isEmpty()) {
            loadCodecs();
        }

        return videoEncoders;
    }
}
