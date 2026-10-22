package fuookami.ospf.kotlin.core.solver.heuristic.operator.update

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.runner.SearchEvaluation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.Mutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.Neighborhood

/** 将变异作为可串联更新阶段 / Adapt mutation into a composable update stage.
 * @property mutation 变异算子 / Mutation operator
 * @property rate 变异概率 / Mutation probability
 */
class MutationUpdate(val mutation: Mutation, val rate: Flt64 = Flt64.one) : SolutionUpdate {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        return parents.map { mutation(
            iteration = iteration, individual = it, model = model, converter = converter, entropy = entropy, mutationRate = rate
        ) }
    }
}

/** 将轨迹邻域提升为种群动作，可再由 VND 或精英包装 / Lift a trajectory neighborhood into a population action, composable with VND or elite wrapping.
 * @property neighborhood 单点邻域 / Single-point neighborhood
 */
class NeighborhoodUpdate(val neighborhood: Neighborhood) : SolutionUpdate {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        return parents.map { neighborhood(
            iteration = iteration, individual = it, model = model, entropy = entropy, converter = converter
        ) }
    }
}

/** 有界空间的对立跳跃，仅在作为动作被选择时执行 / Opposition jump in a bounded space, executed only when selected as an action. */
object OppositionUpdate : SolutionUpdate {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        return parents.map { parent -> parent.solution.mapIndexed { index, value ->
            val lower = model.lowerBoundOf(index)
            val upper = model.upperBoundOf(index)
            if (lower == null || upper == null || !lower.isFinite() || !upper.isFinite()) value
            else clampTo(value = lower + (upper - value.asDouble()), lower = lower, upper = upper).asValue(converter)
        } }
    }
}

/** 有界 Hooke-Jeeves 坐标/模式精调，全部试探经模型预算入口 / Bounded Hooke-Jeeves polling/pattern intensification through metered evaluation.
 * @property initialStep 初始相对步长 / Initial relative step
 * @property contraction 无改善时的收缩率 / Contraction without improvement
 * @property sweeps 每次调用最多轮数 / Maximum sweeps per invocation
 */
class PatternSearchUpdate(
    val initialStep: Double = 0.1,
    val contraction: Double = 0.5,
    val sweeps: Int = 32
) : SolutionUpdate {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        return search(parents.firstOrNull()?.solution ?: return emptyList(), model, converter)
    }

    private fun <O, V> search(
        seed: Solution<V>,
        model: AbstractCallBackModelInterface<*, O, V>,
        converter: IntoValue<V>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V> {
        if (!initialStep.isFinite() || initialStep <= 0 || contraction <= 0 || contraction >= 1 || sweeps <= 0) return emptyList()
        var current = ObjectiveFitnessEvaluator.toIndividual(model, seed) ?: return emptyList()
        val mesh = seed.indices.map { index ->
            val width = (model.upperBoundOf(index) ?: Double.NaN) - (model.lowerBoundOf(index) ?: Double.NaN)
            val localScale = max(abs(seed[index].asDouble()), 1.0)
            initialStep * if (width.isFinite() && width > 0) min(width, localScale) else localScale
        }.toMutableList()
        repeat(sweeps) {
            val base = current
            for (index in seed.indices) {
                for (sign in listOf(1.0, -1.0)) {
                    if (model is SearchEvaluation && model.remaining <= 2) return listOf(current.solution)
                    val trial = current.solution.toMutableList()
                    trial[index] = clampTo(
                        value = current.solution[index].asDouble() + sign * mesh[index],
                        lower = model.lowerBoundOf(index),
                        upper = model.upperBoundOf(index)
                    ).asValue(converter)
                    if (trial == current.solution) continue
                    val evaluated = ObjectiveFitnessEvaluator.toIndividual(model, trial) ?: continue
                    if (compareIndividuals(
                        model = model,
                        lhs = evaluated,
                        rhs = current
                    ) < 0) current = evaluated
                }
            }
            if (current.solution == base.solution) {
                for (index in mesh.indices) mesh[index] *= contraction
            } else {
                val pattern = current.solution.indices.map { index -> clampTo(
                    value = 2 * current.solution[index].asDouble() - base.solution[index].asDouble(),
                    lower = model.lowerBoundOf(index),
                    upper = model.upperBoundOf(index)
                ).asValue(converter) }
                if (pattern != current.solution) {
                    if (model is SearchEvaluation && model.remaining <= 2) return listOf(current.solution)
                    val evaluated = ObjectiveFitnessEvaluator.toIndividual(model, pattern)
                    if (evaluated != null && compareIndividuals(
                        model = model,
                        lhs = evaluated,
                        rhs = current
                    ) < 0) current = evaluated
                }
            }
        }
        return listOf(current.solution)
    }
}

