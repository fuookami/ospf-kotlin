package fuookami.ospf.kotlin.framework.network_scheduling.domain.structure

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearInequalityConstraint
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*

/** 对小结构模型穷举二元决策与有界深度，验证树、覆盖和建设目标。 / Enumerate small structure models to verify trees, coverage and construction objectives. */
class NetworkStructureContextTest {
    @Test
    fun minimumConstructionCostSelectsCheaperSteinerTree() {
        val problem = problem(
            nodeIds = listOf("root", "relay", "terminal"),
            arcs = listOf(
                ArcSpec("direct", "root", "terminal"),
                ArcSpec("to_relay", "root", "relay"),
                ArcSpec("from_relay", "relay", "terminal")
            ),
            root = "root",
            terminals = setOf("terminal"),
            steinerNodes = setOf("relay"),
            services = listOf(service("direct", cost = 8.0)),
            facilities = listOf(facility("relay", cost = 2.0))
        )
        val context = NetworkStructureContext(
            problem = problem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "steiner_cost"
        )
        val model = LinearMetaModel(name = "steiner_cost")

        assertTrue(context.register(model).ok)
        assertTrue(model.hasFlattenedObjectives)
        assertTrue(context.register(model, minimizeCost = true).ok)
        assertFalse(context.register(model, minimizeCost = false).ok)

        val witnesses = enumerate(model, maxDepth = 2)
        val scored = witnesses.map { witness -> evaluate(context.constructionCost, witness, model) to witness }
        val bestCost = assertNotNull(scored.minOfOrNull { it.first })
        assertEquals(Flt64(2.0), bestCost)
        val best = scored.filter { it.first == bestCost }.map { it.second }
        assertTrue(best.all { value("steiner_cost_design_direct_built", it, model) == Flt64.zero })
        assertTrue(best.all { value("steiner_cost_tree_node_relay", it, model) == Flt64.one })
        assertTrue(best.all { value("steiner_cost_design_facility_relay", it, model) == Flt64.one })

        model.setSolution(best.first())
        val solution = assertNotNull(context.extractSolution(model).value)
        assertEquals(setOf(NetworkNodeId("relay")), solution.construction.openFacilities)
        assertEquals(Flt64(2.0), solution.construction.cost.value)
        assertTrue(NetworkArcId("to_relay") in solution.selectedTreeArcs)
        assertTrue(NetworkArcId("from_relay") in solution.selectedTreeArcs)
        assertFalse(NetworkArcId("direct") in solution.selectedTreeArcs)
    }

    @Test
    fun depthOrderRejectsAZeroCostDisconnectedCycleExplicitly() {
        val problem = problem(
            nodeIds = listOf("root", "terminal", "a", "b"),
            arcs = listOf(
                ArcSpec("root_terminal", "root", "terminal"),
                ArcSpec("a_b", "a", "b"),
                ArcSpec("b_a", "b", "a")
            ),
            root = "root",
            terminals = setOf("terminal"),
            steinerNodes = setOf("a", "b")
        )
        val context = NetworkStructureContext(
            problem = problem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "cycle"
        )
        val model = LinearMetaModel(name = "cycle")
        assertTrue(context.register(model, minimizeCost = false).ok)

        val cycle = witness(model) { name ->
            when {
                name.endsWith("_tree_arc_root_terminal") -> Flt64.one
                name.endsWith("_tree_arc_a_b") || name.endsWith("_tree_arc_b_a") -> Flt64.one
                name.endsWith("_tree_node_a") || name.endsWith("_tree_node_b") -> Flt64.one
                name.endsWith("_tree_depth_terminal") || name.endsWith("_tree_depth_a") || name.endsWith("_tree_depth_b") -> Flt64.one
                else -> Flt64.zero
            }
        }
        assertFalse(constraintsHold(model, cycle))
        val orderRows = model.constraints.filterIsInstance<LinearInequalityConstraint<Flt64>>()
        assertEquals(false, orderRows.single { it.name.endsWith("tree_order_a_b") }.isTrue(cycle, model.tokens, false))
        assertEquals(false, orderRows.single { it.name.endsWith("tree_order_b_a") }.isTrue(cycle, model.tokens, false))
    }

