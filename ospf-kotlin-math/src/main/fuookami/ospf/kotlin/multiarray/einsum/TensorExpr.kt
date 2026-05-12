/**
 * 带索引标记的张量表达弌
 * Tensor expression with index labels
 *
 * 提供带编译期索引标记的张量包装类型。
 * Provides tensor wrapper types with compile-time index labels.
 */
package fuookami.ospf.kotlin.multiarray.einsum

import fuookami.ospf.kotlin.multiarray.*

/**
 * 带索引标记的张量表达弌/ Tensor expression with index labels
 *
 * 尌MultiArray 与索引列表关联，用于爱因斯坦求和操作。
 * Associates a MultiArray with an index list for Einstein summation operations.
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.*
 *
 * // 创建一丌2x3 矩阵 / Create a 2x3 matrix
 * val matrix = MultiArray.newWith(Shape2(2, 3), 1.0)
 *
 * // 创建带索引标记的张量表达弌
 * // Create tensor expression with index labels
 * val expr = TensorExpr(matrix, IndexList.of(IndexLabel.I, IndexLabel.J))
 *
 * println(expr.indexNames)  // "i, j"
 * println(expr.size)        // 6
 * ```
 *
 * @param T 元素类型
 * @param S 形状类型
 */
data class TensorExpr<T : Any, S : Shape>(
    /**
     * 数据引用
     * Data reference
     */
    val data: AbstractMultiArray<T, S>,

    /**
     * 索引列表
     * Index list
     */
    val indices: IndexList
) {
    /**
     * 获取形状引用
     * Get shape reference
     */
    val shape: S = data.shape

    /**
     * 获取索引列表名称
     * Get index list names
     */
    val indexNames: String = indices.names

    /**
     * 获取索引列表 ID
     * Get index list IDs
     */
    val indexIds: List<Int> = indices.ids

    /**
     * 获取数组长度（元素总数，
     * Get array length (total elements)
     */
    val size: Int = data.size

    /**
     * 获取维度敌
     * Get number of dimensions
     */
    val dimension: Int = data.shape.dimension

    init {
        // 验证维度数与索引列表长度匹配
        // Validate dimension matches index list length
        if (indices.length != data.shape.dimension) {
            throw EinsumError.IndexListLengthMismatch(
                expected = data.shape.dimension,
                actual = indices.length,
                description = "Array dimension must match index list length"
            )
        }
    }

    companion object {
        /**
         * 创建带索引的张量表达弌
         * Create tensor expression with indices
         *
         * @param data 多维数组
         * @param indices 索引标签数组
         * @return 张量表达弌
         */
        fun <T : Any, S : Shape> new(
            data: AbstractMultiArray<T, S>,
            indices: IndexList
        ): TensorExpr<T, S> {
            return TensorExpr(data, indices)
        }

        /**
         * 从索引标签数组创建张量表达式
         * Create tensor expression from index label array
         *
         * @param data 多维数组
         * @param labels 索引标签数组
         * @return 张量表达弌
         */
        fun <T : Any, S : Shape> new(
            data: AbstractMultiArray<T, S>,
            labels: Array<IndexLabel>
        ): TensorExpr<T, S> {
            return TensorExpr(data, IndexList(labels.toList()))
        }

        /**
         * 使用默认索引创建张量表达式（自动分配 I, J, K, ...，
         * Create tensor expression with default indices (auto-assign I, J, K, ...)
         *
         * @param data 多维数组
         * @return 张量表达弌
         */
        fun <T : Any, S : Shape> withDefaultIndices(
            data: AbstractMultiArray<T, S>
        ): TensorExpr<T, S> {
            val defaultIndices = IndexLabel.values()
                .take(data.shape.dimension)
                .toList()
            return TensorExpr(data, IndexList(defaultIndices))
        }
    }
}

/**
 * 使用默认索引创建张量表达式的便捷函数
 * Convenience function to create tensor expression with default indices
 *
 * 示例 / Example:
 *
 * ```kotlin
 * val matrix = MultiArray.newWith(Shape2(2, 3), 1.0)
 * val expr = tensorExpr(matrix)
 * // 自动分配索引: i, j
 * // Auto-assigned indices: i, j
 * ```
 *
 * @param data 多维数组
 * @return 带默认索引的张量表达弌
 */
fun <T : Any, S : Shape> tensorExpr(data: AbstractMultiArray<T, S>): TensorExpr<T, S> {
    return TensorExpr.withDefaultIndices(data)
}

/**
 * 使用指定索引创建张量表达式的便捷函数
 * Convenience function to create tensor expression with specified indices
 *
 * 示例 / Example:
 *
 * ```kotlin
 * val matrix = MultiArray.newWith(Shape2(2, 3), 1.0)
 * val expr = tensorExpr(matrix, IndexLabel.I, IndexLabel.J)
 * ```
 *
 * @param data 多维数组
 * @param indices 索引标签数组
 * @return 带指定索引的张量表达弌
 */
fun <T : Any, S : Shape> tensorExpr(
    data: AbstractMultiArray<T, S>,
    vararg indices: IndexLabel
): TensorExpr<T, S> {
    return TensorExpr(data, IndexList(indices.toList()))
}
