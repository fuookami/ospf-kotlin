@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity

import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*

/** 以手写业务 oracle 和全部二元组合验证模型，无需原生 solver。 / Verify model with handwritten business oracles and all binary combinations, without a native solver. */
class ActivitySchedulingContextTest {
    private val epoch = Instant.parse("2026-10-22T00:00:00Z")
    private data class Task(val value: String) : TaskId
    private data class Resource(val value: String) : ExecutorId
    private val machine = Resource("machine")
    private val alternate = Resource("alternate")
    private val a = Task("a")
    private val b = Task("b")
    private val c = Task("c")

    @Test
    fun flexibleJobShopChoosesFeasibleMachineAndOrderAgainstManualOracle() {
        val context = context(problem(
            activities = listOf(
                Activity(a, listOf(mode("fast", 2, listOf(0, 2), machine, cost = 3.0), mode("slow", 3, listOf(0, 2), alternate, cost = 1.0))),
                Activity(b, listOf(mode("work", 2, listOf(0, 2), machine)))
            ),
            resources = listOf(resource(machine), resource(alternate)),
            dependencies = listOf(ActivityDependency(a, b)),
            additionalCost = { placement -> ok(q((placement.time.end - epoch).inWholeHours.toDouble())) }
        ))
        // 手工枚举加工模式和开始时刻，非读取生成约束。 / Enumerate modes and starts without reading generated rows.
        val oracle = listOf(Triple("fast", 2, 3.0), Triple("slow", 3, 1.0)).flatMap { (mode, duration, cost) ->
            listOf(0, 2).flatMap { start -> listOf(0, 2).mapNotNull { next ->
                if (next >= start + duration) Triple(cost + start + duration + next + 2, mode, start to next) else null
            } }
        }.minBy { it.first }
        val solution = optimum(context)
        assertEquals(Flt64(oracle.first), solution.cost.value)
        assertEquals(oracle.second, solution.placements.single { it.key.task == a }.key.mode)
        assertEquals(key(a, "fast", 0), solution.placements.single { it.key.task == a }.key)
        assertEquals(key(b, "work", 2), solution.placements.single { it.key.task == b }.key)
    }

    @Test
    fun resourceConstrainedProjectSupportsMilestonesAndCumulativeCrews() {
        val crew = Resource("crew")
        val context = context(problem(
            activities = listOf(
                Activity(a, listOf(mode("build", 2, listOf(0, 1), crew, amount = 2.0))),
                Activity(b, listOf(mode("inspect", 1, listOf(0, 2), crew))),
                Activity(c, listOf(ActivityMode("finish", Duration.ZERO, listOf(at(2), at(3)), emptyList(), q(0.0))))
            ),
            resources = listOf(resource(crew, 2.0)),
            dependencies = listOf(ActivityDependency(a, c), ActivityDependency(b, c)),
            additionalCost = { placement -> ok(q(if (placement.key.task == c) (placement.time.end - epoch).inWholeHours.toDouble() else 0.0)) }
        ))
        val solution = optimum(context)
        assertEquals(Flt64(3.0), solution.cost.value)
        assertEquals(at(3), solution.placements.single { it.key.task == c }.time.end)
        assertFalse(context.validate(listOf(key(a, "build", 0), key(b, "inspect", 0), key(c, "finish", 2))).feasible)
    }

    @Test
    fun phasedJointResourcesUseHalfOpenIntervalsInsteadOfWholeActivityLocks() {
        val technician = Resource("technician")
        fun joint(task: Task, room: Resource, starts: List<Int>) = Activity(task, listOf(ActivityMode(
            id = "joint",
            duration = 2.hours,
            starts = starts.map(::at),
            resources = listOf(
                ActivityResourceUse(room, q(1.0)),
                ActivityResourceUse(technician, q(1.0), until = 1.hours)
            ),
            cost = q(0.0)
        )))
        val context = context(problem(
            activities = listOf(joint(a, machine, listOf(0)), joint(b, alternate, listOf(0, 1))),
            resources = listOf(resource(machine), resource(alternate), resource(technician)),
            additionalCost = { ok(q((it.time.end - epoch).inWholeHours.toDouble())) }
        ))
        val solution = optimum(context)
        assertEquals(Flt64(5.0), solution.cost.value)
        val second = solution.placements.single { it.key.task == b }
        assertEquals(at(1), second.time.start)
        assertEquals(2, second.reservations.size)
        assertEquals(at(2), second.reservations.single { it.resource == technician }.time.end)
    }

