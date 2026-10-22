package fuookami.ospf.kotlin.framework.network_scheduling.domain.allocation

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearInequalityConstraint
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 验证业务装配与小规模运输 oracle，不依赖原生求解器。 / Verify business assembly with small transport oracles, without native solvers. */
class AllocationContextTest {
    @Test
    fun integerRequestCanSplitAcrossDirectAndLayeredServicesWithinSharedQuota() {
        val problem = AllocationProblem(
            locations = listOf(
                AllocationLocation(NetworkNodeId("source"), layer = 0),
                AllocationLocation(NetworkNodeId("hub"), layer = 1),
                AllocationLocation(NetworkNodeId("target"), layer = 2)
            ),
            services = listOf(
                service("direct", "source", "target", 5.0, 10.0),
                service("inbound", "source", "hub", 1.0, 10.0),
                service("outbound", "hub", "target", 1.0, 10.0)
            ),
            requests = listOf(AllocationRequest(
                id = "order",
                origin = NetworkNodeId("source"),
                destination = NetworkNodeId("target"),
                quantity = q(4.0),
                integerStep = q(1.0)
            )),
            quotas = listOf(AllocationQuota(
                id = "hub_quota",
                bounds = bounds(0.0, 3.0),
                usage = mapOf(AllocationFlowKey("order", NetworkArcId("outbound")) to q(1.0))
            )),
            zero = Flt64.zero
        )
        val context = assertNotNull(AllocationContext.create(
            problem = problem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        ).value)
        val model = LinearMetaModel(name = "allocation_split")
        assertTrue(context.register(model).ok)

        val optimum = enumerate(model, context.design.flow.cost, 4)
        assertEquals(Flt64(11.0), optimum.first)
        model.setSolution(optimum.second)
        val result = assertNotNull(context.extractSolution(model).value)
        assertEquals(Flt64(11.0), result.totalCost.value)
        assertEquals(Flt64(3.0), result.shipments.getValue(AllocationFlowKey("order", NetworkArcId("outbound"))).value)
        assertEquals(Flt64.one, result.shipments.getValue(AllocationFlowKey("order", NetworkArcId("direct"))).value)
        assertEquals(Flt64(3.0), result.flow.capacityUsage.getValue("hub_quota").value)
        assertEquals(3, result.shipments.values.count { it.value gr Flt64.zero })
    }

    @Test
    fun optionalHubAndCapacityLevelControlServiceFlow() {
        val requestId = "order"
        val serviceId = NetworkArcId("road")
        val problem = AllocationProblem(
            locations = listOf(
                AllocationLocation(NetworkNodeId("source"), layer = 0),
                AllocationLocation(NetworkNodeId("store"), layer = 1)
            ),
            services = listOf(AllocationService(
                id = serviceId,
                from = NetworkNodeId("source"),
                to = NetworkNodeId("store"),
                cost = q(1.0),
                capacity = bounds(0.0, 5.0),
                capacityLevels = listOf(
                    AllocationCapacityLevel(id = "small", capacity = q(2.0), cost = q(1.0)),
                    AllocationCapacityLevel(id = "large", capacity = q(5.0), cost = q(5.0))
                )
            )),
            requests = listOf(AllocationRequest(
                id = requestId,
                origin = NetworkNodeId("source"),
                destination = NetworkNodeId("store"),
                quantity = q(3.0),
                integerStep = q(1.0)
            )),
            quotas = listOf(AllocationQuota(
                id = "processing",
                bounds = bounds(0.0, 3.0),
                usage = mapOf(AllocationFlowKey(requestId, serviceId) to q(1.0))
            )),
            hubs = listOf(AllocationHub(
                location = NetworkNodeId("store"),
                openingCost = q(2.0),
                quotaIds = setOf("processing")
            )),
            zero = Flt64.zero
        )
        val context = assertNotNull(AllocationContext.create(
            problem = problem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "allocation_design"
        ).value)
        val model = LinearMetaModel(name = "allocation_design")
        assertTrue(context.register(model).ok)

        val feasible = model.tokens.tokensInSolver.map { token ->
            when (token.variable.name) {
                "allocation_design_facility_store", "allocation_design_road_large" -> Flt64.one
                "allocation_design_nominal_order_road" -> Flt64(3.0)
                else -> Flt64.zero
            }
        }
        assertTrue(model.constraints.all { it.isTrue(feasible, model.tokens, false) == true })
        model.setSolution(feasible)
        val result = assertNotNull(context.extractSolution(model).value)
        assertEquals(setOf(NetworkNodeId("store")), result.openHubs)
        assertEquals(mapOf(serviceId to "large"), result.serviceLevels)
        assertEquals(Flt64(7.0), result.constructionCost.value)

        val closed = model.tokens.tokensInSolver.map { token ->
            when (token.variable.name) {
                "allocation_design_road_large" -> Flt64.one
                "allocation_design_nominal_order_road" -> Flt64(3.0)
                else -> Flt64.zero
            }
        }
        assertFalse(model.constraints.all { it.isTrue(closed, model.tokens, false) == true })

        val overQuota = model.tokens.tokensInSolver.map { token ->
            when (token.variable.name) {
                "allocation_design_facility_store", "allocation_design_road_large" -> Flt64.one
                "allocation_design_nominal_order_road" -> Flt64(4.0)
                else -> Flt64.zero
            }
        }
        val quotaConstraint = model.constraints.filterIsInstance<LinearInequalityConstraint<Flt64>>()
            .single { it.name == "allocation_design_nominal_processing_facility_upper" }
        assertFalse(quotaConstraint.isTrue(overQuota, model.tokens, false) == true)
    }

