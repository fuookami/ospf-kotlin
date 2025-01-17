package fuookami.ospf.kotlin.utils.operator

interface Trigonometry<out Ret> {
    fun sin(): Ret
    fun cos(): Ret
    fun tan(): Ret?

    fun asin(): Ret?
    fun acos(): Ret?
    fun atan(): Ret

//    fun sinh(): Ret
//    fun cosh(): Ret
//    fun tanh(): Ret?
//
//    fun asinh(): Ret?
//    fun acosh(): Ret?
//    fun atanh(): Ret?
}

fun <T, Ret> sin(x: T): Ret where T : Trigonometry<Ret> {
    return x.sin()
}

fun <T, Ret> cos(x: T): Ret where T : Trigonometry<Ret> {
    return x.cos()
}

fun <T, Ret> tan(x: T): Ret? where T : Trigonometry<Ret> {
    return x.tan()
}

fun <T, Ret> asin(x: T): Ret? where T : Trigonometry<Ret> {
    return x.asin()
}

fun <T, Ret> acos(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acos()
}

fun <T, Ret> atan(x: T): Ret where T : Trigonometry<Ret> {
    return x.atan()
}

//fun <T, Ret> sinh(x: T): Ret where T : Trigonometry<Ret> {
//    return x.sinh()
//}
//
//fun <T, Ret> cosh(x: T): Ret where T : Trigonometry<Ret> {
//    return x.cosh()
//}
//
//fun <T, Ret> tanh(x: T): Ret? where T : Trigonometry<Ret> {
//    return x.tanh()
//}
//
//fun <T, Ret> asinh(x: T): Ret? where T : Trigonometry<Ret> {
//    return x.asinh()
//}
//
//fun <T, Ret> acosh(x: T): Ret? where T : Trigonometry<Ret> {
//    return x.acosh()
//}
//
//fun <T, Ret> atanh(x: T): Ret? where T : Trigonometry<Ret> {
//    return x.atanh()
//}
