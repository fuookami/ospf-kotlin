package fuookami.ospf.kotlin.utils.functional

fun <T> Iterable<T>.minWithComparatorOrNull(comparator: Comparator<T>): T? {
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

fun <T> Iterable<T>.minWithPartialComparatorOrNull(comparator: PartialComparator<T>): T? {
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

fun <T> Iterable<T>.minWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T> Iterable<T>.minWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}
