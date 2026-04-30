@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.output

import fuookami.ospf.kotlin.core.model.basic.ModelView
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlin.time.Duration

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
    val currentBestSolution: List<Flt64>? = null,
    val iterations: UInt64? = null,
    val nodeCount: UInt64? = null,
    val bestBound: Flt64? = null,
    val mipGap: Flt64 = gap,
    val solveTime: Duration = time
)

typealias SolvingStatusCallBack = (SolvingStatus) -> Try



