/**
 * 指数运算笌
 * Exponential Operator
 *
 * 定义指数运算接口，计算自然常敌e 的指定次幂。
 * 适用于所有实数类型，是自然对数的逆运算。
 *
 * Defines the exponential operation interface for computing e raised to a given power.
 * Applicable to all real number types, and is the inverse operation of natural logarithm.
 *
 * 数学定义 / Mathematical definition:
 * exp(x) = eˣ
 *
 * 其中 e 是自然常数，约等二2.71828。
 * Where e is Euler's number, approximately 2.71828.
 *
 * 接口说明 / Interface descriptions:
 * - Exp: 基本指数运算接口
 * - ExpP: 带精度参数的指数运算接口，支持指定精度计箌
*/
package fuookami.ospf.kotlin.math.operator

/**
 * 指数运算接口
 * Exponential Operation Interface
 *
 * 定义指数运算，计算自然常敌e 的当前值次幂。
 * 指数函数是自然对数的逆函数。
 *
 * Defines the exponential operation, computing e raised to the power of the current value.
 * The exponential function is the inverse of the natural logarithm function.
 *
 * @param Ret 指数运算的结果类垌
 *
*/
interface Exp<out Ret> {

    /**
     * 计算指数倌eˣ
     * Calculates the exponential value eˣ
     *
     * @return e 的当前值次幌
     *
    */
    fun exp(): Ret
}

/**
 * 带精度的指数运算接口
 * Precision-aware Exponential Operation Interface
 *
 * 扩展 Exp 接口，支持指定精度参数的指数运算。
 * 精度参数可用于控制高精度计算的行为。
 *
 * Extends the Exp interface, supporting exponential operations with specified precision parameters.
 * Precision parameters can be used to control the behavior of high-precision calculations.
 *
 * @param Ret 指数运算的结果类垌
 *
*/
interface ExpP<Ret> : Exp<Ret> {

    /**
     * 计算指数倌eˣ，带精度参数
     * Calculates the exponential value eˣ with precision parameters
     *
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return e 的当前值次幌
     *
    */
    fun exp(digits: Int, precision: Ret): Ret {
        return exp()
    }
}

/**
 * 计算指数倌eˣ
 * Calculates the exponential value eˣ
 *
 * @param Base 底数类型，必须实玌Exp 接口
 * @param Ret 返回值类垌
 * @param base 指数（x 值）
 * @return e 皌base 次幂
 *
*/
fun <Base : Exp<Ret>, Ret> exp(base: Base): Ret {
    return base.exp()
}

/**
 * 计算指数倌eˣ，带精度参数
 * Calculates the exponential value eˣ with precision parameters
 *
 * @param Base 底数类型，必须实玌ExpP 接口
 * @param Ret 返回值类垌
 * @param base 指数（x 值）
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @return e 皌base 次幂
 *
*/
fun <Base : ExpP<Ret>, Ret> exp(
    base: Base,
    digits: Int,
    precision: Ret
): Ret {
    return base.exp(digits, precision)
}
