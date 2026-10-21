/**
 * 排列变异算子 / Permutation mutation operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * 在解上取两个不同的随机下标 / Draw two distinct random indices on a solution
 *
 * @param size 解长度 / Solution length
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @return 两个下标 / Two indices
*/
private fun twoIndices(size: Int, entropy: Generator<Flt64>): Pair<Int, Int> {
    val first = (entropy.unit() * size).toInt().coerceIn(0, size - 1)
    var second = (entropy.unit() * size).toInt().coerceIn(0, size - 1)
    if (second == first) {
        second = (first + 1) % size
    }
    return first to second
}

/** 交换变异，交换两个基因 / Swap mutation exchanging two genes */
data object SwapMutation : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val solution = individual.solution
        if (solution.size < 2 || !shouldFire(entropy, mutationRate.toDouble())) {
            return solution.toList()
        }

        val result = solution.toMutableList()
        val (first, second) = twoIndices(solution.size, entropy)
        val temporary = result[first]
        result[first] = result[second]
        result[second] = temporary
        return result
    }
}

/** 逆序变异，反转两个切点之间的片段 / Inversion mutation reversing the segment between two cuts */
data object InversionMutation : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val solution = individual.solution
        if (solution.size < 2 || !shouldFire(entropy, mutationRate.toDouble())) {
            return solution.toList()
        }

        val (first, second) = twoIndices(solution.size, entropy)
        val start = min(first, second)
        val end = max(first, second)
        val result = solution.toMutableList()
        result.subList(start, end).reverse()
        return result
    }
}

/** 插入变异，取出一个基因并插入到另一个位置 / Insertion mutation removing one gene and reinserting it elsewhere */
data object InsertionMutation : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val solution = individual.solution
        if (solution.size < 2 || !shouldFire(entropy, mutationRate.toDouble())) {
            return solution.toList()
        }

        val (from, to) = twoIndices(solution.size, entropy)
        val result = solution.toMutableList()
        val value = result.removeAt(from)
        result.add(to.coerceIn(0, result.size), value)
        return result
    }
}

/**
 * 散射变异，打乱一段随机片段内的基因顺序 / Scramble mutation shuffling the genes inside a random segment
 *
 * @property segmentRatio 片段长度占比 / Segment length as a ratio of the solution length
*/
data class ScrambleMutation(
    val segmentRatio: Flt64 = Flt64(0.2)
) : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val solution = individual.solution
        if (solution.size < 2 || !shouldFire(entropy, mutationRate.toDouble())) {
            return solution.toList()
        }

        val (first, second) = twoIndices(solution.size, entropy)
        val start = min(first, second)
        var end = max(first, second)
        val minimumEnd = min(start + max((solution.size * segmentRatio.toDouble()).toInt(), 2), solution.size)
        end = max(end, minimumEnd)

        val result = solution.toMutableList()
        val segment = result.subList(start, end).shuffled()
        for (index in segment.indices) {
            result[start + index] = segment[index]
        }
        return result
    }
}
