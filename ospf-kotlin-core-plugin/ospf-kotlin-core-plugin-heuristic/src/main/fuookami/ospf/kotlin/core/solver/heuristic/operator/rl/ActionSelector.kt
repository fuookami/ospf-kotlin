/**
 * 动作选择算子 / Action selection operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.rl

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 动作选择算子契约：从一组动作价值中挑一个动作。 / Action selection contract picking one action from a set of action values.
 *
 * 贪心与探索的权衡只发生在这里，因此换一个选择器就等于换一种探索策略，Q 表本身无需改动。 /
 * The exploration–exploitation trade-off lives only here, so swapping the selector swaps the exploration policy
 * without touching the table.
*/
fun interface ActionSelector {

    /**
     * 选择动作 / Select an action
     *
     * @param values 各动作价值 / Action values
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 动作下标，位于 `[0, values.size)` / Action index in `[0, values.size)`
    */
    operator fun invoke(values: List<Flt64>, entropy: Generator<Flt64>): Int
}

/**
 * 取价值最大的动作 / Take the action with the largest value
 *
 * @param values 各动作价值 / Action values
 * @return 动作下标 / Action index
*/
private fun argmax(values: List<Flt64>): Int {
    if (values.isEmpty()) {
        return 0
    }
    var best = 0
    var bestValue = values[0]
    for (index in 1 until values.size) {
        if (values[index] gr bestValue) {
            bestValue = values[index]
            best = index
        }
    }
    return best
}

/**
 * 纯贪心选择，完全不探索。 / Pure greedy selection with no exploration.
 *
 * 适合「Q 表已经可信」的场合，或者在外部已经保证探索度时使用。 / Suited to cases where the table is already
 * trustworthy, or where exploration is provided elsewhere.
*/
data object GreedyActionSelector : ActionSelector {
    override fun invoke(values: List<Flt64>, entropy: Generator<Flt64>): Int = argmax(values)
}

/**
 * ε-贪心选择：以概率 [epsilon] 均匀随机探索，否则取价值最大的动作。 / ε-greedy selection: explore uniformly at
 * random with probability [epsilon], otherwise take the largest action value.
 *
 * @property epsilon 探索概率 / Exploration probability
*/
data class EpsilonGreedyActionSelector(
    val epsilon: Flt64 = Flt64(0.1)
) : ActionSelector {

    override fun invoke(values: List<Flt64>, entropy: Generator<Flt64>): Int {
        if (values.isEmpty()) {
            return 0
        }
        val epsilonValue = epsilon.toDouble().coerceIn(0.0, 1.0)
        val roll = (entropy() ?: Flt64.zero).toDouble()
        if (roll < epsilonValue) {
            // 探索：再取一个独立随机数，避免用同一个值既决定是否探索又决定探索位置。 /
            // Exploration: draw an independent sample instead of reusing the decision roll for the position.
            val pick = (entropy() ?: Flt64.zero).toDouble()
            return (pick * values.size.toDouble()).toInt().coerceIn(0, values.size - 1)
        }
        return argmax(values)
    }
}

/**
 * Softmax（玻尔兹曼）选择：按价值的指数权重抽样，因此次优动作也有机会被选中。 /
 * Softmax (Boltzmann) selection samples actions by exponential weights, so sub-optimal actions are still reachable.
 *
 * @property temperature 温度；趋于零时退化为贪心，越大越接近均匀随机 / Temperature; approaching zero degenerates
 * to greedy, larger values approach uniform random
*/
data class SoftmaxActionSelector(
    val temperature: Flt64 = Flt64.one
) : ActionSelector {

    override fun invoke(values: List<Flt64>, entropy: Generator<Flt64>): Int {
        if (values.isEmpty()) {
            return 0
        }
        val temperatureValue = temperature.toDouble()
        if (temperatureValue <= 0.0) {
            return argmax(values)
        }

        val maximum = values.maxOf { it.toDouble() }
        val weights = DoubleArray(values.size) { index ->
            val shifted = values[index].toDouble() - maximum
            if (shifted.isFinite()) exp(shifted / temperatureValue) else 0.0
        }
        var total = 0.0
        for (weight in weights) {
            total += weight
        }
        if (total <= 0.0 || !total.isFinite()) {
            return argmax(values)
        }

        val roll = (entropy() ?: Flt64.zero).toDouble().coerceIn(0.0, 1.0) * total
        var accumulated = 0.0
        for (index in weights.indices) {
            accumulated += weights[index]
            if (roll <= accumulated) {
                return index
            }
        }
        return values.size - 1
    }
}
