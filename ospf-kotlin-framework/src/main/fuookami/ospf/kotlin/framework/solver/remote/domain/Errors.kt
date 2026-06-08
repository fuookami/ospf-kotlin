/**
 * 远程求解错误模型
 * Remote solve error models
 */
package fuookami.ospf.kotlin.framework.solver.remote.domain

/**
 * 远程求解错误码。
 * Remote solve error code.
 */
enum class RemoteSolverErrorCode {
    /** 参数无效 / Invalid argument */
    INVALID_ARGUMENT,

    /** 任务状态转换无效 / Invalid task state transition */
    INVALID_TASK_STATE_TRANSITION,

    /** 无可用节点 / No eligible node */
    NO_ELIGIBLE_NODE_AVAILABLE,

    /** 节点离线 / Node offline */
    NODE_OFFLINE,

    /** 求解执行失败 / Solver execution failed */
    SOLVER_EXECUTION_FAILED,

    /** 检查点导出失败 / Checkpoint export failed */
    CHECKPOINT_EXPORT_FAILED,

    /** 事件发布失败 / Event publish failed */
    EVENT_PUBLISH_FAILED,

    /** 存储 IO 失败 / Storage I/O failed */
    STORAGE_IO_FAILED,

    /** 任务未在最大轮数内终止 / Task not terminal within max rounds */
    TASK_NOT_TERMINAL_WITHIN_MAX_ROUNDS,

    /** 无兼容节点 / No compatible node */
    NO_COMPATIBLE_NODE_AVAILABLE,

    /** 任务失败 / Task failed */
    TASK_FAILED,

    /** 任务硬超时 / Task hard timeout */
    TASK_FAILED_HARD_TIMEOUT,

    /** 切片超时 / Slice timeout */
    TASK_FAILED_SLICE_TIMEOUT,

    /** 预算超限 / Budget exceeded */
    TASK_FAILED_BUDGET_EXCEEDED,

    /** 远程求解未在最大轮数内完成 / Remote solve did not complete within max rounds */
    REMOTE_SOLVE_NOT_COMPLETED_WITHIN_MAX_ROUNDS,

    /** 内部错误 / Internal error */
    INTERNAL_ERROR
}

/**
 * 远程求解异常。
 * Remote solve exception.
 *
 * @property code 错误码 / Error code
 * @property metadata 元数据 / Metadata
 */
class RemoteSolverException(
    val code: RemoteSolverErrorCode,
    override val message: String,
    val metadata: Map<String, String> = emptyMap(),
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 远程求解错误映射器。
 * Remote solve error mapper.
 */
object RemoteSolverErrorMapper {
    /**
     * 归一化异常。
     * Normalize exception.
     *
     * @param throwable 原始异常 / Original throwable
     * @return 远程求解异常 / Remote solve exception
     */
    fun normalize(throwable: Throwable): RemoteSolverException {
        if (throwable is RemoteSolverException) {
            return throwable
        }
        if (throwable is IllegalArgumentException) {
            return RemoteSolverException(
                code = RemoteSolverErrorCode.INVALID_ARGUMENT,
                message = throwable.message ?: "Invalid argument",
                cause = throwable
            )
        }
        return RemoteSolverException(
            code = RemoteSolverErrorCode.INTERNAL_ERROR,
            message = throwable.message ?: "Internal error",
            cause = throwable
        )
    }

    /**
     * 获取 API 错误码。
     * Get API error code.
     *
     * @param throwable 异常 / Throwable
     * @return API 错误码 / API error code
     */
    fun apiCodeOf(throwable: Throwable): String = normalize(throwable).code.name

    /**
     * 获取原因码。
     * Get reason code.
     *
     * @param code 错误码 / Error code
     * @return 原因码 / Reason code
     */
    fun reasonCodeOf(code: RemoteSolverErrorCode): String = code.name
}
