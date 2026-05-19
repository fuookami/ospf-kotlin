/**
 * List 扩展操作符模块
 * List Extension Operators Module
 *
 * 本模块为嵌套 List 类型提供多维数组风格的访问操作符。
 * This module provides multi-dimensional array style access operators
 * for nested List types.
 *
 * 类型定义：
 * Type definitions:
 * - [List2]: 二维列表类型别名
 *   2D list type alias
 * - [List3]: 三维列表类型别名
 *   3D list type alias
 *
 * 支持的操作：
 * Supported operations:
 * - 使用 All 索引（_a）获取整行或整列
 *   Get entire row or column using All index (_a)
 * - 使用范围索引获取子集
 *   Get subset using range index
 * - 使用 IntRange 进行切片
 *   Slice using IntRange
 *
 * 示例：
 * Example:
 * ```kotlin
 * val matrix: List2<Int> = listOf(
 *     listOf(1, 2, 3),
 *     listOf(4, 5, 6)
 * )
 *
 * // 获取所有行的第 1 列
 * // Get column 1 from all rows
 * val col = matrix[_a, 1]  // [2, 5]
 *
 * // 获取第 0 行的所有列
 * // Get all columns from row 0
 * val row = matrix[0, _a]  // [1, 2, 3]
 * ```
 */
package fuookami.ospf.kotlin.multiarray

import fuookami.ospf.kotlin.utils.functional.get

typealias List2<T> = List<List<T>>
typealias List3<T> = List<List2<T>>

/**
 * List2 的扩展操作符函数
 * Extension operator functions for List2
 *
 * 支持 All 索引（_a）来获取所有元素。
 * Supports All index (_a) to get all elements.
 */
operator fun <T> List2<T>.get(i: DummyIndex.All, j: Int): Iterable<T> {
    return this.mapNotNull { it.getOrNull(j) }
}

operator fun <T> List2<T>.get(i: Int, j: DummyIndex.All): Iterable<T> {
    return this.getOrNull(i) ?: emptyList()
}

operator fun <T> List2<T>.get(i: DummyIndex.All, j: DummyIndex.All): Iterable<T> {
    return this.flatten()
}

/**
 * List3 的扩展操作符函数
 * Extension operator functions for List3
 *
 * 支持 All 索引（_a）来获取所有元素。
 * Supports All index (_a) to get all elements.
 */
operator fun <T> List3<T>.get(i: DummyIndex.All, j: Int, k: Int): Iterable<T> {
    return this.map { it[j, k] }
}

operator fun <T> List3<T>.get(i: Int, j: DummyIndex.All, k: Int): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

operator fun <T> List3<T>.get(i: Int, j: Int, k: DummyIndex.All): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

operator fun <T> List3<T>.get(i: DummyIndex.All, j: DummyIndex.All, k: Int): Iterable<T> {
    return this.flatMap { it[j, k] }
}

operator fun <T> List3<T>.get(i: DummyIndex.All, j: Int, k: DummyIndex.All): Iterable<T> {
    return this.flatMap { it[j, k] }
}

operator fun <T> List3<T>.get(i: Int, j: DummyIndex.All, k: DummyIndex.All): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

operator fun <T> List3<T>.get(i: DummyIndex.All, j: DummyIndex.All, k: DummyIndex.All): Iterable<T> {
    return this.flatMap { it[j, k] }
}