package fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation.model.*

/** 有限候选枚举作为独立 oracle，不要求原生求解器。 / Independent finite-pool enumeration oracle without native solvers. */
class PathCompilationContextTest {
    @Test
    fun customMembershipUsesTheSameFirstMatchingTokenAsTheReferenceScan() {
        val columns = listOf(
            column(id = "a", cost = 1.0, usage = mapOf("limit" to 1.0)),
            column(id = "b", cost = 2.0, usage = mapOf("limit" to 1.0))
        )
        val context = context(listOf(upper("limit", 3.0)), columns)
        val model = LinearMetaModel(name = "custom_membership")
        try {
            val custom = object : AbstractVariableItem<Flt64, Continuous>(
                type = Continuous,
                name = "custom",
                constants = Flt64
            ) {
                override val identifier = RealVar("custom_identifier").identifier
                override val dimension = 0
                override val index = 0
                override val vectorView = intArrayOf(0)
                override fun belongsTo(item: AbstractVariableItem<*, *>): Boolean = true
            }
            assertTrue(model.add(custom).ok)
            assertTrue(context.register(model).ok)
            model.setSolution(model.tokens.tokensInSolver.map { if (it.variable === custom) Flt64.one else Flt64.zero })
            val expected = columns.filter { column ->
                val variable = context.aggregation.compilation.variables.getValue(column.id)
                model.tokens.tokens.first { it.belongsTo(variable) }.result == Flt64.one
            }.map { it.id }
            val solution = assertNotNull(context.extractSolution(model).value)
            assertEquals(expected, solution.selected.map { it.id })
            assertEquals(Flt64(expected.fold(0) { sum, id -> sum + if (id == "a") 1 else 2 }), solution.cost.value)
        } finally {
            model.close()
        }
    }

    @Test
    fun indivisibleOrdersCompeteForSharedCapacityAndMatchEnumeration() {
        val rows = listOf(exact("order1"), exact("order2"), upper("truck", 3.0))
        val columns = listOf(
            column(id = "1-road", cost = 1.0, usage = mapOf("order1" to 1.0, "truck" to 2.0)),
            column(id = "1-rail", cost = 4.0, usage = mapOf("order1" to 1.0)),
            column(id = "2-road", cost = 2.0, usage = mapOf("order2" to 1.0, "truck" to 2.0)),
            column(id = "2-rail", cost = 8.0, usage = mapOf("order2" to 1.0))
        )
        val context = context(rows, columns)
        val model = LinearMetaModel(name = "orders")
        assertTrue(context.register(model).ok)
        val best = enumerate(context, model)
        assertEquals(Flt64(6.0), best.cost.value)
        assertEquals(setOf("1-rail", "2-road"), best.selected.map { it.id }.toSet())
        assertTrue(model.tokens.tokens.all { it.variable.type.isIntegerType })
        model.setSolution(listOf(Flt64(0.5), Flt64(0.5), Flt64(0.5), Flt64(0.5)))
        assertFalse(context.extractSolution(model).ok)
    }

    @Test
    fun columnLifecycleKeepsObjectiveAndRowsSynchronized() {
        val initial = column(id = "first", cost = 7.0, usage = mapOf("order" to 1.0))
        val context = context(listOf(exact("order")), listOf(initial))
        val model = LinearMetaModel(name = "lifecycle")
        assertTrue(context.register(model).ok)
        assertTrue(context.register(model).ok)
        assertTrue(assertNotNull(context.addColumns(listOf(initial), model).value).isEmpty())
        assertFalse(context.addColumns(listOf(column(id = "first", cost = 99.0, usage = mapOf("order" to 1.0))), model).ok)
        val second = column(id = "second", cost = 3.0, usage = mapOf("order" to 1.0))
        assertEquals(1, assertNotNull(context.addColumns(listOf(second), model).value).size)
        assertEquals(Flt64(3.0), enumerate(context, model).cost.value)
        val removedVariable = context.aggregation.compilation.variables.getValue("second")
        assertTrue(context.removeColumns(setOf("second", "absent"), model).ok)
        assertTrue(model.subObjects.none { sub -> sub.polynomial.monomials.any { it.symbol == removedVariable } })
        assertTrue(model.objectiveSnapshot().none { objective -> objective.linearTerms().any { it.second == removedVariable } })
        assertTrue(context.aggregation.compilation.rows.values.none { row -> row.asMutable().monomials.any { it.symbol == removedVariable } })
        assertEquals(Flt64(7.0), enumerate(context, model).cost.value)
        assertEquals(setOf("first"), enumerate(context, model).candidateIds)
        assertEquals(1, assertNotNull(context.addColumns(listOf(second), model).value).size)
        assertEquals(Flt64(3.0), enumerate(context, model).cost.value)
        val other = LinearMetaModel(name = "other")
        assertFalse(context.register(other).ok)
        assertFalse(context.addColumns(listOf(second), other).ok)
        val independent = context(context.problem.rows, context.problem.columns)
        assertTrue(independent.register(other).ok)
        assertNotSame(context.aggregation.compilation.variables.getValue("first"), independent.aggregation.compilation.variables.getValue("first"))
    }

