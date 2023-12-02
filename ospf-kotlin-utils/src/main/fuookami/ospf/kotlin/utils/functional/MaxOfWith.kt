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

fun <T, U> Iterable<U>.maxOfWithPartialComparator(
    comparator: PartialComparator<T>,
    extractor: Extractor<T, U>
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

fun <T, U> Iterable<U>.maxOfWithThreeWayComparator(
    comparator: ThreeWayComparator<T>,
    extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparator(
    comparator: PartialThreeWayComparator<T>,
    extractor: Extractor<T, U>
): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}
