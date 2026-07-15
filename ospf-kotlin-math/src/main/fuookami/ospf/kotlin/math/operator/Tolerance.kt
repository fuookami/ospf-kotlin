/**
 * 容差接口
 * Tolerance Interface
 *
 * 定义容差相关接口，用于支持基于容差的相等和顺序比较。
 * 容差是精度控制的核心概念，允许在一定误差范围内判断两个值是否相等。
 *
 * Defines tolerance-related interfaces for supporting tolerance-based equality
 * and ordering comparisons. Tolerance is a core concept in precision control,
 * allowing determination of whether two values are equal within a certain error range.
 *
 * 使用场景 / Use cases:
 * - 浮点数比辌/ Floating-point number comparison
 * - 数值算法中的收敛判斌/ Convergence determination in numerical algorithms
 * - 物理仿真中的误差控制 / Error control in physical simulations
*/
package fuookami.ospf.kotlin.math.operator

import fuookami.ospf.kotlin.utils.functional.Order

/**
 * 容差接口
 * Tolerance Interface
 *
 * 表示一个具有容差值的类型。
 * 实现此接口的类型可以提供其容差值用于比较操作。
 *
 * Represents a type with a tolerance value.
 * Types implementing this interface can provide their tolerance value for comparison operations.
 *
 * @param T 容差值的类型
 *
*/
interface Tolerance<T> {

    /**
     * 容差值，用于判断两个值是否在误差范围内相筌
     * Tolerance value used to determine if two values are equal within error range
    */
    val tolerance: T
}

/**
 * 带容差的相等判断接口
 * Toleranced Equality Interface
 *
 * 定义带容差参数的相等判断函数式接口。
 * 用于判断两个值是否在指定容差范围内相等。
 *
 * Defines a functional interface for tolerance-based equality comparison.
 * Used to determine if two values are equal within a specified tolerance range.
 *
 * @param T 比较值的类型
 *
*/
fun interface TolerancedEq<T> {

    /**
     * 判断两个值是否在指定容差范围内相筌
     * Determines if two values are equal within the specified tolerance
     *
     * @param lhs 左操作数
     * @param rhs 右操作数
     * @param tolerance 容差倌
     * @return 如果两值在容差范围内相等则返回 true
     *
     * @return True if the two values are equal within tolerance
    */
    fun test(lhs: T, rhs: T, tolerance: T): Boolean
}

/**
 * 带容差的顺序比较接口
 * Toleranced Ordering Interface
 *
 * 定义带容差参数的顺序比较函数式接口。
 * 用于比较两个值的大小关系，考虑容差范围。
 *
 * Defines a functional interface for tolerance-based ordering comparison.
 * Used to compare the ordering relationship of two values, considering tolerance range.
 *
 * @param T 比较值的类型
 *
*/
fun interface TolerancedOrd<T> {

    /**
     * 比较两个值的大小关系，考虑指定容差
     * Compares the ordering relationship of two values, considering specified tolerance
     *
     * @param lhs 左操作数
     * @param rhs 右操作数
     * @param tolerance 容差倌
     * @return 比较结果，可以是 Equal、Less 戌Greater
     *
     * @return Comparison result, can be Equal, Less, or Greater
    */
    fun test(lhs: T, rhs: T, tolerance: T): Order
}

/**
 * 绝对容差籌
 * Absolute Tolerance Class
 *
 * 表示一个绝对容差值。
 * 绝对容差是一个固定数值，直接用于比较两数之差。
 *
 * Represents an absolute tolerance value.
 * Absolute tolerance is a fixed numeric value used directly to compare the difference between two numbers.
 *
 * @param T 容差值的类型
 * @param tolerance 绝对容差倌
 *
*/
data class AbsoluteTolerance<T>(
    override val tolerance: T
) : Tolerance<T>
