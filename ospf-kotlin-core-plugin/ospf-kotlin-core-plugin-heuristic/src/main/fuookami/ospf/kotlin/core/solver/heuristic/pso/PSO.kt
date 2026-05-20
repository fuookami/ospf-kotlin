@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.heuristic.pso

import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.model.basic.MultiObjectLocation
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.utils.memoryUseOver
import fuookami.ospf.kotlin.utils.functional.Order
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

interface AbstractPSOPolicy<ObjValue, V> : AbstractHeuristicPolicy where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    fun accelerate(
        iteration: Iteration,
        particle: Particle<ObjValue, V>,
        bestParticle: Particle<ObjValue, V>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): Particle<ObjValue, V>
}

/**
 *
 * @property c1         local learning factor
 * @property c2         global learning factor
 */
open class PSOPolicy<ObjValue, V>(
    val w: Flt64 = Flt64(0.4),
    val c1: Flt64 = Flt64.two,
    val c2: Flt64 = Flt64.two,
    val maxVelocity: Flt64 = Flt64(10000.0),
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() },
    private val converter: IntoValue<V>
) : HeuristicPolicy(
    iterationLimit = iterationLimit,
    notBetterIterationLimit = notBetterIterationLimit,
    timeLimit = timeLimit
), AbstractPSOPolicy<ObjValue, V> where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    companion object {
        operator fun invoke(
            w: Flt64 = Flt64(0.4),
            c1: Flt64 = Flt64.two,
            c2: Flt64 = Flt64.two,
            maxVelocity: Flt64 = Flt64(10000.0),
            iterationLimit: UInt64 = UInt64.maximum,
            notBetterIterationLimit: UInt64 = UInt64.maximum,
            timeLimit: Duration = 30.minutes,
            randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
        ): PSOPolicy<Flt64, Flt64> {
            return PSOPolicy(
                w = w,
                c1 = c1,
                c2 = c2,
                maxVelocity = maxVelocity,
                iterationLimit = iterationLimit,
                notBetterIterationLimit = notBetterIterationLimit,
                timeLimit = timeLimit,
                randomGenerator = randomGenerator,
                converter = flt64Converter
            )
        }
    }
    override fun accelerate(
        iteration: Iteration,
        particle: Particle<ObjValue, V>,
        bestParticle: Particle<ObjValue, V>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): Particle<ObjValue, V> {
        return particle.new(
            newVelocity = (0 until particle.size).map {
                val posFlt64 = converter.fromValue(particle.solution[it])
                val personalBestFlt64 = particle.currentBest?.solution?.get(it)
                    ?.let { pos -> converter.fromValue(pos) }
                val globalBestFlt64 = converter.fromValue(bestParticle.solution[it])
                val newVelocity = w * particle.velocity[it] +
                        c1 * randomGenerator()!! * (personalBestFlt64?.let { pb -> pb - posFlt64 } ?: Flt64.zero) +
                        c2 * randomGenerator()!! * (globalBestFlt64 - posFlt64)
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

@OptIn(ExperimentalTime::class)
class ParticleSwarmOptimizationAlgorithm<Obj, ObjValue, V>(
    val particleAmount: UInt64 = UInt64(100UL),
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractPSOPolicy<ObjValue, V>,
    // converter must be provided explicitly; use PSOPolicy.Flt64 companion for V=Flt64 convenience
    private val converter: IntoValue<V>
) where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    companion object {
        operator fun invoke(
            particleAmount: UInt64 = UInt64(100UL),
            solutionAmount: UInt64 = UInt64.one,
            policy: AbstractPSOPolicy<Flt64, Flt64> = PSOPolicy()
        ): ParticleSwarmOptimizationAlgorithm<Flt64, Flt64, Flt64> {
            return ParticleSwarmOptimizationAlgorithm(
                particleAmount = particleAmount,
                solutionAmount = solutionAmount,
                policy = policy,
                converter = flt64Converter
            )
        }
    }
    operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, ObjValue, V>,
        initialVelocityGenerator: Extractor<Flt64, UInt64> = { Random.nextFlt64(Flt64.two) - Flt64.one },
        runningCallBack: ((Iteration, Particle<ObjValue, V>, List<Particle<ObjValue, V>>) -> Try)? = null
    ): List<Individual<ObjValue, V>> {
        val iteration = Iteration()
        val initialSolutions = model.initialSolutions(particleAmount)
        var particles = initialSolutions
            .map {
                Particle(
                    fitness = model.objective(it).ifNull { model.defaultObjective },
                    solution = it,
                    velocity = it.indices.map { index -> initialVelocityGenerator(UInt64(index)) },
                    converter = converter
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
                        iteration = iteration,
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
            refreshGoodIndividuals(
                goodIndividuals = goodParticles,
                newIndividuals = newParticles,
                model = model,
                solutionAmount = solutionAmount
            )

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
                    solution = it.solution,
                    fitness = it.fitness
                )
            }
    }
}

typealias PSO = ParticleSwarmOptimizationAlgorithm<Flt64, Flt64, Flt64>
typealias MulObjPSO = ParticleSwarmOptimizationAlgorithm<List<Pair<MultiObjectLocation<Flt64>, Flt64>>, List<Flt64>, Flt64>

