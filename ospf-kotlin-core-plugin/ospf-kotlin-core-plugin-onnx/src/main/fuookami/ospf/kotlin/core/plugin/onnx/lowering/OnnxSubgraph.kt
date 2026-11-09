/**
 * ONNX 子图控制流降阶 / ONNX subgraph control-flow lowering
*/
package fuookami.ospf.kotlin.core.plugin.onnx.lowering

import ai.onnx.proto.OnnxMl
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxAttributeValue
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxNode
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxParseResult
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxParsedGraph
import fuookami.ospf.kotlin.core.plugin.onnx.graph.parseOnnxSubgraph

/** 单次循环允许的最大展开次数 / Maximum unroll count for a single loop */
// ponytail: 展开式建模，规模随迭代次数线性增长，256 之后表达式会膨胀到不可解；需要更长循环时改成
// 真正的循环变量 + 递增约束的紧凑形式。
// ponytail: unrolled modelling grows linearly with the trip count and becomes unsolvable past 256; for longer
// loops switch to a compact formulation with a real iteration variable and increment constraints.
internal const val MAX_UNROLL: Int = 256

/**
 * 子图控制流降阶 / Subgraph control-flow lowering
 *
 * 所有分支与循环都必须能在导入期静态确定，因此只支持：
 * - `If`：条件来自常量；
 * - `Loop`：迭代次数与终止条件均为常量；
 * - `Scan`：扫描长度来自常量扫描输入的首维。
 *
 * 这三类都通过展开实现，展开后的建模规模随迭代次数线性增长。 / Every branch and loop must be statically resolvable at import time, hence only:
 * - `If`: the condition is constant;
 * - `Loop`: the trip count and the termination condition are constant;
 * - `Scan`: the scan length is the leading dimension of a constant scan input.
 *
 * All three unroll, so the modelling size grows linearly with the iteration count.
*/
internal object OnnxSubgraph {

    /** 支持的算子名 / Supported operator names */
    val OPERATORS: Set<String> = setOf("If", "Loop", "Scan")

