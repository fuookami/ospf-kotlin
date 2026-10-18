/**
 * 二维套裁错误类型。 / 2D cutting stock error types.
 *
 * 与 BPP2D/BPP3D 的做法一致：用语义化错误子类型替代裸 `ErrorCode + message` 构造。
 * / Consistent with BPP2D/BPP3D: semantic error subtypes replace bare `ErrorCode + message` constructions.
*/
package fuookami.ospf.kotlin.framework.csp2d.infrastructure

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.Err

/**
 * 二维套裁校验错误：输入数据、配置、几何或切分树不合法。 / 2D cutting stock validation error: invalid input data,
 * configuration, geometry or cut tree.
 *
 * @property detail 错误详情，为空时使用默认消息 / error detail, defaulting when null
*/
class Csp2dValidationError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.IllegalArgument,
    message = detail ?: "CSP2D validation error."
)

/**
 * 二维套裁求解错误：模型注册、求解或结果提取失败。 / 2D cutting stock solving error: model registration, solving or
 * extraction failed.
 *
 * @property detail 错误详情，为空时使用默认消息 / error detail, defaulting when null
*/
class Csp2dSolvingError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.ApplicationFailed,
    message = detail ?: "CSP2D solving error."
)

/**
 * 二维套裁能力不支持错误：请求了当前路径未覆盖的工艺、模式或形状。 / 2D cutting stock capability error: an unsupported
 * process, mode or shape was requested.
 *
 * @property capability 不支持的能力 / the unsupported capability
*/
class Csp2dCapabilityError(
    val capability: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.IllegalArgument,
    message = if (capability != null) "Capability not supported: $capability" else "Capability not supported."
)

/**
 * 二维套裁内部错误：不应到达的分支或状态。 / 2D cutting stock internal error: an unreachable branch or state.
 *
 * @property detail 错误详情，为空时使用默认消息 / error detail, defaulting when null
*/
class Csp2dInternalError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.ApplicationError,
    message = detail ?: "CSP2D internal error."
)
