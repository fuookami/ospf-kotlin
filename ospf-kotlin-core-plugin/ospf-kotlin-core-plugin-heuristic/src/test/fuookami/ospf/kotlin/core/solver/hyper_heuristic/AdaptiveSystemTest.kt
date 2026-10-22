package fuookami.ospf.kotlin.core.solver.hyper_heuristic

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.model.callback.CallBackModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.runner.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.memory.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration.*

private fun <T> checked(result: Ret<T>): T = when (result) {
    is Ok -> result.value
    is Failed -> fail(result.message)
    is Fatal -> fail(result.errors.toString())
}

private fun adaptiveProblem(): SearchProblem<Flt64, Flt64> {
    val model = CallBackModel(
        objectCategory = ObjectCategory.Minimum,
        initialSolutionGenerator = { _ -> Flt64(3.0) },
        converter = IntoValue.Identity
    )
    model.add(RealVar("x"))
    model.addObject(ObjectCategory.Minimum, { x: List<Flt64> -> x[0].pow(2) })
    return singleObjectiveProblem(
        id = "adaptive-quadratic",
        fingerprint = "adaptive-quadratic:1",
        model = model,
        objectiveCodec = SearchCodecs.flt64,
        valueCodec = SearchCodecs.flt64,
        objectiveCoordinates = { listOf(it) }
    )
}

private fun credit(action: String, reward: Double, decision: Long = 0): SelectionTransition = SelectionTransition(
    episode = "adaptive",
    decision = decision,
    action = action,
    before = SelectionContext(),
    after = SelectionContext(),
    available = listOf("a", "b"),
    nextAvailable = listOf("a", "b"),
    reward = Flt64(reward),
    evaluations = 1,
    terminal = false,
    policyVersion = 0
)

private fun point(x: Double, fitness: Double = x): Individual<Flt64, Flt64> = SolutionWithFitness(listOf(Flt64(x)), Flt64(fitness))

private fun permutationProblem(): SearchProblem<Flt64, Flt64> {
    val model = CallBackModel(
        objectCategory = ObjectCategory.Minimum,
        initialSolutionGenerator = { (_, variable) -> Flt64(2 - variable.toInt()) },
        converter = IntoValue.Identity
    )
    repeat(3) { model.add(RealVar("x$it")) }
    model.addObject(ObjectCategory.Minimum, { x: List<Flt64> -> x.mapIndexed { index, value -> Flt64(index) * value }.fold(Flt64.zero) { a, b -> a + b } })
    return singleObjectiveProblem(
        id = "permutation", fingerprint = "permutation:1", model = model,
        objectiveCodec = SearchCodecs.flt64, valueCodec = SearchCodecs.flt64,
        encoding = SearchEncoding.Permutation,
        validSolution = { it.size == 3 && it.toSet() == setOf(Flt64.zero, Flt64.one, Flt64.two) }
    )
}

private class DecrementUpdate(val name: String, val trace: MutableList<Pair<String, Double>>) : SolutionUpdate {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val parent = parents.first()
        val value = parent.solution[0].toFlt64().toDouble()
        trace.add(name to value)
        return listOf(listOf(converter.intoValue(Flt64((value - 1).coerceAtLeast(0.0)))))
    }
}

