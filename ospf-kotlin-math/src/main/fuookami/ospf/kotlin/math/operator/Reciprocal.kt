/**
 * 倒数运算符
 * Reciprocal Operator
 *
 * 定义倒数运算接口，计算数值的倒数（即 1 除以该数）。
 * 倒数是乘法的逆元素，一个数与其倒数的乘积等于 1。
 *
 * Defines the reciprocal operation interface for computing the reciprocal of a number (1 divided by the number).
 * The reciprocal is the multiplicative inverse; the product of a number and its reciprocal equals 1.
 *
 * 数学定义 / Mathematical definition:
 * reciprocal(x) = 1/x
 *
 * 注意：零的倒数未定义，实现在处理此情况时应返回 null 或抛出异常。
 * Note: The reciprocal of zero is undefined; implementations should return null or throw an exception for this case.
 */
package fuookami.ospf.kotlin.math.operator

interface Reciprocal<out Ret> {
    fun reciprocal(): Ret
}
