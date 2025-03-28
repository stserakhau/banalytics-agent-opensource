package com.banalytics.box.service.windows;

import com.banalytics.box.module.model.discovery.AudioDevice;
import com.banalytics.box.module.model.discovery.LocalDevice;
import com.banalytics.box.module.model.discovery.VideoDevice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceInfoAccumulator {
    public Map<String, AudioDevice> audioDevices = new HashMap<>();
    public Map<String, VideoDevice> videoDevices = new HashMap<>();

    LocalDevice currentDevice;

    public void process(List<String> lines) {
        int videoDeviceIndex = 0;
        int audioDeviceIndex = 0;
        for (String details : lines) {
            if (!details.contains("[dshow")) {
                continue;
            }

            if (details.contains("Alternative name")) {
                int camNameStart = details.indexOf("\"");
                int camNameEnd = details.lastIndexOf("\"");
                currentDevice.alternativeName = details.substring(camNameStart + 1, camNameEnd);
            } else {
                int camNameStart = details.indexOf("\"");
                int camNameEnd = details.lastIndexOf("\"");
                if (camNameStart == -1 || camNameEnd == -1) {
                    continue;
                }
                String name = details.substring(camNameStart + 1, camNameEnd);

                int typeStart = details.indexOf("(", camNameEnd + 1);
                int typeEnd = details.lastIndexOf(")");

                String type = details.substring(typeStart + 1, typeEnd);

                if ("audio".equals(type)) {
                    currentDevice = new AudioDevice();
                    audioDeviceIndex++;
                    currentDevice.name = name + " (" + audioDeviceIndex + ")";
                } else {
                    currentDevice = new VideoDevice();
                    videoDeviceIndex++;
                    currentDevice.name = name + " (" + videoDeviceIndex + ")";
                }

            }

            if (currentDevice.isCompleted()) {
                if (currentDevice instanceof AudioDevice ad) {
                    audioDevices.put(currentDevice.alternativeName, ad);
                } else if (currentDevice instanceof VideoDevice vd) {
                    videoDevices.put(currentDevice.alternativeName, vd);
                }
            }
        }
    }
}