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
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.persistence.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.scheduling.*

private fun <T> checkedSchedule(result: Ret<T>): T = when (result) {
    is Ok -> result.value
    is Failed -> fail(result.message ?: "operation failed")
    is Fatal -> fail(result.errors.toString())
}

private fun withoutScheduleTiming(value: JsonElement): JsonElement = when (value) {
    is JsonObject -> JsonObject(value.filterKeys { it != "activeNanos" && it != "elapsedNanos" }.mapValues { withoutScheduleTiming(it.value) })
    is JsonArray -> JsonArray(value.map { withoutScheduleTiming(it) })
    else -> value
}

private fun scheduleSolver(
    reverse: Boolean = false,
    failWorker: Boolean = false,
    mode: ExecutionMode = ExecutionMode.Asynchronous,
    timeMillis: Long? = null,
    onSlow: (() -> Unit)? = null
): HyperHeuristicSolver<Flt64, Flt64> {
    val factories = AtomicInteger()
    val registry = HyperHeuristics.registry<Flt64, Flt64>(IntoValue.Identity)
    checkedSchedule(registry.registerProblem("scheduled", statelessCallbacks = true) { _, _ ->
        val role = factories.getAndIncrement() % 3
        val slow = role == if (reverse) 2 else 1
        val model = CallBackModel(
            objectiveComparator = PartialComparator<Flt64> { lhs, rhs -> lhs.toDouble() < rhs.toDouble() },
            initialSolutionGenerator = { (solution, _) -> Flt64(solution.toInt().toDouble() + 2.0) },
            converter = IntoValue.Identity
        )
        model.add(RealVar("x").apply { range.geq(Flt64(-10.0)); range.leq(Flt64(10.0)) })
        model.addObject(ObjectCategory.Minimum, { solution: List<Flt64> ->
            if (slow) onSlow?.invoke()
            if (slow) Thread.sleep(3)
            if (failWorker && role == 1) error("scheduled worker failure")
            Flt64(solution[0].toDouble() * solution[0].toDouble())
        })
        Ok(singleObjectiveProblem("scheduled", "scheduled:1", model, SearchCodecs.flt64, SearchCodecs.flt64))
    })
    return HyperHeuristicSolver(HyperHeuristicConfig(
        problem = ComponentSpec("scheduled"),
        algorithms = listOf(AlgorithmConfig("algorithm-0", ComponentSpec("pso"), size = 4), AlgorithmConfig("algorithm-1", ComponentSpec("genetic"), size = 4)),
        budget = BudgetConfig(evaluations = 80, sliceEvaluations = 4, timeMillis = timeMillis),
        execution = ExecutionConfig(mode = mode, parallelism = 2, seed = 123),
        persistence = PersistenceConfig(enabled = true)
    ), registry)
}

class ScheduleReplayTest {
    @Test
    fun oppositeCompletionOrderAndQuiescentPrefixesShouldReplayExactly() = runBlocking {
        val solver = scheduleSolver()
        val recorded = checkedSchedule(solver.open())
        checkedSchedule(recorded.recordSchedule())
        checkedSchedule(recorded.advance()); checkedSchedule(recorded.advance())
        val prefix = checkedSchedule(recorded.checkpoint())
        assertEquals(prefix.dispatched, prefix.completed)
        checkedSchedule(recorded.run())
        val expected = checkedSchedule(recorded.checkpoint())
        val log = checkedSchedule(recorded.exportSchedule())
        checkedSchedule(log.validate())
        assertEquals("algorithm-1", log.events.first { it.kind == "complete" }.action)
        val opposite = scheduleSolver(true)
        val replay = checkedSchedule(opposite.open())
        checkedSchedule(replay.replaySchedule(log)); checkedSchedule(replay.run())
        assertEquals(log, checkedSchedule(replay.exportSchedule()))
        assertEquals(withoutScheduleTiming(Json.encodeToJsonElement(expected)), withoutScheduleTiming(Json.encodeToJsonElement(checkedSchedule(replay.checkpoint()))))
        val resumed = checkedSchedule(opposite.open(checkpoint = prefix))
        assertTrue(resumed.advance() is Failed)
        checkedSchedule(resumed.replaySchedule(log)); checkedSchedule(resumed.run())
        assertEquals(withoutScheduleTiming(Json.encodeToJsonElement(expected)), withoutScheduleTiming(Json.encodeToJsonElement(checkedSchedule(resumed.checkpoint()))))
        val folder = Files.createTempDirectory("ospf-schedule-")
        val store = FileSearchStore(folder)
        checkedSchedule(store.saveSchedule("schedule.json", log))
        assertEquals(log, checkedSchedule(store.loadSchedule("schedule.json")))
        val truncated = log.copy(events = log.events.dropLast(1)).let { it.copy(checksum = it.digest()) }
        assertTrue(truncated.validate() is Failed)
        assertTrue(store.saveSchedule("schedule.json", truncated) is Failed)
        assertEquals(log, checkedSchedule(store.loadSchedule("schedule.json")))
        assertTrue(opposite.open(checkpoint = prefix.copy(schedule = prefix.schedule?.copy(cursor = -1))) is Failed)
        val savedSchedule = assertNotNull(prefix.schedule)
        for (identity in listOf(true, false)) {
            val changed = if (identity) savedSchedule.log.copy(problem = "other") else savedSchedule.log.copy(events = savedSchedule.log.events.mapIndexed { index, event ->
                if (index == savedSchedule.log.events.indexOfFirst { it.kind == "complete" }) event.copy(evaluations = event.evaluations - 1) else event
            })
            val signed = changed.copy(checksum = changed.digest())
            assertTrue(opposite.open(checkpoint = prefix.copy(schedule = savedSchedule.copy(log = signed))) is Failed)
        }
        val changed = log.copy(events = log.events.mapIndexed { index, event ->
            if (index == log.events.indexOfFirst { it.kind == "dispatch" }) event.copy(random = event.random xor 1L) else event
        }).let { it.copy(checksum = it.digest()) }
        val diverged = checkedSchedule(opposite.open())
        checkedSchedule(diverged.replaySchedule(changed))
        assertTrue(diverged.advance() is Failed)
        assertTrue(diverged.checkpoint() is Failed)
        checkedSchedule(diverged.close())
        checkedSchedule(recorded.close()); checkedSchedule(replay.close()); checkedSchedule(resumed.close())
        Files.delete(folder.resolve("schedule.json")); Files.delete(folder)
    }

