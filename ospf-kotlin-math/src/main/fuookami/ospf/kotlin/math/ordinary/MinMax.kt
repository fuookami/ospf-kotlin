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
 * 边界情况：空集合调用 min()、max()、minMax() 抛出 NoSuchElementException，
 * 对应的 orNull 版本返回 null。
 * 要求类型实现 Ord 接口以支持比较操作（lt/leq/gt/geq）。
 *
 * Provides functionality for computing minimum, maximum, and both simultaneously for comparable types.
 * min(lhs, rhs): returns the smaller of two values, min(a, b) = a if a < b else b.
 * max(lhs, rhs): returns the larger of two values, max(a, b) = a if a > b else b.
 * minmax(lhs, rhs): returns both min and max simultaneously, avoiding redundant comparisons.
 * Supports variadic parameters and collection operations: min(lhs, ...), max(lhs, ...), Iterable.minMax().
 * minOf/maxOf/minMaxOf: supports extracting values from complex objects via extractor for comparison.
 * minMaxWith/minMaxWithOrNull: supports custom comparator usage.
 * Boundary cases: empty collection calls to min(), max(), minMax() throw NoSuchElementException,
 * corresponding orNull versions return null.
 * Requires type to implement Ord interface for comparison operations (lt/leq/gt/geq).
 */
package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Ord

fun <T : Ord<T>> min(lhs: T, rhs: T): T = if (lhs < rhs) lhs else rhs

fun <T : Ord<T>> min(lhs: T, vararg rhs: T): T {
    var min = lhs
    for (e in rhs) {
        if (e leq min) {
            min = e
        }
    }
    return min
}

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

fun <T : Ord<T>> max(lhs: T, rhs: T): T = if (lhs > rhs) lhs else rhs

fun <T : Ord<T>> max(lhs: T, vararg rhs: T): T {
    var max = lhs
    for (e in rhs) {
        if (e geq max) {
            max = e
        }
    }
    return max
}

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

fun <T : Ord<T>> minmax(lhs: T, rhs: T): Pair<T, T> = Pair(min(lhs, rhs), max(lhs, rhs))

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

fun <T : Ord<T>> Iterable<T>.minMax(): Pair<T, T> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw NoSuchElementException()
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





