package fuookami.ospf.kotlin.core.solver.hyper_heuristic

import kotlin.math.abs
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.callback.CallBackModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.runner.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.BestArchive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.NeverStop
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.memory.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration.*

private fun <T> boundaryValue(result: Ret<T>): T = when (result) {
    is Ok -> result.value
    else -> fail(result.toString())
}

private fun hvContext(old: List<List<Double>>, fresh: List<List<Double>>): RewardContext = RewardContext(
    improved = true,
    contribution = Flt64.one,
    evaluations = 1,
    reference = old.map { point -> point.map { Flt64(it) } },
    candidates = fresh.map { point -> point.map { Flt64(it) } }
)

private fun boundaryPoint(x: Double, objective: Double = x): Individual<Flt64, Flt64> =
    SolutionWithFitness(listOf(Flt64(x)), Flt64(objective))

private fun boundaryProblem(
    violation: ((Solution<Flt64>) -> Ret<Flt64>)? = { Ok(Flt64(abs(it[0].toDouble()))) },
    feasible: (Solution<Flt64>) -> Boolean = { abs(it[0].toDouble()) < 1e-12 },
    admissible: (Solution<Flt64>) -> Boolean = feasible
): SearchProblem<Flt64, Flt64> {
    val model = CallBackModel(
        objectCategory = ObjectCategory.Minimum,
        initialSolutionGenerator = { (index, _) -> Flt64(index.toInt() % 4) },
        converter = IntoValue.Identity
    )
    model.add(RealVar("x"))
    model.addObject(ObjectCategory.Minimum, { solution: List<Flt64> -> -solution[0] })
    return SearchProblem(
        id = "boundary",
        fingerprint = "boundary:1",
        model = model,
        objectiveCodec = SearchCodecs.flt64,
        valueCodec = SearchCodecs.flt64,
        dominates = { a, b -> a < b },
        valid = { it.toDouble().isFinite() },
        feasible = feasible,
        admissible = admissible,
        objectiveCoordinates = { listOf(it) },
        constraintViolation = violation
    )
}

private fun noTiming(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> JsonObject(element.filterKeys { it != "activeNanos" && it != "elapsedNanos" }.mapValues { noTiming(it.value) })
    is JsonArray -> JsonArray(element.map { noTiming(it) })
    else -> element
}

class SearchBoundaryTest {
    @Test
    fun autoCapacityShouldMatchSharedSquareRootBoundaryVectors() {
        for ((horizon, capacity) in listOf(0L to 8, 63L to 8, 64L to 8, 80L to 8, 81L to 9, 99L to 9, 100L to 10,
            65535L to 255, 65536L to 256, 65537L to 256, Long.MAX_VALUE to 256)) {
            val policy = AutoAcceptance<Flt64, Flt64>(SearchCodecs.flt64, horizon)
            assertEquals(capacity, boundaryValue(policy.save()).jsonObject.getValue("capacity").jsonPrimitive.int)
            assertEquals(capacity / 2, policy.historySpan)
        }
    }

    @Test
    fun epsilonShouldMatchSharedStrictMathBinary64Vectors() {
        val config = ConstraintHandlingConfig(
            mode = ConstraintMode.Epsilon, initialEpsilon = 0.75, horizon = 10001, exponent = 1.37
        )
        val handling = SearchConstraintHandling(config, boundaryProblem(), SearchRandom(42))
        for ((steps, expected) in listOf(1283L to 0x3fe3e28720023000L, 1284L to 0x3fe3e1ba572774e1L, 2028L to 0x3fe1982836c8e2ecL)) {
            boundaryValue(handling.load(handling.save().copy(steps = steps)))
            assertEquals(expected, handling.epsilon.toRawBits())
        }
        assertEquals(0x40223f54a1c504c2L, StrictMath.log(9170.0).toRawBits())
        assertEquals(0x3fd799126e159c7eL, StrictMath.pow(547.0 / 10001.0, 1.0 / 3.0 + 0.01).toRawBits())
    }