    @Test
    fun steinerRelayIsRequiredAndWrongWayReachabilityIsModelInfeasibility() {
        val connected = problem(
            nodeIds = listOf("root", "relay", "terminal"),
            arcs = listOf(ArcSpec("to_relay", "root", "relay"), ArcSpec("to_terminal", "relay", "terminal")),
            root = "root",
            terminals = setOf("terminal"),
            steinerNodes = setOf("relay")
        )
        val context = NetworkStructureContext(
            problem = connected,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "relay_path"
        )
        val model = LinearMetaModel(name = "relay_path")
        assertTrue(context.register(model, minimizeCost = false).ok)
        val connectedWitness = assertNotNull(enumerate(model, maxDepth = 2).firstOrNull())
        model.setSolution(connectedWitness)
        val solution = assertNotNull(context.extractSolution(model).value)
        assertEquals(setOf(NetworkNodeId("root"), NetworkNodeId("relay"), NetworkNodeId("terminal")), solution.connectedNodes)
        assertEquals(NetworkArcId("to_relay"), solution.parentArcs[NetworkNodeId("relay")])
        assertEquals(NetworkArcId("to_terminal"), solution.parentArcs[NetworkNodeId("terminal")])

        val disconnected = problem(
            nodeIds = listOf("root", "terminal"),
            arcs = listOf(ArcSpec("backward_only", "terminal", "root")),
            root = "root",
            terminals = setOf("terminal")
        )
        val disconnectedContext = NetworkStructureContext(
            problem = disconnected,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "disconnected"
        )
        val disconnectedModel = LinearMetaModel(name = "disconnected")
        assertTrue(disconnectedContext.register(disconnectedModel, minimizeCost = false).ok)
        assertTrue(enumerate(disconnectedModel, maxDepth = 1).isEmpty())
    }

    @Test
    fun parallelArcsRemainIndependentAndSelfLoopsAreFixedOff() {
        val problem = problem(
            nodeIds = listOf("root", "terminal"),
            arcs = listOf(
                ArcSpec("parallel_a", "root", "terminal"),
                ArcSpec("parallel_b", "root", "terminal"),
                ArcSpec("self", "terminal", "terminal")
            ),
            root = "root",
            terminals = setOf("terminal")
        )
        val context = NetworkStructureContext(
            problem = problem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "parallel"
        )
        val model = LinearMetaModel(name = "parallel")
        assertTrue(context.register(model, minimizeCost = false).ok)
        assertEquals(Flt64.zero, evaluate(assertNotNull(context.selectedTreeArc(NetworkArcId("self")).value), List(model.tokens.tokensInSolver.size) { Flt64.zero }, model))
        assertEquals(0, model.tokens.tokensInSolver.count { it.variable.name.endsWith("_tree_arc_self") })

        val witnesses = enumerate(model, maxDepth = 1)
        assertEquals(2, witnesses.size)
        for (solution in witnesses) {
            val selected = setOf("parallel_a", "parallel_b").filter { id -> value("parallel_tree_arc_$id", solution, model) == Flt64.one }.toSet()
            assertEquals(1, selected.size)
            model.setSolution(solution)
            val result = assertNotNull(context.extractSolution(model).value)
            assertEquals(setOf(NetworkArcId(selected.single())), result.selectedTreeArcs)
            assertEquals(NetworkArcId(selected.single()), result.parentArcs[NetworkNodeId("terminal")])
        }
    }

