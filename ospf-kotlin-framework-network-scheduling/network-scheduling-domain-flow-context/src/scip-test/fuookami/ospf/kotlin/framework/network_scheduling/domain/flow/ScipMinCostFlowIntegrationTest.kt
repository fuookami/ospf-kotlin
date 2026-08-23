package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.scip.ScipColumnGenerationSolver
import fuookami.ospf.kotlin.core.solver.scip.ScipSolverCallBack
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service.MinCostFlowObjective
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service.limits.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** SCIP 真实求解网络流的集成校验。 / Integration check that solves a network flow with SCIP. */
class ScipMinCostFlowIntegrationTest {
    @Test
    fun minCostFlowShouldSolveKnownOptimalWithScip() {
        assumeTrue(isScipRuntimeAvailable(), "SCIP runtime not available in current environment")

        val networkArcs = listOf(
            arc("source", "a", Flt64(10.0)),
            arc("source", "b", Flt64.zero),
            arc("a", "b", Flt64(-5.0)),
            arc("a", "sink", Flt64(10.0)),
            arc("b", "sink", Flt64.one)
        )
        val flowArcs = networkArcs.map {
            val upper = if (it.from.value == "a" && it.to.value == "b") {
                Flt64(2.0)
            } else {
                Flt64(4.0)
            }
            val lower = if (it.from.value == "a" && it.to.value == "b") {
                Flt64.one
            } else {
                Flt64.zero
            }
            flowArc(it, upper, lower)
        }
        val graph = assertNotNull(
            FlowGraph(
                networkGraph = assertNotNull(
                    NetworkGraph(
                        nodes = listOf("source", "a", "b", "sink").map(::node),
                        arcs = networkArcs
                    ).value
                ),
                arcs = flowArcs,
                supplyDemands = listOf(
                    supply("source", Flt64(4.0)),
                    supply("sink", Flt64(-4.0))
                ),
                flowUnit = NoneUnit
            ).value
        )
        val model = LinearMetaModel(name = "scip_four_node_mcmf")
        try {
            assertTrue(FlowConservationConstraint(graph, Flt64NetworkSchedulingSolverValueAdapter)(model).ok)
            assertTrue(CapacityBoundConstraint(graph, Flt64NetworkSchedulingSolverValueAdapter)(model).ok)
            assertTrue(
                MinCostFlowObjective(
                    graph = graph,
                    valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
                    costUnit = NoneUnit
                )(model).ok
            )

            val solver = ScipColumnGenerationSolver(
                config = SolverConfig(threadNum = UInt64.one),
                callBack = ScipSolverCallBack().configuration { _, scip, _, _ ->
                    scip.setIntParam("display/verblevel", 0)
                    ok
                }
            )
            val lpResult = when (val result = runBlocking {
                solver.solveLPWithStatus(
                    name = "scip_four_node_mcmf",
                    metaModel = model
                )
            }) {
                is Ok -> when (val output = result.value) {
                    is ColumnGenerationSolver.LPResultWithStatus.Feasible -> output.result
                    is ColumnGenerationSolver.LPResultWithStatus.Infeasible -> {
                        fail("SCIP reported the known-feasible MCMF model as infeasible")
                    }
                }
                is Failed -> fail("SCIP MCMF LP solve failed: ${result.error}")
                is Fatal -> fail("SCIP MCMF LP solve was fatal: ${result.errors}")
            }

            assertEquals(SolverStatus.Optimal, lpResult.status)
            assertTrue(lpResult.obj eq Flt64(9.0), "SCIP should find the minimum cost of 9 with a->b lower bound")
            model.setSolution(lpResult.solution)
            val expected = listOf(Flt64.one, Flt64(3.0), Flt64.one, Flt64.zero, Flt64(4.0))
            for ((index, expectedValue) in expected.withIndex()) {
                val actual = assertNotNull(model.tokens.find(graph.flowVariables[index])?.result)
                assertTrue(actual eq expectedValue, "Unexpected flow at arc index $index: $actual")
            }
        } finally {
            model.close()
        }
    }

    private fun isScipRuntimeAvailable(): Boolean {
        return runCatching {
            Class.forName("jscip.Scip")
            System.loadLibrary("jscip")
        }.isSuccess
    }

    private fun node(id: String): NetworkNode<Flt64> {
        return assertNotNull(NetworkNode<Flt64>(NetworkNodeId(id)).value)
    }

    private fun arc(from: String, to: String, cost: Flt64): NetworkArc<Flt64> {
        return assertNotNull(
            NetworkArc(
                from = NetworkNodeId(from),
                to = NetworkNodeId(to),
                cost = Quantity(cost, NoneUnit)
            ).value
        )
    }

    private fun flowArc(arc: NetworkArc<Flt64>, upper: Flt64, lower: Flt64 = Flt64.zero): FlowArc<Flt64> {
        return assertNotNull(
            FlowArc(
                networkArc = arc,
                capacity = assertNotNull(
                    CapacityBounds(
                        lower = Quantity(lower, NoneUnit),
                        upper = Quantity(upper, NoneUnit)
                    ).value
                )
            ).value
        )
    }

    private fun supply(id: String, net: Flt64): SupplyDemand<Flt64> {
        return assertNotNull(
            SupplyDemand(
                nodeId = NetworkNodeId(id),
                balance = assertNotNull(NodeBalance(Quantity(net, NoneUnit)).value)
            ).value
        )
    }
}
