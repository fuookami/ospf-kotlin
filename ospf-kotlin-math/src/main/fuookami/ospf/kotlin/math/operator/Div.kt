/**
 * 除法运算符
 * Division Operator
 *
 * 定义除法运算相关接口，包括常规除法、除法赋值和整数除法。
 * 支持运算符重载，允许使用 / 和 /= 运算符进行除法运算。
 *
 * Defines interfaces related to division operations, including regular division,
 * division assignment, and integer division.
 * Supports operator overloading, allowing the use of / and /= operators for division.
 *
 * 数学定义 / Mathematical definition:
 * a / b = a * b⁻¹ (常规除法 / regular division)
 * a intDiv b = floor(a / b) (整数除法 / integer division)
 *
 * 接口说明 / Interface descriptions:
 * - Div: 除法运算符接口，支持 a / b
 * - DivAssign: 除法赋值接口，支持 a /= b
 * - IntDiv: 整数除法接口，支持 a intDiv b
 * - IntDivAssign: 整数除法赋值接口，支持 a intDivAssign b
 */
package fuookami.ospf.kotlin.math.operator

interface Div<in Rhs, out Ret> {
    operator fun div(rhs: Rhs): Ret
}

interface DivAssign<in Rhs> {
    operator fun divAssign(rhs: Rhs)
}

interface IntDiv<in Rhs, out Ret> {
    infix fun intDiv(rhs: Rhs): Ret
}


interface IntDivAssign<in Rhs> {
    infix fun intDivAssign(rhs: Rhs)
}
