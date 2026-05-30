@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 运行心跳模型
 * Running Heartbeat Models
 *
 * 定义求解过程中的子进度、运行中和完成心跳数据结构。
 * Defines sub-progress, running, and finish heartbeat data structures during solving.
 */
package fuookami.ospf.kotlin.framework

import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * 子进度心跳
 * Sub-progress heartbeat
 *
 * @property estimatedTime 预估剩余时间 / Estimated remaining time
 * @property progress 子进度百分比 / Sub-progress percentage
 * @property message 附加消息，可为 null / Optional message, nullable
 */
data class SubProgressHeartBeat(
    val estimatedTime: Duration,
    val progress: Flt64,
    val message: String? = null
)

/**
 * 运行中心跳
 * Running heartbeat
 *
 * @property id 任务标识 / Task identifier
 * @property runTime 已运行时间 / Elapsed run time
 * @property estimatedTime 预估剩余时间 / Estimated remaining time
 * @property optimizedRate 优化率 / Optimization rate
 * @property progress 进度描述，可为 null / Progress description, nullable
 * @property message 附加消息，可为 null / Optional message, nullable
 */
data class RunningHeartBeat(
    val id: String,
    val runTime: Duration,
    val estimatedTime: Duration,
    val optimizedRate: Flt64,
    val progress: String? = null,
    val message: String? = null
) {
    /** 心跳时间戳 / Heartbeat timestamp */
    @OptIn(ExperimentalTime::class)
    val time: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}

/**
 * 完成心跳
 * Finish heartbeat
 *
 * @property id 任务标识 / Task identifier
 * @property runTime 总运行时间 / Total run time
 * @property code 状态码 / Status code
 * @property message 完成消息 / Finish message
 */
data class FinnishHeartBeat(
    val id: String,
    val runTime: Duration,
    val code: UInt64,
    val message: String
) {
    /** 心跳时间戳 / Heartbeat timestamp */
    @OptIn(ExperimentalTime::class)
    val time: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    companion object {
        /**
         * 构造成功完成心跳
         * Construct successful finish heartbeat
         *
         * @param id 任务标识 / Task identifier
         * @param runTime 总运行时间 / Total run time
         * @return 成功完成心跳 / Successful finish heartbeat
         */
        operator fun invoke(id: String, runTime: Duration): FinnishHeartBeat {
            return FinnishHeartBeat(
                id = id,
                runTime = runTime,
                code = UInt64.zero,
                message = ""
            )
        }

        /**
         * 构造带错误的完成心跳
         * Construct finish heartbeat with error
         *
         * @param id 任务标识 / Task identifier
         * @param runTime 总运行时间 / Total run time
         * @param error 错误信息 / Error information
         * @return 带错误的完成心跳 / Finish heartbeat with error
         */
        operator fun invoke(id: String, runTime: Duration, error: Error<ErrorCode>): FinnishHeartBeat {
            return FinnishHeartBeat(
                id = id,
                runTime = runTime,
                code = UInt64(error.code.toULong()),
                message = error.message
            )
        }

        /**
         * 构造带错误的完成心跳（零运行时间）
         * Construct finish heartbeat with error (zero run time)
         *
         * @param id 任务标识 / Task identifier
         * @param error 错误信息 / Error information
         * @return 带错误的完成心跳 / Finish heartbeat with error
         */
        operator fun invoke(id: String, error: Error<ErrorCode>): FinnishHeartBeat {
            return FinnishHeartBeat(
                id = id,
                runTime = Duration.ZERO,
                code = UInt64(error.code.toULong()),
                message = error.message
            )
        }
    }
}
