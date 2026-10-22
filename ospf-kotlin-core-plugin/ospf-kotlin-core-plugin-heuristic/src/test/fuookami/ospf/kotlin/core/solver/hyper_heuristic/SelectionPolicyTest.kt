package fuookami.ospf.kotlin.core.solver.hyper_heuristic

import java.nio.file.Files
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.callback.CallBackModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.*

private fun <T> value(result: Ret<T>): T = when (result) {
    is Ok -> result.value
    is Failed -> fail(result.message ?: "failed")
    is Fatal -> fail(result.errors.toString())
}

private fun transition(action: String, reward: Double, terminal: Boolean = false, decision: Long = 0): SelectionTransition {
    return SelectionTransition(
        episode = "test",
        decision = decision,
        action = action,
        before = SelectionContext(state = 0),
        after = SelectionContext(state = 1),
        available = listOf("a", "b"),
        nextAvailable = listOf("b"),
        reward = Flt64(reward),
        evaluations = 1,
        terminal = terminal,
        policyVersion = 0
    )
}

class SelectionPolicyTest {
    @Test
    fun qLearningShouldCreditTheExecutedActionIncludingTheTerminalTransition() {
        val policy = TabularSelectionPolicy("q-learning", listOf("a", "b"), epsilon = 0.0, alpha = 0.5, gamma = 0.9)
        value(policy.observe(transition("a", 2.0)))
        assertEquals(1.0, policy.snapshot().table[0][0])
        value(policy.observe(transition("b", 4.0, terminal = true, decision = 1)))
        assertEquals(2.0, policy.snapshot().table[0][1])
        assertEquals(2L, policy.revision)
        assertEquals("b", value(policy.select(SelectionContext(), listOf("b"), SearchRandom(1))))
        assertTrue(policy.select(SelectionContext(), listOf("unknown"), SearchRandom(1)) is Failed)
    }

    @Test
    fun qLearningShouldBootstrapOnlyAvailableSuccessorActions() {
        val policy = TabularSelectionPolicy("q-learning", listOf("a", "b"), epsilon = 0.0, alpha = 1.0, gamma = 0.5)
        val initial = policy.snapshot()
        val table = initial.table.map { it.toMutableList() }.toMutableList()
        table[1][0] = 100.0
        table[1][1] = 4.0
        value(policy.restore(initial.copy(table = table)))
        value(policy.observe(transition("a", 1.0)))
        assertEquals(3.0, policy.snapshot().table[0][0])
    }

    @Test
    fun policyRestoreShouldAlignByActionIdsAndFrozenInferenceShouldNotLearn() {
        val source = TabularSelectionPolicy("q-learning", listOf("a", "b"), alpha = 1.0)
        value(source.observe(transition("a", 3.0, terminal = true)))
        val target = TabularSelectionPolicy("q-learning", listOf("b", "a"), learning = false)
        value(target.restore(source.snapshot()))
        val before = target.snapshot()
        assertEquals("a", value(target.select(SelectionContext(), listOf("a", "b"), SearchRandom(1))))
        value(target.observe(transition("b", 1000.0, terminal = true)))
        assertEquals(before, target.snapshot())
    }

    @Test
    fun ucbShouldExploreUnobservedActionsAndUseOnlyObservedRewards() {
        val policy = TabularSelectionPolicy("ucb1", listOf("a", "b"))
        assertEquals("a", value(policy.select(SelectionContext(), listOf("a", "b"), SearchRandom(1))))
        value(policy.observe(transition("a", 1.0)))
        assertEquals("b", value(policy.select(SelectionContext(), listOf("a", "b"), SearchRandom(1))))
        assertEquals(listOf(1L, 0L), policy.snapshot().counts)
    }

    @Test
    fun trajectoryReplayShouldMatchOnlineUpdatesAndPreserveCompleteRecords() {
        val history = listOf(transition("a", 2.0), transition("b", -1.0, true, 1))
        val online = TabularSelectionPolicy("q-learning", listOf("a", "b"))
        val offline = TabularSelectionPolicy("q-learning", listOf("a", "b"))
        history.forEach { value(online.observe(it)) }
        val trainer = PolicyTrainer(HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity))
        value(trainer.replay(offline, history, "progress:1"))
        assertEquals(online.snapshot(), offline.snapshot())
        assertTrue(trainer.replay(offline, history + history, "progress:1") is Failed)
        val path = Files.createTempDirectory("ospf-hyper-replay-").resolve("history.jsonl")
        value(TrajectoryFiles.write(path, history))
        assertEquals(history, value(TrajectoryFiles.read(path)))
    }

    @Test
    fun paretoAndExplicitScalarizedAlgorithmsShouldShareTheCanonicalArchive() = runBlocking {
        val registry = HyperHeuristics.multiObjectiveRegistry(
            converter = IntoValue.Identity,
            scalarizations = mapOf("sum" to { objectives: List<Flt64> -> objectives[0] + objectives[1] })
        )
        value(registry.registerProblem(id = "tradeoff", statelessCallbacks = true) { _, _ ->
            val model = CallBackModel(
                objectCategory = ObjectCategory.Minimum,
                initialSolutionGenerator = { (index, _) -> Flt64(index.toInt() % 5 * 0.5) },
                converter = IntoValue.Identity
            )
            model.add(RealVar("x"))
            Ok(multiObjectiveProblem(
                id = "tradeoff",
                fingerprint = "tradeoff:v1",
                model = model,
                objectivesOf = { solution -> listOf(solution[0].pow(2), (solution[0] - Flt64(2.0)).pow(2)) },
                directions = listOf(true, true),
                valueCodec = SearchCodecs.flt64
            ))
        })
        val config = HyperHeuristicConfig(
            problem = ComponentSpec("tradeoff"),
            algorithms = listOf(
                AlgorithmConfig("nsga2", ComponentSpec("nsga-ii"), size = 6),
                AlgorithmConfig("nsga3", ComponentSpec("nsga-iii"), size = 6),
                AlgorithmConfig("pso", ComponentSpec("pso"), size = 6, scalarization = "sum")
            ),
            budget = BudgetConfig(evaluations = 120, sliceEvaluations = 30),
            execution = ExecutionConfig(parallelism = 3),
            persistence = PersistenceConfig(enabled = true)
        )
        val result = value(HyperHeuristicSolver(config, registry)())
        assertTrue(result.multiObjective)
        assertNull(result.best)
        assertTrue(result.archive.size > 1)
        assertTrue(result.archive.all { it.fitness.size == 2 && it.fitness.all { v -> v.toDouble().isFinite() } })
        val invalid = config.copy(algorithms = listOf(AlgorithmConfig("pso", ComponentSpec("pso"), size = 6)))
        assertTrue(HyperHeuristicSolver(invalid, registry).open() is Failed)
    }
}
