/**
 * 指数运算符
 * Exponential Operator
 *
 * 定义指数运算接口，计算自然常数 e 的指定次幂。
 * 适用于所有实数类型，是自然对数的逆运算。
 *
 * Defines the exponential operation interface for computing e raised to a given power.
 * Applicable to all real number types, and is the inverse operation of natural logarithm.
 *
 * 数学定义 / Mathematical definition:
 * exp(x) = eˣ
 *
 * 其中 e 是自然常数，约等于 2.71828。
 * Where e is Euler's number, approximately 2.71828.
 *
 * 接口说明 / Interface descriptions:
 * - Exp: 基本指数运算接口
 * - ExpP: 带精度参数的指数运算接口，支持指定精度计算
 */
package fuookami.ospf.kotlin.math.operator

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
