package fuookami.ospf.kotlin.core.solver.heuristic.pso

import fuookami.ospf.kotlin.core.solver.heuristic.AbstractHeuristicPolicy
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.ifNull
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.Order

data class Particle<V>(
    override val fitness: V,
    override val solution: List<V>,
    val velocity: List<Flt64>,
    val currentBest: Particle<V>? = null,
    // Safe when V=Flt64 (used by PSO/MulObjPSO typealiases); non-Flt64 callers must provide explicit converter
    private val converter: IntoValue<V> = @Suppress("UNCHECKED_CAST") (IntoValue.Flt64 as IntoValue<V>)
) : Individual<V> where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    init {
        assert(solution.size == velocity.size)
    }

    val size by solution::size

    fun new(
        newVelocity: List<Flt64>,
        iteration: Iteration,
        policy: AbstractHeuristicPolicy,
        model: AbstractCallBackModelInterface<*, V>
    ): Particle<V> {
        val newPosition = (0..<size).map {
            val posFlt64 = converter.fromValue(solution[it])
            val newFlt64 = posFlt64 + velocity[it]
            converter.intoValue(policy.coerceIn(
                iteration = iteration,
                index = it,
                value = newFlt64,
                model = model
            ))
        }
        val newFitness = model.objective(newPosition).ifNull { model.defaultObjective }
        return if (currentBest != null) {
            Particle(
                fitness = newFitness,
                solution = newPosition,
                velocity = newVelocity,
                currentBest = if (model.compareObjective(newFitness, currentBest.fitness) is Order.Less) {
                    null
                } else {
                    currentBest
                },
                converter = converter
            )
        } else {
            Particle(
                fitness = newFitness,
                solution = newPosition,
                velocity = newVelocity,
                currentBest = if (model.compareObjective(newFitness, fitness) is Order.Less) {
                    null
                } else {
                    this
                },
                converter = converter
            )
        }
    }
}



