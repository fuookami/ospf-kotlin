package fuookami.ospf.kotlin.utils.functional

import java.util.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*

typealias Predicate<T> = (T) -> Boolean
typealias Extractor<Ret, T> = (T) -> Ret
typealias Mapper<Ret, T> = (T) -> Ret
typealias Comparator<T> = (T, T) -> Boolean
typealias PartialComparator<T> = (T, T) -> Boolean?
typealias ThreeWayComparator<T> = (T, T) -> Order
typealias PartialThreeWayComparator<T> = (T, T) -> Order?
typealias Generator<Ret> = () -> Ret?

infix fun <T, U : T> Predicate<T>.and(rhs: Predicate<U>) = { it: U -> this(it as T) and rhs(it) }
infix fun <T, U : T> Predicate<T>.or(rhs: Predicate<U>) = { it: U -> this(it) or rhs(it) }
infix fun <T, U : T> Predicate<T>.xor(rhs: Predicate<U>) = { it: U -> this(it) xor rhs(it) }
operator fun <T> Predicate<T>.not(): Predicate<T> = { it: T -> !this(it) }
operator fun <T> ThreeWayComparator<T>.not(): ThreeWayComparator<T> = { lhs, rhs: T -> orderOf(-this(lhs, rhs).value) }

fun <T> MutableList<T>.sortWithComparator(comparator: Comparator<T>) {
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

fun <T> MutableList<T>.sortWithPartialComparator(comparator: PartialComparator<T>) {
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

fun <T> MutableList<T>.sortWithThreeWayComparator(comparator: ThreeWayComparator<T>) {
    this.sortWith { lhs, rhs ->
        comparator(lhs, rhs).value
    }
}

fun <T> MutableList<T>.sortWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>) {
    this.sortWith { lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }
}

fun <T> Iterable<T>.sortedWithComparator(comparator: Comparator<T>): List<T> {
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

fun <T> Iterable<T>.sortedWithPartialComparator(comparator: PartialComparator<T>): List<T> {
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

fun <T> Iterable<T>.sortedWithThreeWayComparator(comparator: ThreeWayComparator<T>): List<T> {
    return this.sortedWith { lhs, rhs ->
        comparator(lhs, rhs).value
    }
}

fun <T> Iterable<T>.sortedWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>): List<T> {
    return this.sortedWith { lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }
}

fun <T> Iterable<T>.minWithComparator(comparator: Comparator<T>): T {
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

fun <T> Iterable<T>.minWithComparatorOrNull(comparator: Comparator<T>): T? {
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

fun <T, U> Iterable<U>.minOfWithComparator(comparator: Comparator<T>, extractor: Extractor<T, U>): T {
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

fun <T, U> Iterable<U>.minOfWithComparatorOrNull(comparator: Comparator<T>, extractor: Extractor<T, U>): T? {
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

fun <T> Iterable<T>.minWithPartialComparator(comparator: PartialComparator<T>): T {
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

fun <T> Iterable<T>.minWithPartialComparatorOrNull(comparator: PartialComparator<T>): T? {
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

fun <T, U> Iterable<U>.minOfWithPartialComparator(comparator: PartialComparator<T>, extractor: Extractor<T, U>): T {
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

fun <T, U> Iterable<U>.minOfWithPartialComparatorOrNull(comparator: PartialComparator<T>, extractor: Extractor<T, U>): T? {
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

fun <T> Iterable<T>.minWithThreeWayComparator(comparator: ThreeWayComparator<T>): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T> Iterable<T>.minWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T, U> Iterable<U>.minOfWithThreeWayComparator(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T, U> Iterable<U>.minOfWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T> Iterable<T>.minWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

fun <T> Iterable<T>.minWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

fun <T, U> Iterable<U>.minOfWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

fun <T, U> Iterable<U>.minOfWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

fun <T> Iterable<T>.maxWithComparator(comparator: Comparator<T>): T {
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

fun <T> Iterable<T>.maxWithComparatorOrNull(comparator: Comparator<T>): T? {
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

fun <T, U> Iterable<U>.maxOfWithComparator(comparator: Comparator<T>, extractor: Extractor<T, U>): T {
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

fun <T, U> Iterable<U>.maxOfWithComparatorOrNull(comparator: Comparator<T>, extractor: Extractor<T, U>): T? {
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

fun <T> Iterable<T>.maxWithPartialComparator(comparator: PartialComparator<T>): T {
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

fun <T> Iterable<T>.maxWithPartialComparatorOrNull(comparator: PartialComparator<T>): T? {
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

fun <T, U> Iterable<U>.maxOfWithPartialComparator(comparator: PartialComparator<T>, extractor: Extractor<T, U>): T {
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

fun <T, U> Iterable<U>.maxOfWithPartialComparatorOrNull(comparator: PartialComparator<T>, extractor: Extractor<T, U>): T? {
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

fun <T> Iterable<T>.maxWithThreeWayComparator(comparator: ThreeWayComparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T> Iterable<T>.maxWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T, U> Iterable<U>.maxOfWithThreeWayComparator(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T, U> Iterable<U>.maxOfWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T> Iterable<T>.maxWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

fun <T> Iterable<T>.maxWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

fun <T> Iterable<T>.minMaxWithComparator(comparator: Comparator<T>): Pair<T, T> {
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

fun <T> Iterable<T>.minMaxWithComparatorOrNull(comparator: Comparator<T>): Pair<T, T>? {
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

fun <T, U> Iterable<U>.minMaxOfWithComparator(comparator: Comparator<T>, extractor: Extractor<T, U>): Pair<T, T> {
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

fun <T, U> Iterable<U>.minMaxOfWithComparatorOrNull(comparator: Comparator<T>, extractor: Extractor<T, U>): Pair<T, T>? {
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

fun <T> Iterable<T>.minMaxWithPartialComparator(comparator: PartialComparator<T>): Pair<T, T> {
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

fun <T> Iterable<T>.minMaxWithPartialComparatorOrNull(comparator: PartialComparator<T>): Pair<T, T>? {
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

fun <T, U> Iterable<U>.minMaxOfWithPartialComparator(comparator: PartialComparator<T>, extractor: Extractor<T, U>): Pair<T, T> {
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

fun <T, U> Iterable<U>.minMaxOfWithPartialComparatorOrNull(comparator: PartialComparator<T>, extractor: Extractor<T, U>): Pair<T, T>? {
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

fun <T> Iterable<T>.minMaxWithThreeWayComparator(comparator: ThreeWayComparator<T>): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T> Iterable<T>.minMaxWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T, U> Iterable<U>.minMaxOfWithThreeWayComparator(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T, U> Iterable<U>.minMaxOfWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T> Iterable<T>.minMaxWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

fun <T> Iterable<T>.minMaxWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

fun <T, U> Iterable<U>.minMaxOfWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

fun <T, U> Iterable<U>.minMaxOfWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

fun <T> Iterable<T>.toSortedSetWithComparator(comparator: Comparator<T>): SortedSet<T> {
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

fun <T> Iterable<T>.toSortedSetWithPartialComparator(comparator: PartialComparator<T>): SortedSet<T> {
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

fun <T> Iterable<T>.toSortedSetWithThreeWayComparator(comparator: ThreeWayComparator<T>): SortedSet<T> {
    return this.toSortedSet { lhs, rhs -> comparator(lhs, rhs).value }
}

fun <T> Iterable<T>.toSortedSetWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>): SortedSet<T> {
    return this.toSortedSet { lhs, rhs -> comparator(lhs, rhs)?.value ?: 0 }
}

fun <K, V> Map<K, V>.toSortedMapWithComparator(comparator: Comparator<K>): SortedMap<K, V> {
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

fun <K, V> Map<K, V>.toSortedMapWithPartialComparator(comparator: PartialComparator<K>): SortedMap<K, V> {
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

fun <K, V> Map<K, V>.toSortedMapWithThreeWayComparator(comparator: ThreeWayComparator<K>): SortedMap<K, V> {
    return this.toSortedMap { lhs, rhs -> comparator(lhs, rhs).value }
}

fun <K, V> Map<K, V>.toSortedMapWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<K>): SortedMap<K, V> {
    return this.toSortedMap { lhs, rhs -> comparator(lhs, rhs)?.value ?: 0 }
}
