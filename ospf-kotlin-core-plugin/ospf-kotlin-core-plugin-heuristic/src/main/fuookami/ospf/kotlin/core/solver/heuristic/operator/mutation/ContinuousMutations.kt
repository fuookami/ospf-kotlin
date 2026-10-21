/**
 * 连续变异算子 / Continuous mutation operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.distribution.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * 逐基因施加变异 / Apply a mutation gene by gene
 *
 * @param V 值类型 / Value type
 * @param solution 原始解 / Original solution
 * @param model 回调模型接口 / Callback model interface
 * @param converter 值转换器 / Value converter
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @param mutationRate 逐基因变异率 / Per-gene mutation rate
 * @param step 单基因变换函数 / Per-gene transformation function
 * @return 变异后的解 / Mutated solution
*/
private fun <V> mutateGenes(
    solution: List<V>,
    model: AbstractCallBackModelInterface<*, *, V>,
    converter: IntoValue<V>,
    entropy: Generator<Flt64>,
    mutationRate: Double,
    step: (index: Int, current: Double, lower: Double?, upper: Double?) -> Double
): Solution<V> where V : RealNumber<V>, V : NumberField<V> {
    val result = solution.toMutableList()
    for (index in result.indices) {
        if (!shouldFire(entropy, mutationRate)) {
            continue
        }
        val current = result[index].asDouble()
        val lower = model.lowerBoundOf(index)
        val upper = model.upperBoundOf(index)
        result[index] = clampTo(step(index, current, lower, upper), lower, upper).asValue(converter)
    }
    return result
}

/**
 * 变量宽度，无有限上下界时退化为 1.0 / Variable width, degrading to 1.0 when the bounds are not finite
 *
 * @param lower 下界 / Lower bound
 * @param upper 上界 / Upper bound
 * @return 宽度 / Width
*/
private fun widthOf(lower: Double?, upper: Double?): Double {
    return if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
        upper - lower
    } else {
        1.0
    }
}

/**
 * 高斯变异 / Gaussian mutation
 *
 * @property sigma 标准差（占变量宽度的比例）/ Standard deviation as a ratio of the variable width
*/
data class GaussianMutation(
    val sigma: Flt64 = Flt64(0.1)
) : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val scale = sigma.toDouble()
        return mutateGenes(individual.solution, model, converter, entropy, mutationRate.toDouble()) { _, current, lower, upper ->
            val noise = when (val sampled = Gaussian(standardDeviation = Flt64.one)(entropy)) {
                is fuookami.ospf.kotlin.utils.functional.Ok -> sampled.value.toDouble()
                else -> 0.0
            }
            current + noise * scale * widthOf(lower, upper)
        }
    }
}

/**
 * 柯西变异，重尾步长有利于跳出局部最优。 / Cauchy mutation whose heavy-tailed step helps escape local optima.
 *
 * @property gamma 尺度参数（占变量宽度的比例）/ Scale parameter as a ratio of the variable width
*/
data class CauchyMutation(
    val gamma: Flt64 = Flt64(0.1)
) : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val scale = gamma.toDouble()
        return mutateGenes(individual.solution, model, converter, entropy, mutationRate.toDouble()) { _, current, lower, upper ->
            val noise = when (val sampled = Cauchy(scale = Flt64.one)(entropy)) {
                is fuookami.ospf.kotlin.utils.functional.Ok -> sampled.value.toDouble()
                else -> 0.0
            }
            current + noise * scale * widthOf(lower, upper)
        }
    }
}

/**
 * 莱维变异，用重尾稳定分布提供大跨度跳变。 / Lévy mutation using a heavy-tailed stable distribution for long jumps.
 *
 * @property alpha 稳定性参数 / Stability parameter
 * @property scale 幅值比例 / Magnitude ratio
*/
data class LevyMutation(
    val alpha: Flt64 = Flt64(1.5),
    val scale: Flt64 = Flt64(0.1)
) : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val magnitude = scale.toDouble()
        return mutateGenes(individual.solution, model, converter, entropy, mutationRate.toDouble()) { _, current, lower, upper ->
            val noise = when (val sampled = Levy(alpha)(entropy)) {
                is fuookami.ospf.kotlin.utils.functional.Ok -> sampled.value.toDouble()
                else -> 0.0
            }
            current + noise * magnitude * widthOf(lower, upper)
        }
    }
}

