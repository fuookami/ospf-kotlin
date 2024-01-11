package fuookami.ospf.kotlin.utils.functional

inline fun <T> Iterable<T>.maxWithComparatorOrNull(
    crossinline comparator: Comparator<T>
): T? {
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

inline fun <T> Iterable<T>.maxWithPartialComparatorOrNull(
    crossinline comparator: PartialComparator<T>
): T? {
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

inline fun <T> Iterable<T>.maxWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

inline fun <T> Iterable<T>.maxWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}
