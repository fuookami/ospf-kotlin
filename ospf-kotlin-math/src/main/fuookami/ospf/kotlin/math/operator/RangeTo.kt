/**
 * 范围运算符
 * Range Operator
 *
 * 定义范围运算接口，用于创建闭区间范围。
 * 支持运算符重载，允许使用 .. 和 until 运算符创建范围。
 *
 * Defines the range operation interface for creating closed interval ranges.
 * Supports operator overloading, allowing the use of .. and until operators to create ranges.
 *
 * 数学定义 / Mathematical definitions:
 * - a..b: 闭区间 [a, b]，包含 a 和 b / closed interval [a, b], inclusive of both endpoints
 * - a until b: 左闭右开区间 [a, b)，包含 a 但不包含 b / half-open interval [a, b), inclusive of a but exclusive of b
 *
 * 使用示例 / Usage example:
 * val range = 1..10  // [1, 10]
 * val halfOpen = 1 until 10  // [1, 10)
 */
package fuookami.ospf.kotlin.math.operator

interface RangeTo<in Rhs, out Ret : Comparable<@UnsafeVariance Ret>> {
    operator fun rangeTo(rhs: Rhs): ClosedRange<@UnsafeVariance Ret>

    infix fun until(rhs: Rhs): ClosedRange<@UnsafeVariance Ret>
}
