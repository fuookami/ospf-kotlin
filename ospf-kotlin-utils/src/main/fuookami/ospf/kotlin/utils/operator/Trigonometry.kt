package fuookami.ospf.kotlin.utils.operator

interface Trigonometry<out Ret> {
    fun sin(): Ret
    fun cos(): Ret
    fun sec(): Ret?
    fun csc(): Ret?
    fun tan(): Ret?
    fun cot(): Ret?

    fun asin(): Ret?
    fun acos(): Ret?
    fun asec(): Ret?
    fun acsc(): Ret?
    fun atan(): Ret
    fun acot(): Ret?

    fun sinh(): Ret
    fun cosh(): Ret
    fun sech(): Ret
    fun csch(): Ret?
    fun tanh(): Ret
    fun coth(): Ret?

    fun asinh(): Ret
    fun acosh(): Ret?
    fun asech(): Ret?
    fun acsch(): Ret?
    fun atanh(): Ret?
    fun acoth(): Ret?
}

fun <T, Ret> sin(x: T): Ret where T : Trigonometry<Ret> {
    return x.sin()
}

fun <T, Ret> cos(x: T): Ret where T : Trigonometry<Ret> {
    return x.cos()
}

fun <T, Ret> sec(x: T): Ret? where T : Trigonometry<Ret> {
    return x.sec()
}

fun <T, Ret> csc(x: T): Ret? where T : Trigonometry<Ret> {
    return x.csc()
}

fun <T, Ret> tan(x: T): Ret? where T : Trigonometry<Ret> {
    return x.tan()
}

fun <T, Ret> cot(x: T): Ret? where T : Trigonometry<Ret> {
    return x.cot()
}

fun <T, Ret> asin(x: T): Ret? where T : Trigonometry<Ret> {
    return x.asin()
}

fun <T, Ret> acos(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acos()
}

fun <T, Ret> asec(x: T): Ret? where T : Trigonometry<Ret> {
    return x.asec()
}

fun <T, Ret> acsc(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acsc()
}

fun <T, Ret> atan(x: T): Ret where T : Trigonometry<Ret> {
    return x.atan()
}

fun <T, Ret> acot(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acot()
}

fun <T, Ret> sinh(x: T): Ret where T : Trigonometry<Ret> {
    return x.sinh()
}

fun <T, Ret> cosh(x: T): Ret where T : Trigonometry<Ret> {
    return x.cosh()
}

fun <T, Ret> sech(x: T): Ret where T : Trigonometry<Ret> {
    return x.sech()
}

fun <T, Ret> csch(x: T): Ret? where T : Trigonometry<Ret> {
    return x.csch()
}

fun <T, Ret> tanh(x: T): Ret where T : Trigonometry<Ret> {
    return x.tanh()
}

fun <T, Ret> coth(x: T): Ret? where T : Trigonometry<Ret> {
    return x.coth()
}

fun <T, Ret> asinh(x: T): Ret where T : Trigonometry<Ret> {
    return x.asinh()
}

fun <T, Ret> acosh(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acosh()
}

fun <T, Ret> asech(x: T): Ret? where T : Trigonometry<Ret> {
    return x.asech()
}

fun <T, Ret> acsch(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acsch()
}

fun <T, Ret> atanh(x: T): Ret? where T : Trigonometry<Ret> {
    return x.atanh()
}

fun <T, Ret> acoth(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acoth()
}
