/**
 * ONNX 图降阶引擎 / ONNX graph lowering engine
*/
package fuookami.ospf.kotlin.core.plugin.onnx.lowering

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxNode
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxParsedGraph
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxTensorConstant

/**
 * 输入绑定中的一个标量位置 / One scalar position inside an input binding
 *
 * @property symbol 绑定的 OSPF 符号 / bound OSPF symbol
 * @property variable 原始符号的线性表达式 / linear expression of the original symbol
*/
internal data class OnnxInputElement(val symbol: Symbol, val variable: OnnxExpr.Linear)

/**
 * 降阶上下文 / Lowering context
 *
 * @property mode 次数模式 / degree mode
 * @property symbols 已产生的中间符号（拓扑序）/ produced intermediate symbols in topological order
 * @property approximatedActivations 采用近似表达的激活节点名 / activation nodes expressed approximately
 * @property bindings 输入名到元素序列的映射 / map from input name to element sequence
*/
internal class OnnxLoweringContext(
    val mode: OnnxDegreeMode,
    val bindings: Map<String, List<OnnxInputElement>>,
    val activationSegments: Int = OnnxActivation.DEFAULT_SEGMENTS
) {
    val symbols: MutableList<fuookami.ospf.kotlin.core.symbol.IntermediateSymbol<Flt64>> = mutableListOf()
    val approximatedActivations: MutableList<String> = mutableListOf()

    /**
     * 当前可见的张量环境 / Currently visible tensor environment
     *
     * 子图降阶会临时替换该引用，从而把作用域隔离在外层环境之外。 / Subgraph lowering temporarily replaces this reference, keeping the scope isolated from the enclosing environment.
     */
    var values: MutableMap<String, OnnxTensorExpr> = mutableMapOf()

    /**
     * 在隔离作用域内执行一段降阶 / Run a lowering block inside an isolated scope
     *
     * @param seed 子图初始可见的张量 / tensors initially visible to the subgraph
     * @param block 降阶逻辑 / lowering logic
     * @return 隔离作用域内产生的张量环境 / tensor environment produced inside the isolated scope
     */
    fun <R> scoped(
        seed: Map<String, OnnxTensorExpr>,
        block: (MutableMap<String, OnnxTensorExpr>) -> Ret<R>
    ): Ret<R> {
        val outer = values
        val local = LinkedHashMap<String, OnnxTensorExpr>()
        local.putAll(seed)
        values = local
        try {
            return block(local)
        } finally {
            values = outer
        }
    }
}

/**
 * 降阶结果 / Lowering result
*/
internal sealed interface OnnxLoweringResult {

    /** 成功 / Success */
    data class Success(val context: OnnxLoweringContext) : OnnxLoweringResult

    /** 失败 / Failure */
    data class Failure(
        val error: fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
    ) : OnnxLoweringResult
}

/**
 * 按拓扑序把图降阶为表达式 / Lower a graph into expressions in topological order
 *
 * @param graph 解析后的图 / parsed graph
 * @param context 降阶上下文 / lowering context
 * @return 降阶结果 / lowering result
 */
internal fun lowerGraph(graph: OnnxParsedGraph, context: OnnxLoweringContext): OnnxLoweringResult {
    for ((name, constant) in graph.initializers) {
        context.values[name] = constantTensorExpr(constant)
    }
    for (input in graph.inputs) {
        val elements = context.bindings[input.name]
            ?: return OnnxLoweringResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "图输入 '${input.name}' 缺少符号绑定 / graph input '${input.name}' has no symbol binding",
                    tensorName = input.name
                )
            )
        val shape = input.staticShape
            ?: return OnnxLoweringResult.Failure(
                OnnxErrorFactory.dynamicShape(
                    detail = "图输入 '${input.name}' 含动态维度 ${input.shape} / " +
                        "graph input '${input.name}' has dynamic dimensions ${input.shape}",
                    tensorName = input.name,
                    shape = input.shape
                )
            )
        if (shape.any { it <= 0L }) {
            return OnnxLoweringResult.Failure(
                OnnxErrorFactory.dynamicShape(
                    detail = "图输入 '${input.name}' 含非正维度 $shape / graph input '${input.name}' has non-positive dimensions $shape",
                    tensorName = input.name,
                    shape = input.shape
                )
            )
        }
        val elementCount = shape.fold(1L) { acc, dim -> acc * dim }
        if (elementCount != elements.size.toLong()) {
            return OnnxLoweringResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "图输入 '${input.name}' 元素数 $elementCount 与绑定符号数 ${elements.size} 不符 / " +
                        "graph input '${input.name}' has $elementCount elements but ${elements.size} symbols are bound",
                    tensorName = input.name
                )
            )
        }
        context.values[input.name] = OnnxTensorExpr(
            shape = shape.map { it.toInt() },
            elements = elements.map { it.variable }
        )
    }

    for (node in graph.nodes) {
        when (val result = lowerNode(node, context)) {
            is OnnxShapeResult.Success -> Unit
            is OnnxShapeResult.Failure -> return OnnxLoweringResult.Failure(result.error)
        }
    }
    return OnnxLoweringResult.Success(context)
}

