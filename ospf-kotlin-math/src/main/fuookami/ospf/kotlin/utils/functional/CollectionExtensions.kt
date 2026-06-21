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
 * - 空集合调用 average() 返回 Failed / Empty collection returns Failed for average()
 * - 空集合调用 averageOrNull() 返回 null / Empty collection returns null for averageOrNull()
 * - 包含 null 元素时 sumOrNull() 和 averageOrNull() 返回 null / Returns null if contains null elements
 */
package fuookami.ospf.kotlin.utils.functional

import kotlin.collections.iterator
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.operator.*

/**
 * 计算 Iterable 元素之和 / Calculate sum of Iterable elements
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @return 元素之和 / Sum of elements
 */
fun <T> Iterable<T>.sum(constants: ArithmeticConstants<T>): T where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    for (element in this) {
        sum += element
    }
    return sum
}

/** 计算 Iterable 元素之和（自动解析常量） / Calculate sum of Iterable elements (auto-resolve constants) */
inline fun <reified T> Iterable<T>.sum(): Ret<T> where T : Arithmetic<T>, T : Plus<T, T> {
    return resolveArithmeticConstantsSafe<T>("Collection").mapResolved { constants ->
        sum(constants)
    }
}

/**
 * 计算 Iterable 可空元素之和，含 null 时返回 null / Calculate sum of nullable Iterable elements, null if contains null
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @return 元素之和，含 null 时返回 null / Sum of elements, or null if contains null
 */
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
    val constants = resolveArithmeticConstantsOrNull<T>("Collection") ?: return null
    return sumOrNull(constants)
}

/**
 * 通过提取器计算 Iterable 元素属性之和 / Calculate sum of element properties via extractor
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @param extractor 从元素提取属性的函数 / Function to extract property from element
 * @return 属性之和 / Sum of extracted properties
 */
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

/**
 * 通过提取器计算 Iterable 元素属性之和（自动解析常量） / Calculate sum via extractor (auto-resolve)
 *
 * @param extractor 从元素提取属性的函数 / Function to extract property from element
 * @return 属性之和 / Sum of extracted properties
 */
inline fun <T, reified U> Iterable<T>.sumOf(
    crossinline extractor: Extractor<U, T>
): Ret<U> where U : Arithmetic<U>, U : Plus<U, U> {
    return resolveArithmeticConstantsSafe<U>("Collection").mapResolved { constants ->
        sumOf(constants, extractor)
    }
}

/**
 * 通过提取器计算 Iterable 元素可空属性之和，含 null 时返回 null / Calculate sum of nullable properties, null if null
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @param extractor 从元素提取可空属性的函数 / Function to extract nullable property from element
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
inline fun <T, U> Iterable<T>.sumOfOrNull(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(constants, extractor) { null }
}

/**
 * 通过提取器计算 Iterable 元素可空属性之和（自动解析常量） / Calculate sum of nullable properties (auto-resolve)
 *
 * @param extractor 从元素提取可空属性的函数 / Function to extract nullable property from element
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
inline fun <T, reified U> Iterable<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    val constants = resolveArithmeticConstantsOrNull<U>("Collection") ?: return null
    return this.sumOfOrNull(constants, extractor) { null }
}

/**
 * 通过提取器计算 Iterable 元素可空属性之和，支持自定义默认值 / Calculate sum of nullable properties with default value
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @param extractor 从元素提取可空属性的函数 / Function to extract nullable property from element
 * @param defaultValue 属性为 null 时的默认值函数 / Default value function when property is null
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
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

/**
 * 通过提取器计算 Iterable 元素可空属性之和（自动解析常量，自定义默认值） / Sum of nullable properties (auto-resolve, with default)
 *
 * @param extractor 从元素提取可空属性的函数 / Function to extract nullable property from element
 * @param defaultValue 属性为 null 时的默认值函数 / Default value function when property is null
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
inline fun <T, reified U> Iterable<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>,
    crossinline defaultValue: (T) -> U?
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    val constants = resolveArithmeticConstantsOrNull<U>("Collection") ?: return null
    return this.sumOfOrNull(constants, extractor, defaultValue)
}

/**
 * 计算 Map 值之和 / Calculate sum of Map values
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @return 值之和 / Sum of values
 */
