package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.math.ordinary.*

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