/**
 * 降阶单个节点 / Lower a single node
 *
 * @param node 节点 / node
 * @param context 降阶上下文 / lowering context
 * @return 降阶结果 / lowering result
 */
internal fun lowerNode(node: OnnxNode, context: OnnxLoweringContext): OnnxShapeResult<Unit> {
    fun operand(name: String): OnnxTensorExpr? {
        return context.values[name]
    }

    fun publish(values: List<OnnxTensorExpr>): OnnxShapeResult<Unit> {
        if (values.size != node.outputs.size) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "算子产出 ${values.size} 个张量但声明了 ${node.outputs.size} 个输出 / " +
                        "operator produced ${values.size} tensors but declares ${node.outputs.size} outputs",
                    nodeName = node.name
                )
            )
        }
        for ((position, outputName) in node.outputs.withIndex()) {
            context.values[outputName] = values[position]
        }
        return OnnxShapeResult.Success(Unit)
    }

    val result: OnnxShapeResult<List<OnnxTensorExpr>> = when (node.opType) {
        "Identity" -> {
            val input = operand(node.inputs[0]) ?: return missingInput(node, node.inputs[0])
            OnnxShapeResult.Success(listOf(input))
        }

        "If", "Loop", "Scan" -> {
            when (val lowered = OnnxSubgraph.lower(node, context)) {
                is OnnxTensorResult.Success -> OnnxShapeResult.Success(lowered.tensors)
                is OnnxTensorResult.Failure -> OnnxShapeResult.Failure(lowered.error)
            }
        }

        "Add" -> binary(node, context) { lhs, rhs, mode, name ->
            OnnxExprAlgebra.add(lhs, rhs, mode, name)
        }

        "Sub" -> binary(node, context) { lhs, rhs, mode, name ->
            OnnxExprAlgebra.sub(lhs, rhs, mode, name)
        }

        "Mul" -> binary(node, context) { lhs, rhs, mode, name ->
            OnnxExprAlgebra.mul(lhs, rhs, mode, name)
        }

        "MatMul" -> {
            val lhs = operand(node.inputs[0]) ?: return missingInput(node, node.inputs[0])
            val rhs = operand(node.inputs[1]) ?: return missingInput(node, node.inputs[1])
            lowerMatMul(node, lhs, rhs, context.mode).let { shape ->
                when (shape) {
                    is OnnxShapeResult.Success -> OnnxShapeResult.Success(listOf(shape.value))
                    is OnnxShapeResult.Failure -> shape
                }
            }
        }

        "Gemm" -> {
            val lhs = operand(node.inputs[0]) ?: return missingInput(node, node.inputs[0])
            val rhs = operand(node.inputs[1]) ?: return missingInput(node, node.inputs[1])
            val bias = node.inputs.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let { operand(it) }
            when (val shape = lowerGemm(node, lhs, rhs, bias, node.attributes, context.mode)) {
                is OnnxShapeResult.Success -> OnnxShapeResult.Success(listOf(shape.value))
                is OnnxShapeResult.Failure -> shape
            }
        }

        "Reshape" -> {
            val input = operand(node.inputs[0]) ?: return missingInput(node, node.inputs[0])
            val shape = node.inputs.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { operand(it) }
            when (val lowered = lowerReshape(node, input, shape)) {
                is OnnxShapeResult.Success -> OnnxShapeResult.Success(listOf(lowered.value))
                is OnnxShapeResult.Failure -> lowered
            }
        }

        "Flatten" -> {
            val input = operand(node.inputs[0]) ?: return missingInput(node, node.inputs[0])
            when (val lowered = lowerFlatten(node, input, node.attributes.int("axis", 1L))) {
                is OnnxShapeResult.Success -> OnnxShapeResult.Success(listOf(lowered.value))
                is OnnxShapeResult.Failure -> lowered
            }
        }

        "Transpose" -> {
            val input = operand(node.inputs[0]) ?: return missingInput(node, node.inputs[0])
            // `ints` 对单元素 INTS 属性与缺失属性都返回列表，这里统一按置换处理。
            // `ints` returns a list for both single-element INTS attributes and missing ones, so it is handled uniformly as a permutation.
            when (val lowered = lowerTranspose(node, input, node.attributes.ints("perm"))) {
                is OnnxShapeResult.Success -> OnnxShapeResult.Success(listOf(lowered.value))
                is OnnxShapeResult.Failure -> lowered
            }
        }

        "Concat" -> {
            val inputs = node.inputs.map { name -> operand(name) ?: return missingInput(node, name) }
            when (val lowered = lowerConcat(node, inputs, node.attributes.int("axis", 0L))) {
                is OnnxShapeResult.Success -> OnnxShapeResult.Success(listOf(lowered.value))
                is OnnxShapeResult.Failure -> lowered
            }
        }

        "Split" -> {
            val input = operand(node.inputs[0]) ?: return missingInput(node, node.inputs[0])
            val split = node.inputs.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { name ->
                constantInts(operand(name))
                    ?: return OnnxShapeResult.Failure(
                        OnnxErrorFactory.unsupportedOperator(
                            opType = "Split",
                            detail = "split 输入必须是静态常量 / the split input must be a static constant",
                            nodeName = node.name
                        )
                    )
            } ?: node.attributes.ints("split")
            when (val lowered = lowerSplit(node, input, node.attributes.int("axis", 0L), split, node.outputs.size)) {
                is OnnxShapeResult.Success -> OnnxShapeResult.Success(lowered.value)
                is OnnxShapeResult.Failure -> lowered
            }
        }

        "Gather" -> {
            val data = operand(node.inputs[0]) ?: return missingInput(node, node.inputs[0])
            val indices = node.inputs.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { operand(it) }
            when (val lowered = lowerGather(node, data, indices, node.attributes.int("axis", 0L))) {
                is OnnxShapeResult.Success -> OnnxShapeResult.Success(listOf(lowered.value))
                is OnnxShapeResult.Failure -> lowered
            }
        }

        "ReduceSum" -> {
            val input = operand(node.inputs[0]) ?: return missingInput(node, node.inputs[0])
            val axes = node.inputs.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { name ->
                constantInts(operand(name))
                    ?: return OnnxShapeResult.Failure(
                        OnnxErrorFactory.unsupportedOperator(
                            opType = "ReduceSum",
                            detail = "axes 输入必须是静态常量 / the axes input must be a static constant",
                            nodeName = node.name
                        )
                    )
            } ?: node.attributes.ints("axes")
            val keepDims = node.attributes.int("keepdims", 1L) != 0L
            when (val lowered = lowerReduceSum(node, input, axes, keepDims, context.mode)) {
                is OnnxShapeResult.Success -> OnnxShapeResult.Success(listOf(lowered.value))
                is OnnxShapeResult.Failure -> lowered
            }
        }

        in OnnxActivation.OPERATORS -> {
            val input = operand(node.inputs[0]) ?: return missingInput(node, node.inputs[0])
            when (val lowered = OnnxActivation.lower(node, input, context, context.activationSegments)) {
                is OnnxShapeResult.Success -> OnnxShapeResult.Success(listOf(lowered.value))
                is OnnxShapeResult.Failure -> lowered
            }
        }

        else -> OnnxShapeResult.Failure(
            OnnxErrorFactory.unsupportedOperator(
                opType = node.opType,
                detail = "算子不在首版白名单内 / the operator is not in the first-version whitelist",
                nodeName = node.name
            )
        )
    }

    return when (result) {
        is OnnxShapeResult.Success -> publish(result.value)
        is OnnxShapeResult.Failure -> result
    }
}