    /**
     * 降阶一个子图控制流节点 / Lower one subgraph control-flow node
     *
     * @param node 节点 / node
     * @param context 降阶上下文 / lowering context
     * @return 降阶结果 / lowering result
     */
    fun lower(node: OnnxNode, context: OnnxLoweringContext): OnnxTensorResult {
        return when (node.opType) {
            "If" -> lowerIf(node, context)
            "Loop" -> lowerLoop(node, context)
            "Scan" -> lowerScan(node, context)
            else -> OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedOperator(
                    opType = node.opType,
                    detail = "子图控制流算子不在白名单内 / the subgraph control-flow operator is not whitelisted",
                    nodeName = node.name
                )
            )
        }
    }

    /** 读取子图属性 / Read a subgraph attribute */
    private fun subgraph(node: OnnxNode, attribute: String): OnnxMl.GraphProto? {
        return (node.attributes.values[attribute] as? OnnxAttributeValue.GraphValue)?.proto
    }

    /**
     * 解析并降阶一个子图 / Parse and lower a subgraph inside an isolated scope
     *
     * @param node 节点 / node
     * @param attribute 子图属性名 / subgraph attribute name
     * @param context 降阶上下文 / lowering context
     * @param seed 子图初始可见张量 / tensors initially visible to the subgraph
     * @param outputs 需要的子图输出名 / requested subgraph output names
     * @return 子图输出张量列表 / subgraph output tensors
     */
    private fun runSubgraph(
        node: OnnxNode,
        attribute: String,
        context: OnnxLoweringContext,
        seed: Map<String, OnnxTensorExpr>,
        outputs: List<String>
    ): OnnxTensorResult {
        val proto = subgraph(node, attribute)
            ?: return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedAttribute(
                    attributeName = attribute,
                    detail = "节点缺少子图属性 '$attribute' / the node has no subgraph attribute '$attribute'",
                    nodeName = node.name
                )
            )
        val parsed = when (val result = parseOnnxSubgraph(proto, 0L)) {
            is OnnxParseResult.Success -> result.graph
            is OnnxParseResult.Failure -> return OnnxTensorResult.Failure(result.error)
        }
        return lowerParsed(node, parsed, context, seed, outputs)
    }

    /**
     * 在隔离作用域内降阶已解析的子图 / Lower an already parsed subgraph inside an isolated scope
     *
     * @param node 节点 / node
     * @param graph 子图 / subgraph
     * @param context 降阶上下文 / lowering context
     * @param seed 初始可见张量 / initially visible tensors
     * @param outputs 需要的输出名 / requested output names
     * @return 子图输出张量列表 / subgraph output tensors
     */
    private fun lowerParsed(
        node: OnnxNode,
        graph: OnnxParsedGraph,
        context: OnnxLoweringContext,
        seed: Map<String, OnnxTensorExpr>,
        outputs: List<String>
    ): OnnxTensorResult {
        val result = context.scoped(seed) { local ->
            // 子图的形式输入由 seed 直接给出，不再从外层绑定表解析。
            // The formal inputs of the subgraph come from the seed, not from the outer binding table.
            for ((name, constant) in graph.initializers) {
                local[name] = constantTensorExpr(constant)
            }
            for (sub in graph.nodes) {
                when (val lowered = lowerNode(sub, context)) {
                    is OnnxShapeResult.Success -> Unit
                    is OnnxShapeResult.Failure -> return@scoped failed(lowered.error.toError())
                }
            }
            val tensors = ArrayList<OnnxTensorExpr>(outputs.size)
            for (name in outputs) {
                val tensor = local[name] ?: local[graph.outputs.firstOrNull { it.name == name }?.name]
                if (tensor == null) {
                    return@scoped failed(
                        OnnxError.OutputSelection(
                            detail = "子图 '${node.name}' 的 '$name' 未被降阶 / subgraph '${node.name}' did not lower '$name'",
                            tensorName = name
                        ).toError()
                    )
                }
                tensors.add(tensor)
            }
            ok(tensors)
        }
        return when (result) {
            is Ok -> OnnxTensorResult.Success(result.value)
            is Failed -> OnnxTensorResult.Failure(
                (result.error.value as? OnnxError) ?: OnnxError.InvalidModel(
                    detail = "子图 '${node.name}' 降阶失败：${result.error.message ?: "unknown"} / " +
                        "subgraph '${node.name}' lowering failed: ${result.error.message ?: "unknown"}"
                )
            )

            is Fatal -> OnnxTensorResult.Failure(
                OnnxError.InvalidModel(
                    detail = "子图 '${node.name}' 降阶失败：${result.errors.first().message ?: "unknown"} / " +
                        "subgraph '${node.name}' lowering failed: ${result.errors.first().message ?: "unknown"}"
                )
            )
        }
    }

    /** 降阶 `If` / Lower `If` */
    private fun lowerIf(node: OnnxNode, context: OnnxLoweringContext): OnnxTensorResult {
        val condition = node.inputs.firstOrNull()
            ?: return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedOperator(
                    opType = "If",
                    detail = "If 缺少条件输入 / If has no condition input",
                    nodeName = node.name
                )
            )
        val values = when (val result = constantValues(condition, context, node)) {
            is OnnxShapeResult.Success -> result.value
            is OnnxShapeResult.Failure -> return OnnxTensorResult.Failure(result.error)
        }
        if (values.size != 1) {
            return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedOperator(
                    opType = "If",
                    detail = "If 条件必须是标量，实际元素数 ${values.size} / If condition must be scalar, got ${values.size} elements",
                    nodeName = node.name
                )
            )
        }
        // 非零即真，与 ONNX 的布尔语义一致。 / Non-zero means true, matching ONNX boolean semantics.
        val taken = if (values[0] != 0.0) "then_branch" else "else_branch"
        val branch = subgraph(node, taken)
            ?: return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedAttribute(
                    attributeName = taken,
                    detail = "If 缺少分支 '$taken' / If has no branch '$taken'",
                    nodeName = node.name
                )
            )
        val parsed = when (val result = parseOnnxSubgraph(branch, 0L)) {
            is OnnxParseResult.Success -> result.graph
            is OnnxParseResult.Failure -> return OnnxTensorResult.Failure(result.error)
        }
        // 分支子图没有形式输入，只捕获外层作用域。 / The branch subgraph has no formal inputs; it only captures the enclosing scope.
        return lowerParsed(node, parsed, context, context.values.toMap(), parsed.outputs.map { it.name })
    }

    /** 降阶 `Loop` / Lower `Loop` */
    private fun lowerLoop(node: OnnxNode, context: OnnxLoweringContext): OnnxTensorResult {
        if (node.inputs.isEmpty()) {
            return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedOperator(
                    opType = "Loop",
                    detail = "Loop 缺少迭代次数输入 / Loop has no trip-count input",
                    nodeName = node.name
                )
            )
        }
        val optionalCount = node.inputs.getOrNull(0).orEmpty()
        if (optionalCount.isEmpty()) {
            return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedOperator(
                    opType = "Loop",
                    detail = "Loop 的迭代次数必须是常量 / the Loop trip count must be constant",
                    nodeName = node.name
                )
            )
        }
        val countValues = when (val result = constantValues(optionalCount, context, node)) {
            is OnnxShapeResult.Success -> result.value
            is OnnxShapeResult.Failure -> return OnnxTensorResult.Failure(result.error)
        }
        val tripCount = countValues.firstOrNull()?.toInt()
            ?: return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedOperator(
                    opType = "Loop",
                    detail = "Loop 的迭代次数无法解析 / the Loop trip count cannot be resolved",
                    nodeName = node.name
                )
            )
        if (tripCount !in 0..MAX_UNROLL) {
            return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedOperator(
                    opType = "Loop",
                    detail = "Loop 迭代次数 $tripCount 超出可展开区间 0..$MAX_UNROLL / " +
                        "Loop trip count $tripCount is outside the unrollable range 0..$MAX_UNROLL",
                    nodeName = node.name
                )
            )
        }
        val body = subgraph(node, "body")
            ?: return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedAttribute(
                    attributeName = "body",
                    detail = "Loop 缺少循环体 / Loop has no body",
                    nodeName = node.name
                )
            )
        val parsed = when (val result = parseOnnxSubgraph(body, 0L)) {
            is OnnxParseResult.Success -> result.graph
            is OnnxParseResult.Failure -> return OnnxTensorResult.Failure(result.error)
        }
        // 携带依赖从第 3 个输入开始，依次向前传递。 / Loop-carried dependencies start at input 3 and are threaded forward.
        val carriedNames = node.inputs.drop(2).filter { it.isNotEmpty() }
        val carried = ArrayList<OnnxTensorExpr>(carriedNames.size)
        for (name in carriedNames) {
            val tensor = context.values[name]
                ?: return OnnxTensorResult.Failure(
                    OnnxErrorFactory.shapeMismatch(
                        detail = "Loop 携带依赖 '$name' 不可得 / Loop-carried dependency '$name' is unavailable",
                        tensorName = name,
                        nodeName = node.name
                    )
                )
            carried.add(tensor)
        }
        val bodyInputNames = parsed.inputs.map { it.name }
        val scanStates = ArrayList<List<OnnxTensorExpr>>()
        var scanning: List<OnnxTensorExpr> = carried
        for (iteration in 0 until tripCount) {
            val seed = LinkedHashMap<String, OnnxTensorExpr>()
            // 循环体形式输入：迭代号、条件、携带依赖。 / Body formal inputs: iteration number, condition, carried dependencies.
            for ((position, inputName) in bodyInputNames.withIndex()) {
                seed[inputName] = when (position) {
                    0 -> scalarConstant(Flt64(iteration.toDouble()))
                    1 -> scalarConstant(Flt64.one)
                    else -> scanning.getOrNull(position - 2)
                        ?: return OnnxTensorResult.Failure(
                            OnnxErrorFactory.shapeMismatch(
                                detail = "循环体缺少第 ${position - 2} 个携带依赖 / the loop body is missing carried dependency ${position - 2}",
                                nodeName = node.name
                            )
                        )
                }
            }
            val bodyOutputs = parsed.outputs.map { it.name }
            val produced = when (
                val result = lowerParsed(node, parsed, context, seed, bodyOutputs)
            ) {
                is OnnxTensorResult.Success -> result.tensors
                is OnnxTensorResult.Failure -> return result
            }
            // 第 0 个输出是下一轮条件，其后是携带依赖，再往后是扫描输出。
            // Output 0 is the next condition, then the carried dependencies, then the scan outputs.
            val condition = produced.firstOrNull()
                ?: return OnnxTensorResult.Failure(
                    OnnxErrorFactory.shapeMismatch(
                        detail = "循环体缺少条件输出 / the loop body produces no condition output",
                        nodeName = node.name
                    )
                )
            if (!condition.isConstant) {
                return OnnxTensorResult.Failure(
                    OnnxErrorFactory.unsupportedOperator(
                        opType = "Loop",
                        detail = "循环终止条件必须为常量，第 $iteration 轮不是 / " +
                            "the loop termination condition must be constant, iteration $iteration is not",
                        nodeName = node.name
                    )
                )
            }
            val next = produced.drop(1).take(carried.size)
            if (next.size != carried.size) {
                return OnnxTensorResult.Failure(
                    OnnxErrorFactory.shapeMismatch(
                        detail = "循环体产出的携带依赖数 ${next.size} 与输入数 ${carried.size} 不符 / " +
                            "the loop body produces ${next.size} carried dependencies but ${carried.size} are given",
                        nodeName = node.name
                    )
                )
            }
            scanning = next
            scanStates.add(produced.drop(1 + carried.size))
        }

        if (tripCount == 0) {
            // 不迭代时携带依赖保持初值，扫描输出为空。 / With zero iterations the carried values keep their initial value and scan outputs are empty.
            val outputs = ArrayList<OnnxTensorExpr>(carried.size)
            outputs.addAll(carried)
            return OnnxTensorResult.Success(outputs)
        }
        val outputs = ArrayList<OnnxTensorExpr>(carried.size + (scanStates.firstOrNull()?.size ?: 0))
        outputs.addAll(scanning)
        val perIteration = scanStates.firstOrNull()?.size ?: 0
        for (index in 0 until perIteration) {
            val slices = scanStates.map { it[index] }
            val concatenated = when (val result = concatScan(node, slices)) {
                is OnnxShapeResult.Success -> result.value
                is OnnxShapeResult.Failure -> return OnnxTensorResult.Failure(result.error)
            }
            outputs.add(concatenated)
        }
        return OnnxTensorResult.Success(outputs)
    }

    /** 降阶 `Scan` / Lower `Scan` */
    private fun lowerScan(node: OnnxNode, context: OnnxLoweringContext): OnnxTensorResult {
        if (node.inputs.isEmpty()) {
            return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedOperator(
                    opType = "Scan",
                    detail = "Scan 缺少输入 / Scan has no input",
                    nodeName = node.name
                )
            )
        }
        val stateCount = node.attributes.int("num_scan_inputs", 0L)
        if (stateCount <= 0L || stateCount > node.inputs.size.toLong()) {
            return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedAttribute(
                    attributeName = "num_scan_inputs",
                    detail = "Scan 的 num_scan_inputs 为 $stateCount，与输入数 ${node.inputs.size} 不匹配 / " +
                        "Scan has num_scan_inputs $stateCount, which does not match ${node.inputs.size} inputs",
                    nodeName = node.name
                )
            )
        }
        val scanInputCount = stateCount.toInt()
        val states = node.inputs.take(node.inputs.size - scanInputCount).filter { it.isNotEmpty() }
        val scanInputs = node.inputs.takeLast(scanInputCount).filter { it.isNotEmpty() }
        if (scanInputs.size != scanInputCount) {
            return OnnxTensorResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "Scan 的扫描输入数 ${scanInputs.size} 与 num_scan_inputs $scanInputCount 不符 / " +
                        "Scan has ${scanInputs.size} scan inputs but num_scan_inputs is $scanInputCount",
                    nodeName = node.name
                )
            )
        }
        val stateTensors = ArrayList<OnnxTensorExpr>(states.size)
        for (name in states) {
            val tensor = context.values[name]
                ?: return OnnxTensorResult.Failure(
                    OnnxErrorFactory.shapeMismatch(
                        detail = "Scan 状态 '$name' 不可得 / Scan state '$name' is unavailable",
                        tensorName = name,
                        nodeName = node.name
                    )
                )
            stateTensors.add(tensor)
        }
        val scanTensors = ArrayList<OnnxTensorExpr>(scanInputs.size)
        for (name in scanInputs) {
            val tensor = context.values[name]
                ?: return OnnxTensorResult.Failure(
                    OnnxErrorFactory.shapeMismatch(
                        detail = "Scan 扫描输入 '$name' 不可得 / Scan input '$name' is unavailable",
                        tensorName = name,
                        nodeName = node.name
                    )
                )
            scanTensors.add(tensor)
        }
        val tripCount = scanTensors.firstOrNull()?.shape?.firstOrNull()
            ?: return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedOperator(
                    opType = "Scan",
                    detail = "Scan 的扫描长度无法从输入形状推导 / the Scan trip count cannot be derived from the input shape",
                    nodeName = node.name
                )
            )
        if (tripCount !in 0..MAX_UNROLL) {
            return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedOperator(
                    opType = "Scan",
                    detail = "Scan 扫描长度 $tripCount 超出可展开区间 0..$MAX_UNROLL / " +
                        "Scan trip count $tripCount is outside the unrollable range 0..$MAX_UNROLL",
                    nodeName = node.name
                )
            )
        }
        val body = subgraph(node, "body")
            ?: return OnnxTensorResult.Failure(
                OnnxErrorFactory.unsupportedAttribute(
                    attributeName = "body",
                    detail = "Scan 缺少循环体 / Scan has no body",
                    nodeName = node.name
                )
            )
        val parsed = when (val result = parseOnnxSubgraph(body, 0L)) {
            is OnnxParseResult.Success -> result.graph
            is OnnxParseResult.Failure -> return OnnxTensorResult.Failure(result.error)
        }
        val bodyInputNames = parsed.inputs.map { it.name }
        var scanning: List<OnnxTensorExpr> = stateTensors
        val scanStates = ArrayList<List<OnnxTensorExpr>>()
        for (iteration in 0 until tripCount) {
            val seed = LinkedHashMap<String, OnnxTensorExpr>()
            for ((position, inputName) in bodyInputNames.withIndex()) {
                seed[inputName] = if (position < scanning.size) {
                    scanning[position]
                } else {
                    val scanIndex = position - scanning.size
                    val source = scanTensors.getOrNull(scanIndex)
                        ?: return OnnxTensorResult.Failure(
                            OnnxErrorFactory.shapeMismatch(
                                detail = "循环体缺少第 $scanIndex 个扫描输入 / the loop body is missing scan input $scanIndex",
                                nodeName = node.name
                            )
                        )
                    sliceLeading(node, source, iteration)
                }
            }
            val produced = when (
                val result = lowerParsed(node, parsed, context, seed, parsed.outputs.map { it.name })
            ) {
                is OnnxTensorResult.Success -> result.tensors
                is OnnxTensorResult.Failure -> return result
            }
            if (produced.size < scanning.size) {
                return OnnxTensorResult.Failure(
                    OnnxErrorFactory.shapeMismatch(
                        detail = "Scan 循环体产出的状态数 ${produced.size} 少于输入状态数 ${scanning.size} / " +
                            "the Scan body produces ${produced.size} states but ${scanning.size} are given",
                        nodeName = node.name
                    )
                )
            }
            scanning = produced.take(stateTensors.size)
            scanStates.add(produced.drop(stateTensors.size))
        }
        val outputs = ArrayList<OnnxTensorExpr>(stateTensors.size + (scanStates.firstOrNull()?.size ?: 0))
        outputs.addAll(scanning)
        val perIteration = scanStates.firstOrNull()?.size ?: 0
        for (index in 0 until perIteration) {
            val concatenated = when (
                val result = concatScan(node, scanStates.map { it[index] })
            ) {
                is OnnxShapeResult.Success -> result.value
                is OnnxShapeResult.Failure -> return OnnxTensorResult.Failure(result.error)
            }
            outputs.add(concatenated)
        }
        return OnnxTensorResult.Success(outputs)
    }

    /** 标量常量张量 / Scalar constant tensor */
    private fun scalarConstant(value: Flt64): OnnxTensorExpr {
        return OnnxTensorExpr(shape = emptyList(), elements = listOf(OnnxExpr.Constant(value)))
    }

    /** 读取常量张量的数值 / Read the values of a constant tensor */
    private fun constantValues(
        name: String,
        context: OnnxLoweringContext,
        node: OnnxNode
    ): OnnxShapeResult<List<Double>> {
        val tensor = context.values[name]
            ?: return OnnxShapeResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "控制张量 '$name' 不可得 / control tensor '$name' is unavailable",
                    tensorName = name,
                    nodeName = node.name
                )
            )
        if (!tensor.isConstant) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.unsupportedOperator(
                    opType = node.opType,
                    detail = "控制张量 '$name' 必须是常量 / control tensor '$name' must be constant",
                    nodeName = node.name
                )
            )
        }
        return OnnxShapeResult.Success(tensor.elements.map { (it as OnnxExpr.Constant).value.toDouble() })
    }

    /** 沿首维切片 / Slice along the leading dimension */
    private fun sliceLeading(node: OnnxNode, tensor: OnnxTensorExpr, index: Int): OnnxTensorExpr {
        if (tensor.shape.size <= 1) {
            return tensor
        }
        val leading = tensor.shape.first()
        if (leading <= 0) {
            return tensor
        }
        val sliceSize = tensor.elementCount / leading
        val start = index * sliceSize
        return OnnxTensorExpr(
            shape = tensor.shape.drop(1),
            elements = tensor.elements.subList(start, start + sliceSize).toList()
        )
    }

    /** 把逐轮扫描输出拼成一条序列 / Concatenate per-iteration scan outputs into one sequence */
    private fun concatScan(node: OnnxNode, slices: List<OnnxTensorExpr>): OnnxShapeResult<OnnxTensorExpr> {
        if (slices.isEmpty()) {
            return OnnxShapeResult.Failure(
                OnnxErrorFactory.shapeMismatch(
                    detail = "扫描输出为空 / the scan output is empty",
                    nodeName = node.name
                )
            )
        }
        val tailShape = slices.first().shape
        val total = slices.fold(0) { acc, slice -> acc + slice.elementCount }
        val elements = ArrayList<OnnxExpr>(total)
        for ((iteration, slice) in slices.withIndex()) {
            if (slice.shape != tailShape) {
                return OnnxShapeResult.Failure(
                    OnnxErrorFactory.shapeMismatch(
                        detail = "第 $iteration 轮扫描输出形状 ${slice.shape} 与首轮 $tailShape 不符 / " +
                            "scan output shape ${slice.shape} at iteration $iteration does not match the first iteration $tailShape",
                        nodeName = node.name
                    )
                )
            }
            elements.addAll(slice.elements)
        }
        return OnnxShapeResult.Success(
            OnnxTensorExpr(
                shape = listOf(slices.size) + tailShape,
                elements = elements
            )
        )
    }
}

/**
 * 子图控制流降阶结果 / Subgraph control-flow lowering result
*/
internal sealed interface OnnxTensorResult {

    /** 成功 / Success */
    data class Success(val tensors: List<OnnxTensorExpr>) : OnnxTensorResult

    /** 失败 / Failure */
    data class Failure(val error: OnnxError) : OnnxTensorResult
}
