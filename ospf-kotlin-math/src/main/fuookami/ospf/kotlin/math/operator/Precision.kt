/**
 * 精度比较工具
 * Precision Comparison Utility
 *
 * 提供基于精度的数值比较功能，用于处理浮点数等实数的近似相等和比较。
 * 由于浮点数存在精度问题，直接使用 == 比较可能导致错误结果，
 * 本模块提供了考虑精度的比较方法。
 *
 * Provides precision-based numeric comparison functionality for handling approximate
 * equality and comparison of real numbers like floating-point numbers.
 * Due to precision issues with floating-point numbers, direct comparison using ==
 * may lead to incorrect results; this module provides precision-aware comparison methods.
 *
 * 使用示例 / Usage example:
 * ```kotlin
 * val precision = withPrecision<Double>()
 * with(precision) {
 *     val a = 0.1 + 0.2
 *     val b = 0.3
 *     if (a eq b) { /* a 和b 在精度范围内相等 */ }
 * }
 * ```
*/
package fuookami.ospf.kotlin.math.operator

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 精度比较籌
 * Precision Comparison Class
 *
 * 提供基于指定精度的数值比较扩展函数。
 * 所有比较操作都考虑精度容差，适用于浮点数等实数类型的比较。
 *
 * Provides extension functions for precision-based numeric comparison.
 * All comparison operations consider the precision tolerance, suitable for
 * comparing real number types like floating-point numbers.
 *
 * @param T 数值类型，必须是实数、加法群且支持绝对值运算
 * @property precision 比较精度，用于判断两个数是否相等
 *
*/
class Precision<T>(
    precision: T
) where T : RealNumber<T>, T : PlusGroup<T>, T : Abs<T> {

    /**
     * 比较精度（取绝对值确保非负）
     * Comparison precision (absolute value ensures non-negative)
    */
    private val precision = precision.abs()

    /**
     * 判断两个数是否在精度范围内相筌
     * Determines if two numbers are equal within precision tolerance
     *
     * @param other 要比较的另一个数
     * @return 如果两数之差的绝对值小于等于精度则返回 true
     *
     * @return True if the absolute difference is less than or equal to precision
    */
    infix fun T.equal(other: T): Boolean {
        return (this - other).abs() <= precision
    }

    /**
     * 比较两个数的大小关系
     * Compares the order relationship between two numbers
     *
     * @param other 要比较的另一个数
     * @return 比较结果，可以是 Equal、Less 戌Greater
     *
     * @return Comparison result, can be Equal, Less, or Greater
    */
    infix fun T.order(other: T): Order {
        return if (this eq other) {
            Order.Equal
        } else {
            val value = this.compareTo(other)
            if (value < 0) {
                Order.Less(value)
            } else {
                Order.Greater(value)
            }
        }
    }

    /**
     * 判断两个数是否在精度范围内不相等
     * Determines if two numbers are unequal within precision tolerance
     *
     * @param other 要比较的另一个数
     * @return 如果两数之差的绝对值大于精度则返回 true
     *
     * @return True if the absolute difference is greater than precision
    */
    infix fun T.unequal(other: T): Boolean {
        return (this - other).abs() > precision
    }

    /**
     * 判断当前数是否小于另一个数（考虑精度，
     * Determines if the current number is less than another (with precision)
     *
     * @param other 要比较的另一个数
     * @return 如果当前数明显小于另一个数（差值大于等于精度）则返囌true
     *
     * @return True if current number is distinctly less than the other (difference >= precision)
    */
    infix fun T.less(other: T): Boolean {
        return (other - this) >= precision
    }

    /**
     * 判断当前数是否小于等于另一个数（考虑精度，
     * Determines if the current number is less than or equal to another (with precision)
     *
     * @param other 要比较的另一个数
     * @return 如果当前数小于等于另一个数（差值小于等于精度）则返囌true
     *
     * @return True if current number is less than or equal to the other (difference <= precision)
    */
    infix fun T.lessEqual(other: T): Boolean {
        return (this - other) <= precision
    }

    /**
     * 判断当前数是否大于另一个数（考虑精度，
     * Determines if the current number is greater than another (with precision)
     *
     * @param other 要比较的另一个数
     * @return 如果当前数明显大于另一个数（差值大于精度）则返囌true
     *
     * @return True if current number is distinctly greater than the other (difference > precision)
    */
    infix fun T.greater(other: T): Boolean {
        return (this - other) > precision
    }

    /**
     * 判断当前数是否大于等于另一个数（考虑精度，
     * Determines if the current number is greater than or equal to another (with precision)
     *
     * @param other 要比较的另一个数
     * @return 如果当前数大于等于另一个数（差值小于等于精度）则返囌true
     *
     * @return True if current number is greater than or equal to the other (difference <= precision)
    */
    infix fun T.greaterEqual(other: T): Boolean {
        return (other - this) <= precision
    }
}

/**
 * 创建精度比较工具实例（使用实数常量）
 * Creates a precision comparison utility instance (using real number constants)
 *
 * @param T 数值类垌
 * @param constants 实数常量提供而
 * @param precision 比较精度，默认使用类型的十进制精庌
 * @return 精度比较工具实例
 *
 * @return Precision comparison utility instance
*/
fun <T> withPrecision(
    constants: RealNumberConstants<T>,
    precision: T = constants.decimalPrecision
): Precision<T> where T : RealNumber<T>, T : PlusGroup<T>, T : Abs<T> = Precision(precision)

/**
 * 创建精度比较工具实例（使用具象化类型，
 * Creates a precision comparison utility instance (using reified type)
 *
 * @param T 数值类垌
 * @param precision 比较精度，如果为 null 则使用类型的默认精度
 * @return 精度比较工具实例
 *
 * @return Precision comparison utility instance
*/
inline fun <reified T> withPrecision(
    precision: T? = null
): Ret<Precision<T>> where T : RealNumber<T>, T : PlusGroup<T>, T : Abs<T> {
    if (precision != null) {
        return Ok(Precision(precision))
    }
    return resolveRealNumberConstantsSafe<T>("Precision").mapResolved { constants ->
        withPrecision(constants, constants.decimalPrecision)
    }
}
