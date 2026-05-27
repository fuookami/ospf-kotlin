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

/**
 * 绝对值运算接口
 * Absolute Value Operation Interface
 *
 * 定义绝对值运算，返回当前数值的非负值。
 * 绝对值表示数值到零的距离，始终为非负数。
 *
 * Defines the absolute value operation, returning the non-negative value of the current number.
 * Absolute value represents the distance from the number to zero, always non-negative.
 *
 * @param Ret 绝对值运算的结果类型
 *
 * @param Ret The result type of the absolute value operation
 */
interface Abs<out Ret> {
    /**
     * 计算绝对倌
     * Calculates the absolute value
     *
     * @return 当前数值的绝对值（非负值）
     *
     * @return The absolute value (non-negative value) of the current number
     */
    fun abs(): Ret
}

/**
 * 计算数值的绝对倌
 * Calculates the absolute value of a number
 *
 * @param T 返回值类垌
 * @param U 输入类型，必须实玌Abs 接口
 * @param num 要计算绝对值的数倌
 * @return 数值的绝对倌
 *
 * @param T The return type
 * @param U The input type, must implement the Abs interface
 * @param num The number to calculate absolute value for
 * @return The absolute value of the number
 */
fun <T, U : Abs<T>> abs(num: U): T {
    return num.abs()
}
