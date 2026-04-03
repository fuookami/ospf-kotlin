package fuookami.ospf.kotlin.utils.math.operator

interface Neg<out Ret> {
    operator fun unaryMinus(): Ret
}
