/**
 * Memetic 组合求解器 / Memetic composed solver
*/
package fuookami.ospf.kotlin.core.solver.heuristic.solver

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.compareStrictly
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.BestArchive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.FixedInitializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.RandomPerturbation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.Neighborhood
import fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance.Acceptance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance.NotWorseAcceptance
import fuookami.ospf.kotlin.core.solver.heuristic.runner.TrajectoryRunner

/**
 * Memetic 求解器：全局搜索 + 局部精修。 / Memetic solver: global search plus local refinement.
 *
 * Memetic 的字面意思是「模因算法」，本质上是**把局部搜索当作个体的一次学习**。这里用两阶段实现： /
 * "Memetic" means meme algorithm; in essence it treats **local search as an individual learning step**. Here it is implemented in
 * two phases:
 *
 * 1. 全局阶段跑一个现有求解器，得到一个好解；
 *    the global phase runs an existing solver and produces a good solution;
 * 2. 局部阶段把这个解当作初始点，用轨迹骨架在其邻域内精修。
 *    the local phase takes that solution as the starting point and refines it with a trajectory skeleton.
 *
 * 两阶段共用同一套算子，因此它本身不引入任何新的搜索机制——只是**调度**。
 * 这也让「hyper-heuristic」有了最朴素的一种形态：先跑谁、再跑谁，由组合决定。 /
 * Both phases share the same operator set, so no new search mechanism is introduced: it is only **scheduling**. This also gives
 * the simplest form of a hyper-heuristic: who runs first and who runs next is decided purely by composition.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property name 算法名 / Algorithm name
 * @property global 全局阶段求解器 / Global-phase solver
 * @property polish 局部阶段的构造器，输入是全局阶段的最优解 / Builder of the local phase, taking the global optimum as input
*/
class MemeticSolver<ObjValue, V>(
    override val name: String,
    private val global: HeuristicSolver<ObjValue, V>,
    private val polish: (List<Double>) -> HeuristicSolver<ObjValue, V>
) : HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {

    override suspend operator fun invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): Ret<HeuristicResult<ObjValue, V>> {
        val globalResult = global(model = model, iteration = iteration)
        if (globalResult !is Ok) {
            return globalResult
        }

        val best = globalResult.value.bestSolution ?: return globalResult
        val values = best.map { it.toFlt64().toDouble() }
        val polishResult = polish(values)(model = model)

        return when {
            polishResult !is Ok -> globalResult
            isBetter(model, polishResult.value.bestObjective, globalResult.value.bestObjective) -> polishResult
            else -> globalResult
        }
    }

    /**
     * 判断局部阶段是否真的改善了结果 / Decide whether the local phase actually improved the result
     *
     * @param model 回调模型接口 / Callback model interface
     * @param candidate 局部阶段的目标值 / Objective of the local phase
     * @param incumbent 全局阶段的目标值 / Objective of the global phase
     * @return 是否更优 / Whether it is better
    */
    private fun isBetter(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        candidate: ObjValue?,
        incumbent: ObjValue?
    ): Boolean {
        if (candidate == null) {
            return false
        }
        if (incumbent == null) {
            return true
        }
        return compareStrictly(model, candidate, incumbent) < 0
    }
}

/**
 * Memetic 求解器工厂。 / Memetic solver factory.
*/
data object MemeticSolvers {

    /**
     * 组合「全局种群搜索 + 局部轨迹精修」。 / Compose "global population search plus local trajectory refinement".
     *
     * 局部阶段默认用「不劣即接受」的判据：它的目标是在已找到的盆地里继续下降，
     * 而不是重新开始探索，因此不应该允许明显变差的移动。 /
     * The local phase defaults to a not-worse acceptance criterion: its job is to keep descending inside the basin already found,
     * not to restart exploration, so clearly worsening moves should not be allowed.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param global 全局阶段求解器 / Global-phase solver
     * @param polishStopping 局部阶段的终止判据 / Stopping criterion of the local phase
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param neighborhood 邻域算子 / Neighbourhood operator
     * @param acceptance 接受判据 / Acceptance criterion
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> memetic(
        global: HeuristicSolver<ObjValue, V>,
        polishStopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        neighborhood: Neighborhood = RandomPerturbation(1),
        acceptance: Acceptance = NotWorseAcceptance
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return MemeticSolver(
            name = "memetic-" + global.name,
            global = global,
            polish = { values ->
                RunnerSolver(
                    name = "memetic-polish",
                    runner = TrajectoryRunner<ObjValue, V>(
                        initializer = FixedInitializer(values),
                        neighborhood = neighborhood,
                        acceptance = acceptance,
                        entropy = entropy,
                        archive = BestArchive(1),
                        stopping = polishStopping,
                        converter = converter
                    )
                )
            }
        )
    }
}