class AdaptiveSystemTest {
    @Test
    fun controlledFailuresShouldPairUsedActionsAndRecordActualSteps(): Unit = runBlocking {
        for (throws in listOf(false, true)) {
            val policy = ChoiceFunctionSelector(listOf("local"))
            val pool = OperatorPool<SolutionUpdate>(
                id = "update",
                candidates = mapOf("local" to PatternSearchUpdate()),
                policy = policy,
                random = SearchRandom(1),
                stateless = setOf("local")
            )
            var initialized = false
            val delegate = object : SearchSession<Flt64, Flt64> {
                override suspend fun step(budget: SearchSlice): Ret<SearchStepResult<Flt64, Flt64>> {
                    if (!initialized) {
                        initialized = true
                        return Ok(SearchStepResult(
                            candidates = listOf(point(3.0, 9.0)), evaluations = 0, steps = 1,
                            termination = SearchTermination.SliceComplete
                        ))
                    }
                    pool.current()
                    assertTrue(budget.acquire())
                    if (throws) error("test operator failure")
                    return Ok(SearchStepResult(
                        candidates = listOf(point(2.0, 4.0)), evaluations = 1, steps = 0,
                        termination = SearchTermination.EvaluationLimit
                    ))
                }

                override suspend fun importSolutions(solutions: List<Solution<Flt64>>, budget: SearchSlice): Ret<SearchStepResult<Flt64, Flt64>> = searchInvalid("unused")
                override fun snapshot(): Ret<SearchSessionSnapshot> = searchInvalid("unused")
                override fun restore(snapshot: SearchSessionSnapshot): Try = searchInvalid("unused")
            }
            val session = ControlledSearchSession(
                delegate = delegate,
                problem = adaptiveProblem(),
                bindings = OperatorBindings(update = pool, mutation = null, neighborhood = null, parameters = emptyMap()),
                reward = RewardPolicy { Flt64(Double.NaN) }
            )
            checked(session.step(SearchSlice(global = SearchBudget(5), evaluations = 5)))
            val failed = checked(session.step(SearchSlice(global = SearchBudget(5), evaluations = 5)))
            assertEquals(SearchTermination.Failed, failed.termination)
            val transition = session.drainTransitions().single()
            assertEquals(0L, transition.steps)
            assertEquals(1L, transition.evaluations)
            assertTrue(transition.failed && transition.terminal)
            assertEquals(Flt64.zero, transition.reward)
            assertEquals(JsonObject(emptyMap()), policy.snapshot().extension.jsonObject.getValue("pending"))
            if (!throws) assertEquals(listOf(point(2.0, 4.0)), failed.candidates)
        }
    }

    @Test
    fun invalidTopLevelRewardsShouldStillCompleteTerminalFeedback(): Unit = runBlocking {
        val registry = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
        checked(registry.registerProblem(id = "adaptive", statelessCallbacks = true) { _, _ -> Ok(adaptiveProblem()) })
        checked(registry.registerReward(id = "invalid", stateless = true) { RewardPolicy { Flt64(Double.NaN) } })
        val run = checked(HyperHeuristicSolver(
            config = HyperHeuristicConfig(
                problem = ComponentSpec("adaptive"),
                algorithms = listOf(AlgorithmConfig(id = "worker", algorithm = ComponentSpec("pso"), size = 4)),
                budget = BudgetConfig(evaluations = 40, sliceEvaluations = 10),
                selection = ComponentSpec("choice-function"),
                learning = LearningConfig(reward = ComponentSpec("invalid"))
            ),
            registry = registry
        ).open())
        assertTrue(run.run() is Failed)
        val event = run.transitions().single { it.scope == "algorithm" }
        assertTrue(event.failed && event.terminal)
        assertEquals(Flt64.zero, event.reward)
        assertEquals(JsonObject(emptyMap()), checked(run.exportPolicy()).root.extension.jsonObject.getValue("pending"))
        checked(run.close())
    }

    @Test
    fun frrShouldForgetOldCreditAndHonorMasks() {
        val policy = NonStationarySelector(
            type = "frr-mab", actions = listOf("a", "b"), windowSize = 4, exploration = 0.0
        )
        for (index in 0..3) checked(policy.observe(credit(if (index % 2 == 0) "a" else "b", if (index % 2 == 0) 10.0 else 0.0, index.toLong())))
        assertEquals("a", checked(policy.select(SelectionContext(), listOf("a", "b"), SearchRandom(1))))
        for (index in 4..7) checked(policy.observe(credit(if (index % 2 == 0) "a" else "b", if (index % 2 == 0) 0.0 else 1.0, index.toLong())))
        assertEquals("b", checked(policy.select(SelectionContext(), listOf("a", "b"), SearchRandom(1))))
        assertEquals("a", checked(policy.select(SelectionContext(), listOf("a"), SearchRandom(1))))
        assertTrue(policy.select(SelectionContext(), listOf("unknown"), SearchRandom(1)) is Failed)
    }

