package fuookami.ospf.kotlin.utils.functional

import java.util.*

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
