package fuookami.ospf.kotlin.core.backend.solver.output

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*

data class SolvingStatus(
    val solver: String,
    val solverIndex: UInt64 = UInt64.zero,
    val solverConfig: SolverConfig,
    val intermediateModel: ModelView<*, *>? = null,
    val solverModel: Any? = null,
    val solverCallBack: Any? = null,
    val objectCategory: ObjectCategory?,
    val time: Duration,
    val obj: Flt64,
    val possibleBestObj: Flt64,
    val initialBestObj: Flt64,
    val gap: Flt64,
    val currentBestSolution: List<Flt64>? = null
)

typealias SolvingStatusCallBack = (SolvingStatus) -> Try
