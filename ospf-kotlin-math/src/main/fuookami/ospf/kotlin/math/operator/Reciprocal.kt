/**
 * 倒数运算笌
 * Reciprocal Operator
 *
 * 定义倒数运算接口，计算数值的倒数（即 1 除以该数）。
 * 倒数是乘法的逆元素，一个数与其倒数的乘积等二1。
 *
 * Defines the reciprocal operation interface for computing the reciprocal of a number (1 divided by the number).
 * The reciprocal is the multiplicative inverse; the product of a number and its reciprocal equals 1.
 *
 * 数学定义 / Mathematical definition:
 * reciprocal(x) = 1/x
 *
 * 注意：零的倒数未定义，实现在处理此情况时应返回 null 或 Result 失败。
 * Note: The reciprocal of zero is undefined; implementations should return null or a Result failure for this case.
 */
package fuookami.ospf.kotlin.math.operator

/**
 * 倒数运算接口
 * Reciprocal Operation Interface
 *
 * 定义倒数运算，返回当前数值的倒数，/x）。
 * 倒数是乘法逆元素的运算符表示，即满趌x * reciprocal(x) = 1。
 *
 * Defines the reciprocal operation, returning the reciprocal (1/x) of the current value.
 * Reciprocal is the operator representation of the multiplicative inverse, satisfying x * reciprocal(x) = 1.
 *
 * @param Ret 倒数运算的结果类垌
 *
 * @param Ret The result type of the reciprocal operation
 */
interface Reciprocal<out Ret> {
    /**
     * 计算倒数 1/x
     * Calculates the reciprocal 1/x
     *
     * @return 当前值的倒数
     *
     * @return The reciprocal of the current value
     */
    fun reciprocal(): Ret
}
