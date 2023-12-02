package fuookami.ospf.kotlin.utils.functional

fun <T, U> Iterable<U>.maxOfWithComparatorOrNull(
    comparator: Comparator<T>,
    extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }, extractor)
}

fun <T, U> Iterable<U>.maxOfWithPartialComparatorOrNull(
    comparator: PartialComparator<T>,
    extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }, extractor)
}

fun <T, U> Iterable<U>.maxOfWithThreeWayComparatorOrNull(
    comparator: ThreeWayComparator<T>,
    extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparatorOrNull(
    comparator: PartialThreeWayComparator<T>,
    extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}
