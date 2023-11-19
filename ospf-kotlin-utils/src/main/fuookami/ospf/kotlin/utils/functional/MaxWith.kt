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

fun <T> Iterable<T>.maxWithThreeWayComparator(comparator: ThreeWayComparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs).value
    }) { it }
}

fun <T> Iterable<T>.maxWithPartialThreeWayComparator(comparator: PartialThreeWayComparator<T>): T {
    return this.maxOfWith({ lhs, rhs ->
        comparator(lhs, rhs)?.value ?: 0
    }) { it }
}

val l = listOf(1)
val c = l.min()
