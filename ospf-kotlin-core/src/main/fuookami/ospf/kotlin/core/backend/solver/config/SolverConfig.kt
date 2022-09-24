package fuookami.ospf.kotlin.core.backend.solver.config

import fuookami.ospf.kotlin.utils.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class SolverConfig(
    val time: Duration,
    val threadNum: UInt64 = UInt64(Runtime.getRuntime().availableProcessors().toULong())
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
            threadNum: UInt64 = UInt64(Runtime.getRuntime().availableProcessors().toULong()),
            gap: Flt64 = Flt64.zero,
            extraConfig: Any? = null
        ) =
            LinearSolverConfig(SolverConfig(time, threadNum), gap, extraConfig)
    }
}
