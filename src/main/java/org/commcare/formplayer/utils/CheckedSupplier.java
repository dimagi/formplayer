package org.commcare.formplayer.utils;

@FunctionalInterface
public interface CheckedSupplier<T> {
    T get() throws Exception;
}
