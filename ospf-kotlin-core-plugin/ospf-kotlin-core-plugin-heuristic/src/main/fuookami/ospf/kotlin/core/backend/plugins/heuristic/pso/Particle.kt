package fuookami.ospf.kotlin.core.backend.plugins.heuristic.pso

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*

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
        model: AbstractCallBackModelInterface<*, V>
    ): Particle<V> {
        val newPosition = (0..<size).map {
            val newPosition = position[it] + velocity[it]
            val token = model.tokens[it]
            newPosition.coerceIn(token.lowerBound!!.value.unwrap(), token.upperBound!!.value.unwrap())
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
