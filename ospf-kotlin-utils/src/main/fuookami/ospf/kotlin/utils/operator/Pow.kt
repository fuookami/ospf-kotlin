package fuookami.ospf.kotlin.utils.operator

interface Pow<out Ret> {
    fun pow(index: Int): Ret

    fun sqr(): Ret
    fun cub(): Ret
}

interface PowFun<in Self, out Ret> {
    fun Self.pow(index: Int): Ret

    fun Self.square(): Ret
    fun Self.cubic(): Ret
}

fun <Base, Ret> pow(base: Base, index: Int): Ret
        where Base : Pow<Ret> {
    return base.pow(index)
}

fun <Base, Ret, Func : PowFun<Base, Ret>> pow(base: Base, index: Int, func: Func): Ret {
    return func.run {
        base.pow(index)
    }
}

fun <Base, Ret> sqr(base: Base): Ret
        where Base : Pow<Ret> {
    return base.sqr()
}

fun <Base, Ret> cub(base: Base): Ret
        where Base : Pow<Ret> {
    return base.cub()
}

interface PowF<in Index, out Ret> {
    fun pow(index: Index): Ret

    fun sqrt(): Ret
    fun cbrt(): Ret
}

fun <Base, Index, Ret> pow(base: Base, index: Index): Ret
        where Base : PowF<Index, Ret> {
    return base.pow(index)
}

fun <Base, Index, Ret> sqrt(base: Base): Ret
        where Base : PowF<Index, Ret> {
    return base.sqrt()
}

fun <Base, Index, Ret> cbrt(base: Base): Ret
        where Base : PowF<Index, Ret> {
    return base.cbrt()
}
