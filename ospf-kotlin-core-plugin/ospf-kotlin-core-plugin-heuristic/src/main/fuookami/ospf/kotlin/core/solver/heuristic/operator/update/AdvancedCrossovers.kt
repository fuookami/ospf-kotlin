/**
 * 进阶交叉算子 / Advanced crossover operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.update

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
 * 模拟二进制交叉（SBX），用分布指数控制子代与父代的接近程度。 / Simulated binary crossover (SBX) whose distribution index controls how close children stay to the parents.
 *
 * @property distributionIndex 分布指数 η，越大子代越接近父代 / Distribution index η; larger values keep children closer to the parents
*/
data class SimulatedBinaryCross(
    val distributionIndex: Flt64 = Flt64(20.0)
) : Cross {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val (first, second) = pickTwo(parents) ?: return emptyList()
        val left = first.solution
        val right = second.solution
        if (left.size != right.size) {
            return listOf(left, right)
        }

        val eta = max(distributionIndex.toDouble(), 0.0)
        val exponent = 1.0 / (eta + 1.0)
        val firstChild = ArrayList<V>(left.size)
        val secondChild = ArrayList<V>(left.size)
        for (index in left.indices) {
            val a = left[index].asDouble()
            val b = right[index].asDouble()
            val lower = model.lowerBoundOf(index)
            val upper = model.upperBoundOf(index)

            if (abs(a - b) < 1.0e-12) {
                firstChild.add(clampTo(a, lower, upper).asValue(converter))
                secondChild.add(clampTo(b, lower, upper).asValue(converter))
                continue
            }

            val random = entropy.unit()
            val beta = if (random <= 0.5) {
                (2.0 * random).pow(exponent)
            } else {
                (1.0 / (2.0 * (1.0 - random))).pow(exponent)
            }
            val childA = 0.5 * ((1.0 + beta) * a + (1.0 - beta) * b)
            val childB = 0.5 * ((1.0 - beta) * a + (1.0 + beta) * b)
            firstChild.add(clampTo(childA, lower, upper).asValue(converter))
            secondChild.add(clampTo(childB, lower, upper).asValue(converter))
        }
        return listOf(firstChild, secondChild)
    }
}

/**
 * 差分交叉（DE/rand/1/bin），用种群差向量构造变异体再做二项式交叉。 / Differential crossover (DE/rand/1/bin) building a mutant from a population difference vector and applying binomial crossover.
 *
 * 父代约定为 `[target, a, b]`：变异体 = a + F × (b − c)，其中 c 缺省时复用 b。 / Parents are expected as `[target, a, b]`: the mutant is a + F × (b − c), where c reuses b when absent.
 *
 * @property differentialWeight 缩放因子 F / Scaling factor F
 * @property crossRate 交叉概率 CR / Crossover rate CR
*/
data class DifferentialCross(
    val differentialWeight: Flt64 = Flt64(0.5),
    val crossRate: Flt64 = Flt64(0.9)
) : Cross {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        if (parents.size < 2) {
            return parents.map { it.solution.toList() }
        }

        val target = parents[0].solution
        val base = parents[1].solution
        val firstDifference = parents[2 % parents.size].solution
        val secondDifference = parents[3 % parents.size].solution
        if (base.size != firstDifference.size || base.size != secondDifference.size || base.size != target.size) {
            return listOf(target.toList())
        }

        val weight = differentialWeight.toDouble()
        val rate = crossRate.toDouble()
        val child = ArrayList<V>(target.size)
        for (index in target.indices) {
            val lower = model.lowerBoundOf(index)
            val upper = model.upperBoundOf(index)
            val mutant = base[index].asDouble() +
                weight * (firstDifference[index].asDouble() - secondDifference[index].asDouble())
            val value = if (entropy.unit() < rate) mutant else target[index].asDouble()
            child.add(clampTo(value, lower, upper).asValue(converter))
        }
        return listOf(child)
    }
}

/**
 * 顺序交叉（OX），保留一个父代的片段，其余位置按另一个父代的相对顺序填充。 / Order crossover (OX) keeping one parent's segment and filling the rest in the other parent's relative order.
 *
 * 仅适用于排列编码的解。 / Intended for permutation-encoded solutions.
*/
data object OrderCross : Cross {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val (first, second) = pickTwo(parents) ?: return emptyList()
        val left = first.solution
        val right = second.solution
        if (left.size != right.size || left.isEmpty()) {
            return listOf(left, right)
        }

        return listOf(
            orderCrossOnce(left, right, entropy),
            orderCrossOnce(right, left, entropy)
        )
    }

    /**
     * 单向顺序交叉 / One-directional order crossover
     *
     * @param segmentSource 提供保留片段的父代 / Parent providing the kept segment
     * @param orderSource 提供相对顺序的父代 / Parent providing the relative order
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 子代解 / Offspring solution
    */
    private fun <V> orderCrossOnce(
        segmentSource: List<V>,
        orderSource: List<V>,
        entropy: Generator<Flt64>
    ): Solution<V> {
        val size = segmentSource.size
        val left = (entropy.unit() * size).toInt().coerceIn(0, size - 1)
        val right = (entropy.unit() * size).toInt().coerceIn(0, size - 1)
        val start = min(left, right)
        val end = if (start == max(left, right)) min(start + 1, size) else max(left, right)

        val child = orderSource.toMutableList()
        val used = HashSet<V>()
        for (index in start until end) {
            child[index] = segmentSource[index]
            used.add(segmentSource[index])
        }

        val freePositions = (0 until size).filter { it < start || it >= end }
        val orderedValues = (0 until size)
            .map { orderSource[(end + it) % size] }
            .filter { !used.contains(it) }
        val amount = min(freePositions.size, orderedValues.size)
        for (index in 0 until amount) {
            child[freePositions[index]] = orderedValues[index]
        }
        return child
    }
}

/**
 * 循环交叉（CX），沿父代映射形成的循环逐个赋值。 / Cycle crossover (CX) assigning genes along the cycles formed by the parents' mapping.
 *
 * 仅适用于排列编码的解。 / Intended for permutation-encoded solutions.
*/
data object CycleCross : Cross {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val (first, second) = pickTwo(parents) ?: return emptyList()
        val left = first.solution
        val right = second.solution
        if (left.size != right.size || left.isEmpty()) {
            return listOf(left, right)
        }

        return listOf(
            cycleCrossOnce(left, right),
            cycleCrossOnce(right, left)
        )
    }

    /**
     * 单向循环交叉 / One-directional cycle crossover
     *
     * @param cycleSource 提供循环内取值的父代 / Parent providing the values inside the cycle
     * @param otherSource 提供剩余位置的父代 / Parent providing the remaining positions
     * @return 子代解 / Offspring solution
    */
    private fun <V> cycleCrossOnce(
        cycleSource: List<V>,
        otherSource: List<V>
    ): Solution<V> {
        val size = cycleSource.size
        val child = otherSource.toMutableList()
        val assigned = BooleanArray(size)
        var index = 0
        var guard = 0
        while (index in 0 until size && !assigned[index] && guard <= size) {
            assigned[index] = true
            child[index] = cycleSource[index]
            val next = otherSource.indexOf(cycleSource[index])
            if (next < 0) {
                break
            }
            index = next
            ++guard
        }
        return child
    }
}
