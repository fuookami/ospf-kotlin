/**
 * Tier-2 群智能位置更新算子 / Tier-2 swarm position update operators
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
 * 鲸鱼优化算法（WOA）。 / Whale optimization algorithm (WOA).
 *
 * 三条互斥分支由 `p` 与 `|A|` 共同选择： / Three mutually exclusive branches are chosen by `p` together with `|A|`:
 *
 * - 包围猎物（`p < 0.5` 且 `|A| < 1`）：X' = X_best − A·|C·X_best − X|
 *   encircling prey: X' = X_best − A·|C·X_best − X|
 * - 随机搜索（`p < 0.5` 且 `|A| ≥ 1`）：X' = X_rand − A·|C·X_rand − X|
 *   random search: X' = X_rand − A·|C·X_rand − X|
 * - 螺旋气泡网（`p ≥ 0.5`）：X' = |X_best − X|·e^{b·l}·cos(2πl) + X_best
 *   bubble-net spiral: X' = |X_best − X|·e^{b·l}·cos(2πl) + X_best
 *
 * `a` 从 2 线性衰减到 0，使 `|A|` 在前半程大概率 ≥ 1（探索）、后半程 < 1（开发）。 /
 * `a` decays linearly from 2 to 0, so `|A|` is mostly ≥ 1 in the first half (exploration) and < 1 in the second (exploitation).
 *
 * @property decay 控制参数 a 的演化源 / Evolution source of the control parameter a
 * @property spiralRate 螺旋形状参数 b / Spiral shape parameter b
*/
data class WhaleOptimizationUpdate(
    val decay: ParameterSource = LinearSource(Flt64.two, Flt64.zero, UInt64(100)),
    val spiralRate: Double = 1.0
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
        val a = decay(iteration).toDouble()

        return parents.map { parent ->
            val current = parent.solution
            val dimension = current.size
            val moved = ArrayList<V>(dimension)

            val p = entropy.unit()
            val spiral = p >= 0.5
            // A 与 C 对每个个体独立抽样，这就是原文中「每头鲸鱼各自更新」的含义。
            // A and C are sampled per individual, which is what "each whale updates independently" means in the original.
            val coefficientA = 2.0 * a * entropy.unit() - a
            val coefficientC = 2.0 * entropy.unit()
            val reference = if (abs(coefficientA) >= 1.0) {
                parents[(entropy.unit() * parents.size).toInt().coerceIn(0, parents.size - 1)].solution
            } else {
                best
            }
            val spiralLength = (a * 2.0 - 1.0) * entropy.unit()

            for (dim in 0 until dimension) {
                val position = current[dim].asDouble()
                val target = reference[dim].asDouble()
                val value = if (spiral) {
                    val distance = abs(best[dim].asDouble() - position)
                    distance * exp(spiralRate * spiralLength) * cos(2.0 * PI * spiralLength) +
                        best[dim].asDouble()
                } else {
                    target - coefficientA * abs(coefficientC * target - position)
                }
                moved.add(
                    clampTo(value, model.lowerBoundOf(dim), model.upperBoundOf(dim)).asValue(converter)
                )
            }
            moved
        }
    }
}

