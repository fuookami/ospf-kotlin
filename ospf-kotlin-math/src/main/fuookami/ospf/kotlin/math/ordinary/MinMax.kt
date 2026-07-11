/**
 * 最小最大值
 * MinMax Functions
 *
 * 提供计算可比较类型的最小值、最大值和同时返回两者的功能。
 * min(lhs, rhs)：返回两个值中的较小者，min(a, b) = a if a < b else b。
 * max(lhs, rhs)：返回两个值中的较大者，max(a, b) = a if a > b else b。
 * minmax(lhs, rhs)：同时返回最小值和最大值，避免重复比较提高效率。
 * 支持可变参数和集合操作：min(lhs, ...), max(lhs, ...), Iterable.minMax() 等。
 * minOf/maxOf/minMaxOf：支持通过提取器从复杂对象中提取值进行比较。
 * minMaxWith/minMaxWithOrNull：支持使用自定义比较器。
 * 边界情况：空集合调用 minMax() 返回 Failed，对应的 OrNull 版本返回 null。
 * 要求类型实现 Ord 接口以支持比较操作（lt/leq/gt/geq）。
 *
 * Provides functionality for computing minimum, maximum, and both simultaneously for comparable types.
 * min(lhs, rhs): returns the smaller of two values, min(a, b) = a if a < b else b.
 * max(lhs, rhs): returns the larger of two values, max(a, b) = a if a > b else b.
 * minmax(lhs, rhs): returns both min and max simultaneously, avoiding redundant comparisons.
 * Supports variadic parameters and collection operations: min(lhs, ...), max(lhs, ...), Iterable.minMax().
 * minOf/maxOf/minMaxOf: supports extracting values from complex objects via extractor for comparison.
 * minMaxWith/minMaxWithOrNull: supports custom comparator usage.
 * Boundary cases: empty collection calls to minMax() return Failed,
 * corresponding OrNull versions return null.
 * Requires type to implement Ord interface for comparison operations (lt/leq/gt/geq).
*/
package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 返回两个值中的较小者
 * Return the smaller of two values
 *
 * @param lhs 左值 / Left value
 * @param rhs 右值 / Right value
 * @return 较小的值 / The smaller value
*/
fun <T : Ord<T>> min(lhs: T, rhs: T): T = if (lhs < rhs) lhs else rhs

/**
 * 返回多个值中的最小值
 * Return the minimum of multiple values
 *
 * @param lhs 第一个值 / First value
 * @param rhs 其余值 / Remaining values
 * @return 最小值 / Minimum value
*/
fun <T : Ord<T>> min(lhs: T, vararg rhs: T): T {
    var min = lhs
    for (e in rhs) {
        if (e leq min) {
            min = e
        }
    }
    return min
}

/**
 * 通过提取器返回多个值中的最小值
 * Return the minimum of multiple values via extractor
 *
 * @param lhs 第一个值 / First value
 * @param rhs 其余值 / Remaining values
 * @param extractor 值提取器 / Value extractor
 * @return 提取后的最小值 / Minimum extracted value
*/
inline fun <T : Ord<T>, U> minOf(
    lhs: U,
    vararg rhs: U,
    crossinline extractor: Extractor<T, U>
): T {
    var min = extractor(lhs)
    for (e in rhs) {
        val v = extractor(e)
        if (v leq min) {
            min = v
        }
    }
    return min
}

/**
 * 返回两个值中的较大者
 * Return the larger of two values
 *
 * @param lhs 左值 / Left value
 * @param rhs 右值 / Right value
 * @return 较大的值 / The larger value
*/
fun <T : Ord<T>> max(lhs: T, rhs: T): T = if (lhs > rhs) lhs else rhs

/**
 * 返回多个值中的最大值
 * Return the maximum of multiple values
 *
 * @param lhs 第一个值 / First value
 * @param rhs 其余值 / Remaining values
 * @return 最大值 / Maximum value
*/
fun <T : Ord<T>> max(lhs: T, vararg rhs: T): T {
    var max = lhs
    for (e in rhs) {
        if (e geq max) {
            max = e
        }
    }
    return max
}

/**
 * 通过提取器返回多个值中的最大值
 * Return the maximum of multiple values via extractor
 *
 * @param lhs 第一个值 / First value
 * @param rhs 其余值 / Remaining values
 * @param extractor 值提取器 / Value extractor
 * @return 提取后的最大值 / Maximum extracted value
*/
inline fun <T : Ord<T>, U> maxOf(
    lhs: U,
    vararg rhs: U,
    crossinline extractor: Extractor<T, U>
): T {
    var max = extractor(lhs)
    for (e in rhs) {
        val v = extractor(e)
        if (v gr max) {
            max = v
        }
    }
    return max
}

/**
 * 同时返回两个值的最小值和最大值
 * Return both min and max of two values simultaneously
 *
 * @param lhs 左值 / Left value
 * @param rhs 右值 / Right value
 * @return 最小值和最大值对 / Pair of minimum and maximum values
*/
fun <T : Ord<T>> minmax(lhs: T, rhs: T): Pair<T, T> = Pair(min(lhs, rhs), max(lhs, rhs))

/**
 * 同时返回多个值的最小值和最大值
 * Return both min and max of multiple values simultaneously
 *
 * @param lhs 第一个值 / First value
 * @param rhs 其余值 / Remaining values
 * @return 最小值和最大值对 / Pair of minimum and maximum values
*/
fun <T : Ord<T>> minMax(lhs: T, vararg rhs: T): Pair<T, T> {
    var min = lhs
    var max = lhs
    for (e in rhs) {
        if (e ls min) {
            min = e
        }
        if (e gr max) {
            max = e
        }
    }
    return Pair(min, max)
}

