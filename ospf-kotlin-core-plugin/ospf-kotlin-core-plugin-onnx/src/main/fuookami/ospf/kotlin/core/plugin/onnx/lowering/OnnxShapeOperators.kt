/**
 * ONNX 形状与索引算子降阶 / ONNX shape and index operator lowering
*/
package fuookami.ospf.kotlin.core.plugin.onnx.lowering

import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxNode

/**
 * 解析常量整数张量为列表 / Decode a constant integer tensor into a list
 *
 * @param tensor 常量张量 / constant tensor
 * @return 整数列表 / integer list
 */
internal fun constantInts(tensor: OnnxTensorExpr?): List<Long>? {
    val source = tensor ?: return null
    if (!source.isConstant) {
        return null
    }
    return source.elements.map { element -> (element as OnnxExpr.Constant).value.toDouble().toLong() }
}

/**
 * 降阶 `Reshape` / Lower `Reshape`
 *
 * @param node 节点 / node
 * @param input 输入张量 / input tensor
 * @param shape 常量形状张量 / constant shape tensor
 * @return 降阶结果 / lowering result
 */
internal fun lowerReshape(
    node: OnnxNode,
    input: OnnxTensorExpr,
    shape: OnnxTensorExpr?
): OnnxShapeResult<OnnxTensorExpr> {
    val requested = constantInts(shape)
        ?: return OnnxShapeResult.Failure(
            OnnxErrorFactory.unsupportedOperator(
                opType = "Reshape",
                detail = "shape 输入必须是静态常量 / the shape input must be a static constant",
                nodeName = node.name
            )
        )
    val total = input.elementCount
    var inferredIndex = -1
    val resolved = MutableList<Int>(requested.size) { 0 }
    var known = 1L
    for ((position, dimension) in requested.withIndex()) {
        when {
            dimension == -1L -> {
                if (inferredIndex >= 0) {
                    return OnnxShapeResult.Failure(
                        OnnxErrorFactory.unsupportedAttribute(
                            attributeName = "shape",
                            detail = "shape 中出现多个 -1 / the shape contains multiple -1 entries",
                            nodeName = node.name
                        )
                    )
                }
                inferredIndex = position
            }

            dimension == 0L -> {
                if (position >= input.shape.size) {
                    return OnnxShapeResult.Failure(
                        OnnxErrorFactory.unsupportedAttribute(
                            attributeName = "shape",
                            detail = "位置 $position 的 0 无对应输入维度 / 0 at position $position has no matching input dimension",
                            nodeName = node.name
                        )
                    )
                }
                resolved[position] = input.shape[position]
                known *= resolved[position]
            }

            dimension > 0L -> {
                resolved[position] = dimension.toInt()
                known *= resolved[position]
            }

            else -> {
                return OnnxShapeResult.Failure(
                    OnnxErrorFactory.unsupportedAttribute(
                        attributeName = "shape",
                        detail = "非法维度 $dimension / illegal dimension $dimension",
                        nodeName = node.name
                    )
                )
            }
        }
    }
    if (inferredIndex >= 0) {
        if (known == 0L || total % known != 0L) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "无法从元素数 $total 推断 -1 维度 / cannot infer the -1 dimension from $total elements",
                    nodeName = node.name
                )
            )
        }
        resolved[inferredIndex] = (total / known).toInt()
    }
    val product = resolved.fold(1L) { acc, dim -> acc * dim }
    if (product != total.toLong()) {
        return OnnxShapeResult.Failure(
            OnnxErrorFactory.shapeMismatch(
                detail = "目标形状 $resolved 的元素数 $product 与源元素数 $total 不符 / " +
                    "target shape $resolved holds $product elements but the source holds $total",
                nodeName = node.name
            )
        )
    }
    if (resolved.any { it <= 0 }) {
        return OnnxShapeResult.Failure(
            OnnxErrorFactory.dynamicShape(
                detail = "Reshape 目标形状含非正维度 $resolved / Reshape target shape contains a non-positive dimension $resolved",
                tensorName = node.outputs.firstOrNull()
            )
        )
    }
    return OnnxShapeResult.Success(OnnxTensorExpr(resolved, input.elements))
}

/**
 * 降阶 `Flatten` / Lower `Flatten`
 *
 * @param node 节点 / node
 * @param input 输入张量 / input tensor
 * @param axis 展平轴 / flatten axis
 * @return 降阶结果 / lowering result
 */