    @Test
    fun coverageHonorsEligibilityFacilityActivationAndExactQuota() {
        val problem = problem(
            nodeIds = listOf("root", "terminal", "f1", "f2", "outsider"),
            arcs = listOf(ArcSpec("route", "root", "terminal")),
            root = "root",
            terminals = setOf("terminal"),
            facilities = listOf(facility("f1", 0.0), facility("f2", 0.0)),
            partitions = listOf(
                FacilityPartitionRequest("north", setOf(NetworkNodeId("f1"), NetworkNodeId("f2"))),
                FacilityPartitionRequest("south", setOf(NetworkNodeId("f1"), NetworkNodeId("f2")))
            ),
            quotas = listOf(FacilityAssignmentQuota(
                facility = NetworkNodeId("f1"),
                minimum = 1,
                maximum = 1
            ))
        )
        val context = NetworkStructureContext(
            problem = problem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "coverage"
        )
        val model = LinearMetaModel(name = "coverage")
        assertTrue(context.register(model, minimizeCost = false).ok)
        assertFalse(context.partitionAssignment("north", NetworkNodeId("outsider")).ok)

        val witnesses = enumerate(model, maxDepth = 1)
        assertTrue(witnesses.isNotEmpty())
        for (solution in witnesses) {
            val f1Assignments = listOf("north", "south").count { partition ->
                evaluate(assertNotNull(context.partitionAssignment(partition, NetworkNodeId("f1")).value), solution, model) == Flt64.one
            }
            assertEquals(1, f1Assignments)
            assertEquals(Flt64.one, value("coverage_design_facility_f1", solution, model))
            assertEquals(Flt64.one, value("coverage_design_facility_f2", solution, model))
        }

        val selected = witnesses.first()
        val ownerToClosedFacility = selected.toMutableList()
        val f1 = model.tokens.tokensInSolver.indexOfFirst { it.variable.name == "coverage_design_facility_f1" }
        assertTrue(f1 >= 0)
        ownerToClosedFacility[f1] = Flt64.zero
        assertFalse(constraintsHold(model, ownerToClosedFacility))
        model.setSolution(selected)
        val owners = assertNotNull(context.extractSolution(model).value).partitionOwners
        assertEquals(setOf("north", "south"), owners.keys)
        assertEquals(1, owners.values.count { it == NetworkNodeId("f1") })
    }

    @Test
    fun optionalCoverageMayBeUnassignedButRequiredCoverageCanRemainInfeasible() {
        fun fixture(required: Boolean): NetworkStructureProblem<Flt64> = problem(
            nodeIds = listOf("root", "terminal", "facility"),
            arcs = listOf(ArcSpec("route", "root", "terminal")),
            root = "root",
            terminals = setOf("terminal"),
            facilities = listOf(facility("facility", 1.0)),
            partitions = listOf(FacilityPartitionRequest(
                id = "zone",
                eligibleFacilities = setOf(NetworkNodeId("facility")),
                required = required
            )),
            budget = q(0.0)
        )

        val optional = NetworkStructureContext(
            problem = fixture(required = false),
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "optional_coverage"
        )
        val optionalModel = LinearMetaModel(name = "optional_coverage")
        assertTrue(optional.register(optionalModel).ok)
        val optionalWitness = assertNotNull(enumerate(optionalModel, maxDepth = 1).minByOrNull { evaluate(optional.constructionCost, it, optionalModel) })
        assertEquals(Flt64.zero, evaluate(optional.constructionCost, optionalWitness, optionalModel))
        assertEquals(Flt64.zero, value("optional_coverage_partition_zone_facility_facility", optionalWitness, optionalModel))
        optionalModel.setSolution(optionalWitness)
        assertTrue(assertNotNull(optional.extractSolution(optionalModel).value).partitionOwners.isEmpty())

        val required = NetworkStructureContext(
            problem = fixture(required = true),
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "required_coverage"
        )
        val requiredModel = LinearMetaModel(name = "required_coverage")
        assertTrue(required.register(requiredModel, minimizeCost = false).ok)
        assertTrue(enumerate(requiredModel, maxDepth = 1).isEmpty())
    }