    @Test
    fun rollingRecoveryPreservesRunningRemainderCompletedFactsAndFrozenChoices() {
        val running = ActivityExecution(
            placement = ActivityPlacement(
                key = key(c, "running", -1),
                time = TimeRange(at(-1), at(2)),
                reservations = listOf(ActivityReservation(machine, TimeRange(at(0), at(2)), q(1.0)))
            ),
            status = ActivityExecutionStatus.Running
        )
        val done = ActivityExecution(
            placement = ActivityPlacement(key(Task("done"), "done", -3), TimeRange(at(-3), at(-2)), emptyList<ActivityReservation<Flt64>>()),
            status = ActivityExecutionStatus.Completed
        )
        val context = context(problem(
            activities = listOf(
                Activity(a, listOf(mode("original", 1, listOf(0, 2), machine), mode("alternative", 1, listOf(0), alternate, cost = 5.0))),
                Activity(b, listOf(mode("frozen", 1, listOf(0, 3), machine)))
            ),
            resources = listOf(resource(machine), resource(alternate)),
            dependencies = listOf(ActivityDependency(c, a), ActivityDependency(Task("done"), c)),
            baselines = listOf(
                ActivityBaseline(key(a, "original", 0), false, q(2.0), q(4.0)),
                ActivityBaseline(key(b, "frozen", 3), true, q(1.0), q(1.0))
            ),
            executions = listOf(running, done)
        ))
        val solution = optimum(context)
        assertEquals(Flt64(2.0), solution.cost.value)
        assertEquals(key(a, "original", 2), solution.placements.single { it.key.task == a }.key)
        assertEquals(key(b, "frozen", 3), solution.placements.single { it.key.task == b }.key)
        assertEquals(listOf(running, done), solution.executions)
        assertEquals(at(-1), solution.executions.first().placement.time.start)
        assertEquals(at(0), solution.executions.first().placement.reservations.single().time.start)
    }

    @Test
    fun resourceCalendarAndMidActivityCapacityDropAreNotMissed() {
        val context = context(problem(
            activities = listOf(Activity(a, listOf(mode("work", 2, listOf(0, 2, 4), machine, amount = 2.0)))),
            resources = listOf(ActivityResource(
                id = machine,
                capacity = q(2.0),
                unavailableTimes = listOf(TimeRange(at(1), at(2))),
                capacities = listOf(ActivityCapacity(TimeRange(at(3), at(4)), q(1.0)))
            ))
        ))
        assertFalse(context.validate(listOf(key(a, "work", 0))).feasible)
        assertFalse(context.validate(listOf(key(a, "work", 2))).feasible)
        assertTrue(context.validate(listOf(key(a, "work", 4))).feasible)
        assertEquals(at(4), optimum(context).placements.single().time.start)
    }

    @Test
    fun allFourDependencyKindsAndMaximumNegativeLagsAgreeWithDirectTimeArithmetic() {
        for (from in ActivityEndpoint.entries) {
            for (to in ActivityEndpoint.entries) {
                val dependency = ActivityDependency(
                    predecessor = a,
                    successor = b,
                    from = from,
                    to = to,
                    minimum = (-1).hours,
                    maximum = 1.hours
                )
                val context = context(problem(
                    activities = listOf(
                        Activity(a, listOf(mode("a", 2, listOf(0, 2), machine))),
                        Activity(b, listOf(mode("b", 1, listOf(0, 1, 3), alternate)))
                    ),
                    resources = listOf(resource(machine), resource(alternate)),
                    dependencies = listOf(dependency)
                ))
                val model = LinearMetaModel(name = "endpoint_test")
                assertTrue(context.register(model).ok)
                for (start in listOf(0, 2)) {
                    for (next in listOf(0, 1, 3)) {
                        val lag = next + (if (to == ActivityEndpoint.Finish) 1 else 0) - start - (if (from == ActivityEndpoint.Finish) 2 else 0)
                        val choices = listOf(key(a, "a", start), key(b, "b", next))
                        assertEquals(lag in -1..1, context.validate(choices).feasible)
                        assertEquals(lag in -1..1, isFeasible(model, valuesFor(context, model, choices)))
                    }
                }
                model.close()
            }
        }
    }

