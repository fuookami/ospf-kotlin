package fuookami.ospf.kotlin.utils.functional

import java.util.*
import kotlin.*
import kotlin.time.*
import kotlin.random.Random
import kotlin.collections.*
import kotlin.reflect.full.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*

fun <T> List<T>.shuffle(
    randomGenerator: Generator<Int> = { Random.nextInt(0, this.size) }
): List<T> {
    val list = this.toMutableList()
    for (i in list.size - 1 downTo 1) {
        val j = randomGenerator()!! % list.size
        val temp = list[i]
        list[i] = list[j]
        list[j] = temp
    }
    return list
}

fun <T> Iterator<T>.collect(): List<T> {
    return this.collectTo(ArrayList())
}

fun <T, M : MutableCollection<T>> Iterator<T>.collectTo(m: M): M {
    while (this.hasNext()) {
        m.add(this.next())
    }
    return m
}

inline fun <R, T> Iterable<T>.lastNotNullOf(
    crossinline extractor: Extractor<R?, T>
): R {
    var result: R? = null

    val iterator = this.iterator()
    while (iterator.hasNext()) {
        result = extractor(iterator.next())
    }

    return result ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

inline fun <R, T> List<T>.lastNotNullOf(
    crossinline extractor: Extractor<R?, T>
): R {
    val iterator = this.listIterator()
    while (iterator.hasPrevious()) {
        val result = extractor(iterator.next())

        if (result != null) {
            return result
        }
    }

    throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

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

inline fun <R, T> List<T>.lastNotNullOfOrNull(
    crossinline extractor: Extractor<R?, T>
): R? {
    val iterator = this.listIterator()
    while (iterator.hasPrevious()) {
        val result = extractor(iterator.next())

        if (result != null) {
            return result
        }
    }

    return null
}

inline fun <T> Iterable<T?>.filterNotNull(
    crossinline predicate: Predicate<T>
): List<T> {
    return this.filterNotNullTo(ArrayList(), predicate)
}

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

inline fun <reified U, T> Iterable<T>.filterIsNotInstance(): List<T> {
    return this.filterIsNotInstanceTo<U, T, MutableList<T>>(ArrayList())
}

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

inline fun <reified U, T> Iterable<T>.filterIsNotInstance(
    crossinline predicate: Predicate<U>
): List<T> {
    return this.filterIsNotInstanceTo<U, T, MutableList<T>>(ArrayList(), predicate)
}

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

inline fun <reified U, T> Iterable<T>.filterIsInstance(
    crossinline predicate: Predicate<U>
): List<U> {
    return this.filterIsInstanceTo(ArrayList(), predicate)
}

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

inline fun <R, T> Iterable<T>.flatMapNotNull(
    crossinline extractor: Extractor<Iterable<R?>, T>
): List<R> {
    return this.flatMapNotNullTo(ArrayList(), extractor)
}

inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapNotNullTo(
    destination: C,
    crossinline extractor: Extractor<Iterable<R?>, T>
): C {
    for (element in this.iterator()) {
        destination.addAll(extractor(element).filterNotNull())
    }
    return destination
}

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

inline fun <T> Iterable<T>.maxWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

inline fun <T> Iterable<T>.maxWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

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

inline fun <T> Iterable<T>.maxWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

inline fun <T> Iterable<T>.maxWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

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

inline fun <T, U> Iterable<U>.maxOfWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

inline fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

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

inline fun <T, U> Iterable<U>.maxOfWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

inline fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

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

inline fun <T> Iterable<T>.minWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

inline fun <T> Iterable<T>.minWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

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

inline fun <T> Iterable<T>.minWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

inline fun <T> Iterable<T>.minWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

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

inline fun <T, U> Iterable<U>.minOfWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

inline fun <T, U> Iterable<U>.minOfWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

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

inline fun <T, U> Iterable<U>.minOfWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

inline fun <T, U> Iterable<U>.minOfWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}


fun <T : Comparable<T>> Iterable<T>.minMax(): Pair<T, T> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw NoSuchElementException()
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

inline fun <T, R : Comparable<R>> Iterable<T>.minMaxBy(
    crossinline extractor: Extractor<R, T>
): Pair<T, T> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw NoSuchElementException()
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

inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOf(
    crossinline extractor: Extractor<R, T>
): Pair<R, R> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw NoSuchElementException()
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

inline fun <T> Iterable<T>.minMaxWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

inline fun <T> Iterable<T>.minMaxWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

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

inline fun <T> Iterable<T>.minMaxWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

inline fun <T> Iterable<T>.minMaxWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

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

inline fun <T, U> Iterable<U>.minMaxOfWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

inline fun <T, U> Iterable<U>.minMaxOfWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

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

inline fun <T, U> Iterable<U>.minMaxOfWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

inline fun <T, U> Iterable<U>.minMaxOfWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

inline fun <K, V, T> Iterable<T>.associateNotNull(
    crossinline extractor: Extractor<Pair<K, V>?, T>
): Map<K, V> {
    return this.associateNotNullTo(LinkedHashMap(), extractor)
}

inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.associateNotNullTo(
    destination: M,
    crossinline extractor: Extractor<Pair<K, V>?, T>
): M {
    return this.mapNotNull(extractor).toMap(destination)
}

inline fun <K, T> Iterable<T>.associateByNotNull(
    crossinline extractor: Extractor<K?, T>
): Map<K, T> {
    return this.associateByNotNullTo(LinkedHashMap(), extractor)
}

inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.associateByNotNullTo(
    destination: M,
    crossinline extractor: Extractor<K?, T>
): M {
    return this.mapNotNull { extractor(it)?.to(it) }.toMap(destination)
}

inline fun <V, T> Iterable<T>.associateWithNotNull(
    crossinline extractor: Extractor<V?, T>
): Map<T, V> {
    return this.associateWithNotNullTo(LinkedHashMap(), extractor)
}

inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.associateWithNotNullTo(
    destination: M,
    crossinline extractor: Extractor<V?, T>
): M {
    return this.mapNotNull { extractor(it)?.let { value -> it to value } }.toMap(destination)
}

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

inline fun <T> MutableList<T>.sortWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
) {
    this.sortWith { lhs, rhs ->
        comparator(lhs, rhs).value
    }
}