internal fun lowerFlatten(
    node: OnnxNode,
    input: OnnxTensorExpr,
    axis: Long
): OnnxShapeResult<OnnxTensorExpr> {
    val rank = input.shape.size
    val normalized = normalizeAxis(axis, rank + 1)
        ?: return OnnxShapeResult.Failure(
            OnnxErrorFactory.shapeMismatch(
                detail = "Flatten 轴 $axis 超出秩 $rank 范围 / Flatten axis $axis is out of range for rank $rank",
                nodeName = node.name
            )
        )
    val outer = input.shape.take(normalized).fold(1) { acc, dim -> acc * dim }
    val inner = input.shape.drop(normalized).fold(1) { acc, dim -> acc * dim }
    return OnnxShapeResult.Success(OnnxTensorExpr(listOf(outer, inner), input.elements))
}

/**
 * 归一化轴索引 / Normalize an axis index
 *
 * @param axis 轴 / axis
 * @param rank 秩 / rank
 * @return 归一化后的轴，越界返回 null / normalized axis, or null when out of range
 */
internal fun normalizeAxis(axis: Long, rank: Int): Int? {
    val normalized = if (axis < 0) axis + rank else axis
    if (normalized < 0 || normalized >= rank) {
        return null
    }
    return normalized.toInt()
}

/**
 * 降阶 `Transpose` / Lower `Transpose`
 *
 * @param node 节点 / node
 * @param input 输入张量 / input tensor
 * @param perm 置换 / permutation
 * @return 降阶结果 / lowering result
 */
internal fun lowerTranspose(
    node: OnnxNode,
    input: OnnxTensorExpr,
    perm: List<Long>
): OnnxShapeResult<OnnxTensorExpr> {
    val rank = input.shape.size
    val normalized = if (perm.isEmpty()) {
        (rank - 1 downTo 0).toList()
    } else {
        val converted = perm.map { normalizeAxis(it, rank) }
        if (converted.any { it == null }) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "Transpose 置换 $perm 超出秩 $rank 范围 / Transpose permutation $perm is out of range for rank $rank",
                    nodeName = node.name
                )
            )
        }
        converted.map { it!! }
    }
    if (normalized.size != rank || normalized.toSet().size != rank) {
        return OnnxShapeResult.Failure(
            OnnxErrorFactory.unsupportedAttribute(
                attributeName = "perm",
                detail = "Transpose 置换 $perm 不是 $rank 阶排列 / Transpose permutation $perm is not a permutation of rank $rank",
                nodeName = node.name
            )
        )
    }
    val targetShape = normalized.map { input.shape[it] }
    val sourceStrides = OnnxShape.strides(input.shape)
    val targetStrides = OnnxShape.strides(targetShape)
    val total = input.elementCount
    val elements = ArrayList<OnnxExpr>(total)
    for (index in 0 until total) {
        val targetCoordinates = OnnxShape.coordinates(index, targetStrides, rank)
        val sourceCoordinates = IntArray(rank)
        for (dimension in 0 until rank) {
            sourceCoordinates[normalized[dimension]] = targetCoordinates[dimension]
        }
        elements.add(input[OnnxShape.flatten(sourceCoordinates, sourceStrides)])
    }
    return OnnxShapeResult.Success(OnnxTensorExpr(targetShape, elements))
}

/**
 * 降阶 `Concat` / Lower `Concat`
 *
 * @param node 节点 / node
 * @param inputs 输入张量 / input tensors
 * @param axis 拼接轴 / concatenation axis
 * @return 降阶结果 / lowering result
 */
internal fun lowerConcat(
    node: OnnxNode,
    inputs: List<OnnxTensorExpr>,
    axis: Long
): OnnxShapeResult<OnnxTensorExpr> {
    if (inputs.isEmpty()) {
        return OnnxShapeResult.Failure(
            OnnxErrorFactory.shapeMismatch(
                detail = "Concat 至少需要一个输入 / Concat requires at least one input",
                nodeName = node.name
            )
        )
    }
    val rank = inputs.first().shape.size
    val normalized = normalizeAxis(axis, rank)
        ?: return OnnxShapeResult.Failure(
            OnnxErrorFactory.shapeMismatch(
                detail = "Concat 轴 $axis 超出秩 $rank 范围 / Concat axis $axis is out of range for rank $rank",
                nodeName = node.name
            )
        )
    for (input in inputs) {
        if (input.shape.size != rank) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "Concat 输入秩不一致：$rank 与 ${input.shape.size} / Concat inputs have different ranks: $rank vs ${input.shape.size}",
                    nodeName = node.name
                )
            )
        }
        for (dimension in 0 until rank) {
            if (dimension != normalized && input.shape[dimension] != inputs.first().shape[dimension]) {
                return OnnxShapeResult.Failure(
                    OnnxErrorFactory.shapeMismatch(
                        detail = "Concat 在维度 $dimension 上形状不一致 / Concat shapes differ on dimension $dimension",
                        nodeName = node.name
                    )
                )
            }
        }
    }

    val targetShape = inputs.first().shape.toMutableList()
    targetShape[normalized] = inputs.sumOf { it.shape[normalized] }
    val targetStrides = OnnxShape.strides(targetShape)
    val outer = targetShape.take(normalized).fold(1) { acc, dim -> acc * dim }
    val inner = targetShape.drop(normalized + 1).fold(1) { acc, dim -> acc * dim }
    val elements = ArrayList<OnnxExpr>(targetShape.fold(1) { acc, dim -> acc * dim })
    for (outerIndex in 0 until outer) {
        for (input in inputs) {
            val sliceSize = input.shape[normalized] * inner
            val start = outerIndex * sliceSize
            for (offset in 0 until sliceSize) {
                elements.add(input[start + offset])
            }
        }
    }
    return OnnxShapeResult.Success(OnnxTensorExpr(targetShape, elements))
}

