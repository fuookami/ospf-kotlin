@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.Ret
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class HeuristicSolutionStatus {
    Feasible,
    Infeasible
}

data class HeuristicResult<ObjValue, V>(
    val bestSolution: Solution<V>?,
    val bestObjective: ObjValue?,
    val status: HeuristicSolutionStatus,
    val iteration: Iteration
) where V : RealNumber<V>, V : NumberField<V>

class BasicHeuristicPolicy(
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes
) : HeuristicPolicy(
    iterationLimit = iterationLimit,
    notBetterIterationLimit = notBetterIterationLimit,
    timeLimit = timeLimit
)

data class Particle<ObjValue, V>(
    val fitness: ObjValue,
    val solution: Solution<V>,
    val velocity: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    val bestPosition: Solution<V>? = null,
    val bestFitness: ObjValue? = null
) where V : RealNumber<V>, V : NumberField<V>

class ParticleSwarmHeuristicSolver<ObjValue, V>(
    val particleAmount: UInt64 = UInt64(100),
    val solutionAmount: UInt64 = UInt64.one,
    val w: Flt64 = Flt64(0.4),
    val c1: Flt64 = Flt64.two,
    val c2: Flt64 = Flt64.two,
    val maxVelocity: Flt64 = Flt64(10000),
    val solveOnObjectiveMiss: Boolean = true,
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64> = { Flt64(0.5) },
    private val initialVelocityGenerator: (index: Int) -> Flt64 = { Flt64.zero },
    private val converter: IntoValue<V>
) where V : RealNumber<V>, V : NumberField<V> {
    val name: String get() = "pso"

    fun withRandomGenerator(randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>): ParticleSwarmHeuristicSolver<ObjValue, V> {
        return ParticleSwarmHeuristicSolver(
            particleAmount = particleAmount,
            solutionAmount = solutionAmount,
            w = w,
            c1 = c1,
            c2 = c2,
            maxVelocity = maxVelocity,
            solveOnObjectiveMiss = solveOnObjectiveMiss,
            randomGenerator = randomGenerator,
            initialVelocityGenerator = initialVelocityGenerator,
            converter = converter
        )
    }

    fun withInitialVelocityGenerator(
        initialVelocityGenerator: (index: Int) -> Flt64
    ): ParticleSwarmHeuristicSolver<ObjValue, V> {
        return ParticleSwarmHeuristicSolver(
            particleAmount = particleAmount,
            solutionAmount = solutionAmount,
            w = w,
            c1 = c1,
            c2 = c2,
            maxVelocity = maxVelocity,
            solveOnObjectiveMiss = solveOnObjectiveMiss,
            randomGenerator = randomGenerator,
            initialVelocityGenerator = initialVelocityGenerator,
            converter = converter
        )
    }

    fun withSolveOnObjectiveMiss(enabled: Boolean): ParticleSwarmHeuristicSolver<ObjValue, V> {
        return ParticleSwarmHeuristicSolver(
            particleAmount = particleAmount,
            solutionAmount = solutionAmount,
            w = w,
            c1 = c1,
            c2 = c2,
            maxVelocity = maxVelocity,
            solveOnObjectiveMiss = enabled,
            randomGenerator = randomGenerator,
            initialVelocityGenerator = initialVelocityGenerator,
            converter = converter
        )
    }

    private fun random(): Flt64 = randomGenerator() ?: Flt64.zero

    private fun clampVelocity(value: Flt64): Flt64 {
        return if (value gr maxVelocity) {
            maxVelocity
        } else if (value ls -maxVelocity) {
            -maxVelocity
        } else {
            value
        }
    }

    private fun toIndividual(particle: Particle<ObjValue, V>): SolutionWithFitness<ObjValue, V> {
        return SolutionWithFitness(
            solution = particle.solution,
            fitness = particle.fitness
        )
    }

    private fun evaluateFitness(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        solution: Solution<V>
    ): ObjValue? {
        return when (model.constraintSatisfied(solution)) {
            false -> null
            else -> model.objective(solution) ?: if (solveOnObjectiveMiss) {
                model.defaultObjective
            } else {
                null
            }
        }
    }

    private fun buildParticle(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        solution: Solution<V>
    ): Particle<ObjValue, V>? {
        val fitness = evaluateFitness(model, solution) ?: return null
        return Particle(
            fitness = fitness,
            solution = solution,
            velocity = solution.indices.map { initialVelocityGenerator(it) }
        )
    }

    private fun better(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        lhs: ObjValue,
        rhs: ObjValue
    ): Boolean {
        return model.compareObjective(lhs, rhs) is Order.Less
    }

    private fun sortedParticles(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        particles: List<Particle<ObjValue, V>>
    ): List<Particle<ObjValue, V>> {
        return particles.sortedWith { lhs, rhs ->
            when (model.compareObjective(lhs.fitness, rhs.fitness)) {
                is Order.Less -> -1
                is Order.Greater -> 1
                else -> 0
            }
        }
    }

    private fun accelerate(
        iteration: Iteration,
        particle: Particle<ObjValue, V>,
        bestParticle: Particle<ObjValue, V>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        policy: AbstractHeuristicPolicy
    ): Particle<ObjValue, V> {
        val localBest = particle.bestPosition ?: particle.solution
        val newSolution = ArrayList<V>(particle.solution.size)
        val newVelocity = ArrayList<fuookami.ospf.kotlin.math.algebra.number.Flt64>(particle.solution.size)
        for (index in particle.solution.indices) {
            val positionFlt64 = converter.fromValue(particle.solution[index])
            val velocity = particle.velocity.getOrElse(index) { Flt64.zero }
            val personalBestFlt64 = converter.fromValue(localBest.getOrElse(index) { particle.solution[index] })
            val globalBestFlt64 = converter.fromValue(bestParticle.solution.getOrElse(index) { particle.solution[index] })
            val velocityNext = clampVelocity(
                w * velocity
                        + c1 * random() * (personalBestFlt64 - positionFlt64)
                        + c2 * random() * (globalBestFlt64 - positionFlt64)
            )
            val positionNextFlt64 = positionFlt64 + velocityNext
            val positionNext = converter.intoValue(
                policy.coerceIn(
                    iteration = iteration,
                    index = index,
                    value = positionNextFlt64,
                    model = model
                )
            )
            newSolution.add(positionNext)
            newVelocity.add(velocityNext)
        }

        val newFitness = evaluateFitness(model, newSolution) ?: particle.fitness
        val retainedBestPosition: Solution<V>
        val retainedBestFitness: ObjValue
        val baselineFitness = particle.bestFitness ?: particle.fitness
        if (better(model, newFitness, baselineFitness)) {
            retainedBestPosition = newSolution
            retainedBestFitness = newFitness
        } else {
            retainedBestPosition = particle.bestPosition ?: particle.solution
            retainedBestFitness = baselineFitness
        }

        return Particle(
            fitness = newFitness,
            solution = newSolution,
            velocity = newVelocity,
            bestPosition = retainedBestPosition,
            bestFitness = retainedBestFitness
        )
    }

    suspend operator fun invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        policy: AbstractHeuristicPolicy = BasicHeuristicPolicy()
    ): Ret<HeuristicResult<ObjValue, V>> {
        val iteration = Iteration()
        val keepAmount = if (solutionAmount leq UInt64.zero) UInt64.one else solutionAmount
        var particles = sortedParticles(
            model,
            model.initialSolutions(particleAmount)
                .mapNotNull { buildParticle(model, it) }
        )

        if (particles.isEmpty()) {
            return Ok(
                HeuristicResult(
                    bestSolution = null,
                    bestObjective = null,
                    status = HeuristicSolutionStatus.Infeasible,
                    iteration = iteration
                )
            )
        }

        var bestParticle = particles.first()
        val goodIndividuals = particles
            .map(::toIndividual)
            .take(keepAmount.toInt())
            .toMutableList()

        while (!policy.finished(iteration)) {
            val newParticles = sortedParticles(
                model,
                particles.map { accelerate(iteration, it, bestParticle, model, policy) }
            )
            var globalBetter = false
            val newBest = newParticles.first()
            if (better(model, newBest.fitness, bestParticle.fitness)) {
                bestParticle = newBest
                globalBetter = true
            }

            val newIndividuals = newParticles.map(::toIndividual)
            refreshGoodIndividuals(
                goodIndividuals = goodIndividuals,
                newIndividuals = newIndividuals,
                model = model,
                solutionAmount = keepAmount
            )
            policy.update(
                iteration = iteration,
                better = globalBetter,
                bestIndividual = toIndividual(bestParticle),
                goodIndividuals = goodIndividuals,
                populations = listOf(newIndividuals),
                model = model
            )
            iteration.next(globalBetter)
            model.flush()
            particles = newParticles
        }

        model.tokens.setSolution(bestParticle.solution)
        return Ok(
            HeuristicResult(
                bestSolution = bestParticle.solution,
                bestObjective = bestParticle.fitness,
                status = HeuristicSolutionStatus.Feasible,
                iteration = iteration
            )
        )
    }

}
