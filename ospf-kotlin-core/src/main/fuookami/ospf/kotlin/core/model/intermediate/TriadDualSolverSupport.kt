/**
 * 三元/四元对偶求解支持
 * Triad/Tetrad dual solver support
 */
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Linear
import fuookami.ospf.kotlin.core.model.mechanism.Quadratic
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.core.solver.QuadraticSolver

/**
 * 求解线性三元模型的对偶问题。
 * Solve the dual problem of a linear triad model.
 */
suspend fun solveDual(
    model: LinearTriadModel,
    solver: LinearSolver
): Ret<kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>> {
    val dualModel = model.dual()

    return when (val result = solver(dualModel)) {
        is Ok -> Ok(dualModel.tidyDualSolution(result.value.solution))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * Triad Farkas dual solver helpers.
 * Triad FarkasDual 求解辅助函数。
 */
suspend fun solveFarkasDual(
    model: LinearTriadModelView,
    solver: LinearSolver
): Ret<kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>> {
    val dualModel = model.farkasDual()

    return when (val result = solver(dualModel)) {
        is Ok -> Ok(dualModel.tidyDualSolution(result.value.solution))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * Quadratic dual solver helpers.
 * Quadratic dual 求解辅助函数。
 */
suspend fun solveDual(
    model: QuadraticTetradModel,
    solver: QuadraticSolver
): Ret<kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>> {
    val dualModel = model.dual()

    return when (val result = solver(dualModel)) {
        is Ok -> Ok(dualModel.tidyDualSolution(result.value.solution))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * Quadratic Farkas dual solver helpers.
 * Quadratic FarkasDual 求解辅助函数。
 */
suspend fun solveFarkasDual(
    model: QuadraticTetradModelView,
    solver: QuadraticSolver
): Ret<kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>> {
    val dualModel = model.farkasDual()

    return when (val result = solver(dualModel)) {
        is Ok -> Ok(dualModel.tidyDualSolution(result.value.solution))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}
