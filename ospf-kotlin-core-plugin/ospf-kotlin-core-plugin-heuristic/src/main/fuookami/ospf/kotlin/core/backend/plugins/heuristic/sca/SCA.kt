package fuookami.ospf.kotlin.core.backend.plugins.heuristic.sca

import kotlin.time.*
import kotlin.time.Duration.Companion.minutes
import kotlin.random.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*

interface AbstractSCAPolicy<V> : AbstractHeuristicPolicy {
    fun r1(iteration: Iteration): Flt64

    fun r2(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*,V>
    ): List<Flt64>

    fun r3(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*,V>
    ): List<Flt64>

    fun move(
        iteration: Iteration,
        solution: SolutionWithFitness<V>,
        bestSolution: SolutionWithFitness<V>,
        r1: Flt64,
        r2: List<Flt64>,
        r3: List<Flt64>,
        model: AbstractCallBackModelInterface<*,V>
    ): SolutionWithFitness<V>
}

data class QLearningState(
    val density: Flt64,
    val distance: Flt64
) {
    val discretize: UInt64 by lazy {
        val densityLevel = if (density leq Flt64.three.reciprocal()) {
            UInt64.zero
        } else if (density leq (Flt64.one - Flt64.three.reciprocal())) {
            UInt64.one
        } else {
            UInt64.two
        }
        val distanceLevel = if (distance leq  Flt64.three.reciprocal()) {
            UInt64.zero
        } else if (distance leq (Flt64.one - Flt64.three.reciprocal())) {
            UInt64.one
        } else {
            UInt64.two
        }
        densityLevel * UInt64.three + distanceLevel
    }
}

