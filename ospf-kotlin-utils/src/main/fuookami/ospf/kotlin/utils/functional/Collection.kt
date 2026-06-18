@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.utils.functional

import java.util.*
import kotlin.time.Duration
import kotlin.random.Random

/**
 * 集合扩展函数
 *
 * Extension functions for collection operations including filtering, mapping, sorting, and aggregation.
 * Provides functional programming utilities for working with collections.
 * 集合操作的扩展函数，包括过滤、映射、排序和聚合。
 * 提供用于处理集合的函数式编程工具。
 *
 * Key features:
 * - Shuffle algorithms with custom random generators
 * - Filter operations with type-safe predicates
 * - Min/max finding with various comparator types
 * - Sorting with custom comparators
 * - Association operations with null-safety
 *
 * 主要特性：
 * - 使用自定义随机生成器的洗牌算法
 * - 类型安全谓词的过滤操作
 * - 使用各种比较器类型查找最小/最大值
 * - 使用自定义比较器排序
 * - 具有空值安全性的关联操作
 */

/** 根据键从键值对列表中获取值 / Get value by key from a pair list */
operator fun <K, V> List<Pair<K, V>>.get(key: K): V? {
    return this.firstOrNull { it.first == key }?.second
}

// Fisher-Yates shuffle algorithm / Fisher-Yates 洗牌算法
// Ensures uniform distribution / 保证均匀分布
/** 使用自定义随机生成器进行洗牌 / Shuffle the list using a custom random generator */
fun <T> List<T>.shuffle(
    randomGenerator: Generator<Int> = { Random.nextInt(0, this.size) }
): List<T> {
    val list = this.toMutableList()
    for (i in list.lastIndex downTo 1) {
        // j must be in range [0, i] for Fisher-Yates / j 必须在 [0, i] 范围内
        val j = randomGenerator()!! % (i + 1)
        val temp = list[i]
        list[i] = list[j]
        list[j] = temp
    }
    return list
}

/** 将迭代器收集为列表 / Collect iterator to a list */
fun <T> Iterator<T>.collect(): List<T> {
    return this.collectTo(ArrayList())
}

/** 将迭代器收集到指定的可变集合中 / Collect iterator into the specified mutable collection */
fun <T, M : MutableCollection<T>> Iterator<T>.collectTo(m: M): M {
    while (this.hasNext()) {
        m.add(this.next())
    }
    return m
}

/** 对每个元素应用转换，返回最后一个非 null 结果，无则返回 null / Apply transform to each element, return the last non-null result or null */
inline fun <R, T> Iterable<T>.lastNotNullOfOrNull(
    crossinline extractor: Extractor<R?, T>
): R? {
    var result: R? = null

    val iterator = this.iterator()
    while (iterator.hasNext()) {
        result = extractor(iterator.next())
    }

    return result
}

/** 从列表末尾向前迭代，返回最后一个非 null 转换结果，无则返回 null / Iterate from the end, return the last non-null transform result or null */
inline fun <R, T> List<T>.lastNotNullOfOrNull(
    crossinline extractor: Extractor<R?, T>
): R? {
    // Start from the end of the list / 从列表末尾开始
    val iterator = this.listIterator(this.size)
    while (iterator.hasPrevious()) {
        val result = extractor(iterator.previous())

        if (result != null) {
            return result
        }
    }

    return null
}

/** 带索引过滤元素 / Filter elements with index predicate */
inline fun <T> Iterable<T>.filterIndexed(
    crossinline predicate: IndexedPredicate<T>
): List<T> {
    return this.mapIndexedNotNull { i, value ->
        if (predicate(i, value)) {
            value
        } else {
            null
        }
    }
}

/** 过滤非 null 元素并满足谓词条件 / Filter non-null elements satisfying the predicate */
inline fun <T> Iterable<T?>.filterNotNull(
    crossinline predicate: Predicate<T>
): List<T> {
    return this.filterNotNullTo(ArrayList(), predicate)
}

