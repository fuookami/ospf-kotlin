/**
 * 变异模式接口与实现
 * Mutation mode interface and implementations
 */
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

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
            mutationRateRange.lowerBound.value.unwrap() + randomGenerator()!! * mutationRateRange.diff.unwrap()
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
            mutationRateRange.lowerBound.value.unwrap() + weight * mutationRateRange.diff.unwrap()
        }
    }
}
