package fuookami.ospf.kotlin.framework.network_scheduling.application

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.allocation.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.service.*

/** 新业务入口与嵌入式注册共享领域模型。 / New business entries share domain assembly with embedded registration. */
class NetworkBusinessApplicationServiceTest {
    @Test
    fun pathEntryPreservesPayloadScopeAndAllowsDeclaredExtraResource() = runBlocking {
        val solver = FixtureSolver { model ->
            val values = listOf(Flt64.zero, Flt64.one)
            assertTrue(model.constraints.all { it.isTrue(values, model.tokens, false) == true })
            ok(SolveReport(
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Optimal,
                solution = SolveSolution(values, Flt64(5.0))
            ))
        }
        val service = NetworkPathApplicationService(solver, Flt64NetworkSchedulingSolverValueAdapter)
        val problem = pathProblem()
        repeat(2) {
            val result = assertNotNull(service.solve(problem).value)
            val solution = assertNotNull(result.solution)
            assertEquals(listOf("rail"), solution.selected.map { it.payload })
            assertEquals(setOf("road", "rail"), solution.candidateIds)
        }
        assertEquals(2, solver.calls)
    }

    @Test
    fun noIncumbentAndTechnicalFailureAreDifferentOutcomes() = runBlocking {
        val timeout = FixtureSolver { ok(SolveReport(
            problemStatus = ProblemStatus.Unknown,
            terminationReason = TerminationReason.TimeLimit,
            solutionPresence = SolutionPresence.None
        )) }
        val result = assertNotNull(NetworkPathApplicationService(timeout, Flt64NetworkSchedulingSolverValueAdapter).solve(pathProblem()).value)
        assertNull(result.solution)
        assertEquals(TerminationReason.TimeLimit, result.report.terminationReason)
        val failure = FixtureSolver { networkSchedulingFailure("测试技术失败 / Test technical failure") }
        assertFalse(NetworkPathApplicationService(failure, Flt64NetworkSchedulingSolverValueAdapter).solve(pathProblem()).ok)
    }

    @Test
    fun allocationUsesFreshUnderlyingDesignAndReturnsBusinessKeys() = runBlocking {
        val solver = FixtureSolver { model ->
            val values = model.tokens.tokensInSolver.map { Flt64(4.0) }
            assertTrue(model.constraints.all { it.isTrue(values, model.tokens, false) == true })
            ok(SolveReport(
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Incumbent,
                solution = SolveSolution(values, Flt64(8.0))
            ))
        }
        val service = NetworkAllocationApplicationService(solver, Flt64NetworkSchedulingSolverValueAdapter)
        repeat(2) {
            val result = assertNotNull(service.solve(allocationProblem()).value?.solution)
            assertEquals(Flt64(4.0), result.shipments.getValue(AllocationFlowKey("shipment", NetworkArcId("road"))).value)
            assertEquals(Flt64(8.0), result.totalCost.value)
        }
        assertEquals(2, solver.calls)
    }

    @Test
    fun generalizedFlowEntryRetainsDistinctInputAndArrivalQuantities() = runBlocking {
        val solver = FixtureSolver { model ->
            val values = listOf(Flt64(2.0), Flt64(2.0))
            assertTrue(model.constraints.all { it.isTrue(values, model.tokens, false) == true })
            ok(SolveReport(
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Optimal,
                solution = SolveSolution(values, Flt64(8.0))
            ))
        }
        val nodes = listOf("s" to 4.0, "t" to -3.0).map { (id, supply) ->
            assertNotNull(GeneralizedFlowNode(
                networkNode = assertNotNull(NetworkNode<Flt64>(NetworkNodeId(id)).value),
                unit = NoneUnit,
                netSupply = q(supply)
            ).value)
        }
        val arcs = listOf("lossy" to 1.0, "standard" to 3.0).map { (id, cost) ->
            assertNotNull(NetworkArc(
                from = NetworkNodeId("s"),
                to = NetworkNodeId("t"),
                cost = q(cost),
                id = NetworkArcId(id)
            ).value)
        }
        val problem = GeneralizedFlowProblem(
            graph = assertNotNull(NetworkGraph(nodes.map { it.networkNode }, arcs).value),
            nodes = nodes,
            arcs = arcs.map { arc -> assertNotNull(GeneralizedFlowArc(
                networkArc = arc,
                inputCapacity = assertNotNull(CapacityBounds(q(0.0), q(4.0)).value),
                productionYield = q(if (arc.id.value == "lossy") 0.5 else 1.0)
            ).value) },
            zero = Flt64.zero
        )
        val service = NetworkGeneralizedFlowApplicationService(solver, Flt64NetworkSchedulingSolverValueAdapter)
        repeat(2) {
            val result = assertNotNull(service.solve(problem).value?.solution)
            assertEquals(Flt64(8.0), result.totalCost.value)
            assertEquals(Flt64.one, result.arcs.getValue(NetworkArcId("lossy")).output.value)
        }
    }

