package com.banalytics.box.module.media.task.storage;

import com.banalytics.box.api.integration.form.ComponentType;
import com.banalytics.box.api.integration.form.annotation.UIComponent;
import com.banalytics.box.api.integration.form.annotation.UIDoc;
import com.banalytics.box.module.AbstractConfiguration;
import com.banalytics.box.module.constants.BitRate;
import com.banalytics.box.module.constants.SplitTimeInterval;
import com.banalytics.box.module.constants.TimestampFormat;
import com.banalytics.box.module.constants.UseFrameRate;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import static com.banalytics.box.api.integration.form.ComponentType.int_input;

@Getter
@Setter
@UIDoc(href = "https://www.banalytics.live/knowledge-base/tasks/continuous-recording")
public class ContinousVideoRecordingConfig extends AbstractConfiguration {

    @UIComponent(
            index = 10,
            type = ComponentType.drop_down,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "sort", value = "asc")
            },
            backendConfig = {
                    @UIComponent.BackendConfig(bean = "taskService", method = "findActiveByStandard", params = {"com.banalytics.box.module.standard.FileStorage"})
            },
            restartOnChange = true
    )
    public UUID storageUuid;

    @UIComponent(index = 20,
            type = ComponentType.checkbox,
            required = true
    )
    public boolean disableAudioRecording = false;

    @UIComponent(index = 30, type = ComponentType.drop_down, required = true, restartOnChange = true)
    public TimestampFormat pathPattern = TimestampFormat.yyyyMMdd_hh_mmss;

    @UIComponent(index = 40, type = ComponentType.drop_down, required = true)
    public BitRate videoBitRate = BitRate.k512;

    @UIComponent(
            index = 50,
            type = int_input,
            required = true,
            uiConfig = {
                    @UIComponent.UIConfig(name = "min", value = "1"),
                    @UIComponent.UIConfig(name = "max", value = "1000")
            }
    )
    public int gop = 60;

//    @UIComponent(
//            index = 30,
//            type = ComponentType.drop_down,
//            required = true,
//            backendConfig = {
//                    @UIComponent.BackendConfig(values = {"cpu", "dxva2", "nvdec", "cuda", "qsv", "d3d11va", "vaapi", "opencl"})
//            },
//            restartOnChange = true
//    )
//    public String hwAccel = "cpu";
//
//    @UIComponent(
//            index = 30,
//            type = ComponentType.drop_down,
//            required = false,
//            backendConfig = {
////                    @UIComponent.BackendConfig(bean = "localMediaDeviceDiscoveryService", method = "accelerationEncoders")
//                    @UIComponent.BackendConfig(values = {"default", "h264_nvenc", "h264_qsv", "h264_vaapi", "h264_amf"})
//            },
//            restartOnChange = true
//    )
//    public String encoder = "default";

    //https://trac.ffmpeg.org/wiki/Encode/H.264
//    @UIComponent(
//            index = 30,
//            type = ComponentType.drop_down,
//            required = true,
//            backendConfig = {
//                    @UIComponent.BackendConfig(values = {
//                            "ultrafast", "superfast", "veryfast", "faster", "fast",
//                            "medium", "slow", "veryfast", "slower", "veryslow"
//                    })
//            }
//    )
//    public String ffmpegPreset = "medium";
//
//    @UIComponent(
//            index = 35,
//            type = ComponentType.drop_down,
//            required = true,
//            backendConfig = {
//                    @UIComponent.BackendConfig(values = {"film", "animation", "grain", "stillimage", "fastdecode", "zerolatency"})
//            }
//    )
//    public String ffmpegTune = "grain";
//
//    @UIComponent(
//            index = 40,
//            type = int_input,
//            required = true,
//            uiConfig = {
//                    @UIComponent.UIConfig(name = "min", value = "0"),
//                    @UIComponent.UIConfig(name = "max", value = "51")
//            }
//    )
//    public int ffmpegCrf = 28;

    @UIComponent(index = 60, type = ComponentType.drop_down, required = true)
    public SplitTimeInterval splitTimeout = SplitTimeInterval.m1;

    @UIComponent(index = 70, type = ComponentType.drop_down, required = true)
    public UseFrameRate useFrameRate = UseFrameRate.CALCULATED_FRAME_RATE;

}
