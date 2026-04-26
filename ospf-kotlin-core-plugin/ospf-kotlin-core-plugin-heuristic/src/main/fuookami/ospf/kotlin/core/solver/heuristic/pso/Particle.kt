package fuookami.ospf.kotlin.core.solver.heuristic.pso

import fuookami.ospf.kotlin.core.solver.heuristic.AbstractHeuristicPolicy
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.utils.functional.ifNull
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.Order

data class Particle<V>(
    override val fitness: V,
    val position: List<Flt64>,
    val velocity: List<Flt64>,
    val currentBest: Particle<V>? = null
) : Individual<V> {
    init {
        assert(position.size == velocity.size)
    }

    override val solution by ::position
    val size by position::size

    fun new(
        newVelocity: List<Flt64>,
        iteration: Iteration,
        policy: AbstractHeuristicPolicy,
        model: AbstractCallBackModelInterface<*, V>
    ): Particle<V> {
        val newPosition = (0..<size).map {
            val newPosition = position[it] + velocity[it]
            policy.coerceIn(
                iteration = iteration,
                index = it,
                value = newPosition,
                model = model
            )
        }
        val newFitness = model.objective(newPosition).ifNull { model.defaultObjective }
        return if (currentBest != null) {
            Particle(
                fitness = newFitness,
                position = newPosition,
                velocity = newVelocity,
                currentBest = if (model.compareObjective(newFitness, currentBest.fitness) is Order.Less) {
                    null
                } else {
                    currentBest
                }
            )
        } else {
            Particle(
                fitness = newFitness,
                position = newPosition,
                velocity = newVelocity,
                currentBest = if (model.compareObjective(newFitness, fitness) is Order.Less) {
                    null
                } else {
                    this
                }
            )
        }
    }
}



