package com.banalytics.box.api.integration.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

public class TimeUtil {
    public static ZoneId agentTimeZone() {
        return ZoneId.of(System.getProperty("user.timezone"));
    }

    public static LocalDateTime currentTimeInServerTz() {
        return LocalDateTime.now(userZone());
    }

    public static LocalDateTime fromMillisToServerTz(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), userZone());
    }

    public static LocalDateTime fromSecondsToServerTz(long seconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), userZone());
    }

    public static ZoneId userZone() {
        String userTimeZone = System.getProperty("user.timezone");
        return ZoneId.of(Objects.requireNonNullElse(userTimeZone, "UTC"));
    }

}
