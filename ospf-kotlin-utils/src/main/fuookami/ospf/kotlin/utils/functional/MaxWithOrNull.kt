package fuookami.ospf.kotlin.utils.functional

fun <T> Iterable<T>.maxWithComparatorOrNull(comparator: Comparator<T>): T? {
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

fun <T> Iterable<T>.maxWithPartialComparatorOrNull(comparator: PartialComparator<T>): T? {
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

fun <T> Iterable<T>.maxWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T> Iterable<T>.maxWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}