/**
 * 哈里斯鹰优化（HHO）。 / Harris hawks optimization (HHO).
 *
 * 逃逸能量 `E = 2·(1 − t/T)·(2r − 1)` 从约 2 衰减到 0，`|E| ≥ 1` 时全局搜索，否则转入围攻： /
 * The escaping energy `E = 2·(1 − t/T)·(2r − 1)` decays from about 2 to 0; `|E| ≥ 1` triggers global search, otherwise the
 * siege begins:
 *
 * - 探索：X' = X_rand − r₃·|X_rand − 2r₄·X|
 *   exploration: X' = X_rand − r₃·|X_rand − 2r₄·X|
 * - 软围攻（`|E| ≥ 0.5`）：X' = X_best − X − E·|J·X_best − X|, J = 2(1 − r₅)
 *   soft besiege: X' = X_best − X − E·|J·X_best − X|, J = 2(1 − r₅)
 * - 硬围攻（`|E| < 0.5`）：X' = X_best − E·|X_best − X|
 *   hard besiege: X' = X_best − E·|X_best − X|
 *
 * 原文的两种「俯冲」策略（带 Lévy 飞行的软/硬围攻）未纳入——它们需要额外的短程候选评估，
 * 与解更新算子「一次产出一批解」的契约不匹配，留待需要时以独立算子补充。 /
 * The two dive strategies of the original (soft/hard besiege with Lévy flights) are omitted: they require extra short-range
 * candidate evaluations that do not fit the "emit a batch of solutions" contract of a solution-update operator.
 *
 * @property decay 逃逸能量的衰减源 / Decay source of the escaping energy
*/
data class HarrisHawkUpdate(
    val decay: ParameterSource = LinearSource(Flt64.two, Flt64.zero, UInt64(100))
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
        val decayFactor = decay(iteration).toDouble()

        return parents.map { parent ->
            val current = parent.solution
            val dimension = current.size
            val energy = decayFactor * (2.0 * entropy.unit() - 1.0)
            val random = parents[(entropy.unit() * parents.size).toInt().coerceIn(0, parents.size - 1)].solution
            val jump = 2.0 * (1.0 - entropy.unit())

            val moved = ArrayList<V>(dimension)
            for (dim in 0 until dimension) {
                val position = current[dim].asDouble()
                val bestPosition = best[dim].asDouble()
                val value = when {
                    abs(energy) >= 1.0 -> {
                        val randomPosition = random[dim].asDouble()
                        randomPosition - entropy.unit() * abs(randomPosition - 2.0 * entropy.unit() * position)
                    }

                    abs(energy) >= 0.5 -> {
                        position - energy * abs(jump * bestPosition - position) + entropy.unit() *
                            (bestPosition - position)
                    }

                    else -> position - energy * abs(bestPosition - position)
                }
                moved.add(
                    clampTo(value, model.lowerBoundOf(dim), model.upperBoundOf(dim)).asValue(converter)
                )
            }
            moved
        }
    }
}

/**
 * 算术优化算法（AOA）。 / Arithmetic optimization algorithm (AOA).
 *
 * 用「数学优化加速器」MOA 在探索与开发之间切换： / A "math optimizer accelerated" (MOA) coefficient switches between exploration and exploitation:
 *
 * - `r₁ > MOA`（探索）：乘法或除法，产生大幅跳变
 *   `r₁ > MOA` (exploration): multiplication or division, producing large jumps
 * - 否则（开发）：加法或减法，产生小幅修正
 *   otherwise (exploitation): addition or subtraction, producing small corrections
 *
 * 除法分支可能把值放大到极大，因此 `MOP` 有下界保护；最终仍由 `clampTo` 拦截非有限值。 /
 * The division branch can amplify values enormously, so `MOP` is floored; non-finite results are still caught by `clampTo`.
 *
 * @property accelerator 数学优化加速器 MOA 的演化源 / Evolution source of the math optimizer accelerated coefficient
 * @property probability 数学优化概率 MOP 的演化源 / Evolution source of the math optimizer probability
 * @property sensitivity 数学优化概率 MOP 的敏感系数 μ / Sensitivity μ of the math optimizer probability
*/
data class ArithmeticOptimizationUpdate(
    val accelerator: ParameterSource = LinearSource(Flt64(0.2), Flt64(0.9), UInt64(100)),
    val probability: ParameterSource = LinearSource(Flt64.one, Flt64(0.001), UInt64(100)),
    val sensitivity: Double = 0.5
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
        val mathOptimizerAccelerated = accelerator(iteration).toDouble()
        val mathOptimizerProbability = probability(iteration).toDouble().coerceIn(1.0e-6, 1.0)

        return parents.map { parent ->
            val current = parent.solution
            val dimension = current.size
            val moved = ArrayList<V>(dimension)

            val first = entropy.unit()
            val second = entropy.unit()
            val third = entropy.unit()

            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val width = if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                    upper - lower
                } else {
                    1.0
                }
                val origin = if (lower != null && lower.isFinite()) lower else 0.0
                val scale = width * sensitivity + origin
                val bestPosition = best[dim].asDouble()

                val value = if (first > mathOptimizerAccelerated) {
                    if (second > 0.5) {
                        bestPosition / mathOptimizerProbability * scale
                    } else {
                        bestPosition * mathOptimizerProbability * scale
                    }
                } else {
                    if (third > 0.5) {
                        bestPosition - mathOptimizerProbability * scale
                    } else {
                        bestPosition + mathOptimizerProbability * scale
                    }
                }
                moved.add(clampTo(value, lower, upper).asValue(converter))
            }
            moved
        }
    }
}
