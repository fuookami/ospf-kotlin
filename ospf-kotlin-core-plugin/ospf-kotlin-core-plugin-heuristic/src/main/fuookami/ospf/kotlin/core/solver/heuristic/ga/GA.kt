/** 遗传算法实现 / Genetic Algorithm implementation */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.heuristic.ga

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.core.model.basic.MultiObjectLocation
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.cleanupAfterSolverRun
import fuookami.ospf.kotlin.core.solver.cleanupOnSolverMemoryPressure
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/** 遗传算法策略接口 / Genetic algorithm policy interface */
interface AbstractGAPolicy<ObjValue, V> :
    AbstractHeuristicPolicy where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    /**
     * 执行种群迁移 / Execute population migration
     *
     * @param iteration 当前迭代 / current iteration
     * @param populations 种群列表 / population list
     * @param model 回调模型 / callback model
     * @return 迁移后的种群列表 / migrated population list
     */
    suspend fun migrate(
        iteration: Iteration,
        populations: List<AbstractPopulation<ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<AbstractPopulation<ObjValue, V>>

    /**
     * 执行选择操作 / Execute selection
     *
     * @param iteration 当前迭代 / current iteration
     * @param population 种群 / population
     * @param model 回调模型 / callback model
     * @return 选中的染色体列表 / selected chromosome list
     */
    suspend fun select(
        iteration: Iteration,
        population: AbstractPopulation<ObjValue, V>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Chromosome<ObjValue, V>>

    /**
     * 执行交叉操作 / Execute crossover
     *
     * @param iteration 当前迭代 / current iteration
     * @param population 染色体列表 / chromosome list
     * @param model 回调模型 / callback model
     * @param parentAmountRange 父代数量范围 / parent amount range
     * @return 交叉后的染色体列表 / crossed chromosome list
     */
    suspend fun cross(
        iteration: Iteration,
        population: List<Chromosome<ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<Chromosome<ObjValue, V>>

    /**
     * 执行变异操作 / Execute mutation
     *
     * @param iteration 当前迭代 / current iteration
     * @param population 染色体列表 / chromosome list
     * @param model 回调模型 / callback model
     * @param mutationRateRange 变异率范围 / mutation rate range
     * @return 变异后的染色体列表 / mutated chromosome list
     */
    suspend fun mutate(
        iteration: Iteration,
        population: List<Chromosome<ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Chromosome<ObjValue, V>>
}

/**
 * 遗传算法策略
 *
 * 实现遗传算法的迁移、选择、交叉和变异操作，支持多种群迁移和精英保留策略。
 *
 * Genetic algorithm policy
 *
 * Implements migration, selection, crossover, and mutation operations for genetic algorithm,
 * supporting multi-population migration and elite preservation strategy.
 *
 * @param ObjValue 目标值类型 / objective value type
 * @param V 值类型 / value type
 * @property migration 迁移策略 / migration strategy
 * @property selectionMode 选择模式 / selection mode
 * @property selection 选择算子 / selection operator
 * @property crossMode 交叉模式 / crossover mode
 * @property cross 交叉算子 / crossover operator
 * @property mutationMode 变异模式 / mutation mode
 * @property mutation 变异算子 / mutation operator
 * @property normalization 目标归一化方法 / objective normalization method
 * @property randomGenerator 随机数生成器 / random number generator
 */
class GAPolicy<ObjValue, V>(
    val migration: Migration<ObjValue, V>,
    val selectionMode: SelectionMode<ObjValue, V>,
    val selection: Selection,
    val crossMode: CrossMode<ObjValue, V>,
    val cross: Cross<V>,
    val mutationMode: MutationMode<ObjValue, V>,
    val mutation: Mutation<V>,
    val normalization: ObjectiveNormalization<ObjValue, V>,
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() },
    private val converter: IntoValue<V>
) : HeuristicPolicy(
    iterationLimit = iterationLimit,
    notBetterIterationLimit = notBetterIterationLimit,
    timeLimit = timeLimit
), AbstractGAPolicy<ObjValue, V> where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    companion object {
        operator fun invoke(
            migration: Migration<Flt64, Flt64>,
            selectionMode: SelectionMode<Flt64, Flt64>,
            selection: Selection,
            crossMode: CrossMode<Flt64, Flt64>,
            cross: Cross<Flt64>,
            mutationMode: MutationMode<Flt64, Flt64>,
            mutation: Mutation<Flt64>,
            normalization: ObjectiveNormalization<Flt64, Flt64>,
            iterationLimit: UInt64 = UInt64.maximum,
            notBetterIterationLimit: UInt64 = UInt64.maximum,
            timeLimit: Duration = 30.minutes,
            randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
        ): GAPolicy<Flt64, Flt64> {
            return GAPolicy(
                migration = migration,
                selectionMode = selectionMode,
                selection = selection,
                crossMode = crossMode,
                cross = cross,
                mutationMode = mutationMode,
                mutation = mutation,
                normalization = normalization,
                iterationLimit = iterationLimit,
                notBetterIterationLimit = notBetterIterationLimit,
                timeLimit = timeLimit,
                randomGenerator = randomGenerator,
                converter = flt64Converter
            )
        }
    }

    /** 执行种群迁移 / Execute population migration */
    override suspend fun migrate(
        iteration: Iteration,
        populations: List<AbstractPopulation<ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<AbstractPopulation<ObjValue, V>> {
        return migration(
            iteration = iteration,
            populations = populations,
            model = model
        )
            .map { (population, newIndividuals) ->
                val individuals = (population.individuals + newIndividuals)
                    .sortedWithPartialThreeWayComparator { lhs, rhs ->
                        model.compareObjective(lhs.fitness, rhs.fitness)
                    }
                AbstractPopulation(
                    individuals = individuals,
                    elites = individuals.take(population.eliteAmount.toInt()),
                    best = individuals.first(),
                    eliteAmount = population.eliteAmount,
                    densityRange = population.densityRange,
                    mutationRateRange = population.mutationRateRange,
                    parentAmountRange = population.parentAmountRange
                )
            }
    }

    /** 执行选择操作 / Execute selection */
    override suspend fun select(
        iteration: Iteration,
        population: AbstractPopulation<ObjValue, V>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Chromosome<ObjValue, V>> {
        val amount = selectionMode(
            iteration = iteration,
            population = population,
            model = model
        )
        val weights = normalization(model, population.individuals.map { it.fitness })
        val indexes = selection(
            iteration = iteration,
            weights = weights,
            amount = amount
        )
        return population.individuals.mapIndexedNotNull { index, individual ->
            if (UInt64(index) in indexes) {
                individual
            } else {
                null
            }
        }
    }

    /** 执行交叉操作 / Execute crossover */
    override suspend fun cross(
        iteration: Iteration,
        population: List<Chromosome<ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<Chromosome<ObjValue, V>> {
        val weights = normalization(model, population.map { it.fitness })
        val parentGroups = crossMode(
            iteration = iteration,
            population = population,
            weights = weights,
            model = model,
            parentAmountRange = parentAmountRange
        )
        return coroutineScope {
            parentGroups.map { parents ->
                async(Dispatchers.Default) {
                    cross(
                        iteration = iteration,
                        parents = parents,
                        model = model
                    ).map { newIndividual ->
                        val fixIndividual = newIndividual.mapIndexed { i, value ->
                            val flt64Value = converter.fromValue(value)
                            val fixedFlt64 = coerceIn(
                                iteration = iteration,
                                index = i,
                                value = flt64Value,
                                model = model
                            )
                            converter.intoValue(fixedFlt64)
                        }
                        Chromosome(
                            solution = fixIndividual,
                            fitness = model.objective(fixIndividual).ifNull { model.defaultObjective }
                        )
                    }
                }
            }.awaitAll()
        }.flatten()
    }

    /** 执行变异操作 / Execute mutation */
    override suspend fun mutate(
        iteration: Iteration,
        population: List<Chromosome<ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Chromosome<ObjValue, V>> {
        val weights = normalization(model, population.map { it.fitness })
        val mutationRate = mutationMode(
            iteration = iteration,
            population = population,
            weights = weights,
            model = model,
            mutationRateRange = mutationRateRange
        )
        return coroutineScope {
            population.mapIndexed { i, individual ->
                async(Dispatchers.Default) {
                    if (randomGenerator()!! geq mutationRate[i]) {
                        val newIndividual = mutation(
                            iteration = iteration,
                            individual = individual,
                            model = model,
                            mutationRate = mutationRate[i]
                        )
                        val fixIndividual = newIndividual.mapIndexed { j, value ->
                            val flt64Value = converter.fromValue(value)
                            val fixedFlt64 = coerceIn(
                                iteration = iteration,
                                index = j,
                                value = flt64Value,
                                model = model
                            )
                            converter.intoValue(fixedFlt64)
                        }
                        Chromosome(
                            solution = fixIndividual,
                            fitness = model.objective(fixIndividual).ifNull { model.defaultObjective }
                        )
                    } else {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }
}

@OptIn(ExperimentalTime::class)
/**
 * 遗传算法
 *
 * 实现基于种群的遗传算法，支持多种群协同进化、精英保留和周期性迁移。
 *
 * Genetic algorithm
 *
 * Implements population-based genetic algorithm, supporting multi-population co-evolution,
 * elite preservation, and periodic migration.
 *
 * @param Obj 目标类型 / objective type
 * @param ObjValue 目标值类型 / objective value type
 * @param V 值类型 / value type
 * @property population 种群构建参数列表 / population builder list
 * @property migrationPeriod 迁移周期 / migration period
 * @property solutionAmount 期望解的数量 / desired number of solutions
 * @property policy 遗传算法策略 / genetic algorithm policy
 */
class GeneAlgorithm<Obj, ObjValue, V>(
    val population: List<PopulationBuilder>,
    val migrationPeriod: UInt64,
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractGAPolicy<ObjValue, V>,
) where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    /**
     * 执行遗传算法 / Execute genetic algorithm
     *
     * @param model 回调模型 / callback model
     * @param runningCallBack 运行回调函数 / running callback function
     * @return 最优染色体列表 / best chromosome list
     */
    suspend operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, ObjValue, V>,
        runningCallBack: ((Iteration, Chromosome<ObjValue, V>, List<Chromosome<ObjValue, V>>, List<AbstractPopulation<ObjValue, V>>) -> Try)? = null
    ): List<Chromosome<ObjValue, V>> {
        val iteration = Iteration()
        val initialSolutions = model
            .initialSolutions(population.sumOf(UInt64) { it.densityRange.lowerBound.value.unwrap() })
            .map {
                Chromosome(
                    solution = it,
                    fitness = model.objective(it).ifNull { model.defaultObjective }
                )
            }
        var populations = population.mapIndexed { i, thisPopulation ->
            val fromIndex = population.take(i).sumOf(UInt64) { it.densityRange.lowerBound.value.unwrap() }
            val toIndex = fromIndex + thisPopulation.densityRange.upperBound.value.unwrap()
            val thisIndividuals = initialSolutions
                .subList(fromIndex.toInt(), toIndex.toInt())
                .sortedWithPartialThreeWayComparator { lhs, rhs ->
                    model.compareObjective(lhs.fitness, rhs.fitness)
                }
            AbstractPopulation(
                individuals = thisIndividuals,
                elites = thisIndividuals.take(thisPopulation.eliteAmount.toInt()),
                best = thisIndividuals.first(),
                eliteAmount = thisPopulation.eliteAmount,
                densityRange = thisPopulation.densityRange,
                mutationRateRange = thisPopulation.mutationRateRange,
                parentAmountRange = thisPopulation.parentAmountRange
            )
        }
        var bestChromosome = populations
            .map { it.best }
            .sortedWithPartialThreeWayComparator { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }
            .first()
        val goodChromosomes = initialSolutions
            .sortedWithPartialThreeWayComparator { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }
            .take(solutionAmount.toInt())
            .toMutableList()

        try {
            while (!policy.finished(iteration)) {
                var globalBetter = false

                if (migrationPeriod > UInt64.zero && iteration.iteration % migrationPeriod == UInt64.zero) {
                    populations = policy.migrate(
                        iteration = iteration,
                        populations = populations,
                        model = model
                    )
                }

                val newPopulationAndChromosomes = coroutineScope {
                    populations.map { population ->
                        async(Dispatchers.Default) {
                            val selected = policy.select(
                                iteration = iteration,
                                population = population,
                                model = model
                            )
                            val crossed = policy.cross(
                                iteration = iteration,
                                population = selected,
                                model = model,
                                parentAmountRange = population.parentAmountRange
                            )
                            val mutated = policy.mutate(
                                iteration = iteration,
                                population = crossed,
                                model = model,
                                mutationRateRange = population.mutationRateRange
                            )
                            val combined = (crossed + mutated + population.elites)
                                .sortedWithPartialThreeWayComparator { lhs, rhs ->
                                    model.compareObjective(lhs.fitness, rhs.fitness)
                                }
                            AbstractPopulation(
                                individuals = combined,
                                elites = combined.take(population.eliteAmount.toInt()),
                                best = combined.first(),
                                eliteAmount = population.eliteAmount,
                                densityRange = population.densityRange,
                                mutationRateRange = population.mutationRateRange,
                                parentAmountRange = population.parentAmountRange
                            ) to (crossed + mutated)
                        }
                    }.awaitAll()
                }
                populations = newPopulationAndChromosomes.map { it.first }
                val newChromosomes = newPopulationAndChromosomes
                    .flatMap { it.second }
                    .sortedWithPartialThreeWayComparator { lhs, rhs ->
                        model.compareObjective(lhs.fitness, rhs.fitness)
                    }
                val newBestChromosome = newChromosomes.first()
                refreshGoodIndividuals(
                    goodIndividuals = goodChromosomes,
                    newIndividuals = newChromosomes,
                    model = model,
                    solutionAmount = solutionAmount
                )
                if (model.compareObjective(newBestChromosome.fitness, bestChromosome.fitness) is Order.Less) {
                    bestChromosome = newBestChromosome
                    globalBetter = true
                }

                model.flush()
                policy.update(
                    iteration = iteration,
                    better = globalBetter,
                    bestIndividual = bestChromosome,
                    goodIndividuals = goodChromosomes,
                    populations = populations.map { it.individuals },
                    model = model
                )
                iteration.next(globalBetter)
                cleanupOnSolverMemoryPressure()

                if (runningCallBack?.invoke(iteration, bestChromosome, goodChromosomes, populations) is Failed) {
                    break
                }
            }

            return goodChromosomes.take(solutionAmount.toInt())
        } finally {
            cleanupAfterSolverRun()
        }
    }
}

/** 单目标遗传算法类型 / Single-objective genetic algorithm type */
typealias GA = GeneAlgorithm<Flt64, Flt64, Flt64>
/** 多目标遗传算法类型 / Multi-objective genetic algorithm type */
typealias MulObjGA = GeneAlgorithm<List<Pair<MultiObjectLocation<Flt64>, Flt64>>, List<Flt64>, Flt64>
