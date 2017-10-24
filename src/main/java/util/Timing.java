package util;

public abstract class Timing {
    public abstract long durationInMs();
    public String getDurationBucket() {
        long timeInS = durationInMs() / 1000;
        if (timeInS < 1) {
            return "lt_001s";
        } else if (timeInS < 5) {
            return "lt_005s";
        } else if (timeInS < 20) {
            return "lt_020s";
        } else if (timeInS < 60) {
            return "lt_060s";
        } else if (timeInS < 120) {
            return "lt_120s";
        } else {
            return "over_120s";
        }
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
}
