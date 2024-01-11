package fuookami.ospf.kotlin.utils.functional

inline fun <T> Iterable<T>.sortedWithComparator(
    crossinline comparator: Comparator<T>
): List<T> {
    return this.sortedWith { lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }
}

inline fun <T> Iterable<T>.sortedWithPartialComparator(
    crossinline comparator: PartialComparator<T>
): List<T> {
    return this.sortedWith { lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }
}

inline fun <T> Iterable<T>.sortedWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
): List<T> {
    return this.sortedWith { lhs, rhs ->
        comparator(lhs, rhs).value
    }
}

inline fun <T> Iterable<T>.sortedWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
): List<T> {
    return this.sortedWith { lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }
}
