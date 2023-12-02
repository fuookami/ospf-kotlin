package fuookami.ospf.kotlin.utils.functional

fun <T, U> Iterable<U>.minOfWithComparatorOrNull(comparator: Comparator<T>, extractor: Extractor<T, U>): T? {
    return this.minOfWithOrNull({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }, extractor)
}

fun <T, U> Iterable<U>.minOfWithPartialComparatorOrNull(
    comparator: PartialComparator<T>,
    extractor: Extractor<T, U>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }, extractor)
}

fun <T, U> Iterable<U>.minOfWithThreeWayComparatorOrNull(
    comparator: ThreeWayComparator<T>,
    extractor: Extractor<T, U>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T, U> Iterable<U>.minOfWithPartialThreeWayComparatorOrNull(
    comparator: PartialThreeWayComparator<T>,
    extractor: Extractor<T, U>
): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}