    @Test
    fun exactHypervolumeShouldMatchThreeAndFourDimensionalInclusionExclusion() {
        for (dimension in 1..6) {
            val reference = List(dimension) { Flt64(5.0) }
            val old = List(dimension) { 2.0 }
            val fresh = List(dimension) { if (it == 0) 1.0 else 3.0 }
            val context = hvContext(listOf(old), listOf(fresh, fresh, List(dimension) { 4.0 }))
            val measured = boundaryValue(HypervolumeDeltaReward(reference).measure(context))
            val expected = Math.pow(2.0, (dimension - 1).toDouble())
            assertEquals(expected, measured.delta, 1e-10, "dimension=$dimension")
            assertTrue(measured.exact)
        }
    }

    @Test
    fun hypervolumeShouldRejectMalformedGeometryAndNeverSilentlyApproximate() {
        val reward = HypervolumeDeltaReward(listOf(Flt64(5.0), Flt64(5.0), Flt64(5.0)))
        assertEquals(0.0, boundaryValue(reward.measure(hvContext(emptyList(), emptyList()))).delta)
        assertEquals(0.0, boundaryValue(reward.measure(hvContext(emptyList(), listOf(listOf(6.0, 0.0, 0.0))))).delta)
        assertTrue(reward.measure(hvContext(emptyList(), listOf(listOf(1.0)))) is Failed)
        assertTrue(reward.measure(hvContext(emptyList(), listOf(listOf(Double.NaN, 1.0, 1.0)))) is Failed)
        val bounded = HypervolumeDeltaReward(referencePoint = List(3) { Flt64(5.0) }, maxWork = 1)
        assertTrue(bounded.evaluate(hvContext(emptyList(), listOf(listOf(1.0, 2.0, 3.0)))) is Failed)
        assertNull(bounded.lastMeasurement)
    }

    @Test
    fun sampledHypervolumeShouldBeReproducibleAndReportAnErrorBound() {
        val context = hvContext(listOf(listOf(2.0, 2.0, 2.0)), listOf(listOf(1.0, 3.0, 3.0)))
        val reward = HypervolumeDeltaReward(
            referencePoint = List(3) { Flt64(5.0) },
            mode = HypervolumeMode.Sampled,
            samples = 30000,
            seed = 732
        )
        val first = boundaryValue(reward.measure(context))
        assertEquals(first, boundaryValue(reward.measure(context)))
        assertFalse(first.exact)
        assertEquals(30000, first.samples)
        assertTrue(abs(first.delta - 4.0) <= first.errorBound)
        assertTrue(first.standardError > 0)
        val state = boundaryValue(reward.save())
        val copy = HypervolumeDeltaReward(
            referencePoint = List(3) { Flt64(5.0) },
            mode = HypervolumeMode.Sampled,
            samples = 30000,
            seed = 732
        )
        boundaryValue(copy.load(state))
        assertEquals(first, copy.lastMeasurement)
        assertEquals(0.0, boundaryValue(copy.measure(context.copy(candidates = context.reference))).delta)
    }

    @Test
    fun automaticReferenceShouldFreezeAndRestoreWithoutRecalibration() {
        val reward = HypervolumeDeltaReward(calibrationObservations = 2)
        val context = hvContext(emptyList(), listOf(listOf(-2.0, 0.0, 3.0)))
        assertFalse(boundaryValue(reward.measure(context)).calibrated)
        assertTrue(reward.measure(hvContext(emptyList(), listOf(listOf(1.0, 2.0)))) is Failed)
        val saved = boundaryValue(reward.save())
        val copy = HypervolumeDeltaReward(calibrationObservations = 2)
        boundaryValue(copy.load(saved))
        assertEquals(boundaryValue(reward.measure(context)), boundaryValue(copy.measure(context)))
        val frozen = boundaryValue(copy.save()).jsonObject.getValue("reference")
        boundaryValue(copy.measure(hvContext(emptyList(), listOf(listOf(100.0, 100.0, 100.0)))))
        assertEquals(frozen, boundaryValue(copy.save()).jsonObject.getValue("reference"))
        assertTrue(boundaryValue(copy.measure(context)).delta > 0)
        val corrupt = JsonObject(saved.jsonObject + ("observations" to JsonPrimitive(100)))
        assertTrue(copy.load(corrupt) is Failed)
    }

