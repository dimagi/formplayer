package util;

import java.util.concurrent.TimeUnit;

public class SimpleTimer extends Timing {
    private long startTimeInNs;
    private long endTimeInNs;

    public void start() {
        startTimeInNs = System.nanoTime();
    }

    public SimpleTimer end() {
        endTimeInNs = System.nanoTime();
        return this;
    }

    @Override
    public long durationInMs() {
        return (endTimeInNs - startTimeInNs) / (1000000);
    }

    public long durationInSeconds() {
        return TimeUnit.SECONDS.convert((endTimeInNs - startTimeInNs), TimeUnit.NANOSECONDS);
    }
}
