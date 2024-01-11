package fuookami.ospf.kotlin.utils.functional

inline fun <T> Iterable<T>.minWithComparator(
    crossinline comparator: Comparator<T>
): T {
    return this.minOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }) { it }
}

inline fun <T> Iterable<T>.minWithPartialComparator(
    crossinline comparator: PartialComparator<T>
): T {
    return this.minOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }) { it }
}

inline fun <T> Iterable<T>.minWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

inline fun <T> Iterable<T>.minWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}
