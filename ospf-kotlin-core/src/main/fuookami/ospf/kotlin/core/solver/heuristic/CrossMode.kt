/**
 * 交叉模式接口与实现
 * Crossover mode interface and implementations
 */
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

/**
 * 交叉模式接口，定义如何从种群中选择父代组合。
 * Crossover mode interface, defining how to select parent combinations from the population.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 */
interface CrossMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    /** 交叉选择方法枚举，支持加权/随机、环形/双向策略。 / Crossover selection method enum, supporting weighted/random and ring/bidirectional strategies. */
    enum class Method {
        /** 加权环形选择 / Weighted ring selection */
        WeightedRing,
        /** 加权双向选择 / Weighted bidirectional selection */
        WeightedBidirectional,
        /** 随机环形选择 / Random ring selection */
        RandomRing,
        /** 随机双向选择 / Random bidirectional selection */
        RandomBidirectional
    }

    /**
     * 为种群中的每个个体选择父代组合。
     * Select parent combinations for each individual in the population.
     *
     * @param T 个体类型 / Individual type
     * @param iteration 当前迭代 / Current iteration
     * @param population 种群个体列表 / Population individual list
     * @param weights 权重列表 / Weight list
     * @param model 回调模型接口 / Callback model interface
     * @param parentAmountRange 父代数量范围 / Parent amount range
     * @return 每个个体对应的父代列表 / Parent list for each individual
     */
    operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<List<T>>
}

/**
 * 单父代交叉模式，每个个体单独作为父代。
 * Single parent crossover mode, each individual acts as a parent independently.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 */
class OneParentCrossMode<ObjValue, V> : CrossMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<List<T>> {
        return population.map { listOf(it) }
    }
}

/**
 * 双父代交叉模式，使用指定方法选择两个父代进行交叉。
 * Two-parent crossover mode, using specified method to select two parents for crossover.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property method 交叉方法 / Crossover method
 */
class TwoParentCrossMode<ObjValue, V>(
    val method: CrossMode.Method
) : CrossMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<List<T>> {
        return when (method) {
            CrossMode.Method.WeightedRing -> {
                val weighted = population.withIndex().sortedByDescending { weights[it.index] }
                population.indices.map { i ->
                    weighted
                        .subList(i, i + 2)
                        .map { it.value }
                } + listOf(listOf(weighted.last().value, weighted.first().value))
            }

            CrossMode.Method.WeightedBidirectional -> {
                val weighted = population.withIndex().sortedByDescending { weights[it.index] }
                (0 until (population.size / 2 + 1)).mapNotNull { i ->
                    val j = population.lastIndex - i
                    if (i <= j) {
                        listOf(weighted[i].value, weighted[j].value)
                    } else {
                        null
                    }
                }
            }

            CrossMode.Method.RandomRing -> {
                val shuffled = population.shuffled()
                population.indices.map {
                    shuffled.subList(it, it + 2)
                } + listOf(listOf(shuffled.last(), shuffled.first()))
            }

            CrossMode.Method.RandomBidirectional -> {
                val shuffled = population.shuffled()
                (0 until (population.size / 2 + 1)).mapNotNull { i ->
                    val j = population.lastIndex - i
                    if (i <= j) {
                        listOf(shuffled[i], shuffled[j])
                    } else {
                        null
                    }
                }
            }
        }
    }
}

/**
 * 多父代交叉模式，支持动态计算父代数量。
 * Multi-parent crossover mode with dynamic parent amount calculation.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property method 交叉方法 / Crossover method
 * @property parentAmountCalculator 父代数量计算器 / Parent amount calculator
 */
