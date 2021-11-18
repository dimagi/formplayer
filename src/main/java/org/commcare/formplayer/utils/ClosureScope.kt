package org.commcare.formplayer.utils

/**
 * Store the value of a variable that can be modified in the scope of a closure.
 */
class ClosureScope<T> private constructor(
    @get:JvmName("get")
    @set:JvmName("set")
    var value: T? = null
) {

    fun isPresent(): Boolean {
        return value != null;
    }

    companion object {
        @JvmStatic
        fun <T> empty(): ClosureScope<T> {
            return ClosureScope()
        }

        @JvmStatic
        fun <T> of(value: T): ClosureScope<T> {
            return ClosureScope(value)
        }
    }
}
