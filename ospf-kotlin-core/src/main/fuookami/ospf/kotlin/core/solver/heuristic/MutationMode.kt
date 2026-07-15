/**
 * 变异模式接口与实现
 * Mutation mode interface and implementations
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.functional.Generator

/**
 * 从范围中获取有限的变异率，若无法确定则返回默认值 0。
 * Get a finite mutation rate from the range, defaulting to 0 if undetermined.
 *
 * 使用范围的上界作为变异率，若上界不存在则返回 Flt64.zero。
 * Uses the upper bound of the range as the mutation rate; returns Flt64.zero if the upper bound is absent.
 *
 * @param range 变异率的值范围 / The value range for mutation rate
 * @return 变异率 / The mutation rate
*/
private fun finiteMutationRateOrDefault(range: ValueRange<Flt64>): Flt64 {
    return range.upperBound.value.unwrapOrNull() ?: Flt64.zero
}

/**
 * 在范围内随机生成变异率，若范围或随机数不可用则返回 null。
 * Generate a random mutation rate within the range, returning null if the range or random value is unavailable.
 *
 * 通过在下界与上界之间随机插值来计算变异率。
 * Calculates the mutation rate by interpolating randomly between the lower and upper bounds.
 *
 * @param range 变异率的值范围 / The value range for mutation rate
 * @param randomGenerator 随机数生成器（产生 [0,1) 的 Flt64）/ Random number generator producing Flt64 in [0,1)
 * @return 随机变异率，或 null / The random mutation rate, or null
*/
private fun randomMutationRateOrNull(
    range: ValueRange<Flt64>,
    randomGenerator: Generator<Flt64>
): Flt64? {
    val lowerBound = range.lowerBound.value.unwrapOrNull() ?: return null
    val diff = range.diffOrNull?.unwrapOrNull() ?: return null
    val random = randomGenerator() ?: return null
    return lowerBound + random * diff
}

/**
 * 根据权重在范围内计算变异率，若范围不可用则返回 null。
 * Calculate mutation rate within the range based on a weight, returning null if the range is unavailable.
 *
 * 通过权重在下界与上界之间进行线性插值来计算变异率。
 * Calculates the mutation rate by linearly interpolating between the lower and upper bounds using the weight.
 *
 * @param range 变异率的值范围 / The value range for mutation rate
 * @param weight 插值权重，通常在 [0,1] 之间 / Interpolation weight, typically in [0,1]
 * @return 加权变异率，或 null / The weighted mutation rate, or null
*/
private fun weightedMutationRateOrNull(
    range: ValueRange<Flt64>,
    weight: Flt64
): Flt64? {
    val lowerBound = range.lowerBound.value.unwrapOrNull() ?: return null
    val diff = range.diffOrNull?.unwrapOrNull() ?: return null
    return lowerBound + weight * diff
}

/**
 * 变异模式接口，定义如何为种群中的个体计算变异率。
 * Mutation mode interface, defining how to calculate mutation rates for individuals in the population.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
*/
interface MutationMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {

    /**
     * 为种群中的每个个体计算变异率。
     * Calculate mutation rate for each individual in the population.
     *
     * @param T 个体类型 / Individual type
     * @param iteration 当前迭代 / Current iteration
     * @param population 种群个体列表 / Population individual list
     * @param weights 权重列表 / Weight list
     * @param model 回调模型接口 / Callback model interface
     * @param mutationRateRange 变异率范围 / Mutation rate range
     * @return 每个个体的变异率 / Mutation rate for each individual
    */
    operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Flt64>
}

/**
 * 静态变异模式，使用固定变异率。
 * Static mutation mode, using a fixed mutation rate.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property mutationRate 固定变异率（可选）/ Fixed mutation rate (optional)
*/
data class StaticMutationMode<ObjValue, V>(
    val mutationRate: Flt64? = null
) : MutationMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {

    /**
     * 对个体执行变异操作。
     * Apply mutation operation to an individual.
     *
     * @param individual 待变异的个体 / Individual to mutate
     * @return 变异后的个体 / Mutated individual
    */
    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Flt64> {
        return List(population.size) {
            mutationRate?.coerceIn(mutationRateRange) ?: mutationRateRange.upperBound.value.unwrap()
        }
    }
}

/**
 * 随机变异模式，在范围内随机生成变异率。
 * Random mutation mode, randomly generating mutation rates within range.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property randomGenerator 随机数生成器 / Random number generator
*/
data class RandomMutationMode<ObjValue, V>(
    private val randomGenerator: Generator<Flt64>
) : MutationMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Flt64> {
        return List(population.size) {
            randomMutationRateOrNull(
                range = mutationRateRange,
                randomGenerator = randomGenerator
            ) ?: finiteMutationRateOrDefault(mutationRateRange)
        }
    }
}

/**
 * 自适应动态变异模式，根据权重差异动态调整变异率。
 * Adaptive dynamic mutation mode, dynamically adjusting mutation rate based on weight differences.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
*/
class AdaptiveDynamicMutationMode<ObjValue, V> : MutationMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Flt64> {
        val weight = (weights.max() - weights.min()) / weights.max()
        return List(population.size) {
            weightedMutationRateOrNull(
                range = mutationRateRange,
                weight = weight
            ) ?: finiteMutationRateOrDefault(mutationRateRange)
        }
    }
}