    @Test
    fun automaticAcceptanceShouldBeInvariantToScaleAndTranslation() {
        val problem = boundaryProblem()
        val values = listOf(10.0, 5.0, 8.0, 7.0, 12.0, 4.0, 6.0, 5.0, 20.0, 3.0, 3.0)
        fun path(scale: Double, offset: Double): List<Boolean> {
            val policy = AutoAcceptance<Flt64, Flt64>(SearchCodecs.flt64, 81)
            var current = boundaryPoint(values.first() * scale + offset)
            return values.drop(1).map { value ->
                val candidate = boundaryPoint(value * scale + offset)
                policy.accept(problem, listOf(current), candidate).also { if (it) current = candidate }
            }
        }
        assertEquals(path(1.0, 0.0), path(1e8, -3e9))
        assertEquals(path(1.0, 0.0), path(0.01, -100.0))
    }

    @Test
    fun automaticAcceptanceShouldAdaptAndResumeItsExactHistory() {
        val problem = boundaryProblem()
        val policy = AutoAcceptance<Flt64, Flt64>(SearchCodecs.flt64, 64)
        repeat(8) { policy.accept(problem, listOf(boundaryPoint(1.0)), boundaryPoint(2.0)) }
        assertEquals(8, policy.historySpan)
        val saved = boundaryValue(policy.save())
        val resumed = AutoAcceptance<Flt64, Flt64>(SearchCodecs.flt64, 64)
        boundaryValue(resumed.load(saved))
        repeat(8) {
            assertEquals(
                policy.accept(problem, listOf(boundaryPoint(2.0)), boundaryPoint(1.0)),
                resumed.accept(problem, listOf(boundaryPoint(2.0)), boundaryPoint(1.0))
            )
        }
        assertEquals(4, policy.historySpan)
        assertEquals(boundaryValue(policy.save()), boundaryValue(resumed.save()))
        assertTrue(AutoAcceptance<Flt64, Flt64>(SearchCodecs.flt64, 65).load(saved) is Failed)
    }

    @Test
    fun nativeConstraintOrderingShouldUseSolutionsNotPunishedObjectives() {
        val problem = boundaryValue(boundaryProblem().withConstraints(ConstraintHandlingConfig(), SearchRandom(1)))
        val feasible = boundaryPoint(0.0, 100.0)
        val infeasible = boundaryPoint(2.0, -100.0)
        assertTrue(compareIndividuals(model = problem.model, lhs = feasible, rhs = infeasible) < 0)
        assertTrue(compareIndividuals(model = problem.model, lhs = boundaryPoint(1.0, 7.0), rhs = boundaryPoint(2.0, 7.0)) < 0)
        assertEquals(listOf(feasible, infeasible), rankIndividuals(problem.model, listOf(infeasible, feasible)))
        assertEquals(Flt64(-100.0), infeasible.fitness)
    }

    @Test
    fun epsilonShouldCalibrateOnceAndTightenOnlyOnCommittedSteps() {
        val problem = boundaryValue(boundaryProblem().withConstraints(
            ConstraintHandlingConfig(mode = ConstraintMode.Epsilon, horizon = 3), SearchRandom(1)
        ))
        val handling = constraintHandling(problem.model)!!
        handling.begin()
        handling.observe(listOf(Flt64(4.0)))
        handling.commit()
        assertEquals(4.0 * 4.0 / 9.0, handling.epsilon, 1e-12)
        val saved = handling.save()
        handling.begin()
        handling.observe(listOf(Flt64(100.0)))
        boundaryValue(handling.load(saved))
        assertEquals(saved, handling.save())
        repeat(2) { handling.begin(); handling.commit() }
        assertEquals(0.0, handling.epsilon)
    }

