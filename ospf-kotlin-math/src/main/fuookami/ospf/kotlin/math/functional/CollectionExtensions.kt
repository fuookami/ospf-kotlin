/**
 * 集合扩展函数
 * Collection Extension Functions
 *
 * 丌Iterable、Sequence 和Map 提供求和与求平均值的扩展函数。
 * 这些扩展函数支持所有实现了 Arithmetic 接口的数值类型，确保类型安全的数值计算。
 *
 * Provides sum and average extension functions for Iterable, Sequence, and Map.
 * These extensions support all numeric types implementing the Arithmetic interface, ensuring type-safe numerical computations.
 *
 * 主要功能 / Main features:
 * - sum/sumOrNull: 计算集合元素之和 / Calculate sum of collection elements
 * - sumOf/sumOfOrNull: 通过提取器计算元素属性之和/ Calculate sum of element properties via extractor
 * - average/averageOrNull: 计算集合元素平均倌/ Calculate average of collection elements
 *
 * 边界情况处理 / Boundary case handling:
 * - 空集合调甌sum() 返回 zero / Empty collection returns zero for sum()
 * - 空集合调甌sumOrNull() 返回 null / Empty collection returns null for sumOrNull()
 * - 空集合调甌average() 抛出 NoSuchElementException / Empty collection throws NoSuchElementException for average()
 * - 空集合调甌averageOrNull() 返回 null / Empty collection returns null for averageOrNull()
 * - 包含 null 元素旌sumOrNull() 和averageOrNull() 返回 null / Returns null if contains null elements
 */
package fuookami.ospf.kotlin.math.functional

import fuookami.ospf.kotlin.math.algebra.concept.Arithmetic
import fuookami.ospf.kotlin.math.algebra.concept.ArithmeticConstants
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.resolveArithmeticConstants
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.math.operator.Div
import fuookami.ospf.kotlin.math.operator.Plus

fun <T> Iterable<T>.sum(constants: ArithmeticConstants<T>): T where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    for (element in this) {
        sum += element
    }
    return sum
}

inline fun <reified T> Iterable<T>.sum(): T where T : Arithmetic<T>, T : Plus<T, T> {
    return sum(resolveArithmeticConstants<T>("Collection"))
}

fun <T> Iterable<T?>.sumOrNull(constants: ArithmeticConstants<T>): T? where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    for (element in this) {
        if (element == null) {
            return null
        }
        sum += element
    }
    return sum
}

inline fun <reified T> Iterable<T?>.sumOrNull(): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return sumOrNull(resolveArithmeticConstants<T>("Collection"))
}

inline fun <T, U> Iterable<T>.sumOf(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = constants.zero
    for (element in this) {
        sum += extractor(element)
    }
    return sum
}

inline fun <T, reified U> Iterable<T>.sumOf(
    crossinline extractor: Extractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    return sumOf(resolveArithmeticConstants<U>("Collection"), extractor)
}

inline fun <T, U> Iterable<T>.sumOfOrNull(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(constants, extractor) { null }
}

inline fun <T, reified U> Iterable<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(resolveArithmeticConstants<U>("Collection"), extractor) { null }
}

inline fun <T, U> Iterable<T>.sumOfOrNull(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U?, T>,
    crossinline defaultValue: (T) -> U?
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = constants.zero
    for (element in this) {
        val value = element?.let { extractor(it) } ?: defaultValue(element)
        if (value == null) {
            return null
        }
        sum += value
    }
    return sum
}

inline fun <T, reified U> Iterable<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>,
    crossinline defaultValue: (T) -> U?
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(resolveArithmeticConstants<U>("Collection"), extractor, defaultValue)
}

fun <K, V> Map<K, V>.sum(constants: ArithmeticConstants<V>): V where V : Arithmetic<V>, V : Plus<V, V> {
    var sum = constants.zero
    for (element in this) {
        sum += element.value
    }
    return sum
}

inline fun <K, reified V> Map<K, V>.sum(): V where V : Arithmetic<V>, V : Plus<V, V> {
    return sum(resolveArithmeticConstants<V>("Collection"))
}

fun <K, V> Map<K, V?>.sumOrNull(constants: ArithmeticConstants<V>): V? where V : Arithmetic<V>, V : Plus<V, V> {
    var sum = constants.zero
    for (element in this) {
        val value = element.value ?: return null
        sum += value
    }
    return sum
}

inline fun <K, reified V> Map<K, V?>.sumOrNull(): V? where V : Arithmetic<V>, V : Plus<V, V> {
    return sumOrNull(resolveArithmeticConstants<V>("Collection"))
}

inline fun <K, V, T> Map<K, V>.sumOf(
    constants: ArithmeticConstants<T>,
    crossinline extractor: Extractor<T, Map.Entry<K, V>>
): T where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    for (element in this) {
        sum += extractor(element)
    }
    return sum
}

