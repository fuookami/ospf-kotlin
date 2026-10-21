/**
 * 经典交叉算子 / Classic crossover operators
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
 * 单点交叉，随机选一个切点后交换尾部。 / Single-point crossover swapping the tails after one random cut.
*/
data object SinglePointCross : Cross {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val (first, second) = pickTwo(parents) ?: return emptyList()
        val a = first.solution.toMutableList()
        val b = second.solution.toMutableList()
        if (a.size != b.size || a.size < 2) {
            return listOf(a, b)
        }

        val cut = 1 + (entropy.unit() * (a.size - 1)).toInt().coerceIn(0, a.size - 2)
        for (index in cut until a.size) {
            val temporary = a[index]
            a[index] = b[index]
            b[index] = temporary
        }
        return listOf(a, b)
    }
}

/**
 * 两点交叉，交换两个切点之间的片段。 / Two-point crossover swapping the segment between two cuts.
*/
data object TwoPointCross : Cross {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val (first, second) = pickTwo(parents) ?: return emptyList()
        val a = first.solution.toMutableList()
        val b = second.solution.toMutableList()
        if (a.size != b.size || a.size < 2) {
            return listOf(a, b)
        }

        val leftRaw = (entropy.unit() * a.size).toInt().coerceIn(0, a.size - 1)
        val rightRaw = (entropy.unit() * a.size).toInt().coerceIn(0, a.size - 1)
        val start = min(leftRaw, rightRaw)
        val end = if (start == max(leftRaw, rightRaw)) {
            min(start + 1, a.size - 1)
        } else {
            max(leftRaw, rightRaw)
        }
        for (index in start until end) {
            val temporary = a[index]
            a[index] = b[index]
            b[index] = temporary
        }
        return listOf(a, b)
    }
}

/**
 * 多点交叉，随机选若干切点后交错交换片段。 / Multi-point crossover swapping alternating segments split by several random cuts.
*/
data class MultiPointCross(
    val cutAmount: Int = 3
) : Cross {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val (first, second) = pickTwo(parents) ?: return emptyList()
        val a = first.solution.toMutableList()
        val b = second.solution.toMutableList()
        if (a.size != b.size || a.size < 2) {
            return listOf(a, b)
        }

        val amount = min(max(cutAmount, 1), a.size - 1)
        val cuts = sortedSetOf<Int>()
        var attempts = 0
        while (cuts.size < amount && attempts < amount * 8) {
            cuts.add((entropy.unit() * a.size).toInt().coerceIn(1, a.size - 1))
            ++attempts
        }
        var swapping = false
        var cursor = 0
        for (cut in cuts) {
            if (swapping) {
                for (index in cursor until cut) {
                    val temporary = a[index]
                    a[index] = b[index]
                    b[index] = temporary
                }
            }
            swapping = !swapping
            cursor = cut
        }
        if (swapping) {
            for (index in cursor until a.size) {
                val temporary = a[index]
                a[index] = b[index]
                b[index] = temporary
            }
        }
        return listOf(a, b)
    }
}

/**
 * 均匀交叉，逐基因独立抛硬币决定是否交换。 / Uniform crossover deciding every gene independently with a coin flip.
 *
 * @property mixingRatio 交换概率 / Exchange probability
*/
data class UniformCross(
    val mixingRatio: Flt64 = Flt64(0.5)
) : Cross {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val (first, second) = pickTwo(parents) ?: return emptyList()
        val a = first.solution.toMutableList()
        val b = second.solution.toMutableList()
        if (a.size != b.size) {
            return listOf(a, b)
        }

        val ratio = mixingRatio.toDouble()
        for (index in a.indices) {
            if (entropy.unit() < ratio) {
                val temporary = a[index]
                a[index] = b[index]
                b[index] = temporary
            }
        }
        return listOf(a, b)
    }
}

/**
 * 算术交叉，两个子代是父代的互补加权平均。 / Arithmetic crossover whose two children are complementary weighted averages of the parents.
*/
data object ArithmeticCross : Cross {
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

        val alpha = entropy.unit()
        val firstChild = ArrayList<V>(left.size)
        val secondChild = ArrayList<V>(left.size)
        for (index in left.indices) {
            val a = left[index].asDouble()
            val b = right[index].asDouble()
            val lower = model.lowerBoundOf(index)
            val upper = model.upperBoundOf(index)
            firstChild.add(clampTo(alpha * a + (1.0 - alpha) * b, lower, upper).asValue(converter))
            secondChild.add(clampTo(alpha * b + (1.0 - alpha) * a, lower, upper).asValue(converter))
        }
        return listOf(firstChild, secondChild)
    }
}

/**
 * 混合交叉（BLX-α），在父代张成的扩展区间内均匀采样。 / Blend crossover (BLX-α) sampling uniformly inside the parents' expanded interval.
 *
 * @property expansion 扩展系数 α / Expansion coefficient α
*/
data class BlendCross(
    val expansion: Flt64 = Flt64(0.5)
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

        val alpha = expansion.toDouble()
        val firstChild = ArrayList<V>(left.size)
        val secondChild = ArrayList<V>(left.size)
        for (index in left.indices) {
            val a = left[index].asDouble()
            val b = right[index].asDouble()
            val spread = abs(a - b) * alpha
            val low = min(a, b) - spread
            val high = max(a, b) + spread
            val lower = model.lowerBoundOf(index)
            val upper = model.upperBoundOf(index)
            firstChild.add(clampTo(low + entropy.unit() * (high - low), lower, upper).asValue(converter))
            secondChild.add(clampTo(low + entropy.unit() * (high - low), lower, upper).asValue(converter))
        }
        return listOf(firstChild, secondChild)
    }
}
