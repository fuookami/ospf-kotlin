package fuookami.ospf.kotlin.utils.operator

interface RangeTo<in Rhs, Ret : Comparable<Ret>> {
    operator fun rangeTo(rhs: Rhs): ClosedRange<Ret>
}