    @Test
    fun fullCostAndAdditionalRowDualChangePreferredPathAndRejectStaleScopes() {
        val context = context(listOf(exact("order"), upper(id = "resource", amount = 1.0)), emptyList())
        val model = LinearMetaModel(name = "pricing")
        assertTrue(context.register(model).ok)
        val cheap = column(id = "cheap", cost = 3.0, usage = mapOf("order" to 1.0, "resource" to 1.0))
        val detour = column(id = "detour", cost = 5.0, usage = mapOf("order" to 1.0))
        val prices = MetaDualSolution(
            constraints = model.constraints.associateWith { if (it.toString().contains("resource")) Flt64(-4.0) else Flt64(6.0) },
            symbols = emptyMap()
        )
        val duals = assertNotNull(context.aggregation.refreshShadowPrices(
            target = model,
            duals = prices,
            scope = "branch0/graph1"
        ).value)
        assertEquals(Flt64.one, context.aggregation.reducedCost(
            candidate = cheap,
            duals = duals,
            scope = "branch0/graph1"
        ).value)
        assertEquals(Flt64(-1.0), context.aggregation.reducedCost(
            candidate = detour,
            duals = duals,
            scope = "branch0/graph1"
        ).value)
        assertFalse(context.aggregation.reducedCost(
            candidate = detour,
            duals = duals,
            scope = "branch1/graph1"
        ).ok)
        assertTrue(context.addColumns(listOf(detour), model).ok)
        assertFalse(context.aggregation.reducedCost(
            candidate = detour,
            duals = duals,
            scope = "branch0/graph1"
        ).ok)
        assertFalse(context.aggregation.refreshShadowPrices(
            target = model,
            duals = MetaDualSolution(emptyMap(), emptyMap()),
            scope = "next"
        ).ok)
        val next = assertNotNull(context.aggregation.refreshShadowPrices(
            target = model,
            duals = prices,
            scope = "next"
        ).value)
        assertEquals(Flt64(-1.0), context.aggregation.reducedCost(
            candidate = detour,
            duals = next,
            scope = "next"
        ).value)
        context.aggregation.compilation.variables.getValue("detour").range.eq(UInt8.zero)
        assertFalse(context.aggregation.reducedCost(
            candidate = cheap,
            duals = next,
            scope = "next"
        ).ok)
    }

    @Test
    fun reducedCostBatchPreservesInputOrderAndCallsTheAdapterForEveryCandidate() {
        val normalized = mutableListOf<Flt64>()
        var failedValue: Flt64? = null
        val adapter = object : NetworkSchedulingSolverValueAdapter<Flt64> {
            override fun toSolverValue(value: Flt64): Ret<Flt64> = ok(value)

            override fun fromSolverValue(value: Flt64): Ret<Flt64> = ok(value)

            override fun normalize(quantity: Quantity<Flt64>, targetUnit: PhysicalUnit): Ret<Flt64> {
                normalized.add(quantity.value)
                return if (quantity.value == failedValue) {
                    networkSchedulingFailure("批次适配失败：${quantity.value} / Batch adapter rejected ${quantity.value}")
                } else {
                    ok(quantity.value)
                }
            }
        }
        val context = PathCompilationContext(
            problem = PathCompilationProblem(
                rows = listOf(exact("order")),
                columns = emptyList<PathColumn<Flt64, String>>(),
                costUnit = NoneUnit
            ),
            adapter = adapter
        )
        val model = LinearMetaModel(name = "pricing_batch_order")
        assertTrue(context.register(model).ok)
        val duals = assertNotNull(context.aggregation.refreshShadowPrices(
            target = model,
            duals = MetaDualSolution(model.constraints.associateWith { Flt64.zero }, emptyMap()),
            scope = "batch"
        ).value)
        val later = column(id = "later", cost = 9.0, usage = mapOf("order" to 2.0))
        val earlier = column(id = "earlier", cost = 5.0, usage = mapOf("order" to 1.0))

        normalized.clear()
        assertEquals(listOf(Flt64(9.0), Flt64(5.0)), assertNotNull(context.aggregation.reducedCosts(
            candidates = listOf(later, earlier),
            duals = duals,
            scope = "batch"
        ).value))
        assertEquals(listOf(Flt64(9.0), Flt64(2.0), Flt64(5.0), Flt64.one), normalized)

        normalized.clear()
        failedValue = Flt64(9.0)
        val firstFailure = assertIs<Failed<*, *, *>>(context.aggregation.reducedCosts(
            candidates = listOf(later, earlier),
            duals = duals,
            scope = "batch"
        ))
        assertTrue(firstFailure.error.message?.contains("Batch adapter rejected") == true)
        assertEquals(listOf(Flt64(9.0)), normalized)
        model.close()
    }

