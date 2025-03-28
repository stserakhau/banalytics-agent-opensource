package com.banalytics.box.module;

import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

public interface MediaConsumer extends Consumer<MediaConsumer.MediaData> {

    @RequiredArgsConstructor
    public static class MediaData {
        public final int streamId;

        public final MediaType mediaType;

        public final byte[] data;

        public final boolean mediaParamsChanged;

        public static MediaData of(int streamId, MediaType mediaType, byte[] data, boolean mediaParamsChanged) {
            return new MediaData(streamId, mediaType, data, mediaParamsChanged);
        }

        public enum MediaType {
            VIDEO, AUDIO
        }
    }
}
