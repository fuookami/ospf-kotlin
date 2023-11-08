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

data class Particle(
    val fitness: Flt64?,
    val position: List<Flt64>,
    val velocity: List<Flt64>,
    val currentBest: Particle? = null
) {
    init {
        assert(position.size == velocity.size)
    }

    val size by position::size

    fun new(newVelocity: List<Flt64>, model: CallBackModelInterface): Particle {
        val newPosition = (0 until size).map { position[it] + velocity[it] }
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

interface PSOPolicy {
    fun transformPartial(particle: Particle, bestPartial: Particle, model: CallBackModelInterface): Particle
    fun finished(iteration: Iteration): Boolean
}

open class CommonPSOPolicy(
    val w: Flt64 = Flt64(0.4),
    /** 局部学习因子 */
    val c1: Flt64 = Flt64.two,
    /** 全局学习因子 */
    val c2: Flt64 = Flt64.two,
    val maxVelocity: Flt64 = Flt64(10000.0),
    val iterationLimit: UInt64 = UInt64.maximum,
    val notBetterIterationLimit: UInt64 = UInt64.maximum,
    val timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Flt64(Random.nextDouble()) }
) : PSOPolicy {
    override fun transformPartial(particle: Particle, bestPartial: Particle, model: CallBackModelInterface): Particle {
        return particle.new(
            (0 until particle.size).map {
                val newVelocity = w * particle.velocity[it] +
                        c1 * randomGenerator()!! * (particle.currentBest?.position?.get(it)?.let { pos -> pos - particle.position[it] } ?: Flt64.zero) +
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

class PSO(
    val particleAmount: UInt64 = UInt64(100UL),
    val solutionAmount: UInt64 = UInt64.one,
    val policy: PSOPolicy = CommonPSOPolicy()
) {
    operator fun invoke(
        model: CallBackModelInterface,
        initialVelocityGenerator: Extractor<Flt64, UInt64> = { Flt64(Random.nextDouble(2.0) - 1.0) }
    ): List<Pair<Solution, Flt64?>> {
        val iteration = Iteration()
        val initialSolutions = model.initialSolutions(particleAmount)
        var particles = initialSolutions
            .map {
                Particle(
                    if (model.constraintSatisfied(it) == true) {
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
        }

        return goodParticles.map { Pair(it.position, it.fitness) }
    }

    private fun refreshGoodParticles(goodParticles: MutableList<Particle>, newParticles: List<Particle>, model: CallBackModelInterface) {
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
            goodParticles.addAll(newParticles.subList(j, minOf(newParticles.size, maxOf(j, solutionAmount.toInt() - goodParticles.size))))
        }
        if (UInt64(goodParticles.size) > solutionAmount) {
            (UInt64.zero until UInt64(goodParticles.size) - solutionAmount).forEach { _ ->
                goodParticles.removeLast()
            }
        }
    }
}
