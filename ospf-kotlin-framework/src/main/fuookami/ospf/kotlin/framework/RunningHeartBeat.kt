@file:OptIn(kotlin.time.ExperimentalTime::class)

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

data class SubProgressHeartBeat(
    val estimatedTime: Duration,
    val progress: Flt64,
    val message: String? = null
)

data class RunningHeartBeat(
    val id: String,
    val runTime: Duration,
    val estimatedTime: Duration,
    val optimizedRate: Flt64,
    val progress: String? = null,
    val message: String? = null
) {
    @OptIn(ExperimentalTime::class)
    val time: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}

data class FinnishHeartBeat(
    val id: String,
    val runTime: Duration,
    val code: UInt64,
    val message: String
) {
    @OptIn(ExperimentalTime::class)
    val time: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    companion object {
        operator fun invoke(id: String, runTime: Duration): FinnishHeartBeat {
            return FinnishHeartBeat(
                id = id,
                runTime = runTime,
                code = UInt64.zero,
                message = ""
            )
        }

        operator fun invoke(id: String, runTime: Duration, error: Error<ErrorCode>): FinnishHeartBeat {
            return FinnishHeartBeat(
                id = id,
                runTime = runTime,
                code = UInt64(error.code.toULong()),
                message = error.message
            )
        }

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

