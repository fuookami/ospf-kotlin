package fuookami.ospf.kotlin.core.backend.solver.output

import fuookami.ospf.kotlin.utils.math.Flt64
import kotlin.time.Duration

data class LinearSolverOutput(
    val obj: Flt64,
    val results: List<Flt64>,
    val time: Duration,
    val possibleBestObj: Flt64,
    val gap: Flt64
)