    @Test
    fun stochasticRankingShouldUseBatchComparisonsAndRestoreItsRandomStream() {
        val entries = listOf(boundaryPoint(0.0, 100.0), boundaryPoint(1.0, 10.0), boundaryPoint(2.0, -100.0))
        for (probability in listOf(0.0, 0.45, 1.0)) {
            val problem = boundaryValue(boundaryProblem().withConstraints(
                ConstraintHandlingConfig(mode = ConstraintMode.StochasticRanking, objectiveProbability = probability), SearchRandom(42)
            ))
            val handling = constraintHandling(problem.model)!!
            val saved = handling.save()
            val ranked = rankIndividuals(problem.model, entries)
            boundaryValue(handling.load(saved))
            assertEquals(ranked, rankIndividuals(problem.model, entries))
            if (probability == 0.0) assertEquals(entries, ranked)
            if (probability == 1.0) assertEquals(entries.reversed(), ranked)
            assertTrue(compareIndividuals(model = problem.model, lhs = entries[0], rhs = entries[2]) < 0)
        }
    }

    @Test
    fun explicitSearchDomainAndUnknownFeasibilityMustNotBeRelaxedAway() {
        val source = boundaryProblem(
            violation = { Ok(Flt64.zero) },
            feasible = { false },
            admissible = { it[0] >= Flt64.zero }
        )
        val problem = boundaryValue(source.withConstraints(ConstraintHandlingConfig(), SearchRandom(1)))
        assertFalse(problem.admissible(listOf(Flt64(-1.0))))
        val memory = SearchMemory(problem = source, capacity = 20, workingCapacity = 20, acceptance = NotWorseAcceptancePolicy())
        memory.offer(listOf(boundaryPoint(0.0)))
        assertTrue(memory.archive.isEmpty())
        assertFalse(problem.feasible(listOf(Flt64.zero)))
    }

    @Test
    fun partialNativeStepsAndFailedImportsShouldRollbackConstraintState(): Unit = runBlocking {
        var failExisting = false
        val source = boundaryProblem(violation = { solution ->
            if (failExisting && solution[0] == Flt64.zero) searchInvalid("deliberate violation failure")
            else Ok(Flt64(abs(solution[0].toDouble())))
        })
        val problem = boundaryValue(source.withConstraints(ConstraintHandlingConfig(mode = ConstraintMode.Epsilon), SearchRandom(1)))
        val session = ParticleSwarmRunner<Flt64, Flt64>(
            size = 4, entropy = SearchRandom(2), archive = BestArchive(20), stopping = NeverStop, converter = IntoValue.Identity
        ).openSession(
            problem = problem,
            codec = RunnerStateCodecs.particles(problem),
            importer = { state, incoming -> ContextStateSynchronizer(problem).particles(state, incoming) }
        )
        boundaryValue(session.step(SearchSlice(SearchBudget(10), 4)))
        val before = boundaryValue(session.snapshot())
        val partial = boundaryValue(session.step(SearchSlice(SearchBudget(1), 1)))
        assertEquals(0L, partial.steps)
        assertEquals(before.state, boundaryValue(session.snapshot()).state)
        assertEquals(before.constraints, boundaryValue(session.snapshot()).constraints)
        failExisting = true
        assertTrue(session.importSolutions(listOf(listOf(Flt64(2.0))), SearchSlice(SearchBudget(10), 10)) is Failed)
        assertEquals(before.state, boundaryValue(session.snapshot()).state)
        assertEquals(before.constraints, boundaryValue(session.snapshot()).constraints)
    }