/**
 * 逐元素二元算子分派 / Dispatch an element-wise binary operator
 *
 * @param node 节点 / node
 * @param context 降阶上下文 / lowering context
 * @param op 标量运算 / scalar operation
 * @return 降阶结果 / lowering result
 */
private fun binary(
    node: OnnxNode,
    context: OnnxLoweringContext,
    op: (OnnxExpr, OnnxExpr, OnnxDegreeMode, String) -> OnnxExprResult
): OnnxShapeResult<List<OnnxTensorExpr>> {
    val lhs = context.values[node.inputs[0]] ?: return missingInput(node, node.inputs[0])
    val rhs = context.values[node.inputs[1]] ?: return missingInput(node, node.inputs[1])
    return when (val lowered = lowerElementwiseBinary(node, lhs, rhs, context.mode, op)) {
        is OnnxShapeResult.Success -> OnnxShapeResult.Success(listOf(lowered.value))
        is OnnxShapeResult.Failure -> lowered
    }
}

/**
 * 缺失输入的失败结果 / Failure result for a missing operand
 *
 * @param node 节点 / node
 * @param name 输入名 / input name
 * @return 失败结果 / failure result
 */
private fun missingInput(node: OnnxNode, name: String): OnnxShapeResult<Nothing> {
    return OnnxShapeResult.Failure(
        OnnxErrorFactory.shapeMismatch(
            detail = "节点输入 '$name' 未定义 / node input '$name' is not defined",
            nodeName = node.name,
            tensorName = name
        )
    )
}

/**
 * 供测试与模型层使用的常量张量入口 / Constant tensor entry point used by tests and the model layer
 *
 * @param constant 常量张量 / constant tensor
 * @return 张量表达式 / tensor expression
 */
fun onnxConstantTensorExpr(constant: OnnxTensorConstant): OnnxTensorExpr {
    return constantTensorExpr(constant)
}
