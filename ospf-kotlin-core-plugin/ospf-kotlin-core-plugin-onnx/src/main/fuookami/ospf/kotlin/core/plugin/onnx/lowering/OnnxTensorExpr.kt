/**
 * ONNX 张量表达式与形状运算 / ONNX tensor expressions and shape operations
*/
package fuookami.ospf.kotlin.core.plugin.onnx.lowering

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 降阶次数模式 / Lowering degree mode
 *
 * @property maxDegree 允许的最大表达式次数 / maximum allowed expression degree
*/
enum class OnnxDegreeMode(val maxDegree: Int) {
    /** 仅允许线性（常数与一次）/ linear only (constant and affine) */
    Linear(1),

    /** 允许二次 / quadratic allowed */
    Quadratic(2);

    override fun toString(): String {
        return when (this) {
            Linear -> "Linear"
            Quadratic -> "Quadratic"
        }
    }
}

/**
 * 一个 ONNX 张量的降阶表示 / Lowered representation of one ONNX tensor
 *
 * @property shape 静态形状 / static shape
 * @property elements 行主序元素表达式 / row-major element expressions
*/
data class OnnxTensorExpr(
    val shape: List<Int>,
    val elements: List<OnnxExpr>
) {
    /** 元素个数 / Element count */
    val elementCount: Int
        get() = shape.fold(1) { acc, dim -> acc * dim }

    init {
        require(shape.all { it > 0 }) { "张量形状必须全为静态正数 / tensor shape must be entirely static and positive" }
        require(elements.size == shape.fold(1) { acc, dim -> acc * dim }) {
            "元素数量与形状不一致 / element count does not match shape"
        }
    }

    /** 是否标量（rank 0）/ Whether this is a scalar (rank 0) */
    val isScalar: Boolean
        get() = shape.isEmpty()

    /** 是否全为常数 / Whether every element is a constant */
    val isConstant: Boolean
        get() = elements.all { it is OnnxExpr.Constant }

    /**
     * 按行主序读取第 [index] 个元素 / Read element [index] in row-major order
     *
     * @param index 展平索引 / flattened index
     * @return 元素表达式 / element expression
     */
    operator fun get(index: Int): OnnxExpr {
        return elements[index]
    }

    /**
     * 构造同形状的新张量 / Build a new tensor with the same shape
     *
     * @param newElements 新元素 / new elements
     * @return 新张量 / new tensor
     */
    fun withElements(newElements: List<OnnxExpr>): OnnxTensorExpr {
        return OnnxTensorExpr(shape, newElements)
    }
}

/**
 * 行主序索引工具 / Row-major index helpers
*/
internal object OnnxShape {

    /**
     * 计算行主序步长 / Compute row-major strides
     *
     * @param shape 形状 / shape
     * @return 每个维度对应的步长 / stride of each dimension
     */
    fun strides(shape: List<Int>): List<Int> {
        val strides = MutableList(shape.size) { 1 }
        for (index in shape.indices.reversed()) {
            strides[index] = if (index == shape.lastIndex) 1 else strides[index + 1] * shape[index + 1]
        }
        return strides
    }

    /**
     * 把展平索引展开为多维坐标 / Expand a flattened index into multi-dimensional coordinates
     *
     * @param index 展平索引 / flattened index
     * @param strides 步长 / strides
     * @param rank 维数 / rank
     * @return 坐标 / coordinates
     */
    fun coordinates(index: Int, strides: List<Int>, rank: Int): IntArray {
        val coordinates = IntArray(rank)
        var remaining = index
        for (dimension in 0 until rank) {
            val stride = strides[dimension]
            coordinates[dimension] = remaining / stride
            remaining %= stride
        }
        return coordinates
    }

    /**
     * 把多维坐标折算为展平索引 / Fold multi-dimensional coordinates into a flattened index
     *
     * @param coordinates 坐标 / coordinates
     * @param strides 步长 / strides
     * @return 展平索引 / flattened index
     */
    fun flatten(coordinates: IntArray, strides: List<Int>): Int {
        var index = 0
        for (dimension in coordinates.indices) {
            index += coordinates[dimension] * strides[dimension]
        }
        return index
    }

