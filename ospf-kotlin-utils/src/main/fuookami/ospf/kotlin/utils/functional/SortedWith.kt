package fuookami.ospf.kotlin.utils.functional

fun <T> Iterable<T>.sortedWithComparator(comparator: Comparator<T>): List<T> {
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

fun <T> Iterable<T>.sortedWithPartialComparator(comparator: PartialComparator<T>): List<T> {
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

fun <T> Iterable<T>.sortedWithThreeWayComparator(comparator: ThreeWayComparator<T>): List<T> {
    return this.sortedWith { lhs, rhs ->
        comparator(lhs, rhs).value
    }
}

fun <T> Iterable<T>.sortedWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>): List<T> {
    return this.sortedWith { lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }
}
