package fuookami.ospf.kotlin.utils.operator

sealed interface Order {
    val value: Int

    open fun ifEqual(f: () -> Order): Order = this

    data class Less(override val value: Int = -1) : Order {
        init {
            assert(value < 0)
        }
    }

    data object Equal : Order {
        override val value = 0

        override fun ifEqual(f: () -> Order) = f()
    }

    data class Greater(override val value: Int = 1) : Order {
        init {
            assert(value > 0)
        }
    }
}

fun orderOf(value: Int): Order {
    return if (value < 0) {
        Order.Less(value)
    } else if (value > 0) {
        Order.Greater(value)
    } else {
        Order.Equal
    }
}

fun <T : Comparable<T>> orderBetween(lhs: T, rhs: T): Order {
    return orderOf(lhs.compareTo(rhs))
}

@JvmName("comparableOrd")
infix fun <T : Comparable<T>> T.ord(rhs: T): Order {
    return orderOf(this.compareTo(rhs))
}

@JvmName("comparableNullableOrd")
infix fun <T : Comparable<T>> T?.ord(rhs: T?): Order {
    return if (this == null && rhs != null) {
        Order.Less()
    } else if (this != null && rhs == null) {
        Order.Greater()
    } else if (this != null && rhs != null) {
        this ord rhs
    } else {
        Order.Equal
    }
}

interface PartialOrd<in Self> : PartialEq<Self> {
    override fun partialEq(rhs: Self): Boolean? {
        return partialOrd(rhs)?.let { it is Order.Equal }
    }

    infix fun partialOrd(rhs: Self): Order?
}

interface Ord<in Self> : PartialOrd<Self>, Eq<Self>, Comparable<Self> {
    infix fun ord(rhs: Self): Order {
        return (this partialOrd rhs)!!
    }

    override fun compareTo(other: Self): Int {
        return (this ord other).value
    }

    infix fun ls(rhs: Self): Boolean {
        return this < rhs
    }

    infix fun leq(rhs: Self): Boolean {
        return this <= rhs
    }

    infix fun gr(rhs: Self): Boolean {
        return this > rhs
    }

    infix fun geq(rhs: Self): Boolean {
        return this >= rhs
    }
}

infix fun <T : PartialOrd<T>> T?.partialOrd(rhs: T?): Order? {
    return if (this == null && rhs != null) {
        Order.Less()
    } else if (this != null && rhs == null) {
        Order.Greater()
    } else if (this != null && rhs != null) {
        this partialOrd rhs
    } else {
        Order.Equal
    }
}

infix fun <T : Ord<T>> T?.ord(rhs: T?): Order {
    return if (this == null && rhs != null) {
        Order.Less()
    } else if (this != null && rhs == null) {
        Order.Greater()
    } else if (this != null && rhs != null) {
        this ord rhs
    } else {
        Order.Equal
    }
}
