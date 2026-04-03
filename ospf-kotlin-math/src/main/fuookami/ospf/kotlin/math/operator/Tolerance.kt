package fuookami.ospf.kotlin.math.operator

interface Tolerance<T> {
    val tolerance: T
}

fun interface TolerancedEq<T> {
    fun test(lhs: T, rhs: T, tolerance: T): Boolean
}

fun interface TolerancedOrd<T> {
    fun test(lhs: T, rhs: T, tolerance: T): Order
}

data class AbsoluteTolerance<T>(
    override val tolerance: T
) : Tolerance<T>

