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

fun <T> Iterable<T>.minMaxWithThreeWayComparator(comparator: ThreeWayComparator<T>): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T> Iterable<T>.minMaxWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}