    @Test
    fun adaptivePursuitShouldFollowHandCalculatedProbabilityAndKeepFloor() {
        val policy = NonStationarySelector(
            type = "adaptive-pursuit", actions = listOf("a", "b"), rate = 0.5, minimumProbability = 0.1
        )
        checked(policy.observe(credit("a", 1.0)))
        val probabilities = policy.snapshot().extension.jsonObject.getValue("probabilities").jsonObject
        assertEquals(0.7, probabilities.getValue("a").jsonPrimitive.double, 1e-12)
        assertEquals(0.3, probabilities.getValue("b").jsonPrimitive.double, 1e-12)
        repeat(100) { checked(policy.observe(credit("a", 1.0, it.toLong() + 1))) }
        val final = policy.snapshot().extension.jsonObject.getValue("probabilities").jsonObject
        assertTrue(final.getValue("b").jsonPrimitive.double >= 0.1)
        assertEquals("b", checked(policy.select(SelectionContext(), listOf("b"), SearchRandom(1))))
    }

    @Test
    fun choiceFunctionShouldPairDispatchPredecessorAcrossOutOfOrderCompletion() {
        val policy = ChoiceFunctionSelector(listOf("a", "b"))
        checked(policy.select(SelectionContext(decision = 0), listOf("a"), SearchRandom(1)))
        checked(policy.observe(credit("a", 1.0)))
        checked(policy.select(SelectionContext(decision = 1), listOf("b"), SearchRandom(1)))
        checked(policy.select(SelectionContext(decision = 2), listOf("a"), SearchRandom(1)))
        checked(policy.observe(credit("a", 0.0, 2)))
        checked(policy.observe(credit("b", 5.0, 1)))
        val pairs = policy.snapshot().extension.jsonObject.getValue("pairs").jsonObject
        assertEquals(1.0, pairs.getValue("a").jsonObject.getValue("b").jsonPrimitive.double, 1e-12)
    }

    @Test
    fun everyAdaptiveSelectorShouldRestoreByIdsAndFreezeLearning() {
        for (type in listOf("frr-mab", "choice-function", "adaptive-pursuit")) {
            val source = NonStationarySelector(type, listOf("a", "b"))
            checked(source.observe(credit("b", 2.0)))
            val frozen = NonStationarySelector(type = type, actions = listOf("b", "a"), learning = false)
            checked(frozen.restore(source.snapshot()))
            val before = frozen.snapshot()
            checked(frozen.select(SelectionContext(), listOf("a", "b"), SearchRandom(1)))
            checked(frozen.observe(credit("a", 99.0)))
            assertEquals(before, frozen.snapshot())
            assertTrue(frozen.restore(before.copy(version = 99)) is Failed)
        }
    }

    @Test
    fun timeAndExtremeRewardsShouldMeasureGainsAndForgetOldBreakthroughs() {
        val context = RewardContext(
            improved = true, contribution = Flt64.one, evaluations = 5, elapsedNanos = 2_000_000_000,
            action = "a", reference = listOf(listOf(Flt64(10.0))), candidates = listOf(listOf(Flt64(6.0)))
        )
        assertEquals(2.0, TimeNormalizedReward().reward(context).toDouble(), 1e-8)
        val extreme = ExtremeValueReward(2)
        assertEquals(4.0, extreme.reward(context).toDouble())
        val saved = checked(extreme.save())
        assertEquals(4.0, extreme.reward(context.copy(candidates = context.reference)).toDouble())
        assertEquals(0.0, extreme.reward(context.copy(candidates = context.reference)).toDouble())
        checked(extreme.load(saved))
        assertEquals(4.0, extreme.reward(context.copy(candidates = context.reference)).toDouble())
    }

    @Test
    fun hypervolumeShouldUseUnionNotOffspringOrFrontSize() {
        val reward = HypervolumeDeltaReward(listOf(Flt64(5.0), Flt64(5.0)))
        val reference = listOf(listOf(Flt64(2.0), Flt64(3.0)))
        val context = RewardContext(
            improved = true, contribution = Flt64.one, evaluations = 1,
            reference = reference, candidates = listOf(listOf(Flt64(3.0), Flt64(2.0)))
        )
        assertEquals(2.0, reward.reward(context).toDouble())
        assertEquals(0.0, reward.reward(context.copy(candidates = reference + reference)).toDouble())
        assertEquals(0.0, reward.reward(context.copy(candidates = listOf(listOf(Flt64(4.0), Flt64(4.0))))).toDouble())
        assertTrue(!reward.reward(context.copy(candidates = listOf(listOf(Flt64.one)))).toDouble().isFinite())
    }