inline fun <T> MutableList<T>.sortWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
) {
    this.sortWith { lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }
}

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

inline fun <T> Iterable<T>.sortedWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): List<T> {
    return this.sortedWith { lhs, rhs ->
        comparator(lhs, rhs).value
    }
}

inline fun <T> Iterable<T>.sortedWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): List<T> {
    return this.sortedWith { lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }
}

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

inline fun <T> Iterable<T>.toSortedSetWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): SortedSet<T> {
    return this.toSortedSet { lhs, rhs -> comparator(lhs, rhs).value }
}

inline fun <T> Iterable<T>.toSortedSetWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): SortedSet<T> {
    return this.toSortedSet { lhs, rhs -> comparator(lhs, rhs)?.value ?: 0 }
}

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

inline fun <K, V> Map<K, V>.toSortedMapWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<K>
): SortedMap<K, V> {
    return this.toSortedMap { lhs, rhs -> comparator(lhs, rhs).value }
}

inline fun <K, V> Map<K, V>.toSortedMapWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<K>
): SortedMap<K, V> {
    return this.toSortedMap { lhs, rhs -> comparator(lhs, rhs)?.value ?: 0 }
}

fun Iterable<Duration>.sum(): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + duration }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Iterable<T>.sum(): T where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = (T::class.companionObjectInstance!! as ArithmeticConstants<T>).zero
    for (element in this) {
        sum += element
    }
    return sum
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Iterable<T?>.sumOrNull(): T? where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = (T::class.companionObjectInstance!! as ArithmeticConstants<T>).zero
    for (element in this) {
        if (element == null) {
            return null
        }
        sum += element
    }
    return sum
}

@Suppress("UNCHECKED_CAST")
inline fun <T, reified U> Iterable<T>.sumOf(
    crossinline extractor: Extractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = (U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero
    for (element in this) {
        sum += extractor(element)
    }
    return sum
}

inline fun <T, reified U> Iterable<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(extractor) { null }
}

@Suppress("UNCHECKED_CAST")
inline fun <T, reified U> Iterable<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>,
    crossinline defaultValue: (T) -> U?
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = (U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero
    for (element in this) {
        val value = element?.let { extractor(it) } ?: defaultValue(element)
        if (value == null) {
            return null
        }
        sum += value
    }
    return sum
}

@Suppress("UNCHECKED_CAST")
inline fun <K, reified V> Map<K, V>.sum(): V where V : Arithmetic<V>, V : Plus<V, V> {
    var sum = (V::class.companionObjectInstance!! as ArithmeticConstants<V>).zero
    for (element in this) {
        sum += element.value
    }
    return sum
}

@Suppress("UNCHECKED_CAST")
inline fun <K, reified V> Map<K, V?>.sumOrNull(): V? where V : Arithmetic<V>, V : Plus<V, V> {
    var sum = (V::class.companionObjectInstance!! as ArithmeticConstants<V>).zero
    for (element in this) {
        val value = element.value ?: return null
        sum += value
    }
    return sum
}

@Suppress("UNCHECKED_CAST")
inline fun <K, V, reified T> Map<K, V>.sumOf(
    crossinline extractor: Extractor<T, Map.Entry<K, V>>
): T where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = (T::class.companionObjectInstance!! as ArithmeticConstants<T>).zero
    for (element in this) {
        sum += extractor(element)
    }
    return sum
}

inline fun <K, V, reified T> Map<K, V>.sumOfOrNull(
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    return this.sumOfOrNull(extractor) { null }
}

@Suppress("UNCHECKED_CAST")
inline fun <K, V, reified T> Map<K, V>.sumOfOrNull(
    crossinline extractor: Extractor<T?, Map.Entry<K, V>>,
    crossinline defaultValue: (Map.Entry<K, V>) -> T?
): T? where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = (T::class.companionObjectInstance!! as ArithmeticConstants<T>).zero
    for (element in this) {
        val value = element.value?.let { extractor(element) } ?: defaultValue(element)
        if (value == null) {
            return null
        }
        sum += value
    }
    return sum
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Sequence<T>.sum(): T where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = (T::class.companionObjectInstance!! as ArithmeticConstants<T>).zero
    for (element in this) {
        sum += element
    }
    return sum
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Sequence<T?>.sumOrNull(): T? where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = (T::class.companionObjectInstance!! as ArithmeticConstants<T>).zero
    for (element in this) {
        if (element == null) {
            return null
        }
        sum += element
    }
    return sum
}

@Suppress("UNCHECKED_CAST")
inline fun <T, reified U> Sequence<T>.sumOf(
    crossinline extractor: Extractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = (U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero
    for (element in this) {
        sum += extractor(element)
    }
    return sum
}

inline fun <T, reified U> Sequence<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfOrNull(extractor) { null }
}

@Suppress("UNCHECKED_CAST")
inline fun <T, reified U> Sequence<T>.sumOfOrNull(
    crossinline extractor: Extractor<U?, T>,
    crossinline defaultValue: (T) -> U? = { null }
): U? where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = (U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero
    for (element in this) {
        val value = element?.let { extractor(it) } ?: defaultValue(element)
        if (value == null) {
            return null
        }
        sum += value
    }
    return sum
}