/**
 * 降阶 `Split` / Lower `Split`
 *
 * @param node 节点 / node
 * @param input 输入张量 / input tensor
 * @param axis 切分轴 / split axis
 * @param split 各段长度，空时按输出数量均分 / segment sizes, evenly split when empty
 * @param outputCount 输出数量 / output count
 * @return 降阶结果 / lowering result
 */
internal fun lowerSplit(
    node: OnnxNode,
    input: OnnxTensorExpr,
    axis: Long,
    split: List<Long>,
    outputCount: Int
): OnnxShapeResult<List<OnnxTensorExpr>> {
    val rank = input.shape.size
    val normalized = normalizeAxis(axis, rank)
        ?: return OnnxShapeResult.Failure(
            OnnxErrorFactory.shapeMismatch(
                detail = "Split 轴 $axis 超出秩 $rank 范围 / Split axis $axis is out of range for rank $rank",
                nodeName = node.name
            )
        )
    if (outputCount <= 0) {
        return OnnxShapeResult.Failure(
            OnnxErrorFactory.shapeMismatch(
                detail = "Split 需要至少一个输出 / Split requires at least one output",
                nodeName = node.name
            )
        )
    }
    val axisSize = input.shape[normalized]
    val sizes = if (split.isEmpty()) {
        if (axisSize % outputCount != 0) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "轴 $normalized 的长度 $axisSize 无法均分为 $outputCount 段 / " +
                        "axis $normalized of length $axisSize cannot be split evenly into $outputCount parts",
                    nodeName = node.name
                )
            )
        }
        List(outputCount) { axisSize / outputCount }
    } else {
        if (split.size != outputCount) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "split 长度 ${split.size} 与输出数量 $outputCount 不符 / " +
                        "split length ${split.size} does not match output count $outputCount",
                    nodeName = node.name
                )
            )
        }
        if (split.any { it <= 0L }) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.unsupportedAttribute(
                    attributeName = "split",
                    detail = "split 含非正段长 $split / split contains a non-positive segment size $split",
                    nodeName = node.name
                )
            )
        }
        split.map { it.toInt() }
    }
    if (sizes.sum() != axisSize) {
        return OnnxShapeResult.Failure(
            OnnxErrorFactory.shapeMismatch(
                detail = "split 之和 ${sizes.sum()} 与轴长度 $axisSize 不符 / split sum ${sizes.sum()} does not match axis size $axisSize",
                nodeName = node.name
            )
        )
    }

    val outer = input.shape.take(normalized).fold(1) { acc, dim -> acc * dim }
    val inner = input.shape.drop(normalized + 1).fold(1) { acc, dim -> acc * dim }
    val buffers = sizes.map { size -> ArrayList<OnnxExpr>(outer * size * inner) }
    var cursor = 0
    for (outerIndex in 0 until outer) {
        for ((outputIndex, size) in sizes.withIndex()) {
            val sliceSize = size * inner
            for (offset in 0 until sliceSize) {
                buffers[outputIndex].add(input[cursor + offset])
            }
            cursor += sliceSize
        }
    }
    val outputs = sizes.mapIndexed { index, size ->
        val shape = input.shape.toMutableList()
        shape[normalized] = size
        OnnxTensorExpr(shape, buffers[index])
    }
    return OnnxShapeResult.Success(outputs)
}

/**
 * 降阶 `Gather` / Lower `Gather`
 *
 * @param node 节点 / node
 * @param data 数据张量 / data tensor
 * @param indices 常量索引张量 / constant index tensor
 * @param axis 采集轴 / gather axis
 * @return 降阶结果 / lowering result
 */
