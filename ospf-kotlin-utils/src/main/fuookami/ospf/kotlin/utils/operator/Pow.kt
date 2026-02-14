package fuookami.ospf.kotlin.utils.operator

interface Pow<out Ret> {
    fun pow(index: Int): Ret

    fun sqr(): Ret
    fun cub(): Ret
}

interface PowP<Ret> : Pow<Ret> {
    fun pow(index: Int, digits: Int, precision: Ret): Ret {
        return pow(index)
    }
}

interface PowFun<in Self, out Ret> {
    fun Self.pow(index: Int): Ret

    fun Self.sqr(): Ret
    fun Self.cub(): Ret
}

interface PowFunP<in Self, Ret> {
    fun Self.pow(index: Int, digits: Int, precision: Ret): Ret
}

fun <Base : Pow<Ret>, Ret> pow(
    base: Base,
    index: Int
): Ret {
    return base.pow(index)
}

fun <Base, Ret, Func : PowFun<Base, Ret>> pow(
    base: Base,
    index: Int,
    func: Func
): Ret {
    return func.run {
        base.pow(index)
    }
}

fun <Base : Pow<Ret>, Ret> sqr(base: Base): Ret {
    return base.sqr()
}

fun <Base, Ret, Func : PowFun<Base, Ret>> sqr(
    base: Base,
    func: Func
): Ret {
    return with(func) {
        base.sqr()
    }
}

fun <Base : Pow<Ret>, Ret> cub(base: Base): Ret {
    return base.cub()
}

fun <Base, Ret, Func : PowFun<Base, Ret>> cub(
    base: Base,
    func: Func
): Ret {
    return with(func) {
        base.cub()
    }
}

interface PowF<in Index, out Ret> {
    fun pow(index: Index): Ret

    fun sqrt(): Ret
    fun cbrt(): Ret
}

interface PowFFun<in Self, in Index, out Ret> {
    fun Self.pow(index: Index): Ret

    fun Self.sqrt(): Ret
    fun Self.cbrt(): Ret
}

interface PowFP<in Index, Ret>: PowF<Index, Ret> {
    fun pow(index: Index, digits: Int, precision: Ret): Ret {
        return pow(index)
    }

    fun sqrt(digits: Int, precision: Ret): Ret {
        return sqrt()
    }

    fun cbrt(digits: Int, precision: Ret): Ret {
        return cbrt()
    }
}

interface PowFPFun<in Self, in Index, Ret> {
    fun Self.pow(index: Index, digits: Int, precision: Ret): Ret
    fun Self.sqrt(digits: Int, precision: Ret): Ret
    fun Self.cbrt(digits: Int, precision: Ret): Ret
}

fun <Base : PowF<Index, Ret>, Index, Ret> pow(
    base: Base,
    index: Index
): Ret {
    return base.pow(index)
}

fun <Base, Index, Ret, Func : PowFFun<Base, Index, Ret>> pow(
    base: Base,
    index: Index,
    func: Func
): Ret {
    return with(func) {
        base.pow(index)
    }
}

fun <Base : PowFP<Index, Ret>, Index, Ret> pow(
    base: Base,
    index: Index,
    digits: Int,
    precision: Ret
): Ret {
    return base.pow(
        index = index,
        digits = digits,
        precision = precision
    )
}

fun <Base, Index, Ret, Func : PowFPFun<Base, Index, Ret>> pow(
    base: Base,
    index: Index,
    digits: Int,
    precision: Ret,
    func: Func
): Ret {
    return with(func) {
        base.pow(
            index = index,
            digits = digits,
            precision = precision
        )
    }
}

fun <Base : PowF<Index, Ret>, Index, Ret> sqrt(base: Base): Ret {
    return base.sqrt()
}

fun <Base, Index, Ret, Func : PowFFun<Base, Index, Ret>> sqrt(
    base: Base,
    func: Func
): Ret {
    return with(func) {
        base.sqrt()
    }
}

fun <Base : PowFP<Index, Ret>, Index, Ret> sqrt(
    base: Base,
    digits: Int,
    precision: Ret
): Ret {
    return base.sqrt(digits, precision)
}

fun <Base, Index, Ret, Func : PowFPFun<Base, Index, Ret>> sqrt(
    base: Base,
    digits: Int,
    precision: Ret,
    func: Func
): Ret {
    return with(func) {
        base.sqrt(digits, precision)
    }
}

fun <Base : PowF<Index, Ret>, Index, Ret> cbrt(base: Base): Ret {
    return base.cbrt()
}

fun <Base, Index, Ret, Func : PowFFun<Base, Index, Ret>> cbrt(
    base: Base,
    func: Func
): Ret {
    return with(func) {
        base.cbrt()
    }
}

fun <Base : PowFP<Index, Ret>, Index, Ret> cbrt(
    base: Base,
    digits: Int,
    precision: Ret
): Ret {
    return base.cbrt(digits, precision)
}

fun <Base, Index, Ret, Func : PowFPFun<Base, Index, Ret>> cbrt(
    base: Base,
    digits: Int,
    precision: Ret,
    func: Func
): Ret {
    return with(func) {
        base.cbrt(digits, precision)
    }
}