/** 过滤非 null 元素并满足谓词条件，添加到目标集合 / Filter non-null elements satisfying the predicate into destination */
inline fun <T, C : MutableCollection<T>> Iterable<T?>.filterNotNullTo(
    destination: C,
    crossinline predicate: Predicate<T>
): C {
    for (element in this.iterator()) {
        if (element != null && predicate(element)) {
            destination.add(element)
        }
    }
    return destination
}

/** 过滤掉指定类型的元素 / Filter out elements of type U */
inline fun <reified U, T> Iterable<T>.filterIsNotInstance(): List<T> {
    return this.filterIsNotInstanceTo<U, T, MutableList<T>>(ArrayList())
}

/** 过滤掉指定类型的元素，添加到目标集合 / Filter out elements of type U into destination */
inline fun <reified U, T, C : MutableCollection<in T>> Iterable<T>.filterIsNotInstanceTo(
    destination: C
): C {
    for (element in this.iterator()) {
        if (element !is U) {
            destination.add(element)
        }
    }
    return destination
}

/** 过滤掉指定类型且满足谓词的元素 / Filter out elements of type U satisfying the predicate */
inline fun <reified U, T> Iterable<T>.filterIsNotInstance(
    crossinline predicate: Predicate<U>
): List<T> {
    return this.filterIsNotInstanceTo<U, T, MutableList<T>>(ArrayList(), predicate)
}

/** 过滤掉指定类型且满足谓词的元素，添加到目标集合 / Filter out elements of type U satisfying the predicate into destination */
inline fun <reified U, T, C : MutableCollection<in T>> Iterable<T>.filterIsNotInstanceTo(
    destination: C,
    crossinline predicate: Predicate<U>
): C {
    for (element in this.iterator()) {
        if (element !is U || predicate(element)) {
            destination.add(element)
        }
    }
    return destination
}

/** 过滤指定类型且满足谓词的元素 / Filter elements of type U satisfying the predicate */
inline fun <reified U, T> Iterable<T>.filterIsInstance(
    crossinline predicate: Predicate<U>
): List<U> {
    return this.filterIsInstanceTo(ArrayList(), predicate)
}

/** 过滤指定类型且满足谓词的元素，添加到目标集合 / Filter elements of type U satisfying the predicate into destination */
inline fun <reified U, T, C : MutableCollection<in U>> Iterable<T>.filterIsInstanceTo(
    destination: C,
    crossinline predicate: Predicate<U>
): C {
    for (element in this.iterator()) {
        if (element is U && predicate(element)) {
            destination.add(element)
        }
    }
    return destination
}

/** 映射并展平，跳过 null 结果 / Flat map skipping null results */
inline fun <R, T> Iterable<T>.flatMapNotNull(
    crossinline extractor: Extractor<Iterable<R?>, T>
): List<R> {
    return this.flatMapNotNullTo(ArrayList(), extractor)
}

/** 映射并展平，跳过 null 结果，添加到目标集合 / Flat map skipping null results into destination */
inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapNotNullTo(
    destination: C,
    crossinline extractor: Extractor<Iterable<R?>, T>
): C {
    for (element in this.iterator()) {
        destination.addAll(extractor(element).filterNotNull())
    }
    return destination
}

