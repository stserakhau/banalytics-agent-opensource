package com.banalytics.box.module;

import com.banalytics.box.module.MediaConsumer.MediaData;
import com.banalytics.box.module.MediaConsumer.MediaData.MediaType;
import com.banalytics.box.module.webrtc.PortalWebRTCIntegrationConfiguration;
import com.banalytics.box.service.utility.TrafficControl;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RealTimeOutputStream extends OutputStream {
    private final PortalWebRTCIntegrationConfiguration configuration;

    private final MediaType mediaType;

    private int bufferSize;
    private byte[] buffer;
    private byte[] flushBuffer;
    private int bufferPosition = 0;

    public RealTimeOutputStream(PortalWebRTCIntegrationConfiguration configuration, MediaType mediaType) {
        this.configuration = configuration;
        PortalWebRTCIntegrationConfiguration.ImageSize is = configuration.getPreviewMediaStreamSize();
        this.currentWidth = is.width;
        this.currentHeight = is.height;
        this.mediaType = mediaType;
        if (mediaType == MediaType.AUDIO) {
            initializeBuffer(1600);
        } else {
            initializeBuffer(1000);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        buffer[this.bufferPosition] = (byte) b;
        this.bufferPosition++;
        if (bufferPosition == buffer.length) {
            flush();
        }
    }

    public void reset() throws IOException {
        bufferPosition = 0;
    }

    private Map<Integer, MediaConsumer> packetConsumers = new HashMap<>();

    public void addPacketConsumer(int streamId, MediaConsumer consumer) {
        Map<Integer, MediaConsumer> packetConsumers = new HashMap<>(this.packetConsumers);
        packetConsumers.put(streamId, consumer);
        this.packetConsumers = packetConsumers;
    }

    public void removePacketConsumer(int streamId) {
        Map<Integer, MediaConsumer> packetConsumers = new HashMap<>(this.packetConsumers);
        packetConsumers.remove(streamId);
        this.packetConsumers = packetConsumers;
    }

    public boolean hasConsumers() {
        return !packetConsumers.isEmpty();
    }


    @Override
    public void flush() throws IOException {
        flush(this.bufferSize);
    }

    int speedCounter = 0;
    long measurementStart = 0;

    int prevDataAmount = -1;
    long sendTimestamp = 0;

    int flushedWidth;
    int flushedHeight;

    private synchronized void flush(int newBufferSize) throws IOException {
        if (bufferPosition > 0) {
            byte[] sendData;
            if (bufferPosition == buffer.length) {
                sendData = this.flushBuffer;
            } else {
                sendData = new byte[bufferPosition];
            }
            System.arraycopy(this.buffer, 0, sendData, 0, bufferPosition);
            bufferPosition = 0;
            try {
                int dataAmount = sendData.length * packetConsumers.size();
                long now = System.currentTimeMillis();
                sendTimestamp = now;
                TrafficControl.INSTANCE.acquireGeneralResource(dataAmount, false);
                prevDataAmount = dataAmount;
            } catch (Throwable e) {
                throw new IOException(e);
            }

            packetConsumers.forEach((streamId, mediaConsumer) -> {
                boolean mediaParamsChanged = false;
                if (mediaType == MediaType.VIDEO) {
                    mediaParamsChanged = currentWidth != flushedWidth && currentHeight != flushedHeight;
                    flushedWidth = currentWidth;
                    flushedHeight = currentHeight;
                } else if (mediaType == MediaType.AUDIO) {
//                    mediaParamsChanged = false;
//                    log.info("Send audio packet: " + sendData.length);
                }
                mediaConsumer.accept(MediaData.of(streamId, mediaType, sendData, mediaParamsChanged));
            });

//            for (MediaConsumer consumer : packetConsumers) {
//                if (mediaType == MediaType.AUDIO) {
//                    log.info("Send audio packet: " + sendData.length);
//                }
//                consumer.accept(MediaData.of(streamId, mediaType, sendData));
//            }
            if (TrafficControl.INSTANCE.hasGeneralOnFlightOverhead()) {
                decreaseQuality(true);
            }
        }

        if (newBufferSize != this.bufferSize) {// if request to change buffer time to preset
            initializeBuffer(newBufferSize);   // then set new value
        } else {                               // otherwise
            if (speedCounter % 5 == 0) {      // align buffer to RT stream
                if (mediaType == MediaType.VIDEO) {
                    long now = System.currentTimeMillis();
                    if (this.measurementStart > 0) {
                        double measurementTime = (now - measurementStart) / 1000.0;
                        if (measurementTime > 0.2) {
                            double packetsPerSecond = speedCounter / measurementTime;
                            if (packetsPerSecond > 15) {
                                newBufferSize = this.bufferSize + 100;
                            } else if (packetsPerSecond < 10) {
                                newBufferSize = this.bufferSize - 50;
                            }
                            log.debug("Packets per second {} / {} = {} ({} bytes)", speedCounter, measurementTime, packetsPerSecond, newBufferSize);
                            initializeBuffer(newBufferSize);
                        }
                    }
                    this.measurementStart = now;
                }
                speedCounter = 0;
            }
        }

        this.speedCounter++;
    }

    private void initializeBuffer(int bufferSize) {
        bufferSize = bufferSize < 150 ? 150 : Math.min(bufferSize, 15000);
        this.bufferSize = bufferSize;
        this.buffer = new byte[bufferSize];
        this.flushBuffer = new byte[bufferSize];
    }

    public boolean propsChanged = false;
    public boolean bitrateChanged = false;

    public int requestedImageWidth = -1;
    public int requestedImageHeight = -1;

    public int imageWidth = -1;
    public int imageHeight = -1;
    public double k = 6.0 / 9.0;
    public double fps = 30;
    public boolean fpsChanged = false;


    public int currentWidth;
    public int currentHeight;


    public void setRequestedImageSize(int requestedImageWidth, int requestedImageHeight) {
        log.debug("Requested image size {}x{}", requestedImageWidth, requestedImageHeight);
        PortalWebRTCIntegrationConfiguration.ImageSize is = configuration.getPreviewMediaStreamSize();

        if (doUpdateImageSize(requestedImageWidth, requestedImageHeight)) {
            this.requestedImageWidth = is.width;
            this.requestedImageHeight = is.height;
            reCalcCurrentSize();
            updateMaxBitrate();
        }
    }

    public void setFps(double fps) {
        double deviation = this.fps > fps ? fps / this.fps : this.fps / fps;
        if (1 - deviation < 0.03) {
            this.fpsChanged = false;
            return;
        }
        this.fps = fps * 1.2;
        this.fpsChanged = true;
    }

    public void setStreamSize(int imageWidth, int imageHeight) {
        if (this.imageWidth != imageWidth || this.imageHeight != imageHeight) {
            if (doUpdateImageSize(imageWidth, this.imageWidth)) {
                this.imageWidth = imageWidth;
                this.imageHeight = imageHeight;
                reCalcCurrentSize();
                updateMaxBitrate();
            }
        }
    }

    //if image width request lower than 20% don't do anything
    //the same logic in media stream view
    private boolean doUpdateImageSize(int width, int height) {
        return requestedImageWidth != width || requestedImageHeight != height;
    }

    private void reCalcCurrentSize() {
        PortalWebRTCIntegrationConfiguration.ImageSize is = configuration.getPreviewMediaStreamSize();
//        if (requestedImageWidth < is.width || requestedImageHeight < is.height) {
//            requestedImageWidth = is.width;
//            requestedImageHeight = is.height;
//        }
        if (imageWidth < is.width || imageHeight < is.height) {
            currentWidth = imageWidth;
            currentHeight = imageHeight;
        } else {
            currentWidth = is.width;
            currentHeight = (int) (is.width * k);
        }
//
//        double scaling = 1;
//        if (requestedImageWidth < imageWidth) {
//            scaling = (double) requestedImageWidth / imageWidth;
//        }
//        currentWidth = (int) (scaling * imageWidth);
//        currentHeight = (int) (scaling * imageHeight);

        propsChanged = true;

//        printState();
    }

    int maxBitrate = 60000;
    int bitrate = 60000;

    public int bitrate() {
        return bitrate;
    }

    private void updateMaxBitrate() {
//        if (configuration.rtMediaQualityStrategy == PortalWebRTCIntegrationConfiguration.QualityStrategy.DYNAMIC) {
//            int units = (int) Math.ceil(currentWidth / 200.0);
//            units = (units == 0 ? 1 : units);
//            this.maxBitrate = units * configuration.maxBitRate;
//        } else {
            this.maxBitrate = configuration.maxBitrate;
//        }
//        this.maxBitrate = Math.min(this.maxBitrate, 1500000);

        this.bitrate = this.maxBitrate * 2 / 3;

        bitrateChanged = true;
//        int bufferSize = units * 600 + 200 * ((units + 1) / 2);
//        flush(bufferSize);
    }

    public void increaseQuality() {
        int currBitRate = bitrate;
        bitrate += bitrate / 3;
        if (bitrate > maxBitrate) {
            bitrate = maxBitrate;
        }

        if (currBitRate != bitrate) {
            bitrateChanged = true;
//            printState();
        }
    }

    public void decreaseQuality(boolean resetToMin) {
        if (resetToMin) {
            bitrate = 40000;
        } else {
            int currBitRate = bitrate;
            bitrate -= bitrate / 2;

            if (bitrate < 80000) {
                bitrate = 80000;
            }

            if (currBitRate != bitrate) {
                bitrateChanged = true;
//                printState();
            }
        }
    }

//    private void printState() {
//        log.info("{}x{} ~ {}x{} = {}x{} / {}", requestedImageWidth, requestedImageHeight, imageWidth, imageHeight, currentWidth, currentHeight, bitrate());
//    }
//
//    public void pushAudio(Buffer buffer) {
//        ShortBuffer in = (ShortBuffer)buffer;
//        ByteBuffer outBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
//        for (int i = 0; i < buffer.capacity(); i++) {
////            short val = (short) ((double) in.get(i) * SC16);
//            outBuffer.putShort(in.get(i));
//        }
//        for (MediaConsumer consumer : packetConsumers) {
//            consumer.accept(MediaData.of(MediaData.MediaType.AUDIO, outBuffer.array()));
//        }
//    }
}
