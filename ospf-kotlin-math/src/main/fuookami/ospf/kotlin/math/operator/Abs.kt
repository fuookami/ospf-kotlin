/**
 * 绝对值运算符
 * Absolute Value Operator
 *
 * 定义绝对值运算的接口，用于获取数值的非负值。
 * 适用于所有实数类型，包括有符号整数、浮点数和自定义数值类型。
 *
 * Defines the interface for absolute value operation, used to obtain the non-negative value of a number.
 * Applicable to all real number types, including signed integers, floating-point numbers, and custom numeric types.
 *
 * 数学定义 / Mathematical definition:
 * |x| = x  if x >= 0
 * |x| = -x if x < 0
 *
 * 使用示例 / Usage example:
 * val result = abs(-5)  // result = 5
 */
package fuookami.ospf.kotlin.math.operator

interface Abs<out Ret> {
    fun abs(): Ret
}

fun <T, U : Abs<T>> abs(num: U): T {
    return num.abs()
}
