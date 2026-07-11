/**
 * BPP3D 领域错误类型
 * BPP3D domain error types
 *
 * 定义 BPP3D 领域的语义化错误类型，替代裸 ErrorCode + message 构造。
 * Defines semantic error types for the BPP3D domain, replacing bare ErrorCode + message constructions.
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.error

import fuookami.ospf.kotlin.utils.error.*

/**
 * BPP3D 能力不支持错误
 * BPP3D capability not supported error
 *
 * 用于 BPP3D 领域中不支持的能力错误。
 * Used for capability-not-supported errors in the BPP3D domain.
 *
 * @property capability 不支持的能力 / The unsupported capability
*/
class Bpp3dCapabilityError(
    val capability: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.IllegalArgument,
    message = if (capability != null) "Capability not supported: $capability" else "Capability not supported."
)

/**
 * BPP3D 求解错误
 * BPP3D solving error
 *
 * 用于 BPP3D 领域中求解相关的错误。
 * Used for solving-related errors in the BPP3D domain.
 *
 * @property detail 错误详情 / Error detail
*/
class Bpp3dSolvingError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.ApplicationFailed,
    message = detail ?: "BPP3D solving error."
)

/**
 * BPP3D 内部错误
 * BPP3D internal error
 *
 * 用于 BPP3D 领域中的内部逻辑错误。
 * Used for internal logic errors in the BPP3D domain.
 *
 * @property detail 错误详情 / Error detail
*/
class Bpp3dInternalError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.ApplicationError,
    message = detail ?: "BPP3D internal error."
)

/**
 * BPP3D 参数验证错误
 * BPP3D parameter validation error
 *
 * 用于 BPP3D 领域中的参数验证错误。
 * Used for parameter validation errors in the BPP3D domain.
 *
 * @property detail 错误详情 / Error detail
*/
class Bpp3dValidationError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.IllegalArgument,
    message = detail ?: "BPP3D validation error."
)
