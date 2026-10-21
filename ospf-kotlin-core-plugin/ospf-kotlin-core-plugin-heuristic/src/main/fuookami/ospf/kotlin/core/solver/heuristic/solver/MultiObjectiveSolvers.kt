/**
 * 多目标求解器工厂 / Multi-objective solver factories
*/
package fuookami.ospf.kotlin.core.solver.heuristic.solver

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.Cross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.Mutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SolutionUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.Dominance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SimulatedBinaryCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.PolynomialMutation
import fuookami.ospf.kotlin.core.solver.heuristic.runner.Nsga2Runner
import fuookami.ospf.kotlin.core.solver.heuristic.runner.Nsga3Runner
import fuookami.ospf.kotlin.core.solver.heuristic.runner.MultiObjectiveResult
import fuookami.ospf.kotlin.core.solver.heuristic.runner.MultiObjectiveSolver

/**
 * 基于 [Nsga2Runner] 的多目标求解器。 / Multi-objective solver backed by [Nsga2Runner].
 *
 * @param V 值类型 / Value type
 * @property runner NSGA-II 骨架 / NSGA-II skeleton
*/
class Nsga2Solver<V>(
    val runner: Nsga2Runner<V>
) : MultiObjectiveSolver<V> where V : RealNumber<V>, V : NumberField<V> {

    override val name: String get() = runner.name

    override suspend operator fun invoke(
        model: AbstractCallBackModelInterface<*, *, V>
    ): Ret<MultiObjectiveResult<V>> {
        return runner.run(model)
    }
}

/**
 * 多目标求解器工厂。 / Multi-objective solver factories.
*/
data object MultiObjectiveSolvers {

    /**
     * 组合 NSGA-II。 / Compose NSGA-II.
     *
     * 交叉与变异默认取 MO 场景下的常用组合：SBX 交叉 + 多项式变异，它们的分布指数对实数编码的多目标问题更合适。 /
     * Crossover and mutation default to the usual MO pair: SBX plus polynomial mutation, whose distribution indices suit
     * real-coded multi-objective problems better.
     *
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param objectivesOf 目标向量提取器 / Objective vector extractor
     * @param dominance 支配判据 / Dominance predicate
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 解更新算子 / Solution update operator
     * @param mutation 变异算子 / Mutation operator
     * @param mutationRate 变异率 / Mutation rate
     * @param archiveAmount 前沿档案容量 / Front archive capacity
     * @return 多目标求解器 / Multi-objective solver
    */
    fun <V> nsga2(
        size: Int,
        objectivesOf: (Solution<V>) -> List<Flt64>,
        dominance: Dominance,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = SimulatedBinaryCross(),
        mutation: Mutation = PolynomialMutation(),
        mutationRate: Flt64 = Flt64(0.1),
        archiveAmount: Int = 100
    ): MultiObjectiveSolver<V> where V : RealNumber<V>, V : NumberField<V> {
        return Nsga2Solver(
            runner = Nsga2Runner(
                size = size,
                objectivesOf = objectivesOf,
                dominance = dominance,
                update = update,
                mutation = mutation,
                mutationRate = mutationRate,
                entropy = entropy,
                stopping = stopping,
                converter = converter,
                archiveAmount = archiveAmount
            )
        )
    }

    /**
     * 组合 NSGA-III。 / Compose NSGA-III.
     *
     * 参考点在 `divisions` 上做 Das–Dennis 构造：目标数 m、分割数 p 时参考点数量为 `C(p + m − 1, m − 1)`。
     * 目标数越多，`divisions` 应越小，否则参考点数量会急剧膨胀。 /
     * Reference points use the Das–Dennis construction over `divisions`: with `m` objectives and `p` divisions the count is
     * `C(p + m − 1, m − 1)`. The more objectives, the smaller `divisions` should be, otherwise the count explodes.
     *
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param objectivesOf 目标向量提取器 / Objective vector extractor
     * @param dominance 支配判据 / Dominance predicate
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 解更新算子 / Solution update operator
     * @param mutation 变异算子 / Mutation operator
     * @param mutationRate 变异率 / Mutation rate
     * @param archiveAmount 前沿档案容量 / Front archive capacity
     * @param divisions 参考点分割数 / Divisions of the reference points
     * @return 多目标求解器 / Multi-objective solver
    */
    fun <V> nsga3(
        size: Int,
        objectivesOf: (Solution<V>) -> List<Flt64>,
        dominance: Dominance,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = SimulatedBinaryCross(),
        mutation: Mutation = PolynomialMutation(),
        mutationRate: Flt64 = Flt64(0.1),
        archiveAmount: Int = 100,
        divisions: Int = 8
    ): MultiObjectiveSolver<V> where V : RealNumber<V>, V : NumberField<V> {
        return Nsga2Solver(
            runner = Nsga3Runner(
                size = size,
                objectivesOf = objectivesOf,
                dominance = dominance,
                update = update,
                mutation = mutation,
                mutationRate = mutationRate,
                entropy = entropy,
                stopping = stopping,
                converter = converter,
                archiveAmount = archiveAmount,
                divisions = divisions
            )
        )
    }
}
