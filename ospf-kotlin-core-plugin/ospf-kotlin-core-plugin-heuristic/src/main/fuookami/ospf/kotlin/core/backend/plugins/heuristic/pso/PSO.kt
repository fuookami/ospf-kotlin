package fuookami.ospf.kotlin.core.backend.plugins.heuristic.pso

import kotlin.time.*
import kotlin.random.*
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*

data class Particle<V>(
    val fitness: V,
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

interface AbstractPSOPolicy<V> : AbstractHeuristicPolicy {
    fun transformPartial(
        particle: Particle<V>,
        bestPartial: Particle<V>,
        model: AbstractCallBackModelInterface<*, V>
    ): Particle<V>
}

/**
 *
 * @property c1         local learning factor
 * @property c2         global learning factor
 */
open class PSOPolicy<V>(
    val w: Flt64 = Flt64(0.4),
    val c1: Flt64 = Flt64.two,
    val c2: Flt64 = Flt64.two,
    val maxVelocity: Flt64 = Flt64(10000.0),
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
) : HeuristicPolicy(iterationLimit, notBetterIterationLimit, timeLimit), AbstractPSOPolicy<V> {
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
}

class ParticleSwarmOptimizationAlgorithm<Obj, V>(
    val particleAmount: UInt64 = UInt64(100UL),
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractPSOPolicy<V> = PSOPolicy()
) {
    operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, V>,
        initialVelocityGenerator: Extractor<Flt64, UInt64> = { Random.nextFlt64(Flt64.two) - Flt64.one },
        runningCallBack: ((Iteration, Particle<V>, List<Particle<V>>) -> Try)? = null
    ): List<Pair<Solution, V?>> {
        val iteration = Iteration()
        val initialSolutions = model.initialSolutions(particleAmount)
        var particles = initialSolutions
            .map {
                Particle(
                    fitness = model.objective(it).ifNull { model.defaultObjective },
                    position = it,
                    velocity = it.indices.map { index -> initialVelocityGenerator(UInt64(index)) }
                )
            }
            .sortedWithPartialThreeWayComparator { lhs, rhs -> model.compareObjective(lhs.fitness, rhs.fitness) }
        var bestParticle = particles.first()
        val goodParticles = particles.take(solutionAmount.toInt()).toMutableList()

        while (!policy.finished(iteration)) {
            var globalBetter = false

            val newParticles = particles
                .map { policy.transformPartial(it, bestParticle, model) }
                .sortedWithPartialThreeWayComparator { lhs, rhs -> model.compareObjective(lhs.fitness, rhs.fitness) }
            val newBestParticle = newParticles.first()
            particles = newParticles
            if (model.compareObjective(newBestParticle.fitness, bestParticle.fitness) is Order.Less) {
                bestParticle = newBestParticle
                globalBetter = true
            }
            refreshGoodParticles(goodParticles, newParticles, model)

            model.flush()
            iteration.next(globalBetter)
            if (memoryUseOver()) {
                System.gc()
            }

            if (runningCallBack?.invoke(iteration, bestParticle, goodParticles) is Failed) {
                break
            }
        }

        return goodParticles
            .take(solutionAmount.toInt())
            .map { it.position to it.fitness }
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
    }
}

typealias PSO = ParticleSwarmOptimizationAlgorithm<Flt64, Flt64>
typealias MulObjPSO = ParticleSwarmOptimizationAlgorithm<MulObj, List<Flt64>>
