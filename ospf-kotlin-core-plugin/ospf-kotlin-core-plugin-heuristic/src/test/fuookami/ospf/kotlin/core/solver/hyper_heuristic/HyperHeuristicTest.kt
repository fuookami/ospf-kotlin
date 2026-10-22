package fuookami.ospf.kotlin.core.solver.hyper_heuristic

import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.callback.CallBackModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.runner.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.BestArchive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.NeverStop
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.persistence.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration.*

private fun <T> success(result: Ret<T>): T = when (result) {
    is Ok -> result.value
    is Failed -> fail(result.message ?: "operation failed")
    is Fatal -> fail(result.errors.toString())
}

private fun model(counter: AtomicInteger = AtomicInteger(), offset: Double = 0.0): CallBackModel<Flt64> {
    val model = CallBackModel(
        objectCategory = ObjectCategory.Minimum,
        initialSolutionGenerator = { (solution, variable) -> Flt64((solution.toInt() + variable.toInt()) % 5 - 2.0) },
        converter = IntoValue.Identity
    )
    model.add(RealVar("x"))
    model.add(RealVar("y"))
    model.addObject(ObjectCategory.Minimum, { solution: List<Flt64> ->
        counter.incrementAndGet()
        (solution[0] - Flt64(offset)).pow(2) + solution[1].pow(2)
    })
    return model
}

