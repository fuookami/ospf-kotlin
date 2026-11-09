/**
 * ONNX 模型构造夹具 / ONNX model builder fixtures
 *
 * 测试模型全部在内存中构造，不提交任何 `.onnx` 或 `.pb` 二进制。 / All test models are built in memory; no `.onnx` or `.pb` binary is committed.
*/
package fuookami.ospf.kotlin.core.plugin.onnx

import ai.onnx.proto.OnnxMl

/** 主域 opset 版本 / Main-domain opset version */
const val TEST_OPSET: Long = 13L

/** 测试模型 IR 版本 / Test model IR version */
const val TEST_IR_VERSION: Long = 8L

/**
 * 构造单精度张量 / Build a Float32 tensor
 *
 * @param name 张量名 / tensor name
 * @param dims 形状 / shape
 * @param values 行主序数据 / row-major data
 * @return 协议张量 / protocol tensor
 */
fun floatTensor(
    name: String,
    dims: List<Long>,
    values: List<Float>
): OnnxMl.TensorProto {
    return OnnxMl.TensorProto.newBuilder()
        .setName(name)
        .setDataType(1)
        .addAllDims(dims)
        .addAllFloatData(values)
        .build()
}

/**
 * 构造双精度张量 / Build a Float64 tensor
 *
 * @param name 张量名 / tensor name
 * @param dims 形状 / shape
 * @param values 行主序数据 / row-major data
 * @return 协议张量 / protocol tensor
 */
fun doubleTensor(
    name: String,
    dims: List<Long>,
    values: List<Double>
): OnnxMl.TensorProto {
    return OnnxMl.TensorProto.newBuilder()
        .setName(name)
        .setDataType(11)
        .addAllDims(dims)
        .addAllDoubleData(values)
        .build()
}

/**
 * 构造整数张量 / Build an Int64 tensor
 *
 * @param name 张量名 / tensor name
 * @param dims 形状 / shape
 * @param values 行主序数据 / row-major data
 * @return 协议张量 / protocol tensor
 */
fun longTensor(
    name: String,
    dims: List<Long>,
    values: List<Long>
): OnnxMl.TensorProto {
    return OnnxMl.TensorProto.newBuilder()
        .setName(name)
        .setDataType(7)
        .addAllDims(dims)
        .addAllInt64Data(values)
        .build()
}

/**
 * 构造浮点值信息 / Build a Float32 value info
 *
 * @param name 张量名 / tensor name
 * @param shape 形状，null 元素表示动态维度 / shape, null entries are dynamic dimensions
 * @return 协议值信息 / protocol value info
 */
fun floatValueInfo(name: String, shape: List<Long?>): OnnxMl.ValueInfoProto {
    return valueInfo(name, shape, elemType = 1)
}

/**
 * 构造双精度值信息 / Build a Float64 value info
 *
 * @param name 张量名 / tensor name
 * @param shape 形状，null 元素表示动态维度 / shape, null entries are dynamic dimensions
 * @return 协议值信息 / protocol value info
 */
fun doubleValueInfo(name: String, shape: List<Long?>): OnnxMl.ValueInfoProto {
    return valueInfo(name, shape, elemType = 11)
}

/**
 * 构造值信息 / Build a value info
 *
 * @param name 张量名 / tensor name
 * @param shape 形状，null 元素表示动态维度 / shape, null entries are dynamic dimensions
 * @param elemType `TensorProto.DataType` 值 / `TensorProto.DataType` value
 * @return 协议值信息 / protocol value info
 */
fun valueInfo(name: String, shape: List<Long?>, elemType: Int): OnnxMl.ValueInfoProto {
    val shapeProto = OnnxMl.TensorShapeProto.newBuilder()
    for (dim in shape) {
        val dimension = OnnxMl.TensorShapeProto.Dimension.newBuilder()
        if (dim != null) {
            dimension.setDimValue(dim)
        } else {
            dimension.setDimParam("N")
        }
        shapeProto.addDim(dimension)
    }
    val tensorType = OnnxMl.TypeProto.Tensor.newBuilder()
        .setElemType(elemType)
        .setShape(shapeProto)
    val type = OnnxMl.TypeProto.newBuilder().setTensorType(tensorType)
    return OnnxMl.ValueInfoProto.newBuilder()
        .setName(name)
        .setType(type)
        .build()
}