    @Test
    fun synchronizationCyclesAreAllowedWhenTimeRelationsAreConsistent() {
        val context = context(problem(
            activities = listOf(Activity(a, listOf(mode("a", 1, listOf(0), machine))), Activity(b, listOf(mode("b", 1, listOf(0), alternate)))),
            resources = listOf(resource(machine), resource(alternate)),
            dependencies = listOf(
                ActivityDependency(a, b, ActivityEndpoint.Start, ActivityEndpoint.Start, Duration.ZERO, Duration.ZERO),
                ActivityDependency(b, a, ActivityEndpoint.Start, ActivityEndpoint.Start, Duration.ZERO, Duration.ZERO)
            )
        ))
        assertEquals(2, optimum(context).placements.size)
    }

    @Test
    fun optionalPredecessorCannotBeSilentlyCancelledForMandatorySuccessor() {
        for (presence in ActivityDependencyPresence.entries) {
            val context = context(problem(
                activities = listOf(Activity(a, emptyList(), q(2.0)), Activity(b, listOf(mode("b", 1, listOf(0), machine)))),
                resources = listOf(resource(machine)),
                dependencies = listOf(ActivityDependency(a, b, presence = presence))
            ))
            val selected = listOf(key(b, "b", 0))
            val expected = presence == ActivityDependencyPresence.WhenBothPresent
            assertEquals(expected, context.validate(selected).feasible)
            val model = LinearMetaModel(name = "presence_test")
            assertTrue(context.register(model).ok)
            assertEquals(expected, isFeasible(model, valuesFor(context, model, selected)))
            model.close()
        }
    }

    @Test
    fun extraCoverageAndBudgetRowsChangeTheOptimumAndCheckCancellation() {
        fun build(extra: Boolean): ActivitySchedulingContext<Flt64> = context(problem(
            activities = listOf(Activity(a, listOf(mode("cheap", 1, listOf(0), machine), mode("green", 1, listOf(0), alternate, cost = 3.0)), q(10.0))),
            resources = listOf(resource(machine), resource(alternate)),
            rows = if (extra) listOf(ActivityRow(
                id = "green_required",
                unit = NoneUnit,
                coefficients = mapOf(key(a, "green", 0) to q(1.0)),
                cancellationCoefficients = mapOf(a to q(-1.0)),
                lower = q(1.0)
            )) else emptyList()
        ))
        assertEquals("cheap", optimum(build(false)).placements.single().key.mode)
        val constrained = build(true)
        assertEquals("green", optimum(constrained).placements.single().key.mode)
        assertFalse(constrained.validate(emptyList()).feasible)
    }

    @Test
    fun freshRegistrationsNeverReuseVariablesAndRejectWrongModelOrFractionalSolutions() {
        val input = problem(listOf(Activity(a, listOf(mode("work", 1, listOf(0, 1), machine)))), listOf(resource(machine)))
        val first = context(input)
        val second = context(input)
        val model = LinearMetaModel(name = "first")
        val other = LinearMetaModel(name = "second")
        assertTrue(first.register(model).ok)
        assertFalse(first.register(other).ok)
        assertTrue(second.register(other).ok)
        assertNotSame(first.aggregation.variable(key(a, "work", 0)), second.aggregation.variable(key(a, "work", 0)))
        assertFalse(first.extractSolution(other).ok)
        assertFalse(first.extractSolution(model).ok)
        model.setSolution(listOf(Flt64(0.5), Flt64(0.5)))
        assertFalse(first.extractSolution(model).ok)
        model.setSolution(listOf(Flt64(1.0 + 1e-10), Flt64(-1e-10)))
        val tolerantResult = first.extractSolution(model)
        assertTrue(tolerantResult.ok, (tolerantResult as? Failed<*, *, *>)?.message)
        model.close()
        other.close()
    }

    @Test
    fun impossibleRequiredActivityRemainsAnInfeasibleModelNotAConstructionError() {
        val context = context(problem(listOf(Activity<Flt64>(a, emptyList())), emptyList()))
        val model = LinearMetaModel(name = "impossible")
        assertTrue(context.register(model).ok)
        assertFalse(context.validate(emptyList()).feasible)
        assertFalse(isFeasible(model, emptyList()))
        model.close()
    }

    @Test
    fun fixedFactsAloneCanViolateCapacityAndDependency() {
        val fact = ActivityExecution(
            placement = ActivityPlacement(key(a, "running", -1), TimeRange(at(-1), at(1)), listOf(ActivityReservation(machine, TimeRange(at(0), at(1)), q(2.0)))),
            status = ActivityExecutionStatus.Running
        )
        val context = context(problem(emptyList(), listOf(resource(machine)), executions = listOf(fact)))
        val model = LinearMetaModel(name = "fixed_infeasible")
        assertTrue(context.register(model).ok)
        assertFalse(context.validate(emptyList()).feasible)
        assertFalse(isFeasible(model, emptyList()))
        model.close()
    }

