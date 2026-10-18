/**
 * 二维装箱错误类型。 / 2D packing error types.
 *
 * 与 BPP3D 的 `Bpp3dErrors` 对齐：用语义化错误子类型替代裸 `ErrorCode + message` 构造。
 * / Mirrors BPP3D `Bpp3dErrors`: semantic error subtypes replace bare `ErrorCode + message` constructions.
*/
package fuookami.ospf.kotlin.framework.bpp2d.infrastructure

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.Err

/**
 * 二维装箱校验错误：输入数据、配置或几何关系不合法。 / 2D packing validation error: invalid input data, configuration or geometry.
 *
 * @property detail 错误详情，为空时使用默认消息 / error detail, defaulting when null
*/
class Bpp2dValidationError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.IllegalArgument,
    message = detail ?: "BPP2D validation error."
)

/**
 * 二维装箱求解错误：模型注册、求解或结果提取失败。 / 2D packing solving error: model registration, solving or extraction failed.
 *
 * @property detail 错误详情，为空时使用默认消息 / error detail, defaulting when null
*/
class Bpp2dSolvingError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.ApplicationFailed,
    message = detail ?: "BPP2D solving error."
)

/**
 * 二维装箱能力不支持错误：请求了当前路径未覆盖的形状或排样方式。 / 2D packing capability error: an unsupported shape or layout path was requested.
 *
 * @property capability 不支持的能力 / the unsupported capability
*/
class Bpp2dCapabilityError(
    val capability: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.IllegalArgument,
    message = if (capability != null) "Capability not supported: $capability" else "Capability not supported."
)

/**
 * 二维装箱内部错误：不应到达的分支或状态。 / 2D packing internal error: an unreachable branch or state.
 *
 * @property detail 错误详情，为空时使用默认消息 / error detail, defaulting when null
*/
class Bpp2dInternalError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.ApplicationError,
    message = detail ?: "BPP2D internal error."
)