class SCAPolicy<V>(
    private val alpha: Flt64 = Flt64(0.1),
    private val gamma: Flt64 = Flt64(0.9),
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
) : HeuristicPolicy(iterationLimit, notBetterIterationLimit, timeLimit), AbstractSCAPolicy<V> {
    private val qTable = mutableMapOf<UInt64, MutableList<Flt64>>()
    private var currentState: UInt64 = UInt64.zero

    private val r1Range: Pair<Flt64, Flt64> get() {
        return when (currentState / UInt64.three) {
            UInt64.one -> Flt64.zero to (Flt64.one - Flt64.three.reciprocal())
            UInt64.two -> (Flt64.one - Flt64.three.reciprocal()) to (Flt64.one + Flt64.three.reciprocal())
            else -> (Flt64.one + Flt64.three.reciprocal()) to Flt64.two
        }
    }

    override fun r1(iteration: Iteration): Flt64 {
        val iterationCoefficient = max(
            ((iteration.iteration.toFlt64() * Flt64.pi)
                    / (Flt64.two * iterationLimit.toFlt64())).exp(),
            ((Flt64(iteration.time.toDouble(DurationUnit.SECONDS)) * Flt64.pi)
                    / (Flt64.two * Flt64(timeLimit.toDouble(DurationUnit.SECONDS)))).exp()
        )
        val (minR1, maxR1) = r1Range
        return minR1 + (maxR1 - minR1) * iterationCoefficient
    }

    override fun r2(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Flt64> {
        return model.tokens.tokens.indices.map {
            Flt64.two * Flt64.pi * randomGenerator()!!
        }
    }

    private val r3Range: Pair<Flt64, Flt64> get() {
        return when (currentState % UInt64.three) {
            UInt64.one -> Flt64.zero to (Flt64.one - Flt64.three.reciprocal())
            UInt64.two -> (Flt64.one - Flt64.three.reciprocal()) to (Flt64.one + Flt64.three.reciprocal())
            else -> (Flt64.one + Flt64.three.reciprocal()) to Flt64.two
        }
    }

    override fun r3(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*,V>
    ): List<Flt64> {
        val (minR3, maxR3) = r3Range
        return model.tokens.tokens.indices.map {
            minR3 + randomGenerator()!! * (maxR3 - minR3)
        }
    }

    override fun move(
        iteration: Iteration,
        solution: SolutionWithFitness<V>,
        bestSolution: SolutionWithFitness<V>,
        r1: Flt64,
        r2: List<Flt64>,
        r3: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>
    ): SolutionWithFitness<V> {
        val newSolution = solution.solution.mapIndexed { index, position ->
            val delta = r3[index] * bestSolution.solution[index] - position
            if (randomGenerator()!! < Flt64.two.reciprocal()) {
                position + r1 * r2[index].sin() * abs(delta)
            } else {
                position + r1 * r2[index].cos() * abs(delta)
            }
        }
        return SolutionWithFitness(
            newSolution,
            model.objective(newSolution).ifNull { model.defaultObjective }
        )
    }

    override fun update(
        iteration: Iteration,
        better: Boolean,
        bestIndividual: Individual<*>,
        goodIndividuals: List<Individual<*>>,
        populations: List<List<Individual<*>>>,
        model: AbstractCallBackModelInterface<*, *>
    ) {
        if (better) {
            updateQTable(Flt64.one)
        } else {
            updateQTable(-Flt64(0.5))
        }
        updateState(populations.flatten(), bestIndividual, model)
    }

    private fun updateState(
        population: List<Individual<*>>,
        best: Individual<*>,
        model: AbstractCallBackModelInterface<*, *>
    ) {
        val density = calculateDensity(population)
        val distance = calculateDistance(population, best, model)
        currentState = QLearningState(density, distance).discretize
    }

    private fun updateQTable(reward: Flt64) {
        val actions = qTable.getOrPut(currentState) { mutableListOf(Flt64(9)) }
        val maxNextQ = qTable[currentState]?.maxOrNull() ?: Flt64.zero
        val bestAction = actions.indexOf(actions.maxOrNull()!!)
        actions[bestAction] += alpha * (reward + gamma * maxNextQ - actions[bestAction])
    }

    private fun calculateDensity(
        population: List<Individual<*>>
    ): Flt64 {
        val center = population.flatMap { it.solution }.average()
        return population.sumOf { individual ->
            individual.solution.sumOf { position ->
                (position - center).abs()
            }
        } / Flt64(population.size * population.first().solution.size)
    }

    private fun calculateDistance(
        population: List<Individual<*>>,
        best: Individual<*>,
        model: AbstractCallBackModelInterface<*, *>
    ): Flt64 {
        val maxDistance = model.tokens.tokens.sumOf {
            (it.upperBound!!.value.unwrap() - it.lowerBound!!.value.unwrap()).sqr()
        }.sqrt()
        return population.sumOf { individual ->
            individual.solution.withIndex().sumOf { (index, position) ->
                (position - best.solution[index]).sqr()
            }.sqrt()
        } / (Flt64(population.size) * maxDistance)
    }
}

class SineCosineAlgorithm<Obj, V>(
    val populationAmount: UInt64 = UInt64(100UL),
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractSCAPolicy<V>
) {
    suspend operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, V>,
        runningCallBack: ((Iteration, SolutionWithFitness<V>) -> Try)? = null
    ): List<Individual<V>> {
        val iteration = Iteration()
        val initialSolutions = model.initialSolutions(populationAmount)
        var population = initialSolutions
            .map {
                SolutionWithFitness(
                    it,
                    model.objective(it).ifNull { model.defaultObjective }
                )
            }
            .sortedWithPartialThreeWayComparator { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }
        var bestIndividual = population.first()
        val goodIndividuals = population.take(solutionAmount.toInt()).toMutableList()

        while (!policy.finished(iteration)) {
            var globalBetter = false

            val r1 = policy.r1(iteration)
            val r2 = policy.r2(iteration, model)
            val r3 = policy.r3(iteration, model)

            val newPopulation = coroutineScope {
                population.map { individual ->
                    async(Dispatchers.Default) {
                        policy.move(
                            iteration = iteration,
                            solution = individual,
                            bestSolution = bestIndividual,
                            r1 = r1,
                            r2 = r2,
                            r3 = r3,
                            model = model
                        )
                    }
                }.awaitAll()
            }.sortedWithPartialThreeWayComparator { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }
            val newBestIndividual = newPopulation.first()
            population = newPopulation
            if (model.compareObjective(newBestIndividual.fitness, bestIndividual.fitness) is Order.Less) {
                bestIndividual = newBestIndividual
                globalBetter = true
            }
            refreshGoodIndividuals(goodIndividuals, newPopulation, model)

            model.flush()
            policy.update(
                iteration = iteration,
                better = globalBetter,
                bestIndividual = bestIndividual,
                goodIndividuals = goodIndividuals,
                populations = listOf(population),
                model = model
            )
            iteration.next(globalBetter)
            if (memoryUseOver()) {
                System.gc()
            }

            if (runningCallBack?.invoke(iteration, bestIndividual) is Failed) {
                break
            }
        }

        return goodIndividuals.take(solutionAmount.toInt())
    }
}

typealias SCA = SineCosineAlgorithm<Flt64, Flt64>
typealias MulObjSCA = SineCosineAlgorithm<MulObj, List<Flt64>>
