package fuookami.ospf.kotlin.core.solver.hyper_heuristic

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
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.RecoverableComponent
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.persistence.RunCheckpoint

private fun <T> protocolChecked(result: Ret<T>): T = when (result) {
    is Ok -> result.value
    is Failed -> fail(result.message)
    is Fatal -> fail(result.errors.toString())
}

private class ProtocolCountReward : RewardPolicy, RecoverableComponent {
    private var count = 0L

    override fun reward(context: RewardContext): Flt64 {
        ++count
        return Flt64(count.toDouble())
    }

    override fun save(): Ret<JsonElement> = Ok(JsonPrimitive(count))

    override fun load(state: JsonElement): Try {
        val value = (state as? JsonPrimitive)?.longOrNull
        if (value == null || value < 0) return searchInvalid("无效奖励计数 / Invalid reward count")
        count = value
        return ok
    }
}

class RewardStateProtocolTest {
    private fun solver(rewardFactory: () -> RewardPolicy = { ProtocolCountReward() }): HyperHeuristicSolver<Flt64, Flt64> {
        val registry = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
        protocolChecked(registry.registerReward("protocol-count", factory = rewardFactory))
        protocolChecked(registry.registerProblem(id = "protocol", statelessCallbacks = true) { _, _ ->
            val model = CallBackModel(
                objectCategory = ObjectCategory.Minimum,
                initialSolutionGenerator = { (solution, _) -> Flt64(solution.toInt().toDouble() + 2.0) },
                converter = IntoValue.Identity
            )
            model.add(RealVar("x").apply { range.geq(Flt64(-10.0)); range.leq(Flt64(10.0)) })
            model.addObject(ObjectCategory.Minimum, { values: List<Flt64> -> values[0] * values[0] })
            Ok(singleObjectiveProblem(
                id = "protocol",
                fingerprint = "protocol:1",
                model = model,
                objectiveCodec = SearchCodecs.flt64,
                valueCodec = SearchCodecs.flt64
            ))
        })
        return HyperHeuristicSolver(HyperHeuristicConfig(
            problem = ComponentSpec("protocol"),
            algorithms = (0..1).map { index -> AlgorithmConfig(
                id = "worker-$index",
                algorithm = ComponentSpec("pso"),
                size = 4,
                parameters = linkedMapOf(
                    "personalWeight" to listOf(ComponentSpec("constant")),
                    "inertia" to listOf(ComponentSpec("constant"))
                )
            ) },
            budget = BudgetConfig(evaluations = 80, sliceEvaluations = 4, sliceSteps = 1),
            execution = ExecutionConfig(mode = ExecutionMode.Serial),
            memory = MemoryConfig(exchange = ComponentSpec("none")),
            learning = LearningConfig(reward = ComponentSpec("protocol-count")),
            persistence = PersistenceConfig(enabled = true)
        ), registry)
    }

    private fun rewrite(saved: RunCheckpoint, transform: (MutableMap<String, JsonElement>) -> Unit): RunCheckpoint {
        return saved.copy(workers = saved.workers.mapValues { (_, worker) ->
            val state = worker.session.state.jsonObject.toMutableMap()
            transform(state)
            worker.copy(session = worker.session.copy(state = JsonObject(state)))
        })
    }

    @Test
    fun statefulRewardSingletonsShouldFailBeforeSearch(): Unit = runBlocking {
        val shared = ProtocolCountReward()
        assertTrue(solver { shared }.open() is Failed)
    }

    @Test
    fun sharedAndLegacyStatesShouldResumeIndependentRewards(): Unit = runBlocking {
        val solver = solver()
        val run = protocolChecked(solver.open())
        repeat(2) { protocolChecked(run.advance()) }
        val initialized = protocolChecked(run.checkpoint())
        for (worker in initialized.workers.values) {
            assertEquals(JsonPrimitive(0), worker.session.state.jsonObject["rewardState"])
            assertEquals(JsonPrimitive(1), worker.session.state.jsonObject["rewardStateVersion"])
            assertNull(worker.session.state.jsonObject["reward"])
        }
        repeat(2) { protocolChecked(run.advance()) }
        val saved = protocolChecked(run.checkpoint())
        assertEquals(JsonPrimitive(4), saved.components["reward"])
        for (worker in saved.workers.values) assertEquals(JsonPrimitive(2), worker.session.state.jsonObject["rewardState"])
        val legacyKotlin = rewrite(saved) { it.remove("rewardStateVersion") }
        val legacyRust = rewrite(saved) {
            it["reward"] = it.remove("rewardState")!!
            it.remove("rewardStateVersion")
        }
        protocolChecked(run.run())
        val expected = protocolChecked(run.checkpoint())
        for (checkpoint in listOf(saved, legacyKotlin, legacyRust)) {
            val restored = protocolChecked(solver.open(checkpoint = checkpoint))
            protocolChecked(restored.run())
            val actual = protocolChecked(restored.checkpoint())
            assertEquals(expected.components, actual.components)
            assertEquals(expected.policy, actual.policy)
            assertEquals(expected.archive, actual.archive)
            assertEquals(expected.transitions.map { Triple(it.scope, it.action, it.reward) }, actual.transitions.map { Triple(it.scope, it.action, it.reward) })
            for ((id, worker) in expected.workers) {
                fun withoutTiming(state: JsonElement): JsonElement = when (state) {
                    is JsonObject -> JsonObject(state.filterKeys { it != "activeNanos" }.mapValues { withoutTiming(it.value) })
                    is JsonArray -> JsonArray(state.map { withoutTiming(it) })
                    else -> state
                }
                assertEquals(withoutTiming(worker.session.state), withoutTiming(actual.workers.getValue(id).session.state))
            }
            protocolChecked(restored.close())
        }
        for (invalid in listOf(
            rewrite(saved) { it["rewardStateVersion"] = JsonPrimitive(99) },
            rewrite(saved) { it.remove("rewardState") },
            rewrite(saved) { it["reward"] = JsonPrimitive(2) }
        )) assertTrue(solver.open(checkpoint = invalid) is Failed)
        protocolChecked(run.close())
    }
}
