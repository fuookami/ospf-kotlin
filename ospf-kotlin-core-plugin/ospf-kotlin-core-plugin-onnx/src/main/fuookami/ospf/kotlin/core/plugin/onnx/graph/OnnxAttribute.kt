/**
 * ONNX 属性归一化 / ONNX attribute normalization
*/
package fuookami.ospf.kotlin.core.plugin.onnx.graph

import ai.onnx.proto.OnnxMl

/**
 * 归一化后的 ONNX 属性值 / Normalized ONNX attribute value
 *
 * 只保留首版算子需要的标量、列表与张量形态；子图、稀疏张量等形态直接拒绝。 / Only scalar, list and tensor forms needed by the first-version operators are kept;
 * subgraphs, sparse tensors and other forms are rejected outright.
*/
sealed interface OnnxAttributeValue {

    /** 浮点标量 / Float scalar */
    data class FloatScalar(val value: Double) : OnnxAttributeValue

    /** 整数标量 / Integer scalar */
    data class IntScalar(val value: Long) : OnnxAttributeValue

    /** 字符串标量 / String scalar */
    data class StringScalar(val value: String) : OnnxAttributeValue

    /** 浮点列表 / Float list */
    data class FloatList(val values: List<Double>) : OnnxAttributeValue

    /** 整数列表 / Integer list */
    data class IntList(val values: List<Long>) : OnnxAttributeValue

    /** 字符串列表 / String list */
    data class StringList(val values: List<String>) : OnnxAttributeValue

    /** 常量张量 / Constant tensor */
    data class TensorValue(val value: OnnxTensorConstant) : OnnxAttributeValue

    /** 子图 / Subgraph */
    data class GraphValue(val proto: OnnxMl.GraphProto) : OnnxAttributeValue

    /** 子图列表 / Subgraph list */
    data class GraphsValue(val protos: List<OnnxMl.GraphProto>) : OnnxAttributeValue
}

/**
 * 节点属性集合 / Node attribute collection
 *
 * @property values 归一化属性映射 / normalized attribute map
*/
data class OnnxAttributes(val values: Map<String, OnnxAttributeValue>) {

    /** 是否包含指定属性 / Whether the attribute exists */
    fun has(name: String): Boolean {
        return values.containsKey(name)
    }

    /**
     * 读取浮点属性 / Read a float attribute
     *
     * @param name 属性名 / attribute name
     * @param default 缺省值 / default value
     * @return 属性值 / attribute value
     */
    fun float(name: String, default: Double): Double {
        return (values[name] as? OnnxAttributeValue.FloatScalar)?.value ?: default
    }

    /**
     * 读取可空浮点属性 / Read a nullable float attribute
     *
     * @param name 属性名 / attribute name
     * @return 属性值，缺失或类型不符返回 null / attribute value, or null when absent or mistyped
     */
    fun floatOrNull(name: String): Double? {
        return (values[name] as? OnnxAttributeValue.FloatScalar)?.value
    }

    /**
     * 读取整数属性 / Read an integer attribute
     *
     * @param name 属性名 / attribute name
     * @param default 缺省值 / default value
     * @return 属性值 / attribute value
     */
    fun int(name: String, default: Long): Long {
        return (values[name] as? OnnxAttributeValue.IntScalar)?.value ?: default
    }

    /**
     * 读取可空整数属性 / Read a nullable integer attribute
     *
     * @param name 属性名 / attribute name
     * @return 属性值，缺失或类型不符返回 null / attribute value, or null when absent or mistyped
     */
    fun intOrNull(name: String): Long? {
        return (values[name] as? OnnxAttributeValue.IntScalar)?.value
    }

    /**
     * 读取整数列表属性 / Read an integer list attribute
     *
     * @param name 属性名 / attribute name
     * @return 列表，缺失或类型不符返回空列表 / list, or empty when absent or mistyped
     */
    fun ints(name: String): List<Long> {
        return (values[name] as? OnnxAttributeValue.IntList)?.values ?: emptyList()
    }

