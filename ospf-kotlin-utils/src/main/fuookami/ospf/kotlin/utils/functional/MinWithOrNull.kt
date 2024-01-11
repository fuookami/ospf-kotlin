package fuookami.ospf.kotlin.utils.functional

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
