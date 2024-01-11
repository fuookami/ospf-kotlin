package fuookami.ospf.kotlin.utils.functional

import java.util.*

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
