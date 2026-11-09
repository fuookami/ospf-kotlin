/**
 * ONNX 张量算子降阶 / ONNX tensor operator lowering
*/
package fuookami.ospf.kotlin.core.plugin.onnx.lowering

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxAttributes
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxNode

/**
 * 逐元素二元算子降阶 / Lower an element-wise binary operator
 *
 * @param node 节点 / node
 * @param lhs 左操作数 / left operand
 * @param rhs 右操作数 / right operand
 * @param mode 次数模式 / degree mode
 * @param op 标量运算 / scalar operation
 * @return 降阶结果 / lowering result
 */
internal fun lowerElementwiseBinary(
    node: OnnxNode,
    lhs: OnnxTensorExpr,
    rhs: OnnxTensorExpr,
    mode: OnnxDegreeMode,
    op: (OnnxExpr, OnnxExpr, OnnxDegreeMode, String) -> OnnxExprResult
): OnnxShapeResult<OnnxTensorExpr> {
    val shape = when (val result = OnnxShape.broadcastShape(lhs.shape, rhs.shape, node.name)) {
        is OnnxShapeResult.Success -> result.value
        is OnnxShapeResult.Failure -> return result
    }
    val left = when (val result = OnnxShape.broadcast(lhs, shape, node.name)) {
        is OnnxShapeResult.Success -> result.value
        is OnnxShapeResult.Failure -> return result
    }
    val right = when (val result = OnnxShape.broadcast(rhs, shape, node.name)) {
        is OnnxShapeResult.Success -> result.value
        is OnnxShapeResult.Failure -> return result
    }
    val elements = ArrayList<OnnxExpr>(shape.fold(1) { acc, dim -> acc * dim })
    for (index in left.elements.indices) {
        when (val result = op(left[index], right[index], mode, node.name)) {
            is OnnxExprResult.Success -> elements.add(result.expr)
            is OnnxExprResult.Failure -> return OnnxShapeResult.Failure(result.error)
        }
    }
    return OnnxShapeResult.Success(OnnxTensorExpr(shape, elements))
}

/**
 * 降阶 `MatMul` / Lower `MatMul`
 *
 * 支持 ONNX 的完整 MatMul 形状规则：秩 1 操作数按向量处理，秩 >2 的批量维按 numpy 规则广播。
 * 批量维只影响索引，不改变每个元素的多项式次数。 / Supports the full ONNX MatMul shape rules: rank-1 operands are treated as vectors and rank >2 batch
 * dimensions broadcast under numpy rules. Batch dimensions only affect indexing; they do not change
 * the polynomial degree of any element.
 *
 * @param node 节点 / node
 * @param lhs 左操作数 / left operand
 * @param rhs 右操作数 / right operand
 * @param mode 次数模式 / degree mode
 * @return 降阶结果 / lowering result
 */
internal fun lowerMatMul(
    node: OnnxNode,
    lhs: OnnxTensorExpr,
    rhs: OnnxTensorExpr,
    mode: OnnxDegreeMode
): OnnxShapeResult<OnnxTensorExpr> {
    val lhsWasVector = lhs.shape.size == 1
    val rhsWasVector = rhs.shape.size == 1

    // 秩 1 操作数按 (1, K) / (K, 1) 参与计算，最后再把该维压掉。
    // Rank-1 operands participate as (1, K) / (K, 1); the added dimension is squeezed out at the end.
    val left = if (lhsWasVector) {
        OnnxTensorExpr(listOf(1, lhs.shape[0]), lhs.elements)
    } else {
        lhs
    }
    val right = if (rhsWasVector) {
        OnnxTensorExpr(listOf(rhs.shape[0], 1), rhs.elements)
    } else {
        rhs
    }
    if (left.shape.size < 2 || right.shape.size < 2) {
        return OnnxShapeResult.Failure(
            OnnxErrorFactory.shapeMismatch(
                detail = "MatMul 操作数秩不得为 0，实际为 ${lhs.shape} 与 ${rhs.shape} / " +
                    "MatMul operands must not be rank 0, got ${lhs.shape} and ${rhs.shape}",
                nodeName = node.name
            )
        )
    }

    val rows = left.shape[left.shape.size - 2]
    val inner = left.shape[left.shape.size - 1]
    val rightInner = right.shape[right.shape.size - 2]
    val columns = right.shape[right.shape.size - 1]
    if (inner != rightInner) {
        return OnnxShapeResult.Failure(
            OnnxErrorFactory.shapeMismatch(
                detail = "MatMul 内维不一致：$inner 与 $rightInner / MatMul inner dimensions differ: $inner vs $rightInner",
                nodeName = node.name
            )
        )
    }

    // 批量维广播：两侧除末两维之外的前缀。 / Batch broadcast: the prefixes before the last two dimensions.
    val leftBatch = left.shape.dropLast(2)
    val rightBatch = right.shape.dropLast(2)
    val batchShape = when (val result = OnnxShape.broadcastShape(leftBatch, rightBatch, node.name)) {
        is OnnxShapeResult.Success -> result.value
        is OnnxShapeResult.Failure -> return result
    }
    val leftBroadcast = when (
        val result = OnnxShape.broadcast(left, batchShape + listOf(rows, inner), node.name)
    ) {
        is OnnxShapeResult.Success -> result.value
        is OnnxShapeResult.Failure -> return result
    }
    val rightBroadcast = when (
        val result = OnnxShape.broadcast(right, batchShape + listOf(rightInner, columns), node.name)
    ) {
        is OnnxShapeResult.Success -> result.value
        is OnnxShapeResult.Failure -> return result
    }

    val batchCount = batchShape.fold(1) { acc, dim -> acc * dim }
    val leftBatchSize = rows * inner
    val rightBatchSize = rightInner * columns
    val elements = ArrayList<OnnxExpr>(batchCount * rows * columns)
    for (batch in 0 until batchCount) {
        val leftOffset = batch * leftBatchSize
        val rightOffset = batch * rightBatchSize
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                var accumulator: OnnxExpr? = null
                for (position in 0 until inner) {
                    val product = when (
                        val result = OnnxExprAlgebra.mul(
                            lhs = leftBroadcast[leftOffset + row * inner + position],
                            rhs = rightBroadcast[rightOffset + position * columns + column],
                            mode = mode,
                            nodeName = node.name
                        )
                    ) {
                        is OnnxExprResult.Success -> result.expr
                        is OnnxExprResult.Failure -> return OnnxShapeResult.Failure(result.error)
                    }
                    accumulator = if (accumulator == null) {
                        product
                    } else {
                        when (val sum = OnnxExprAlgebra.add(accumulator, product, mode, node.name)) {
                            is OnnxExprResult.Success -> sum.expr
                            is OnnxExprResult.Failure -> return OnnxShapeResult.Failure(sum.error)
                        }
                    }
                }
                elements.add(accumulator ?: OnnxExpr.Constant(Flt64.zero))
            }
        }
    }

    // 按 ONNX 规则压掉由秩 1 操作数引入的维度。 / Squeeze the dimensions introduced by rank-1 operands, per ONNX rules.
    val finalShape = batchShape +
        (if (lhsWasVector) emptyList() else listOf(rows)) +
        (if (rhsWasVector) emptyList() else listOf(columns))
    return OnnxShapeResult.Success(OnnxTensorExpr(finalShape, elements))
}

