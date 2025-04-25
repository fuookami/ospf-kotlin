package fuookami.ospf.kotlin.core.backend.solver.heuristic

import kotlin.time.*
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.utils.math.*

interface AbstractHeuristicPolicy {
    fun finished(iteration: Iteration): Boolean
}

abstract class HeuristicPolicy(
    val iterationLimit: UInt64 = UInt64.maximum,
    val notBetterIterationLimit: UInt64 = UInt64.maximum,
    val timeLimit: Duration = 30.minutes,
) : AbstractHeuristicPolicy {
    override fun finished(iteration: Iteration): Boolean {
        return iteration.iteration > iterationLimit
                || iteration.notBetterIteration > notBetterIterationLimit
                || iteration.time > timeLimit
    }
}
