package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.math.ordinary.*

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