    @Test
    fun lateAcceptanceShouldUseDelayedHistoryAndRestoreIt() {
        val problem = adaptiveProblem()
        val acceptance = LateAcceptance<Flt64, Flt64>(SearchCodecs.flt64, 2)
        assertTrue(acceptance.accept(problem, listOf(point(10.0)), point(5.0)))
        val saved = checked(acceptance.save())
        assertTrue(acceptance.accept(problem, listOf(point(5.0)), point(8.0)))
        assertFalse(acceptance.accept(problem, listOf(point(8.0)), point(9.0)))
        checked(acceptance.load(saved))
        assertTrue(acceptance.accept(problem, listOf(point(5.0)), point(8.0)))
    }

    @Test
    fun recordAcceptanceShouldHandleNegativeValuesAndTightenToZero() {
        val acceptance = RecordToRecordAcceptance<Flt64, Flt64>(initialRatio = 0.1, horizon = 2)
        val problem = adaptiveProblem()
        assertTrue(acceptance.accept(problem, listOf(point(-100.0)), point(-95.0)))
        assertFalse(acceptance.accept(problem, listOf(point(-95.0)), point(-90.0)))
        assertFalse(acceptance.accept(problem, listOf(point(-95.0)), point(-99.0)))
        assertTrue(acceptance.accept(problem, listOf(point(-95.0)), point(-101.0)))
    }

    @Test
    fun stateBridgeShouldWarmStartVelocityAndRetainRealShadeMemory() {
        val problem = adaptiveProblem()
        val bridge = ContextStateSynchronizer(problem = problem, maxVelocity = 2.0)
        val particles = listOf(ParticleState(individual = point(3.0), velocity = listOf(Flt64.zero), best = point(1.0)))
        val imported = checked(bridge.particles(particles, listOf(point(6.0))))
        assertEquals(Flt64(2.0), imported.single().velocity.single())
        assertEquals(Flt64.one, imported.single().best.fitness)
        val memory = ShadeMemory(memoryF = listOf(0.7), memoryCr = listOf(0.2))
        val shade = ShadeState(population = listOf(point(3.0), point(4.0)), memory = memory, archive = emptyList())
        val aligned = checked(bridge.shade(shade, listOf(point(2.0))))
        assertEquals(memory, aligned.memory)
        assertEquals(listOf(Flt64(2.0), Flt64(3.0)), aligned.population.map { it.fitness })
        assertEquals(listOf(listOf(Flt64(4.0))), aligned.archive)
    }

    @Test
    fun stateBridgeShouldEstimatePositiveCovarianceAndResetPaths() {
        val bridge = ContextStateSynchronizer(adaptiveProblem())
        val original = CmaEsState<Flt64, Flt64>(
            mean = listOf(0.0), sigma = 0.3, covariance = listOf(listOf(1.0)),
            pathSigma = listOf(2.0), pathCovariance = listOf(3.0), generation = 10, best = point(0.0)
        )
        val bridged = checked(bridge.cmaEs(original, listOf(point(1.0), point(3.0))))
        assertEquals(listOf(2.0), bridged.mean)
        assertTrue(bridged.covariance.single().single() > 0)
        assertEquals(listOf(0.0), bridged.pathSigma)
        assertEquals(0, bridged.generation)
        assertEquals(original.best, bridged.best)
        assertEquals(original.covariance, checked(bridge.cmaEs(original, listOf(point(1.0)))).covariance)
    }

    @Test
    fun patternSearchShouldConvergeDeterministically() {
        val problem = adaptiveProblem()
        val results = PatternSearchUpdate(sweeps = 50)(
            iteration = Iteration(), parents = listOf(point(3.0, 9.0)), model = problem.model,
            converter = IntoValue.Identity, entropy = SearchRandom(1)
        )
        assertTrue(results.single()[0].toDouble().let { kotlin.math.abs(it) < 1e-5 }, results.toString())
    }