    @Test
    fun stopDrainAndEndedCheckpointShouldRemainStoppedOnReplay() = runBlocking {
        val recorded = checkedSchedule(scheduleSolver().open())
        checkedSchedule(recorded.recordSchedule()); checkedSchedule(recorded.advance())
        recorded.requestStop()
        val stopped = checkedSchedule(recorded.checkpoint())
        val log = checkedSchedule(recorded.exportSchedule())
        val solver = scheduleSolver(true)
        val replay = checkedSchedule(solver.open())
        checkedSchedule(replay.replaySchedule(log)); checkedSchedule(replay.run())
        assertEquals(SearchTermination.Stopped, replay.result().termination)
        assertEquals(withoutScheduleTiming(Json.encodeToJsonElement(stopped)), withoutScheduleTiming(Json.encodeToJsonElement(checkedSchedule(replay.checkpoint()))))
        val restored = checkedSchedule(solver.open(checkpoint = stopped))
        checkedSchedule(restored.replaySchedule(log)); assertFalse(checkedSchedule(restored.advance()))
        checkedSchedule(recorded.close()); checkedSchedule(replay.close()); checkedSchedule(restored.close())
        Unit
    }

    @Test
    fun failureShouldDrainOnceAndReplayWithoutDuplicateCommits() = runBlocking {
        val recorded = checkedSchedule(scheduleSolver(failWorker = true).open())
        checkedSchedule(recorded.recordSchedule())
        assertTrue(recorded.run() is Failed)
        val saved = checkedSchedule(recorded.checkpoint())
        assertEquals(saved.dispatched, saved.completed)
        assertTrue(saved.diagnostics.isNotEmpty())
        val log = checkedSchedule(recorded.exportSchedule())
        assertTrue(log.events.any { it.kind == "complete" && it.failed })
        val replay = checkedSchedule(scheduleSolver(reverse = true, failWorker = true).open())
        checkedSchedule(replay.replaySchedule(log)); assertTrue(replay.run() is Failed)
        assertEquals(log, checkedSchedule(replay.exportSchedule()))
        assertEquals(saved.completed, checkedSchedule(replay.checkpoint()).completed)
        checkedSchedule(recorded.close()); checkedSchedule(replay.close())
        Unit
    }

    @Test
    fun timeBudgetsAndNonAsynchronousModesShouldRejectScheduleControl() = runBlocking {
        for (solver in listOf(scheduleSolver(timeMillis = 1000), scheduleSolver(mode = ExecutionMode.Serial), scheduleSolver(mode = ExecutionMode.Synchronous))) {
            val run = checkedSchedule(solver.open())
            assertTrue(run.recordSchedule() is Failed)
            checkedSchedule(run.close())
        }
    }

    @Test
    fun stopObservedWhileAdvanceWaitsShouldReplayTheSameTerminalFeedback() = runBlocking {
        var request: (() -> Unit)? = null
        val recorded = checkedSchedule(scheduleSolver(onSlow = { request?.invoke() }).open())
        request = recorded::requestStop
        checkedSchedule(recorded.recordSchedule())
        checkedSchedule(recorded.run())
        val expected = checkedSchedule(recorded.checkpoint())
        val log = checkedSchedule(recorded.exportSchedule())
        assertTrue(log.events.any { it.kind == "complete" && it.stopped })
        val replay = checkedSchedule(scheduleSolver(true).open())
        checkedSchedule(replay.replaySchedule(log)); checkedSchedule(replay.run())
        assertEquals(withoutScheduleTiming(Json.encodeToJsonElement(expected)), withoutScheduleTiming(Json.encodeToJsonElement(checkedSchedule(replay.checkpoint()))))
        assertEquals(log, checkedSchedule(replay.exportSchedule()))
        checkedSchedule(recorded.close()); checkedSchedule(replay.close())
        Unit
    }

    @Test
    fun synchronousTerminalFeedbackShouldUseCommittedCostNotRacingCounters() = runBlocking {
        val checkpoints = mutableListOf<JsonElement>()
        for (reverse in listOf(false, true)) {
            val run = checkedSchedule(scheduleSolver(reverse = reverse, mode = ExecutionMode.Synchronous).open())
            checkedSchedule(run.run())
            val events = run.transitions().filter { it.scope == "algorithm" }
            assertTrue(events.dropLast(1).none { it.terminal })
            assertTrue(events.last().terminal)
            checkpoints.add(withoutScheduleTiming(Json.encodeToJsonElement(checkedSchedule(run.checkpoint()))))
            checkedSchedule(run.close())
        }
        assertEquals(checkpoints[0], checkpoints[1])
    }
}