inline fun <K, V, reified T> Map<K, V>.sumOf(
    crossinline extractor: Extractor<T, Map.Entry<K, V>>
): T where T : Arithmetic<T>, T : Plus<T, T> {
    return sumOf(resolveArithmeticConstants<T>("Collection"), extractor)
}

inline fun <K, V, T> Map<K, V>.sumOfOrNull(
    constants: ArithmeticConstants<T>,
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return this.sumOfOrNull(constants, extractor) { null }
}

inline fun <K, V, reified T> Map<K, V>.sumOfOrNull(
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return this.sumOfOrNull(resolveArithmeticConstants<T>("Collection"), extractor) { null }
}

inline fun <K, V, T> Map<K, V>.sumOfOrNull(
    constants: ArithmeticConstants<T>,
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>,
    crossinline defaultValue: (Map.Entry<K, V>) -> T?
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    for (element in this) {
        val value = element.value?.let { extractor(element) } ?: defaultValue(element)
        if (value == null) {
            return null
        }
        sum += value
    }
    return sum
}

inline fun <K, V, reified T> Map<K, V>.sumOfOrNull(
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>,
    crossinline defaultValue: (Map.Entry<K, V>) -> T?
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return this.sumOfOrNull(resolveArithmeticConstants<T>("Collection"), extractor, defaultValue)
}

fun <T> Sequence<T>.sum(constants: ArithmeticConstants<T>): T where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    for (element in this) {
        sum += element
    }
    return sum
}

inline fun <reified T> Sequence<T>.sum(): T where T : Arithmetic<T>, T : Plus<T, T> {
    return sum(resolveArithmeticConstants<T>("Collection"))
}

fun <T> Sequence<T?>.sumOrNull(constants: ArithmeticConstants<T>): T? where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    for (element in this) {
        if (element == null) {
            return null
        }
        sum += element
    }
    return sum
}

inline fun <reified T> Sequence<T?>.sumOrNull(): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return sumOrNull(resolveArithmeticConstants<T>("Collection"))
}

inline fun <T, U> Sequence<T>.sumOf(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = constants.zero
    for (element in this) {
        sum += extractor(element)
    }
    return sum
}

inline fun <T, reified U> Sequence<T>.sumOf(
    crossinline extractor: Extractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    return sumOf(resolveArithmeticConstants<U>("Collection"), extractor)
}

inline fun <T, U> Sequence<T>.sumOfOrNull(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(constants, extractor) { null }
}

inline fun <T, reified U> Sequence<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(resolveArithmeticConstants<U>("Collection"), extractor) { null }
}

inline fun <T, U> Sequence<T>.sumOfOrNull(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U?, T>,
    crossinline defaultValue: (T) -> U? = { null }
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = constants.zero
    for (element in this) {
        val value = element?.let { extractor(it) } ?: defaultValue(element)
        if (value == null) {
            return null
        }
        sum += value
    }
    return sum
}

inline fun <T, reified U> Sequence<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>,
    crossinline defaultValue: (T) -> U? = { null }
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(resolveArithmeticConstants<U>("Collection"), extractor, defaultValue)
}

// Throw on empty collection to avoid division by zero / 空集合抛异常避免除零
fun <T> Iterable<T>.average(constants: ArithmeticConstants<T>): Flt64 where T : RealNumber<T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        throw NoSuchElementException("Cannot compute average of an empty collection.")
    }
    return sum.toFlt64() / count.toFlt64()
}

inline fun <reified T> Iterable<T>.average(): Flt64 where T : RealNumber<T> {
    return average(resolveArithmeticConstants<T>("Collection"))
}

// Throw on empty collection to avoid division by zero / 空集合抛异常避免除零
fun <T> Iterable<T>.average(constants: ArithmeticConstants<T>): T where T : RealNumber<T>, T : Div<T, T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        throw NoSuchElementException("Cannot compute average of an empty collection.")
    }
    return sum / count
}

inline fun <reified T> Iterable<T>.average(): T where T : RealNumber<T>, T : Div<T, T> {
    return average(resolveArithmeticConstants<T>("Collection"))
}

// Return null for empty collection to avoid division by zero / 空集合返回null避免除零
fun <T> Iterable<T?>.averageOrNull(constants: ArithmeticConstants<T>): Flt64? where T : RealNumber<T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        if (element == null) {
            return null
        }
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        return null
    }
    return sum.toFlt64() / count.toFlt64()
}

inline fun <reified T> Iterable<T?>.averageOrNull(): Flt64? where T : RealNumber<T> {
    return averageOrNull(resolveArithmeticConstants<T>("Collection"))
}