fun <K, V> Map<K, V>.sum(constants: ArithmeticConstants<V>): V where V : Arithmetic<V>, V : Plus<V, V> {
    var sum = constants.zero
    for (element in this) {
        sum += element.value
    }
    return sum
}

/** 计算 Map 值之和（自动解析常量） / Calculate sum of Map values (auto-resolve constants) */
inline fun <K, reified V> Map<K, V>.sum(): Ret<V> where V : Arithmetic<V>, V : Plus<V, V> {
    return resolveArithmeticConstantsSafe<V>("Collection").mapResolved { constants ->
        sum(constants)
    }
}

/**
 * 计算 Map 可空值之和，含 null 时返回 null / Calculate sum of nullable Map values, null if contains null
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @return 值之和，含 null 时返回 null / Sum of values, or null if contains null
 */
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
    val constants = resolveArithmeticConstantsOrNull<V>("Collection") ?: return null
    return sumOrNull(constants)
}

/**
 * 通过提取器计算 Map 元素属性之和 / Calculate sum of Map entry properties via extractor
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @param extractor 从 Map 条目提取属性的函数 / Function to extract property from Map entry
 * @return 属性之和 / Sum of extracted properties
 */
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

/**
 * 通过提取器计算 Map 元素属性之和（自动解析常量） / Calculate sum of Map entry properties (auto-resolve)
 *
 * @param extractor 从 Map 条目提取属性的函数 / Function to extract property from Map entry
 * @return 属性之和 / Sum of extracted properties
 */
inline fun <K, V, reified T> Map<K, V>.sumOf(
    crossinline extractor: Extractor<T, Map.Entry<K, V>>
): Ret<T> where T : Arithmetic<T>, T : Plus<T, T> {
    return resolveArithmeticConstantsSafe<T>("Collection").mapResolved { constants ->
        sumOf(constants, extractor)
    }
}

/**
 * 通过提取器计算 Map 元素可空属性之和，含 null 时返回 null / Sum of nullable Map entry properties, null if null
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @param extractor 从 Map 条目提取可空属性的函数 / Function to extract nullable property from Map entry
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
inline fun <K, V, T> Map<K, V>.sumOfOrNull(
    constants: ArithmeticConstants<T>,
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return this.sumOfOrNull(constants, extractor) { null }
}

/**
 * 通过提取器计算 Map 元素可空属性之和（自动解析常量） / Sum of nullable Map entry properties (auto-resolve)
 *
 * @param extractor 从 Map 条目提取可空属性的函数 / Function to extract nullable property from Map entry
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
inline fun <K, V, reified T> Map<K, V>.sumOfOrNull(
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    val constants = resolveArithmeticConstantsOrNull<T>("Collection") ?: return null
    return this.sumOfOrNull(constants, extractor) { null }
}

/**
 * 通过提取器计算 Map 元素可空属性之和，支持自定义默认值 / Sum of nullable Map properties with default value
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @param extractor 从 Map 条目提取可空属性的函数 / Function to extract nullable property from Map entry
 * @param defaultValue 属性为 null 时的默认值函数 / Default value function when property is null
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
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

/**
 * 通过提取器计算 Map 元素可空属性之和（自动解析常量，自定义默认值） / Sum of nullable Map properties (auto-resolve, default)
 *
 * @param extractor 从 Map 条目提取可空属性的函数 / Function to extract nullable property from Map entry
 * @param defaultValue 属性为 null 时的默认值函数 / Default value function when property is null
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
inline fun <K, V, reified T> Map<K, V>.sumOfOrNull(
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>,
    crossinline defaultValue: (Map.Entry<K, V>) -> T?
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    val constants = resolveArithmeticConstantsOrNull<T>("Collection") ?: return null
    return this.sumOfOrNull(constants, extractor, defaultValue)
}

/**
 * 计算 Sequence 元素之和 / Calculate sum of Sequence elements
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @return 元素之和 / Sum of elements
 */
fun <T> Sequence<T>.sum(constants: ArithmeticConstants<T>): T where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    for (element in this) {
        sum += element
    }
    return sum
}

