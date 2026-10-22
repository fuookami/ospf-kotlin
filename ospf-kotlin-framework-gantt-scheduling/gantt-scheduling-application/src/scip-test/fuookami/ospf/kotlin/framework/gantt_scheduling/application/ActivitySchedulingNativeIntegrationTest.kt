@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.application

import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.scip.ScipColumnGenerationSolver
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.service.*

/** 显式原生验收，运行时缺失直接失败而不伪装通过。 / Opt-in native verification; missing runtime fails rather than silently passing. */
class ActivitySchedulingNativeIntegrationTest {
    private val origin = Instant.parse("2026-10-22T00:00:00Z")
    private data class Task(val value: String) : TaskId
    private data class Resource(val value: String) : ExecutorId
    private val a = Task("a")
    private val b = Task("b")
    private val machine = Resource("machine")
    private val crew = Resource("crew")

    @Test
    fun actualMilpChoosesFlexibleMachineAndPrecedenceFeasibleOrder() = runBlocking {
        val alternate = Resource("alternate")
        val result = service().solve(problem(
            activities = listOf(
                Activity(a, listOf(
                    ActivityMode("fast", 2.hours, listOf(at(0), at(2)), listOf(ActivityResourceUse(machine, q(1.0))), q(3.0)),
                    ActivityMode("slow", 3.hours, listOf(at(0), at(2)), listOf(ActivityResourceUse(alternate, q(1.0))), q(1.0))
                )),
                Activity(b, listOf(ActivityMode("next", 2.hours, listOf(at(0), at(2)), listOf(ActivityResourceUse(machine, q(1.0))), q(0.0))))
            ),
            resources = listOf(ActivityResource(machine, q(1.0)), ActivityResource(alternate, q(1.0))),
            dependencies = listOf(ActivityDependency(a, b))
        ))
        val outcome = unwrap(result)
        val schedule = assertNotNull(outcome.schedule)
        assertEquals(ProblemStatus.Feasible, outcome.report.problemStatus)
        assertEquals(SolutionPresence.Optimal, outcome.report.solutionPresence)
        assertTrue(schedule.cost.value eq Flt64(3.0))
        assertEquals("fast", schedule.placements.single { it.key.task == a }.key.mode)
        assertEquals(at(2), schedule.placements.single { it.key.task == b }.time.start)
    }

    @Test
    fun actualMilpPreservesPhasedJointResourcesAndCalendar() = runBlocking {
        val second = Resource("second")
        fun activity(task: Task, room: Resource, starts: List<Instant>) = Activity(task, listOf(ActivityMode(
            id = "joint",
            duration = 2.hours,
            starts = starts,
            resources = listOf(ActivityResourceUse(room, q(1.0)), ActivityResourceUse(crew, q(1.0), until = 1.hours)),
            cost = q(1.0)
        )))
        val outcome = unwrap(service().solve(problem(
            activities = listOf(activity(a, machine, listOf(at(0))), activity(b, second, listOf(at(0), at(1), at(2)))),
            resources = listOf(
                ActivityResource(machine, q(1.0)),
                ActivityResource(second, q(1.0), unavailableTimes = listOf(TimeRange(at(3), at(4)))),
                ActivityResource(crew, q(1.0))
            )
        )))
        val schedule = assertNotNull(outcome.schedule)
        assertEquals(at(1), schedule.placements.single { it.key.task == b }.time.start)
        assertTrue(schedule.cost.value eq Flt64(2.0))
        assertEquals(2, schedule.placements.single { it.key.task == b }.reservations.size)
    }

    @Test
    fun actualMilpPreservesRunningFactsAndChargesBaselineChanges() = runBlocking {
        val fact = ActivityExecution(
            placement = ActivityPlacement(
                key = ActivityChoiceKey(b, "running", at(-1)),
                time = TimeRange(at(-1), at(2)),
                reservations = listOf(ActivityReservation(machine, TimeRange(at(0), at(2)), q(1.0)))
            ),
            status = ActivityExecutionStatus.Running
        )
        val outcome = unwrap(service().solve(problem(
            activities = listOf(Activity(a, listOf(ActivityMode("work", 1.hours, listOf(at(0), at(2)), listOf(ActivityResourceUse(machine, q(1.0))), q(1.0))))),
            resources = listOf(ActivityResource(machine, q(1.0))),
            dependencies = listOf(ActivityDependency(b, a)),
            baselines = listOf(ActivityBaseline(ActivityChoiceKey(a, "work", at(0)), false, q(2.0), q(5.0))),
            executions = listOf(fact)
        )))
        val schedule = assertNotNull(outcome.schedule)
        assertEquals(listOf(fact), schedule.executions)
        assertEquals(at(2), schedule.placements.single().time.start)
        assertTrue(schedule.cost.value eq Flt64(3.0))
    }

