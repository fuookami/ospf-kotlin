@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.application

import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.service.*

/** 独立入口和扩展装配的合同测试。 / Contract tests for standalone entry and extension assembly. */
class ActivitySchedulingApplicationServiceTest {
    private val start = Instant.parse("2026-10-22T00:00:00Z")
    private data class Task(val value: String) : TaskId
    private val task = Task("work")

    @Test
    fun noIncumbentTimeLimitAndInfeasibilityRemainStructuredOutcomes() = runBlocking {
        for ((status, termination) in listOf(
            ProblemStatus.Unknown to TerminationReason.TimeLimit,
            ProblemStatus.Infeasible to TerminationReason.Completed,
            ProblemStatus.Unknown to TerminationReason.Cancelled
        )) {
            val solver = FixtureSolver { ok(SolveReport(
                problemStatus = status,
                terminationReason = termination,
                solutionPresence = SolutionPresence.None
            )) }
            val outcome = assertNotNull(ActivitySchedulingApplicationService(solver, schedulingSolverValueAdapter).solve(problem()).value)
            assertNull(outcome.schedule)
            assertEquals(status, outcome.report.problemStatus)
            assertEquals(termination, outcome.report.terminationReason)
            assertEquals(1, solver.calls)
        }
    }

    @Test
    fun externalConstraintChangesOptimalChoiceWithoutModifyingApplication() = runBlocking {
        val solver = FixtureSolver { model ->
            val tokens = model.tokens.tokensInSolver
            val candidates = listOf(listOf(Flt64.one, Flt64.zero), listOf(Flt64.zero, Flt64.one))
            val chosen = candidates.single { values -> model.constraints.all { it.isTrue(values, model.tokens, false) == true } }
            assertEquals(2, tokens.size)
            ok(report(chosen, optimal = true))
        }
        val service = ActivitySchedulingApplicationService(solver, schedulingSolverValueAdapter)
        val outcome = assertNotNull(service.solve(problem()) { context, model ->
            val early = assertNotNull(context.aggregation.variable(ActivityChoiceKey(task, "early", start)))
            model.addConstraint(early eq Flt64.zero, name = "customer_forbids_early")
        }.value)
        assertEquals("late", assertNotNull(outcome.schedule).placements.single().key.mode)
        assertEquals(Flt64(3.0), outcome.schedule.cost.value)
        assertEquals(SolutionPresence.Optimal, outcome.report.solutionPresence)
        assertEquals(2, outcome.schedule.candidateScope.size)
    }

    @Test
    fun timeLimitedIncumbentIsNotUpgradedToOptimal() = runBlocking {
        val solver = FixtureSolver { ok(report(listOf(Flt64.one, Flt64.zero), optimal = false)) }
        val outcome = assertNotNull(ActivitySchedulingApplicationService(solver, schedulingSolverValueAdapter).solve(problem()).value)
        assertNotNull(outcome.schedule)
        assertEquals(SolutionPresence.Incumbent, outcome.report.solutionPresence)
        assertEquals(TerminationReason.TimeLimit, outcome.report.terminationReason)
        assertEquals(ProofStatus.None, outcome.report.proof.status)
    }

    @Test
    fun wrongDimensionNonFiniteAndFractionalIncumbentsAreRejected() = runBlocking {
        for (values in listOf(
            listOf(Flt64.one),
            listOf(Flt64.infinity, Flt64.zero),
            listOf(Flt64(0.5), Flt64(0.5)),
            listOf(Flt64.one, Flt64.one)
        )) {
            val solver = FixtureSolver { ok(report(values, optimal = false)) }
            assertFalse(ActivitySchedulingApplicationService(solver, schedulingSolverValueAdapter).solve(problem()).ok)
        }
    }

    @Test
    fun externalRuleIsRecheckedEvenIfBackendReturnsAnInvalidIncumbent() = runBlocking {
        val solver = FixtureSolver { ok(report(listOf(Flt64.one, Flt64.zero), optimal = true)) }
        val result = ActivitySchedulingApplicationService(solver, schedulingSolverValueAdapter).solve(problem()) { context, model ->
            model.addConstraint(assertNotNull(context.aggregation.variable(ActivityChoiceKey(task, "early", start))) eq Flt64.zero, name = "forbid")
        }
        assertFalse(result.ok)
    }

    private fun report(values: List<Flt64>, optimal: Boolean): SolveReport<Flt64> = SolveReport(
        problemStatus = ProblemStatus.Feasible,
        terminationReason = if (optimal) TerminationReason.Completed else TerminationReason.TimeLimit,
        solutionPresence = if (optimal) SolutionPresence.Optimal else SolutionPresence.Incumbent,
        solution = SolveSolution(values, Flt64(3.0))
    )

    private fun problem() = ActivitySchedulingProblem(
        horizon = TimeRange(start, start + 4.hours),
        activities = listOf(Activity(task, listOf(
            ActivityMode("early", 1.hours, listOf(start), emptyList(), Quantity(Flt64.one, NoneUnit)),
            ActivityMode("late", 1.hours, listOf(start + 1.hours), emptyList(), Quantity(Flt64(3.0), NoneUnit))
        ))),
        resources = emptyList(),
        costUnit = NoneUnit
    )

    private class FixtureSolver(private val report: (LinearMetaModel<Flt64>) -> Ret<SolveReport<Flt64>>) : ColumnGenerationSolver {
        override val name = "activity-fixture"
        var calls = 0
        override suspend fun solveMILP(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            calls += 1
            return report(metaModel)
        }

        override suspend fun solveLP(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<ColumnGenerationSolver.LPResult> = error("Activity scheduling does not invoke pricing")
    }
}
