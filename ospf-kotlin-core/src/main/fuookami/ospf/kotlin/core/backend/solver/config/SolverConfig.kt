package fuookami.ospf.kotlin.core.backend.solver.config

import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*

data class SolverConfig(
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
    val gap: Flt64 = Flt64.zero,
    val notImprovementTime: Duration? = null,
    val dumpMechanismModelConcurrent: Boolean? = null,
    val dumpMechanismModelBlocking: Boolean? = null,
    val dumpIntermediateModelConcurrent: Boolean? = null,
    val extraConfig: Any? = null
)
