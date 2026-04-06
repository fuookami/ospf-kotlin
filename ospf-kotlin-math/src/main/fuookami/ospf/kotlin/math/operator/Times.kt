/**
 * 乘法运算符
 * Multiplication Operator
 *
 * 定义乘法运算相关接口，包括标量乘法和向量叉积。
 * 支持运算符重载，允许使用 * 运算符和 x 函数。
 *
 * Defines interfaces related to multiplication operations, including scalar multiplication and vector cross product.
 * Supports operator overloading, allowing the use of * operator and x function.
 *
 * 数学定义 / Mathematical definitions:
 * - a * b (标量乘法 / scalar multiplication)
 * - a x b (向量叉积 / vector cross product，主要用于三维向量)
 *
 * 接口说明 / Interface descriptions:
 * - Times: 乘法运算符接口，支持 a * b
 * - TimesAssign: 乘法赋值接口，支持 a *= b
 * - Cross: 叉积运算符接口，支持 a x b（主要用于向量运算）
 */
package fuookami.ospf.kotlin.math.operator

interface Times<in Rhs, out Ret> {
    operator fun times(rhs: Rhs): Ret
}

interface TimesAssign<in Rhs> {
    operator fun timesAssign(rhs: Rhs)
}

interface Cross<in Rhs, out Ret> {
    infix fun x(rhs: Rhs): Ret
}
