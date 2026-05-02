@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
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

typealias InitialVelocityGenerator = (index: Int) -> Flt64

enum class HeuristicSolutionStatus {
    Feasible,
    Infeasible
}

data class HeuristicResult<V>(
    val bestSolution: Solution?,
    val bestObjective: V?,
    val status: HeuristicSolutionStatus,
    val iteration: Iteration
)

class BasicHeuristicPolicy(
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes
) : HeuristicPolicy(
    iterationLimit = iterationLimit,
    notBetterIterationLimit = notBetterIterationLimit,
    timeLimit = timeLimit
)

data class Particle<V>(
    override val fitness: V,
    override val solution: Solution,
    val velocity: List<Flt64>,
    val currentBest: SolutionWithFitness<V>? = null
) : Individual<V>

class ParticleSwarmHeuristicSolver<V>(
    val particleAmount: UInt64 = UInt64(100),
    val solutionAmount: UInt64 = UInt64.one,
    val w: Flt64 = Flt64(0.4),
    val c1: Flt64 = Flt64.two,
    val c2: Flt64 = Flt64.two,
    val maxVelocity: Flt64 = Flt64(10000),
    val solveOnObjectiveMiss: Boolean = true,
    private val randomGenerator: Generator<Flt64> = { Flt64(0.5) },
    private val initialVelocityGenerator: InitialVelocityGenerator = { Flt64.zero }
) where V : RealNumber<V>, V : NumberField<V> {
    val name: String get() = "pso"

    fun withRandomGenerator(randomGenerator: Generator<Flt64>): ParticleSwarmHeuristicSolver<V> {
        return ParticleSwarmHeuristicSolver(
            particleAmount = particleAmount,
            solutionAmount = solutionAmount,
            w = w,
            c1 = c1,
            c2 = c2,
            maxVelocity = maxVelocity,
            solveOnObjectiveMiss = solveOnObjectiveMiss,
            randomGenerator = randomGenerator,
            initialVelocityGenerator = initialVelocityGenerator
        )
    }

    fun withInitialVelocityGenerator(
        initialVelocityGenerator: InitialVelocityGenerator
    ): ParticleSwarmHeuristicSolver<V> {
        return ParticleSwarmHeuristicSolver(
            particleAmount = particleAmount,
            solutionAmount = solutionAmount,
            w = w,
            c1 = c1,
            c2 = c2,
            maxVelocity = maxVelocity,
            solveOnObjectiveMiss = solveOnObjectiveMiss,
            randomGenerator = randomGenerator,
            initialVelocityGenerator = initialVelocityGenerator
        )
    }

    fun withSolveOnObjectiveMiss(enabled: Boolean): ParticleSwarmHeuristicSolver<V> {
        return ParticleSwarmHeuristicSolver(
            particleAmount = particleAmount,
            solutionAmount = solutionAmount,
            w = w,
            c1 = c1,
            c2 = c2,
            maxVelocity = maxVelocity,
            solveOnObjectiveMiss = enabled,
            randomGenerator = randomGenerator,
            initialVelocityGenerator = initialVelocityGenerator
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

    private fun toIndividual(particle: Particle<V>): SolutionWithFitness<V> {
        return SolutionWithFitness(
            solution = particle.solution,
            fitness = particle.fitness
        )
    }

    private fun evaluateFitness(
        model: AbstractCallBackModelInterface<*, V>,
        solution: Solution
    ): V? {
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
        model: AbstractCallBackModelInterface<*, V>,
        solution: Solution
    ): Particle<V>? {
        val fitness = evaluateFitness(model, solution) ?: return null
        return Particle(
            fitness = fitness,
            solution = solution,
            velocity = solution.indices.map { initialVelocityGenerator(it) }
        )
    }

    private fun better(
        model: AbstractCallBackModelInterface<*, V>,
        lhs: V,
        rhs: V
    ): Boolean {
        return model.compareObjective(lhs, rhs) is Order.Less
    }

    private fun sortedParticles(
        model: AbstractCallBackModelInterface<*, V>,
        particles: List<Particle<V>>
    ): List<Particle<V>> {
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
        particle: Particle<V>,
        bestParticle: Particle<V>,
        model: AbstractCallBackModelInterface<*, V>,
        policy: AbstractHeuristicPolicy
    ): Particle<V> {
        val localBest = particle.currentBest?.solution ?: particle.solution
        val newSolution = ArrayList<Flt64>(particle.solution.size)
        val newVelocity = ArrayList<Flt64>(particle.solution.size)
        for (index in particle.solution.indices) {
            val position = particle.solution[index]
            val velocity = particle.velocity.getOrElse(index) { Flt64.zero }
            val personalBest = localBest.getOrElse(index) { position }
            val globalBest = bestParticle.solution.getOrElse(index) { position }
            val velocityNext = clampVelocity(
                w * velocity
                        + c1 * random() * (personalBest - position)
                        + c2 * random() * (globalBest - position)
            )
            val positionNext = policy.coerceIn(
                iteration = iteration,
                index = index,
                value = position + velocityNext,
                model = model
            )
            newSolution.add(positionNext)
            newVelocity.add(velocityNext)
        }

        val newFitness = evaluateFitness(model, newSolution) ?: particle.fitness
        val candidateBest = SolutionWithFitness(newSolution, newFitness)
        val baselineBest = particle.currentBest ?: SolutionWithFitness(particle.solution, particle.fitness)
        val retainedBest = if (better(model, candidateBest.fitness, baselineBest.fitness)) {
            candidateBest
        } else {
            baselineBest
        }

        return Particle(
            fitness = newFitness,
            solution = newSolution,
            velocity = newVelocity,
            currentBest = retainedBest
        )
    }

    suspend operator fun invoke(
        model: AbstractCallBackModelInterface<*, V>,
        policy: AbstractHeuristicPolicy = BasicHeuristicPolicy()
    ): Ret<HeuristicResult<V>> {
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

        model.tokens.setSolverSolution(bestParticle.solution)
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
