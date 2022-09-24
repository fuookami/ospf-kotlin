package fuookami.ospf.kotlin.utils.operator

interface PartialOrd<Self> : PartialEq<Self> {
    infix fun partialOrd(rhs: Self): Int?
}

interface Ord<Self> : PartialOrd<Self>, Eq<Self>, Comparable<Self> {
    infix fun ord(rhs: Self): Int {
        return (this partialOrd rhs)!!
    }

    override fun compareTo(other: Self): Int {
        return this ord other
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
