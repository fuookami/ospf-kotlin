package fuookami.ospf.kotlin.core.backend.plugins.heuristic.pso

import kotlin.time.*
import kotlin.random.*
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*

data class Particle<V>(
    val fitness: V?,
    val position: List<Flt64>,
    val velocity: List<Flt64>,
    val currentBest: Particle<V>? = null
) {
    init {
        assert(position.size == velocity.size)
    }

    val size by position::size

    fun new(
        newVelocity: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>
    ): Particle<V> {
        val newPosition = (0..<size).map {
            val newPosition = position[it] + velocity[it]
            val token = model.tokens[it]
            if (newPosition gr token.upperBound!!.value.unwrap()) {
                token.upperBound!!.value.unwrap()
            } else if (newPosition ls token.lowerBound!!.value.unwrap()) {
                token.lowerBound!!.value.unwrap()
            } else {
                newPosition
            }
        }
        val newFitness = if (model.constraintSatisfied(newPosition) == true) {
            model.objective(newPosition)
        } else {
            null
        }
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

interface PSOPolicy<V> {
    fun transformPartial(
        particle: Particle<V>,
        bestPartial: Particle<V>,
        model: AbstractCallBackModelInterface<*, V>
    ): Particle<V>

    fun finished(iteration: Iteration): Boolean
}

/**
 *
 * @param c1: local learning factor
 * @param c2: global learning factor
 */
open class CommonPSOPolicy<V>(
    val w: Flt64 = Flt64(0.4),
    val c1: Flt64 = Flt64.two,
    val c2: Flt64 = Flt64.two,
    val maxVelocity: Flt64 = Flt64(10000.0),
    val iterationLimit: UInt64 = UInt64.maximum,
    val notBetterIterationLimit: UInt64 = UInt64.maximum,
    val timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Flt64(Random.nextDouble()) }
) : PSOPolicy<V> {
    override fun transformPartial(
        particle: Particle<V>,
        bestPartial: Particle<V>,
        model: AbstractCallBackModelInterface<*, V>
    ): Particle<V> {
        return particle.new(
            (0 until particle.size).map {
                val newVelocity = w * particle.velocity[it] +
                        c1 * randomGenerator()!! * (particle.currentBest?.position?.get(it)
                    ?.let { pos -> pos - particle.position[it] } ?: Flt64.zero) +
                        c2 * randomGenerator()!! * (bestPartial.position[it] - particle.position[it])
                if (newVelocity gr maxVelocity) {
                    maxVelocity
                } else if (newVelocity ls -maxVelocity) {
                    -maxVelocity
                } else {
                    newVelocity
                }
            },
            model
        )
    }

    override fun finished(iteration: Iteration): Boolean {
        return iteration.iteration > iterationLimit
                || iteration.notBetterIteration > notBetterIterationLimit
                || iteration.time > timeLimit
    }
}

class ParticleSwarmOptimizationAlgorithm<Obj, V>(
    val particleAmount: UInt64 = UInt64(100UL),
    val solutionAmount: UInt64 = UInt64.one,
    val policy: PSOPolicy<V> = CommonPSOPolicy()
) {
    operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, V>,
        initialVelocityGenerator: Extractor<Flt64, UInt64> = { Flt64(Random.nextDouble(2.0) - 1.0) }
    ): List<Pair<Solution, V?>> {
        val iteration = Iteration()
        val initialSolutions = model.initialSolutions(particleAmount)
        var particles = initialSolutions
            .map {
                Particle(
                    if (model.constraintSatisfied(it) != true) {
                        model.objective(it)
                    } else {
                        null
                    }, it, it.indices.map { index -> initialVelocityGenerator(UInt64(index)) })
            }
            .sortedWithPartialThreeWayComparator { lhs, rhs -> model.compareObjective(lhs.fitness, rhs.fitness) }
        var bestParticles = particles.first()
        val goodParticles = particles.subList(0, min(UInt64(particles.size), solutionAmount).toInt()).toMutableList()

        while (!policy.finished(iteration)) {
            val newParticles = particles
                .map { policy.transformPartial(it, bestParticles, model) }
                .sortedWithPartialThreeWayComparator { lhs, rhs -> model.compareObjective(lhs.fitness, rhs.fitness) }
            val newBestParticle = newParticles.first()
            particles = newParticles
            if (model.compareObjective(newBestParticle.fitness, bestParticles.fitness) is Order.Less) {
                bestParticles = newBestParticle
            }
            refreshGoodParticles(goodParticles, newParticles, model)
            model.flush()
        }

        return goodParticles.map { Pair(it.position, it.fitness) }
    }

    private fun refreshGoodParticles(
        goodParticles: MutableList<Particle<V>>,
        newParticles: List<Particle<V>>,
        model: AbstractCallBackModelInterface<Obj, V>
    ) {
        var i = 0
        var j = 0
        while (i != goodParticles.size && j != newParticles.size) {
            if (model.compareObjective(newParticles[j].fitness, goodParticles[i].fitness) is Order.Less) {
                goodParticles.add(i, newParticles[j])
                ++i
                ++j
            } else {
                ++i
            }
        }
        if (j != newParticles.size) {
            goodParticles.addAll(
                newParticles.subList(
                    j,
                    minOf(newParticles.size, maxOf(j, solutionAmount.toInt() - goodParticles.size))
                )
            )
        }
        if (UInt64(goodParticles.size) > solutionAmount) {
            (UInt64.zero until UInt64(goodParticles.size) - solutionAmount).forEach { _ ->
                goodParticles.removeLast()
            }
        }
    }
}

typealias PSO = ParticleSwarmOptimizationAlgorithm<Flt64, Flt64>
typealias MulObjPSO = ParticleSwarmOptimizationAlgorithm<MulObj, List<Flt64>>