    @Test
    fun emptyEligibilityAndOverlargeMinimumQuotaRegisterAsInfeasibleModels() {
        val emptyEligibility = problem(
            nodeIds = listOf("root", "terminal"),
            arcs = listOf(ArcSpec("route", "root", "terminal")),
            root = "root",
            terminals = setOf("terminal"),
            partitions = listOf(FacilityPartitionRequest("empty", emptySet()))
        )
        val emptyContext = NetworkStructureContext(
            problem = emptyEligibility,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "empty_eligibility"
        )
        val emptyModel = LinearMetaModel(name = "empty_eligibility")
        assertTrue(emptyContext.register(emptyModel, minimizeCost = false).ok)
        assertTrue(enumerate(emptyModel, maxDepth = 1).isEmpty())

        val excessiveMinimum = problem(
            nodeIds = listOf("root", "terminal", "facility"),
            arcs = listOf(ArcSpec("route", "root", "terminal")),
            root = "root",
            terminals = setOf("terminal"),
            facilities = listOf(facility("facility", 0.0)),
            partitions = listOf(FacilityPartitionRequest("not_eligible", emptySet(), required = false)),
            quotas = listOf(FacilityAssignmentQuota(NetworkNodeId("facility"), minimum = 1))
        )
        val quotaContext = NetworkStructureContext(
            problem = excessiveMinimum,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "excessive_quota"
        )
        val quotaModel = LinearMetaModel(name = "excessive_quota")
        assertTrue(quotaContext.register(quotaModel, minimizeCost = false).ok)
        assertTrue(enumerate(quotaModel, maxDepth = 1).isEmpty())
    }

    @Test
    fun unknownReferencesDuplicateCoverageAndInvalidQuotasStillFailRegistration() {
        val known = problem(
            nodeIds = listOf("root", "terminal", "facility"),
            arcs = listOf(ArcSpec("route", "root", "terminal")),
            root = "root",
            terminals = setOf("terminal"),
            facilities = listOf(facility("facility", 0.0))
        )
        val unknownRoot = NetworkStructureProblem(
            design = known.design,
            root = NetworkNodeId("missing"),
            terminals = setOf(NetworkNodeId("terminal"))
        )
        assertFalse(NetworkStructureContext(unknownRoot, Flt64NetworkSchedulingSolverValueAdapter).register(
            LinearMetaModel(name = "unknown_root"), minimizeCost = false
        ).ok)

        val duplicatePartition = NetworkStructureProblem(
            design = known.design,
            root = NetworkNodeId("root"),
            terminals = setOf(NetworkNodeId("terminal")),
            partitions = listOf(
                FacilityPartitionRequest("duplicate", setOf(NetworkNodeId("facility"))),
                FacilityPartitionRequest("duplicate", setOf(NetworkNodeId("facility")))
            )
        )
        assertFalse(NetworkStructureContext(duplicatePartition, Flt64NetworkSchedulingSolverValueAdapter).register(
            LinearMetaModel(name = "duplicate_partition"), minimizeCost = false
        ).ok)

        val unknownFacility = NetworkStructureProblem(
            design = known.design,
            root = NetworkNodeId("root"),
            terminals = setOf(NetworkNodeId("terminal")),
            partitions = listOf(FacilityPartitionRequest("unknown_facility", setOf(NetworkNodeId("missing"))))
        )
        assertFalse(NetworkStructureContext(unknownFacility, Flt64NetworkSchedulingSolverValueAdapter).register(
            LinearMetaModel(name = "unknown_facility"), minimizeCost = false
        ).ok)

        val duplicateQuota = problem(
            nodeIds = listOf("root", "terminal", "facility"),
            arcs = listOf(ArcSpec("route", "root", "terminal")),
            root = "root",
            terminals = setOf("terminal"),
            facilities = listOf(facility("facility", 0.0)),
            quotas = listOf(
                FacilityAssignmentQuota(NetworkNodeId("facility"), maximum = 1),
                FacilityAssignmentQuota(NetworkNodeId("facility"), maximum = 2)
            )
        )
        assertFalse(NetworkStructureContext(duplicateQuota, Flt64NetworkSchedulingSolverValueAdapter).register(
            LinearMetaModel(name = "duplicate_quota"), minimizeCost = false
        ).ok)

        val invalidQuota = problem(
            nodeIds = listOf("root", "terminal", "facility"),
            arcs = listOf(ArcSpec("route", "root", "terminal")),
            root = "root",
            terminals = setOf("terminal"),
            facilities = listOf(facility("facility", 0.0)),
            quotas = listOf(FacilityAssignmentQuota(NetworkNodeId("facility"), minimum = -1))
        )
        assertFalse(NetworkStructureContext(invalidQuota, Flt64NetworkSchedulingSolverValueAdapter).register(
            LinearMetaModel(name = "invalid_quota"), minimizeCost = false
        ).ok)
    }

