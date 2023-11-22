package fuookami.ospf.kotlin.utils.functional

inline fun <T> Iterable<T>.maxWithComparator(crossinline comparator: Comparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }) { it }
}

inline fun <T> Iterable<T>.maxWithPartialComparator(crossinline comparator: PartialComparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }) { it }
}

inline fun <T> Iterable<T>.maxWithThreeWayComparator(crossinline comparator: ThreeWayComparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

inline fun <T> Iterable<T>.maxWithPartialThreeWayComparator(crossinline comparator: PartialThreeWayComparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}
