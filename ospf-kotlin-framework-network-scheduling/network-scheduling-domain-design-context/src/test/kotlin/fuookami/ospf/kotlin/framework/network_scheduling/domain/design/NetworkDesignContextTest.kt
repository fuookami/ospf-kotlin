package fuookami.ospf.kotlin.framework.network_scheduling.domain.design

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Gram
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearInequalityConstraint
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*

/** 对小模型穷举变量域，与手算设施/扩容最优值对照。 / Enumerate small models against hand-derived facility/expansion optima. */
class NetworkDesignContextTest {
    @Test
    fun facilitiesParallelServicesAndCapacityLevelsMatchEnumerationOracle() {
        val context = NetworkDesignContext(fixture(), Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel(name = "design_oracle")
        assertTrue(context.register(model).ok)
        val solution = enumerate(model, context.constructionCost, context.flow.cost)
        assertEquals(Flt64(10.0), solution.first)
        model.setSolution(solution.second)
        val result = assertNotNull(context.extractSolution(model).value)
        assertEquals(setOf(NetworkNodeId("t")), result.openFacilities)
        assertEquals(mapOf(NetworkArcId("road") to "large"), result.serviceLevels)
        assertEquals(Flt64(8.0), result.constructionCost.value)
        assertEquals(Flt64(2.0), result.flow.cost.value)
    }

    @Test
    fun finiteConstructionBudgetChangesTheOptimalParallelService() {
        val context = NetworkDesignContext(fixture(q(3.0)), Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel(name = "budget_oracle")
        assertTrue(context.register(model).ok)
        val solution = enumerate(model, context.constructionCost, context.flow.cost)
        assertEquals(Flt64(11.0), solution.first)
        model.setSolution(solution.second)
        assertEquals(mapOf(NetworkArcId("air") to "only"), assertNotNull(context.extractSolution(model).value).serviceLevels)
    }

    @Test
    fun closedFacilityCannotCarryFlowAndProcessingCapacityIsShared() {
        val context = NetworkDesignContext(fixture(), Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel(name = "closed")
        assertTrue(context.register(model).ok)
        val closed = model.tokens.tokensInSolver.map { token ->
            when (token.variable.name) {
                "network_design_road_large" -> Flt64.one
                "network_design_nominal_goods_road" -> Flt64(2.0)
                else -> Flt64.zero
            }
        }
        assertFalse(model.constraints.all { it.isTrue(closed, model.tokens, false) == true })
        val overloaded = model.tokens.tokensInSolver.map { token ->
            when (token.variable.name) {
                "network_design_facility_t", "network_design_road_large" -> Flt64.one
                "network_design_nominal_goods_road" -> Flt64(3.0)
                else -> Flt64.zero
            }
        }
        assertFalse(model.constraints.all { it.isTrue(overloaded, model.tokens, false) == true })
    }

    @Test
    fun invalidScenarioAttachmentAndUndeclaredCapacityEnvelopeAreRejected() {
        val base = fixture()
        val context = NetworkDesignContext(base, Flt64NetworkSchedulingSolverValueAdapter)
        assertFalse(context.attachFlow(LinearMetaModel(name = "premature"), context.flow).ok)
        val invalid = NetworkDesignProblem(
            flow = base.flow,
            services = listOf(NetworkService(NetworkArcId("road"), listOf(CapacityLevel("too_large", q(4.0), q(1.0)))))
        )
        assertFalse(NetworkDesignContext(invalid, Flt64NetworkSchedulingSolverValueAdapter).register(LinearMetaModel(name = "invalid")).ok)
    }

    @Test
    fun zeroCapacityConsumptionCannotBypassServiceOrFacilityClosure() {
        val base = fixture()
        val flow = FlowProblem(
            graph = base.flow.graph,
            arcs = base.flow.arcs,
            commodities = base.flow.commodities,
            zero = Flt64.zero,
            capacityUsage = mapOf(FlowKey("goods", NetworkArcId("road")) to q(0.0)),
            resources = base.flow.resources
        )
        val context = NetworkDesignContext(
            problem = NetworkDesignProblem(
                flow = flow,
                services = base.services,
                facilities = base.facilities
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        )
        assertFalse(context.register(LinearMetaModel(name = "zero_usage")).ok)
    }

    @Test
    fun scenarioCapacityInGramsScalesToNominalKilogramsAndRespectsDesignLimits() {
        val source = NetworkNodeId("s")
        val target = NetworkNodeId("t")
        val service = NetworkArcId("route")
        val arc = assertNotNull(NetworkArc(
            from = source,
            to = target,
            cost = q(0.0),
            id = service
        ).value)
        val graph = assertNotNull(NetworkGraph(
            nodes = listOf(source, target).map { assertNotNull(NetworkNode<Flt64>(it).value) },
            arcs = listOf(arc)
        ).value)
        val key = FlowKey("goods", service)
        val processing = FlowCapacity(
            id = "processing",
            bounds = assertNotNull(CapacityBounds(
                lower = qKg(0.0),
                upper = qKg(0.8)
            ).value),
            usage = mapOf(key to qKg(0.001))
        )
        val nominalCommodity = assertNotNull(FlowCommodity<Flt64>(
            id = "goods"
        ).value)
        val nominalFlow = FlowProblem(
            graph = graph,
            arcs = listOf(assertNotNull(FlowArc(
                networkArc = arc,
                capacity = assertNotNull(CapacityBounds(
                    lower = qKg(0.0),
                    upper = qKg(5.0)
                ).value)
            ).value)),
            commodities = listOf(CommodityFlow(
                commodity = nominalCommodity,
                unit = Gram
            )),
            zero = Flt64.zero,
            resources = listOf(processing)
        )
        val design = NetworkDesignContext(
            problem = NetworkDesignProblem(
                flow = nominalFlow,
                services = listOf(NetworkService(
                    arc = service,
                    levels = listOf(CapacityLevel(
                        id = "small",
                        capacity = qKg(1.0),
                        cost = q(0.0)
                    )),
                    required = true
                )),
                facilities = listOf(NetworkFacility(
                    node = target,
                    cost = q(0.0),
                    required = true,
                    capacityRows = setOf("processing")
                ))
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        )
        val model = LinearMetaModel(name = "mixed_capacity_units")
        assertTrue(design.register(model).ok)

        val scenarioCommodity = assertNotNull(FlowCommodity(
            id = "goods",
            supplyDemands = listOf(
                assertNotNull(SupplyDemand(source, qGram(900.0)).value),
                assertNotNull(SupplyDemand(target, qGram(-900.0)).value)
            )
        ).value)
        val scenarioFlow = FlowContext(
            problem = FlowProblem(
                graph = graph,
                arcs = listOf(assertNotNull(FlowArc(
                    networkArc = arc,
                    capacity = assertNotNull(CapacityBounds(
                        lower = qGram(0.0),
                        upper = qGram(2000.0)
                    ).value)
                ).value)),
                commodities = listOf(CommodityFlow(
                    commodity = scenarioCommodity,
                    unit = Gram
                )),
                zero = Flt64.zero,
                resources = listOf(processing)
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "scenario"
        )
        assertTrue(design.attachFlow(model, scenarioFlow).ok)

        fun witness(flowGrams: Double): List<Flt64> {
            return model.tokens.tokensInSolver.map { token ->
                when (token.variable.name) {
                    "network_design_route_small", "network_design_facility_t" -> Flt64.one
                    "scenario_goods_route" -> Flt64(flowGrams)
                    else -> Flt64.zero
                }
            }
        }
        fun isTrue(name: String, values: List<Flt64>): Boolean? {
            return model.constraints.filterIsInstance<LinearInequalityConstraint<Flt64>>().single { it.name == name }.isTrue(
                solution = values,
                tokenTable = model.tokens,
                zeroIfNone = false
            )
        }

        val belowLevelAboveFacility = witness(flowGrams = 900.0)
        assertEquals(true, isTrue("scenario_route_design_upper", belowLevelAboveFacility))
        assertEquals(true, isTrue("scenario_route_available_capacity", belowLevelAboveFacility))
        assertEquals(true, isTrue("scenario_route_t_facility", belowLevelAboveFacility))
        assertEquals(false, isTrue("scenario_processing_facility_upper", belowLevelAboveFacility))

        val aboveSelectedLevel = witness(flowGrams = 1100.0)
        assertEquals(false, isTrue("scenario_route_design_upper", aboveSelectedLevel))
    }

    private fun qKg(value: Double) = Quantity(Flt64(value), Kilogram)
    private fun qGram(value: Double) = Quantity(Flt64(value), Gram)

    private fun fixture(budget: Quantity<Flt64>? = null): NetworkDesignProblem<Flt64> {
        val nodes = listOf("s", "t").map { assertNotNull(NetworkNode<Flt64>(NetworkNodeId(it)).value) }
        val arcs = listOf("road" to 1.0, "air" to 4.0).map { (id, cost) -> assertNotNull(NetworkArc(
            from = nodes[0].id, to = nodes[1].id, cost = q(cost), id = NetworkArcId(id)
        ).value) }
        val commodity = assertNotNull(FlowCommodity("goods", listOf(
            assertNotNull(SupplyDemand(nodes[0].id, q(2.0)).value), assertNotNull(SupplyDemand(nodes[1].id, q(-2.0)).value)
        )).value)
        val flow = FlowProblem(
            graph = assertNotNull(NetworkGraph(nodes, arcs).value),
            arcs = arcs.map { assertNotNull(FlowArc(it, assertNotNull(CapacityBounds(q(0.0), q(3.0)).value)).value) },
            commodities = listOf(CommodityFlow(commodity = commodity, unit = NoneUnit, integerStep = q(1.0))),
            zero = Flt64.zero,
            resources = listOf(FlowCapacity("processing", assertNotNull(CapacityBounds(q(0.0), q(2.0)).value), arcs.associate { FlowKey("goods", it.id) to q(1.0) }))
        )
        return NetworkDesignProblem(
            flow = flow,
            services = listOf(
                NetworkService(NetworkArcId("road"), listOf(CapacityLevel("small", q(1.0), q(2.0)), CapacityLevel("large", q(3.0), q(6.0)))),
                NetworkService(NetworkArcId("air"), listOf(CapacityLevel("only", q(3.0), q(1.0))))
            ),
            facilities = listOf(NetworkFacility(node = nodes[1].id, cost = q(2.0), capacityRows = setOf("processing"))),
            budget = budget
        )
    }

    private fun q(value: Double) = Quantity(Flt64(value), NoneUnit)

    private fun enumerate(model: LinearMetaModel<Flt64>, construction: LinearPolynomial<Flt64>, operating: LinearPolynomial<Flt64>): Pair<Flt64, List<Flt64>> {
        val tokens = model.tokens.tokensInSolver
        var best = Flt64.infinity
        var witness: List<Flt64>? = null
        val values = MutableList(tokens.size) { Flt64.zero }
        fun evaluate(expression: LinearPolynomial<Flt64>): Flt64 {
            return expression.monomials.fold(expression.constant) { sum, term ->
                sum + term.coefficient * values[tokens.indexOfFirst { it.variable == term.symbol }]
            }
        }
        fun visit(index: Int) {
            if (index == values.size) {
                if (model.constraints.any { it.isTrue(values, model.tokens, false) != true }) return
                val objective = evaluate(construction) + evaluate(operating)
                if (objective ls best) { best = objective; witness = values.toList() }
                return
            }
            val upper = if (tokens[index].variable.name.contains("_nominal_")) 3 else 1
            for (value in 0..upper) { values[index] = Flt64(value.toDouble()); visit(index + 1) }
        }
        visit(0)
        return best to assertNotNull(witness)
    }
}
