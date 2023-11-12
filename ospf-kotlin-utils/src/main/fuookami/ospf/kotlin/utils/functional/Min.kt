package fuookami.ospf.kotlin.utils.functional

fun <T> Iterable<T>.minWithComparator(comparator: Comparator<T>): T {
    return this.minOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }) { it }
}

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

fun <T> Iterable<T>.minWithPartialComparator(comparator: PartialComparator<T>): T {
    return this.minOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
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

fun <T, U> Iterable<U>.minOfWithPartialComparatorOrNull(comparator: PartialComparator<T>, extractor: Extractor<T, U>): T? {
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

fun <T> Iterable<T>.minWithThreeWayComparator(comparator: ThreeWayComparator<T>): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T> Iterable<T>.minWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T, U> Iterable<U>.minOfWithThreeWayComparator(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T, U> Iterable<U>.minOfWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T> Iterable<T>.minWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

fun <T> Iterable<T>.minWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

fun <T, U> Iterable<U>.minOfWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): T {
    return this.minOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

fun <T, U> Iterable<U>.minOfWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): T? {
    return this.minOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}
