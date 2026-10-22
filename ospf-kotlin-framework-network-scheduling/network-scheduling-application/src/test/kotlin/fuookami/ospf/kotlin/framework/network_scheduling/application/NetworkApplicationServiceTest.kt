package fuookami.ospf.kotlin.framework.network_scheduling.application

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.service.*

/** 正常无解终态和独立/嵌入式装配使用同一合同。 / Normal no-solution outcomes and shared standalone/embedded assembly. */
class NetworkApplicationServiceTest {
    @Test
    fun noIncumbentTimeLimitIsAResultRatherThanExtractionFailure() = runBlocking {
        val solver = ReportSolver { SolveReport(
            problemStatus = ProblemStatus.Unknown, terminationReason = TerminationReason.TimeLimit,
            solutionPresence = SolutionPresence.None
        ) }
        val result = assertNotNull(NetworkFlowApplicationService(solver, Flt64NetworkSchedulingSolverValueAdapter).solve(problem()).value)
        assertNull(result.solution)
        assertEquals(TerminationReason.TimeLimit, result.report.terminationReason)
        assertEquals(1, solver.calls)
    }

    @Test
    fun infeasibilityIsPreservedWithoutInventingAFeasibleFlow() = runBlocking {
        val solver = ReportSolver { SolveReport(
            problemStatus = ProblemStatus.Infeasible, terminationReason = TerminationReason.Completed,
            solutionPresence = SolutionPresence.None
        ) }
        val result = assertNotNull(NetworkFlowApplicationService(solver, Flt64NetworkSchedulingSolverValueAdapter).solve(problem()).value)
        assertEquals(ProblemStatus.Infeasible, result.report.problemStatus)
        assertNull(result.solution)
    }

    @Test
    fun downstreamRuleParticipatesWithoutChangingApplicationOrchestration() = runBlocking {
        val solver = ReportSolver { model ->
            assertTrue(model.constraints.any { it.toString().isNotBlank() })
            val values = listOf(Flt64.one)
            assertTrue(model.constraints.all { it.isTrue(values, model.tokens, false) == true })
            SolveReport(
                problemStatus = ProblemStatus.Feasible, terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Optimal, solution = SolveSolution(values, Flt64(2.0))
            )
        }
        val result = NetworkFlowApplicationService(solver, Flt64NetworkSchedulingSolverValueAdapter).solve(problem()) { context, model ->
            val expression = assertNotNull(context.flow(FlowKey("goods", NetworkArcId("service"))).value)
            model.addConstraint(expression leq Flt64.one, name = "business_budget")
        }
        assertEquals(Flt64(2.0), assertNotNull(result.value?.solution).cost.value)
    }

    private fun problem(): FlowProblem<Flt64> {
        fun q(value: Double) = Quantity(Flt64(value), NoneUnit)
        val nodes = listOf("s", "t").map { assertNotNull(NetworkNode<Flt64>(NetworkNodeId(it)).value) }
        val arc = assertNotNull(NetworkArc(from = nodes[0].id, to = nodes[1].id, cost = q(2.0), id = NetworkArcId("service")).value)
        val commodity = assertNotNull(FlowCommodity("goods", listOf(
            assertNotNull(SupplyDemand(nodes[0].id, q(1.0)).value), assertNotNull(SupplyDemand(nodes[1].id, q(-1.0)).value)
        )).value)
        return FlowProblem(
            graph = assertNotNull(NetworkGraph(nodes, listOf(arc)).value),
            arcs = listOf(assertNotNull(FlowArc(arc, assertNotNull(CapacityBounds(q(0.0), q(2.0)).value)).value)),
            commodities = listOf(CommodityFlow(commodity, NoneUnit)), zero = Flt64.zero
        )
    }

    private class ReportSolver(private val report: (LinearMetaModel<Flt64>) -> SolveReport<Flt64>) : ColumnGenerationSolver {
        override val name = "report-fixture"
        var calls = 0
        override suspend fun solveMILP(
            name: String, metaModel: LinearMetaModel<Flt64>, toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?, solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> { calls += 1; return ok(report(metaModel)) }
        override suspend fun solveLP(
            name: String, metaModel: LinearMetaModel<Flt64>, toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?, solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<ColumnGenerationSolver.LPResult> = error("Not used by standalone orchestration")
    }
}