class MultiParentCrossMode<ObjValue, V>(
    val method: CrossMode.Method,
    val parentAmountCalculator: (Iteration, List<Flt64>, ValueRange<UInt64>) -> UInt64
) : CrossMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        /**
         * 以随机父代数量计算器创建多父代交叉模式。
         * Create a multi-parent crossover mode with random parent amount calculator.
         *
         * @param ObjValue 目标值类型 / Objective value type
         * @param V 值类型 / Value type
         * @param method 交叉方法 / Crossover method
         * @param randomGenerator 随机数生成器 / Random number generator
         * @return 多父代交叉模式实例 / Multi-parent crossover mode instance
         */
        operator fun <ObjValue, V> invoke(
            method: CrossMode.Method,
            randomGenerator: Generator<Flt64>
        ): MultiParentCrossMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
            return MultiParentCrossMode(method) { _, _, range ->
                range.fixedValue
                    ?: (range.lowerBound.value.unwrap() + (randomGenerator()!! * range.diff.unwrap().toFlt64()).round().toUInt64())
            }
        }
    }

    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<List<T>> {
        val parentAmount = parentAmountCalculator(iteration, weights, parentAmountRange)
        return if (parentAmount == UInt64.zero) {
            OneParentCrossMode<ObjValue, V>()(
                iteration = iteration,
                population = population,
                weights = weights,
                model = model,
                parentAmountRange = parentAmountRange
            )
        } else {
            when (method) {
                CrossMode.Method.WeightedRing -> {
                    val weighted = population.withIndex().sortedByDescending { weights[it.index] }
                    population.indices.map { i ->
                        parentAmount.indices.map { j ->
                            weighted[(i + j.toInt()) % weighted.size].value
                        }
                    }
                }

                CrossMode.Method.WeightedBidirectional -> {
                    val weighted = population.withIndex().sortedByDescending { weights[it.index] }
                    (0 until (population.size / 2 + 1)).mapNotNull { i0 ->
                        val forwardAmount = if (parentAmount % UInt64.two == UInt64.zero) {
                            parentAmount / UInt64.two
                        } else {
                            parentAmount / UInt64.two + UInt64.one
                        }
                        val backwardAmount = parentAmount - forwardAmount
                        val i1 = i0 + forwardAmount.toInt()
                        val j0 = population.size - i0 - backwardAmount.toInt()
                        val j1 = population.size - i0
                        if (i1 <= j0) {
                            (weighted.subList(i0, i1) + weighted.subList(j0, j1)).map {
                                it.value
                            }
                        } else {
                            null
                        }
                    }
                }

                CrossMode.Method.RandomRing -> {
                    val shuffled = population.shuffled()
                    population.indices.map { i ->
                        parentAmount.indices.map { j ->
                            shuffled[(i + j.toInt()) % shuffled.size]
                        }
                    }
                }

                CrossMode.Method.RandomBidirectional -> {
                    val shuffled = population.shuffled()
                    (0 until (population.size / 2 + 1)).mapNotNull { i0 ->
                        val forwardAmount = if (parentAmount % UInt64.two == UInt64.zero) {
                            parentAmount / UInt64.two
                        } else {
                            parentAmount / UInt64.two + UInt64.one
                        }
                        val backwardAmount = parentAmount - forwardAmount
                        val i1 = i0 + forwardAmount.toInt()
                        val j0 = population.size - i0 - backwardAmount.toInt()
                        val j1 = population.size - i0
                        if (i1 <= j0) {
                            (shuffled.subList(i0, i1) + shuffled.subList(j0, j1))
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }
}

/**
 * 自适应多父代交叉模式，根据权重动态调整父代数量。
 * Adaptive multi-parent crossover mode, dynamically adjusting parent count based on weights.
 */
data object AdaptiveMultiParentCrossMode {
    /**
     * 创建自适应多父代交叉模式。
     * Create an adaptive multi-parent crossover mode.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param method 交叉方法 / Crossover method
     * @return 多父代交叉模式实例 / Multi-parent crossover mode instance
     */
    operator fun <ObjValue, V> invoke(
        method: CrossMode.Method
    ): MultiParentCrossMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return MultiParentCrossMode(method) { _, weights, range ->
            if (range.fixedValue != null) {
                return@MultiParentCrossMode range.fixedValue!!
            }

            val weight = (weights.max() - weights.min()) / weights.max()
            (range.lowerBound.value.unwrap() + (weight * range.diff.unwrap().toFlt64()).round().toUInt64())
        }
    }
}
