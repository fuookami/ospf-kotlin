/**
 * 启发式迭代计数器
 * Heuristic iteration counter
 */
@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.heuristic

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 迭代计数器，跟踪总迭代次数、无改进迭代次数和运行时间。
 * Iteration counter, tracking total iterations, no-improvement iterations, and elapsed time.
 */
@OptIn(ExperimentalTime::class)
open class Iteration(
    private var _iteration: UInt64 = UInt64.zero,
    private var _notBetterIteration: UInt64 = UInt64.zero,
    private val begin: Instant = Clock.System.now()
) {
    val iteration by ::_iteration
    val notBetterIteration by ::_notBetterIteration
    val time get() = Clock.System.now() - begin

    open fun next(better: Boolean) {
        _iteration += UInt64.one
        if (better) {
            _notBetterIteration = UInt64.zero
        } else {
            _notBetterIteration += UInt64.one
        }
    }
}