private fun registry(counter: AtomicInteger = AtomicInteger(), stateless: Boolean = true): SearchRegistry<Flt64, Flt64> {
    val registry = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
    success(registry.registerProblem(id = "quadratic", statelessCallbacks = stateless) { spec, _ ->
        val offset = spec.parameters["offset"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        Ok(singleObjectiveProblem(
            id = "quadratic",
            fingerprint = "quadratic:$offset",
            model = model(counter, offset),
            objectiveCodec = SearchCodecs.flt64,
            valueCodec = SearchCodecs.flt64
        ))
    })
    return registry
}

private fun config(types: List<String> = listOf("genetic", "pso"), mode: ExecutionMode = ExecutionMode.Synchronous): HyperHeuristicConfig {
    return HyperHeuristicConfig(
        problem = ComponentSpec("quadratic"),
        algorithms = types.mapIndexed { index, type -> AlgorithmConfig("$index-$type", ComponentSpec(type), size = 6) },
        budget = BudgetConfig(evaluations = 160, decisions = 12, sliceEvaluations = 40),
        execution = ExecutionConfig(mode = mode, parallelism = 2, seed = 123),
        persistence = PersistenceConfig(enabled = true)
    )
}

private fun stripTiming(value: JsonElement): JsonElement = when (value) {
    is JsonObject -> JsonObject(value.filterKeys { it != "activeNanos" }.mapValues { stripTiming(it.value) })
    is JsonArray -> JsonArray(value.map { stripTiming(it) })
    else -> value
}

class HyperHeuristicTest {
    @Test
    fun configurationFormatsShouldRoundTripAndRejectInvalidDefinitions() {
        val configured = config().copy(selection = ComponentSpec("q-learning"))
        for (format in ConfigurationFormat.entries) {
            assertEquals(configured, success(ConfigurationFormats.decode(success(ConfigurationFormats.encode(configured, format)), format)))
        }
        assertTrue(configured.copy(budget = BudgetConfig()).validate() is Failed)
        assertTrue(configured.copy(algorithms = configured.algorithms + configured.algorithms.first()).validate() is Failed)
        assertTrue(ConfigurationFormats.decode("{\"version\":1,\"version\":2}", ConfigurationFormat.Json) is Failed)
        assertTrue(ConfigurationFormats.decode("version: 1\nversion: 2", ConfigurationFormat.Yaml) is Failed)
    }

    @Test
    fun nestedAndConcurrentSlicesShouldNotOverspend() = runBlocking {
        val budget = SearchBudget(17)
        val root = SearchSlice(budget, 13)
        val granted = (0 until 100).map { async(Dispatchers.Default) { root.child(4, 1).acquire() } }.awaitAll().count { it }
        assertEquals(13, granted)
        assertEquals(13L, root.used)
        assertEquals(13L, budget.used)
        assertFalse(SearchSlice(budget, 0).acquire())
    }

    @Test
    fun allExecutionModesShouldRespectTheExactGlobalEvaluationLimit() = runBlocking {
        for (mode in ExecutionMode.entries) {
            val counter = AtomicInteger()
            val configuration = config(mode = mode).copy(budget = BudgetConfig(evaluations = 31, sliceEvaluations = 16))
            val result = success(HyperHeuristicSolver(configuration, registry(counter))())
            assertEquals(31L, result.evaluations)
            assertEquals(31L, counter.get().toLong())
            assertEquals(SearchTermination.EvaluationLimit, result.termination)
            assertNotNull(result.best)
        }
    }

    @Test
    fun nativeFamiliesShouldResumeWithoutLosingTheirState() = runBlocking {
        val types = listOf("genetic", "hill-climbing", "pso", "clpso", "shade", "l-shade", "cma-es")
        for (type in types) {
            val configuration = config(listOf(type)).copy(execution = ExecutionConfig(seed = 321), budget = BudgetConfig(evaluations = 200, decisions = 8, sliceEvaluations = 40))
            val solver = HyperHeuristicSolver(configuration, registry())
            val original = success(solver.open())
            val checkpoint = try {
                success(original.advance())
                success(original.advance())
                success(original.checkpoint())
            } finally {
                success(original.close())
            }
            val resumed = success(solver.open(checkpoint = checkpoint))
            val resumedResult: SearchResult<Flt64, Flt64>
            val resumedState: RunCheckpoint
            try {
                resumedResult = success(resumed.run())
                resumedState = success(resumed.checkpoint())
            } finally {
                success(resumed.close())
            }
            val continuous = success(solver.open())
            try {
                val expected = success(continuous.run())
                val expectedState = success(continuous.checkpoint())
                assertEquals(expected.archive.map { Pair(it.solution, it.fitness) }, resumedResult.archive.map { Pair(it.solution, it.fitness) }, type)
                assertEquals(expected.evaluations, resumedResult.evaluations, type)
                assertEquals(expectedState.workers.values.map { stripTiming(Json.encodeToJsonElement(SearchSessionSnapshot.serializer(), it.session)) }, resumedState.workers.values.map { stripTiming(Json.encodeToJsonElement(SearchSessionSnapshot.serializer(), it.session)) }, type)
                assertEquals(expectedState.workers.values.map { it.random }, resumedState.workers.values.map { it.random }, type)
            } finally {
                success(continuous.close())
            }
        }
    }

    @Test
    fun partiallyEvaluatedGenerationsShouldNotCommitNativeState() = runBlocking {
        val solver = HyperHeuristicSolver(config(listOf("pso")), registry())
        val run = success(solver.open())
        try {
            success(run.advance())
            val before = success(run.checkpoint())
            assertTrue(before.workers.values.single().initialized)
            assertTrue(before.workers.values.single().session.iteration > 0)
        } finally {
            success(run.close())
        }
        val problem = singleObjectiveProblem(
            id = "quadratic",
            fingerprint = "test",
            model = model(),
            objectiveCodec = SearchCodecs.flt64,
            valueCodec = SearchCodecs.flt64
        )
        val native = ParticleSwarmRunner<Flt64, Flt64>(
            size = 6,
            entropy = SearchRandom(1),
            archive = BestArchive(10),
            stopping = NeverStop,
            converter = IntoValue.Identity
        ).openSession(problem = problem, codec = RunnerStateCodecs.particles(problem))
        success(native.step(SearchSlice(SearchBudget(100), 6)))
        val before = success(native.snapshot())
        val partial = success(native.step(SearchSlice(SearchBudget(2), 2)))
        val after = success(native.snapshot())
        assertEquals(0L, partial.steps)
        assertEquals(SearchTermination.EvaluationLimit, partial.termination)
        assertEquals(before.state, after.state)
        assertEquals(before.iteration, after.iteration)
        problem.model.close()
    }

    @Test
    fun operatorPoliciesShouldReceiveActualStepFeedbackAndSurviveCheckpoints() = runBlocking {
        val algorithm = AlgorithmConfig(
            id = "adaptive",
            algorithm = ComponentSpec("genetic"),
            size = 6,
            updates = listOf(ComponentSpec("gwo"), ComponentSpec("sca")),
            mutations = listOf(ComponentSpec("gaussian"), ComponentSpec("none")),
            selection = ComponentSpec("q-learning")
        )
        val solver = HyperHeuristicSolver(config().copy(algorithms = listOf(algorithm)), registry())
        val run = success(solver.open())
        try {
            success(run.run())
            val policy = success(run.exportPolicy())
            assertEquals(setOf("update", "mutation"), policy.operators.getValue("adaptive").keys)
            assertTrue(policy.operators.getValue("adaptive").values.all { it.revision > 0 })
            val snapshot = success(run.checkpoint())
            val loaded = success(solver.open(checkpoint = snapshot))
            try {
                assertEquals(policy, success(loaded.exportPolicy()))
            } finally {
                success(loaded.close())
            }
        } finally {
            success(run.close())
        }
    }

    @Test
    fun unsupportedSlotsAndNonrecoverableCallbacksShouldFailBeforeSearch() = runBlocking {
        val c = config(listOf("cma-es"))
        val invalid = c.copy(algorithms = c.algorithms.map { it.copy(updates = listOf(ComponentSpec("gwo"))) })
        assertTrue(HyperHeuristicSolver(invalid, registry()).open() is Failed)
        assertTrue(HyperHeuristicSolver(c, registry(stateless = false)).open() is Failed)
        val badSize = config(listOf("shade")).copy(algorithms = listOf(AlgorithmConfig("small", ComponentSpec("shade"), size = 2)))
        assertTrue(HyperHeuristicSolver(badSize, registry()).open() is Failed)
        val unknown = config().copy(selection = ComponentSpec("unregistered"))
        assertTrue(HyperHeuristicSolver(unknown, registry()).open() is Failed)
    }

    @Test
    fun synchronousRunsAndSeparateInvocationsShouldBeDeterministic() = runBlocking {
        val solver = HyperHeuristicSolver(config(), registry())
        val first = success(solver())
        val second = success(solver())
        assertEquals(first.archive.map { Pair(it.solution, it.fitness) }, second.archive.map { Pair(it.solution, it.fitness) })
        assertEquals(first.evaluations, second.evaluations)
        assertEquals(first.decisions, second.decisions)
    }

    @Test
    fun fileStoresShouldRejectCorruptionAndPathTraversal() = runBlocking {
        val directory = Files.createTempDirectory("ospf-hyper-store-")
        val store = FileSearchStore(directory)
        val run = success(HyperHeuristicSolver(config(), registry()).open())
        try {
            success(run.advance())
            val checkpoint = success(run.checkpoint())
            success(store.saveCheckpoint("run.json", checkpoint))
            assertEquals(checkpoint, success(store.loadCheckpoint("run.json")))
            val policy = success(run.exportPolicy())
            success(store.savePolicy("policy.json", policy))
            assertEquals(policy, success(store.loadPolicy("policy.json")))
            assertTrue(store.loadPolicy("../policy.json") is Failed)
            Files.writeString(directory.resolve("policy.json"), "{}")
            assertTrue(store.loadPolicy("policy.json") is Failed)
        } finally {
            success(run.close())
        }
    }

    @Test
    fun instanceTrainingShouldFreezeHoldoutsAndRejectLeakingSplits() = runBlocking {
        fun instance(id: String, offset: Double) = TrainingCase(id, ComponentSpec("quadratic", buildJsonObject { put("offset", offset) }))
        val split = TrainingSplit(listOf(instance("train", 0.0)), listOf(instance("validation", 1.0)), listOf(instance("test", 2.0)))
        val configuration = config().copy(selection = ComponentSpec("q-learning"), budget = BudgetConfig(evaluations = 50, sliceEvaluations = 16))
        val report = success(PolicyTrainer(registry()).train(configuration, split, listOf(1, 2)))
        assertEquals(2, report.training.size)
        assertEquals(2, report.validation.size)
        assertEquals(2, report.test.size)
        assertTrue(report.policy.root.revision > 0)
        assertTrue(TrainingSplit(split.training, split.training).validate() is Failed)
    }
    @Test
    fun compositeSessionsShouldRestoreChildStreamsAndStablePolicyPaths() = runBlocking {
        for (type in listOf("island", "memetic")) {
            val composite = AlgorithmConfig(
                id = "composite",
                algorithm = ComponentSpec(type),
                children = listOf(
                    AlgorithmConfig("global", ComponentSpec("genetic"), size = 6, updates = listOf(ComponentSpec("gwo"), ComponentSpec("sca"))),
                    AlgorithmConfig("local", ComponentSpec("hill-climbing"), size = 6)
                )
            )
            val configuration = config().copy(
                algorithms = listOf(composite),
                budget = BudgetConfig(evaluations = 220, decisions = 20, sliceEvaluations = 40),
                execution = ExecutionConfig(seed = 782)
            )
            val solver = HyperHeuristicSolver(configuration, registry())
            val first = success(solver.open())
            val saved = try {
                repeat(8) { success(first.advance()) }
                success(first.checkpoint())
            } finally { success(first.close()) }
            val resumed = success(solver.open(checkpoint = saved))
            val continuous = success(solver.open())
            try {
                success(resumed.run())
                success(continuous.run())
                val expected = success(continuous.checkpoint())
                val actual = success(resumed.checkpoint())
                assertEquals(stripTiming(Json.encodeToJsonElement(RunCheckpoint.serializer(), expected)), stripTiming(Json.encodeToJsonElement(RunCheckpoint.serializer(), actual)), type)
                assertEquals(setOf("global/update"), actual.policy.operators.getValue("composite").keys)
            } finally {
                success(resumed.close())
                success(continuous.close())
            }
        }
    }

    @Test
    fun asynchronousStopAndCheckpointShouldDrainOutOfOrderFeedbackExactlyOnce() = runBlocking {
        val registered = registry()
        success(registered.registerAlgorithm("delayed") { context ->
            BuiltInAlgorithms.create(
                config = context.config.copy(algorithm = ComponentSpec("pso")),
                problem = context.problem,
                random = context.random,
                converter = IntoValue.Identity,
                bindings = context.bindings
            ).map { native ->
                object : SearchSession<Flt64, Flt64> by native {
                    override suspend fun step(budget: SearchSlice): Ret<SearchStepResult<Flt64, Flt64>> {
                        delay(if (context.config.id == "a-slow") 100 else 1)
                        return native.step(budget)
                    }
                }
            }
        })
        val configuration = config(mode = ExecutionMode.Asynchronous).copy(
            algorithms = listOf(AlgorithmConfig("a-slow", ComponentSpec("delayed"), size = 6), AlgorithmConfig("b-fast", ComponentSpec("delayed"), size = 6)),
            selection = ComponentSpec("q-learning"),
            budget = BudgetConfig(evaluations = 100, decisions = 8, sliceEvaluations = 20)
        )
        val solver = HyperHeuristicSolver(configuration, registered)
        val run = success(solver.open())
        try {
            assertTrue(success(run.advance()))
            assertEquals("b-fast", run.transitions().single().action)
            val saved = success(run.checkpoint())
            assertEquals(2L, saved.completed)
            assertEquals(2L, saved.dispatched)
            assertEquals(setOf(0L, 1L), saved.transitions.map { it.decision }.toSet())
            val loaded = success(solver.open(checkpoint = saved))
            try {
                loaded.requestStop()
                assertEquals(SearchTermination.Stopped, success(loaded.run()).termination)
                assertEquals(saved.transitions, loaded.transitions())
            } finally { success(loaded.close()) }
            run.requestStop()
            assertEquals(SearchTermination.Stopped, success(run.run()).termination)
            assertEquals(2L, success(run.exportPolicy()).root.revision)
        } finally { success(run.close()) }
    }

    @Test
    fun callbackFailuresShouldKeepCompletedCandidatesAndPairEveryDispatchedAction() = runBlocking {
        val registered = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
        success(registered.registerProblem(id = "fails", statelessCallbacks = true) { _, _ ->
            val calls = AtomicInteger()
            val base = model()
            base.addObject(ObjectCategory.Minimum, { _: List<Flt64> ->
                if (calls.incrementAndGet() == 3) throw IllegalStateException("expected test callback failure")
                Flt64.zero
            })
            Ok(singleObjectiveProblem(
                id = "fails",
                fingerprint = "fails:1",
                model = base,
                objectiveCodec = SearchCodecs.flt64,
                valueCodec = SearchCodecs.flt64
            ))
        })
        val run = success(HyperHeuristicSolver(config().copy(problem = ComponentSpec("fails"), selection = ComponentSpec("q-learning")), registered).open())
        try {
            assertTrue(run.run() is Failed)
            val result = run.result()
            assertEquals(SearchTermination.Failed, result.termination)
            assertTrue(result.archive.isNotEmpty())
            assertEquals(result.decisions, run.transitions().count { it.scope == "algorithm" }.toLong())
            assertTrue(run.transitions().all { it.failed && it.terminal })
            assertEquals(result.decisions, success(run.exportPolicy()).root.revision)
        } finally { success(run.close()) }
    }

    @Test
    fun invalidCandidatesAndUnknownFeasibilityShouldNotBecomeValidArchiveEntries() = runBlocking {
        val base = model()
        val problem = SearchProblem(
            id = "unknown",
            fingerprint = "unknown:1",
            model = base,
            objectiveCodec = SearchCodecs.flt64,
            valueCodec = SearchCodecs.flt64,
            dominates = { lhs, rhs -> lhs < rhs },
            feasible = { false }
        )
        val budget = SearchBudget(3)
        assertNull(success(problem.evaluate(listOf(Flt64.zero, Flt64.zero), SearchSlice(budget, 1))).candidate)
        assertNull(success(problem.evaluate(listOf(Flt64.zero), SearchSlice(budget, 1))).candidate)
        assertEquals(2L, budget.used)
        assertEquals(0L, budget.objectiveCalls)
        base.close()
        val tooSmall = config().copy(selection = ComponentSpec("q-learning", buildJsonObject { put("states", 2) }))
        assertTrue(HyperHeuristicSolver(tooSmall, registry()).open() is Failed)
    }

    @Test
    fun allRegisteredNativeAlgorithmsShouldExecuteUnderTheSameBudgetProtocol() = runBlocking {
        for (type in BuiltInAlgorithms.nativeIds) {
            val configuration = config(listOf(type)).copy(budget = BudgetConfig(evaluations = 80, decisions = 3, sliceEvaluations = 32))
            val result = success(HyperHeuristicSolver(configuration, registry())())
            assertTrue(result.evaluations <= 80, type)
            assertTrue(result.archive.isNotEmpty(), type)
        }
    }

    @Test
    fun kotlinDslShouldMatchFilesAndMigrationsMustBeExplicit() {
        val expected = config(listOf("pso")).copy(execution = ExecutionConfig(), persistence = PersistenceConfig())
        val configured = success(hyperHeuristic {
            problem = ComponentSpec("quadratic")
            budget = expected.budget
            algorithm(id = "0-pso", type = "pso") { size = 6 }
        })
        assertEquals(expected, configured)
        val current = success(ConfigurationFormats.encode(configured, ConfigurationFormat.Json))
        val future = JsonObject(Json.parseToJsonElement(current).jsonObject + ("version" to JsonPrimitive(2))).toString()
        assertTrue(ConfigurationFormats.decode(future, ConfigurationFormat.Json) is Failed)
        val migrated = success(ConfigurationFormats.decode(
            text = future,
            format = ConfigurationFormat.Json,
            migrations = mapOf(2 to ConfigurationMigration { document -> Ok(JsonObject(document + ("version" to JsonPrimitive(1)))) })
        ))
        assertEquals(configured, migrated)
        assertTrue(hyperHeuristic { budget = BudgetConfig(evaluations = 10) } is Failed)
    }

    @Test
    fun globalStepBudgetsShouldBeReservedAcrossConcurrentWorkers() = runBlocking {
        for (mode in ExecutionMode.entries) {
            val configuration = config(mode = mode).copy(budget = BudgetConfig(steps = 3, sliceEvaluations = 40, sliceSteps = 2))
            val result = success(HyperHeuristicSolver(configuration, registry())())
            assertEquals(3L, result.steps, mode.name)
            assertEquals(SearchTermination.StepLimit, result.termination)
        }
    }

    @Test
    fun statefulGlobalExtensionsShouldEitherRestoreTheirStateOrFailPreflight() = runBlocking {
        class CountedFeatures : FeatureExtractor, RecoverableComponent {
            private var count = 0
            override val version = "counted:1"
            override val stateCount = 64
            override fun extract(progress: SearchProgress): SelectionContext {
                ++count
                return ProgressFeatures.extract(progress).copy(features = listOf(Flt64(count.toDouble())))
            }
            override fun save(): Ret<JsonElement> = Ok(JsonPrimitive(count))
            override fun load(state: JsonElement): Try {
                count = state.jsonPrimitive.int
                return ok
            }
        }
        val registered = registry()
        success(registered.registerFeatures("counted") { CountedFeatures() })
        success(registered.registerReward("unrecoverable") { RewardPolicy { Flt64.one } })
        val invalid = config().copy(learning = LearningConfig(reward = ComponentSpec("unrecoverable")))
        assertTrue(HyperHeuristicSolver(invalid, registered).open() is Failed)
        val configuration = config().copy(learning = LearningConfig(features = ComponentSpec("counted")))
        val solver = HyperHeuristicSolver(configuration, registered)
        val original = success(solver.open())
        try {
            success(original.advance())
            val saved = success(original.checkpoint())
            assertTrue(saved.components.containsKey("features"))
            val loaded = success(solver.open(checkpoint = saved))
            try {
                success(original.run())
                success(loaded.run())
                assertEquals(original.transitions(), loaded.transitions())
                assertEquals(success(original.checkpoint()).components, success(loaded.checkpoint()).components)
            } finally { success(loaded.close()) }
        } finally { success(original.close()) }
    }

    @Test
    fun baselineComparisonShouldReportQualityCostsAndFailureRates() = runBlocking {
        val configured = config().copy(budget = BudgetConfig(evaluations = 80, sliceEvaluations = 32))
        val reports = success(PolicyEvaluator(registry()).compare(
            configurations = mapOf(
                "random" to configured.copy(selection = ComponentSpec("random")),
                "round-robin" to configured,
                "failed" to configured.copy(algorithms = listOf(AlgorithmConfig("missing", ComponentSpec("missing"))))
            ),
            cases = listOf(TrainingCase("quadratic", ComponentSpec("quadratic"))),
            seeds = listOf(1, 2),
            quality = { it.best?.fitness }
        ))
        assertEquals(Flt64.zero, reports.getValue("random").failureRate)
        assertEquals(2, reports.getValue("random").quality.size)
        assertEquals(160L, reports.getValue("round-robin").evaluations)
        assertEquals(Flt64.one, reports.getValue("failed").failureRate)
        assertTrue(reports.getValue("failed").quality.isEmpty())
    }

}
