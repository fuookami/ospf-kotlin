/**
 * ONNX 降阶错误工厂 / ONNX lowering error factory
*/
package fuookami.ospf.kotlin.core.plugin.onnx.lowering

import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError

/**
 * 降阶阶段的结构化错误构造 / Structured error construction for the lowering stage
*/
internal object OnnxErrorFactory {

    /**
     * 构造次数溢出错误 / Build a degree overflow error
     *
     * @param detail 细节 / detail
     * @param nodeName 节点名 / node name
     * @return 结构化错误 / structured error
     */
    fun degreeOverflow(detail: String, nodeName: String? = null): OnnxError {
        return OnnxError.DegreeOverflow(detail = detail, nodeName = nodeName)
    }

    /**
     * 构造算子不支持错误 / Build an unsupported operator error
     *
     * @param opType 算子类型 / operator type
     * @param detail 细节 / detail
     * @param nodeName 节点名 / node name
     * @return 结构化错误 / structured error
     */
    fun unsupportedOperator(opType: String, detail: String, nodeName: String? = null): OnnxError {
        return OnnxError.UnsupportedOperator(opType = opType, detail = detail, nodeName = nodeName)
    }

    /**
     * 构造形状不匹配错误 / Build a shape mismatch error
     *
     * @param detail 细节 / detail
     * @param nodeName 节点名 / node name
     * @param tensorName 张量名 / tensor name
     * @param shape 形状 / shape
     * @return 结构化错误 / structured error
     */
    fun shapeMismatch(
        detail: String,
        nodeName: String? = null,
        tensorName: String? = null,
        shape: List<Long?>? = null
    ): OnnxError {
        return OnnxError.ShapeMismatch(
            detail = detail,
            nodeName = nodeName,
            tensorName = tensorName,
            shape = shape
        )
    }

    /**
     * 构造动态形状错误 / Build a dynamic shape error
     *
     * @param detail 细节 / detail
     * @param tensorName 张量名 / tensor name
     * @param shape 形状 / shape
     * @return 结构化错误 / structured error
     */
    fun dynamicShape(detail: String, tensorName: String? = null, shape: List<Long?>? = null): OnnxError {
        return OnnxError.DynamicShape(detail = detail, tensorName = tensorName, shape = shape)
    }

    /**
     * 构造缺少范围错误 / Build a missing bound error
     *
     * @param detail 细节 / detail
     * @param nodeName 节点名 / node name
     * @param tensorName 张量名 / tensor name
     * @return 结构化错误 / structured error
     */
    fun missingBound(detail: String, nodeName: String? = null, tensorName: String? = null): OnnxError {
        return OnnxError.MissingBound(detail = detail, nodeName = nodeName, tensorName = tensorName)
    }

    /**
     * 构造属性不支持错误 / Build an unsupported attribute error
     *
     * @param attributeName 属性名 / attribute name
     * @param detail 细节 / detail
     * @param nodeName 节点名 / node name
     * @return 结构化错误 / structured error
     */
    fun unsupportedAttribute(attributeName: String, detail: String, nodeName: String? = null): OnnxError {
        return OnnxError.UnsupportedAttribute(
            attributeName = attributeName,
            detail = detail,
            nodeName = nodeName
        )
    }
}