    @Test
    fun invalidViolationCallbacksMustFailWithoutCommittingInitialization(): Unit = runBlocking {
        for (callback in listOf<(Solution<Flt64>) -> Ret<Flt64>>(
            { Ok(Flt64(-1.0)) }, { Ok(Flt64(Double.NaN)) }, { searchInvalid("failure") }
        )) {
            val problem = boundaryValue(boundaryProblem(callback).withConstraints(ConstraintHandlingConfig(), SearchRandom(1)))
            val session = ParticleSwarmRunner<Flt64, Flt64>(
                size = 4, entropy = SearchRandom(2), archive = BestArchive(20), stopping = NeverStop, converter = IntoValue.Identity
            ).openSession(problem = problem, codec = RunnerStateCodecs.particles(problem))
            val before = boundaryValue(session.snapshot())
            assertEquals(SearchTermination.Failed, boundaryValue(session.step(SearchSlice(SearchBudget(10), 10))).termination)
            val after = boundaryValue(session.snapshot())
            assertFalse(after.initialized)
            assertEquals(before.constraints, after.constraints)
        }
        assertTrue(boundaryProblem(violation = null).withConstraints(ConstraintHandlingConfig(), SearchRandom(1)) is Failed)
    }

    @Test
    fun constrainedFamiliesShouldRecoverAndKeepStrictGlobalArchives(): Unit = runBlocking {
        for (type in listOf("genetic", "hill-climbing", "pso", "clpso", "shade", "l-shade", "cma-es")) {
            val registry = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
            boundaryValue(registry.registerProblem(id = "boundary", statelessCallbacks = true) { _, _ -> Ok(boundaryProblem()) })
            val config = HyperHeuristicConfig(
                problem = ComponentSpec("boundary"),
                algorithms = listOf(AlgorithmConfig(
                    id = "worker", algorithm = ComponentSpec(type), size = 6,
                    constraints = ConstraintHandlingConfig(mode = ConstraintMode.Epsilon, horizon = 6)
                )),
                budget = BudgetConfig(evaluations = 200, decisions = 6, sliceEvaluations = 32),
                memory = MemoryConfig(acceptance = ComponentSpec("auto")),
                persistence = PersistenceConfig(enabled = true)
            )
            val solver = HyperHeuristicSolver(config, registry)
            val original = boundaryValue(solver.open())
            boundaryValue(original.advance())
            boundaryValue(original.advance())
            val checkpoint = boundaryValue(original.checkpoint())
            val resumed = boundaryValue(solver.open(checkpoint = checkpoint))
            val expected = boundaryValue(original.run())
            val actual = boundaryValue(resumed.run())
            assertEquals(expected.archive, actual.archive, type)
            assertTrue(actual.archive.all { it.solution[0] == Flt64.zero && it.fitness == Flt64.zero }, type)
            val expectedState = boundaryValue(original.checkpoint()).workers.getValue("worker").session
            val actualState = boundaryValue(resumed.checkpoint()).workers.getValue("worker").session
            assertEquals(noTiming(expectedState.state), noTiming(actualState.state), type)
            assertEquals(expectedState.constraints, actualState.constraints, type)
            assertEquals(boundaryValue(original.exportPolicy()), boundaryValue(resumed.exportPolicy()), type)
            boundaryValue(original.close())
            boundaryValue(resumed.close())
        }
    }

