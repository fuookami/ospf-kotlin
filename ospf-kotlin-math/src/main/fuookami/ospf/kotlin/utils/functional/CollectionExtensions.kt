/**
 * 集合扩展函数
 * Collection Extension Functions
 *
 * 为 Iterable、Sequence 和 Map 提供求和与求平均值的扩展函数。
 * 这些扩展函数支持所有实现了 Arithmetic 接口的数值类型，确保类型安全的数值计算。
 *
 * Provides sum and average extension functions for Iterable, Sequence, and Map.
 * These extensions support all numeric types implementing the Arithmetic interface, ensuring type-safe numerical computations.
 *
 * 主要功能 / Main features:
 * - sum/sumOrNull: 计算集合元素之和 / Calculate sum of collection elements
 * - sumOf/sumOfOrNull: 通过提取器计算元素属性之和 / Calculate sum of element properties via extractor
 * - average/averageOrNull: 计算集合元素平均值 / Calculate average of collection elements
 *
 * 边界情况处理 / Boundary case handling:
 * - 空集合调用 sum() 返回 zero / Empty collection returns zero for sum()
 * - 空集合调用 sumOrNull() 返回 null / Empty collection returns null for sumOrNull()
 * - 空集合调用 average() 抛出 NoSuchElementException / Empty collection throws NoSuchElementException for average()
 * - 空集合调用 averageOrNull() 返回 null / Empty collection returns null for averageOrNull()
 * - 包含 null 元素时 sumOrNull() 和 averageOrNull() 返回 null / Returns null if contains null elements
 */
package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.math.algebra.concept.Arithmetic
import fuookami.ospf.kotlin.math.algebra.concept.ArithmeticConstants
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.resolveArithmeticConstants
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.operator.Div
import fuookami.ospf.kotlin.math.operator.Plus
import kotlin.collections.iterator

/** 计算 Iterable 元素之和 / Calculate sum of Iterable elements */
fun <T> Iterable<T>.sum(constants: ArithmeticConstants<T>): T where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    for (element in this) {
        sum += element
    }
    return sum
}

/** 计算 Iterable 元素之和（自动解析常量） / Calculate sum of Iterable elements (auto-resolve constants) */
inline fun <reified T> Iterable<T>.sum(): T where T : Arithmetic<T>, T : Plus<T, T> {
    return sum(resolveArithmeticConstants<T>("Collection"))
}

/** 计算 Iterable 可空元素之和，含 null 时返回 null / Calculate sum of nullable Iterable elements, null if contains null */
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

/** 计算 Iterable 可空元素之和（自动解析常量） / Calculate sum of nullable Iterable elements (auto-resolve) */
inline fun <reified T> Iterable<T?>.sumOrNull(): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return sumOrNull(resolveArithmeticConstants<T>("Collection"))
}

/** 通过提取器计算 Iterable 元素属性之和 / Calculate sum of element properties via extractor */
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

/** 通过提取器计算 Iterable 元素属性之和（自动解析常量） / Calculate sum via extractor (auto-resolve) */
inline fun <T, reified U> Iterable<T>.sumOf(
    crossinline extractor: Extractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    return sumOf(resolveArithmeticConstants<U>("Collection"), extractor)
}

/** 通过提取器计算 Iterable 元素可空属性之和，含 null 时返回 null / Calculate sum of nullable properties, null if null */
inline fun <T, U> Iterable<T>.sumOfOrNull(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(constants, extractor) { null }
}

/** 通过提取器计算 Iterable 元素可空属性之和（自动解析常量） / Calculate sum of nullable properties (auto-resolve) */
inline fun <T, reified U> Iterable<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(resolveArithmeticConstants<U>("Collection"), extractor) { null }
}

/** 通过提取器计算 Iterable 元素可空属性之和，支持自定义默认值 / Calculate sum of nullable properties with default value */
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

