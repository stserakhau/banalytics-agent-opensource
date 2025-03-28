package com.banalytics.box.module.constants;

import java.time.LocalDateTime;

import static com.banalytics.box.api.integration.utils.TimeUtil.currentTimeInServerTz;

public enum SplitTimeInterval {
    s10(10000),
    m1(60000);

    public final int intervalMillis;

    SplitTimeInterval(int rate) {
        this.intervalMillis = rate;
    }

    public static LocalDateTime ceilTimeout(LocalDateTime time, SplitTimeInterval interval) {
        int seconds = time.getSecond();
        switch (interval) {
            case m1:
                seconds = 0;
                break;
            case s10:
                seconds = (seconds / 10) * 10;
                break;
        }

        return LocalDateTime.of(
                time.getYear(),
                time.getMonth(),
                time.getDayOfMonth(),
                time.getHour(),
                time.getMinute(),
                seconds
        );
    }

    public static LocalDateTime floorTimeout(LocalDateTime time, SplitTimeInterval interval) {
        boolean addMinute = false;
        int minutes = time.getMinute();
        int seconds = time.getSecond();
        int nanos = time.getNano();
        switch (interval) {
            case m1:
                if (seconds > 0 || nanos > 0) {
                    addMinute = true;
                }
                seconds = 0;
                break;
            case s10:
                seconds = (seconds / 10) * 10;
                if (nanos > 0) {
                    seconds += 10;
                }
                if (seconds > 59) {
                    seconds = 0;
                    addMinute = true;
                }
                break;
        }

        LocalDateTime ldt = LocalDateTime.of(
                time.getYear(),
                time.getMonth(),
                time.getDayOfMonth(),
                time.getHour(),
                minutes,
                seconds
        );
        if (addMinute) {
            ldt = ldt.plusMinutes(1);
        }
        return ldt;
    }

    public static void main(String[] args) {
        LocalDateTime ldt = currentTimeInServerTz();
        System.out.println(ldt);
        System.out.println(ceilTimeout(ldt, m1));
        System.out.println(floorTimeout(ldt, m1));

        System.out.println(ceilTimeout(ldt, s10));
        System.out.println(floorTimeout(ldt, s10));
    }
}
