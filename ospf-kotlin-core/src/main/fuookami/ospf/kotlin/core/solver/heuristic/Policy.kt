/**
 * 启发式策略接口与基类
 * Heuristic policy interface and base class
 */
@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.heuristic

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 启发式策略的抽象接口，定义值约束、状态更新和终止判断。
 * Abstract interface for heuristic policies, defining value coercion, state updates, and termination checks.
 */
interface AbstractHeuristicPolicy {
    fun coerceIn(
        iteration: Iteration,
        index: Int,
        value: Flt64,
        model: AbstractCallBackModelInterface<*, *, *>
    ): Flt64 {
        val token = model.tokens[index]
        return value.coerceIn(token.lowerBound!!.value.unwrap(), token.upperBound!!.value.unwrap())
    }

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

    fun update(
        iteration: Iteration,
        better: Boolean,
        bestIndividual: Individual<*, *>,
        goodIndividuals: List<Individual<*, *>>,
        populations: List<List<Individual<*, *>>>,
        model: AbstractCallBackModelInterface<*, *, *>
    ) {
    }

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
