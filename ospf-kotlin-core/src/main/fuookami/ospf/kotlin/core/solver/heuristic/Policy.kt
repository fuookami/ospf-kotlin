@file:OptIn(kotlin.time.ExperimentalTime::class)
/** 启发式策略接口与基类 / Heuristic policy interface and base class */
package fuookami.ospf.kotlin.core.solver.heuristic

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 启发式策略的抽象接口，定义值约束、状态更新和终止判断。
 * Abstract interface for heuristic policies, defining value coercion, state updates, and termination checks.
 */
interface AbstractHeuristicPolicy {
    /**
     * 将 Flt64 值约束到变量边界内。
     * Coerce a Flt64 value within the variable bounds.
     *
     * @param iteration 当前迭代 / Current iteration
     * @param index 变量索引 / Variable index
     * @param value 待约束的值 / Value to coerce
     * @param model 回调模型接口 / Callback model interface
     * @return 约束后的值 / Coerced value
     */
    fun coerceIn(
        iteration: Iteration,
        index: Int,
        value: Flt64,
        model: AbstractCallBackModelInterface<*, *, *>
    ): Flt64 {
        val token = model.tokens[index]
        return value.coerceIn(token.lowerBound!!.value.unwrap(), token.upperBound!!.value.unwrap())
    }

    /**
     * 将泛型值约束到变量边界内。
     * Coerce a generic value within the variable bounds.
     *
     * @param V 值类型 / Value type
     * @param iteration 当前迭代 / Current iteration
     * @param index 变量索引 / Variable index
     * @param value 待约束的值 / Value to coerce
     * @param model 回调模型接口 / Callback model interface
     * @param converter 值转换器 / Value converter
     * @return 约束后的值 / Coerced value
     */
    fun <V> coerceIn(
        iteration: Iteration,
        index: Int,
        value: V,
        model: AbstractCallBackModelInterface<*, *, *>,
        converter: IntoValue<V>
    ): V where V : RealNumber<V>, V : NumberField<V> {
        val flt64Value = converter.fromValue(value)
        val fixed = coerceIn(iteration, index, flt64Value, model)
        return converter.intoValue(fixed)
    }

    /**
     * 更新启发式策略状态。
     * Update heuristic policy state.
     *
     * @param iteration 当前迭代 / Current iteration
     * @param better 本轮是否有改进 / Whether this round improved
     * @param bestIndividual 当前最优个体 / Current best individual
     * @param goodIndividuals 优良个体列表 / Good individuals list
     * @param populations 种群列表 / Populations list
     * @param model 回调模型接口 / Callback model interface
     */
    fun update(
        iteration: Iteration,
        better: Boolean,
        bestIndividual: Individual<*, *>,
        goodIndividuals: List<Individual<*, *>>,
        populations: List<List<Individual<*, *>>>,
        model: AbstractCallBackModelInterface<*, *, *>
    ) {
    }

    /**
     * 判断启发式搜索是否终止。
     * Check whether the heuristic search should terminate.
     *
     * @param iteration 当前迭代 / Current iteration
     * @return 是否终止 / Whether to terminate
     */
    fun finished(iteration: Iteration): Boolean
}

/**
 * 启发式策略基类，提供基于迭代次数和时间的终止条件。
 * Base class for heuristic policies, providing termination conditions based on iteration count and time.
 *
 * @property iterationLimit 最大迭代次数 / Maximum iteration count
 * @property notBetterIterationLimit 最大无改进迭代次数 / Maximum no-improvement iteration count
 * @property timeLimit 时间限制 / Time limit
 */
abstract class HeuristicPolicy(
    val iterationLimit: UInt64 = UInt64.maximum,
    val notBetterIterationLimit: UInt64 = UInt64.maximum,
    val timeLimit: Duration = 30.minutes
) : AbstractHeuristicPolicy {
    override fun finished(iteration: Iteration): Boolean {
        return iteration.iteration > iterationLimit
                || iteration.notBetterIteration > notBetterIterationLimit
                || iteration.time > timeLimit
    }
}
