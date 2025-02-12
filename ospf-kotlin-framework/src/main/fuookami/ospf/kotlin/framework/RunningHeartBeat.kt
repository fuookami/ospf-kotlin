package fuookami.ospf.kotlin.framework

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*

data class RunningHeartBeat(
    val id: String,
    val runTime: Duration,
    val estimatedTime: Duration,
    val optimizedRate: Flt64
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
            return FinnishHeartBeat(id, runTime, UInt64.zero, "")
        }

        operator fun invoke(id: String, runTime: Duration, error: Error): FinnishHeartBeat {
            return FinnishHeartBeat(id, runTime, error.code.toUInt64(), error.message)
        }

        operator fun invoke(id: String, error: Error): FinnishHeartBeat {
            return FinnishHeartBeat(id, Duration.ZERO, error.code.toUInt64(), error.message)
        }
    }
}