/** 均匀变异，在整个区间内重新采样 / Uniform mutation resampling across the whole interval */
data object UniformMutation : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        return mutateGenes(individual.solution, model, converter, entropy, mutationRate.toDouble()) { _, current, lower, upper ->
            if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                lower + entropy.unit() * (upper - lower)
            } else {
                current + (entropy.unit() - 0.5) * 2.0
            }
        }
    }
}

/** 边界变异，把基因推到变量的下界或上界 / Boundary mutation pushing a gene onto the lower or upper bound */
data object BoundaryMutation : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        return mutateGenes(individual.solution, model, converter, entropy, mutationRate.toDouble()) { _, current, lower, upper ->
            when {
                lower != null && upper != null && lower.isFinite() && upper.isFinite() ->
                    if (entropy.unit() < 0.5) lower else upper

                else -> current
            }
        }
    }
}

/**
 * 多项式变异（Deb），由分布指数控制扰动集中在边界附近的强度。 / Polynomial mutation (Deb) whose distribution index controls how strongly perturbations concentrate near the bounds.
 *
 * @property distributionIndex 分布指数 η / Distribution index η
*/
data class PolynomialMutation(
    val distributionIndex: Flt64 = Flt64(20.0)
) : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val eta = max(distributionIndex.toDouble(), 0.0)
        return mutateGenes(individual.solution, model, converter, entropy, mutationRate.toDouble()) { _, current, lower, upper ->
            if (lower == null || upper == null || !lower.isFinite() || !upper.isFinite() || upper <= lower) {
                current + (entropy.unit() - 0.5) * 2.0
            } else {
                val delta1 = (current - lower) / (upper - lower)
                val delta2 = (upper - current) / (upper - lower)
                val random = entropy.unit()
                val deltaQ = if (random <= 0.5) {
                    (2.0 * random + (1.0 - 2.0 * random) * (1.0 - delta1).pow(eta + 1.0))
                        .pow(1.0 / (eta + 1.0)) - 1.0
                } else {
                    1.0 - (2.0 * (1.0 - random) + 2.0 * (random - 0.5) * (1.0 - delta2).pow(eta + 1.0))
                        .pow(1.0 / (eta + 1.0))
                }
                current + deltaQ * (upper - lower)
            }
        }
    }
}

/**
 * 非一致变异（Michalewicz），扰动幅度随迭代推进而衰减。 / Non-uniform mutation (Michalewicz) whose perturbation magnitude decays with the iterations.
 *
 * @property decay 衰减指数 b / Decay exponent b
 * @property maxIteration 迭代上限，用于计算进度 / Iteration limit used to compute progress
*/
data class NonUniformMutation(
    val decay: Flt64 = Flt64(5.0),
    val maxIteration: UInt64 = UInt64(1000)
) : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val b = max(decay.toDouble(), 1.0e-12)
        val progress = if (maxIteration == UInt64.zero) {
            0.0
        } else {
            (iteration.iteration.toFlt64().toDouble() / maxIteration.toFlt64().toDouble()).coerceIn(0.0, 1.0)
        }
        val shrink = (1.0 - progress).pow(b)

        return mutateGenes(individual.solution, model, converter, entropy, mutationRate.toDouble()) { _, current, lower, upper ->
            if (lower == null || upper == null || !lower.isFinite() || !upper.isFinite()) {
                current + (entropy.unit() - 0.5) * 2.0
            } else {
                val random = entropy.unit()
                val delta = 1.0 - random.pow(shrink)
                if (entropy.unit() < 0.5) {
                    current + delta * (upper - current)
                } else {
                    current - delta * (current - lower)
                }
            }
        }
    }
}

/**
 * 混沌变异，先用 Logistic 映射打散均匀随机数再映射到变量区间 / Chaotic mutation scrambling the uniform sample through the logistic map before mapping it onto the variable interval
 *
 * @property iterations Logistic 映射迭代次数 / Number of logistic-map iterations
*/
data class ChaoticMutation(
    val iterations: Int = 3
) : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        return mutateGenes(individual.solution, model, converter, entropy, mutationRate.toDouble()) { _, current, lower, upper ->
            var chaotic = entropy.unit().coerceIn(1.0e-6, 1.0 - 1.0e-6)
            repeat(max(iterations, 1)) {
                chaotic = 4.0 * chaotic * (1.0 - chaotic)
            }
            if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                lower + chaotic * (upper - lower)
            } else {
                current + (chaotic - 0.5) * 2.0
            }
        }
    }
}
