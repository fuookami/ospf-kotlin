/**
 * 带索引标记的张量表达式
 * Tensor expression with index labels
 *
 * 提供带编译期索引标记的张量包装类型。
 * Provides tensor wrapper types with compile-time index labels.
*/
package fuookami.ospf.kotlin.multiarray.einsum

import kotlin.ConsistentCopyVisibility
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 带索引标记的张量表达式/ Tensor expression with index labels
 *
 * 尌MultiArray 与索引列表关联，用于爱因斯坦求和操作。
 * Associates a MultiArray with an index list for Einstein summation operations.
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.*
 *
 * // 创建一个 2x3 矩阵 / Create a 2x3 matrix
 * val matrix = MultiArray.newWith(Shape2(2, 3), 1.0)
 *
 * // 创建带索引标记的张量表达式
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
@ConsistentCopyVisibility
data class TensorExpr<T : Any, S : Shape> private constructor(

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
     * 获取维度数
     * Get number of dimensions
    */
    val dimension: Int = data.shape.dimension

    /** 工厂方法 / Factory methods */
    companion object {
        /**
         * 创建带索引的张量表达式，失败时返回 null
         * Create tensor expression with indices, returning null on failure
         *
         * @param data 多维数组
         * @param indices 索引标签数组
         * @return 张量表达式或 null
        */
        fun <T : Any, S : Shape> newOrNull(
            data: AbstractMultiArray<T, S>,
            indices: IndexList
        ): TensorExpr<T, S>? {
            return newSafe(data, indices).value
        }

        /**
         * 创建带索引的张量表达式，失败时返回 Failed
         * Create tensor expression with indices, returning Failed on failure
         *
         * @param data 多维数组
         * @param indices 索引列表
         * @return 张量表达式结果
        */
        fun <T : Any, S : Shape> newSafe(
            data: AbstractMultiArray<T, S>,
            indices: IndexList
        ): Ret<TensorExpr<T, S>> {
            if (indices.length != data.shape.dimension) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Array dimension must match index list length: expected=${data.shape.dimension}, actual=${indices.length}."
                )
            }
            return Ok(TensorExpr(data, indices))
        }

        /**
         * 创建带索引的张量表达式
         * Create tensor expression with indices
         *
         * @param data 多维数组
         * @param indices 索引列表
         * @return 张量表达式结果
        */
        fun <T : Any, S : Shape> new(
            data: AbstractMultiArray<T, S>,
            indices: IndexList
        ): Ret<TensorExpr<T, S>> {
            return newSafe(data, indices)
        }

        /**
         * 从索引标签数组创建张量表达式
         * Create tensor expression from index label array
         *
         * @param data 多维数组
         * @param labels 索引标签数组
         * @return 张量表达式结果
        */
        fun <T : Any, S : Shape> new(
            data: AbstractMultiArray<T, S>,
            labels: Array<IndexLabel>
        ): Ret<TensorExpr<T, S>> {
            return newSafe(data, IndexList(labels.toList()))
        }

        /**
         * 使用默认索引创建张量表达式（自动分配 I, J, K, ...）
         * Create tensor expression with default indices (auto-assign I, J, K, ...)
         *
         * @param data 多维数组
         * @return 张量表达式结果
        */
        fun <T : Any, S : Shape> withDefaultIndices(
            data: AbstractMultiArray<T, S>
        ): Ret<TensorExpr<T, S>> {
            val defaultIndices = IndexLabel.values()
                .take(data.shape.dimension)
                .toList()
            return newSafe(data, IndexList(defaultIndices))
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
 * @return 带默认索引的张量表达式
*/
fun <T : Any, S : Shape> tensorExpr(data: AbstractMultiArray<T, S>): Ret<TensorExpr<T, S>> {
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
 * @return 带指定索引的张量表达式
*/
fun <T : Any, S : Shape> tensorExpr(
    data: AbstractMultiArray<T, S>,
    vararg indices: IndexLabel
): Ret<TensorExpr<T, S>> {
    return TensorExpr.new(data, IndexList(indices.toList()))
}
