/**
 * 环境选择算子 / Survivor selection operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.constraintHandling
import fuookami.ospf.kotlin.core.solver.heuristic.operator.compareStrictly

/**
 * 环境选择算子接口，决定父代与子代如何合并为下一代。 / Survivor selection interface deciding how parents and offspring merge into the next generation.
*/
interface SurvivorSelection {

    /**
     * 选出进入下一代的个体 / Select the individuals entering the next generation
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param T 个体类型 / Individual type
     * @param model 回调模型接口 / Callback model interface
     * @param parents 父代个体 / Parent individuals
     * @param offspring 子代个体 / Offspring individuals
     * @param amount 目标个体数量 / Target individual amount
     * @return 下一代的个体 / Individuals of the next generation
    */
    operator fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        parents: List<T>,
        offspring: List<T>,
        amount: Int
    ): List<T> where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V>
}

/**
 * 排序合并后取前 amount 个个体 / Rank the merged pool and take the first `amount` individuals
 *
 * 这里用「逐个挑最优」而不是 `sortedWith`：比较器来自模型，可能是带容差的近似比较，
 * 不具备传递性，TimSort 会因此抛 `Comparison method violates its general contract`。
 * 逐个挑选只依赖两两比较的结果，不做传递性假设，代价是 O(k·n)，而 k 与 n 都是种群规模级别。
 * /
 * Top-k is selected by repeatedly picking the best instead of `sortedWith`: the comparator comes from the model and may be an
 * approximate one without transitivity, which makes TimSort throw `Comparison method violates its general contract`. Repeated
 * selection relies only on pairwise comparisons and makes no transitivity assumption; it costs O(k·n) with k and n at
 * population scale.
 *
 * @param model 回调模型接口 / Callback model interface
 * @param individuals 待排序个体 / Individuals to rank
 * @param amount 保留数量 / Amount to keep
 * @return 保留的个体 / Kept individuals
*/
private fun <ObjValue, V, T> bestOf(
    model: AbstractCallBackModelInterface<*, ObjValue, V>,
    individuals: List<T>,
    amount: Int
): List<T> where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
    constraintHandling(model)?.let { return it.rank(individuals).take(amount.coerceAtLeast(0)) }
    val remaining = individuals.toMutableList()
    val kept = ArrayList<T>(minOf(amount, remaining.size))

    repeat(minOf(amount, remaining.size)) {
        var bestIndex = 0
        for (index in 1 until remaining.size) {
            if (compareStrictly(model, remaining[index].fitness, remaining[bestIndex].fitness) < 0) {
                bestIndex = index
            }
        }
        kept.add(remaining.removeAt(bestIndex))
    }
    return kept
}

/** (μ + λ) 环境选择：父代与子代一起竞争 / (μ + λ) survivor selection: parents and offspring compete together */
data object ElitistSurvivor : SurvivorSelection {
    override operator fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        parents: List<T>,
        offspring: List<T>,
        amount: Int
    ): List<T> where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
        return bestOf(model, parents + offspring, amount)
    }
}

/** (μ, λ) 环境选择：只保留子代 / (μ, λ) survivor selection: only offspring survive */
data object GenerationalSurvivor : SurvivorSelection {
    override operator fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        parents: List<T>,
        offspring: List<T>,
        amount: Int
    ): List<T> where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
        return bestOf(model, offspring, amount)
    }
}

/**
 * 截断环境选择：合并后按优度截断，不做其他处理 / Truncation survivor selection: merge and cut by quality
 *
 * @property amount 保留数量 / Amount to keep
*/
data class TruncationSurvivor(
    val amount: Int
) : SurvivorSelection {
    override operator fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        parents: List<T>,
        offspring: List<T>,
        amount: Int
    ): List<T> where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
        return bestOf(model, parents + offspring, minOf(this.amount, amount))
    }
}

/**
 * 精英保留环境选择：先保留最优父代，再用子代补足 / Elitist survivor selection: keep the best parents first, then fill with offspring
 *
 * @property eliteAmount 精英父代数量 / Number of elite parents
*/
data class ElitistPreservingSurvivor(
    val eliteAmount: Int
) : SurvivorSelection {
    override operator fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        parents: List<T>,
        offspring: List<T>,
        amount: Int
    ): List<T> where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
        val elites = bestOf(model, parents, minOf(eliteAmount, amount))
        val rest = bestOf(model, offspring, amount - elites.size)
        return elites + rest
    }
}
