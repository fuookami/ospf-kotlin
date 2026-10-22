package fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration

import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.runner.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.BestArchive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.ObjectiveFitnessEvaluator
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.NeverStop
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.ElitistSurvivor
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.RandomPerturbation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.Dominance
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.OperatorBindings

internal object BuiltInAlgorithms {
    val populationUpdates: Map<String, () -> SolutionUpdate> = linkedMapOf(
        "genetic" to { SinglePointCross }, "gwo" to { GreyWolfUpdate() }, "sca" to { SineCosineUpdate() },
        "mvo" to { MultiVerseUpdate() }, "hs" to { HarmonySearchUpdate() }, "wca" to { WaterCycleUpdate() },
        "soa" to { SeagullUpdate() }, "warso" to { WarStrategyUpdate() }, "evo" to { EnergyValleyUpdate() },
        "gco" to { GerminalCenterUpdate() }, "de" to { DifferentialCross() }, "woa" to { WhaleOptimizationUpdate() },
        "hho" to { HarrisHawkUpdate() }, "aoa" to { ArithmeticOptimizationUpdate() }, "tlbo" to { TeachingLearningUpdate() },
        "ssa" to { SalpSwarmUpdate() }, "gsa" to { GravitationalSearchUpdate() }, "fa" to { FireflyUpdate() },
        "cs" to { CuckooSearchUpdate() }, "ba" to { BatAlgorithmUpdate() }, "mfo" to { MothFlameUpdate() },
        "goa" to { GrasshopperUpdate() }
    )
    val nativeIds = populationUpdates.keys + setOf("pso", "apso", "clpso", "shade", "l-shade", "cma-es", "hill-climbing", "ns", "random-walk", "simulated-annealing")

    fun slots(type: String): Set<String> = when (type) {
        in populationUpdates.keys, "nsga-ii", "nsga-iii" -> setOf("update", "mutation")
        "hill-climbing", "ns", "random-walk", "simulated-annealing" -> setOf("neighborhood")
        else -> emptySet()
    }

    fun parameters(type: String): Set<String> = when (type) {
        "pso", "apso" -> setOf("inertia", "personalWeight", "socialWeight")
        "clpso" -> setOf("inertia")
        "l-shade" -> setOf("sizeRatio")
        "simulated-annealing" -> setOf("temperature")
        else -> emptySet()
    }