/** 流水线中间产物必须重新评估，禁止复用旧适应度 / Pipeline intermediates are re-evaluated, never assigned stale fitness.
 * @property stages 按顺序执行的更新 / Updates in execution order
 */
class PipelineCompositeUpdate(val stages: List<SolutionUpdate>) : SolutionUpdate {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        return pipeline(iteration, parents.map { it.solution }, model, converter, entropy)
    }

    private fun <O, V> pipeline(
        iteration: Iteration,
        seeds: List<Solution<V>>,
        model: AbstractCallBackModelInterface<*, O, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V> {
        var current = seeds.mapNotNull { ObjectiveFitnessEvaluator.toIndividual(model, it) }
        for (stage in stages) {
            if (current.isEmpty()) break
            val next = stage(
                iteration = iteration, parents = current, model = model, converter = converter, entropy = entropy
            )
            current = next.mapNotNull { ObjectiveFitnessEvaluator.toIndividual(model, it) }
                .sortedWith { a, b -> compareIndividuals(
                    model = model,
                    lhs = a,
                    rhs = b
                ) }
        }
        return current.map { it.solution }
    }
}

/** 改善后重置到第一邻域的 VND / VND restarting at the first neighborhood after improvement.
 * @property neighborhoods 有序邻域更新 / Ordered neighborhood updates
 * @property maxTrials 有限尝试上限 / Finite trial limit
 */
class VNDCompositeUpdate(val neighborhoods: List<SolutionUpdate>, val maxTrials: Int = 50) : SolutionUpdate {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        return descend(iteration, parents.firstOrNull()?.solution ?: return emptyList(), model, converter, entropy)
    }

    private fun <O, V> descend(
        iteration: Iteration,
        seed: Solution<V>,
        model: AbstractCallBackModelInterface<*, O, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V> {
        var current = ObjectiveFitnessEvaluator.toIndividual(model, seed) ?: return emptyList()
        var index = 0
        var trials = 0
        while (index < neighborhoods.size && trials++ < maxTrials) {
            val candidate = neighborhoods[index](
                iteration = iteration, parents = listOf(current), model = model, converter = converter, entropy = entropy
            ).mapNotNull { ObjectiveFitnessEvaluator.toIndividual(model, it) }
                .minWithOrNull { a, b -> compareIndividuals(
                    model = model,
                    lhs = a,
                    rhs = b
                ) }
            if (candidate != null && compareIndividuals(
                model = model,
                lhs = candidate,
                rhs = current
            ) < 0) {
                current = candidate
                index = 0
            } else ++index
        }
        return listOf(current.solution)
    }
}

/** 仅精调已排序种群的 Top-K，保留其余个体 / Intensify only Top-K of a ranked population, retaining the rest.
 * @property localSearch 单个精英的更新 / Single-elite update
 * @property amount 精英数 / Elite count
 */
class EliteLocalSearchWrapper(val localSearch: SolutionUpdate, val amount: Int = 1) : SolutionUpdate {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        return parents.take(amount.coerceAtLeast(0)).flatMap { localSearch(
            iteration = iteration, parents = listOf(it), model = model, converter = converter, entropy = entropy
        ) } + parents.drop(amount.coerceAtLeast(0)).map { it.solution }
    }
}
