package fuookami.ospf.kotlin.core.backend.plugins.heuristic.mvo

import kotlin.time.*
import kotlin.random.*
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*

typealias Universe<V> = SolutionWithFitness<V>

interface AbstractMVOPolicy<V> {
    /**
     * calculate WEP (Wormhole Existence Probability)
     */
    fun wep(iteration: Iteration): Flt64

    /**
     * calculate TDR (Travelling Distance Rate)
     */
    fun tdr(iteration: Iteration): Flt64

    fun whiteHoleRates(
        model: AbstractCallBackModelInterface<*, V>,
        objs: List<V>
    ): List<Flt64>

    fun transformUniverses(
        iteration: Iteration,
        bestSolution: Solution,
        solutions: List<Universe<V>>,
        model: AbstractCallBackModelInterface<*, V>,
        wep: Flt64,
        tdr: Flt64
    ): List<Solution>

    fun finished(iteration: Iteration): Boolean
}

/**
 *
 * @param V
 * @property minWEP         minimum wormhole existence probability
 * @property maxWEP         maximum wormhole existence probability
 */
open class MVOPolicy<V>(
    val minWEP: Flt64 = Flt64(0.2),
    val maxWEP: Flt64 = Flt64(1.0),
    val whiteHoleSelector: Selection,
    val whiteHoleRateCalculator: ObjectiveNormalization<V>,
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
) : HeuristicPolicy(iterationLimit, notBetterIterationLimit, timeLimit), AbstractMVOPolicy<V> {
    companion object {
        operator fun invoke(
            minWEP: Flt64 = Flt64(0.2),
            maxWEP: Flt64 = Flt64(1.0),
            iterationLimit: UInt64 = UInt64.maximum,
            notBetterIterationLimit: UInt64 = UInt64.maximum,
            timeLimit: Duration = 30.minutes,
            randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
        ): MVOPolicy<Flt64> {
            return MVOPolicy(
                minWEP = minWEP,
                maxWEP = maxWEP,
                whiteHoleRateCalculator = SumNormalization,
                whiteHoleSelector = RouletteSelection(randomGenerator),
                iterationLimit = iterationLimit,
                notBetterIterationLimit = notBetterIterationLimit,
                timeLimit = timeLimit,
                randomGenerator = randomGenerator
            )
        }
    }

    override fun wep(iteration: Iteration): Flt64 {
        return minWEP + (maxWEP - minWEP) * max(
            Flt64(iteration.time / timeLimit),
            iteration.iteration.toFlt64() / iterationLimit.toFlt64()
        )
    }

    override fun tdr(iteration: Iteration): Flt64 {
        return Flt64.one - max(
            Flt64(iteration.time / timeLimit),
            iteration.iteration.toFlt64() / iterationLimit.toFlt64()
        ).pow(Flt64(6.0).reciprocal()).toFlt64()
    }

    override fun whiteHoleRates(
        model: AbstractCallBackModelInterface<*, V>,
        objs: List<V>
    ): List<Flt64> {
        return whiteHoleRateCalculator(model, objs)
    }

    override fun transformUniverses(
        iteration: Iteration,
        bestSolution: Solution,
        solutions: List<Universe<V>>,
        model: AbstractCallBackModelInterface<*, V>,
        wep: Flt64,
        tdr: Flt64
    ): List<Solution> {
        val newSolutions = ArrayList<Solution>()
        val whiteHoleRates = whiteHoleRates(model, solutions.map { it.fitness })
        for ((i, solution) in solutions.withIndex()) {
            val newSolution = solution.solution.toMutableList()
            for ((dimension, value) in solution.solution.withIndex()) {
                val token = model.tokens[dimension]
                var newValue = if (randomGenerator()!! ls whiteHoleRates[i]) {
                    solutions[whiteHoleSelector(iteration, whiteHoleRates)].solution[dimension]
                } else {
                    value
                }
                newValue = if (randomGenerator()!! ls wep) {
                    applyWormhole(
                        bestSolution = bestSolution,
                        dimension = dimension,
                        token = token,
                        tdr = tdr
                    )
                } else {
                    newValue
                }
                newSolution[dimension] = newValue.coerceIn(token.lowerBound!!.value.unwrap(), token.upperBound!!.value.unwrap())
            }
        }
        return newSolutions
    }

    private fun applyWormhole(
        bestSolution: Solution,
        dimension: Int,
        token: Token,
        tdr: Flt64
    ): Flt64 {
        val range = token.upperBound!!.value.unwrap() - token.lowerBound!!.value.unwrap()
        return if (randomGenerator()!! ls Flt64(0.5)) {
            bestSolution[dimension] + tdr * (range * randomGenerator()!! + token.lowerBound!!.value.unwrap())
        } else {
            bestSolution[dimension] - tdr * (range * randomGenerator()!! + token.lowerBound!!.value.unwrap())
        }
    }
}

class MultiVerseOptimizer<Obj, V>(
    val universeAmount: UInt64 = UInt64(100UL),
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractMVOPolicy<V>
) {
    operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, V>,
        runningCallBack: ((Iteration, Universe<V>, List<Universe<V>>) -> Try)? = null
    ): List<Individual<V>> {
        val iteration = Iteration()
        var universes = model.initialSolutions(universeAmount)
            .map {
                Universe(
                    solution = it,
                    fitness = model.objective(it).ifNull { model.defaultObjective }
                )
            }
            .sortedWithPartialThreeWayComparator { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }
        var bestUniverse = universes.first()
        val gooUniverses = universes.take(solutionAmount.toInt()).toMutableList()

        while (!policy.finished(iteration)) {
            var globalBetter = false
            val wep = policy.wep(iteration)
            val tdr = policy.tdr(iteration)

            val newUniverses = policy
                .transformUniverses(
                    iteration = iteration,
                    bestSolution = bestUniverse.solution,
                    solutions = universes,
                    model = model,
                    wep = wep,
                    tdr = tdr
                )
                .map {
                    Universe(
                        solution = it,
                        fitness = model.objective(it).ifNull { model.defaultObjective }
                    )
                }
                .sortedWithPartialThreeWayComparator { lhs, rhs ->
                    model.compareObjective(lhs.fitness, rhs.fitness)
                }
            val newBestUniverse = newUniverses.first()
            universes = newUniverses
            if (model.compareObjective(newBestUniverse.fitness, bestUniverse.fitness) is Order.Less) {
                bestUniverse = newBestUniverse
                globalBetter = true
            }
            refreshGoodIndividuals(gooUniverses, newUniverses, model, solutionAmount)

            model.flush()
            iteration.next(globalBetter)
            if (memoryUseOver()) {
                System.gc()
            }

            if (runningCallBack?.invoke(iteration, bestUniverse, gooUniverses) is Failed) {
                break
            }
        }

        return gooUniverses.take(solutionAmount.toInt())
    }
}

typealias MVO = MultiVerseOptimizer<Flt64, Flt64>
typealias MulObjMVO = MultiVerseOptimizer<MulObj, List<Flt64>>