    @Test
    fun invalidUnitsAndDownwardLayerServicesReturnErrors() {
        val locations = listOf(
            AllocationLocation<Flt64>(NetworkNodeId("upper"), layer = 2),
            AllocationLocation<Flt64>(NetworkNodeId("lower"), layer = 1)
        )
        val reverseService = AllocationService(
            id = NetworkArcId("reverse"),
            from = NetworkNodeId("upper"),
            to = NetworkNodeId("lower"),
            cost = q(1.0),
            capacity = bounds(0.0, 10.0)
        )
        val wrongLayerProblem = AllocationProblem(
            locations = locations,
            services = listOf(reverseService),
            requests = emptyList(),
            zero = Flt64.zero
        )
        assertFalse(AllocationContext.create(
            problem = wrongLayerProblem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        ).ok)

        val unitMismatchProblem = AllocationProblem(
            locations = listOf(
                AllocationLocation<Flt64>(NetworkNodeId("source")),
                AllocationLocation<Flt64>(NetworkNodeId("target"))
            ),
            services = listOf(service("road", "source", "target", 1.0, 10.0)),
            requests = listOf(AllocationRequest(
                id = "mass",
                origin = NetworkNodeId("source"),
                destination = NetworkNodeId("target"),
                quantity = Quantity(Flt64.one, Kilogram),
                unit = NoneUnit
            )),
            zero = Flt64.zero
        )
        assertFalse(AllocationContext.create(
            problem = unitMismatchProblem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        ).ok)
    }

    @Test
    fun heterogeneousCapacityNeedsExplicitUsageAndInfiniteUpperBoundsAreRejected() {
        val source = NetworkNodeId("source")
        val target = NetworkNodeId("target")
        val serviceId = NetworkArcId("truck")
        val locations = listOf(AllocationLocation<Flt64>(source), AllocationLocation<Flt64>(target))
        val services = listOf(AllocationService(
            id = serviceId,
            from = source,
            to = target,
            cost = q(1.0),
            capacity = bounds(0.0, 4.0, Kilogram)
        ))
        val requests = listOf(AllocationRequest(
            id = "pieces",
            origin = source,
            destination = target,
            quantity = q(2.0)
        ))
        val missingUsage = AllocationProblem(
            locations = locations,
            services = services,
            requests = requests,
            zero = Flt64.zero
        )
        val missingUsageContext = assertNotNull(AllocationContext.create(
            problem = missingUsage,
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        ).value)
        assertFalse(missingUsageContext.register(LinearMetaModel(name = "missing_usage")).ok)

        val explicitUsage = AllocationProblem(
            locations = locations,
            services = services,
            requests = requests,
            zero = Flt64.zero,
            serviceUsage = mapOf(AllocationFlowKey("pieces", serviceId) to q(2.0, Kilogram))
        )
        val explicitContext = assertNotNull(AllocationContext.create(
            problem = explicitUsage,
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        ).value)
        val explicitModel = LinearMetaModel(name = "explicit_usage")
        assertTrue(explicitContext.register(explicitModel).ok)
        explicitModel.setSolution(listOf(Flt64(2.0)))
        assertEquals(Flt64(4.0), assertNotNull(explicitContext.extractSolution(explicitModel).value)
            .flow.capacityUsage.getValue("arc:truck").value)

        val infiniteService = AllocationService(
            id = serviceId,
            from = source,
            to = target,
            cost = q(1.0),
            capacity = assertNotNull(CapacityBounds(
                lower = q(0.0),
                upper = Quantity(Flt64.infinity, NoneUnit)
            ).value)
        )
        val infiniteProblem = AllocationProblem(
            locations = locations,
            services = listOf(infiniteService),
            requests = requests,
            zero = Flt64.zero
        )
        val infiniteContext = assertNotNull(AllocationContext.create(
            problem = infiniteProblem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        ).value)
        assertFalse(infiniteContext.register(LinearMetaModel(name = "infinite_capacity")).ok)
    }

