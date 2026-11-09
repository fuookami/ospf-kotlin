/**
 * ONNX 图解析 / ONNX graph parsing
*/
package fuookami.ospf.kotlin.core.plugin.onnx.graph

import ai.onnx.proto.OnnxMl

/**
 * 解析后的 ONNX 节点 / Parsed ONNX node
 *
 * @property name 节点名，缺省时按算子与序号合成 / node name, synthesized from op type and index when absent
 * @property opType 算子类型 / operator type
 * @property inputs 输入张量名列表 / input tensor names
 * @property outputs 输出张量名列表 / output tensor names
 * @property attributes 归一化属性 / normalized attributes
 * @property index 在原图中的位置 / position in the original graph
*/
data class OnnxNode(
    val name: String,
    val opType: String,
    val inputs: List<String>,
    val outputs: List<String>,
    val attributes: OnnxAttributes,
    val index: Int
)

/**
 * 解析后的 ONNX 图 / Parsed ONNX graph
 *
 * @property inputs 图输入 / graph inputs
 * @property outputs 图输出 / graph outputs
 * @property initializers 常量张量（已并入 `Constant` 节点）/ constant tensors (with `Constant` nodes folded in)
 * @property nodes 按原顺序保留的计算节点 / computational nodes kept in original order
 * @property opsetVersion 主域 opset 版本 / main-domain opset version
 * @property producerName 生成工具名 / producing tool name
*/
data class OnnxParsedGraph(
    val inputs: List<OnnxValueInfo>,
    val outputs: List<OnnxValueInfo>,
    val initializers: Map<String, OnnxTensorConstant>,
    val nodes: List<OnnxNode>,
    val opsetVersion: Long,
    val producerName: String
)

/**
 * 图输入输出元信息 / Graph input/output metadata
 *
 * @property name 张量名 / tensor name
 * @property dtype 数据类型 / data type
 * @property shape 形状，动态维度为 null 元素 / shape, with null entries for dynamic dimensions
*/
data class OnnxValueInfo(
    val name: String,
    val dtype: OnnxDataType,
    val shape: List<Long?>
) {
    /** 是否全静态 / Whether the shape is fully static */
    val staticShape: List<Long>?
        get() = if (shape.any { it == null }) null else shape.map { it!! }
}

/**
 * 解析结果 / Parse result
*/
internal sealed interface OnnxParseResult {

    /** 成功 / Success */
    data class Success(val graph: OnnxParsedGraph) : OnnxParseResult

    /** 失败 / Failure */
    data class Failure(val error: fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError) : OnnxParseResult
}

/** 首版支持的主域 opset 区间 / Main-domain opset range supported by the first version */
val SUPPORTED_OPSET: IntRange = 13..24

/** 主域名称 / Main domain name */
private const val MAIN_DOMAIN = ""

/**
 * 把 ONNX 模型字节解析为 [OnnxParsedGraph] / Parse ONNX model bytes into [OnnxParsedGraph]
 *
 * 解析顺序：protobuf 解码 → opset 校验 → 图结构校验 → initializer 与 `Constant` 折叠 → 输入可得出校验。 / Order: protobuf decode → opset validation → graph structure validation → initializer and
 * `Constant` folding → input availability validation.
 *
 * @param bytes 模型字节 / model bytes
 * @return 解析结果 / parse result
 */
internal fun parseOnnxGraph(bytes: ByteArray): OnnxParseResult {
    val model = try {
        OnnxMl.ModelProto.parseFrom(bytes)
    } catch (e: Exception) {
        return OnnxParseResult.Failure(
            fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.LoadFailed(
                detail = e.message ?: e::class.simpleName ?: "protobuf decode error"
            )
        )
    }
    return parseOnnxGraph(model)
}

/**
 * 把 `ModelProto` 解析为 [OnnxParsedGraph] / Parse a `ModelProto` into [OnnxParsedGraph]
 *
 * @param model 协议模型 / protocol model
 * @return 解析结果 / parse result
 */