/**
 * 通过提取器同时返回多个值的最小值和最大值
 * Return both min and max of multiple values via extractor
 *
 * @param lhs 第一个值 / First value
 * @param rhs 其余值 / Remaining values
 * @param extractor 值提取器 / Value extractor
 * @return 提取后的最小值和最大值对 / Pair of minimum and maximum extracted values
*/
inline fun <T : Ord<T>, U> minMaxOf(
    lhs: U,
    vararg rhs: U,
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    var min = extractor(lhs)
    var max = min
    for (e in rhs) {
        val v = extractor(e)
        if (v ls min) {
            min = v
        }
        if (v gr max) {
            max = v
        }
    }
    return Pair(min, max)
}

/** 返回可迭代集合的最小值和最大值，空集合返回 Failed / Return min and max of iterable, Failed for empty collection */
fun <T : Ord<T>> Iterable<T>.minMaxSafe(): Ret<Pair<T, T>> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return Failed(ErrorCode.DataEmpty, "Cannot compute minMax of an empty collection.")
    }

    var min = iterator.next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (v ls min) {
            min = v
        }
        if (v gr max) {
            max = v
        }
    }
    return Ok(Pair(min, max))
}

/** 返回可迭代集合的最小值和最大值，空集合返回 Failed / Return min and max of iterable, Failed for empty collection */
fun <T : Ord<T>> Iterable<T>.minMax(): Ret<Pair<T, T>> {
    return minMaxSafe()
}

/** 返回可迭代集合的最小值和最大值，空集合返回 null / Return min and max of iterable, null for empty collection */
fun <T : Ord<T>> Iterable<T>.minMaxOrNull(): Pair<T, T>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }

    var min = iterator.next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (v ls min) {
            min = v
        }
        if (v gr max) {
            max = v
        }
    }
    return Pair(min, max)
}

/**
 * 通过提取器返回集合中最小和最大元素本身
 * Return the min and max elements themselves via extractor
 *
 * @param extractor 值提取器 / Value extractor
 * @return 最小和最大元素对 / Pair of minimum and maximum elements
*/
inline fun <T : Ord<T>, U> Iterable<U>.minMaxBy(
    crossinline extractor: Extractor<T, U>
): Pair<U, U> {
    val iterator = this.iterator()
    var minE = iterator().next()
    var maxE = minE
    var min = extractor(minE)
    var max = min
    while (iterator.hasNext()) {
        val e = iterator.next()
        val v = extractor(e)
        if (v leq min) {
            minE = e
            min = v
        }
        if (v geq max) {
            maxE = e
            max = v
        }
    }
    return Pair(minE, maxE)
}

/**
 * 通过提取器返回集合中最小和最大元素本身，空集合返回 null
 * Return min and max elements via extractor, null for empty
 *
 * @param extractor 值提取器 / Value extractor
 * @return 最小和最大元素对，空集合返回 null / Pair of min and max elements, or null for empty
*/
inline fun <T : Ord<T>, U> Iterable<U>.minMaxByOrNull(
    crossinline extractor: Extractor<T, U>
): Pair<U, U>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var minE = iterator().next()
    var maxE = minE
    var min = extractor(minE)
    var max = min
    while (iterator.hasNext()) {
        val e = iterator.next()
        val v = extractor(e)
        if (v leq min) {
            minE = e
            min = v
        }
        if (v geq max) {
            maxE = e
            max = v
        }
    }
    return Pair(minE, maxE)
}

/**
 * 通过提取器返回集合中最小和最大提取值
 * Return the min and max extracted values from collection
 *
 * @param extractor 值提取器 / Value extractor
 * @return 最小和最大提取值对 / Pair of minimum and maximum extracted values
*/
inline fun <T : Ord<T>, U> Iterable<U>.minMaxOf(
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    val iterator = this.iterator()
    var min = extractor(iterator().next())
    var max = min
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (v leq min) {
            min = v
        }
        if (v geq max) {
            max = v
        }
    }
    return Pair(min, max)
}

/**
 * 通过提取器返回集合中最小和最大提取值，空集合返回 null
 * Return min and max extracted values, null for empty
 *
 * @param extractor 值提取器 / Value extractor
 * @return 最小和最大提取值对，空集合返回 null / Pair of min and max extracted values, or null for empty
*/
inline fun <T : Ord<T>, U> Iterable<U>.minMaxOfOrNull(
    crossinline extractor: Extractor<T, U>
): Pair<T, T>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var min = extractor(iterator().next())
    var max = min
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (v leq min) {
            min = v
        }
        if (v geq max) {
            max = v
        }
    }
    return Pair(min, max)
}

/**
 * 使用自定义比较器返回集合的最小值和最大值
 * Return min and max using custom comparator
 *
 * @param comparator 自定义比较器 / Custom comparator
 * @return 最小值和最大值对 / Pair of minimum and maximum values
*/
fun <T> Iterable<T>.minMaxWith(comparator: kotlin.Comparator<T>): Pair<T, T> {
    val iterator = this.iterator()
    var min = iterator().next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (comparator.compare(v, min) < 0) {
            min = v
        }
        if (comparator.compare(v, max) > 0) {
            max = v
        }
    }
    return Pair(min, max)
}

/**
 * 使用自定义比较器返回集合的最小值和最大值，空集合返回 null
 * Return min and max using comparator, null for empty
 *
 * @param comparator 自定义比较器 / Custom comparator
 * @return 最小值和最大值对，空集合返回 null / Pair of min and max values, or null for empty
*/
fun <T> Iterable<T>.minMaxWithOrNull(comparator: kotlin.Comparator<T>): Pair<T, T>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var min = iterator().next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (comparator.compare(v, min) < 0) {
            min = v
        }
        if (comparator.compare(v, max) > 0) {
            max = v
        }
    }
    return Pair(min, max)
}
