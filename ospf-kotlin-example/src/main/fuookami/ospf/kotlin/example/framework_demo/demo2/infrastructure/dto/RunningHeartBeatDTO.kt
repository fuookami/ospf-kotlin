@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.datetime.*
import kotlin.time.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * Data transfer object for a running solver's heartbeat signal.
 * 运行中求解器心跳信号的数据传输对象。
 *
 * @property runTime 求解器启动以来的已用时长 / the elapsed duration since the solver started
 * @property estimatedTime 预估剩余时长 / the estimated remaining duration
 * @property optimizedRate 当前优化进度比率 / the current optimization progress rate
*/
data class RunningHeartBeatDTO(
    val id: String,
    val runTime: Duration,
    val estimatedTime: Duration,
    val optimizedRate: Flt64
) {

    /** The timestamp when this heartbeat was created. / 创建此心跳时的时间戳。 */
    val time: LocalDateTime = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault())
}

/**
 * Data transfer object for a finished solver's heartbeat signal.
 * 已完成求解器心跳信号的数据传输对象。
 *
 * @property runTime 求解器运行的总时长 / the total duration the solver ran
 * @property code 完成代码（零表示成功） / the completion code (zero for success)
 * @property message 完成消息或错误描述 / the completion message or error description
*/
data class FinnishHeartBeatDTO(
    val id: String,
    val runTime: Duration,
    val code: UInt64,
    val message: String
) {

    /** The timestamp when this heartbeat was created. / 创建此心跳时的时间戳。 */
    val time: LocalDateTime = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault())

    companion object {
        operator fun invoke(
            id: String,
            runTime: Duration
        ): FinnishHeartBeatDTO {
            return FinnishHeartBeatDTO(
                id = id,
                runTime = runTime,
                code = UInt64.zero,
                message = ""
            )
        }

        operator fun invoke(
            id: String,
            runTime: Duration,
            error: Error<ErrorCode>
        ): FinnishHeartBeatDTO {
            return FinnishHeartBeatDTO(
                id = id,
                runTime = runTime,
                code = UInt64(error.code.ordinal),
                message = error.message
            )
        }

        operator fun invoke(
            id: String,
            error: Error<ErrorCode>
        ): FinnishHeartBeatDTO {
            return FinnishHeartBeatDTO(
                id = id,
                runTime = Duration.ZERO,
                code = UInt64(error.code.ordinal),
                message = error.message
            )
        }
    }
}