internal fun parseOnnxGraph(model: OnnxMl.ModelProto): OnnxParseResult {
    if (!model.hasGraph()) {
        return OnnxParseResult.Failure(
            fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.InvalidModel("模型缺少 graph / model has no graph")
        )
    }

    val opset = when (val result = resolveOpset(model)) {
        is OnnxOpsetResult.Success -> result.version
        is OnnxOpsetResult.Failure -> return OnnxParseResult.Failure(result.error)
    }

    return parseGraphBody(
        graph = model.graph,
        opset = opset,
        producerName = if (model.hasProducerName()) model.producerName else "",
        allowExternalInputs = false
    )
}

/**
 * 把子图解析为 [OnnxParsedGraph] / Parse a subgraph into [OnnxParsedGraph]
 *
 * 子图节点可以引用外层作用域的张量，因此允许未知输入名通过解析期校验，
 * 由降阶层在找不到值时给出明确错误。 / Subgraph nodes may reference tensors of the enclosing scope, so unknown input names pass
 * the parse-time check; the lowering stage reports a clear error when a value cannot be resolved.
 *
 * @param graph 子图协议 / subgraph protocol
 * @param opset 主域 opset 版本 / main-domain opset version
 * @return 解析结果 / parse result
 */
internal fun parseOnnxSubgraph(graph: OnnxMl.GraphProto, opset: Long): OnnxParseResult {
    return parseGraphBody(
        graph = graph,
        opset = opset,
        producerName = "",
        allowExternalInputs = true
    )
}

/**
 * 解析图主体 / Parse a graph body
 *
 * @param graph 图协议 / graph protocol
 * @param opset 主域 opset 版本 / main-domain opset version
 * @param producerName 生成工具名 / producing tool name
 * @param allowExternalInputs 是否允许引用外层作用域 / whether outer-scope references are allowed
 * @return 解析结果 / parse result
 */
private fun parseGraphBody(
    graph: OnnxMl.GraphProto,
    opset: Long,
    producerName: String,
    allowExternalInputs: Boolean
): OnnxParseResult {
    if (graph.nodeCount == 0 && graph.initializerCount == 0) {
        return OnnxParseResult.Failure(
            fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.InvalidModel("图既无节点也无初始化器 / graph has neither nodes nor initializers")
        )
    }

    val initializers = LinkedHashMap<String, OnnxTensorConstant>()
    for (proto in graph.initializerList) {
        val decoded = decodeTensorConstant(proto)
        val constant = decoded.value
            ?: return OnnxParseResult.Failure(
                fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.InvalidModel(decoded.detail ?: "初始化器解码失败")
            )
        val name = constant.name
            ?: return OnnxParseResult.Failure(
                fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.InvalidModel("初始化器缺少名称 / initializer has no name")
            )
        if (initializers.containsKey(name)) {
            return OnnxParseResult.Failure(
                fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.InvalidModel("初始化器 '$name' 重复定义 / duplicated initializer '$name'")
            )
        }
        initializers[name] = constant
    }

    val inputs = mutableListOf<OnnxValueInfo>()
    for (proto in graph.inputList) {
        val info = when (val result = decodeValueInfo(proto)) {
            is OnnxValueInfoResult.Success -> result.value
            is OnnxValueInfoResult.Failure -> return OnnxParseResult.Failure(result.error)
        }
        // 作为初始化器出现的输入不是真实输入。 / Inputs that appear as initializers are not real inputs.
        if (!initializers.containsKey(info.name)) {
            inputs.add(info)
        }
    }

    val outputs = mutableListOf<OnnxValueInfo>()
    for (proto in graph.outputList) {
        val info = when (val result = decodeValueInfo(proto)) {
            is OnnxValueInfoResult.Success -> result.value
            is OnnxValueInfoResult.Failure -> return OnnxParseResult.Failure(result.error)
        }
        outputs.add(info)
    }
    if (outputs.isEmpty()) {
        return OnnxParseResult.Failure(
            fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.InvalidModel("图缺少输出 / graph has no output")
        )
    }

    val nodes = mutableListOf<OnnxNode>()
    for ((index, proto) in graph.nodeList.withIndex()) {
        val node = when (val result = decodeNode(proto, index)) {
            is OnnxNodeResult.Success -> result.node
            is OnnxNodeResult.Failure -> return OnnxParseResult.Failure(result.error)
        }
        if (node.opType == "Constant") {
            val folded = when (val result = foldConstant(node)) {
                is OnnxConstantFoldResult.Success -> result
                is OnnxConstantFoldResult.Failure -> return OnnxParseResult.Failure(result.error)
            }
            for ((name, constant) in folded.constants) {
                if (initializers.containsKey(name)) {
                    return OnnxParseResult.Failure(
                        fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.InvalidModel(
                            detail = "常量 '$name' 与初始化器重名 / constant '$name' collides with an initializer",
                            nodeName = node.name
                        )
                    )
                }
                initializers[name] = constant
            }
            continue
        }
        nodes.add(node)
    }

    val available = initializers.keys.toMutableSet()
    available.addAll(inputs.map { it.name })
    for (node in nodes) {
        for (input in node.inputs) {
            if (input.isEmpty()) {
                continue
            }
            if (!available.contains(input)) {
                // 子图允许引用外层作用域，降阶时再解析。 / Subgraphs may reference the enclosing scope; resolution happens during lowering.
                if (allowExternalInputs) {
                    continue
                }
                return OnnxParseResult.Failure(
                    fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.InvalidModel(
                        detail = "节点输入 '$input' 在当前位置不可得（要求图按拓扑序排列） / " +
                            "node input '$input' is unavailable at this position (graph must be topologically ordered)",
                        nodeName = node.name
                    )
                )
            }
        }
        for (output in node.outputs) {
            if (output.isEmpty()) {
                continue
            }
            if (!available.add(output)) {
                return OnnxParseResult.Failure(
                    fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.InvalidModel(
                        detail = "输出张量 '$output' 重复定义 / duplicated output tensor '$output'",
                        nodeName = node.name
                    )
                )
            }
        }
    }
    if (!allowExternalInputs) {
        for (output in outputs) {
            if (!available.contains(output.name)) {
                return OnnxParseResult.Failure(
                    fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.InvalidModel(
                        detail = "图输出 '${output.name}' 未被任何节点或常量定义 / graph output '${output.name}' is not produced"
                    )
                )
            }
        }
    }

    return OnnxParseResult.Success(
        OnnxParsedGraph(
            inputs = inputs,
            outputs = outputs,
            initializers = initializers,
            nodes = nodes,
            opsetVersion = opset,
            producerName = producerName
        )
    )
}