/**
 * 降阶 `Gemm` / Lower `Gemm`
 *
 * @param node 节点 / node
 * @param lhs 左操作数 A / left operand A
 * @param rhs 右操作数 B / right operand B
 * @param bias 可选偏置 C / optional bias C
 * @param attributes 节点属性 / node attributes
 * @param mode 次数模式 / degree mode
 * @return 降阶结果 / lowering result
 */
internal fun lowerGemm(
    node: OnnxNode,
    lhs: OnnxTensorExpr,
    rhs: OnnxTensorExpr,
    bias: OnnxTensorExpr?,
    attributes: OnnxAttributes,
    mode: OnnxDegreeMode
): OnnxShapeResult<OnnxTensorExpr> {
    if (lhs.shape.size != 2 || rhs.shape.size != 2) {
        return OnnxShapeResult.Failure(
            OnnxErrorFactory.unsupportedOperator(
                opType = "Gemm",
                detail = "Gemm 只支持二维输入，实际为 ${lhs.shape} 与 ${rhs.shape} / " +
                    "Gemm supports 2-D inputs only, got ${lhs.shape} and ${rhs.shape}",
                nodeName = node.name
            )
        )
    }
    val transA = attributes.int("transA", 0L) != 0L
    val transB = attributes.int("transB", 0L) != 0L
    val alpha = attributes.float("alpha", 1.0)
    val beta = attributes.float("beta", 1.0)

    val left = if (transA) transpose2d(lhs) else lhs
    val right = if (transB) transpose2d(rhs) else rhs

    val product = when (val result = lowerMatMul(node, left, right, mode)) {
        is OnnxShapeResult.Success -> result.value
        is OnnxShapeResult.Failure -> return result
    }
    val scaled = when (val result = scaleTensor(node, product, Flt64(alpha), mode)) {
        is OnnxShapeResult.Success -> result.value
        is OnnxShapeResult.Failure -> return result
    }
    if (bias == null) {
        return OnnxShapeResult.Success(scaled)
    }
    val scaledBias = when (val result = scaleTensor(node, bias, Flt64(beta), mode)) {
        is OnnxShapeResult.Success -> result.value
        is OnnxShapeResult.Failure -> return result
    }
    return lowerElementwiseBinary(node, scaled, scaledBias, mode, OnnxExprAlgebra::add)
}

/** 二维转置 / Transpose a 2-D tensor */
internal fun transpose2d(tensor: OnnxTensorExpr): OnnxTensorExpr {
    val rows = tensor.shape[0]
    val columns = tensor.shape[1]
    val elements = ArrayList<OnnxExpr>(rows * columns)
    for (column in 0 until columns) {
        for (row in 0 until rows) {
            elements.add(tensor[row * columns + column])
        }
    }
    return OnnxTensorExpr(listOf(columns, rows), elements)
}

/** 张量常数缩放 / Scale a tensor by a constant */
internal fun scaleTensor(
    node: OnnxNode,
    tensor: OnnxTensorExpr,
    factor: Flt64,
    mode: OnnxDegreeMode
): OnnxShapeResult<OnnxTensorExpr> {
    val elements = ArrayList<OnnxExpr>(tensor.elements.size)
    for (element in tensor.elements) {
        when (val result = OnnxExprAlgebra.mul(element, OnnxExpr.Constant(factor), mode, node.name)) {
            is OnnxExprResult.Success -> elements.add(result.expr)
            is OnnxExprResult.Failure -> return OnnxShapeResult.Failure(result.error)
        }
    }
    return OnnxShapeResult.Success(OnnxTensorExpr(tensor.shape, elements))
}