    /**
     * 计算多向广播后的形状 / Compute the broadcast result shape
     *
     * @param left 左操作数形状 / left operand shape
     * @param right 右操作数形状 / right operand shape
     * @param nodeName 节点名 / node name
     * @return 广播形状 / broadcast shape
     */
    fun broadcastShape(
        left: List<Int>,
        right: List<Int>,
        nodeName: String
    ): OnnxShapeResult<List<Int>> {
        val rank = maxOf(left.size, right.size)
        val result = MutableList(rank) { 1 }
        for (position in 0 until rank) {
            val leftDim = left.getOrNull(left.size - rank + position) ?: 1
            val rightDim = right.getOrNull(right.size - rank + position) ?: 1
            if (leftDim != rightDim && leftDim != 1 && rightDim != 1) {
                return OnnxShapeResult.Failure(
                    OnnxErrorFactory.shapeMismatch(
                        detail = "广播维度不兼容：$left 与 $right / incompatible broadcast dimensions: $left vs $right",
                        nodeName = nodeName,
                        shape = listOf(left.size.toLong(), right.size.toLong())
                    )
                )
            }
            result[position] = maxOf(leftDim, rightDim)
        }
        return OnnxShapeResult.Success(result)
    }

    /**
     * 把张量按目标形状广播 / Broadcast a tensor into the target shape
     *
     * @param tensor 源张量 / source tensor
     * @param target 目标形状 / target shape
     * @param nodeName 节点名 / node name
     * @return 广播结果 / broadcast result
     */
    fun broadcast(
        tensor: OnnxTensorExpr,
        target: List<Int>,
        nodeName: String
    ): OnnxShapeResult<OnnxTensorExpr> {
        if (tensor.shape == target) {
            return OnnxShapeResult.Success(tensor)
        }
        val sourceRank = tensor.shape.size
        val targetRank = target.size
        if (sourceRank > targetRank) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "源张量秩 $sourceRank 大于目标秩 $targetRank / source rank $sourceRank exceeds target rank $targetRank",
                    nodeName = nodeName
                )
            )
        }
        for (position in 0 until sourceRank) {
            val sourceDim = tensor.shape[sourceRank - 1 - position]
            val targetDim = target[targetRank - 1 - position]
            if (sourceDim != targetDim && sourceDim != 1) {
                return OnnxShapeResult.Failure(
                    OnnxErrorFactory.shapeMismatch(
                        detail = "维度 $sourceDim 无法广播到 $targetDim / dimension $sourceDim cannot broadcast to $targetDim",
                        nodeName = nodeName
                    )
                )
            }
        }

        val targetStrides = strides(target)
        val sourceStrides = strides(tensor.shape)
        val offset = targetRank - sourceRank
        val elements = ArrayList<OnnxExpr>(target.fold(1) { acc, dim -> acc * dim })
        for (index in 0 until target.fold(1) { acc, dim -> acc * dim }) {
            val coordinates = coordinates(index, targetStrides, targetRank)
            val sourceCoordinates = IntArray(sourceRank)
            for (dimension in 0 until sourceRank) {
                val sourceDim = tensor.shape[dimension]
                sourceCoordinates[dimension] = if (sourceDim == 1) 0 else coordinates[offset + dimension]
            }
            elements.add(tensor[flatten(sourceCoordinates, sourceStrides)])
        }
        return OnnxShapeResult.Success(OnnxTensorExpr(target, elements))
    }
}

/**
 * 形状运算结果 / Shape operation result
*/
internal sealed interface OnnxShapeResult<out T> {

    /** 成功 / Success */
    data class Success<T>(val value: T) : OnnxShapeResult<T>

    /** 失败 / Failure */
    data class Failure(
        val error: fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
    ) : OnnxShapeResult<Nothing>
}

/**
 * 把常数张量转为张量表达式 / Convert a constant tensor into a tensor expression
 *
 * @param constant 常量张量 / constant tensor
 * @return 张量表达式 / tensor expression
 */
internal fun constantTensorExpr(
    constant: fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxTensorConstant
): OnnxTensorExpr {
    return OnnxTensorExpr(
        shape = constant.shape,
        elements = (0 until constant.elementCount).map {
            OnnxExpr.Constant(Flt64(constant.doubleAt(it)))
        }
    )
}