/** 计算 Sequence 元素之和（自动解析常量） / Calculate sum of Sequence elements (auto-resolve) */
inline fun <reified T> Sequence<T>.sum(): Ret<T> where T : Arithmetic<T>, T : Plus<T, T> {
    return resolveArithmeticConstantsSafe<T>("Collection").mapResolved { constants ->
        sum(constants)
    }
}

/**
 * 计算 Sequence 可空元素之和，含 null 时返回 null / Calculate sum of nullable Sequence elements, null if null
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @return 元素之和，含 null 时返回 null / Sum of elements, or null if contains null
 */
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
    val constants = resolveArithmeticConstantsOrNull<T>("Collection") ?: return null
    return sumOrNull(constants)
}

/**
 * 通过提取器计算 Sequence 元素属性之和 / Calculate sum of Sequence element properties via extractor
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @param extractor 从元素提取属性的函数 / Function to extract property from element
 * @return 属性之和 / Sum of extracted properties
 */
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

/**
 * 通过提取器计算 Sequence 元素属性之和（自动解析常量） / Calculate sum of Sequence properties (auto-resolve)
 *
 * @param extractor 从元素提取属性的函数 / Function to extract property from element
 * @return 属性之和 / Sum of extracted properties
 */
inline fun <T, reified U> Sequence<T>.sumOf(
    crossinline extractor: Extractor<U, T>
): Ret<U> where U : Arithmetic<U>, U : Plus<U, U> {
    return resolveArithmeticConstantsSafe<U>("Collection").mapResolved { constants ->
        sumOf(constants, extractor)
    }
}

/**
 * 通过提取器计算 Sequence 元素可空属性之和，含 null 时返回 null / Sum of nullable Sequence properties, null if null
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @param extractor 从元素提取可空属性的函数 / Function to extract nullable property from element
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
inline fun <T, U> Sequence<T>.sumOfOrNull(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(constants, extractor) { null }
}

/**
 * 通过提取器计算 Sequence 元素可空属性之和（自动解析常量） / Sum of nullable Sequence properties (auto-resolve)
 *
 * @param extractor 从元素提取可空属性的函数 / Function to extract nullable property from element
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
inline fun <T, reified U> Sequence<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    val constants = resolveArithmeticConstantsOrNull<U>("Collection") ?: return null
    return this.sumOfOrNull(constants, extractor) { null }
}

/**
 * 通过提取器计算 Sequence 元素可空属性之和，支持自定义默认值 / Sum of nullable Sequence properties with default
 *
 * @param constants 算术常量（提供零值） / Arithmetic constants (provides zero value)
 * @param extractor 从元素提取可空属性的函数 / Function to extract nullable property from element
 * @param defaultValue 属性为 null 时的默认值函数 / Default value function when property is null
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
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

/**
 * 通过提取器计算 Sequence 元素可空属性之和（自动解析常量，自定义默认值） / Sum of nullable Seq properties (auto-resolve)
 *
 * @param extractor 从元素提取可空属性的函数 / Function to extract nullable property from element
 * @param defaultValue 属性为 null 时的默认值函数 / Default value function when property is null
 * @return 属性之和，含 null 时返回 null / Sum of properties, or null if contains null
 */
inline fun <T, reified U> Sequence<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>,
    crossinline defaultValue: (T) -> U? = { null }
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    val constants = resolveArithmeticConstantsOrNull<U>("Collection") ?: return null
    return this.sumOfOrNull(constants, extractor, defaultValue)
}

// Return Failed on empty collection to avoid division by zero / 空集合返回 Failed 避免除零

/**
 * 计算 Iterable 元素平均值，返回 Flt64 / Calculate average of Iterable elements, returns Flt64
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 元素平均值结果（Flt64） / Average result of elements as Flt64
 */
fun <T> Iterable<T>.averageSafe(constants: ArithmeticConstants<T>): Ret<Flt64> where T : RealNumber<T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        return Failed(ErrorCode.DataEmpty, "Cannot compute average of an empty collection.")
    }
    return Ok(sum.toFlt64() / count.toFlt64())
}

