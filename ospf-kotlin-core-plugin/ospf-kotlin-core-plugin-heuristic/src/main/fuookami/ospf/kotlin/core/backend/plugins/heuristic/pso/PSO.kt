package fuookami.ospf.kotlin.core.backend.plugins.heuristic.pso

import kotlin.time.*
import kotlin.random.*
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*

interface AbstractPSOPolicy<V> : AbstractHeuristicPolicy {
    fun accelerate(
        iteration: Iteration,
        particle: Particle<V>,
        bestParticle: Particle<V>,
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
    override fun accelerate(
        iteration: Iteration,
        particle: Particle<V>,
        bestParticle: Particle<V>,
        model: AbstractCallBackModelInterface<*, V>
    ): Particle<V> {
        return particle.new(
            newVelocity = (0 until particle.size).map {
                val newVelocity = w * particle.velocity[it] +
                        c1 * randomGenerator()!! * (particle.currentBest?.position?.get(it)
                    ?.let { pos -> pos - particle.position[it] } ?: Flt64.zero) +
                        c2 * randomGenerator()!! * (bestParticle.position[it] - particle.position[it])
                if (newVelocity gr maxVelocity) {
                    maxVelocity
                } else if (newVelocity ls -maxVelocity) {
                    -maxVelocity
                } else {
                    newVelocity
                }
            },
            iteration = iteration,
            policy = this,
            model = model
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
    ): List<Individual<V>> {
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
                .map {
                    policy.accelerate(
                        iteration =  iteration,
                        particle = it,
                        bestParticle = bestParticle,
                        model = model
                    )
                }
                .sortedWithPartialThreeWayComparator { lhs, rhs ->
                    model.compareObjective(lhs.fitness, rhs.fitness)
                }
            val newBestParticle = newParticles.first()
            particles = newParticles
            if (model.compareObjective(newBestParticle.fitness, bestParticle.fitness) is Order.Less) {
                bestParticle = newBestParticle
                globalBetter = true
            }
            refreshGoodIndividuals(goodParticles, newParticles, model, solutionAmount)

            model.flush()
            policy.update(
                iteration = iteration,
                better = globalBetter,
                bestIndividual = bestParticle,
                goodIndividuals = goodParticles,
                populations = listOf(particles),
                model = model
            )
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
            .map {
                SolutionWithFitness(
                    solution = it.position,
                    fitness = it.fitness
                )
            }
    }
}

typealias PSO = ParticleSwarmOptimizationAlgorithm<Flt64, Flt64>
typealias MulObjPSO = ParticleSwarmOptimizationAlgorithm<MulObj, List<Flt64>>
