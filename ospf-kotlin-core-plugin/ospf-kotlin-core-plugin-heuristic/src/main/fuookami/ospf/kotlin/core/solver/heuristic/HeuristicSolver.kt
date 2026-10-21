/**
 * 启发式求解器契约 / Heuristic solver contract
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.runner.AbstractRunner

/**
 * 启发式求解器契约，所有算法对外的统一入口。 / Heuristic solver contract, the unified external entry point of every algorithm.
 *
 * 算法名、结果结构与错误语义都由本接口固定，具体算法只提供 `runner`。 / The algorithm name, result shape, and error semantics are fixed here; a concrete algorithm supplies only a `runner`.
*/
interface HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {

    /** 算法名 / Algorithm name */
    val name: String

    /**
     * 求解模型 / Solve the model
     *
     * @param model 回调模型接口 / Callback model interface
     * @param iteration 迭代计数器 / Iteration counter
     * @return 求解结果 / Solve result
    */
    suspend operator fun invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration = Iteration()
    ): Ret<HeuristicResult<ObjValue, V>>
}

/**
 * 基于搜索骨架的求解器，把任意 [AbstractRunner] 适配为 [HeuristicSolver]。 / Runner-backed solver adapting any [AbstractRunner] into a [HeuristicSolver].
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property name 算法名 / Algorithm name
 * @property runner 搜索骨架 / Search skeleton
*/
class RunnerSolver<ObjValue, V>(
    override val name: String,
    val runner: AbstractRunner<*, ObjValue, V>
) : HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {

    override suspend operator fun invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): Ret<HeuristicResult<ObjValue, V>> {
        val individuals = runner.run(model = model, iteration = iteration)
        val best = individuals.firstOrNull()
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "the search produced no individual / 搜索未产生任何个体"
            )

        val status = when (model.constraintSatisfied(best.solution)) {
            false -> HeuristicSolutionStatus.Infeasible
            else -> HeuristicSolutionStatus.Feasible
        }
        return ok(
            HeuristicResult(
                bestSolution = best.solution,
                bestObjective = best.fitness,
                status = status,
                iteration = iteration
            )
        )
    }
}