    fun <O, V> create(
        config: AlgorithmConfig,
        problem: SearchProblem<O, V>,
        random: SearchRandom,
        converter: IntoValue<V>,
        bindings: OperatorBindings
    ): Ret<SearchSession<O, V>> where V : RealNumber<V>, V : NumberField<V> {
        val type = config.algorithm.type
        val archive = BestArchive<O, V>(100)
        val p = config.algorithm.parameters
        val rate = Flt64(p.number("mutationRate", 0.1))
        if (type in populationUpdates) {
            val runner = PopulationRunner<O, V>(
                size = config.size,
                fitnessEvaluator = ObjectiveFitnessEvaluator,
                update = bindings.solutionUpdate ?: populationUpdates.getValue(type)(),
                mutation = bindings.mutationOperator ?: if (type == "genetic") GaussianMutation() else NoMutation,
                survivor = ElitistSurvivor,
                mutationRate = rate,
                entropy = random,
                archive = archive,
                stopping = NeverStop,
                converter = converter
            )
            return Ok(runner.openSession(
                problem = problem,
                codec = RunnerStateCodecs.population(problem),
                importer = { state, incoming -> Ok(ElitistSurvivor(problem.model, state, incoming, config.size)) }
            ))
        }
        if (type in setOf("hill-climbing", "ns", "random-walk", "simulated-annealing")) {
            val acceptance = when (type) {
                "random-walk" -> AlwaysAcceptance
                "simulated-annealing" -> MetropolisAcceptance(
                    temperature = bindings.parameter("temperature", GeometricSource(Flt64(100.0), Flt64(0.95))),
                    entropy = random
                )
                else -> BetterOnlyAcceptance
            }
            val runner = TrajectoryRunner<O, V>(
                neighborhood = bindings.neighborhoodOperator ?: RandomPerturbation(1),
                acceptance = acceptance,
                fitnessEvaluator = ObjectiveFitnessEvaluator,
                entropy = random,
                archive = archive,
                stopping = NeverStop,
                converter = converter
            )
            return Ok(runner.openSession(
                problem = problem,
                codec = RunnerStateCodecs.trajectory(problem),
                importer = { state, incoming -> Ok(incoming.firstOrNull() ?: state) }
            ))
        }
        if (type == "pso" || type == "apso") {
            val runner = ParticleSwarmRunner<O, V>(
                size = config.size,
                inertia = bindings.parameter("inertia", if (type == "apso") LinearSource(
                    from = Flt64(0.9),
                    to = Flt64(0.4),
                    limit = UInt64(1000)
                ) else ConstantSource(Flt64(0.4))),
                personalWeight = bindings.parameter("personalWeight", ConstantSource(Flt64(2.0))),
                socialWeight = bindings.parameter("socialWeight", ConstantSource(Flt64(2.0))),
                maxVelocity = Flt64(p.number("maxVelocity", 10000.0)),
                fitnessEvaluator = ObjectiveFitnessEvaluator,
                entropy = random,
                archive = archive,
                stopping = NeverStop,
                converter = converter
            )
            return Ok(runner.openSession(
                problem = problem,
                codec = RunnerStateCodecs.particles(problem),
                importer = { state, incoming ->
                    val keep = state.sortedWith { a, b -> compare(problem, a.best, b.best) }.take((state.size - incoming.size).coerceAtLeast(0))
                    Ok((keep + incoming.take(state.size).map { ParticleState(
                        individual = it,
                        velocity = List(it.solution.size) { Flt64.zero },
                        best = it
                    ) }).take(state.size))
                }
            ))
        }
        if (type == "clpso") {
            val runner = ClpsoRunner<O, V>(
                size = config.size,
                inertia = bindings.parameter("inertia", LinearSource(
                    from = Flt64(0.9),
                    to = Flt64(0.4),
                    limit = UInt64(1000)
                )),
                learningWeight = Flt64(p.number("learningWeight", 1.49445)),
                maxVelocity = Flt64(p.number("maxVelocity", 10000.0)),
                refreshInterval = p.integer("refreshInterval", 7),
                stagnationLimit = p.integer("stagnationLimit", 5),
                fitnessEvaluator = ObjectiveFitnessEvaluator,
                entropy = random,
                archive = archive,
                stopping = NeverStop,
                converter = converter
            )
            return Ok(runner.openSession(
                problem = problem,
                codec = RunnerStateCodecs.clpso(problem),
                importer = { state, incoming ->
                    val keep = state.sortedWith { a, b -> compare(problem, a.best, b.best) }.take((state.size - incoming.size).coerceAtLeast(0))
                    Ok((keep + incoming.take(state.size).map { individual ->
                        ClpsoParticle(
                            individual = individual,
                            velocity = List(individual.solution.size) { Flt64.zero },
                            best = individual,
                            exemplars = List(individual.solution.size) { 0 },
                            stagnation = 0
                        )
                    }).take(state.size).mapIndexed { index, particle ->
                        particle.copy(exemplars = List(particle.individual.solution.size) { index }, stagnation = 0)
                    })
                }
            ))
        }
        if (type == "shade" || type == "l-shade") {
            val runner = ShadeRunner<O, V>(
                size = config.size,
                memorySize = p.integer("memorySize", 5),
                pBestRate = p.number("pBestRate", 0.2),
                sizeRatio = bindings.parameter("sizeRatio", if (type == "l-shade") LinearSource(
                    from = Flt64.one,
                    to = Flt64(p.integer("minimumSize", 4).toDouble() / config.size),
                    limit = UInt64(p.integer("sizeReductionSteps", 1000))
                ) else ConstantSource(Flt64.one)),
                minimumSize = p.integer("minimumSize", 4),
                fitnessEvaluator = ObjectiveFitnessEvaluator,
                entropy = random,
                archive = archive,
                stopping = NeverStop,
                converter = converter
            )
            return Ok(runner.openSession(
                problem = problem,
                codec = RunnerStateCodecs.shade(problem),
                importer = { state, incoming ->
                    Ok(state.copy(population = ElitistSurvivor(problem.model, state.population, incoming, state.population.size)))
                }
            ))
        }
        if (type == "cma-es") {
            val sigma = p.number("initialSigma", 0.3)
            val runner = CmaEsRunner<O, V>(
                initialSigma = sigma,
                fitnessEvaluator = ObjectiveFitnessEvaluator,
                entropy = random,
                archive = archive,
                stopping = NeverStop,
                converter = converter
            )
            return Ok(runner.openSession(
                problem = problem,
                codec = RunnerStateCodecs.cmaEs(problem),
                importer = if (p["restartOnImport"]?.jsonPrimitive?.booleanOrNull != true) null else { state, incoming ->
                        val seed = incoming.firstOrNull()
                        if (seed == null) Ok(state) else {
                            val dimension = seed.solution.size
                            Ok(CmaEsState(
                                mean = seed.solution.map { it.toFlt64().toDouble() },
                                sigma = sigma,
                                covariance = List(dimension) { row -> List(dimension) { column -> if (row == column) 1.0 else 0.0 } },
                                pathSigma = List(dimension) { 0.0 },
                                pathCovariance = List(dimension) { 0.0 },
                                generation = 0,
                                best = seed
                            ))
                        }
                }
            ))
        }
        return searchInvalid("unknown native algorithm: $type")
    }

