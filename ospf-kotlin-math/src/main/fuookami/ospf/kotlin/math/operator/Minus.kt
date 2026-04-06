/**
 * 减法运算符
 * Subtraction Operator
 *
 * 定义减法运算相关接口，包括减法运算和自减运算。
 * 支持运算符重载，允许使用 - 和 -- 运算符。
 *
 * Defines interfaces related to subtraction operations, including subtraction and decrement.
 * Supports operator overloading, allowing the use of - and -- operators.
 *
 * 数学定义 / Mathematical definition:
 * a - b (减法 / subtraction)
 * --a = a - 1 (自减 / decrement)
 *
 * 接口说明 / Interface descriptions:
 * - Minus: 减法运算符接口，支持 a - b
 * - MinusAssign: 减法赋值接口，支持 a -= b
 * - Dec: 自减运算符接口，支持 --a 或 a--
 */
package fuookami.ospf.kotlin.math.operator

interface Minus<in Rhs, out Ret> {
    operator fun minus(rhs: Rhs): Ret
}

interface MinusAssign<in Rhs> {
    operator fun minusAssign(rhs: Rhs)
}

interface Dec<Self> {
    operator fun dec(): Dec<Self>
}
