/**
 * 终止判据算子 / Stopping criterion operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration

/**
 * 终止判据接口，把「何时停止」从搜索骨架中独立出来。 / Stopping criterion interface that separates "when to stop" from the search skeleton.
*/
fun interface StoppingCriterion {

    /**
     * 判断是否应当终止 / Decide whether the search should stop
     *
     * @param iteration 当前迭代 / Current iteration
     * @return 是否终止 / Whether to stop
    */
    operator fun invoke(iteration: Iteration): Boolean
}

/** 永不主动终止（由其他判据组合使用）/ Never stops on its own (used through composition) */
data object NeverStop : StoppingCriterion {
    override fun invoke(iteration: Iteration): Boolean = false
}

/**
 * 迭代次数上限 / Iteration count limit
 *
 * @property limit 最大迭代次数 / Maximum iteration count
*/
data class IterationLimit(
    val limit: UInt64
) : StoppingCriterion {
    override fun invoke(iteration: Iteration): Boolean {
        return iteration.iteration >= limit
    }
}

/**
 * 连续无改进次数上限 / Consecutive no-improvement limit
 *
 * @property limit 最大连续无改进次数 / Maximum consecutive no-improvement iterations
*/
data class NotBetterLimit(
    val limit: UInt64
) : StoppingCriterion {
    override fun invoke(iteration: Iteration): Boolean {
        return iteration.notBetterIteration >= limit
    }
}

/**
 * 墙钟时间上限 / Wall-clock time limit
 *
 * @property limit 最大已用时间 / Maximum elapsed time
*/
data class TimeLimit(
    val limit: Duration
) : StoppingCriterion {
    override fun invoke(iteration: Iteration): Boolean {
        return iteration.time >= limit
    }
}

/**
 * 任一判据命中即终止 / Stop when any of the criteria is met
 *
 * @param other 另一个判据 / The other criterion
 * @return 组合后的判据 / Combined criterion
*/
infix fun StoppingCriterion.or(other: StoppingCriterion): StoppingCriterion {
    return StoppingCriterion { iteration -> this(iteration) || other(iteration) }
}

/**
 * 所有判据命中才终止 / Stop only when all criteria are met
 *
 * @param other 另一个判据 / The other criterion
 * @return 组合后的判据 / Combined criterion
*/
infix fun StoppingCriterion.and(other: StoppingCriterion): StoppingCriterion {
    return StoppingCriterion { iteration -> this(iteration) && other(iteration) }
}
