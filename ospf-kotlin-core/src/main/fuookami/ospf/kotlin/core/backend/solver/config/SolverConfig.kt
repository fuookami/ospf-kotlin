package fuookami.ospf.kotlin.core.backend.solver.config

import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.utils.math.*

data class SolverConfig(
    val time: Duration,
    val threadNum: UInt64 = UInt64(Runtime.getRuntime().availableProcessors())
)

data class LinearSolverConfig(
    val basicConfig: SolverConfig,
    val gap: Flt64 = Flt64.zero,
    val extraConfig: Any? = null
) {
    val time: Duration by basicConfig::time
    val threadNum: UInt64 by basicConfig::threadNum

    companion object {
        operator fun invoke(
            time: Duration = 30.seconds,
            threadNum: UInt64 = UInt64(Runtime.getRuntime().availableProcessors()),
            gap: Flt64 = Flt64.zero,
            extraConfig: Any? = null
        ) =
            LinearSolverConfig(SolverConfig(time, threadNum), gap, extraConfig)
    }
}
