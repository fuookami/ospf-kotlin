/**
 * Tier-2 位置更新算子（第三批）/ Tier-2 position update operators (third batch)
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
 * 布谷鸟搜索（CS）。 / Cuckoo search (CS).
 *
 * 两步： / Two steps:
 *
 * - 莱维飞行：X' = X + α·Levy(λ)，重尾步长让搜索同时具备局部细搜与远程跳跃
 *   Lévy flight: X' = X + α·Levy(λ); the heavy tail gives both local refinement and long jumps
 * - 宿主发现：以概率 `p_a` 放弃当前巢，直接在变量域内重建
 *   host discovery: with probability `p_a` the nest is abandoned and rebuilt uniformly inside the domain
 *
 * 莱维步长用 Mantegna 公式生成：`step = u / |v|^{1/λ}`，`u ~ N(0, σ²)`，`v ~ N(0, 1)`。 /
 * The Lévy step uses Mantegna's formula: `step = u / |v|^{1/λ}` with `u ~ N(0, σ²)` and `v ~ N(0, 1)`.
 *
 * @property levyExponent 莱维指数 λ / Lévy exponent λ
 * @property stepScale 步长系数 α / Step scale α
 * @property abandonmentProbability 宿主发现概率 p_a / Host discovery probability p_a
*/
data class CuckooSearchUpdate(
    val levyExponent: Double = 1.5,
    val stepScale: Double = 0.01,
    val abandonmentProbability: Double = 0.25
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

        val best = parents.first().solution
        val sigma = levySigma(levyExponent)

        return parents.map { parent ->
            val current = parent.solution
            val dimension = current.size
            val abandoned = entropy.unit() < abandonmentProbability
            val moved = ArrayList<V>(dimension)

            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val width = if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                    upper - lower
                } else {
                    1.0
                }
                val origin = if (lower != null && lower.isFinite()) lower else 0.0
                val position = current[dim].asDouble()

                val value = if (abandoned) {
                    // 放弃巢穴：在变量域内重建。
                    // Abandon the nest: rebuild uniformly inside the domain.
                    origin + entropy.unit() * width
                } else {
                    val u = normal(entropy) * sigma
                    val v = abs(normal(entropy)).coerceAtLeast(1.0e-12)
                    val levy = u / v.pow(1.0 / levyExponent)
                    // 朝最优方向缩放，避免无方向的纯随机游走。
                    // Scale towards the best so the walk is not purely undirected.
                    position + stepScale * levy * (position - best[dim].asDouble() + width)
                }
                moved.add(clampTo(value, lower, upper).asValue(converter))
            }
            moved
        }
    }

    /** 计算 Mantegna 公式中的 σ / Compute σ of Mantegna's formula */
    private fun levySigma(exponent: Double): Double {
        val numerator = gamma(1.0 + exponent) * sin(PI * exponent / 2.0)
        val denominator = gamma((1.0 + exponent) / 2.0) * exponent * 2.0.pow((exponent - 1.0) / 2.0)
        return (numerator / denominator).pow(1.0 / exponent)
    }

    /** Lanczos 近似的 Γ 函数 / Γ function through the Lanczos approximation */
    private fun gamma(value: Double): Double {
        val coefficients = doubleArrayOf(
            676.5203681218851, -1259.1392167224028, 771.32342877765313,
            -176.61502916214059, 12.507343278686905, -0.13857109526572012,
            9.9843695780195716e-6, 1.5056327351493116e-7
        )
        if (value < 0.5) {
            return PI / (sin(PI * value) * gamma(1.0 - value))
        }
        val shifted = value - 1.0
        var series = 0.99999999999980993
        for (index in coefficients.indices) {
            series += coefficients[index] / (shifted + index + 1.0)
        }
        val t = shifted + coefficients.size - 0.5
        return sqrt(2.0 * PI) * t.pow(shifted + 0.5) * exp(-t) * series
    }

    /** 由均匀熵源近似标准正态（4 次均匀求和）/ Approximate a standard normal from four uniforms */
    private fun normal(entropy: Generator<Flt64>): Double {
        return entropy.unit() + entropy.unit() + entropy.unit() + entropy.unit() - 2.0
    }
}

/**
 * 蝙蝠算法（BA）。 / Bat algorithm (BA).
 *
 * 每只蝙蝠按频率缩放向最优解跳跃，并用响度控制跳幅： / Each bat jumps towards the best solution scaled by its frequency, with loudness controlling the jump size:
 *
 * f = f_min + (f_max − f_min)·β
 * X' = X + (X − X_best)·f·r
 *
 * 原文还维护速度、脉冲发射率与响度的衰减；速度是跨迭代状态，无法放进无状态算子，
 * 这里用「频率×随机数」直接作用于位置，等价于单位时间步长下的一次脉冲。 /
 * The original also maintains velocity, pulse rate and loudness decay; velocity is cross-iteration state that does not fit a
 * stateless operator, so here frequency times a random number acts directly on the position, equivalent to one pulse at a unit
 * time step.
 *
 * @property minimumFrequency 最小频率 / Minimum frequency
 * @property maximumFrequency 最大频率 / Maximum frequency
*/
data class BatAlgorithmUpdate(
    val minimumFrequency: Double = 0.0,
    val maximumFrequency: Double = 2.0
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

        val best = parents.first().solution

        return parents.map { parent ->
            val current = parent.solution
            val dimension = current.size
            val frequency = minimumFrequency + entropy.unit() * (maximumFrequency - minimumFrequency)
            val moved = ArrayList<V>(dimension)

            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val position = current[dim].asDouble()
                val step = (position - best[dim].asDouble()) * frequency * entropy.unit()
                moved.add(clampTo(position + step, lower, upper).asValue(converter))
            }
            moved
        }
    }
}