    @Test
    fun structureEntryPreservesNormalNoIncumbentOutcome() = runBlocking {
        val root = assertNotNull(NetworkNode<Flt64>(NetworkNodeId("root")).value)
        val problem = NetworkStructureProblem(
            design = NetworkDesignProblem(FlowProblem(
                graph = assertNotNull(NetworkGraph(listOf(root), emptyList<NetworkArc<Flt64>>()).value),
                arcs = emptyList(),
                commodities = emptyList(),
                zero = Flt64.zero
            )),
            root = root.id,
            terminals = emptySet()
        )
        val solver = FixtureSolver { ok(SolveReport(
            problemStatus = ProblemStatus.Unknown,
            terminationReason = TerminationReason.TimeLimit,
            solutionPresence = SolutionPresence.None
        )) }
        val service = NetworkStructureApplicationService(solver, Flt64NetworkSchedulingSolverValueAdapter)
        repeat(2) {
            val result = assertNotNull(service.solve(problem).value)
            assertNull(result.solution)
            assertEquals(TerminationReason.TimeLimit, result.report.terminationReason)
        }
    }

    private fun pathProblem() = PathCompilationProblem(
        rows = listOf(
            PathCompilationRow(id = "order", unit = NoneUnit, lower = q(1.0), upper = q(1.0)),
            PathCompilationRow(id = "road-capacity", unit = NoneUnit, upper = q(0.0))
        ),
        columns = listOf(
            PathColumn(id = "road", payload = "road", cost = q(1.0), coefficients = mapOf("order" to q(1.0), "road-capacity" to q(1.0))),
            PathColumn(id = "rail", payload = "rail", cost = q(5.0), coefficients = mapOf("order" to q(1.0)))
        ),
        costUnit = NoneUnit
    )

    private fun allocationProblem(): AllocationProblem<Flt64> {
        val source = NetworkNodeId("source")
        val target = NetworkNodeId("target")
        return AllocationProblem(
            locations = listOf(AllocationLocation(source), AllocationLocation(target)),
            services = listOf(AllocationService(
                id = NetworkArcId("road"),
                from = source,
                to = target,
                cost = q(2.0),
                capacity = assertNotNull(CapacityBounds(q(0.0), q(10.0)).value)
            )),
            requests = listOf(AllocationRequest(
                id = "shipment",
                origin = source,
                destination = target,
                quantity = q(4.0),
                integerStep = q(1.0)
            )),
            zero = Flt64.zero
        )
    }

    private fun q(value: Double) = Quantity(Flt64(value), NoneUnit)

    private class FixtureSolver(val solve: (LinearMetaModel<Flt64>) -> Ret<SolveReport<Flt64>>) : ColumnGenerationSolver {
        override val name = "network-business-fixture"
        var calls = 0

        override suspend fun solveMILP(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            calls += 1
            return solve(metaModel)
        }

        override suspend fun solveLP(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<ColumnGenerationSolver.LPResult> = error("Not used")
    }
}