    @Test
    fun reducedCostBatchRejectsStaleDualsAndAuditsAdapterModelChangesAfterPricing() {
        var target: LinearMetaModel<Flt64>? = null
        var mutateModel = false
        val adapter = object : NetworkSchedulingSolverValueAdapter<Flt64> {
            override fun toSolverValue(value: Flt64): Ret<Flt64> = ok(value)

            override fun fromSolverValue(value: Flt64): Ret<Flt64> = ok(value)

            override fun normalize(quantity: Quantity<Flt64>, targetUnit: PhysicalUnit): Ret<Flt64> {
                if (mutateModel) {
                    mutateModel = false
                    val current = target
                        ?: return networkSchedulingFailure("缺少测试模型 / Missing test model")
                    val extension = BinVar("adapter_extension")
                    if (current.add(extension) !is Ok || current.addConstraint(
                        relation = LinearPolynomial(extension) leq Flt64.one,
                        name = "adapter_extension_limit"
                    ) !is Ok) {
                        return networkSchedulingFailure("测试扩展注册失败 / Test extension registration failed")
                    }
                }
                return ok(quantity.value)
            }
        }
        val context = PathCompilationContext(
            problem = PathCompilationProblem(
                rows = listOf(exact("order")),
                columns = emptyList<PathColumn<Flt64, String>>(),
                costUnit = NoneUnit
            ),
            adapter = adapter
        )
        val model = LinearMetaModel(name = "pricing_batch_freshness")
        target = model
        assertTrue(context.register(model).ok)
        val oldDuals = assertNotNull(context.aggregation.refreshShadowPrices(
            target = model,
            duals = MetaDualSolution(model.constraints.associateWith { Flt64.zero }, emptyMap()),
            scope = "batch"
        ).value)
        val firstColumn = column(id = "registered", cost = 1.0, usage = mapOf("order" to 1.0))
        assertTrue(context.addColumns(listOf(firstColumn), model).ok)
        val staleFailure = assertIs<Failed<*, *, *>>(context.aggregation.reducedCosts(
            candidates = listOf(column(id = "", cost = 2.0, usage = mapOf("order" to 1.0))),
            duals = oldDuals,
            scope = "batch"
        ))
        assertTrue(staleFailure.error.message?.contains("Stale duals") == true)

        val currentDuals = assertNotNull(context.aggregation.refreshShadowPrices(
            target = model,
            duals = MetaDualSolution(model.constraints.associateWith { Flt64.zero }, emptyMap()),
            scope = "batch"
        ).value)
        mutateModel = true
        assertFalse(context.aggregation.reducedCosts(
            candidates = listOf(column(id = "candidate", cost = 2.0, usage = mapOf("order" to 1.0))),
            duals = currentDuals,
            scope = "batch"
        ).ok)
        model.close()
    }

    @Test
    fun externalUnpricedConstraintCannotSilentlyContributeZero() {
        val context = context(listOf(exact("order")), listOf(column(id = "p", cost = 1.0, usage = mapOf("order" to 1.0))))
        val model = LinearMetaModel(name = "external")
        assertTrue(context.register(model).ok)
        assertTrue(model.addConstraint(context.aggregation.compilation.rows.getValue("order") leq Flt64(2.0)).ok)
        assertFalse(context.aggregation.refreshShadowPrices(
            target = model,
            duals = MetaDualSolution(model.constraints.associateWith { Flt64.zero }, emptyMap()),
            scope = "all"
        ).ok)
    }

