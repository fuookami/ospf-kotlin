package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.math.ordinary.minMaxOfWithOrNull

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

fun <T, U> Iterable<U>.minMaxOfWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T, U> Iterable<U>.minMaxOfWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): Pair<T, T>? {
    return this.minMaxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}
