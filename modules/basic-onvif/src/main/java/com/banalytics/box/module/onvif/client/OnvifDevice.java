package com.banalytics.box.module.onvif.client;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.common.xmlschema.LSInputImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.onvif.ver10.device.wsdl.Device;
import org.onvif.ver10.deviceio.wsdl.DeviceIOPort;
import org.onvif.ver10.events.wsdl.EventPortType;
import org.onvif.ver10.events.wsdl.PullPointSubscription;
import org.onvif.ver10.media.wsdl.Media;
import org.onvif.ver10.recording.wsdl.RecordingPort;
import org.onvif.ver10.search.wsdl.SearchPort;
import org.onvif.ver20.imaging.wsdl.ImagingPort;
import org.onvif.ver20.media.wsdl.Media2;
import org.onvif.ver20.ptz.wsdl.PTZ;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.validation.SchemaFactory;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.spi.Provider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;


public class OnvifDevice {
    final String ip;

    final SecurityHandler securityHandler;

    private Device device;
    private PTZ ptz;
    private Media media;
    private Media2 media2;
    private ImagingPort imaging;
    private EventPortType events;
    private PullPointSubscription pullPointSubscription;
    private SearchPort search;
    private DeviceIOPort io;
    private RecordingPort recordingPort;

    public OnvifDevice(String ip, int port, String username, String password/*, OnvifConfiguration.TimeType timeType*/) {
        this.ip = ip;

        try {
//            Bus bus = BusFactory.getDefaultBus();
//            OASISCatalogManager catalogManager = OASISCatalogManager.getCatalogManager(bus);
//            catalogManager.loadCatalog(OnvifDevice.class.getResource("/META-INF/catalog.xml"));
//
//            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
//            factory.setBus(bus);
//            factory.setServiceClass(Device.class);
//            factory.setAddress(getDefaultDeviceURL(ip, port));
//            factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap/");// use soap 1.1
//            factory.getBus().setExtension(catalogManager, OASISCatalogManager.class);
//            Device device = (Device) factory.create();

//            BindingProvider bp1 = (BindingProvider) device;
//            ((SOAPBinding)bp1.getBinding()).setMTOMEnabled(false);


            Device device = Utils.getServiceProxy(
                    (BindingProvider) Service.create(
                            new QName("http://www.onvif.org/ver10/device/wsdl", "DeviceService")
                    ).getPort(new QName("http://www.onvif.org/ver10/device/wsdl", "DevicePort"), Device.class),
                    getDefaultDeviceURL(ip, port)
            ).create(Device.class);

            this.securityHandler =
                    isNotEmpty(username) ?
                            new SecurityHandler(
                                    username,
                                    isNotEmpty(password) ? password : "",
                                    /*timeType,*/ device)
                            : null;

            List<org.onvif.ver10.device.wsdl.Service> services = device.getServices(true);

            for (org.onvif.ver10.device.wsdl.Service service : services) {
                final String ns = service.getNamespace();
                final String xAddr = service.getXAddr();
                switch (ns) {
                    case "http://www.onvif.org/ver10/device/wsdl": {
                        this.device = Utils.getServiceProxy(
                                (BindingProvider) Service.create(
                                        new QName("http://www.onvif.org/ver10/device/wsdl", "DeviceService")
                                ).getPort(new QName("http://www.onvif.org/ver10/device/wsdl", "DevicePort"), Device.class),
                                update(xAddr, ip, port),
                                this.securityHandler
                        ).create(Device.class);
                        break;
                    }
                    case "http://www.onvif.org/ver20/ptz/wsdl": {
                        this.ptz = Utils.getServiceProxy(
                                (BindingProvider) Service.create(
                                        new QName(ns, "PtzService")
                                ).getPort(new QName(ns, "PtzPort"), PTZ.class),
                                update(xAddr, ip, port),
                                securityHandler
                        ).create(PTZ.class);
                        break;
                    }
                    case "http://www.onvif.org/ver10/media/wsdl": {
                        this.media = Utils.getServiceProxy(
                                (BindingProvider) Service.create(
                                        new QName(ns, "MediaService")
                                ).getPort(new QName(ns, "MediaPort"), org.onvif.ver10.media.wsdl.Media.class),
                                update(xAddr, ip, port),
                                securityHandler
                        ).create(Media.class);
                        break;
                    }
//                case "http://www.onvif.org/ver20/media/wsdl": {
//                    this.media2 = getServiceProxy(
//                            (BindingProvider) Service.create(
//                                    new QName(ns, "MediaService")
//                            ).getPort(new QName(ns, "MediaPort"), Media2.class),
//                            update(xAddr, ip, port),
//                            securityHandler
//                    ).create(Media2.class);
//                    break;
//                }
//                case "http://www.onvif.org/ver20/imaging/wsdl": {
//                    this.imaging =
//                            getServiceProxy(
//                                    (BindingProvider) Service.create(
//                                            new QName(ns, "ImagingService")
//                                    ).getPort(new QName(ns, "ImagingPort"), ImagingPort.class),
//                                    update(xAddr, ip, port),
//                                    securityHandler
//                            ).create(ImagingPort.class);
//                    break;
//                }
//                case "http://www.onvif.org/ver10/search/wsdl": {
//                    this.search =
//                            getServiceProxy(
//                                    (BindingProvider) Service.create(
//                                            new QName(ns, "SearchService")
//                                    ).getPort(new QName(ns, "SearchPort"), SearchPort.class),
//                                    update(xAddr, ip, port),
//                                    securityHandler
//                            ).create(SearchPort.class);
//                    break;
//                }
//                case "http://www.onvif.org/ver10/deviceIO/wsdl": {
//                    this.io =
//                            getServiceProxy(
//                                    (BindingProvider) Service.create(
//                                            new QName(ns, "DeviceIOService")
//                                    ).getPort(new QName(ns, "DeviceIOPort"), DeviceIOPort.class),
//                                    update(xAddr, ip, port),
//                                    securityHandler
//                            ).create(DeviceIOPort.class);
//                }
//                case "http://www.onvif.org/ver10/recording/wsdl": {
//                    this.recordingPort =
//                            getServiceProxy(
//                                    (BindingProvider) Service.create(
//                                            new QName(ns, "RecordingService")
//                                    ).getPort(new QName(ns, "RecordingPort"), RecordingPort.class),
//                                    update(xAddr, ip, port),
//                                    securityHandler
//                            ).create(RecordingPort.class);
//                }
//                case "http://www.onvif.org/ver10/events/wsdl": {
//                    EventService eventService = new EventService();
//                    this.events =
//                            getServiceProxy(
//                                    (BindingProvider) eventService.getEventPort(),
//                                    update(xAddr, ip, port),
//                                    securityHandler
//                            ).create(EventPortType.class);
//
//                    this.pullPointSubscription =
//                            getServiceProxy(
//                                    (BindingProvider) eventService.getPullPointSubscriptionPort(),
//                                    update(xAddr, ip, port),
//                                    securityHandler
//                            ).create(PullPointSubscription.class);
//                    break;
//                }
                }
            }
            //http://www.onvif.org/ver20/analytics/wsdl
            //http://www.onvif.org/ver10/recording/wsdl
            //http://192.168.1.101:10100/onvif/Replay
        } catch(Exception e) {
            throw new RuntimeException(e);
        } finally {
//            currentThread.setContextClassLoader(contextClassLoader);
        }
    }

