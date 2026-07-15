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
 *
 * @param model 线性三元模型 / The linear triad model
 * @param solver 线性求解器 / The linear solver
 * @return 约束到对偶值的映射，或错误 / Mapping of constraints to dual values, or error
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
 *
 * @param model 线性三元模型视图 / The linear triad model view
 * @param solver 线性求解器 / The linear solver
 * @return 约束到 Farkas 对偶值的映射，或错误 / Mapping of constraints to Farkas dual values, or error
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
 *
 * @param model 二次四元模型 / The quadratic tetrad model
 * @param solver 二次求解器 / The quadratic solver
 * @return 约束到对偶值的映射，或错误 / Mapping of constraints to dual values, or error
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
 *
 * @param model 二次四元模型视图 / The quadratic tetrad model view
 * @param solver 二次求解器 / The quadratic solver
 * @return 约束到 Farkas 对偶值的映射，或错误 / Mapping of constraints to Farkas dual values, or error
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