    @Test
    fun independentFltXContextsNormalizeUnitsAndGuardRegistrationOptions() {
        val root = NetworkNodeId("root")
        val terminal = NetworkNodeId("terminal")
        val route = NetworkArcId("route")
        val networkArc = assertNotNull(NetworkArc(
            from = root,
            to = terminal,
            cost = Quantity(FltX("0"), NoneUnit),
            id = route
        ).value)
        val nodes = listOf(root, terminal).map(::nodeFltX)
        val graph = assertNotNull(NetworkGraph(nodes, listOf(networkArc)).value)
        val flow = FlowProblem(
            graph = graph,
            arcs = listOf(FlowArc(
                networkArc = networkArc,
                capacity = assertNotNull(CapacityBounds(
                    lower = Quantity(FltX("0"), Kilogram),
                    upper = Quantity(FltX("1"), Kilogram)
                ).value)
            ).let { assertNotNull(it.value) }),
            commodities = emptyList(),
            zero = FltX("0")
        )
        val problem = NetworkStructureProblem(
            design = NetworkDesignProblem(
                flow = flow,
                services = listOf(NetworkService(
                    arc = route,
                    levels = listOf(CapacityLevel(
                        id = "half",
                        capacity = Quantity(FltX("500"), Gram),
                        cost = Quantity(FltX("2"), NoneUnit)
                    )),
                    required = true
                ))
            ),
            root = root,
            terminals = setOf(terminal)
        )
        val first = NetworkStructureContext(
            problem = problem,
            adapter = FltXNetworkSchedulingSolverValueAdapter,
            name = "flt_x_first"
        )
        val second = NetworkStructureContext(
            problem = problem,
            adapter = FltXNetworkSchedulingSolverValueAdapter,
            name = "flt_x_second"
        )
        assertNotSame(first.designContext, second.designContext)

        val firstModel = LinearMetaModel(name = "flt_x_first")
        val secondModel = LinearMetaModel(name = "flt_x_second")
        assertTrue(first.register(firstModel).ok)
        assertTrue(first.register(firstModel, minimizeCost = true).ok)
        assertFalse(first.register(firstModel, minimizeCost = false).ok)
        assertFalse(first.register(LinearMetaModel(name = "reused_context"), minimizeCost = true).ok)
        assertTrue(second.register(secondModel).ok)

        for ((context, model) in listOf(first to firstModel, second to secondModel)) {
            val best = enumerate(model, maxDepth = 1).minByOrNull { evaluate(context.constructionCost, it, model) }
            val witness = assertNotNull(best)
            assertEquals(Flt64(2.0), evaluate(context.constructionCost, witness, model))
            model.setSolution(witness)
            val solution = assertNotNull(context.extractSolution(model).value)
            assertEquals(FltX("2"), solution.construction.cost.value)
            assertEquals(mapOf(route to "half"), solution.construction.serviceLevels)
            assertEquals(setOf(route), solution.selectedTreeArcs)
        }
    }

    @Test
    fun customObjectiveCanSuppressDefaultConstructionCostObjective() {
        val problem = problem(
            nodeIds = listOf("root", "terminal"),
            arcs = listOf(ArcSpec("route", "root", "terminal")),
            root = "root",
            terminals = setOf("terminal")
        )
        val context = NetworkStructureContext(
            problem = problem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "embedded"
        )
        val model = LinearMetaModel(name = "embedded")

        assertTrue(context.register(model, minimizeCost = false).ok)
        assertTrue(context.register(model, minimizeCost = false).ok)
        assertFalse(context.register(model, minimizeCost = true).ok)
        assertTrue(model.subObjects.isEmpty())
        assertFalse(model.hasFlattenedObjectives)
    }

    private data class ArcSpec(val id: String, val from: String, val to: String)

