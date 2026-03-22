@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.backend.solver.iis

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.UInt64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class IISConfig(
    val time: Duration = 30.seconds,
    val threadNum: UInt64 = if (Runtime.getRuntime().availableProcessors() <= 16) {
        UInt64(Runtime.getRuntime().availableProcessors())
    } else if (Runtime.getRuntime().availableProcessors() < 24) {
        UInt64(16)
    } else if (Runtime.getRuntime().availableProcessors() < 32) {
        UInt64(24)
    } else {
        UInt64(32)
    },
    val notImprovementTime: Duration? = null,
    val computingStatusCallBack: IISComputingStatusCallBack? = null,
    val slackTolerance: Flt64 = Flt64(1e-6),
    val extraConfig: Any? = null
)