/** 通过提取器计算 Iterable 元素可空属性之和（自动解析常量，自定义默认值） / Sum of nullable properties (auto-resolve, with default) */
inline fun <T, reified U> Iterable<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>,
    crossinline defaultValue: (T) -> U?
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(resolveArithmeticConstants<U>("Collection"), extractor, defaultValue)
}

/** 计算 Map 值之和 / Calculate sum of Map values */
fun <K, V> Map<K, V>.sum(constants: ArithmeticConstants<V>): V where V : Arithmetic<V>, V : Plus<V, V> {
    var sum = constants.zero
    for (element in this) {
        sum += element.value
    }
    return sum
}

/** 计算 Map 值之和（自动解析常量） / Calculate sum of Map values (auto-resolve constants) */
inline fun <K, reified V> Map<K, V>.sum(): V where V : Arithmetic<V>, V : Plus<V, V> {
    return sum(resolveArithmeticConstants<V>("Collection"))
}

/** 计算 Map 可空值之和，含 null 时返回 null / Calculate sum of nullable Map values, null if contains null */
fun <K, V> Map<K, V?>.sumOrNull(constants: ArithmeticConstants<V>): V? where V : Arithmetic<V>, V : Plus<V, V> {
    var sum = constants.zero
    for (element in this) {
        val value = element.value ?: return null
        sum += value
    }
    return sum
}

/** 计算 Map 可空值之和（自动解析常量） / Calculate sum of nullable Map values (auto-resolve) */
inline fun <K, reified V> Map<K, V?>.sumOrNull(): V? where V : Arithmetic<V>, V : Plus<V, V> {
    return sumOrNull(resolveArithmeticConstants<V>("Collection"))
}

/** 通过提取器计算 Map 元素属性之和 / Calculate sum of Map entry properties via extractor */
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

/** 通过提取器计算 Map 元素属性之和（自动解析常量） / Calculate sum of Map entry properties (auto-resolve) */
inline fun <K, V, reified T> Map<K, V>.sumOf(
    crossinline extractor: Extractor<T, Map.Entry<K, V>>
): T where T : Arithmetic<T>, T : Plus<T, T> {
    return sumOf(resolveArithmeticConstants<T>("Collection"), extractor)
}

/** 通过提取器计算 Map 元素可空属性之和，含 null 时返回 null / Sum of nullable Map entry properties, null if null */
inline fun <K, V, T> Map<K, V>.sumOfOrNull(
    constants: ArithmeticConstants<T>,
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return this.sumOfOrNull(constants, extractor) { null }
}

/** 通过提取器计算 Map 元素可空属性之和（自动解析常量） / Sum of nullable Map entry properties (auto-resolve) */
inline fun <K, V, reified T> Map<K, V>.sumOfOrNull(
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return this.sumOfOrNull(resolveArithmeticConstants<T>("Collection"), extractor) { null }
}

/** 通过提取器计算 Map 元素可空属性之和，支持自定义默认值 / Sum of nullable Map properties with default value */
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

/** 通过提取器计算 Map 元素可空属性之和（自动解析常量，自定义默认值） / Sum of nullable Map properties (auto-resolve, default) */
inline fun <K, V, reified T> Map<K, V>.sumOfOrNull(
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>,
    crossinline defaultValue: (Map.Entry<K, V>) -> T?
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return this.sumOfOrNull(resolveArithmeticConstants<T>("Collection"), extractor, defaultValue)
}

/** 计算 Sequence 元素之和 / Calculate sum of Sequence elements */
fun <T> Sequence<T>.sum(constants: ArithmeticConstants<T>): T where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    for (element in this) {
        sum += element
    }
    return sum
}

/** 计算 Sequence 元素之和（自动解析常量） / Calculate sum of Sequence elements (auto-resolve) */
inline fun <reified T> Sequence<T>.sum(): T where T : Arithmetic<T>, T : Plus<T, T> {
    return sum(resolveArithmeticConstants<T>("Collection"))
}

/** 计算 Sequence 可空元素之和，含 null 时返回 null / Calculate sum of nullable Sequence elements, null if null */
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

