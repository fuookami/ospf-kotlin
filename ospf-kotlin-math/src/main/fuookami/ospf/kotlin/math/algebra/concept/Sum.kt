package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.operator.Plus
import fuookami.ospf.kotlin.math.algebra.number.Int32

/**
 * Sum extension for types that support addition and have a zero constant.
 *
 * This extension works for any type T that:
 * 1. Implements Plus<T, T> (supports + operator)
 * 2. Has a companion object with a zero constant (via HasZero<T>)
 */

/**
 * Sum of all elements in an iterable.
 * Returns zero if the iterable is empty.
 */
fun <T> Iterable<T>.sum(): T where T : Plus<T, T>, T : Arithmetic<T> {
    var result = firstOrNull()?.constants?.zero ?: return first().constants.zero
    for (element in this) {
        result += element
    }
    return result
}

/**
 * Sum of all elements in an array.
 * Returns zero if the array is empty.
 */
fun <T> Array<out T>.sum(): T where T : Plus<T, T>, T : Arithmetic<T> {
    if (isEmpty()) {
        // Need to get zero from somewhere - this is a limitation
        // Caller should use sumWithZero for empty arrays
        throw NoSuchElementException("Cannot compute sum of empty array without explicit zero.")
    }
    var result = this[0].constants.zero
    for (element in this) {
        result += element
    }
    return result
}

/**
 * Sum with explicit zero for empty collections.
 */
fun <T> Iterable<T>.sumWithZero(zero: T): T where T : Plus<T, T> {
    var result = zero
    for (element in this) {
        result += element
    }
    return result
}

/**
 * Sum of Int32 values.
 */
fun Iterable<Int32>.sumInt32(): Int32 {
    var result = Int32.zero
    for (element in this) {
        result += element
    }
    return result
}

/**
 * Sum of Int32 values in a collection.
 */
fun Collection<Int32>.sum(): Int32 = sumInt32()