package fuookami.ospf.kotlin.utils.functional

inline fun <T, U> Iterable<U>.minOfWithComparator(
    crossinline comparator: Comparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.minOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }, extractor)
}

inline fun <T, U> Iterable<U>.minOfWithPartialComparator(
    crossinline comparator: PartialComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.minOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }, extractor)
}

inline fun <T, U> Iterable<U>.minOfWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

inline fun <T, U> Iterable<U>.minOfWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>,
    crossinline extractor: Extractor<T, U>
): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}
