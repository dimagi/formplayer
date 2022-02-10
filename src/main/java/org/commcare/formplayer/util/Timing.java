package org.commcare.formplayer.util;

public abstract class Timing {
    public abstract long durationInMs();

    public String getDurationBucket() {
        long timeInSeconds = durationInMs() / 1000;
        return getDurationBucket(timeInSeconds);
    }

    public String formatDuration() {
        return String.format("%.3fs", durationInMs() / 1000.);
    }

    public static Timing constant(final long durationInMs) {
        return new Timing() {
            @Override
            public long durationInMs() {
                return durationInMs;
            }
        };
    }

    public static String getDurationBucket(long timeInSeconds) {
        if (timeInSeconds < 1) {
            return "lt_001s";
        } else if (timeInSeconds < 5) {
            return "lt_005s";
        } else if (timeInSeconds < 20) {
            return "lt_020s";
        } else if (timeInSeconds < 60) {
            return "lt_060s";
        } else if (timeInSeconds < 120) {
            return "lt_120s";
        } else if (timeInSeconds < 300) {
            return "lt_300s";
        } else if (timeInSeconds < 600) {
            return "lt_600s";
        } else {
            return "over_600s";
        }
    }
}