/**
 * opset 解析结果 / opset resolution result
*/
private sealed interface OnnxOpsetResult {
    data class Success(val version: Long) : OnnxOpsetResult
    data class Failure(val error: fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError) : OnnxOpsetResult
}

/**
 * 校验并解析主域 opset / Validate and resolve the main-domain opset
 *
 * @param model 协议模型 / protocol model
 * @return opset 结果 / opset result
 */
private fun resolveOpset(model: OnnxMl.ModelProto): OnnxOpsetResult {
    var mainVersion: Long? = null
    for (opset in model.opsetImportList) {
        val domain = opset.domain
        if (domain != MAIN_DOMAIN && domain != "ai.onnx") {
            return OnnxOpsetResult.Failure(
                fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.UnsupportedOpset(
                    domain = domain,
                    version = opset.version,
                    supported = SUPPORTED_OPSET
                )
            )
        }
        mainVersion = opset.version
    }
    val version = mainVersion
        ?: return OnnxOpsetResult.Failure(
            fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.UnsupportedOpset(
                domain = MAIN_DOMAIN,
                version = 0L,
                supported = SUPPORTED_OPSET
            )
        )
    if (version !in SUPPORTED_OPSET.map { it.toLong() }) {
        return OnnxOpsetResult.Failure(
            fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.UnsupportedOpset(
                domain = MAIN_DOMAIN,
                version = version,
                supported = SUPPORTED_OPSET
            )
        )
    }
    return OnnxOpsetResult.Success(version)
}

/**
 * 值信息解析结果 / value info resolution result
*/
private sealed interface OnnxValueInfoResult {
    data class Success(val value: OnnxValueInfo) : OnnxValueInfoResult
    data class Failure(val error: fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError) : OnnxValueInfoResult
}

/**
 * 解码 `ValueInfoProto` / Decode a `ValueInfoProto`
 *
 * @param proto 协议值信息 / protocol value info
 * @return 解码结果 / decoding result
 */
