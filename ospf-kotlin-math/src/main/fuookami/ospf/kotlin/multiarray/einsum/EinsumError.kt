/**
 * 爱因斯坦求和错误定义
 * Einstein summation error definitions
 *
 * 提供爱因斯坦求和操作的错误类型。
 * Provides error types for Einstein summation operations.
 */
package fuookami.ospf.kotlin.multiarray.einsum

/**
 * 爱因斯坦求和错误类型 / Einstein summation error type
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.EinsumError
 *
 * try {
 *     val result = matmul(a, b)
 * } catch (e: EinsumError) {
 *     println(e.message)
 * }
 * ```
 */
sealed class EinsumError(
    override val message: String
) : Exception(message) {

    /**
     * 维度不匹配错误
     * Dimension mismatch error
     *
     * @param expected 期望的维度数
     * @param actual 实际的维度数
     * @param description 描述信息
     */
    data class DimensionMismatch(
        val expected: Int,
        val actual: Int,
        val description: String
    ) : EinsumError(
        "Dimension mismatch: expected $expected, got $actual. " +
        "维度不匹配：期望 $expected，实陌$actual. ($description)"
    )

    /**
     * 形状不兼容错误
     * Incompatible shapes error
     *
     * @param shape1 第一个张量的形状
     * @param shape2 第二个张量的形状
     * @param description 描述信息
     */
    data class IncompatibleShapes(
        val shape1: List<Int>,
        val shape2: List<Int>,
        val description: String
    ) : EinsumError(
        "Incompatible shapes: ${shape1} vs ${shape2}. " +
        "形状不兼容：${shape1} vs ${shape2}. ($description)"
    )

    /**
     * 索引重复错误
     * Duplicate indices error
     *
     * @param index 重复的索引ID
     */
    data class DuplicateIndices(
        val index: Int
    ) : EinsumError(
        "Duplicate index: $index. 索引重复，index."
    )

    /**
     * 不支持的运算错误
     * Unsupported operation error
     *
     * @param description 描述信息
     */
    data class UnsupportedOperation(
        val description: String
    ) : EinsumError(
        "Unsupported operation: $description. 不支持的运算，description."
    )

    /**
     * 索引越界错误
     * Index out of bounds error
     *
     * @param index 索引值
     * @param maxIndex 最大索引值
     */
    data class IndexOutOfBounds(
        val index: Int,
        val maxIndex: Int
    ) : EinsumError(
        "Index out of bounds: $index exceeds $maxIndex. " +
        "索引越界，index 超出 $maxIndex."
    )

    /**
     * 索引列表长度不匹配错误
     * Index list length mismatch error
     *
     * @param expected 期望的长度
     * @param actual 实际的长度
     * @param description 描述信息
     */
    data class IndexListLengthMismatch(
        val expected: Int,
        val actual: Int,
        val description: String
    ) : EinsumError(
        "Index list length mismatch: expected $expected, got $actual. " +
        "索引列表长度不匹配：期望 $expected，实陌$actual. ($description)"
    )
}
