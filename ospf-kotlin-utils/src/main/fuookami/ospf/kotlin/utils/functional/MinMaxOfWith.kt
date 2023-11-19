package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.math.ordinary.minMaxOfWith

fun <T, U> Iterable<U>.minMaxOfWithComparator(comparator: Comparator<T>, extractor: Extractor<T, U>): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }, extractor)
}

fun <T, U> Iterable<U>.minMaxOfWithPartialComparator(comparator: PartialComparator<T>, extractor: Extractor<T, U>): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }, extractor)
}

fun <T, U> Iterable<U>.minMaxOfWithThreeWayComparator(comparator: ThreeWayComparator<T>, extractor: Extractor<T, U>): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }, extractor)
}

fun <T, U> Iterable<U>.minMaxOfWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>, extractor: Extractor<T, U>): Pair<T, T> {
    return this.minMaxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }, extractor)
}
