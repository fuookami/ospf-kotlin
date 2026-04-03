package fuookami.ospf.kotlin.math.operator

interface RangeTo<in Rhs, out Ret : Comparable<@UnsafeVariance Ret>> {
    operator fun rangeTo(rhs: Rhs): ClosedRange<@UnsafeVariance Ret>

    infix fun until(rhs: Rhs): ClosedRange<@UnsafeVariance Ret>
}
