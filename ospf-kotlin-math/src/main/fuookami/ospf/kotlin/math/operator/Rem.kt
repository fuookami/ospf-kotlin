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

interface Rem<in Rhs, out Ret> {
    operator fun rem(rhs: Rhs): Ret

    infix fun mod(rhs: Rhs) = this % rhs
}

interface RemAssign<in Rhs> {
    operator fun remAssign(rhs: Rhs)
}
