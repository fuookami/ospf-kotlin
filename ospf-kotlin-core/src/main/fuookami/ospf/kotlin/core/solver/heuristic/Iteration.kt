@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 启发式迭代计数器 / Heuristic iteration counter */
package fuookami.ospf.kotlin.core.solver.heuristic

import kotlin.time.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 迭代计数器，跟踪总迭代次数、无改进迭代次数和运行时间。
 * Iteration counter, tracking total iterations, no-improvement iterations, and elapsed time.
 *
 * @property _iteration 当前迭代次数 / Current iteration count
 * @property _notBetterIteration 连续无改进迭代次数 / Consecutive no-improvement iteration count
 * @property begin 迭代开始时间 / Iteration start time
*/
@OptIn(ExperimentalTime::class)
open class Iteration(
    private var _iteration: UInt64 = UInt64.zero,
    private var _notBetterIteration: UInt64 = UInt64.zero,
    private val begin: Instant = Clock.System.now()
) {

    /** 当前迭代次数 / Current iteration count */
    val iteration by ::_iteration

    /** 连续无改进迭代次数 / Consecutive no-improvement iteration count */
    val notBetterIteration by ::_notBetterIteration

    /** 已用时间 / Elapsed time */
    val time get() = Clock.System.now() - begin

    /**
     * 推进迭代计数器。
     * Advance the iteration counter.
     *
     * @param better 本轮是否有改进 / Whether this round improved
    */
    open fun next(better: Boolean) {
        _iteration += UInt64.one
        if (better) {
            _notBetterIteration = UInt64.zero
        } else {
            _notBetterIteration += UInt64.one
        }
    }
}
