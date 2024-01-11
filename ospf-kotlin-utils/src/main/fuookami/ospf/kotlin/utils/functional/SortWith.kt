package fuookami.ospf.kotlin.utils.functional

inline fun <T> MutableList<T>.sortWithComparator(
    crossinline comparator: Comparator<T>
) {
    this.sortWith { lhs, rhs ->
        if (comparator(lhs, rhs)) {
            -1
        } else if (comparator(rhs, lhs)) {
            1
        } else {
            0
        }
    }
}

inline fun <T> MutableList<T>.sortWithPartialComparator(
    crossinline comparator: PartialComparator<T>
) {
    this.sortWith { lhs, rhs ->
        if (comparator(lhs, rhs) == true) {
            -1
        } else if (comparator(rhs, lhs) == true) {
            1
        } else {
            0
        }
    }
}

inline fun <T> MutableList<T>.sortWithThreeWayComparator(
    crossinline comparator: ThreeWayComparator<T>
) {
    this.sortWith { lhs, rhs ->
        comparator(lhs, rhs).value
    }
}

inline fun <T> MutableList<T>.sortWithPartialThreeWayComparator(
    crossinline comparator: PartialThreeWayComparator<T>
) {
    this.sortWith { lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }
}
