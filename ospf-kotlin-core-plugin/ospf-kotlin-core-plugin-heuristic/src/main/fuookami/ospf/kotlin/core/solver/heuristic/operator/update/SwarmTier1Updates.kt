/**
 * Tier-1 群智能位置更新算子 / Tier-1 swarm position update operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.update

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.LinearSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * 水循环算法（WCA）。 / Water cycle algorithm (WCA).
 *
 * 按优度把个体分成三层：海（最优）、河流（次优若干）、溪流（其余）。溪流汇入河流、河流汇入海，
 * 流动过程本身就是「向更优个体收缩」： / Individuals are split into three layers by quality: the sea (best), rivers (the next few) and streams (the rest).
 * Streams flow into rivers and rivers into the sea, and that flow is precisely "contracting towards a better individual":
 *
 * X' = X + rand · (X_target − X)
 *
 * 以 `rainingProbability` 的概率降下新雨（在取值域内随机重生），为种群引入全新位置，避免整体塌缩。 /
 * With probability `rainingProbability` fresh rain is generated at a uniformly random position, injecting entirely new
 * locations and preventing the whole population from collapsing.
 *
 * @property riverAmount 河流数量 / River amount
 * @property flowScale 流动步长系数 / Flow step scale
 * @property rainingProbability 降雨概率 / Raining probability
*/
data class WaterCycleUpdate(
    val riverAmount: Int = 4,
    val flowScale: Double = 1.0,
    val rainingProbability: Double = 0.1
) : SolutionUpdate {

    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        if (parents.isEmpty()) {
            return emptyList()
        }

        val sea = parents.first().solution
        val riverCount = riverAmount.coerceIn(0, max(parents.size - 1, 0))

        return parents.mapIndexed { index, parent ->
            val current = parent.solution

            if (entropy.unit() < rainingProbability) {
                // 降雨：在取值域内随机重生。
                // Raining: rebirth at a uniformly random position.
                val dimension = current.size
                val rain = ArrayList<V>(dimension)
                for (dim in 0 until dimension) {
                    val lower = model.lowerBoundOf(dim)
                    val upper = model.upperBoundOf(dim)
                    val width = if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                        upper - lower
                    } else {
                        1.0
                    }
                    val origin = if (lower != null && lower.isFinite()) lower else 0.0
                    rain.add(clampTo(origin + entropy.unit() * width, lower, upper).asValue(converter))
                }
                return@mapIndexed rain
            }

            // 海自己不流动；河流流向海；溪流流向某条河流（按下标轮转分配，父代已按优度排序）。
            // The sea does not flow; rivers flow into the sea; streams flow into one of the rivers, assigned round-robin by index
            // because the parents are already ranked by quality.
            val target = when {
                index == 0 -> sea
                index <= riverCount -> sea
                riverCount == 0 -> sea
                else -> parents[((index - 1) % riverCount) + 1].solution
            }

            val dimension = current.size
            val moved = ArrayList<V>(dimension)
            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val step = if (index == 0) {
                    // 海做一次小幅随机游走，避免失去多样性。
                    // The sea performs a small random walk to keep some diversity.
                    val width = if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                        upper - lower
                    } else {
                        1.0
                    }
                    (entropy.unit() * 2.0 - 1.0) * 0.1 * width * flowScale
                } else {
                    entropy.unit() * (target[dim].asDouble() - current[dim].asDouble()) * flowScale
                }
                moved.add(clampTo(current[dim].asDouble() + step, lower, upper).asValue(converter))
            }
            moved
        }
    }
}

/**
 * 海鸥优化算法（SOA）。 / Seagull optimization algorithm (SOA).
 *
 * 两个阶段由控制参数 A 切换： / Two phases switched by the control parameter A:
 *
 * - A ≥ 1（迁徙/探索）：个体被最优解牵引并叠加随机放大，向全局最优靠拢；
 *   A ≥ 1 (migration / exploration): the individual is pulled by the best solution with a random amplification towards the
 *   global optimum;
 * - A < 1（攻击/开发）：沿螺旋轨迹围绕最优解收缩。
 *   A < 1 (attack / exploitation): contracts around the best solution along a spiral trajectory.
 *
 * 螺旋半径随角度指数增长，使个体由外向内绕圈逼近，而不是直接跳向最优，因而保留了绕开局部最优的能力。 /
 * The spiral radius grows exponentially with the angle so that individuals circle inwards instead of jumping straight at the
 * best solution, which preserves the ability to bypass local optima.
 *
 * @property control 控制参数 A 的演化源 / Evolution source of the control parameter A
 * @property spiralRate 螺旋增长率 / Spiral growth rate
*/
data class SeagullUpdate(
    val control: ParameterSource = LinearSource(Flt64.two, Flt64.zero, UInt64(100)),
    val spiralRate: Double = 0.1
) : SolutionUpdate {

    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val best = parents.firstOrNull()?.solution ?: return emptyList()
        val coefficient = control(iteration).toDouble()

        return parents.map { parent ->
            val current = parent.solution
            val migrated = coefficient >= 1.0
            val dimension = current.size
            val child = ArrayList<V>(dimension)

            for (index in 0 until dimension) {
                val lower = model.lowerBoundOf(index)
                val upper = model.upperBoundOf(index)
                val position = current[index].asDouble()
                val target = best[index].asDouble()

                val moved = if (migrated) {
                    val amplification = 2.0 * coefficient * coefficient * entropy.unit()
                    position + amplification * (target - position)
                } else {
                    val theta = 2.0 * PI * entropy.unit()
                    val radius = exp(theta * spiralRate)
                    target + (position - target) * radius * cos(theta)
                }
                child.add(clampTo(moved, lower, upper).asValue(converter))
            }
            child
        }
    }
}
