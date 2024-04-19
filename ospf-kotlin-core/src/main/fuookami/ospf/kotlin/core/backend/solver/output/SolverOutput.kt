package fuookami.ospf.kotlin.core.backend.solver.output

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.model.*

data class SolverOutput(
    val obj: Flt64,
    val solution: Solution,
    val time: Duration,
    val possibleBestObj: Flt64,
    val gap: Flt64
)
