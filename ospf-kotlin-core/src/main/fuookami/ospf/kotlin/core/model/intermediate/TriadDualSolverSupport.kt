/**
 * 三元/四元对偶求解支持
 * Triad/Tetrad dual solver support
 */
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.*

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
 * 三元 Farkas 对偶求解辅助函数。
 * Triad Farkas dual solver helpers.
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
 * 四元二次对偶求解辅助函数。
 * Quadratic dual solver helpers.
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
 * 四元二次 Farkas 对偶求解辅助函数。
 * Quadratic Farkas dual solver helpers.
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
