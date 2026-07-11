/**
 * 钳位函数
 * Clamp Function
 *
 * 将值限制在指定范围内，确保返回值在 [min, max] 区间内。
 * 数学定义：clamp(v, min, max) = min if v < min, max if v > max, v otherwise。
 * 也称为截断函数或饱和函数，常用于数值范围限制、颜色值归一化等场景。
 * 边界情况：若 min > max，函数仍按定义执行，建议调用者确保 min <= max。
 * 要求类型实现 Ord 接口以支持比较操作。
 *
 * Restricts a value to a specified range, ensuring the result is within [min, max].
 * Mathematical definition: clamp(v, min, max) = min if v < min, max if v > max, v otherwise.
 * Also known as truncation or saturation function, commonly used for value limiting, color normalization, etc.
 * Boundary case: if min > max, function still executes per definition; caller should ensure min <= max.
 * Requires type to implement Ord interface for comparison operations.
*/
package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.utils.functional.Ord

/**
 * 将值限制在 [min, max] 范围内
 * Restricts value to [min, max] range
 *
 * @param v 待限制的值 / Value to restrict
 * @param min 最小值 / Minimum value
 * @param max 最大值 / Maximum value
 * @return 限制后的值 / Restricted value
*/
fun <T : Ord<T>> clamp(v: T, min: T, max: T): T = if (v < min) min else if (v > max) max else v
