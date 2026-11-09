/**
 * ONNX 插件结构化错误 / Structured errors of the ONNX plugin
*/
package fuookami.ospf.kotlin.core.plugin.onnx.error

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.core.error.*

/**
 * ONNX 插件错误基类 / Base class of ONNX plugin errors
 *
 * 所有公共加载、解析、降阶与推理入口都以 [Ret] 返回这些错误，不向业务层抛出异常。 / All public load, parse, lower and inference entry points return these
 * errors as [Ret] values and never throw to the business layer.
 *
 * @property errorCode 错误码 / Error code
 * @property message 错误消息 / Error message
 * @property nodeName 相关节点名（可空） / Related node name (nullable)
 * @property tensorName 相关张量名（可空） / Related tensor name (nullable)
 * @property shape 相关张量形状（可空） / Related tensor shape (nullable)
*/
sealed class OnnxError(
    override val errorCode: ErrorCode,
    override val message: String,
    open val nodeName: String? = null,
    open val tensorName: String? = null,
    open val shape: List<Long?>? = null
) : StructuredCoreError {

    /**
     * 拼接上下文后的完整消息 / Full message with context appended
     *
     * @return 含节点、张量与形状上下文的消息 / message including node, tensor and shape context
     */
    fun fullMessage(): String {
        val context = buildList {
            nodeName?.let { add("node=$it") }
            tensorName?.let { add("tensor=$it") }
            shape?.let { add("shape=$it") }
        }
        return if (context.isEmpty()) {
            message
        } else {
            "$message (${context.joinToString(", ")})"
        }
    }

    override fun toError(): Error<ErrorCode> {
        // 结构化错误本身作为关联值保留，调用方可取回节点、张量与形状上下文。
        // The structured error itself is carried as the associated value so callers can recover node, tensor and shape context.
        return ExErr(errorCode, fullMessage(), this)
    }

    /** 模型加载失败（IO、文件不存在、protobuf 解码异常）/ Model loading failed (IO, missing file, protobuf decode) */
    data class LoadFailed(
        val detail: String,
        override val tensorName: String? = null
    ) : OnnxError(
        errorCode = ErrorCode.DeserializationFailed,
        message = "ONNX 模型加载失败 / Failed to load ONNX model: $detail",
        tensorName = tensorName
    )

    /** 模型结构非法（缺图、节点输入不可得、重复输出名等）/ Invalid model structure (missing graph, unavailable node input, duplicated output name) */
    data class InvalidModel(
        val detail: String,
        override val nodeName: String? = null
    ) : OnnxError(
        errorCode = ErrorCode.IllegalArgument,
        message = "ONNX 模型结构非法 / Invalid ONNX model: $detail",
        nodeName = nodeName
    )

    /** 不支持的 opset / Unsupported opset */
    data class UnsupportedOpset(
        val domain: String,
        val version: Long,
        val supported: IntRange
    ) : OnnxError(
        errorCode = ErrorCode.IllegalArgument,
        message = "不支持的 ONNX opset / Unsupported ONNX opset: domain='$domain', version=$version, supported=$supported"
    )

    /** 不支持的算子 / Unsupported operator */
    data class UnsupportedOperator(
        val opType: String,
        val detail: String,
        override val nodeName: String? = null
    ) : OnnxError(
        errorCode = ErrorCode.IllegalArgument,
        message = "不支持的 ONNX 算子 / Unsupported ONNX operator '$opType': $detail",
        nodeName = nodeName
    )

    /** 不支持的数据类型 / Unsupported data type */
    data class UnsupportedDType(
        val dataType: String,
        override val tensorName: String? = null
    ) : OnnxError(
        errorCode = ErrorCode.IllegalArgument,
        message = "不支持的 ONNX 数据类型 / Unsupported ONNX data type: $dataType",
        tensorName = tensorName
    )

    /** 动态形状或未知维度 / Dynamic shape or unknown dimension */
    data class DynamicShape(
        val detail: String,
        override val tensorName: String? = null,
        override val shape: List<Long?>? = null
    ) : OnnxError(
        errorCode = ErrorCode.IllegalArgument,
        message = "ONNX 张量形状必须是全静态的 / ONNX tensor shape must be fully static: $detail",
        tensorName = tensorName,
        shape = shape
    )

    /** 形状不匹配 / Shape mismatch */
    data class ShapeMismatch(
        val detail: String,
        override val nodeName: String? = null,
        override val tensorName: String? = null,
        override val shape: List<Long?>? = null
    ) : OnnxError(
        errorCode = ErrorCode.IllegalArgument,
        message = "ONNX 张量形状不匹配 / ONNX tensor shape mismatch: $detail",
        nodeName = nodeName,
        tensorName = tensorName,
        shape = shape
    )

    /** 输入绑定不匹配 / Input binding mismatch */
    data class BindingMismatch(
        val detail: String,
        override val tensorName: String? = null
    ) : OnnxError(
        errorCode = ErrorCode.IllegalArgument,
        message = "ONNX 输入绑定不匹配 / ONNX input binding mismatch: $detail",
        tensorName = tensorName
    )

    /** 输出选择非法 / Invalid output selection */
    data class OutputSelection(
        val detail: String,
        override val tensorName: String? = null
    ) : OnnxError(
        errorCode = ErrorCode.IllegalArgument,
        message = "ONNX 输出选择非法 / Invalid ONNX output selection: $detail",
        tensorName = tensorName
    )

    /** 不支持的属性 / Unsupported attribute */
    data class UnsupportedAttribute(
        val attributeName: String,
        val detail: String,
        override val nodeName: String? = null
    ) : OnnxError(
        errorCode = ErrorCode.IllegalArgument,
        message = "不支持的 ONNX 属性 / Unsupported ONNX attribute '$attributeName': $detail",
        nodeName = nodeName
    )

    /** 表达式次数超过二次 / Expression degree exceeds quadratic */
    data class DegreeOverflow(
        val detail: String,
        override val nodeName: String? = null
    ) : OnnxError(
        errorCode = ErrorCode.IllegalArgument,
        message = "ONNX 表达式次数超限 / ONNX expression degree overflow: $detail",
        nodeName = nodeName
    )

    /** 缺少有限范围，无法线性化 / Missing finite bounds required for linearization */
    data class MissingBound(
        val detail: String,
        override val nodeName: String? = null,
        override val tensorName: String? = null
    ) : OnnxError(
        errorCode = ErrorCode.IllegalArgument,
        message = "缺少有限输入范围 / Missing finite input bounds: $detail",
        nodeName = nodeName,
        tensorName = tensorName
    )

    /** 推理失败 / Inference failed */
    data class InferenceFailed(
        val detail: String
    ) : OnnxError(
        errorCode = ErrorCode.ApplicationFailed,
        message = "ONNX 推理失败 / ONNX inference failed: $detail"
    )

    /** 资源已关闭 / Resource already closed */
    data class ResourceClosed(
        val detail: String
    ) : OnnxError(
        errorCode = ErrorCode.ApplicationError,
        message = "ONNX 资源已关闭 / ONNX resource already closed: $detail"
    )
}