    @Test
    fun pipelineAndVndShouldReevaluateIntermediatesAndRestartOnImprovement() {
        val problem = adaptiveProblem()
        val trace = ArrayList<Pair<String, Double>>()
        val first = DecrementUpdate("first", trace)
        val second = DecrementUpdate("second", trace)
        val pipeline = PipelineCompositeUpdate(listOf(first, second))
        val result = pipeline(
            iteration = Iteration(), parents = listOf(point(3.0, 9.0)), model = problem.model,
            converter = IntoValue.Identity, entropy = SearchRandom(1)
        )
        assertEquals(listOf("first" to 3.0, "second" to 2.0), trace)
        assertEquals(Flt64.one, result.single().single())
        trace.clear()
        VNDCompositeUpdate(listOf(first, second))(
            iteration = Iteration(), parents = listOf(point(3.0, 9.0)), model = problem.model,
            converter = IntoValue.Identity, entropy = SearchRandom(1)
        )
        assertEquals(listOf("first", "first", "first", "first", "second"), trace.map { it.first })
        trace.clear()
        val elite = EliteLocalSearchWrapper(first, 1)(
            iteration = Iteration(), parents = listOf(point(2.0), point(3.0), point(4.0)), model = problem.model,
            converter = IntoValue.Identity, entropy = SearchRandom(1)
        )
        assertEquals(1, trace.size)
        assertEquals(listOf(1.0, 3.0, 4.0), elite.map { it.single().toDouble() })
    }

    @Test
    fun ruinAndRepairShouldPreservePermutationAndRejectIrreparableInputs() {
        val original = (0..6).toList()
        val ruined = checked(RandomRuin<Int>().remove(original, 4, SearchRandom(1)))
        assertEquals(3, ruined.retained.size)
        val estimate = InsertionCost<Int> { partial, item, position -> Ok(Flt64(kotlin.math.abs(position - item).toDouble() + partial.size)) }
        for (repair in listOf(GreedyInsertRepair(estimate), RegretKInsertRepair(estimate, 3))) {
            val result = checked(repair.repair(ruined))
            assertEquals(original, result.sorted())
        }
        assertTrue(RandomRuin<Int>().remove(listOf(1, 1), 1, SearchRandom(1)) is Failed)
        assertTrue(GreedyInsertRepair(InsertionCost<Int> { _, _, _ -> Ok(null) }).repair(ruined) is Failed)
        val worst = checked(WorstCostRuin<Int> { _, index -> Ok(Flt64(index)) }.remove(original, 2, SearchRandom(1)))
        assertEquals(listOf(5, 6), worst.removed)
        val related = checked(RelatednessRuin<Int> { a, b -> Ok(Flt64(kotlin.math.abs(a - b))) }.remove(original, 3, SearchRandom(1)))
        assertEquals(original, (related.retained + related.removed).sorted())
    }

    @Test
    fun constraintRankersShouldSeparateSearchToleranceFromFeasibility() {
        val candidates = listOf(
            ConstraintAssessment(value = "relaxed", objective = Flt64.zero, violation = Flt64(0.1)),
            ConstraintAssessment(value = "feasible", objective = Flt64.one, violation = Flt64.zero)
        )
        val evaluator = EpsilonConstraintEvaluator(initialEpsilon = 0.2, horizon = 10)
        assertEquals("relaxed", checked(evaluator.rank(candidates, 0)).first().value)
        assertEquals("feasible", checked(evaluator.rank(candidates, 10)).first().value)
        assertEquals("feasible", checked(StochasticRanking(0.0).rank(candidates, SearchRandom(1))).first().value)
        assertEquals("relaxed", checked(StochasticRanking(1.0).rank(candidates, SearchRandom(1))).first().value)
        assertTrue(evaluator.rank(candidates.map { it.copy(violation = Flt64(-1.0)) }, 0) is Failed)
    }

    @Test
    fun newComponentsShouldRoundTripFormatsAndRunWithCheckpoints(): Unit = runBlocking {
        val registry = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
        checked(registry.registerProblem(id = "adaptive", statelessCallbacks = true) { _, _ -> Ok(adaptiveProblem()) })
        val config = HyperHeuristicConfig(
            problem = ComponentSpec("adaptive"),
            algorithms = listOf(AlgorithmConfig(
                id = "local", algorithm = ComponentSpec("genetic"), size = 1,
                updates = listOf(ComponentSpec("pattern-search", buildJsonObject { put("sweeps", 2) })),
                mutations = listOf(ComponentSpec("none")), selection = ComponentSpec("adaptive-pursuit")
            )),
            budget = BudgetConfig(evaluations = 100, decisions = 5, sliceEvaluations = 20),
            selection = ComponentSpec("frr-mab"),
            memory = MemoryConfig(acceptance = ComponentSpec("late")),
            learning = LearningConfig(reward = ComponentSpec("extreme-value")),
            persistence = PersistenceConfig(enabled = true)
        )
        for (format in ConfigurationFormat.entries) assertEquals(config, checked(ConfigurationFormats.decode(checked(ConfigurationFormats.encode(config, format)), format)))
        val solver = HyperHeuristicSolver(config, registry)
        val run = checked(solver.open())
        repeat(2) { checked(run.advance()) }
        val checkpoint = checked(run.checkpoint())
        checked(run.close())
        val restored = checked(solver.open(checkpoint))
        val result = checked(restored.run())
        checked(restored.close())
        assertTrue(result.archive.isNotEmpty())
        assertTrue(result.evaluations <= 100)
        assertTrue(result.diagnostics.isEmpty(), result.diagnostics.toString())
    }