    fun <V> nsga(
        config: AlgorithmConfig,
        problem: SearchProblem<List<Flt64>, V>,
        random: SearchRandom,
        converter: IntoValue<V>,
        bindings: OperatorBindings
    ): Ret<SearchSession<List<Flt64>, V>> where V : RealNumber<V>, V : NumberField<V> {
        val dominance = object : Dominance {
            override fun dominates(lhs: List<Flt64>, rhs: List<Flt64>): Boolean = problem.dominates(lhs, rhs)
        }
        val runner = if (config.algorithm.type == "nsga-iii") Nsga3Runner(
            size = config.size,
            objectivesOf = { emptyList() },
            dominance = dominance,
            update = bindings.solutionUpdate ?: SimulatedBinaryCross(),
            mutation = bindings.mutationOperator ?: PolynomialMutation(),
            mutationRate = Flt64(config.algorithm.parameters.number("mutationRate", 0.1)),
            entropy = random,
            stopping = NeverStop,
            converter = converter,
            archiveAmount = 100,
            divisions = config.algorithm.parameters.integer("divisions", 8)
        ) else Nsga2Runner(
            size = config.size,
            objectivesOf = { emptyList() },
            dominance = dominance,
            update = bindings.solutionUpdate ?: SimulatedBinaryCross(),
            mutation = bindings.mutationOperator ?: PolynomialMutation(),
            mutationRate = Flt64(config.algorithm.parameters.number("mutationRate", 0.1)),
            entropy = random,
            stopping = NeverStop,
            converter = converter,
            archiveAmount = 100
        )
        return Ok(runner.openSession(
            problem = problem,
            codec = RunnerStateCodecs.population(problem),
            importer = { state, incoming -> Ok(runner.selectImported(state + incoming)) }
        ))
    }

    private fun <O, V> compare(
        problem: SearchProblem<O, V>,
        lhs: Individual<O, V>,
        rhs: Individual<O, V>
    ): Int
        where V : RealNumber<V>, V : NumberField<V> = when {
        problem.dominates(lhs.fitness, rhs.fitness) -> -1
        problem.dominates(rhs.fitness, lhs.fitness) -> 1
        else -> 0
    }
}

internal fun JsonObject.number(name: String, default: Double): Double = (get(name) as? JsonPrimitive)?.doubleOrNull ?: default
internal fun JsonObject.integer(name: String, default: Int): Int = (get(name) as? JsonPrimitive)?.intOrNull ?: default
