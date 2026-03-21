package fuookami.ospf.kotlin.core.backend.solver.heuristic

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*

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