    @Test
    fun configurationFormatsShouldPreserveNewBoundariesAndRejectInvalidModes() {
        val config = HyperHeuristicConfig(
            problem = ComponentSpec("boundary"),
            algorithms = listOf(AlgorithmConfig(
                id = "worker", algorithm = ComponentSpec("pso"), constraints = ConstraintHandlingConfig(mode = ConstraintMode.StochasticRanking)
            )),
            budget = BudgetConfig(evaluations = 100),
            memory = MemoryConfig(acceptance = ComponentSpec("auto")),
            learning = LearningConfig(reward = ComponentSpec("hypervolume-delta", buildJsonObject {
                put("mode", "sampled")
                put("reference", JsonArray(List(4) { JsonPrimitive(5.0) }))
            }))
        )
        for (format in ConfigurationFormat.entries) {
            assertEquals(config, boundaryValue(ConfigurationFormats.decode(boundaryValue(ConfigurationFormats.encode(config, format)), format)))
        }
        val registry = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
        assertTrue(registry.reward(ComponentSpec("hypervolume-delta", buildJsonObject { put("mode", "magic") })) is Failed)
        assertTrue(registry.reward(ComponentSpec("hypervolume-delta", buildJsonObject { put("samples", 1.5) })) is Failed)
    }

    @Test
    fun tomlShouldRoundtripNestedStagesAndRejectInvalidDocuments() {
        val stages = buildJsonObject {
            put("stages", JsonArray(listOf(buildJsonObject {
                put("type", "vnd")
                put("parameters", buildJsonObject {
                    put("stages", JsonArray(listOf(buildJsonObject { put("type", "pattern-search") })))
                })
            })))
        }
        val config = HyperHeuristicConfig(
            problem = ComponentSpec("boundary", buildJsonObject { put("signedZero", -0.0); put("float", 5.0) }),
            algorithms = listOf(AlgorithmConfig(
                id = "local",
                algorithm = ComponentSpec("genetic"),
                updates = listOf(ComponentSpec("pipeline", stages)),
                constraints = ConstraintHandlingConfig(mode = ConstraintMode.Epsilon, exponent = 1.5)
            )),
            budget = BudgetConfig(evaluations = 100)
        )
        val text = boundaryValue(ConfigurationFormats.encode(config, ConfigurationFormat.Toml))
        assertEquals(config, boundaryValue(ConfigurationFormats.decode(text, ConfigurationFormat.Toml)))
        val dated = text.replace("signedZero = -0.0", "signedZero = -0.0\nproblem.parameters.date = 2026-10-22T10:00:00Z")
        assertEquals(JsonPrimitive("2026-10-22T10:00:00Z"), boundaryValue(ConfigurationFormats.decode(dated, ConfigurationFormat.Toml)).problem.parameters["date"])
        for (invalid in listOf(
            "version = 1\nversion = 1\n$text",
            "unknown = 1\n$text",
            text.replace("version = 1", "version = 2"),
            "value = nan\n$text",
            "value = +inf\n$text",
            " ".repeat(4 * 1024 * 1024 + 1)
        )) {
            assertTrue(ConfigurationFormats.decode(invalid, ConfigurationFormat.Toml) is Failed)
        }
        val nullable = config.copy(problem = ComponentSpec("boundary", buildJsonObject { put("businessNull", JsonNull) }))
        assertTrue(ConfigurationFormats.encode(nullable, ConfigurationFormat.Toml) is Failed)
        assertEquals(nullable, boundaryValue(ConfigurationFormats.decode(
            boundaryValue(ConfigurationFormats.encode(nullable, ConfigurationFormat.Json)), ConfigurationFormat.Json
        )))
    }