internal fun lowerGather(
    node: OnnxNode,
    data: OnnxTensorExpr,
    indices: OnnxTensorExpr?,
    axis: Long
): OnnxShapeResult<OnnxTensorExpr> {
    val indexList = constantInts(indices)
        ?: return OnnxShapeResult.Failure(
            OnnxErrorFactory.unsupportedOperator(
                opType = "Gather",
                detail = "indices 必须是静态常量 / indices must be a static constant",
                nodeName = node.name
            )
        )
    val rank = data.shape.size
    val normalized = normalizeAxis(axis, rank)
        ?: return OnnxShapeResult.Failure(
            OnnxErrorFactory.shapeMismatch(
                detail = "Gather 轴 $axis 超出秩 $rank 范围 / Gather axis $axis is out of range for rank $rank",
                nodeName = node.name
            )
        )
    val axisSize = data.shape[normalized]
    val resolved = MutableList(indexList.size) { 0 }
    for ((position, raw) in indexList.withIndex()) {
        val index = if (raw < 0) raw + axisSize else raw
        if (index < 0 || index >= axisSize) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "Gather 索引 $raw 超出轴长度 $axisSize / Gather index $raw is out of range for axis size $axisSize",
                    nodeName = node.name
                )
            )
        }
        resolved[position] = index.toInt()
    }
    val outer = data.shape.take(normalized).fold(1) { acc, dim -> acc * dim }
    val inner = data.shape.drop(normalized + 1).fold(1) { acc, dim -> acc * dim }
    val targetShape = data.shape.take(normalized) + indices!!.shape + data.shape.drop(normalized + 1)
    val elements = ArrayList<OnnxExpr>(outer * resolved.size * inner)
    for (outerIndex in 0 until outer) {
        for (index in resolved) {
            val start = outerIndex * axisSize * inner + index * inner
            for (offset in 0 until inner) {
                elements.add(data[start + offset])
            }
        }
    }
    return OnnxShapeResult.Success(OnnxTensorExpr(targetShape, elements))
}

/**
 * 降阶 `ReduceSum` / Lower `ReduceSum`
 *
 * @param node 节点 / node
 * @param input 输入张量 / input tensor
 * @param axes 归约轴 / reduction axes
 * @param keepDims 是否保留维度 / whether dimensions are kept
 * @param mode 次数模式 / degree mode
 * @return 降阶结果 / lowering result
 */
internal fun lowerReduceSum(
    node: OnnxNode,
    input: OnnxTensorExpr,
    axes: List<Long>,
    keepDims: Boolean,
    mode: OnnxDegreeMode
): OnnxShapeResult<OnnxTensorExpr> {
    val rank = input.shape.size
    val normalized = if (axes.isEmpty()) {
        (0 until rank).toList()
    } else {
        val converted = axes.map { normalizeAxis(it, rank) }
        if (converted.any { it == null }) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "ReduceSum 轴 $axes 超出秩 $rank 范围 / ReduceSum axes $axes are out of range for rank $rank",
                    nodeName = node.name
                )
            )
        }
        converted.map { it!! }.distinct()
    }
    val sourceStrides = OnnxShape.strides(input.shape)
    val targetShape = input.shape.filterIndexed { dimension, _ -> dimension !in normalized }
    val total = input.elementCount
    val accumulatorIndices = IntArray(total) { -1 }
    val targetStrides = OnnxShape.strides(targetShape)
    val accumulatorList = ArrayList<OnnxExpr>()
    for (index in 0 until total) {
        val sourceCoordinates = OnnxShape.coordinates(index, sourceStrides, rank)
        val targetCoordinates = sourceCoordinates.filterIndexed { dimension, _ -> dimension !in normalized }
        val targetIndex = OnnxShape.flatten(targetCoordinates.toIntArray(), targetStrides)
        if (accumulatorIndices[targetIndex] == -1) {
            accumulatorIndices[targetIndex] = accumulatorList.size
            accumulatorList.add(input[index])
        } else {
            val position = accumulatorIndices[targetIndex]
            val sum = when (val result = OnnxExprAlgebra.add(accumulatorList[position], input[index], mode, node.name)) {
                is OnnxExprResult.Success -> result.expr
                is OnnxExprResult.Failure -> return OnnxShapeResult.Failure(result.error)
            }
            accumulatorList[position] = sum
        }
    }
    val finalShape = if (keepDims) {
        input.shape.mapIndexed { dimension, size -> if (dimension in normalized) 1 else size }
    } else {
        targetShape
    }
    return OnnxShapeResult.Success(OnnxTensorExpr(finalShape, accumulatorList))
}