    @Test
    fun unitNormalizationAndFltXStayAtRegistrationAndExtractionBoundaries() {
        val row = PathCompilationRow(
            id = "mass",
            unit = Kilogram,
            lower = Quantity(FltX.one, Kilogram),
            upper = Quantity(FltX.one, Kilogram)
        )
        val column = PathColumn(
            id = "lot",
            payload = "lot",
            cost = Quantity(FltX(2), NoneUnit),
            coefficients = mapOf("mass" to Quantity(FltX(1000), Gram))
        )
        val context = PathCompilationContext(
            problem = PathCompilationProblem(rows = listOf(row), columns = listOf(column), costUnit = NoneUnit),
            adapter = FltXNetworkSchedulingSolverValueAdapter
        )
        val model = LinearMetaModel(name = "units")
        assertTrue(context.register(model).ok)
        model.setSolution(listOf(Flt64.one))
        val result = assertNotNull(context.extractSolution(model).value)
        assertTrue(FltX.one eq result.usage.getValue("mass").value)
        val wrong = PathColumn(
            id = "wrong",
            payload = "wrong",
            cost = Quantity(FltX.one, NoneUnit),
            coefficients = mapOf("mass" to Quantity(FltX.one, Meter))
        )
        assertFalse(context.addColumns(listOf(wrong), model).ok)
        val failingAdapter = object : NetworkSchedulingSolverValueAdapter<Flt64> {
            override fun toSolverValue(value: Flt64): Ret<Flt64> = networkSchedulingFailure("故意转换失败 / Intentional conversion failure")
            override fun fromSolverValue(value: Flt64): Ret<Flt64> = ok(value)
        }
        val failure = PathCompilationContext(
            problem = PathCompilationProblem(
                rows = listOf(exact("order")),
                columns = emptyList<PathColumn<Flt64, String>>(),
                costUnit = NoneUnit
            ),
            adapter = failingAdapter
        )
        assertFalse(failure.register(LinearMetaModel(name = "failed")).ok)
        assertFalse(failure.register(LinearMetaModel(name = "retry")).ok)
    }

