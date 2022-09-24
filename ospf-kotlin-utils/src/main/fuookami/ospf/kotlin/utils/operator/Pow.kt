package fuookami.ospf.kotlin.utils.operator

interface Pow<out Ret> {
    fun pow(index: Int): Ret

    fun square(): Ret
    fun cubic(): Ret
}

interface PowFun<Self, out Ret> {
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

fun <Base, Ret> square(base: Base): Ret
    where Base : Pow<Ret> {
    return base.square()
}

fun <Base, Ret> cubic(base: Base): Ret
    where Base : Pow<Ret> {
    return base.cubic()
}

interface PowF<in Index, out Ret> {
    fun pow(index: Index): Ret

    fun sqr(): Ret
    fun cbr(): Ret
}

fun <Base, Index, Ret> pow(base: Base, index: Index): Ret
    where Base : PowF<Index, Ret> {
    return base.pow(index)
}

fun <Base, Index, Ret> sqr(base: Base): Ret
    where Base : PowF<Index, Ret> {
    return base.sqr()
}

fun <Base, Index, Ret> cbr(base: Base): Ret
    where Base : PowF<Index, Ret> {
    return base.cbr()
}