    private fun problem(
        nodeIds: List<String>,
        arcs: List<ArcSpec>,
        root: String,
        terminals: Set<String>,
        steinerNodes: Set<String> = emptySet(),
        services: List<NetworkService<Flt64>> = emptyList(),
        facilities: List<NetworkFacility<Flt64>> = emptyList(),
        partitions: List<FacilityPartitionRequest> = emptyList(),
        quotas: List<FacilityAssignmentQuota> = emptyList(),
        budget: Quantity<Flt64>? = null
    ): NetworkStructureProblem<Flt64> {
        val nodes = nodeIds.map(::node)
        val networkArcs = arcs.map { arc(it.id, it.from, it.to) }
        val graph = assertNotNull(NetworkGraph(nodes, networkArcs).value)
        val flow = FlowProblem(
            graph = graph,
            arcs = networkArcs.map { arc ->
                assertNotNull(FlowArc(
                    networkArc = arc,
                    capacity = assertNotNull(CapacityBounds(q(0.0), q(10.0)).value)
                ).value)
            },
            commodities = emptyList(),
            zero = Flt64.zero
        )
        return NetworkStructureProblem(
            design = NetworkDesignProblem(
                flow = flow,
                services = services,
                facilities = facilities,
                budget = budget
            ),
            root = NetworkNodeId(root),
            terminals = terminals.map(::NetworkNodeId).toSet(),
            steinerNodes = steinerNodes.map(::NetworkNodeId).toSet(),
            partitions = partitions,
            quotas = quotas
        )
    }

    private fun enumerate(model: LinearMetaModel<Flt64>, maxDepth: Int): List<List<Flt64>> {
        val tokens = model.tokens.tokensInSolver
        val values = MutableList(tokens.size) { Flt64.zero }
        val solutions = mutableListOf<List<Flt64>>()
        fun visit(index: Int) {
            if (index == values.size) {
                if (constraintsHold(model, values)) solutions.add(values.toList())
                return
            }
            val upper = if (tokens[index].variable.name.contains("_tree_depth_")) maxDepth else 1
            for (value in 0..upper) {
                values[index] = Flt64(value.toDouble())
                visit(index + 1)
            }
        }
        visit(0)
        return solutions
    }

    private fun witness(model: LinearMetaModel<Flt64>, value: (String) -> Flt64): List<Flt64> {
        return model.tokens.tokensInSolver.map { token -> value(token.variable.name) }
    }

    private fun constraintsHold(model: LinearMetaModel<Flt64>, values: List<Flt64>): Boolean {
        return model.constraints.all { it.isTrue(values, model.tokens, false) == true }
    }

    private fun evaluate(expression: LinearPolynomial<Flt64>, values: List<Flt64>, model: LinearMetaModel<Flt64>): Flt64 {
        val tokens = model.tokens.tokensInSolver
        return expression.monomials.fold(expression.constant) { total, monomial ->
            val index = tokens.indexOfFirst { it.variable == monomial.symbol }
            assertTrue(index >= 0, "Missing variable from objective: ${monomial.symbol}")
            total + monomial.coefficient * values[index]
        }
    }

    private fun value(name: String, values: List<Flt64>, model: LinearMetaModel<Flt64>): Flt64 {
        val index = model.tokens.tokensInSolver.indexOfFirst { it.variable.name == name }
        assertTrue(index >= 0, "Missing model variable: $name")
        return values[index]
    }

    private fun node(id: String): NetworkNode<Flt64> {
        return assertNotNull(NetworkNode<Flt64>(NetworkNodeId(id)).value)
    }

    private fun nodeFltX(id: NetworkNodeId): NetworkNode<FltX> {
        return assertNotNull(NetworkNode<FltX>(id).value)
    }

    private fun arc(id: String, from: String, to: String): NetworkArc<Flt64> {
        return assertNotNull(NetworkArc(
            from = NetworkNodeId(from),
            to = NetworkNodeId(to),
            cost = q(0.0),
            id = NetworkArcId(id)
        ).value)
    }

    private fun facility(id: String, cost: Double): NetworkFacility<Flt64> {
        return NetworkFacility(
            node = NetworkNodeId(id),
            cost = q(cost)
        )
    }

    private fun service(id: String, cost: Double): NetworkService<Flt64> {
        return NetworkService(
            arc = NetworkArcId(id),
            levels = listOf(CapacityLevel("built", q(10.0), q(cost)))
        )
    }

    private fun q(value: Double) = Quantity(Flt64(value), NoneUnit)
}