    @Test
    fun nsgaAndScalarizedNativeSessionsShouldRetainConstraintAndObjectiveSemantics(): Unit = runBlocking {
        for ((type, mode) in listOf("nsga-ii", "nsga-iii", "pso").flatMap { type -> ConstraintMode.entries.map { type to it } }) {
            val registry = HyperHeuristics.multiObjectiveRegistry(
                converter = IntoValue.Identity,
                scalarizations = mapOf("first" to { values: List<Flt64> -> values[0] })
            )
            boundaryValue(registry.registerProblem(id = "boundary-mo", statelessCallbacks = true) { _, _ ->
                val source = boundaryProblem()
                val multi = multiObjectiveProblem(
                    id = "boundary-mo",
                    fingerprint = "boundary-mo:1",
                    model = source.model,
                    objectivesOf = { solution -> listOf(-solution[0], solution[0], solution[0].pow(2)) },
                    directions = listOf(true, false, true),
                    valueCodec = SearchCodecs.flt64,
                    constraintViolation = source.constraintViolation
                )
                Ok(SearchProblem(
                    id = multi.id, fingerprint = multi.fingerprint, model = multi.model,
                    objectiveCodec = multi.objectiveCodec, valueCodec = multi.valueCodec,
                    dominates = multi.dominates, multiObjective = true, valid = multi.valid,
                    feasible = source.feasible, objectiveCoordinates = multi.objectiveCoordinates,
                    constraintViolation = multi.constraintViolation
                ))
            })
            val config = HyperHeuristicConfig(
                problem = ComponentSpec("boundary-mo"),
                algorithms = listOf(AlgorithmConfig(
                    id = "worker", algorithm = ComponentSpec(type), size = 6,
                    scalarization = if (type == "pso") "first" else null,
                    constraints = ConstraintHandlingConfig(mode = mode, horizon = 4)
                )),
                budget = BudgetConfig(evaluations = 100, decisions = 4, sliceEvaluations = 32),
                memory = MemoryConfig(acceptance = ComponentSpec("auto")),
                learning = LearningConfig(reward = ComponentSpec("hypervolume-delta", buildJsonObject {
                    put("reference", JsonArray(List(3) { JsonPrimitive(10.0) }))
                })),
                persistence = PersistenceConfig(enabled = true)
            )
            val solver = HyperHeuristicSolver(config, registry)
            val original = boundaryValue(solver.open())
            boundaryValue(original.advance())
            val resumed = boundaryValue(solver.open(checkpoint = boundaryValue(original.checkpoint())))
            val expected = boundaryValue(original.run())
            val actual = boundaryValue(resumed.run())
            assertTrue(actual.multiObjective)
            assertNull(actual.best)
            assertTrue(actual.archive.isNotEmpty(), type)
            assertTrue(actual.archive.all {
                val x = it.solution[0]
                abs(x.toDouble()) < 1e-12 && it.fitness == listOf(-x, x, x.pow(2))
            }, "$type: ${actual.archive}")
            assertEquals(expected.archive, actual.archive, type)
            val expectedSnapshot = boundaryValue(original.checkpoint()).workers.getValue("worker").session
            val actualSnapshot = boundaryValue(resumed.checkpoint()).workers.getValue("worker").session
            assertEquals(
                noTiming(Json.encodeToJsonElement(SearchSessionSnapshot.serializer(), expectedSnapshot)),
                noTiming(Json.encodeToJsonElement(SearchSessionSnapshot.serializer(), actualSnapshot)),
                type
            )
            boundaryValue(original.close())
            boundaryValue(resumed.close())
        }
    }

    @Test
    fun unsupportedConstraintComponentsShouldFailBeforeSearch(): Unit = runBlocking {
        val registry = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
        boundaryValue(registry.registerProblem(id = "boundary", statelessCallbacks = true) { _, _ -> Ok(boundaryProblem()) })
        var invoked = false
        boundaryValue(registry.registerAlgorithm("unsupported") { _ ->
            invoked = true
            searchInvalid("must not construct unsupported session")
        })
        val config = HyperHeuristicConfig(
            problem = ComponentSpec("boundary"),
            algorithms = listOf(AlgorithmConfig(
                id = "worker", algorithm = ComponentSpec("unsupported"), constraints = ConstraintHandlingConfig()
            )),
            budget = BudgetConfig(evaluations = 10)
        )
        assertTrue(HyperHeuristicSolver(config, registry).open() is Failed)
        assertFalse(invoked)
        assertTrue(ConstraintHandlingConfig(initialEpsilon = -1.0).validate() is Failed)
        assertTrue(ConstraintHandlingConfig(objectiveProbability = 2.0).validate() is Failed)
    }
}