    @Test
    fun portfoliosShouldBeSmallAndEncodingSpecific() {
        val continuous = checked(OrthogonalOperatorPortfolio.continuous())
        assertEquals(5, continuous.size)
        assertTrue(continuous.flatMap { it.updates }.none { it.type == "order" })
        assertEquals(setOf("swap", "inversion"), checked(OrthogonalOperatorPortfolio.permutation()).single().mutations.map { it.type }.toSet())
        assertTrue(OrthogonalOperatorPortfolio.continuous(3) is Failed)
    }

    @Test
    fun choiceReplayShouldRetainRecordedPredecessorsAndRejectMismatchedFeedback() {
        val source = ChoiceFunctionSelector(listOf("a", "b"))
        checked(source.observe(credit("a", 1.0)))
        checked(source.select(SelectionContext(decision = 1), listOf("b"), SearchRandom(1)))
        assertEquals("a", source.predecessor(1))
        assertTrue(source.observe(credit("a", 2.0, 1)) is Failed)
        val recorded = credit("b", 5.0, 1).copy(previousAction = source.predecessor(1))
        checked(source.observe(recorded))
        val replay = ChoiceFunctionSelector(listOf("a", "b"))
        checked(replay.observe(credit("a", 1.0)))
        checked(replay.observe(recorded))
        assertEquals(source.snapshot(), replay.snapshot())
        checked(source.select(SelectionContext(decision = 2), listOf("a"), SearchRandom(1)))
        source.discard(2)
        assertEquals(JsonObject(emptyMap()), source.snapshot().extension.jsonObject.getValue("pending"))
    }

    @Test
    fun adaptiveCheckpointShouldMatchContinuousSearchForEveryNewSelector(): Unit = runBlocking {
        for (type in listOf("frr-mab", "choice-function", "adaptive-pursuit")) {
            val registry = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
            checked(registry.registerProblem(id = "adaptive", statelessCallbacks = true) { _, _ -> Ok(adaptiveProblem()) })
            val config = HyperHeuristicConfig(
                problem = ComponentSpec("adaptive"),
                algorithms = listOf("a", "b").map { id -> AlgorithmConfig(
                    id = id, algorithm = ComponentSpec("gwo"), size = 4,
                    updates = listOf(ComponentSpec("gwo"), ComponentSpec("hho")),
                    mutations = listOf(ComponentSpec("none")), selection = ComponentSpec(type)
                ) },
                budget = BudgetConfig(evaluations = 160, decisions = 8, sliceEvaluations = 30),
                selection = ComponentSpec(type),
                execution = ExecutionConfig(parallelism = 2),
                learning = LearningConfig(reward = ComponentSpec("extreme-value")),
                persistence = PersistenceConfig(enabled = true)
            )
            val solver = HyperHeuristicSolver(config, registry)
            val original = checked(solver.open())
            checked(original.advance())
            val saved = checked(original.checkpoint())
            val resumed = checked(solver.open(saved))
            val expected = checked(original.run())
            val actual = checked(resumed.run())
            assertEquals(expected.archive, actual.archive, type)
            assertEquals(expected.evaluations, actual.evaluations, type)
            assertEquals(checked(original.exportPolicy()), checked(resumed.exportPolicy()), type)
            assertEquals(original.transitions().map { it.copy(elapsedNanos = 0) }, resumed.transitions().map { it.copy(elapsedNanos = 0) }, type)
            checked(original.close())
            checked(resumed.close())
        }
    }

