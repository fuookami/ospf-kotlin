package fuookami.ospf.kotlin.core.backend.solver.config

import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.utils.math.*

data class SolverConfig(
    val time: Duration = 30.seconds,
    val threadNum: UInt64 = UInt64(Runtime.getRuntime().availableProcessors()),
    val gap: Flt64 = Flt64.zero,
    val notImprovementTime: Duration? = null,
    val dumpMechanismModelConcurrent: Boolean? = null,
    val dumpIntermediateModelConcurrent: Boolean? = null,
    val extraConfig: Any? = null
)
