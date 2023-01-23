package org.commcare.formplayer.utils;

@FunctionalInterface
public interface CheckedRunnableInt<T> {
    Integer run();
}
