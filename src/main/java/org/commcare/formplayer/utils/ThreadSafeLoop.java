package org.commcare.formplayer.utils;

import java.util.function.Predicate;

public class ThreadSafeLoop {

    /**
     * Execute a body of code in a do/while loop until the predicate evaluates to true.
     *
     * This is made thread safe by aborting the loop if the thread is interrupted.
     *
     * @param body Body of the loop
     * @param repeatLoop Predicate that is used to evaluate the while condition
     * @param <T> Return type of the body
     */
    public static <T> T doWhile(CheckedSupplier<T> body, Predicate<T> repeatLoop) throws Exception {
        return doWhile(body, repeatLoop, Integer.MAX_VALUE);
    }

    public static <T> T doWhile(CheckedSupplier<T> body, Predicate<T> repeatLoop, int maxIterations) throws Exception {
        T result;
        int count = 0;
        do {
            count += 1;
            result = body.get();
        } while (!Thread.interrupted() && repeatLoop.test(result) && count < maxIterations);
        return result;
    }
}
