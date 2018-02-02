package util;

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
}