/** 计算 Sequence 可空元素之和（自动解析常量） / Calculate sum of nullable Sequence elements (auto-resolve) */
inline fun <reified T> Sequence<T?>.sumOrNull(): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return sumOrNull(resolveArithmeticConstants<T>("Collection"))
}

/** 通过提取器计算 Sequence 元素属性之和 / Calculate sum of Sequence element properties via extractor */
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

/** 通过提取器计算 Sequence 元素属性之和（自动解析常量） / Calculate sum of Sequence properties (auto-resolve) */
inline fun <T, reified U> Sequence<T>.sumOf(
    crossinline extractor: Extractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    return sumOf(resolveArithmeticConstants<U>("Collection"), extractor)
}

/** 通过提取器计算 Sequence 元素可空属性之和，含 null 时返回 null / Sum of nullable Sequence properties, null if null */
inline fun <T, U> Sequence<T>.sumOfOrNull(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(constants, extractor) { null }
}

/** 通过提取器计算 Sequence 元素可空属性之和（自动解析常量） / Sum of nullable Sequence properties (auto-resolve) */
inline fun <T, reified U> Sequence<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(resolveArithmeticConstants<U>("Collection"), extractor) { null }
}

/** 通过提取器计算 Sequence 元素可空属性之和，支持自定义默认值 / Sum of nullable Sequence properties with default */
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

/** 通过提取器计算 Sequence 元素可空属性之和（自动解析常量，自定义默认值） / Sum of nullable Seq properties (auto-resolve) */
inline fun <T, reified U> Sequence<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>,
    crossinline defaultValue: (T) -> U? = { null }
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(resolveArithmeticConstants<U>("Collection"), extractor, defaultValue)
}

// Throw on empty collection to avoid division by zero / 空集合抛异常避免除零

/** 计算 Iterable 元素平均值，返回 Flt64 / Calculate average of Iterable elements, returns Flt64 */
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

/** 计算 Iterable 元素平均值，返回 Flt64（自动解析常量） / Calculate average as Flt64 (auto-resolve) */
inline fun <reified T> Iterable<T>.average(): Flt64 where T : RealNumber<T> {
    return average(resolveArithmeticConstants<T>("Collection"))
}

// Throw on empty collection to avoid division by zero / 空集合抛异常避免除零

/** 计算 Iterable 元素平均值，返回同类型 / Calculate average of Iterable elements, returns same type */
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

/** 计算 Iterable 元素平均值，返回同类型（自动解析常量） / Calculate average as same type (auto-resolve) */
inline fun <reified T> Iterable<T>.average(): T where T : RealNumber<T>, T : Div<T, T> {
    return average(resolveArithmeticConstants<T>("Collection"))
}

// Return null for empty collection to avoid division by zero / 空集合返回null避免除零

/** 计算 Iterable 可空元素平均值，返回 Flt64，含 null 或空集合返回 null / Average of nullable elements as Flt64, null if null/empty */
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

/** 计算 Iterable 可空元素平均值，返回 Flt64（自动解析常量） / Average of nullable elements as Flt64 (auto-resolve) */
inline fun <reified T> Iterable<T?>.averageOrNull(): Flt64? where T : RealNumber<T> {
    return averageOrNull(resolveArithmeticConstants<T>("Collection"))
}

// Return null for empty collection to avoid division by zero / 空集合返回null避免除零

/** 计算 Iterable 可空元素平均值，返回同类型，含 null 或空集合返回 null / Average of nullable elements, null if null/empty */
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

/** 计算 Iterable 可空元素平均值，返回同类型（自动解析常量） / Average of nullable elements (auto-resolve) */
inline fun <reified T> Iterable<T?>.averageOrNull(): T? where T : RealNumber<T>, T : Div<T, T> {
    return averageOrNull(resolveArithmeticConstants<T>("Collection"))
}

// Throw on empty map to avoid division by zero / 空Map抛异常避免除零

/** 计算 Map 值平均值，返回 Flt64 / Calculate average of Map values, returns Flt64 */
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