/**
 * 飞蛾扑火优化（MFO）。 / Moth-flame optimization (MFO).
 *
 * 飞蛾围绕「火焰」（当前前沿中的解）做对数螺旋飞行： / Moths fly a logarithmic spiral around flames, i.e. solutions from the current front:
 *
 * X' = |F − X|·e^{b·t}·cos(2πt) + F,  t ∈ [−1, 1]
 *
 * 火焰数量随迭代从 `size` 递减到 1，使搜索由「多点并行探索」过渡到「围绕最优精修」。 /
 * The flame count decreases from `size` to 1 with iterations, shifting the search from parallel exploration to refinement around
 * the best.
 *
 * @property spiralRate 螺旋形状参数 b / Spiral shape parameter b
 * @property flameRatio 火焰数量占种群规模的比例演化源 / Evolution source of the flame count as a ratio of the population size
*/
data class MothFlameUpdate(
    val spiralRate: Double = 1.0,
    val flameRatio: ParameterSource = LinearSource(Flt64.one, Flt64(0.01), UInt64(100))
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

        val size = parents.size
        // 火焰数量随迭代递减到 1，使搜索从多点并行探索过渡到围绕最优精修。
        // The flame count decreases towards 1 with iterations, shifting the search from parallel exploration to refinement around the best.
        val flameAmount = (size * flameRatio(iteration).toDouble()).toInt().coerceIn(1, size)

        return parents.mapIndexed { index, parent ->
            val current = parent.solution
            val dimension = current.size
            val flame = parents[(index % flameAmount).coerceIn(0, size - 1)].solution
            val spiral = (entropy.unit() * 2.0 - 1.0)

            val moved = ArrayList<V>(dimension)
            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val distance = abs(flame[dim].asDouble() - current[dim].asDouble())
                val value = distance * exp(spiralRate * spiral) * cos(2.0 * PI * spiral) +
                    flame[dim].asDouble()
                moved.add(clampTo(value, lower, upper).asValue(converter))
            }
            moved
        }
    }
}

/**
 * 蝗虫优化算法（GOA）。 / Grasshopper optimization algorithm (GOA).
 *
 * 蝗虫受到邻近个体的排斥/吸引合力、重力与风力： / A grasshopper is subject to the repulsion/attraction of its neighbours, gravity, and wind:
 *
 * X' = c·Σ_j [c·(UB − LB)/2·s(|X_j − X|)·(X_j − X)/d] + X_best
 * s(r) = f·e^{−r/l} − e^{−r}
 *
 * 收缩系数 `c` 从 `c_max` 线性降到 `c_min`，同时缩小社会作用范围与吸引强度，实现由探索到开发的过渡。 /
 * The shrinking coefficient `c` falls linearly from `c_max` to `c_min`, narrowing both the social range and the attraction strength
 * and thereby shifting exploration into exploitation.
 *
 * @property shrink 收缩系数 c 的演化源 / Evolution source of the shrinking coefficient c
 * @property attractionScale 吸引强度 f / Attraction strength f
 * @property attractionLength 吸引长度尺度 l / Attraction length scale l
*/
data class GrasshopperUpdate(
    val shrink: ParameterSource = LinearSource(Flt64.one, Flt64(0.00004), UInt64(100)),
    val attractionScale: Double = 0.5,
    val attractionLength: Double = 1.5
) : SolutionUpdate {

    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T: Individual<*, V> {
        if (parents.isEmpty()) {
            return emptyList()
        }

        val best = parents.first().solution
        val coefficient = shrink(iteration).toDouble()

        return parents.map { parent ->
            val current = parent.solution
            val dimension = current.size
            val moved = ArrayList<V>(dimension)

            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val width = if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                    upper - lower
                } else {
                    1.0
                }

                var social = 0.0
                for (other in parents) {
                    val distance = abs(other.solution[dim].asDouble() - current[dim].asDouble())
                    if (distance <= 0.0) {
                        continue
                    }
                    val interaction = attractionScale * exp(-distance / attractionLength) - exp(-distance)
                    social += interaction *
                        (other.solution[dim].asDouble() - current[dim].asDouble()) / distance
                }

                val value = coefficient * coefficient * (width / 2.0) * social + best[dim].asDouble()
                moved.add(clampTo(value, lower, upper).asValue(converter))
            }
            moved
        }
    }
}
