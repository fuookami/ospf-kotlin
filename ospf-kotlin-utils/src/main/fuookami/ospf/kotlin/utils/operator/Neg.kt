package fuookami.ospf.kotlin.utils.operator

interface Neg<out Ret> {
    operator fun unaryMinus(): Ret
}
