package fuookami.ospf.kotlin.core.backend.solver.output

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*

data class LinearSolverOutput(
    val obj: Flt64,
    val results: List<Flt64>,
    val time: Duration,
    val possibleBestObj: Flt64,
    val gap: Flt64
)
