package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.scip.ScipColumnGenerationSolver
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model.*

/** SCIP 真实求解广义流的集成校验。 / Integration check that solves generalized flow with SCIP. */
class ScipGeneralizedFlowIntegrationTest {
    @Test
    fun lossAdjustmentsShouldReachKnownOptimalSolutionWithScip() = runBlocking {
        assumeTrue(isScipRuntimeAvailable(), "SCIP runtime not available in current environment")

        val source = node("source", 4.0)
        val sink = node("sink", -3.0)
        val lossy = networkArc(
            id = "lossy",
            from = "source",
            to = "sink",
            tariff = 1.0
        )
        val standard = networkArc(
            id = "standard",
            from = "source",
            to = "sink",
            tariff = 3.0
        )
        val problem = GeneralizedFlowProblem(
            graph = assertNotNull(NetworkGraph(
                nodes = listOf(source.networkNode, sink.networkNode),
                arcs = listOf(lossy, standard)
            ).value),
            nodes = listOf(source, sink),
            arcs = listOf(
                generalizedArc(
                    networkArc = lossy,
                    upperInput = 4.0,
                    yield = 0.5
                ),
                generalizedArc(
                    networkArc = standard,
                    upperInput = 4.0,
                    yield = 1.0
                )
            ),
            zero = Flt64.zero,
            costUnit = NoneUnit
        )
        val context = GeneralizedFlowContext(
            problem = problem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "scip_generalized_flow"
        )
        val model = LinearMetaModel(name = "scip_generalized_flow")
        try {
            assertTrue(context.register(model).ok)
            val solver = ScipColumnGenerationSolver(config = SolverConfig(threadNum = UInt64.one))
            val result = when (val solve = solver.solveLPWithStatus(
                name = "scip_generalized_flow",
                metaModel = model
            )) {
                is Ok -> when (val output = solve.value) {
                    is ColumnGenerationSolver.LPResultWithStatus.Feasible -> output.result
                    is ColumnGenerationSolver.LPResultWithStatus.Infeasible -> fail(
                        "SCIP reported the known-feasible generalized-flow model as infeasible"
                    )
                }
                is Failed -> fail("SCIP generalized-flow LP solve failed: ${solve.error}")
                is Fatal -> fail("SCIP generalized-flow LP solve was fatal: ${solve.errors}")
            }

            assertEquals(SolverStatus.Optimal, result.status)
            assertTrue(result.obj eq Flt64(8.0), "SCIP should find the generalized-flow cost of 8")
            model.setSolution(result.solution)
            val solution = assertNotNull(context.extractSolution(model).value)
            assertTrue(solution.arcs.getValue(lossy.id).input.value eq Flt64(2.0))
            assertTrue(solution.arcs.getValue(standard.id).input.value eq Flt64(2.0))
            assertTrue(solution.arcs.getValue(lossy.id).output.value eq Flt64.one)
            assertTrue(solution.arcs.getValue(standard.id).output.value eq Flt64(2.0))
            assertTrue(solution.totalCost.value eq Flt64(8.0))
            assertTrue(solution.nodes.values.all { it.residual.value eq Flt64.zero })
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

    private fun node(id: String, netSupply: Double): GeneralizedFlowNode<Flt64> {
        val networkNode = assertNotNull(NetworkNode<Flt64>(NetworkNodeId(id)).value)
        return assertNotNull(GeneralizedFlowNode(
            networkNode = networkNode,
            unit = NoneUnit,
            netSupply = Quantity(Flt64(netSupply), NoneUnit)
        ).value)
    }

    private fun networkArc(id: String, from: String, to: String, tariff: Double): NetworkArc<Flt64> {
        return assertNotNull(NetworkArc(
            from = NetworkNodeId(from),
            to = NetworkNodeId(to),
            cost = Quantity(Flt64(tariff), NoneUnit),
            id = NetworkArcId(id)
        ).value)
    }

    private fun generalizedArc(
        networkArc: NetworkArc<Flt64>,
        upperInput: Double,
        yield: Double
    ): GeneralizedFlowArc<Flt64> {
        val capacity = assertNotNull(CapacityBounds(
            lower = Quantity(Flt64.zero, NoneUnit),
            upper = Quantity(Flt64(upperInput), NoneUnit)
        ).value)
        return assertNotNull(GeneralizedFlowArc(
            networkArc = networkArc,
            inputCapacity = capacity,
            productionYield = Quantity(Flt64(yield), NoneUnit)
        ).value)
    }
}
