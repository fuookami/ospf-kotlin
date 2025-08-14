package fuookami.ospf.kotlin.utils.operator

interface Exp<out Ret> {
    fun exp(): Ret
}

interface ExpP<Ret> : Exp<Ret> {
    fun exp(digits: Int, precision: Ret): Ret {
        return exp()
    }
}

fun <Base : Exp<Ret>, Ret> exp(base: Base): Ret {
    return base.exp()
}

fun <Base : ExpP<Ret>, Ret> exp(
    base: Base,
    digits: Int,
    precision: Ret
): Ret {
    return base.exp(digits, precision)
}