// Return null for empty collection to avoid division by zero / 空集合返回null避免除零
fun <T> Iterable<T?>.averageOrNull(constants: ArithmeticConstants<T>): T? where T : RealNumber<T>, T : Div<T, T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        if (element == null) {
            return null
        }
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        return null
    }
    return sum / count
}

inline fun <reified T> Iterable<T?>.averageOrNull(): T? where T : RealNumber<T>, T : Div<T, T> {
    return averageOrNull(resolveArithmeticConstants<T>("Collection"))
}

// Throw on empty map to avoid division by zero / 空Map抛异常避免除雌
fun <K, V> Map<K, V>.average(constants: ArithmeticConstants<V>): Flt64 where V : RealNumber<V> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element.value
        count += constants.one
    }
    if (count == constants.zero) {
        throw NoSuchElementException("Cannot compute average of an empty map.")
    }
    return sum.toFlt64() / count.toFlt64()
}

inline fun <K, reified V> Map<K, V>.average(): Flt64 where V : RealNumber<V> {
    return average(resolveArithmeticConstants<V>("Collection"))
}

// Throw on empty map to avoid division by zero / 空Map抛异常避免除雌
fun <K, V> Map<K, V>.average(constants: ArithmeticConstants<V>): V where V : RealNumber<V>, V : Div<V, V> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element.value
        count += constants.one
    }
    if (count == constants.zero) {
        throw NoSuchElementException("Cannot compute average of an empty map.")
    }
    return sum / count
}

inline fun <K, reified V> Map<K, V>.average(): V where V : RealNumber<V>, V : Div<V, V> {
    return average(resolveArithmeticConstants<V>("Collection"))
}

// Return null for empty map to avoid division by zero / 空Map返回null避免除零
fun <K, V> Map<K, V?>.averageOrNull(constants: ArithmeticConstants<V>): Flt64? where V : RealNumber<V> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        val value = element.value ?: return null
        sum += value
        count += constants.one
    }
    if (count == constants.zero) {
        return null
    }
    return sum.toFlt64() / count.toFlt64()
}

inline fun <K, reified V> Map<K, V?>.averageOrNull(): Flt64? where V : RealNumber<V> {
    return averageOrNull(resolveArithmeticConstants<V>("Collection"))
}

// Return null for empty map to avoid division by zero / 空Map返回null避免除零
fun <K, V> Map<K, V?>.averageOrNull(constants: ArithmeticConstants<V>): V? where V : RealNumber<V>, V : Div<V, V> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        val value = element.value ?: return null
        sum += value
        count += constants.one
    }
    if (count == constants.zero) {
        return null
    }
    return sum / count
}

inline fun <K, reified V> Map<K, V?>.averageOrNull(): V? where V : RealNumber<V>, V : Div<V, V> {
    return averageOrNull(resolveArithmeticConstants<V>("Collection"))
}

// Throw on empty sequence to avoid division by zero / 空序列抛异常避免除零
fun <T> Sequence<T>.average(constants: ArithmeticConstants<T>): Flt64 where T : RealNumber<T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        throw NoSuchElementException("Cannot compute average of an empty sequence.")
    }
    return sum.toFlt64() / count.toFlt64()
}

inline fun <reified T> Sequence<T>.average(): Flt64 where T : RealNumber<T> {
    return average(resolveArithmeticConstants<T>("Collection"))
}

// Throw on empty sequence to avoid division by zero / 空序列抛异常避免除零
fun <T> Sequence<T>.average(constants: ArithmeticConstants<T>): T where T : RealNumber<T>, T : Div<T, T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        throw NoSuchElementException("Cannot compute average of an empty sequence.")
    }
    return sum / count
}

inline fun <reified T> Sequence<T>.average(): T where T : RealNumber<T>, T : Div<T, T> {
    return average(resolveArithmeticConstants<T>("Collection"))
}

// Return null for empty sequence to avoid division by zero / 空序列返回null避免除零
fun <T> Sequence<T?>.averageOrNull(constants: ArithmeticConstants<T>): Flt64? where T : RealNumber<T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        if (element == null) {
            return null
        }
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        return null
    }
    return sum.toFlt64() / count.toFlt64()
}

inline fun <reified T> Sequence<T?>.averageOrNull(): Flt64? where T : RealNumber<T> {
    return averageOrNull(resolveArithmeticConstants<T>("Collection"))
}

// Return null for empty sequence to avoid division by zero / 空序列返回null避免除零
fun <T> Sequence<T?>.averageOrNull(constants: ArithmeticConstants<T>): T? where T : RealNumber<T>, T : Div<T, T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        if (element == null) {
            return null
        }
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        return null
    }
    return sum / count
}

inline fun <reified T> Sequence<T?>.averageOrNull(): T? where T : RealNumber<T>, T : Div<T, T> {
    return averageOrNull(resolveArithmeticConstants<T>("Collection"))
}