    @Test
    fun downstreamVariableBoundsAreValidatedEvenWithoutExtraRows() {
        val context = context(problem(listOf(Activity(a, listOf(mode("work", 1, listOf(0), machine)))), listOf(resource(machine))))
        val model = LinearMetaModel(name = "extra_bounds")
        assertTrue(context.register(model).ok)
        val extra = BinVar("external_fixed_zero")
        extra.range.setFalse()
        assertTrue(model.add(extra).ok)
        model.setSolution(model.tokens.tokensInSolver.map { Flt64.one })
        assertFalse(context.extractSolution(model).ok)
        model.close()
    }

    @Test
    fun wrongUnitsAndNonFiniteOrFailingConversionsAreRejectedBeforeRegistration() {
        val input = problem(listOf(Activity(a, listOf(mode("work", 1, listOf(0), machine)))), listOf(resource(machine)))
        val broken = object : SchedulingSolverValueAdapter<Flt64> by schedulingSolverValueAdapter {
            override fun fromValue(value: Flt64): Flt64 = throw IllegalArgumentException("adapter failure")
        }
        assertFalse(ActivitySchedulingContext.create(input, broken).ok)
        val nonFinite = object : SchedulingSolverValueAdapter<Flt64> by schedulingSolverValueAdapter {
            override fun fromValue(value: Flt64): Flt64 = Flt64.infinity
        }
        assertFalse(ActivitySchedulingContext.create(input, nonFinite).ok)
        val underflow = object : SchedulingSolverValueAdapter<Flt64> by schedulingSolverValueAdapter {
            override fun fromValue(value: Flt64): Flt64 = Flt64.zero
        }
        assertFalse(ActivitySchedulingContext.create(input, underflow).ok)
        assertFalse(context(input).validate(listOf(key(a, "unknown", 0))).feasible)
        assertFalse(context(input).validate(listOf(key(a, "work", 0), key(a, "work", 0))).feasible)
    }

    @Test
    fun fltXMassReservationsNormalizeWithoutLosingResourceIdentity() {
        val input = ActivitySchedulingProblem(
            horizon = TimeRange(at(0), at(3)),
            activities = listOf(Activity(a, listOf(ActivityMode(
                id = "mass",
                duration = 1.hours,
                starts = listOf(at(0)),
                resources = listOf(ActivityResourceUse(machine, Quantity(FltX("500"), Gram))),
                cost = Quantity(FltX("1.25"), NoneUnit)
            )))),
            resources = listOf(ActivityResource(machine, Quantity(FltX("0.5"), Kilogram))),
            costUnit = NoneUnit
        )
        val context = assertNotNull(ActivitySchedulingContext.create(input, GenericSolverValueAdapter(FltX)).value)
        val model = LinearMetaModel(name = "fltx")
        assertTrue(context.register(model).ok)
        model.setSolution(listOf(Flt64.one))
        val solution = assertNotNull(context.extractSolution(model).value)
        assertTrue(solution.cost.value eq FltX("1.25"))
        assertEquals(Gram, solution.placements.single().reservations.single().quantity.unit)
        model.close()
    }

    @Test
    fun invalidUnitsIdentitiesReferencesAndExecutionContractsReturnErrors() {
        val baseMode = mode("work", 1, listOf(0), machine)
        val cases = listOf(
            problem(listOf(Activity(a, listOf(baseMode)), Activity(a, listOf(baseMode))), listOf(resource(machine))),
            problem(listOf(Activity(a, listOf(baseMode))), emptyList()),
            problem(listOf(Activity(a, listOf(baseMode, baseMode))), listOf(resource(machine))),
            problem(listOf(Activity(a, listOf(mode("bad", -1, listOf(0), machine)))), listOf(resource(machine))),
            problem(listOf(Activity(a, listOf(mode("bad", 1, listOf(0, 0), machine)))), listOf(resource(machine))),
            problem(listOf(Activity(a, listOf(mode("bad", 2, listOf(5), machine)))), listOf(resource(machine))),
            problem(listOf(Activity(a, listOf(baseMode))), listOf(resource(machine)), dependencies = listOf(ActivityDependency(a, b))),
            problem(listOf(Activity(a, listOf(baseMode))), listOf(resource(machine)), baselines = listOf(ActivityBaseline(key(a, "missing", 0), true, q(0.0), q(0.0)))),
            problem(listOf(Activity(a, listOf(baseMode))), listOf(ActivityResource(machine, Quantity(Flt64.one, Kilogram)))),
            problem(listOf(Activity(a, listOf(baseMode))), listOf(resource(machine)), rows = listOf(ActivityRow("bad", NoneUnit, mapOf(key(b, "unknown", 0) to q(1.0)), upper = q(1.0)))),
            problem(emptyList(), listOf(resource(machine)), executions = listOf(ActivityExecution(
                placement = ActivityPlacement(key(a, "run", -1), TimeRange(at(-1), at(1)), listOf(ActivityReservation(machine, TimeRange(at(-1), at(1)), q(1.0)))),
                status = ActivityExecutionStatus.Running
            )))
        )
        cases.forEachIndexed { index, input -> assertFalse(ActivitySchedulingContext.create(input, schedulingSolverValueAdapter).ok, "case=$index") }
    }