    @Test
    fun actualMilpReportsImpossibleFrozenCalendarAsInfeasible() = runBlocking {
        val outcome = unwrap(service().solve(problem(
            activities = listOf(Activity(a, listOf(ActivityMode("work", 1.hours, listOf(at(0), at(2)), listOf(ActivityResourceUse(machine, q(1.0))), q(1.0))))),
            resources = listOf(ActivityResource(machine, q(1.0), unavailableTimes = listOf(TimeRange(at(0), at(1))))),
            baselines = listOf(ActivityBaseline(ActivityChoiceKey(a, "work", at(0)), true, q(0.0), q(0.0)))
        )))
        assertEquals(ProblemStatus.Infeasible, outcome.report.problemStatus)
        assertNull(outcome.schedule)
    }

    @Test
    fun actualMilpWaitsForMaterialAndRetainsInventoryTrace() = runBlocking {
        val outcome = unwrap(service().solve(problem(
            activities = listOf(Activity(a, listOf(ActivityMode(
                id = "consume",
                duration = 1.hours,
                starts = listOf(at(0), at(2)),
                resources = listOf(ActivityResourceUse(machine, q(1.0))),
                cost = q(1.0),
                materialFlows = listOf(ActivityMaterialFlow("raw", q(-2.0)))
            )))),
            resources = listOf(ActivityResource(machine, q(1.0))),
            materials = listOf(ActivityMaterial("raw", q(0.0))),
            materialEvents = listOf(ActivityMaterialEvent("raw", at(2), q(2.0)))
        )))
        val schedule = assertNotNull(outcome.schedule)
        assertEquals(SolutionPresence.Optimal, outcome.report.solutionPresence)
        assertEquals(at(2), schedule.placements.single().time.start)
        assertTrue(schedule.materialInventory.single().levels.last().quantity.value eq Flt64.zero)
    }

    @Test
    fun actualMilpUsesOnlyExplicitCostedOrderCancellationForMaterialShortage() = runBlocking {
        val outcome = unwrap(service().solve(problem(
            activities = listOf(Activity(
                id = a,
                modes = listOf(ActivityMode(
                    id = "fulfill",
                    duration = 1.hours,
                    starts = listOf(at(0)),
                    resources = emptyList(),
                    cost = q(1.0),
                    materialFlows = listOf(ActivityMaterialFlow("raw", q(-2.0)))
                )),
                cancellationCost = q(7.0)
            )),
            resources = emptyList(),
            materials = listOf(ActivityMaterial("raw", q(1.0)))
        )))
        val schedule = assertNotNull(outcome.schedule)
        assertEquals(setOf(a), schedule.cancelled)
        assertTrue(schedule.cost.value eq Flt64(7.0))
        assertTrue(schedule.materialInventory.single().levels.last().quantity.value eq Flt64.one)
    }

    private fun service(): ActivitySchedulingApplicationService<Flt64> {
        assertFalse(System.getenv("JSCIP_HOME").isNullOrBlank(), "JSCIP_HOME required; native verification cannot be skipped")
        assertFalse(System.getenv("SCIP_HOME").isNullOrBlank(), "SCIP_HOME required; native verification cannot be skipped")
        val runtime = runCatching { Class.forName("jscip.Scip"); System.loadLibrary("jscip") }
        assertTrue(runtime.isSuccess, "SCIP native load failed: ${runtime.exceptionOrNull()}")
        return ActivitySchedulingApplicationService(
            solver = ScipColumnGenerationSolver(config = SolverConfig(threadNum = UInt64.one)),
            adapter = schedulingSolverValueAdapter
        )
    }

    private fun problem(
        activities: List<Activity<Flt64>>,
        resources: List<ActivityResource<Flt64>>,
        dependencies: List<ActivityDependency> = emptyList(),
        baselines: List<ActivityBaseline<Flt64>> = emptyList(),
        executions: List<ActivityExecution<Flt64>> = emptyList(),
        materials: List<ActivityMaterial<Flt64>> = emptyList(),
        materialEvents: List<ActivityMaterialEvent<Flt64>> = emptyList()
    ) = ActivitySchedulingProblem(
        horizon = TimeRange(at(0), at(6)),
        activities = activities,
        resources = resources,
        dependencies = dependencies,
        baselines = baselines,
        executions = executions,
        costUnit = NoneUnit,
        materials = materials,
        materialEvents = materialEvents
    )

    private fun at(hour: Int) = origin + hour.hours
    private fun q(value: Double) = Quantity(Flt64(value), NoneUnit)

    private fun <T> unwrap(result: Ret<T>): T = when (result) {
        is Ok -> result.value
        is Failed -> fail("${result.error.code}: ${result.error.message}")
        is Fatal -> fail(result.errors.joinToString { "${it.code}: ${it.message}" })
    }
}
