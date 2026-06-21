/**
 * CSP1D 领域错误类型
 * CSP1D domain error types
 *
 * 定义 CSP1D 领域的语义化错误类型，替代裸 ErrorCode + message 构造。
 * Defines semantic error types for the CSP1D domain, replacing bare ErrorCode + message constructions.
 */
package fuookami.ospf.kotlin.framework.csp1d.domain.material.error

import fuookami.ospf.kotlin.utils.error.*

/**
 * CSP1D 生命周期错误
 * CSP1D lifecycle error
 *
 * 用于 CSP1D 应用生命周期相关的错误（注册、恢复、上下文等）。
 * Used for errors related to the CSP1D application lifecycle (registration, recovery, context, etc.).
 *
 * @property detail 错误详情 / Error detail
 */
class Csp1dLifecycleError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.ApplicationError,
    message = detail ?: "CSP1D lifecycle error."
)

/**
 * CSP1D 类型错误
 * CSP1D type error
 *
 * 用于 CSP1D 领域中不支持的类型错误。
 * Used for unsupported type errors in the CSP1D domain.
 *
 * @property type 不支持的类型 / The unsupported type
 */
class Csp1dTypeError(
    val type: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.IllegalArgument,
    message = if (type != null) "Unsupported type: $type" else "Unsupported type."
)

/**
 * CSP1D 求解错误
 * CSP1D solving error
 *
 * 用于 CSP1D 领域中求解相关的错误。
 * Used for solving-related errors in the CSP1D domain.
 *
 * @property detail 错误详情 / Error detail
 */
class Csp1dSolvingError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.ApplicationFailed,
    message = detail ?: "CSP1D solving error."
)

/**
 * CSP1D 能力不支持错误
 * CSP1D capability not supported error
 *
 * 用于 CSP1D 领域中不支持的能力错误。
 * Used for capability-not-supported errors in the CSP1D domain.
 *
 * @property capability 不支持的能力 / The unsupported capability
 */
class Csp1dCapabilityError(
    val capability: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.IllegalArgument,
    message = if (capability != null) "Capability not supported: $capability" else "Capability not supported."
)