    private fun problem(
        activities: List<Activity<Flt64>>,
        resources: List<ActivityResource<Flt64>>,
        dependencies: List<ActivityDependency> = emptyList(),
        baselines: List<ActivityBaseline<Flt64>> = emptyList(),
        executions: List<ActivityExecution<Flt64>> = emptyList(),
        rows: List<ActivityRow<Flt64>> = emptyList(),
        additionalCost: ((ActivityPlacement<Flt64>) -> Ret<Quantity<Flt64>>)? = null
    ) = ActivitySchedulingProblem(
        horizon = TimeRange(at(0), at(6)),
        activities = activities,
        resources = resources,
        dependencies = dependencies,
        baselines = baselines,
        executions = executions,
        rows = rows,
        costUnit = NoneUnit,
        additionalCost = additionalCost
    )

    private fun mode(id: String, duration: Int, starts: List<Int>, resource: Resource, cost: Double = 0.0, amount: Double = 1.0) = ActivityMode(
        id = id,
        duration = duration.hours,
        starts = starts.map(::at),
        resources = listOf(ActivityResourceUse(resource, q(amount))),
        cost = q(cost)
    )

    private fun context(input: ActivitySchedulingProblem<Flt64>) = assertNotNull(ActivitySchedulingContext.create(input, schedulingSolverValueAdapter).value)
    private fun resource(id: Resource, capacity: Double = 1.0) = ActivityResource(id, q(capacity))
    private fun at(hour: Int): Instant = epoch + hour.hours
    private fun q(value: Double) = Quantity(Flt64(value), NoneUnit)
    private fun key(task: Task, mode: String, start: Int) = ActivityChoiceKey(task, mode, at(start))
    private fun isFeasible(model: LinearMetaModel<Flt64>, values: List<Flt64>) = model.constraints.all { it.isTrue(values, model.tokens, false) == true }

    private fun valuesFor(context: ActivitySchedulingContext<Flt64>, model: LinearMetaModel<Flt64>, choices: List<ActivityChoiceKey>): List<Flt64> {
        val selected = choices.mapNotNull { context.aggregation.variable(it) }.toSet() + context.problem.activities
            .filter { task -> choices.none { it.task == task.id } }.mapNotNull { context.aggregation.cancellationVariable(it.id) }
        return model.tokens.tokensInSolver.map { if (it.variable in selected) Flt64.one else Flt64.zero }
    }

    private fun optimum(context: ActivitySchedulingContext<Flt64>): ActivitySchedule<Flt64> {
        val model = LinearMetaModel(name = "activity_oracle")
        assertTrue(context.register(model).ok)
        val size = model.tokens.tokensInSolver.size
        assertTrue(size <= 16)
        var best: Flt64? = null
        var solution: List<Flt64>? = null
        for (mask in 0 until (1 shl size)) {
            val values = List(size) { if (mask and (1 shl it) != 0) Flt64.one else Flt64.zero }
            if (!isFeasible(model, values)) continue
            val expression = context.aggregation.compilation.cost
            val tokens = model.tokens.tokensInSolver
            val objective = expression.monomials.fold(expression.constant) { sum, term ->
                sum + term.coefficient * values[tokens.indexOfFirst { it.variable == term.symbol }]
            }
            if (best == null || (objective ls best)) {
                best = objective
                solution = values
            }
        }
        model.setSolution(assertNotNull(solution, "No feasible binary assignment"))
        val schedule = assertNotNull(context.extractSolution(model).value)
        assertEquals(best, schedule.cost.value)
        assertTrue(context.validate(schedule.placements.map { it.key }).feasible)
        model.close()
        return schedule
    }
}
