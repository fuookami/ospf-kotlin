package fuookami.ospf.kotlin.framework

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*

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
    val time: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}

data class FinnishHeartBeat(
    val id: String,
    val runTime: Duration,
    val code: UInt64,
    val message: String
) {
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

        operator fun invoke(id: String, runTime: Duration, error: Error): FinnishHeartBeat {
            return FinnishHeartBeat(
                id = id,
                runTime = runTime,
                code = error.code.toUInt64(),
                message = error.message
            )
        }

        operator fun invoke(id: String, error: Error): FinnishHeartBeat {
            return FinnishHeartBeat(
                id = id,
                runTime = Duration.ZERO,
                code = error.code.toUInt64(),
                message = error.message
            )
        }
    }
}