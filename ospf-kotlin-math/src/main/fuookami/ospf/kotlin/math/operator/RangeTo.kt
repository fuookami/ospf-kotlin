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

/**
 * 范围运算符接口
 * Range Operator Interface
 *
 * 定义范围运算，支持 .. 和 until 运算符。
 * 用于创建闭区间和半开区间的范围对象。
 *
 * Defines range operations, supporting .. and until operators.
 * Used to create closed interval and half-open interval range objects.
 *
 * @param Rhs 右操作数（范围终点）的类型
 * @param Ret 返回值（范围类型）的类型，必须是可比较的
 *
 * @param Rhs The type of the right operand (range end)
 * @param Ret The type of the return value (range type), must be Comparable
 */
interface RangeTo<in Rhs, out Ret : Comparable<@UnsafeVariance Ret>> {
    /**
     * 创建闭区间范围 [this, rhs]
     * Creates a closed interval range [this, rhs]
     *
     * @param rhs 范围终点
     * @return 闭区间范围对象
     *
     * @param rhs Range end
     * @return Closed interval range object
     */
    operator fun rangeTo(rhs: Rhs): ClosedRange<@UnsafeVariance Ret>

    /**
     * 创建半开区间范围 [this, rhs)
     * Creates a half-open interval range [this, rhs)
     *
     * @param rhs 范围终点（不包含）
     * @return 半开区间范围对象
     *
     * @param rhs Range end (exclusive)
     * @return Half-open interval range object
     */
    infix fun until(rhs: Rhs): ClosedRange<@UnsafeVariance Ret>
}