/** 计算 Iterable 元素平均值，返回 Flt64 / Calculate average of Iterable elements, returns Flt64 */
fun <T> Iterable<T>.average(constants: ArithmeticConstants<T>): Ret<Flt64> where T : RealNumber<T> {
    return averageSafe(constants)
}

/** 计算 Iterable 元素平均值，返回 Flt64（自动解析常量） / Calculate average as Flt64 (auto-resolve) */
inline fun <reified T> Iterable<T>.averageSafe(): Ret<Flt64> where T : RealNumber<T> {
    return resolveArithmeticConstantsSafe<T>("Collection").flatMapResolved { constants ->
        averageSafe(constants)
    }
}

/** 计算 Iterable 元素平均值，返回 Flt64（自动解析常量） / Calculate average as Flt64 (auto-resolve) */
inline fun <reified T> Iterable<T>.average(): Ret<Flt64> where T : RealNumber<T> {
    return averageSafe()
}

// Return Failed on empty collection to avoid division by zero / 空集合返回 Failed 避免除零

/**
 * 计算 Iterable 元素平均值，返回同类型 / Calculate average of Iterable elements, returns same type
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 元素平均值结果（同类型） / Average result of elements as same type
 */
@JvmName("iterableAverageSafeAsSameType")
fun <T> Iterable<T>.averageSafe(constants: ArithmeticConstants<T>): Ret<T> where T : RealNumber<T>, T : Div<T, T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        return Failed(ErrorCode.DataEmpty, "Cannot compute average of an empty collection.")
    }
    return Ok(sum / count)
}

/** 计算 Iterable 元素平均值，返回同类型 / Calculate average of Iterable elements, returns same type */
@JvmName("iterableAverageAsSameType")
fun <T> Iterable<T>.average(constants: ArithmeticConstants<T>): Ret<T> where T : RealNumber<T>, T : Div<T, T> {
    return averageSafe(constants)
}

/** 计算 Iterable 元素平均值，返回同类型（自动解析常量） / Calculate average as same type (auto-resolve) */
@JvmName("iterableAverageSafeAsSameType")
inline fun <reified T> Iterable<T>.averageSafe(): Ret<T> where T : RealNumber<T>, T : Div<T, T> {
    return resolveArithmeticConstantsSafe<T>("Collection").flatMapResolved { constants ->
        averageSafe(constants)
    }
}

/** 计算 Iterable 元素平均值，返回同类型（自动解析常量） / Calculate average as same type (auto-resolve) */
@JvmName("iterableAverageAsSameType")
inline fun <reified T> Iterable<T>.average(): Ret<T> where T : RealNumber<T>, T : Div<T, T> {
    return averageSafe()
}

// Return null for empty collection to avoid division by zero / 空集合返回null避免除零

/**
 * 计算 Iterable 可空元素平均值，返回 Flt64，含 null 或空集合返回 null / Average of nullable elements as Flt64, null if null/empty
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 元素平均值（Flt64），含 null 或空集合返回 null / Average as Flt64, or null if null/empty
 */
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
    val constants = resolveArithmeticConstantsOrNull<T>("Collection") ?: return null
    return averageOrNull(constants)
}

// Return null for empty collection to avoid division by zero / 空集合返回null避免除零

/**
 * 计算 Iterable 可空元素平均值，返回同类型，含 null 或空集合返回 null / Average of nullable elements, null if null/empty
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 元素平均值（同类型），含 null 或空集合返回 null / Average as same type, or null if null/empty
 */
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
    val constants = resolveArithmeticConstantsOrNull<T>("Collection") ?: return null
    return averageOrNull(constants)
}

// Return Failed on empty map to avoid division by zero / 空 Map 返回 Failed 避免除零

/**
 * 计算 Map 值平均值，返回 Flt64 / Calculate average of Map values, returns Flt64
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 值平均值结果（Flt64） / Average result of values as Flt64
 */
fun <K, V> Map<K, V>.averageSafe(constants: ArithmeticConstants<V>): Ret<Flt64> where V : RealNumber<V> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element.value
        count += constants.one
    }
    if (count == constants.zero) {
        return Failed(ErrorCode.DataEmpty, "Cannot compute average of an empty map.")
    }
    return Ok(sum.toFlt64() / count.toFlt64())
}