/** 使用比较器求最大值，集合为空时抛异常 / Find maximum using comparator, throw if empty */
inline fun <T> Iterable<T>.maxWithComparator(
    crossinline comparator: Comparator<T>
): T {
    return this.maxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用偏比较器求最大值，集合为空时抛异常 / Find maximum using partial comparator, throw if empty */
inline fun <T> Iterable<T>.maxWithPartialComparator(
    crossinline comparator: PartialComparator<T>
): T {
    return this.maxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用三路比较器求最大值，集合为空时抛异常 / Find maximum using three-way comparator, throw if empty */
inline fun <T> Iterable<T>.maxWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

/** 使用偏三路比较器求最大值，集合为空时抛异常 / Find maximum using partial three-way comparator, throw if empty */
inline fun <T> Iterable<T>.maxWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

/** 使用比较器求最大值，集合为空时返回 null / Find maximum using comparator, return null if empty */
inline fun <T> Iterable<T>.maxWithComparatorOrNull(
    crossinline comparator: Comparator<T>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用偏比较器求最大值，集合为空时返回 null / Find maximum using partial comparator, return null if empty */
inline fun <T> Iterable<T>.maxWithPartialComparatorOrNull(
    crossinline comparator: PartialComparator<T>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用三路比较器求最大值，集合为空时返回 null / Find maximum using three-way comparator, return null if empty */
inline fun <T> Iterable<T>.maxWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

/** 使用偏三路比较器求最大值，集合为空时返回 null / Find maximum using partial three-way comparator, return null if empty */
inline fun <T> Iterable<T>.maxWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

/** 使用比较器和提取器求最大值，集合为空时抛异常 / Find maximum using comparator and extractor, throw if empty */
inline fun <T, U> Iterable<U>.maxOfWithComparator(
    crossinline comparator: Comparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用偏比较器和提取器求最大值，集合为空时抛异常 / Find maximum using partial comparator and extractor, throw if empty */
inline fun <T, U> Iterable<U>.maxOfWithPartialComparator(
    crossinline comparator: PartialComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用三路比较器和提取器求最大值，集合为空时抛异常 / Find maximum using three-way comparator and extractor, throw if empty */
inline fun <T, U> Iterable<U>.maxOfWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

/** 使用偏三路比较器和提取器求最大值，集合为空时抛异常 / Find maximum using partial three-way comparator and extractor, throw if empty */
inline fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

/** 使用比较器和提取器求最大值，集合为空时返回 null / Find maximum using comparator and extractor, return null if empty */
inline fun <T, U> Iterable<U>.maxOfWithComparatorOrNull(
    crossinline comparator: Comparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用偏比较器和提取器求最大值，集合为空时返回 null / Find maximum using partial comparator and extractor, return null if empty */
inline fun <T, U> Iterable<U>.maxOfWithPartialComparatorOrNull(
    crossinline comparator: PartialComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用三路比较器和提取器求最大值，集合为空时返回 null / Find maximum using three-way comparator and extractor, return null if empty */
inline fun <T, U> Iterable<U>.maxOfWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

/** 使用偏三路比较器和提取器求最大值，集合为空时返回 null / Find maximum using partial three-way comparator and extractor, return null if empty */
inline fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

/** 使用比较器求最小值，集合为空时抛异常 / Find minimum using comparator, throw if empty */
inline fun <T> Iterable<T>.minWithComparator(
    crossinline comparator: Comparator<T>
): T {
    return this.minOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用偏比较器求最小值，集合为空时抛异常 / Find minimum using partial comparator, throw if empty */
inline fun <T> Iterable<T>.minWithPartialComparator(
    crossinline comparator: PartialComparator<T>
): T {
    return this.minOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用三路比较器求最小值，集合为空时抛异常 / Find minimum using three-way comparator, throw if empty */
inline fun <T> Iterable<T>.minWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

/** 使用偏三路比较器求最小值，集合为空时抛异常 / Find minimum using partial three-way comparator, throw if empty */
inline fun <T> Iterable<T>.minWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

/** 使用比较器求最小值，集合为空时返回 null / Find minimum using comparator, return null if empty */
inline fun <T> Iterable<T>.minWithComparatorOrNull(
    crossinline comparator: Comparator<T>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用偏比较器求最小值，集合为空时返回 null / Find minimum using partial comparator, return null if empty */
inline fun <T> Iterable<T>.minWithPartialComparatorOrNull(
    crossinline comparator: PartialComparator<T>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用三路比较器求最小值，集合为空时返回 null / Find minimum using three-way comparator, return null if empty */
inline fun <T> Iterable<T>.minWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

/** 使用偏三路比较器求最小值，集合为空时返回 null / Find minimum using partial three-way comparator, return null if empty */
inline fun <T> Iterable<T>.minWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

/** 使用比较器和提取器求最小值，集合为空时抛异常 / Find minimum using comparator and extractor, throw if empty */
inline fun <T, U> Iterable<U>.minOfWithComparator(
    crossinline comparator: Comparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.minOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用偏比较器和提取器求最小值，集合为空时抛异常 / Find minimum using partial comparator and extractor, throw if empty */
inline fun <T, U> Iterable<U>.minOfWithPartialComparator(
    crossinline comparator: PartialComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.minOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用三路比较器和提取器求最小值，集合为空时抛异常 / Find minimum using three-way comparator and extractor, throw if empty */
inline fun <T, U> Iterable<U>.minOfWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

/** 使用偏三路比较器和提取器求最小值，集合为空时抛异常 / Find minimum using partial three-way comparator and extractor, throw if empty */
inline fun <T, U> Iterable<U>.minOfWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

/** 使用比较器和提取器求最小值，集合为空时返回 null / Find minimum using comparator and extractor, return null if empty */
inline fun <T, U> Iterable<U>.minOfWithComparatorOrNull(
    crossinline comparator: Comparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.minOfWithOrNull({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用偏比较器和提取器求最小值，集合为空时返回 null / Find minimum using partial comparator and extractor, return null if empty */
inline fun <T, U> Iterable<U>.minOfWithPartialComparatorOrNull(
    crossinline comparator: PartialComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用三路比较器和提取器求最小值，集合为空时返回 null / Find minimum using three-way comparator and extractor, return null if empty */
inline fun <T, U> Iterable<U>.minOfWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

/** 使用偏三路比较器和提取器求最小值，集合为空时返回 null / Find minimum using partial three-way comparator and extractor, return null if empty */
inline fun <T, U> Iterable<U>.minOfWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

/** 同时获取最小值和最大值，集合为空时返回 null / Find both min and max simultaneously, return null if empty */
fun <T : Comparable<T>> Iterable<T>.minMaxOrNull(): Pair<T, T>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }

    var min = iterator.next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (v < min) {
            min = v
        }
        if (v > max) {
            max = v
        }
    }
    return Pair(min, max)
}

/** 按提取值同时获取最小和最大元素，集合为空时返回 null / Find min and max elements by extractor, return null if empty */
inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByOrNull(
    crossinline extractor: Extractor<R, T>
): Pair<T, T>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }

    val v = iterator.next()
    val r = extractor(v)
    var min = Pair(v, r)
    var max = Pair(v, r)
    while (iterator.hasNext()) {
        val v0 = iterator.next()
        val r0 = extractor(v0)
        if (r0 < min.second) {
            min = Pair(v0, r0)
        }
        if (r0 > max.second) {
            max = Pair(v0, r0)
        }
    }
    return Pair(min.first, max.first)
}

/** 同时获取提取值的最小值和最大值，集合为空时返回 null / Find min and max of extracted value, return null if empty */
inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfOrNull(
    crossinline extractor: Extractor<R, T>
): Pair<R, R>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }

    var min = extractor(iterator.next())
    var max = min
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (v < min) {
            min = v
        }
        if (v > max) {
            max = v
        }
    }
    return Pair(min, max)
}

/** 使用比较器同时获取最小值和最大值，集合为空时抛异常 / Find min and max using comparator, throw if empty */
inline fun <T> Iterable<T>.minMaxWithComparator(
    crossinline comparator: Comparator<T>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用偏比较器同时获取最小值和最大值，集合为空时抛异常 / Find min and max using partial comparator, throw if empty */
inline fun <T> Iterable<T>.minMaxWithPartialComparator(
    crossinline comparator: PartialComparator<T>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用三路比较器同时获取最小值和最大值，集合为空时抛异常 / Find min and max using three-way comparator, throw if empty */
inline fun <T> Iterable<T>.minMaxWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

/** 使用偏三路比较器同时获取最小值和最大值，集合为空时抛异常 / Find min and max using partial three-way comparator, throw if empty */
inline fun <T> Iterable<T>.minMaxWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

/** 使用比较器同时获取最小值和最大值，集合为空时返回 null / Find min and max using comparator, return null if empty */
inline fun <T> Iterable<T>.minMaxWithComparatorOrNull(
    crossinline comparator: Comparator<T>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用偏比较器同时获取最小值和最大值，集合为空时返回 null / Find min and max using partial comparator, return null if empty */
inline fun <T> Iterable<T>.minMaxWithPartialComparatorOrNull(
    crossinline comparator: PartialComparator<T>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }) { it }
}

/** 使用三路比较器同时获取最小值和最大值，集合为空时返回 null / Find min and max using three-way comparator, return null if empty */
inline fun <T> Iterable<T>.minMaxWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

/** 使用偏三路比较器同时获取最小值和最大值，集合为空时返回 null / Find min and max using partial three-way comparator, return null if empty */
inline fun <T> Iterable<T>.minMaxWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

/** 使用比较器和提取器同时获取提取值的最小最大值，集合为空时抛异常 / Find min and max of extracted value using comparator, throw if empty */
inline fun <T, U> Iterable<U>.minMaxOfWithComparator(
    crossinline comparator: Comparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用偏比较器和提取器同时获取提取值的最小最大值，集合为空时抛异常 / Find min and max of extracted value using partial comparator, throw if empty */
inline fun <T, U> Iterable<U>.minMaxOfWithPartialComparator(
    crossinline comparator: PartialComparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用三路比较器和提取器同时获取提取值的最小最大值，集合为空时抛异常 / Find min and max of extracted value using three-way comparator, throw if empty */
inline fun <T, U> Iterable<U>.minMaxOfWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

/** 使用偏三路比较器和提取器同时获取提取值的最小最大值，集合为空时抛异常 / Find min and max of extracted value using partial three-way comparator, throw if empty */
inline fun <T, U> Iterable<U>.minMaxOfWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

/** 使用比较器和提取器同时获取提取值的最小最大值，集合为空时返回 null / Find min and max of extracted value using comparator, return null if empty */
inline fun <T, U> Iterable<U>.minMaxOfWithComparatorOrNull(
    crossinline comparator: Comparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用偏比较器和提取器同时获取提取值的最小最大值，集合为空时返回 null / Find min and max of extracted value using partial comparator, return null if empty */
inline fun <T, U> Iterable<U>.minMaxOfWithPartialComparatorOrNull(
    crossinline comparator: PartialComparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }, extractor)
}

/** 使用三路比较器和提取器同时获取提取值的最小最大值，集合为空时返回 null / Find min and max of extracted value using three-way comparator, return null if empty */
inline fun <T, U> Iterable<U>.minMaxOfWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

/** 使用偏三路比较器和提取器同时获取提取值的最小最大值，集合为空时返回 null / Find min and max of extracted value using partial three-way comparator, return null if empty */
inline fun <T, U> Iterable<U>.minMaxOfWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

/** 关联非 null 键值对 / Associate non-null key-value pairs */
inline fun <K, V, T> Iterable<T>.associateNotNull(
    crossinline extractor: Extractor<Pair<K, V>?, T>
): Map<K, V> {
    return this.associateNotNullTo(LinkedHashMap(), extractor)
}

/** 关联非 null 键值对到目标 Map / Associate non-null key-value pairs to destination map */
inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.associateNotNullTo(
    destination: M,
    crossinline extractor: Extractor<Pair<K, V>?, T>
): M {
    return this.mapNotNull(extractor).toMap(destination)
}

/** 按非 null 键关联 / Associate by non-null key */
inline fun <K, T> Iterable<T>.associateByNotNull(
    crossinline extractor: Extractor<K?, T>
): Map<K, T> {
    return this.associateByNotNullTo(LinkedHashMap(), extractor)
}

/** 按非 null 键关联到目标 Map / Associate by non-null key to destination map */
inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.associateByNotNullTo(
    destination: M,
    crossinline extractor: Extractor<K?, T>
): M {
    return this.mapNotNull { extractor(it)?.to(it) }.toMap(destination)
}

/** 关联非 null 值 / Associate with non-null value */
inline fun <V, T> Iterable<T>.associateWithNotNull(
    crossinline extractor: Extractor<V?, T>
): Map<T, V> {
    return this.associateWithNotNullTo(LinkedHashMap(), extractor)
}

/** 关联非 null 值到目标 Map / Associate with non-null value to destination map */
inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.associateWithNotNullTo(
    destination: M,
    crossinline extractor: Extractor<V?, T>
): M {
    return this.mapNotNull { extractor(it)?.let { value -> it to value } }.toMap(destination)
}

/** 使用比较器原地排序 / Sort in-place using comparator */
inline fun <T> MutableList<T>.sortWithComparator(
    crossinline comparator: Comparator<T>
) {
    this.sortWith { lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }
}

/** 使用偏比较器原地排序 / Sort in-place using partial comparator */
inline fun <T> MutableList<T>.sortWithPartialComparator(
    crossinline comparator: PartialComparator<T>
) {
    this.sortWith { lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }
}

/** 使用三路比较器原地排序 / Sort in-place using three-way comparator */
inline fun <T> MutableList<T>.sortWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
) {
    this.sortWith { lhs, rhs ->
        comparator(lhs, rhs).value
    }
}

/** 使用偏三路比较器原地排序 / Sort in-place using partial three-way comparator */
inline fun <T> MutableList<T>.sortWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
) {
    this.sortWith { lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }
}

/** 使用比较器排序并返回新列表 / Return sorted new list using comparator */
inline fun <T> Iterable<T>.sortedWithComparator(
    crossinline comparator: Comparator<T>
): List<T> {
    return this.sortedWith { lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }
}

/** 使用偏比较器排序并返回新列表 / Return sorted new list using partial comparator */
inline fun <T> Iterable<T>.sortedWithPartialComparator(
    crossinline comparator: PartialComparator<T>
): List<T> {
    return this.sortedWith { lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }
}

/** 使用三路比较器排序并返回新列表 / Return sorted new list using three-way comparator */
inline fun <T> Iterable<T>.sortedWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): List<T> {
    return this.sortedWith { lhs, rhs ->
        comparator(lhs, rhs).value
    }
}

/** 使用偏三路比较器排序并返回新列表 / Return sorted new list using partial three-way comparator */
inline fun <T> Iterable<T>.sortedWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): List<T> {
    return this.sortedWith { lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }
}

/** 使用比较器转换为排序集 / Convert to sorted set using comparator */
inline fun <T> Iterable<T>.toSortedSetWithComparator(
    crossinline comparator: Comparator<T>
): SortedSet<T> {
    return this.toSortedSet { lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }
}

/** 使用偏比较器转换为排序集 / Convert to sorted set using partial comparator */
inline fun <T> Iterable<T>.toSortedSetWithPartialComparator(
    crossinline comparator: PartialComparator<T>
): SortedSet<T> {
    return this.toSortedSet { lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }
}

/** 使用三路比较器转换为排序集 / Convert to sorted set using three-way comparator */
inline fun <T> Iterable<T>.toSortedSetWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): SortedSet<T> {
    return this.toSortedSet { lhs, rhs -> comparator(lhs, rhs).value }
}

/** 使用偏三路比较器转换为排序集 / Convert to sorted set using partial three-way comparator */
inline fun <T> Iterable<T>.toSortedSetWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): SortedSet<T> {
    return this.toSortedSet { lhs, rhs -> comparator(lhs, rhs)?.value ?: 0 }
}

/** 使用比较器转换为排序映射 / Convert to sorted map using comparator */
inline fun <K, V> Map<K, V>.toSortedMapWithComparator(
    crossinline comparator: Comparator<K>
): SortedMap<K, V> {
    return this.toSortedMap { lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }
}

/** 使用偏比较器转换为排序映射 / Convert to sorted map using partial comparator */
inline fun <K, V> Map<K, V>.toSortedMapWithPartialComparator(
    crossinline comparator: PartialComparator<K>
): SortedMap<K, V> {
    return this.toSortedMap { lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }
}

/** 使用三路比较器转换为排序映射 / Convert to sorted map using three-way comparator */
inline fun <K, V> Map<K, V>.toSortedMapWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<K>
): SortedMap<K, V> {
    return this.toSortedMap { lhs, rhs -> comparator(lhs, rhs).value }
}

/** 使用偏三路比较器转换为排序映射 / Convert to sorted map using partial three-way comparator */
inline fun <K, V> Map<K, V>.toSortedMapWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<K>
): SortedMap<K, V> {
    return this.toSortedMap { lhs, rhs -> comparator(lhs, rhs)?.value ?: 0 }
}

/** 求持续时间总和 / Sum of durations */
fun Iterable<Duration>.sum(): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + duration }
}
