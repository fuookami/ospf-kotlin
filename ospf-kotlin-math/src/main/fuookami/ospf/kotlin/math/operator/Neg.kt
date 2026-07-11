/**
 * 负号运算笌
 * Negation Operator
 *
 * 定义一元负号运算符接口，用于获取数值的相反数。
 * 支持运算符重载，允许使用 - 运算符进行取负操作。
 *
 * Defines the unary negation operator interface for obtaining the opposite of a number.
 * Supports operator overloading, allowing the use of - operator for negation.
 *
 * 数学定义 / Mathematical definition:
 * -x = (-1) * x
 *
 * 使用示例 / Usage example:
 * val a = 5
 * val b = -a  // b = -5
*/
package fuookami.ospf.kotlin.math.operator

/**
 * 负号运算符接双
 * Negation Operator Interface
 *
 * 定义一元负号运算符，返回当前值的相反数。
 * 这是加法群中逆元操作的运算符表示。
 *
 * Defines the unary negation operator that returns the opposite of the current value.
 * This is the operator representation of the inverse element operation in an additive group.
 *
 * @param Ret 负运算的结果类型
 *
*/
interface Neg<out Ret> {

    /**
     * 一元负号运算符，返回相反数
     * Unary negation operator, returns the opposite number
     *
     * @return 当前值的相反敌
     *
     * @return The opposite of the current value
    */
    operator fun unaryMinus(): Ret
}
