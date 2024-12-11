package fuookami.ospf.kotlin.core.backend.solver.config

import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*

data class SolverConfig(
    val time: Duration = 30.seconds,
    val threadNum: UInt64 = if (Runtime.getRuntime().availableProcessors() <= 16) {
        UInt64(Runtime.getRuntime().availableProcessors())
    } else {
        max(
            UInt64.two.pow(UInt64(Runtime.getRuntime().availableProcessors()).lg2()!!.toFlt64().ceil().toUInt64().toInt()),
            UInt64(32)
        )
    },
    val gap: Flt64 = Flt64.zero,
    val notImprovementTime: Duration? = null,
    val dumpMechanismModelConcurrent: Boolean? = null,
    val dumpIntermediateModelConcurrent: Boolean? = null,
    val extraConfig: Any? = null
)