    private static String getDefaultDeviceURL(String ip, int port) {
        return "http://" + ip + ":" + port + "/onvif/device_service";
    }

    private static String update(String url, String ip, int port) {
        try {
            URL u = new URL(url);
            return u.getProtocol() + "://" + ip + ":" + port + u.getPath();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public Device device() {
        return device;
    }

    public PTZ ptz() {
        return ptz;
    }

    public Media media() {
        return media;
    }

    public Media2 media2() {
        return media2;
    }

    public ImagingPort imaging() {
        return imaging;
    }

    public EventPortType events() {
        return events;
    }

    public PullPointSubscription pullPointSubscription() {
        return pullPointSubscription;
    }

    public SearchPort search() {
        return search;
    }

    public DeviceIOPort io() {
        return io;
    }

    public RecordingPort recording() {
        return recordingPort;
    }

    public static void main(String[] args) throws Exception {
        OnvifDevice d = new OnvifDevice("192.168.0.93", 80, "admin", "");

        // Создаем порт

        System.out.println("Device client created successfully!");
    }

//    public static void main(String[] args) throws Exception {
////        OnvifDevice d = new OnvifDevice("185.152.136.159", 10100, "admin", "iT12GFKpop");
//        OnvifDevice d = new OnvifDevice("192.168.0.92", 80, "admin", "iT12GFKpop");
//
//        SearchScope searchScope = new SearchScope();
//
//        String searchToken = d.search.findRecordings(searchScope, 100, DatatypeFactory.createDuration("PT20S"));
//
//        FindRecordingResultList res = d.search.getRecordingSearchResults(searchToken, 0, 100, DatatypeFactory.createDuration("PT20S"));
//
//        for(RecordingInformation ri : res.getRecordingInformation()){
//            String recToken = ri.getRecordingToken();
//
//            RecordingInformation ri1 = d.search.getRecordingInformation(recToken);
//            System.out.println(ri);
//        }
//
//        System.out.println();
//    }

//    public static void main(String[] args) {
//        try {
//            System.setProperty("xml.catalog.files", "D:\\work\\projects\\banalytics-opensource\\banalytics-agent\\modules\\basic-onvif\\src\\main\\resources\\META-INF\\catalog.xml");
//            String ip = "192.168.0.92"; // Замените на IP-адрес устройства
//            int port = 80;            // Замените на порт устройства
//
//            // Создайте сервис
//            QName serviceName = new QName("http://www.onvif.org/ver10/device/wsdl", "DeviceService");
//            QName portName = new QName("http://www.onvif.org/ver10/device/wsdl", "DevicePort");
//
//            Service service = Service.create(serviceName);
//            Device device = service.getPort(portName, Device.class);
//
//            // Настройка BindingProvider для URL устройства
//            String endpointURL = "http://" + ip + ":" + port + "/onvif/device_service";
//            ((BindingProvider) device).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointURL);
//
//            System.out.println("Device connected successfully!");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