/** 计算 Map 值平均值，返回 Flt64 / Calculate average of Map values, returns Flt64 */
fun <K, V> Map<K, V>.average(constants: ArithmeticConstants<V>): Ret<Flt64> where V : RealNumber<V> {
    return averageSafe(constants)
}

/** 计算 Map 值平均值，返回 Flt64（自动解析常量） / Average of Map values as Flt64 (auto-resolve) */
inline fun <K, reified V> Map<K, V>.averageSafe(): Ret<Flt64> where V : RealNumber<V> {
    return resolveArithmeticConstantsSafe<V>("Collection").flatMapResolved { constants ->
        averageSafe(constants)
    }
}

/** 计算 Map 值平均值，返回 Flt64（自动解析常量） / Average of Map values as Flt64 (auto-resolve) */
inline fun <K, reified V> Map<K, V>.average(): Ret<Flt64> where V : RealNumber<V> {
    return averageSafe()
}

// Return Failed on empty map to avoid division by zero / 空 Map 返回 Failed 避免除零

/**
 * 计算 Map 值平均值，返回同类型 / Calculate average of Map values, returns same type
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 值平均值结果（同类型） / Average result of values as same type
 */
@JvmName("mapAverageSafeAsSameType")
fun <K, V> Map<K, V>.averageSafe(constants: ArithmeticConstants<V>): Ret<V> where V : RealNumber<V>, V : Div<V, V> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element.value
        count += constants.one
    }
    if (count == constants.zero) {
        return Failed(ErrorCode.DataEmpty, "Cannot compute average of an empty map.")
    }
    return Ok(sum / count)
}

/** 计算 Map 值平均值，返回同类型 / Calculate average of Map values, returns same type */
@JvmName("mapAverageAsSameType")
fun <K, V> Map<K, V>.average(constants: ArithmeticConstants<V>): Ret<V> where V : RealNumber<V>, V : Div<V, V> {
    return averageSafe(constants)
}

/** 计算 Map 值平均值，返回同类型（自动解析常量） / Average of Map values as same type (auto-resolve) */
@JvmName("mapAverageSafeAsSameType")
inline fun <K, reified V> Map<K, V>.averageSafe(): Ret<V> where V : RealNumber<V>, V : Div<V, V> {
    return resolveArithmeticConstantsSafe<V>("Collection").flatMapResolved { constants ->
        averageSafe(constants)
    }
}

/** 计算 Map 值平均值，返回同类型（自动解析常量） / Average of Map values as same type (auto-resolve) */
@JvmName("mapAverageAsSameType")
inline fun <K, reified V> Map<K, V>.average(): Ret<V> where V : RealNumber<V>, V : Div<V, V> {
    return averageSafe()
}

// Return null for empty map to avoid division by zero / 空Map返回null避免除零

/**
 * 计算 Map 可空值平均值，返回 Flt64，含 null 或空返回 null / Average of nullable Map values as Flt64, null if null/empty
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 值平均值（Flt64），含 null 或空返回 null / Average as Flt64, or null if null/empty
 */
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
    val constants = resolveArithmeticConstantsOrNull<V>("Collection") ?: return null
    return averageOrNull(constants)
}

// Return null for empty map to avoid division by zero / 空Map返回null避免除零

/**
 * 计算 Map 可空值平均值，返回同类型，含 null 或空返回 null / Average of nullable Map values, null if null/empty
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 值平均值（同类型），含 null 或空返回 null / Average as same type, or null if null/empty
 */
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
    val constants = resolveArithmeticConstantsOrNull<V>("Collection") ?: return null
    return averageOrNull(constants)
}

// Return Failed on empty sequence to avoid division by zero / 空序列返回 Failed 避免除零

/**
 * 计算 Sequence 元素平均值，返回 Flt64 / Calculate average of Sequence elements, returns Flt64
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 元素平均值结果（Flt64） / Average result of elements as Flt64
 */
