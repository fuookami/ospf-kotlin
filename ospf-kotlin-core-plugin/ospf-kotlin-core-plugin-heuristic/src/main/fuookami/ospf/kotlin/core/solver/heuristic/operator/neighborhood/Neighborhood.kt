/**
 * 邻域算子 / Neighbourhood operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.distribution.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 邻域算子接口，为单解搜索生成邻域候选（模拟退火、爬山、邻域搜索共用）。 / Neighbourhood operator interface generating neighbour candidates for single-solution search (shared by SA, hill climbing, and neighbourhood search).
*/
interface Neighborhood {

    /**
     * 生成一个邻域解 / Generate one neighbour solution
     *
     * @param V 值类型 / Value type
     * @param iteration 当前迭代 / Current iteration
     * @param individual 当前个体 / Current individual
     * @param model 回调模型接口 / Callback model interface
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param converter 值转换器 / Value converter
     * @return 邻域解 / Neighbour solution
    */
    operator fun <V> invoke(
        iteration: Iteration,
        individual: Individual<*, V>,
        model: AbstractCallBackModelInterface<*, *, V>,
        entropy: Generator<Flt64>,
        converter: IntoValue<V>
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>
}

/**
 * 在随机维度上施加有界扰动 / Apply a bounded perturbation on random dimensions
 *
 * @param individual 当前个体 / Current individual
 * @param model 回调模型接口 / Callback model interface
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @param converter 值转换器 / Value converter
 * @param amount 扰动维度数量 / Number of perturbed dimensions
 * @param step 生成单维扰动的函数 / Function generating a per-dimension perturbation
 * @return 邻域解 / Neighbour solution
*/
private fun <V> perturb(
    individual: Individual<*, V>,
    model: AbstractCallBackModelInterface<*, *, V>,
    entropy: Generator<Flt64>,
    converter: IntoValue<V>,
    amount: Int,
    step: (Double) -> Double
): Solution<V> where V : RealNumber<V>, V : NumberField<V> {
    val solution = individual.solution.toMutableList()
    if (solution.isEmpty()) {
        return solution
    }

    val dimension = solution.size
    val target = min(amount, dimension)
    val touched = HashSet<Int>()
    var attempts = 0
    while (touched.size < target && attempts < dimension * 4) {
        touched.add(((entropy()?.toDouble() ?: 0.0) * dimension).toInt().coerceIn(0, dimension - 1))
        ++attempts
    }

    for (index in touched) {
        val token = model.tokens[index]
        val lowerBound = token.lowerBound?.value?.unwrap()?.toDouble() ?: Double.NEGATIVE_INFINITY
        val upperBound = token.upperBound?.value?.unwrap()?.toDouble() ?: Double.POSITIVE_INFINITY
        // 无有限上下界时以 1.0 作为参考宽度，避免宽度为无穷导致步长失效。
        // Fall back to a reference width of 1.0 when the bounds are not finite, so an infinite width cannot invalidate the step.
        val width = if (lowerBound.isFinite() && upperBound.isFinite()) upperBound - lowerBound else 1.0
        val current = converter.fromValue(solution[index]).toDouble()
        val sampledStep = step(width).let { if (it.isNaN()) 0.0 else it }
        solution[index] = converter.intoValue(Flt64((current + sampledStep).coerceIn(lowerBound, upperBound)))
    }
    return solution
}

/**
 * 均匀随机扰动 / Uniform random perturbation
 *
 * @property amount 扰动维度数量 / Number of perturbed dimensions
*/
data class RandomPerturbation(
    val amount: Int = 1
) : Neighborhood {
    override operator fun <V> invoke(
        iteration: Iteration,
        individual: Individual<*, V>,
        model: AbstractCallBackModelInterface<*, *, V>,
        entropy: Generator<Flt64>,
        converter: IntoValue<V>
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V> {
        return perturb(individual, model, entropy, converter, amount) { width ->
            ((entropy()?.toDouble() ?: 0.5) - 0.5) * width
        }
    }
}

/**
 * 高斯扰动 / Gaussian perturbation
 *
 * @property amount 扰动维度数量 / Number of perturbed dimensions
 * @property standardDeviation 标准差（相对变量宽度的比例）/ Standard deviation as a ratio of the variable width
*/
data class GaussianPerturbation(
    val amount: Int = 1,
    val standardDeviation: Flt64 = Flt64(0.1)
) : Neighborhood {
    override operator fun <V> invoke(
        iteration: Iteration,
        individual: Individual<*, V>,
        model: AbstractCallBackModelInterface<*, *, V>,
        entropy: Generator<Flt64>,
        converter: IntoValue<V>
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V> {
        return perturb(individual, model, entropy, converter, amount) { width ->
            val noise = when (val sampled = Gaussian(standardDeviation = standardDeviation)(entropy)) {
                is Ok -> sampled.value.toDouble()
                else -> 0.0
            }
            noise * width
        }
    }
}

/**
 * 莱维扰动 / Lévy perturbation
 *
 * @property amount 扰动维度数量 / Number of perturbed dimensions
 * @property alpha 稳定性参数 / Stability parameter
*/
data class LevyPerturbation(
    val amount: Int = 1,
    val alpha: Flt64 = Flt64(1.5)
) : Neighborhood {
    override operator fun <V> invoke(
        iteration: Iteration,
        individual: Individual<*, V>,
        model: AbstractCallBackModelInterface<*, *, V>,
        entropy: Generator<Flt64>,
        converter: IntoValue<V>
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V> {
        return perturb(individual, model, entropy, converter, amount) { width ->
            val noise = when (val sampled = Levy(alpha)(entropy)) {
                is Ok -> sampled.value.toDouble()
                else -> 0.0
            }
            noise * width * 0.1
        }
    }
}
