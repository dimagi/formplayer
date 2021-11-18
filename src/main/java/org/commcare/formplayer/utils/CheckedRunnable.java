package org.commcare.formplayer.utils;

@FunctionalInterface
public interface CheckedRunnable<T> {
    void run() throws Exception;
}