/** 计算 Map 值平均值，返回 Flt64（自动解析常量） / Average of Map values as Flt64 (auto-resolve) */
inline fun <K, reified V> Map<K, V>.average(): Flt64 where V : RealNumber<V> {
    return average(resolveArithmeticConstants<V>("Collection"))
}

// Throw on empty map to avoid division by zero / 空Map抛异常避免除零

/** 计算 Map 值平均值，返回同类型 / Calculate average of Map values, returns same type */
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

/** 计算 Map 值平均值，返回同类型（自动解析常量） / Average of Map values as same type (auto-resolve) */
inline fun <K, reified V> Map<K, V>.average(): V where V : RealNumber<V>, V : Div<V, V> {
    return average(resolveArithmeticConstants<V>("Collection"))
}

// Return null for empty map to avoid division by zero / 空Map返回null避免除零

/** 计算 Map 可空值平均值，返回 Flt64，含 null 或空返回 null / Average of nullable Map values as Flt64, null if null/empty */
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

/** 计算 Map 可空值平均值，返回 Flt64（自动解析常量） / Average of nullable Map values as Flt64 (auto-resolve) */
inline fun <K, reified V> Map<K, V?>.averageOrNull(): Flt64? where V : RealNumber<V> {
    return averageOrNull(resolveArithmeticConstants<V>("Collection"))
}

// Return null for empty map to avoid division by zero / 空Map返回null避免除零

/** 计算 Map 可空值平均值，返回同类型，含 null 或空返回 null / Average of nullable Map values, null if null/empty */
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

/** 计算 Map 可空值平均值，返回同类型（自动解析常量） / Average of nullable Map values (auto-resolve) */
inline fun <K, reified V> Map<K, V?>.averageOrNull(): V? where V : RealNumber<V>, V : Div<V, V> {
    return averageOrNull(resolveArithmeticConstants<V>("Collection"))
}

// Throw on empty sequence to avoid division by zero / 空序列抛异常避免除零

/** 计算 Sequence 元素平均值，返回 Flt64 / Calculate average of Sequence elements, returns Flt64 */
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

/** 计算 Sequence 元素平均值，返回 Flt64（自动解析常量） / Average of Sequence elements as Flt64 (auto-resolve) */
inline fun <reified T> Sequence<T>.average(): Flt64 where T : RealNumber<T> {
    return average(resolveArithmeticConstants<T>("Collection"))
}

// Throw on empty sequence to avoid division by zero / 空序列抛异常避免除零

/** 计算 Sequence 元素平均值，返回同类型 / Calculate average of Sequence elements, returns same type */
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

/** 计算 Sequence 元素平均值，返回同类型（自动解析常量） / Average of Sequence elements as same type (auto-resolve) */
inline fun <reified T> Sequence<T>.average(): T where T : RealNumber<T>, T : Div<T, T> {
    return average(resolveArithmeticConstants<T>("Collection"))
}

// Return null for empty sequence to avoid division by zero / 空序列返回null避免除零

/** 计算 Sequence 可空元素平均值，返回 Flt64，含 null 或空返回 null / Average of nullable Sequence as Flt64, null if null/empty */
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

/** 计算 Sequence 可空元素平均值，返回 Flt64（自动解析常量） / Average of nullable Sequence as Flt64 (auto-resolve) */
inline fun <reified T> Sequence<T?>.averageOrNull(): Flt64? where T : RealNumber<T> {
    return averageOrNull(resolveArithmeticConstants<T>("Collection"))
}

// Return null for empty sequence to avoid division by zero / 空序列返回null避免除零

/** 计算 Sequence 可空元素平均值，返回同类型，含 null 或空返回 null / Average of nullable Sequence, null if null/empty */
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

/** 计算 Sequence 可空元素平均值，返回同类型（自动解析常量） / Average of nullable Sequence (auto-resolve) */
inline fun <reified T> Sequence<T?>.averageOrNull(): T? where T : RealNumber<T>, T : Div<T, T> {
    return averageOrNull(resolveArithmeticConstants<T>("Collection"))
}
