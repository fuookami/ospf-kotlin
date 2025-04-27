package fuookami.ospf.kotlin.core.backend.solver.heuristic

import kotlin.time.*
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

interface AbstractHeuristicPolicy {
    fun coerceIn(
        iteration: Iteration,
        index: Int,
        value: Flt64,
        model: AbstractCallBackModelInterface<*, *>
    ): Flt64 {
        val token = model.tokens[index]
        return value.coerceIn(token.lowerBound!!.value.unwrap(), token.upperBound!!.value.unwrap())
    }

    fun update(
        iteration: Iteration,
        better: Boolean,
        bestIndividual: Individual<*>,
        goodIndividuals: List<Individual<*>>,
        populations: List<List<Individual<*>>>,
        model: AbstractCallBackModelInterface<*, *>
    ) {}

    fun finished(iteration: Iteration): Boolean
}

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
