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

/** 二维列表类型别名 / 2D list type alias */
typealias List2<T> = List<List<T>>

/** 三维列表类型别名 / 3D list type alias */
typealias List3<T> = List<List2<T>>

/**
 * 通过 All 索引和 Int 索引获取所有行的指定列
 * Get specified column from all rows using All index and Int index
 *
 * @param i All 索引 / All index
 * @param j 列索引 / Column index
 * @return 指定列的所有元素 / All elements from the specified column
 */
operator fun <T> List2<T>.get(i: DummyIndex.All, j: Int): Iterable<T> {
    return this.mapNotNull { it.getOrNull(j) }
}

/**
 * 获取指定行的所有列
 * Get all columns from the specified row
 *
 * @param i 行索引 / Row index
 * @param j All 索引 / All index
 * @return 指定行的所有元素 / All elements from the specified row
 */
operator fun <T> List2<T>.get(i: Int, j: DummyIndex.All): Iterable<T> {
    return this.getOrNull(i) ?: emptyList()
}

/**
 * 获取所有元素
 * Get all elements
 *
 * @param i All 索引 / All index
 * @param j All 索引 / All index
 * @return 所有元素 / All elements
 */
operator fun <T> List2<T>.get(i: DummyIndex.All, j: DummyIndex.All): Iterable<T> {
    return this.flatten()
}

/**
 * 通过 All 索引获取所有层的指定行指定列
 * Get specified row and column from all layers using All index
 *
 * @param i All 索引 / All index
 * @param j 行索引 / Row index
 * @param k 列索引 / Column index
 * @return 所有层的指定行指定列元素 / Elements at specified row and column from all layers
 */
operator fun <T> List3<T>.get(i: DummyIndex.All, j: Int, k: Int): Iterable<T> {
    return this.map { it[j, k] }
}

/**
 * 获取指定层指定列的所有行
 * Get all rows from the specified layer and column
 *
 * @param i 层索引 / Layer index
 * @param j All 索引 / All index
 * @param k 列索引 / Column index
 * @return 指定层指定列的所有行元素 / All row elements from the specified layer and column
 */
operator fun <T> List3<T>.get(i: Int, j: DummyIndex.All, k: Int): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

/**
 * 获取指定层指定行的所有列
 * Get all columns from the specified layer and row
 *
 * @param i 层索引 / Layer index
 * @param j 行索引 / Row index
 * @param k All 索引 / All index
 * @return 指定层指定行的所有列元素 / All column elements from the specified layer and row
 */
operator fun <T> List3<T>.get(i: Int, j: Int, k: DummyIndex.All): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

/**
 * 获取指定列的所有元素
 * Get all elements from the specified column
 *
 * @param i All 索引 / All index
 * @param j All 索引 / All index
 * @param k 列索引 / Column index
 * @return 指定列的所有元素 / All elements from the specified column
 */
operator fun <T> List3<T>.get(i: DummyIndex.All, j: DummyIndex.All, k: Int): Iterable<T> {
    return this.flatMap { it[j, k] }
}

/**
 * 获取指定行的所有元素
 * Get all elements from the specified row
 *
 * @param i All 索引 / All index
 * @param j 行索引 / Row index
 * @param k All 索引 / All index
 * @return 指定行的所有元素 / All elements from the specified row
 */
operator fun <T> List3<T>.get(i: DummyIndex.All, j: Int, k: DummyIndex.All): Iterable<T> {
    return this.flatMap { it[j, k] }
}

/**
 * 获取指定层的所有元素
 * Get all elements from the specified layer
 *
 * @param i 层索引 / Layer index
 * @param j All 索引 / All index
 * @param k All 索引 / All index
 * @return 指定层的所有元素 / All elements from the specified layer
 */
operator fun <T> List3<T>.get(i: Int, j: DummyIndex.All, k: DummyIndex.All): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

/**
 * 获取所有元素
 * Get all elements
 *
 * @param i All 索引 / All index
 * @param j All 索引 / All index
 * @param k All 索引 / All index
 * @return 所有元素 / All elements
 */
operator fun <T> List3<T>.get(i: DummyIndex.All, j: DummyIndex.All, k: DummyIndex.All): Iterable<T> {
    return this.flatMap { it[j, k] }
}
