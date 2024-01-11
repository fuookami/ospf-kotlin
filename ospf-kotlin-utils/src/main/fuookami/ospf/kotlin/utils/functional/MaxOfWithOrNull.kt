package fuookami.ospf.kotlin.utils.functional

inline fun <T, U> Iterable<U>.maxOfWithComparatorOrNull(
    crossinline comparator: Comparator<T>,
    crossinline extractor: Extractor<T, U>
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

inline fun <T, U> Iterable<U>.maxOfWithPartialComparatorOrNull(
    crossinline comparator: PartialComparator<T>,
    crossinline extractor: Extractor<T, U>
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

inline fun <T, U> Iterable<U>.maxOfWithThreeWayComparatorOrNull(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

inline fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparatorOrNull(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}
