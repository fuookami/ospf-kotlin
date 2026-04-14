@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.output

import fuookami.ospf.kotlin.core.intermediate_model.BasicLinearTriadModelView
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.model.Solution
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlin.time.Duration

sealed interface SolverOutput {}
sealed interface UnifiedSolverOutput : SolverOutput {
    val iterations: UInt64?
    val nodeCount: UInt64?
    val bestBound: Flt64?
    val mipGap: Flt64?
    val solveTime: Duration?
}
sealed interface LinearSolverOutput : SolverOutput {}
sealed interface QuadraticSolverOutput : SolverOutput {}

data class FeasibleSolverOutput(
    val obj: Flt64,
    val solution: Solution,
    val time: Duration,
    val possibleBestObj: Flt64,
    val gap: Flt64,
    override val iterations: UInt64? = null,
    override val nodeCount: UInt64? = null,
    override val bestBound: Flt64? = null,
    override val mipGap: Flt64 = gap,
    override val solveTime: Duration = time
) : LinearSolverOutput, QuadraticSolverOutput, UnifiedSolverOutput

data class LinearInfeasibleSolverOutput(
    val iis: BasicLinearTriadModelView,
    override val iterations: UInt64? = null,
    override val nodeCount: UInt64? = null,
    override val bestBound: Flt64? = null,
    override val mipGap: Flt64? = null,
    override val solveTime: Duration? = null
) : LinearSolverOutput, UnifiedSolverOutput

data class QuadraticInfeasibleSolverOutput(
    val iis: QuadraticTetradModelView,
    override val iterations: UInt64? = null,
    override val nodeCount: UInt64? = null,
    override val bestBound: Flt64? = null,
    override val mipGap: Flt64? = null,
    override val solveTime: Duration? = null
) : QuadraticSolverOutput, UnifiedSolverOutput



