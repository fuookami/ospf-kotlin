package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*

/** 用独立可行见证验证模型，不依赖原生求解器。 / Validate independent witnesses without native solvers. */
class FlowContextTest {
    @Test
    fun indexedBalancesMatchFullArcReferenceForMultigraphAndIsolatedNodes() = runBlocking {
        val arcs = listOf(
            arc("forward", "s", "a", 1.0),
            arc("parallel", "s", "a", 2.0),
            arc("loop", "a", "a", 3.0),
            arc("finish", "a", "t", 4.0),
            arc("reverse", "t", "s", 5.0)
        )
        val products = listOf(
            product("all", 2.0),
            CommodityFlow(
                commodity = product("restricted", 1.0).commodity,
                unit = NoneUnit,
                allowedArcs = setOf(NetworkArcId("forward"), NetworkArcId("finish"))
            )
        )
        val base = problem(arcs, products)
        val graph = assertNotNull(NetworkGraph(base.graph.nodes + node("isolated"), arcs).value)
        val problem = FlowProblem(graph = graph, arcs = base.arcs, commodities = products, zero = Flt64.zero)
        val context = FlowContext(problem, Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel(name = "indexed_balances")
        try {
            assertTrue(context.register(model).ok)
            val snapshot = assertNotNull(model.state.refresh().value)
            for (product in products) {
                for (node in graph.nodes) {
                    val expected = linkedMapOf<String, Flt64>()
                    for (arc in arcs) {
                        if (product.allowedArcs?.contains(arc.id) == false) continue
                        val variableName = "network_flow_${product.commodity.id}_${arc.id.value}"
                        var coefficient = Flt64.zero
                        if (arc.from == node.id) coefficient += Flt64.one
                        if (arc.to == node.id) coefficient -= Flt64.one
                        if (coefficient != Flt64.zero) expected[variableName] = coefficient
                    }
                    val row = snapshot.rows.single { it.sourceName == "network_flow_balance_${product.commodity.id}_${node.id.value}" }
                    assertEquals(expected, row.coefficients.entries.associate { it.key.name to it.value })
                    assertEquals(
                        product.commodity.supplyDemands.firstOrNull { it.nodeId == node.id }?.net?.value ?: Flt64.zero,
                        row.rhs
                    )
                }
            }
            model.setSolution(List(model.tokens.tokensInSolver.size) { Flt64.zero })
            val solution = assertNotNull(context.extractSolution(model).value)
            for (product in products) {
                for (node in graph.nodes) {
                    val expected = product.commodity.supplyDemands.firstOrNull { it.nodeId == node.id }?.net
                        ?: Quantity(Flt64.zero, product.unit)
                    assertEquals(expected, solution.satisfiedBalances.getValue(product.commodity.id to node.id))
                }
            }
            for ((key, amount) in solution.flows) {
                assertEquals(products.first { it.commodity.id == key.commodity }.unit, amount.unit)
                assertEquals(Flt64.zero, amount.value)
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun knownTransshipmentOptimumAndParallelServicesAreExtractedByIdentity() {
        val arcs = listOf(arc("sa", "s", "a", 10.0), arc("sb", "s", "b", 0.0),
            arc("ab", "a", "b", -5.0), arc("at", "a", "t", 10.0), arc("bt", "b", "t", 1.0),
            arc("slow", "b", "t", 20.0), arc("loop", "b", "b", 1.0))
        val problem = problem(arcs, listOf(product("goods", 4.0)), arcs.map {
            flowArc(it, if (it.id.value == "ab") 2.0 else 4.0, if (it.id.value == "ab") 1.0 else 0.0)
        })
        val context = FlowContext(problem, Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel(name = "transshipment")
        assertTrue(context.register(model).ok)
        val quantities = mapOf("sa" to 1.0, "sb" to 3.0, "ab" to 1.0, "at" to 0.0, "bt" to 4.0)
        val values = model.tokens.tokensInSolver.map { token ->
            quantities[token.variable.name.substringAfterLast('_')]?.let(::Flt64) ?: Flt64.zero
        }
        assertTrue(model.constraints.all { it.isTrue(values, model.tokens, false) == true })
        model.setSolution(values)
        val solution = assertNotNull(context.extractSolution(model).value)
        assertEquals(Flt64(9.0), solution.cost.value)
        assertEquals(Flt64.zero, solution.flows.getValue(FlowKey("goods", NetworkArcId("slow"))).value)
        assertEquals(Flt64.zero, solution.flows.getValue(FlowKey("goods", NetworkArcId("loop"))).value)
        val wrong = values.mapIndexed { index, value -> if (model.tokens.tokensInSolver[index].variable.name.endsWith("_ab")) Flt64.zero else value }
        assertFalse(model.constraints.all { it.isTrue(wrong, model.tokens, false) == true })
    }

    @Test
    fun immutableProblemCreatesIndependentModelStateAndRejectsContextReuse() {
        val problem = problem(listOf(arc("service", "s", "t", 2.0)), listOf(product("goods", 2.0)))
        val first = FlowContext(problem, Flt64NetworkSchedulingSolverValueAdapter)
        val second = FlowContext(problem, Flt64NetworkSchedulingSolverValueAdapter)
        val a = LinearMetaModel(name = "a")
        val b = LinearMetaModel(name = "b")
        assertTrue(first.register(a).ok)
        assertTrue(second.register(b).ok)
        assertNotSame(a.tokens.tokens.first().variable, b.tokens.tokens.first().variable)
        assertFalse(first.register(b).ok)
        assertTrue(first.register(a).ok)
    }

    @Test
    fun integerStepsScaleBothBalancesCostsAndSharedCapacity() {
        val arc = arc("service", "s", "t", 3.0)
        val product = product("lots", 4.0, Quantity(Flt64(2.0), NoneUnit))
        val context = FlowContext(problem(listOf(arc), listOf(product)), Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel(name = "integer_flow")
        assertTrue(context.register(model).ok)
        assertTrue(model.tokens.tokens.single().variable.type.isIntegerType)
        val witness = listOf(Flt64(2.0))
        assertTrue(model.constraints.all { it.isTrue(witness, model.tokens, false) == true })
        model.setSolution(witness)
        val solution = assertNotNull(context.extractSolution(model).value)
        assertEquals(Flt64(4.0), solution.flows.values.single().value)
        assertEquals(Flt64(12.0), solution.cost.value)
    }

    @Test
    fun heterogeneousCommodityUnitsRequireExplicitCapacityUsage() {
        val arc = arc("truck", "s", "t", 1.0)
        val mass = assertNotNull(FlowCommodity("mass", emptyList<SupplyDemand<Flt64>>()).value)
        val count = assertNotNull(FlowCommodity("pieces", emptyList<SupplyDemand<Flt64>>()).value)
        val graph = assertNotNull(NetworkGraph(listOf(node("s"), node("t")), listOf(arc)).value)
        val capacity = assertNotNull(CapacityBounds(Quantity(Flt64.zero, Kilogram), Quantity(Flt64(10.0), Kilogram)).value)
        val products = listOf(CommodityFlow(mass, Gram), CommodityFlow(count, NoneUnit))
        val invalid = FlowProblem(graph = graph, arcs = listOf(assertNotNull(FlowArc(arc, capacity).value)), commodities = products, zero = Flt64.zero)
        assertFalse(FlowContext(invalid, Flt64NetworkSchedulingSolverValueAdapter).register(LinearMetaModel(name = "invalid")).ok)
        val valid = FlowProblem(
            graph = graph,
            arcs = invalid.arcs,
            commodities = products,
            zero = Flt64.zero,
            capacityUsage = mapOf(FlowKey("pieces", arc.id) to Quantity(Flt64(2.0), Kilogram))
        )
        val context = FlowContext(valid, Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel(name = "heterogeneous")
        assertTrue(context.register(model).ok)
        val coefficients = assertNotNull(context.load("arc:truck").value).monomials.map { it.coefficient }.sortedBy { it.toDouble() }
        assertTrue(coefficients[0] eq Flt64(0.001))
        assertTrue(coefficients[1] eq Flt64(2.0))
    }

    @Test
    fun commodityServiceCostsAvailabilityAndCrossArcResourceAreJointlyEnforced() {
        val arcs = listOf(arc("road", "s", "t", 99.0), arc("rail", "s", "t", 99.0))
        val road = NetworkArcId("road")
        val rail = NetworkArcId("rail")
        val products = listOf(
            CommodityFlow(
                commodity = product("bulk", 2.0).commodity,
                unit = NoneUnit,
                costs = mapOf(road to q(2.0)),
                allowedArcs = setOf(road)
            ),
            CommodityFlow(
                commodity = product("parcel", 1.0).commodity,
                unit = NoneUnit,
                costs = mapOf(rail to q(4.0)),
                allowedArcs = setOf(rail)
            )
        )
        val base = problem(arcs, products)
        val context = FlowContext(
            problem = FlowProblem(
                graph = base.graph,
                arcs = base.arcs,
                commodities = products,
                zero = Flt64.zero,
                resources = listOf(FlowCapacity(
                    id = "shared_dispatch",
                    bounds = assertNotNull(CapacityBounds(q(0.0), q(3.0)).value),
                    usage = mapOf(FlowKey("bulk", road) to q(1.0), FlowKey("parcel", rail) to q(1.0))
                ))
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        )
        val model = LinearMetaModel(name = "multi_commodity")
        assertTrue(context.register(model).ok)
        assertEquals(2, model.tokens.tokensInSolver.size)
        val values = model.tokens.tokensInSolver.map { if ("_bulk_" in it.variable.name) Flt64(2.0) else Flt64.one }
        assertTrue(model.constraints.all { it.isTrue(values, model.tokens, false) == true })
        model.setSolution(values)
        val result = assertNotNull(context.extractSolution(model).value)
        assertEquals(Flt64(8.0), result.cost.value)
        assertEquals(Flt64(3.0), result.capacityUsage.getValue("shared_dispatch").value)
        assertEquals(setOf("bulk", "parcel"), result.commodityCosts.keys)
    }

    @Test
    fun explicitBoundedRelaxationIsNotSilentlyAddedToHardDemand() {
        val arc = arc("service", "s", "t", 1.0)
        val base = problem(listOf(arc), listOf(product("goods", 2.0)), listOf(flowArc(arc, 1.0)))
        val hard = FlowContext(base, Flt64NetworkSchedulingSolverValueAdapter)
        val hardModel = LinearMetaModel(name = "hard")
        assertTrue(hard.register(hardModel).ok)
        assertFalse(hardModel.constraints.all { it.isTrue(listOf(Flt64.one), hardModel.tokens, false) == true })
        val soft = FlowProblem(
            graph = base.graph,
            arcs = base.arcs,
            commodities = base.commodities,
            zero = Flt64.zero,
            slacks = listOf(
                FlowSlack("goods", NetworkNodeId("s"), q(1.0), q(0.0), q(10.0)),
                FlowSlack("goods", NetworkNodeId("t"), q(0.0), q(1.0), q(10.0))
            )
        )
        val context = FlowContext(soft, Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel(name = "soft")
        assertTrue(context.register(model).ok)
        val values = model.tokens.tokensInSolver.map { token ->
            when (token.variable.name) {
                "network_flow_goods_service", "network_flow_unused_goods_s", "network_flow_unmet_goods_t" -> Flt64.one
                else -> Flt64.zero
            }
        }
        assertTrue(model.constraints.all { it.isTrue(values, model.tokens, false) == true })
        model.setSolution(values)
        val solution = assertNotNull(context.extractSolution(model).value)
        assertEquals(Flt64(21.0), solution.cost.value)
        assertEquals(Flt64.one, solution.unmetDemand.getValue("goods" to NetworkNodeId("t")).value)
    }

    @Test
    fun relaxationCannotInventSupplyDemandOrExceedTheOriginalBalance() {
        val base = problem(listOf(arc("service", "s", "t", -1.0)), listOf(product("goods", 2.0)))
        val invalid = listOf(
            FlowSlack("goods", NetworkNodeId("s"), q(0.0), q(1.0), q(0.0)),
            FlowSlack("goods", NetworkNodeId("t"), q(1.0), q(0.0), q(0.0)),
            FlowSlack("goods", NetworkNodeId("s"), q(3.0), q(0.0), q(0.0))
        )
        for (slack in invalid) {
            val context = FlowContext(
                problem = FlowProblem(
                    graph = base.graph,
                    arcs = base.arcs,
                    commodities = base.commodities,
                    zero = Flt64.zero,
                    slacks = listOf(slack)
                ),
                adapter = Flt64NetworkSchedulingSolverValueAdapter
            )
            assertFalse(context.register(LinearMetaModel(name = "invalid_relaxation")).ok)
        }
    }

    @Test
    fun fltXAndFailedSolverConversionAreSupportedAtBoundary() {
        val a = assertNotNull(NetworkNode<FltX>(NetworkNodeId("s")).value)
        val b = assertNotNull(NetworkNode<FltX>(NetworkNodeId("t")).value)
        val arc = assertNotNull(NetworkArc(from = a.id, to = b.id, cost = Quantity(FltX("2"), NoneUnit)).value)
        val capacity = assertNotNull(CapacityBounds(Quantity(FltX("0"), NoneUnit), Quantity(FltX("3"), NoneUnit)).value)
        val commodity = assertNotNull(FlowCommodity("x", emptyList<SupplyDemand<FltX>>()).value)
        val problem = FlowProblem(
            graph = assertNotNull(NetworkGraph(listOf(a, b), listOf(arc)).value),
            arcs = listOf(assertNotNull(FlowArc(arc, capacity).value)),
            commodities = listOf(CommodityFlow(commodity, NoneUnit)),
            zero = FltX("0")
        )
        val context = FlowContext(problem, FltXNetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel(name = "fltx")
        assertTrue(context.register(model).ok)
        model.setSolution(listOf(Flt64.zero))
        assertEquals(FltX("0"), assertNotNull(context.extractSolution(model).value).cost.value)
        assertFalse(Flt64NetworkSchedulingSolverValueAdapter.toSolverValue(Flt64.infinity).ok)
    }

    private fun q(value: Double) = Quantity(Flt64(value), NoneUnit)
    private fun node(id: String) = assertNotNull(NetworkNode<Flt64>(NetworkNodeId(id)).value)
    private fun arc(id: String, from: String, to: String, cost: Double) = assertNotNull(NetworkArc(
        from = NetworkNodeId(from), to = NetworkNodeId(to), cost = q(cost), id = NetworkArcId(id)
    ).value)
    private fun flowArc(arc: NetworkArc<Flt64>, upper: Double, lower: Double = 0.0) = assertNotNull(FlowArc(arc, assertNotNull(CapacityBounds(q(lower), q(upper)).value)).value)
    private fun product(id: String, demand: Double, step: Quantity<Flt64>? = null): CommodityFlow<Flt64> {
        val commodity = assertNotNull(FlowCommodity(id, listOf(
            assertNotNull(SupplyDemand(NetworkNodeId("s"), q(demand)).value),
            assertNotNull(SupplyDemand(NetworkNodeId("t"), q(-demand)).value)
        )).value)
        return CommodityFlow(commodity = commodity, unit = NoneUnit, integerStep = step)
    }
    private fun problem(arcs: List<NetworkArc<Flt64>>, products: List<CommodityFlow<Flt64>>, capacities: List<FlowArc<Flt64>> = arcs.map { flowArc(it, 10.0) }): FlowProblem<Flt64> {
        val ids = (arcs.flatMap { listOf(it.from.value, it.to.value) } + listOf("s", "t")).distinct()
        return FlowProblem(
            graph = assertNotNull(NetworkGraph(ids.map(::node), arcs).value), arcs = capacities,
            commodities = products, zero = Flt64.zero
        )
    }
}
