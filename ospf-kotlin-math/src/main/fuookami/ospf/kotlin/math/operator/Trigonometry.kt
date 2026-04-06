/**
 * 三角函数
 * Trigonometric Functions
 *
 * 定义三角函数和反三角函数接口，包括基本三角函数、双曲函数及其反函数。
 * 这些函数是数学分析的基础工具，广泛应用于几何、物理和工程计算。
 *
 * Defines interfaces for trigonometric and inverse trigonometric functions,
 * including basic trigonometric functions, hyperbolic functions, and their inverses.
 * These functions are fundamental tools in mathematical analysis, widely used in
 * geometry, physics, and engineering calculations.
 *
 * 数学定义 / Mathematical definitions:
 * 基本三角函数 / Basic trigonometric functions:
 * - sin(x): 正弦函数 / sine function
 * - cos(x): 余弦函数 / cosine function
 * - tan(x) = sin(x)/cos(x): 正切函数 / tangent function
 * - sec(x) = 1/cos(x): 正割函数 / secant function
 * - csc(x) = 1/sin(x): 余割函数 / cosecant function
 * - cot(x) = cos(x)/sin(x): 余切函数 / cotangent function
 *
 * 反三角函数 / Inverse trigonometric functions:
 * - asin(x): 反正弦函数 / arcsine function
 * - acos(x): 反余弦函数 / arccosine function
 * - atan(x): 反正切函数 / arctangent function
 * - asec(x): 反正割函数 / arcsecant function
 * - acsc(x): 反余割函数 / arccosecant function
 * - acot(x): 反余切函数 / arccotangent function
 *
 * 双曲函数 / Hyperbolic functions:
 * - sinh(x): 双曲正弦 / hyperbolic sine
 * - cosh(x): 双曲余弦 / hyperbolic cosine
 * - tanh(x): 双曲正切 / hyperbolic tangent
 * - sech(x): 双曲正割 / hyperbolic secant
 * - csch(x): 双曲余割 / hyperbolic cosecant
 * - coth(x): 双曲余切 / hyperbolic cotangent
 *
 * 反双曲函数 / Inverse hyperbolic functions:
 * - asinh(x): 反双曲正弦 / inverse hyperbolic sine
 * - acosh(x): 反双曲余弦 / inverse hyperbolic cosine
 * - atanh(x): 反双曲正切 / inverse hyperbolic tangent
 * - asech(x): 反双曲正割 / inverse hyperbolic secant
 * - acsch(x): 反双曲余割 / inverse hyperbolic cosecant
 * - acoth(x): 反双曲余切 / inverse hyperbolic cotangent
 */
package fuookami.ospf.kotlin.math.operator

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
