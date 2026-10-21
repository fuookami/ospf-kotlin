/**
 * 轨迹级搜索 / Trajectory-level search
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.Archive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.Initializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.ModelInitializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.FitnessEvaluator
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.DefaultOnMissFitnessEvaluator
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.toIndividual
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.Neighborhood
import fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance.Acceptance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance.BetterOnlyAcceptance

/**
 * 轨迹级搜索模板，一步 = 邻域候选 → 接受判据。 / Trajectory-level search template where one step is neighbourhood candidate → acceptance.
 *
 * 适用于单解搜索（模拟退火、爬山、禁忌搜索、变邻域）：状态始终是一个个体。
 * 初始解由 [initializer] 从模型产生，因此求解器可以在拿到模型之前就完成组装。 / Fits single-solution search (simulated annealing, hill climbing, tabu search, variable neighbourhood): the state is always one individual.
 * The initial solution is produced from the model by [initializer], so the solver can be assembled before the model is known.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property initializer 初始解算子 / Initial solution operator
 * @property neighborhood 邻域算子 / Neighbourhood operator
 * @property acceptance 接受判据 / Acceptance criterion
 * @property fitnessEvaluator 适应度评估算子 / Fitness evaluation operator
*/
class TrajectoryRunner<ObjValue, V>(
    val initializer: Initializer = ModelInitializer,
    val neighborhood: Neighborhood,
    val acceptance: Acceptance = BetterOnlyAcceptance,
    val fitnessEvaluator: FitnessEvaluator = DefaultOnMissFitnessEvaluator,
    private val entropy: Generator<Flt64>,
    archive: Archive<ObjValue, V>,
    stopping: StoppingCriterion,
    converter: IntoValue<V>
) : AbstractRunner<Individual<ObjValue, V>?, ObjValue, V>(archive, stopping, converter)
    where V : RealNumber<V>, V : NumberField<V> {

    override suspend fun initialize(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): Individual<ObjValue, V>? {
        val solution = initializer(
            iteration = iteration,
            model = model,
            amount = UInt64.one,
            converter = converter
        ).firstOrNull() ?: return null
        return fitnessEvaluator.toIndividual(model, solution)
    }

    override suspend fun advance(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        state: Individual<ObjValue, V>?
    ): Individual<ObjValue, V>? {
        val current = state ?: return null
        val neighbourSolution = neighborhood(
            iteration = iteration,
            individual = current,
            model = model,
            entropy = entropy,
            converter = converter
        )
        val candidate = fitnessEvaluator.toIndividual(model, neighbourSolution) ?: return current
        return if (acceptance(model = model, iteration = iteration, current = current, candidate = candidate)) {
            candidate
        } else {
            current
        }
    }

    override fun membersOf(state: Individual<ObjValue, V>?): List<Individual<ObjValue, V>> {
        return listOfNotNull(state)
    }
}