    @Test
    fun rejectsAffineUnitsNonFiniteAdapterResultsAndMaximizationModels() {
        val affine = PathCompilationContext(
            problem = PathCompilationProblem(
                rows = listOf(PathCompilationRow(id = "temperature", unit = Celsius, lower = Quantity(Flt64(1.0), Celsius))),
                columns = emptyList<PathColumn<Flt64, String>>(),
                costUnit = NoneUnit
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        )
        assertFalse(affine.register(LinearMetaModel(name = "affine")).ok)

        val nonFiniteAdapter = object : NetworkSchedulingSolverValueAdapter<Flt64> {
            override fun toSolverValue(value: Flt64): Ret<Flt64> = ok(Flt64(Double.NaN))
            override fun fromSolverValue(value: Flt64): Ret<Flt64> = ok(value)
        }
        val nonFinite = PathCompilationContext(
            problem = PathCompilationProblem(
                rows = listOf(exact("order")),
                columns = emptyList<PathColumn<Flt64, String>>(),
                costUnit = NoneUnit
            ),
            adapter = nonFiniteAdapter
        )
        assertFalse(nonFinite.register(LinearMetaModel(name = "non_finite")).ok)
        assertFalse(nonFinite.aggregation.compile(column(id = "candidate", cost = 1.0, usage = mapOf("order" to 1.0))).ok)

        val maximizing = context(listOf(exact("order")), emptyList())
        assertFalse(maximizing.register(LinearMetaModel(
            name = "maximizing",
            objectCategory = ObjectCategory.Maximum
        )).ok)
    }

    @Test
    fun pricingRejectsChangesToExposedObjectivePolynomialsAfterDualRefresh() {
        val context = context(
            rows = listOf(exact("order")),
            columns = listOf(column(id = "registered", cost = 1.0, usage = mapOf("order" to 1.0)))
        )
        val model = LinearMetaModel(name = "objective_change")
        assertTrue(context.register(model).ok)
        val duals = assertNotNull(context.aggregation.refreshShadowPrices(
            target = model,
            duals = MetaDualSolution(model.constraints.associateWith { Flt64.zero }, emptyMap()),
            scope = "current"
        ).value)
        val variable = context.aggregation.compilation.variables.getValue("registered")
        assertTrue(model.addObject(
            category = ObjectCategory.Minimum,
            polynomial = Flt64(2.0) * LinearPolynomial(variable),
            name = "external_path_cost",
            displayName = null
        ).ok)
        assertFalse(context.aggregation.reducedCost(
            candidate = column(id = "candidate", cost = 1.0, usage = mapOf("order" to 1.0)),
            duals = duals,
            scope = "current"
        ).ok)
        assertFalse(context.aggregation.refreshShadowPrices(
            target = model,
            duals = MetaDualSolution(model.constraints.associateWith { Flt64.zero }, emptyMap()),
            scope = "new"
        ).ok)
    }

    @Test
    fun alteredFlattenedPathCostsAreRejectedBeforePricingAndExtraction() {
        val context = context(
            rows = listOf(exact("order")),
            columns = listOf(column(id = "registered", cost = 1.0, usage = mapOf("order" to 1.0)))
        )
        val model = LinearMetaModel(name = "opaque_objective")
        assertTrue(context.register(model).ok)
        assertTrue(model.hasFlattenedObjectives)
        val duals = assertNotNull(context.aggregation.refreshShadowPrices(
            target = model,
            duals = MetaDualSolution(model.constraints.associateWith { Flt64.zero }, emptyMap()),
            scope = "current"
        ).value)
        val variable = context.aggregation.compilation.variables.getValue("registered")
        assertTrue(model.minimize(Flt64(9.0) * LinearPolynomial(variable), name = "opaque_cost").ok)
        assertTrue(model.hasFlattenedObjectives)
        assertFalse(context.aggregation.reducedCost(
            candidate = column(id = "candidate", cost = 1.0, usage = mapOf("order" to 1.0)),
            duals = duals,
            scope = "current"
        ).ok)
        assertFalse(context.aggregation.refreshShadowPrices(
            target = model,
            duals = MetaDualSolution(model.constraints.associateWith { Flt64.zero }, emptyMap()),
            scope = "new"
        ).ok)
        model.setSolution(listOf(Flt64.one))
        assertFalse(context.extractSolution(model).ok)
    }

    @Test
    fun orderedMultimodalServicesRetainFullStateAndTerminalNonAdditiveCost() {
        val arcs = listOf(
            Service(id = "road", from = "s", to = "hub", stage = 0, cost = 2.0),
            Service(id = "rail", from = "s", to = "hub", stage = 0, cost = 3.0),
            Service(id = "inspect", from = "hub", to = "t", stage = 1, cost = 1.0),
            Service(id = "bypass", from = "s", to = "t", stage = 1, cost = 0.0)
        )
        val graph = graph(listOf("s", "hub", "t"), arcs)
        val request = PathProblem(
            graph = graph,
            source = NetworkNodeId("s"),
            initial = ServiceState(stage = 0, cost = 0.0, activities = emptyList()),
            startContribution = { _, state -> ok(state.copy(cost = 1.0)) },
            transition = { label, arc ->
                ok(if (arc.stage == label.state.stage) ServiceState(
                    stage = arc.stage + 1,
                    cost = label.state.cost + arc.cost,
                    activities = label.state.activities + arc.id
                ) else null)
            },
            terminal = { node, state -> node == NetworkNodeId("t") && state.stage == 2 },
            terminalContribution = { label -> ok(label.state.copy(cost = label.state.cost + if ("rail" in label.state.activities) -3.0 else 2.0)) }
        )
        val generated = assertNotNull(PathSearchContext().search(request).value)
        assertTrue(generated.complete)
        assertEquals(2, generated.paths.size)
        val columns = generated.paths.map { path ->
            PathColumn(
                id = path.state.activities.joinToString("/"),
                payload = path.state,
                cost = q(path.state.cost),
                coefficients = mapOf("chain" to q(1.0))
            )
        }
        val context = PathCompilationContext(
            problem = PathCompilationProblem(rows = listOf(exact("chain")), columns = columns, costUnit = NoneUnit),
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        )
        val model = LinearMetaModel(name = "service_chain")
        assertTrue(context.register(model).ok)
        model.setSolution(columns.map { if ("rail" in it.payload.activities) Flt64.one else Flt64.zero })
        val result = assertNotNull(context.extractSolution(model).value)
        assertEquals(Flt64(2.0), result.cost.value)
        assertEquals(listOf("rail", "inspect"), result.selected.single().payload.activities)
        val truncated = assertNotNull(PathSearchContext().search(
            problem = request,
            budget = PathSearchBudget(maximumLabels = 1)
        ).value)
        assertFalse(truncated.complete)
        assertTrue(truncated.paths.isEmpty())
    }

    @Test
    fun arcServiceCoverageUsesStableParallelArcIdentityAndRepeatedUsage() {
        val fast = NetworkArcId("parallel-fast")
        val slow = NetworkArcId("parallel-slow")
        val walks = listOf(listOf(fast, fast), listOf(slow), listOf(fast))
        val rows = listOf(upper(id = "vehicle", amount = 2.0), PathCompilationRow(
            id = fast.value,
            unit = NoneUnit,
            lower = q(2.0)
        ), exact(slow.value))
        val columns = walks.mapIndexed { index, walk ->
            column(id = "walk$index", cost = listOf(3.0, 2.0, 2.0)[index], usage = mapOf("vehicle" to 1.0) +
                walk.groupingBy { it.value }.eachCount().mapValues { it.value.toDouble() })
        }
        val context = context(rows, columns)
        val model = LinearMetaModel(name = "arc_coverage")
        assertTrue(context.register(model).ok)
        val result = enumerate(context, model)
        assertEquals(setOf("walk0", "walk1"), result.selected.map { it.id }.toSet())
        assertEquals(Flt64(2.0), result.usage.getValue(fast.value).value)
        assertEquals(Flt64.one, result.usage.getValue(slow.value).value)
    }

    private fun enumerate(context: PathCompilationContext<Flt64, String>, model: LinearMetaModel<Flt64>): PathCompilationSolution<Flt64, String> {
        val size = model.tokens.tokensInSolver.size
        val feasible = (0 until (1 shl size)).mapNotNull { mask ->
            val values = (0 until size).map { if ((mask and (1 shl it)) != 0) Flt64.one else Flt64.zero }
            if (!model.constraints.all {
                it.isTrue(solution = values, tokenTable = model.tokens, zeroIfNone = false) == true
            }) return@mapNotNull null
            model.setSolution(values)
            val result = assertNotNull(context.extractSolution(model).value)
            val objective = model.objectiveSnapshot().fold(Flt64.zero) { acc, sub -> acc + assertNotNull(sub.evaluate(values)) }
            assertEquals(result.cost.value, objective)
            result
        }
        return assertNotNull(feasible.minByOrNull { it.cost.value.toDouble() })
    }

    private fun context(rows: List<PathCompilationRow<Flt64>>, columns: List<PathColumn<Flt64, String>>) = PathCompilationContext(
        problem = PathCompilationProblem(rows = rows, columns = columns, costUnit = NoneUnit),
        adapter = Flt64NetworkSchedulingSolverValueAdapter
    )

    private fun column(
        id: String,
        cost: Double,
        usage: Map<String, Double>
    ) = PathColumn(
        id = id,
        payload = id,
        cost = q(cost),
        coefficients = usage.mapValues { q(it.value) }
    )

    private fun exact(id: String) = PathCompilationRow(id = id, unit = NoneUnit, lower = q(1.0), upper = q(1.0))
    private fun upper(id: String, amount: Double) = PathCompilationRow(id = id, unit = NoneUnit, upper = q(amount))
    private fun q(value: Double) = Quantity(Flt64(value), NoneUnit)
    private data class Service(
        val id: String,
        val from: String,
        val to: String,
        val stage: Int,
        val cost: Double
    )

    private data class ServiceState(
        val stage: Int,
        val cost: Double,
        val activities: List<String>
    )

    private fun graph(nodes: List<String>, arcs: List<Service>): NetworkGraph<String, Service> = assertNotNull(NetworkGraph(
        nodes = nodes,
        arcs = arcs,
        access = NetworkGraphAccess(
            nodeId = { NetworkNodeId(it) },
            arcId = { NetworkArcId(it.id) },
            from = { NetworkNodeId(it.from) },
            to = { NetworkNodeId(it.to) }
        )
    ).value)
}
