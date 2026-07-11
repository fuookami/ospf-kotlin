/**
 * Gantt Scheduling 领域错误类型
 * Gantt Scheduling domain error types
 *
 * 定义 Gantt Scheduling 领域的语义化错误类型，替代裸 ErrorCode + message 构造。
 * Defines semantic error types for the Gantt Scheduling domain, replacing bare ErrorCode + message constructions.
*/
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.error

import fuookami.ospf.kotlin.utils.error.*

/**
 * Gantt Scheduling 能力不支持错误
 * Gantt Scheduling capability not supported error
 *
 * 用于 Gantt Scheduling 领域中不支持的能力错误。
 * Used for capability-not-supported errors in the Gantt Scheduling domain.
 *
 * @property capability 不支持的能力 / The unsupported capability
*/
class GanttSchedulingCapabilityError(
    val capability: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.IllegalArgument,
    message = if (capability != null) "Capability not supported: $capability" else "Capability not supported."
) {

    /** 工厂方法 / Factory methods */
    companion object {
        /**
         * 创建能力不支持错误
         * Create capability not supported error
         *
         * @param capability 不支持的能力 / The unsupported capability
         * @return GanttSchedulingCapabilityError 实例 / GanttSchedulingCapabilityError instance
        */
        fun of(capability: String): GanttSchedulingCapabilityError {
            return GanttSchedulingCapabilityError(capability)
        }

        /**
         * 创建通用能力不支持错误
         * Create generic capability not supported error
         *
         * @return GanttSchedulingCapabilityError 实例 / GanttSchedulingCapabilityError instance
        */
        fun unsupported(): GanttSchedulingCapabilityError {
            return GanttSchedulingCapabilityError()
        }
    }
}

/**
 * Gantt Scheduling 生命周期错误
 * Gantt Scheduling lifecycle error
 *
 * 用于 Gantt Scheduling 领域中生命周期相关的错误。
 * Used for lifecycle-related errors in the Gantt Scheduling domain.
 *
 * @property detail 错误详情 / Error detail
*/
class GanttSchedulingLifecycleError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.ApplicationError,
    message = detail ?: "Gantt Scheduling lifecycle error."
)

/**
 * Gantt Scheduling 求解错误
 * Gantt Scheduling solving error
 *
 * 用于 Gantt Scheduling 领域中求解相关的错误。
 * Used for solving-related errors in the Gantt Scheduling domain.
 *
 * @property detail 错误详情 / Error detail
*/
class GanttSchedulingSolvingError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.ApplicationFailed,
    message = detail ?: "Gantt Scheduling solving error."
)

/**
 * Gantt Scheduling 参数验证错误
 * Gantt Scheduling parameter validation error
 *
 * 用于 Gantt Scheduling 领域中的参数验证错误。
 * Used for parameter validation errors in the Gantt Scheduling domain.
 *
 * @property detail 错误详情 / Error detail
*/
class GanttSchedulingValidationError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.IllegalArgument,
    message = detail ?: "Gantt Scheduling validation error."
)
