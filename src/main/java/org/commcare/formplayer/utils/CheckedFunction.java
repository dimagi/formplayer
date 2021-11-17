package org.commcare.formplayer.utils;

@FunctionalInterface
public interface CheckedFunction<T, R, X extends Throwable> {
    R apply(T var1) throws X;
}
