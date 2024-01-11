package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.math.ordinary.*

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