    @Test
    fun fltXValuesArePreservedAndContextsOwnIndependentRegistrationState() {
        val source = NetworkNodeId("source")
        val target = NetworkNodeId("target")
        val problem = AllocationProblem(
            locations = listOf(AllocationLocation<FltX>(source), AllocationLocation<FltX>(target)),
            services = listOf(AllocationService(
                id = NetworkArcId("road"),
                from = source,
                to = target,
                cost = qx(2.0),
                capacity = boundsX(0.0, 3.0)
            )),
            requests = listOf(AllocationRequest(
                id = "shipment",
                origin = source,
                destination = target,
                quantity = qx(1.0)
            )),
            zero = FltX.zero
        )
        val first = assertNotNull(AllocationContext.create(
            problem = problem,
            adapter = FltXNetworkSchedulingSolverValueAdapter,
            name = "first_allocation"
        ).value)
        val second = assertNotNull(AllocationContext.create(
            problem = problem,
            adapter = FltXNetworkSchedulingSolverValueAdapter,
            name = "second_allocation"
        ).value)
        val firstModel = LinearMetaModel(name = "first")
        val secondModel = LinearMetaModel(name = "second")
        assertTrue(first.register(firstModel).ok)
        assertTrue(second.register(secondModel).ok)
        assertNotSame(firstModel.tokens.tokens.single().variable, secondModel.tokens.tokens.single().variable)
        assertFalse(first.register(secondModel).ok)

        firstModel.setSolution(listOf(Flt64.one))
        val result = assertNotNull(first.extractSolution(firstModel).value)
        assertEquals(FltX(1.0), result.shipments.values.single().value)
        assertEquals(FltX(2.0), result.totalCost.value)
    }

    private fun service(id: String, from: String, to: String, cost: Double, capacity: Double): AllocationService<Flt64> {
        return AllocationService(
            id = NetworkArcId(id),
            from = NetworkNodeId(from),
            to = NetworkNodeId(to),
            cost = q(cost),
            capacity = bounds(0.0, capacity)
        )
    }

    private fun q(value: Double, unit: PhysicalUnit = NoneUnit) = Quantity(Flt64(value), unit)
    private fun qx(value: Double, unit: PhysicalUnit = NoneUnit) = Quantity(FltX(value), unit)
    private fun bounds(lower: Double, upper: Double, unit: PhysicalUnit = NoneUnit) =
        assertNotNull(CapacityBounds(q(lower, unit), q(upper, unit)).value)
    private fun boundsX(lower: Double, upper: Double, unit: PhysicalUnit = NoneUnit) =
        assertNotNull(CapacityBounds(qx(lower, unit), qx(upper, unit)).value)

    private fun enumerate(
        model: LinearMetaModel<Flt64>,
        objective: LinearPolynomial<Flt64>,
        upperBound: Int
    ): Pair<Flt64, List<Flt64>> {
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
                val value = evaluate(objective)
                if (value ls best) {
                    best = value
                    witness = values.toList()
                }
                return
            }
            for (value in 0..upperBound) {
                values[index] = Flt64(value.toDouble())
                visit(index + 1)
            }
        }
        visit(0)
        return best to assertNotNull(witness)
    }
}
