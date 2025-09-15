package fuookami.ospf.kotlin.core.backend.solver.output

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*

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
