@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.backend.solver.output

import fuookami.ospf.kotlin.core.backend.intermediate_model.BasicLinearTriadModelView
import fuookami.ospf.kotlin.core.backend.intermediate_model.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import kotlin.time.Duration

sealed interface SolverOutput {}
sealed interface LinearSolverOutput : SolverOutput {}
sealed interface QuadraticSolverOutput : SolverOutput {}

data class FeasibleSolverOutput(
    val obj: Flt64,
    val solution: Solution,
    val time: Duration,
    val possibleBestObj: Flt64,
    val gap: Flt64
) : LinearSolverOutput, QuadraticSolverOutput

data class LinearInfeasibleSolverOutput(
    val iis: BasicLinearTriadModelView
) : LinearSolverOutput

data class QuadraticInfeasibleSolverOutput(
    val iis: QuadraticTetradModelView
) : QuadraticSolverOutput



