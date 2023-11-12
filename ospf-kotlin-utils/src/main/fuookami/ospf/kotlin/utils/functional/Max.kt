package fuookami.ospf.kotlin.utils.functional

fun <T> Iterable<T>.maxWithComparator(comparator: Comparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }) { it }
}

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

fun <T, U> Iterable<U>.maxOfWithComparator(comparator: Comparator<T>, extractor: Extractor<T, U>): T {
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

fun <T, U> Iterable<U>.maxOfWithComparatorOrNull(comparator: Comparator<T>, extractor: Extractor<T, U>): T? {
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

fun <T> Iterable<T>.maxWithPartialComparator(comparator: PartialComparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
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

fun <T, U> Iterable<U>.maxOfWithPartialComparator(comparator: PartialComparator<T>, extractor: Extractor<T, U>): T {
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

fun <T, U> Iterable<U>.maxOfWithPartialComparatorOrNull(comparator: PartialComparator<T>, extractor: Extractor<T, U>): T? {
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

fun <T> Iterable<T>.maxWithThreeWayComparator(comparator: ThreeWayComparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T> Iterable<T>.maxWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T, U> Iterable<U>.maxOfWithThreeWayComparator(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T, U> Iterable<U>.maxOfWithThreeWayComparatorOrNull(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T> Iterable<T>.maxWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

fun <T> Iterable<T>.maxWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}

fun <T, U> Iterable<U>.maxOfWithPartialThreeWayComparatorOrNull(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): T? {
    return this.maxOfWithOrNull({ lhs: T, rhs: T ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}
