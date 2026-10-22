package fuookami.ospf.kotlin.framework.network_scheduling.application

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.scip.ScipColumnGenerationSolver
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.service.NetworkStructureApplicationService

/** 显式 SCIP profile 的根树与设施覆盖闭环，目标费用对照枚举期望。 / Opt-in SCIP rooted-tree and facility-coverage closure against the enumeration oracle. */
class NetworkStructureNativeIntegrationTest {
    @Test
    fun solverMatchesTheEnumeratedConstructionCostAndSelectedStructure() = runBlocking {
        assumeTrue(runtimeAvailable(), "SCIP native runtime unavailable; not verified")
        val solver = ScipColumnGenerationSolver(config = SolverConfig(threadNum = UInt64.one))
        val result = assertNotNull(NetworkStructureApplicationService(
            solver = solver,
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        ).solve(problem()).value)

        assertEquals(ProblemStatus.Feasible, result.report.problemStatus)
        val solution = assertNotNull(result.solution)
        assertEquals(Flt64(2.0), solution.construction.cost.value)
        assertEquals(setOf(NetworkNodeId("hub")), solution.construction.openFacilities)
        assertTrue(solution.construction.serviceLevels.isEmpty())
        assertEquals(setOf(NetworkArcId("root_hub"), NetworkArcId("hub_terminal")), solution.selectedTreeArcs)
        assertEquals(mapOf("district" to NetworkNodeId("hub")), solution.partitionOwners)
        assertEquals(setOf(NetworkNodeId("root"), NetworkNodeId("hub"), NetworkNodeId("terminal")), solution.connectedNodes)
    }

    @Test
    fun unreachableButWellFormedTerminalIsReportedAsModelInfeasible() = runBlocking {
        assumeTrue(runtimeAvailable(), "SCIP native runtime unavailable; not verified")
        val root = NetworkNodeId("root")
        val terminal = NetworkNodeId("terminal")
        val nodes = listOf(root, terminal).map { id ->
            assertNotNull(NetworkNode<Flt64>(id).value)
        }
        val backward = arc("backward", terminal, root)
        val flow = FlowProblem(
            graph = assertNotNull(NetworkGraph(nodes, listOf(backward)).value),
            arcs = listOf(FlowArc(
                networkArc = backward,
                capacity = assertNotNull(CapacityBounds(q(0.0), q(10.0)).value)
            ).let { assertNotNull(it.value) }),
            commodities = emptyList(),
            zero = Flt64.zero
        )
        val problem = NetworkStructureProblem(
            design = NetworkDesignProblem(
                flow = flow,
                facilities = listOf(NetworkFacility(
                    node = root,
                    cost = q(1.0),
                    required = true
                ))
            ),
            root = root,
            terminals = setOf(terminal)
        )
        val result = assertNotNull(NetworkStructureApplicationService(
            solver = ScipColumnGenerationSolver(config = SolverConfig(threadNum = UInt64.one)),
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        ).solve(problem).value)

        assertEquals(ProblemStatus.Infeasible, result.report.problemStatus)
        assertNull(result.solution)
    }

    private fun problem(): NetworkStructureProblem<Flt64> {
        val root = NetworkNodeId("root")
        val hub = NetworkNodeId("hub")
        val terminal = NetworkNodeId("terminal")
        val nodes = listOf(root, hub, terminal).map { id ->
            assertNotNull(NetworkNode<Flt64>(id).value)
        }
        val arcs = listOf(
            arc("root_hub", root, hub),
            arc("hub_terminal", hub, terminal),
            arc("direct", root, terminal)
        )
        val graph = assertNotNull(NetworkGraph(nodes, arcs).value)
        val flow = FlowProblem(
            graph = graph,
            arcs = arcs.map { networkArc ->
                assertNotNull(FlowArc(
                    networkArc = networkArc,
                    capacity = assertNotNull(CapacityBounds(q(0.0), q(10.0)).value)
                ).value)
            },
            commodities = emptyList(),
            zero = Flt64.zero
        )
        return NetworkStructureProblem(
            design = NetworkDesignProblem(
                flow = flow,
                services = listOf(NetworkService(
                    arc = NetworkArcId("direct"),
                    levels = listOf(CapacityLevel(
                        id = "built",
                        capacity = q(10.0),
                        cost = q(5.0)
                    ))
                )),
                facilities = listOf(NetworkFacility(
                    node = hub,
                    cost = q(2.0)
                ))
            ),
            root = root,
            terminals = setOf(terminal),
            steinerNodes = setOf(hub),
            partitions = listOf(FacilityPartitionRequest(
                id = "district",
                eligibleFacilities = setOf(hub)
            )),
            quotas = listOf(FacilityAssignmentQuota(
                facility = hub,
                minimum = 1,
                maximum = 1
            ))
        )
    }

    private fun arc(id: String, from: NetworkNodeId, to: NetworkNodeId): NetworkArc<Flt64> {
        return assertNotNull(NetworkArc(
            from = from,
            to = to,
            cost = q(0.0),
            id = NetworkArcId(id)
        ).value)
    }

    private fun q(value: Double) = Quantity(Flt64(value), NoneUnit)

    private fun runtimeAvailable(): Boolean = runCatching {
        Class.forName("jscip.Scip")
        System.loadLibrary("jscip")
    }.isSuccess
}