fun <T> Sequence<T>.averageSafe(constants: ArithmeticConstants<T>): Ret<Flt64> where T : RealNumber<T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        return Failed(ErrorCode.DataEmpty, "Cannot compute average of an empty sequence.")
    }
    return Ok(sum.toFlt64() / count.toFlt64())
}

/** 计算 Sequence 元素平均值，返回 Flt64 / Calculate average of Sequence elements, returns Flt64 */
fun <T> Sequence<T>.average(constants: ArithmeticConstants<T>): Ret<Flt64> where T : RealNumber<T> {
    return averageSafe(constants)
}

/** 计算 Sequence 元素平均值，返回 Flt64（自动解析常量） / Average of Sequence elements as Flt64 (auto-resolve) */
inline fun <reified T> Sequence<T>.averageSafe(): Ret<Flt64> where T : RealNumber<T> {
    return resolveArithmeticConstantsSafe<T>("Collection").flatMapResolved { constants ->
        averageSafe(constants)
    }
}

/** 计算 Sequence 元素平均值，返回 Flt64（自动解析常量） / Average of Sequence elements as Flt64 (auto-resolve) */
inline fun <reified T> Sequence<T>.average(): Ret<Flt64> where T : RealNumber<T> {
    return averageSafe()
}

// Return Failed on empty sequence to avoid division by zero / 空序列返回 Failed 避免除零

/**
 * 计算 Sequence 元素平均值，返回同类型 / Calculate average of Sequence elements, returns same type
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 元素平均值结果（同类型） / Average result of elements as same type
 */
@JvmName("sequenceAverageSafeAsSameType")
fun <T> Sequence<T>.averageSafe(constants: ArithmeticConstants<T>): Ret<T> where T : RealNumber<T>, T : Div<T, T> {
    var sum = constants.zero
    var count = constants.zero
    for (element in this) {
        sum += element
        count += constants.one
    }
    if (count == constants.zero) {
        return Failed(ErrorCode.DataEmpty, "Cannot compute average of an empty sequence.")
    }
    return Ok(sum / count)
}

/** 计算 Sequence 元素平均值，返回同类型 / Calculate average of Sequence elements, returns same type */
@JvmName("sequenceAverageAsSameType")
fun <T> Sequence<T>.average(constants: ArithmeticConstants<T>): Ret<T> where T : RealNumber<T>, T : Div<T, T> {
    return averageSafe(constants)
}

/** 计算 Sequence 元素平均值，返回同类型（自动解析常量） / Average of Sequence elements as same type (auto-resolve) */
@JvmName("sequenceAverageSafeAsSameType")
inline fun <reified T> Sequence<T>.averageSafe(): Ret<T> where T : RealNumber<T>, T : Div<T, T> {
    return resolveArithmeticConstantsSafe<T>("Collection").flatMapResolved { constants ->
        averageSafe(constants)
    }
}

/** 计算 Sequence 元素平均值，返回同类型（自动解析常量） / Average of Sequence elements as same type (auto-resolve) */
@JvmName("sequenceAverageAsSameType")
inline fun <reified T> Sequence<T>.average(): Ret<T> where T : RealNumber<T>, T : Div<T, T> {
    return averageSafe()
}

// Return null for empty sequence to avoid division by zero / 空序列返回null避免除零

/**
 * 计算 Sequence 可空元素平均值，返回 Flt64，含 null 或空返回 null / Average of nullable Sequence as Flt64, null if null/empty
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 元素平均值（Flt64），含 null 或空返回 null / Average as Flt64, or null if null/empty
 */
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
    val constants = resolveArithmeticConstantsOrNull<T>("Collection") ?: return null
    return averageOrNull(constants)
}

// Return null for empty sequence to avoid division by zero / 空序列返回null避免除零

/**
 * 计算 Sequence 可空元素平均值，返回同类型，含 null 或空返回 null / Average of nullable Sequence, null if null/empty
 *
 * @param constants 算术常量（提供零值和单位值） / Arithmetic constants (provides zero and one values)
 * @return 元素平均值（同类型），含 null 或空返回 null / Average as same type, or null if null/empty
 */
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
    val constants = resolveArithmeticConstantsOrNull<T>("Collection") ?: return null
    return averageOrNull(constants)
}
