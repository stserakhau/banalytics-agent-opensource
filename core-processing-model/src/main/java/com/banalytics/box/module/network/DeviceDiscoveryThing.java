package com.banalytics.box.module.network;

import com.banalytics.box.api.integration.webrtc.channel.environment.ThingApiCallReq;
import com.banalytics.box.module.AbstractThing;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.Singleton;
import com.banalytics.box.module.Thing;
import com.banalytics.box.service.SystemThreadsService;
import com.banalytics.box.service.discovery.DeviceDiscoveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.util.Map;
import java.util.Set;

import static com.banalytics.box.module.State.RUN;

@Slf4j
@Order(Thing.StarUpOrder.INTEGRATION)
public class DeviceDiscoveryThing extends AbstractThing<DeviceDiscoveryConfiguration> implements Singleton {
    private static final String PARAM_ADDRESS = "address";
    private static final String PARAM_MASK = "mask";

    private DeviceDiscoveryService deviceDiscoveryService;

    public DeviceDiscoveryThing(BoxEngine engine) {
        super(engine);
    }

    @Override
    protected void doInit() throws Exception {
    }

    @Override
    protected void doStart() throws Exception {
        deviceDiscoveryService = engine.getBean(DeviceDiscoveryService.class);
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    public Set<String> apiMethodsPermissions() {
        return Set.of(PERMISSION_READ, "scan*");
    }

    @Override
    public Object call(Map<String, Object> params) throws Exception {
        if (getState() != RUN) {
            throw new Exception("error.thing.notInitialized");
        }
        String method = (String) params.get(ThingApiCallReq.PARAM_METHOD);

        switch (method) {
            case "readAvailableSubnets" -> {
                return deviceDiscoveryService.availableSubnets();
            }
            case "scanSubnet" -> {

                String addr = (String) params.get(PARAM_ADDRESS);
                String mask = (String) params.get(PARAM_MASK);
                SystemThreadsService.execute(this, () -> {
                    try {
                        deviceDiscoveryService.scanSubnets(addr, mask, configuration.pingTimeout);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
                return "";
            }
            case "readScanSubnetResult" -> {
                return deviceDiscoveryService.listDiscoveredDevices();
            }
        }

        throw new Exception("Invalid method: " + method);
    }
}
