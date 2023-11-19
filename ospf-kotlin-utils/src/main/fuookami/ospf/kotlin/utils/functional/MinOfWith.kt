package fuookami.ospf.kotlin.utils.functional

fun <T, U> Iterable<U>.minOfWithComparator(comparator: Comparator<T>, extractor: Extractor<T, U>): T {
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

fun <T, U> Iterable<U>.minOfWithPartialComparator(comparator: PartialComparator<T>, extractor: Extractor<T, U>): T {
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

fun <T, U> Iterable<U>.minOfWithThreeWayComparator(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T, U> Iterable<U>.minOfWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}
