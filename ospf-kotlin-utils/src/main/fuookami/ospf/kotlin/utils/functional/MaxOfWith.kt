package fuookami.ospf.kotlin.utils.functional

inline fun <T, U> Iterable<U>.maxOfWithComparator(
    crossinline comparator: Comparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }, extractor)
}

inline fun <T, U> Iterable<U>.maxOfWithPartialComparator(
    crossinline comparator: PartialComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }, extractor)
}

inline fun <T, U> Iterable<U>.maxOfWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

inline fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}
