/**
 * 求和扩展
 * Sum Extension
 *
 * 为支持加法运算和具有零常量的类型提供求和扩展函数。
 * Provides sum extension functions for types that support addition and have a zero constant.
 *
 * 此扩展适用于任何满足以下条件的类型 T，
 * 1. 实现 Plus<T, T>（支挌+ 运算符）
 * 2. 具有包含零常量的伴生对象（通过 HasZero<T>，
 *
 * This extension works for any type T that:
 * 1. Implements Plus<T, T> (supports + operator)
 * 2. Has a companion object with a zero constant (via HasZero<T>)
 */
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.algebra.number.Int32
import fuookami.ospf.kotlin.math.operator.Plus

/**
 * 对可迭代对象中所有元素求和。
 * 如果可迭代对象为空，返回零。
 *
 * Sums all elements in an iterable.
 * Returns zero if the iterable is empty.
 *
 * @return 所有元素的和
 * @return The sum of all elements
 */
fun <T> Iterable<T>.sum(): T where T : Plus<T, T>, T : Arithmetic<T> {
    var result = firstOrNull()?.constants?.zero ?: return first().constants.zero
    for (element in this) {
        result += element
    }
    return result
}

/**
 * 对数组中所有元素求和。
 * 如果数组为空，抛出异常。
 *
 * Sums all elements in an array.
 * Throws an exception if the array is empty.
 *
 * @return 所有元素的和
 * @return The sum of all elements
 * @throws NoSuchElementException 如果数组为空
 * @throws NoSuchElementException If the array is empty
 */
fun <T> Array<out T>.sum(): T where T : Plus<T, T>, T : Arithmetic<T> {
    if (isEmpty()) {
        // Need to get zero from somewhere - this is a limitation / 需要从某处获取零值，这是一个限制
        // Caller should use sumWithZero for empty arrays / 调用方应对空数组使用 sumWithZero
        throw NoSuchElementException("Cannot compute sum of empty array without explicit zero.")
    }
    var result = this[0].constants.zero
    for (element in this) {
        result += element
    }
    return result
}

/**
 * 使用显式零值对可迭代对象求和，适用于空集合。
 *
 * Sums with explicit zero for empty collections.
 *
 * @param zero 空集合时的零倌
 * @param zero The zero value for empty collections
 * @return 所有元素的和
 * @return The sum of all elements
 */
fun <T> Iterable<T>.sumWithZero(zero: T): T where T : Plus<T, T> {
    var result = zero
    for (element in this) {
        result += element
    }
    return result
}

/**
 * 富Int32 值集合求和。
 *
 * Sums Int32 values.
 *
 * @return 所朌Int32 值的和
 * @return The sum of all Int32 values
 */
fun Iterable<Int32>.sumInt32(): Int32 {
    var result = Int32.zero
    for (element in this) {
        result += element
    }
    return result
}

/**
 * 富Int32 值集合求和。
 *
 * Sums Int32 values in a collection.
 *
 * @return 所朌Int32 值的和
 * @return The sum of all Int32 values
 */
fun Collection<Int32>.sum(): Int32 = sumInt32()