/**
 * 重命名值信息 / Rename a value info
 *
 * @param name 新名称 / new name
 * @return 重命名后的值信息 / renamed value info
 */
fun OnnxMl.ValueInfoProto.renamed(name: String): OnnxMl.ValueInfoProto {
    return toBuilder().setName(name).build()
}

/**
 * 构造节点 / Build a node
 *
 * @param opType 算子类型 / operator type
 * @param inputs 输入名 / input names
 * @param outputs 输出名 / output names
 * @param name 节点名 / node name
 * @param attributes 属性构造块 / attribute builder block
 * @return 协议节点 / protocol node
 */
fun node(
    opType: String,
    inputs: List<String>,
    outputs: List<String>,
    name: String = "",
    attributes: OnnxMl.NodeProto.Builder.() -> Unit = {}
): OnnxMl.NodeProto {
    val builder = OnnxMl.NodeProto.newBuilder()
        .setOpType(opType)
        .addAllInput(inputs)
        .addAllOutput(outputs)
    if (name.isNotEmpty()) {
        builder.setName(name)
    }
    builder.attributes()
    return builder.build()
}

/**
 * 构造整型属性 / Build an int attribute
 *
 * @param name 属性名 / attribute name
 * @param value 属性值 / attribute value
 * @return 协议属性 / protocol attribute
 */
fun intAttr(name: String, value: Long): OnnxMl.AttributeProto {
    return OnnxMl.AttributeProto.newBuilder()
        .setName(name)
        .setType(OnnxMl.AttributeProto.AttributeType.INT)
        .setI(value)
        .build()
}

/**
 * 构造浮点属性 / Build a float attribute
 *
 * @param name 属性名 / attribute name
 * @param value 属性值 / attribute value
 * @return 协议属性 / protocol attribute
 */
fun floatAttr(name: String, value: Double): OnnxMl.AttributeProto {
    return OnnxMl.AttributeProto.newBuilder()
        .setName(name)
        .setType(OnnxMl.AttributeProto.AttributeType.FLOAT)
        .setF(value.toFloat())
        .build()
}

/**
 * 构造整型列表属性 / Build an int list attribute
 *
 * @param name 属性名 / attribute name
 * @param values 属性值 / attribute values
 * @return 协议属性 / protocol attribute
 */
fun intsAttr(name: String, values: List<Long>): OnnxMl.AttributeProto {
    return OnnxMl.AttributeProto.newBuilder()
        .setName(name)
        .setType(OnnxMl.AttributeProto.AttributeType.INTS)
        .addAllInts(values)
        .build()
}

/**
 * 构造完整模型 / Build a complete model
 *
 * @param inputs 图输入 / graph inputs
 * @param outputs 图输出 / graph outputs
 * @param nodes 计算节点 / computational nodes
 * @param initializers 初始化器 / initializers
 * @param opset 主域 opset / main-domain opset
 * @param extraOpsets 附加 opset（用于测试自定义域）/ extra opsets (for custom-domain tests)
 * @return 协议模型 / protocol model
 */
fun model(
    inputs: List<OnnxMl.ValueInfoProto>,
    outputs: List<OnnxMl.ValueInfoProto>,
    nodes: List<OnnxMl.NodeProto>,
    initializers: List<OnnxMl.TensorProto> = emptyList(),
    opset: Long = TEST_OPSET,
    extraOpsets: List<Pair<String, Long>> = emptyList()
): OnnxMl.ModelProto {
    val graph = OnnxMl.GraphProto.newBuilder()
        .setName("test-graph")
        .addAllInput(inputs)
        .addAllOutput(outputs)
        .addAllNode(nodes)
        .addAllInitializer(initializers)
        .build()
    val builder = OnnxMl.ModelProto.newBuilder()
        .setIrVersion(TEST_IR_VERSION)
        .setProducerName("ospf-kotlin-test")
        .setGraph(graph)
        .addOpsetImport(
            OnnxMl.OperatorSetIdProto.newBuilder()
                .setDomain("")
                .setVersion(opset)
        )
    for ((domain, version) in extraOpsets) {
        builder.addOpsetImport(
            OnnxMl.OperatorSetIdProto.newBuilder()
                .setDomain(domain)
                .setVersion(version)
        )
    }
    return builder.build()
}