    /**
     * 读取字符串属性 / Read a string attribute
     *
     * @param name 属性名 / attribute name
     * @param default 缺省值 / default value
     * @return 属性值 / attribute value
     */
    fun string(name: String, default: String): String {
        return (values[name] as? OnnxAttributeValue.StringScalar)?.value ?: default
    }

    /**
     * 读取张量属性 / Read a tensor attribute
     *
     * @param name 属性名 / attribute name
     * @return 常量张量，缺失或类型不符返回 null / constant tensor, or null when absent or mistyped
     */
    fun tensor(name: String): OnnxTensorConstant? {
        return (values[name] as? OnnxAttributeValue.TensorValue)?.value
    }
}

/**
 * 把 `AttributeProto` 列表归一化为 [OnnxAttributes] / Normalize `AttributeProto` list into [OnnxAttributes]
 *
 * @param attributes 协议属性列表 / protocol attribute list
 * @param nodeName 节点名，用于错误上下文 / node name for error context
 * @return 归一化属性，含不支持形态时失败 / normalized attributes, failing on unsupported forms
 */
internal fun normalizeAttributes(
    attributes: List<OnnxMl.AttributeProto>,
    nodeName: String
): OnnxAttributeResult {
    val values = LinkedHashMap<String, OnnxAttributeValue>()
    for (attribute in attributes) {
        val name = attribute.name
        val normalized = when (attribute.type) {
            OnnxMl.AttributeProto.AttributeType.FLOAT ->
                OnnxAttributeValue.FloatScalar(attribute.f.toDouble())

            OnnxMl.AttributeProto.AttributeType.INT ->
                OnnxAttributeValue.IntScalar(attribute.i)

            OnnxMl.AttributeProto.AttributeType.STRING ->
                OnnxAttributeValue.StringScalar(attribute.s.toStringUtf8())

            OnnxMl.AttributeProto.AttributeType.FLOATS ->
                OnnxAttributeValue.FloatList(attribute.floatsList.map { it.toDouble() })

            OnnxMl.AttributeProto.AttributeType.INTS ->
                OnnxAttributeValue.IntList(attribute.intsList.toList())

            OnnxMl.AttributeProto.AttributeType.STRINGS ->
                OnnxAttributeValue.StringList(attribute.stringsList.map { it.toStringUtf8() })

            OnnxMl.AttributeProto.AttributeType.TENSOR ->
                OnnxAttributeValue.TensorValue(decodeTensorConstant(attribute.t).value
                    ?: return OnnxAttributeResult.Failure(
                        attributeName = name,
                        detail = "属性 '$name' 的张量无法解码 / tensor attribute '$name' cannot be decoded",
                        nodeName = nodeName
                    ))

            OnnxMl.AttributeProto.AttributeType.GRAPH ->
                OnnxAttributeValue.GraphValue(attribute.g)

            OnnxMl.AttributeProto.AttributeType.GRAPHS ->
                OnnxAttributeValue.GraphsValue(attribute.graphsList.toList())

            else ->
                return OnnxAttributeResult.Failure(
                    attributeName = name,
                    detail = "属性 '$name' 类型为 ${attribute.type}，不受支持 / attribute '$name' of type ${attribute.type} is unsupported",
                    nodeName = nodeName
                )
        }
        values[name] = normalized
    }
    return OnnxAttributeResult.Success(OnnxAttributes(values))
}

/**
 * 属性归一化结果 / Attribute normalization result
*/
internal sealed interface OnnxAttributeResult {

    /** 成功 / Success */
    data class Success(val attributes: OnnxAttributes) : OnnxAttributeResult

    /** 失败 / Failure */
    data class Failure(
        val attributeName: String,
        val detail: String,
        val nodeName: String
    ) : OnnxAttributeResult
}