    @Test
    fun newSelectorsShouldRunAsynchronouslyWithoutDuplicateFeedback(): Unit = runBlocking {
        val registry = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
        checked(registry.registerProblem(id = "adaptive", statelessCallbacks = true) { _, _ -> Ok(adaptiveProblem()) })
        val config = HyperHeuristicConfig(
            problem = ComponentSpec("adaptive"),
            algorithms = listOf("a", "b").map { AlgorithmConfig(id = it, algorithm = ComponentSpec("pso"), size = 4) },
            budget = BudgetConfig(evaluations = 80, decisions = 8, sliceEvaluations = 10),
            selection = ComponentSpec("choice-function"),
            execution = ExecutionConfig(mode = ExecutionMode.Asynchronous, parallelism = 2),
            learning = LearningConfig(reward = ComponentSpec("time-normalized")),
            persistence = PersistenceConfig(enabled = true)
        )
        val run = checked(HyperHeuristicSolver(config, registry).open())
        val result = checked(run.run())
        val events = run.transitions().filter { it.scope == "algorithm" }
        assertEquals(result.decisions, events.size.toLong())
        assertEquals(events.size, events.map { it.decision }.distinct().size)
        assertTrue(events.all { it.elapsedNanos >= 0 && it.reward.toDouble().isFinite() })
        assertEquals(events.size.toLong(), checked(run.exportPolicy()).root.revision)
        checked(run.close())
    }

    @Test
    fun ruinSessionShouldShareBudgetsAndRestorePermutationAndRandomState(): Unit = runBlocking {
        val problem = permutationProblem()
        fun session(seed: Long) = RuinRecreateSearchSession(
            problem = problem,
            operator = RuinRecreateOperator(
                ruin = RandomRuin<Flt64>(),
                repair = RegretKInsertRepair(InsertionCost<Flt64> { _, item, position -> Ok(item * Flt64(position)) })
            ),
            amount = 2, random = SearchRandom(seed), statelessEstimates = true
        )
        val source = session(123)
        val budget = SearchBudget(4)
        checked(source.step(SearchSlice(global = budget, evaluations = 2, steps = 2)))
        val saved = checked(source.snapshot())
        val restored = session(999)
        checked(restored.restore(saved))
        val expected = checked(source.step(SearchSlice(global = budget, evaluations = 2, steps = 2)))
        val actual = checked(restored.step(SearchSlice(global = SearchBudget(2), evaluations = 2, steps = 2)))
        assertEquals(expected.candidates, actual.candidates)
        assertEquals(4L, budget.used)
        assertEquals(checked(source.snapshot()), checked(restored.snapshot()))
    }

    @Test
    fun domainRuinSessionShouldPlugIntoUnmodifiedCoordinator(): Unit = runBlocking {
        val registry = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
        checked(registry.registerProblem(id = "permutation", statelessCallbacks = true) { _, _ -> Ok(permutationProblem()) })
        checked(registry.registerAlgorithm("domain-ruin") { context -> Ok(RuinRecreateSearchSession(
            problem = context.problem,
            operator = RuinRecreateOperator(
                ruin = RandomRuin<Flt64>(),
                repair = RegretKInsertRepair(InsertionCost<Flt64> { _, item, position -> Ok(item * Flt64(position)) })
            ),
            amount = 2,
            random = context.random,
            statelessEstimates = true
        )) })
        val config = HyperHeuristicConfig(
            problem = ComponentSpec("permutation"),
            algorithms = listOf(AlgorithmConfig(id = "ruin", algorithm = ComponentSpec("domain-ruin"))),
            budget = BudgetConfig(evaluations = 12, sliceEvaluations = 3, sliceSteps = 3),
            selection = ComponentSpec("adaptive-pursuit"),
            persistence = PersistenceConfig(enabled = true)
        )
        val solver = HyperHeuristicSolver(config, registry)
        val original = checked(solver.open())
        checked(original.advance())
        val resumed = checked(solver.open(checked(original.checkpoint())))
        val expected = checked(original.run())
        val actual = checked(resumed.run())
        assertEquals(expected.archive, actual.archive)
        assertEquals(12L, actual.evaluations)
        assertEquals(checked(original.exportPolicy()), checked(resumed.exportPolicy()))
        assertTrue(actual.archive.all { it.solution.toSet() == setOf(Flt64.zero, Flt64.one, Flt64.two) })
        checked(original.close())
        checked(resumed.close())
    }
}