private fun decodeValueInfo(proto: OnnxMl.ValueInfoProto): OnnxValueInfoResult {
    val name = proto.name
    if (!proto.hasType() || !proto.type.hasTensorType()) {
        return OnnxValueInfoResult.Failure(
            fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.UnsupportedDType(
                dataType = "non-tensor value",
                tensorName = name
            )
        )
    }
    val tensorType = proto.type.tensorType
    val dtype = OnnxDataType.of(tensorType.elemType)
        ?: return OnnxValueInfoResult.Failure(
            fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.UnsupportedDType(
                dataType = OnnxDataType.displayNameOf(tensorType.elemType),
                tensorName = name
            )
        )
    val shape = mutableListOf<Long?>()
    if (tensorType.hasShape()) {
        for (dim in tensorType.shape.dimList) {
            shape.add(if (dim.hasDimValue()) dim.dimValue else null)
        }
    }
    return OnnxValueInfoResult.Success(OnnxValueInfo(name = name, dtype = dtype, shape = shape))
}

/**
 * 节点解析结果 / node resolution result
*/
private sealed interface OnnxNodeResult {
    data class Success(val node: OnnxNode) : OnnxNodeResult
    data class Failure(val error: fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError) : OnnxNodeResult
}

/**
 * 解码 `NodeProto` / Decode a `NodeProto`
 *
 * @param proto 协议节点 / protocol node
 * @param index 节点位置 / node position
 * @return 解码结果 / decoding result
 */
private fun decodeNode(proto: OnnxMl.NodeProto, index: Int): OnnxNodeResult {
    val opType = proto.opType
    val name = if (proto.hasName() && proto.name.isNotEmpty()) proto.name else "${opType}_$index"
    val attributes = when (val result = normalizeAttributes(proto.attributeList, name)) {
        is OnnxAttributeResult.Success -> result.attributes
        is OnnxAttributeResult.Failure -> return OnnxNodeResult.Failure(
            fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.UnsupportedAttribute(
                attributeName = result.attributeName,
                detail = result.detail,
                nodeName = name
            )
        )
    }
    return OnnxNodeResult.Success(
        OnnxNode(
            name = name,
            opType = opType,
            inputs = proto.inputList.toList(),
            outputs = proto.outputList.toList(),
            attributes = attributes,
            index = index
        )
    )
}

/**
 * `Constant` 节点折叠结果 / `Constant` node folding result
*/
private sealed interface OnnxConstantFoldResult {
    data class Success(val constants: Map<String, OnnxTensorConstant>) : OnnxConstantFoldResult
    data class Failure(val error: fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError) : OnnxConstantFoldResult
}

/**
 * 把 `Constant` 节点折叠为常量表 / Fold a `Constant` node into the constant table
 *
 * @param node 常量节点 / constant node
 * @return 折叠结果 / folding result
 */
private fun foldConstant(node: OnnxNode): OnnxConstantFoldResult {
    val output = node.outputs.firstOrNull()
        ?: return OnnxConstantFoldResult.Failure(
            fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.InvalidModel(
                detail = "Constant 节点缺少输出 / Constant node has no output",
                nodeName = node.name
            )
        )
    val tensor = node.attributes.tensor("value")
    if (tensor != null) {
        return OnnxConstantFoldResult.Success(mapOf(output to tensor.copy(name = output)))
    }
    val scalarValue = node.attributes.floatOrNull("value_float")
    if (scalarValue != null) {
        return OnnxConstantFoldResult.Success(
            mapOf(
                output to OnnxTensorConstant(
                    name = output,
                    dtype = OnnxDataType.Float32,
                    shape = emptyList(),
                    doubles = doubleArrayOf(scalarValue),
                    longs = LongArray(0)
                )
            )
        )
    }
    val intValue = node.attributes.intOrNull("value_int")
    if (intValue != null) {
        return OnnxConstantFoldResult.Success(
            mapOf(
                output to OnnxTensorConstant(
                    name = output,
                    dtype = OnnxDataType.Int64,
                    shape = emptyList(),
                    doubles = DoubleArray(0),
                    longs = longArrayOf(intValue)
                )
            )
        )
    }
    return OnnxConstantFoldResult.Failure(
        fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError.UnsupportedAttribute(
            attributeName = "value",
            detail = "Constant 节点只支持 value/value_float/value_int / Constant node supports only value/value_float/value_int",
            nodeName = node.name
        )
    )
}
