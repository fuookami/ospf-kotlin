/**
 * 取余运算符
 * Remainder Operator
 *
 * 定义取余（模）运算相关接口，包括取余运算和取余赋值。
 * 支持运算符重载，允许使用 % 运算符和 mod 函数。
 *
 * Defines interfaces related to remainder (modulo) operations, including remainder and remainder assignment.
 * Supports operator overloading, allowing the use of % operator and mod function.
 *
 * 数学定义 / Mathematical definition:
 * a % b = a - (a / b) * b (取余运算 / remainder operation)
 * a mod b 与 a % b 等价 / a mod b is equivalent to a % b
 *
 * 接口说明 / Interface descriptions:
 * - Rem: 取余运算符接口，支持 a % b 和 a mod b
 * - RemAssign: 取余赋值接口，支持 a %= b
 *
 * 注意：取余运算结果的符号取决于实现，应参考具体类型的文档。
 * Note: The sign of the remainder result depends on the implementation; refer to specific type documentation.
 */
package fuookami.ospf.kotlin.math.operator

/**
 * 取余运算符接口
 * Remainder Operator Interface
 *
 * 定义取余运算，支持 % 运算符和 mod 中缀函数。
 * 取余运算返回除法的余数部分。
 *
 * Defines the remainder operation, supporting % operator and mod infix function.
 * Remainder operation returns the remainder part of division.
 *
 * @param Rhs 右操作数（除数）的类型
 * @param Ret 返回值（余数）的类型
 *
 * @param Rhs The type of the right operand (divisor)
 * @param Ret The type of the return value (remainder)
 */
interface Rem<in Rhs, out Ret> {
    /**
     * 取余运算符，计算余数
     * Remainder operator, calculates the remainder
     *
     * @param rhs 除数
     * @return 余数
     *
     * @param rhs Divisor
     * @return Remainder
     */
    operator fun rem(rhs: Rhs): Ret

    /**
     * 取模运算符，与 rem 等价
     * Modulo operator, equivalent to rem
     *
     * @param rhs 除数
     * @return 余数
     *
     * @param rhs Divisor
     * @return Remainder
     */
    infix fun mod(rhs: Rhs) = this % rhs
}

/**
 * 取余赋值接口
 * Remainder Assignment Interface
 *
 * 定义取余赋值运算，支持 %= 运算符。
 * 将左操作数取余右操作数，并将结果赋值给左操作数。
 *
 * Defines the remainder assignment operation, supporting %= operator.
 * Computes the remainder of left operand divided by right operand and assigns the result to the left operand.
 *
 * @param Rhs 右操作数（除数）的类型
 *
 * @param Rhs The type of the right operand (divisor)
 */
interface RemAssign<in Rhs> {
    /**
     * 取余赋值运算符，将余数赋值给左操作数
     * Remainder assignment operator, assigns the remainder to the left operand
     *
     * @param rhs 除数
     *
     * @param rhs Divisor
     */
    operator fun remAssign(rhs: Rhs)
}