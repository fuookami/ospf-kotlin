package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.math.ordinary.*

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
