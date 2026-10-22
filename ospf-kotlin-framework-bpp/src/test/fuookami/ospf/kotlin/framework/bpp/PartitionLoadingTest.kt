package fuookami.ospf.kotlin.framework.bpp

import kotlin.test.*
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.bpp.application.*
import fuookami.ospf.kotlin.framework.bpp.infrastructure.Flt64PackingValueAdapter
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.*
import fuookami.ospf.kotlin.framework.bpp.domain.partition_loading.*

/** 无商业许可证的分区主问题、定价系数与生命周期验证。 / License-free partition master, pricing coefficient and lifecycle tests. */
class PartitionLoadingTest {
    private val partitions = listOf(LoadingPartition("fore", setOf("big")), LoadingPartition("aft", setOf("a", "b")))

    private fun registration(fixed: Boolean = false, required: Boolean = false, internalConflict: Boolean = false): RegisteredDiscreteLoading<Flt64> {
        val context = DiscreteLoadingContext(
            problem = DiscreteLoadingProblem(
                items = listOf(LoadingItem("one", Quantity(Flt64.one, Kilogram), required = required), LoadingItem("two", Quantity(Flt64.one, Kilogram), required = required, fixedPositionId = if (fixed) "b" else null)),
                positions = listOf("big", "a", "b").map { LoadingPosition(it, UInt64(2), Quantity(Flt64(2), Kilogram)) },
                allowedAssignments = listOf("one", "two").flatMap { item -> listOf("big", "a", "b").map { AssignmentKey(item, it) } }.toSet(),
                conflicts = setOf(PositionConflict("big", "a"), PositionConflict("big", "b")) + if (internalConflict) setOf(PositionConflict("a", "b")) else emptySet(),
                layoutGroups = listOf(LayoutSelectionGroup("whole", setOf("big", "a", "b"), listOf(LoadingLayout("large", setOf("big")), LoadingLayout("small", setOf("a", "b"))))),
                weightUnit = Kilogram
            ),
            adapter = Flt64PackingValueAdapter
        )
        val model = LinearMetaModel<Flt64>(name = "partition-test", converter = IntoValue.Identity)
        assertIs<Ok<*, *, *>>(context.register(model))
        // 业务全局行与目标在列生成之前注册，其系数必须进入定价。
        // Register business rows and objectives before CG so their coefficients enter pricing.
        model.addConstraint(sum(context.aggregation.counts.values.toList()) leq Flt64(2), name = "business_resource")
        model.minimize(-sum(context.aggregation.loaded.values.toList()))
        return RegisteredDiscreteLoading(context, model)
    }

    @Test
    fun `explicit ownership rejects missing duplicate and unknown partitions`(): Unit = runBlocking {
        val registered = registration()
        for (invalid in listOf(partitions.take(1), partitions + LoadingPartition("other", setOf("a")), listOf(LoadingPartition("unknown", setOf("missing"))))) {
            assertIs<Failed<*, *, *>>(PartitionLoadingContext.create(registered.context, registered.model, invalid))
        }
    }

    @Test
    fun `partition combinations match sparse MILP with cross partition overlap layouts uniqueness and fixed cargo`(): Unit = runBlocking {
        val solver = EnumerationSolver()
        for (fixed in listOf(false, true)) {
            val direct = assertIs<Ok<DiscreteLoadingSolveResult<Flt64>, *, *>>(DiscreteLoadingApplicationService<Flt64>(milpSolver = { solver.solveMILP(it) }).solve(factory = { Ok(registration(fixed)) })).value
            val columns = assertIs<Ok<PartitionColumnGenerationResult<Flt64>, *, *>>(PartitionColumnGenerationApplicationService(solver, AllConfigurations()).solve(
                registration = registration(fixed), partitions = partitions, config = PartitionColumnGenerationConfig(iterationLimit = 1)
            )).value
            assertEquals(direct.objective, columns.loading.objective)
            assertEquals("Feasible", columns.loading.status)
            assertEquals("partition_column_generation", columns.loading.path)
            assertFalse(columns.lpConverged)
            assertEquals(2, columns.loading.solution.assignments.size)
            assertFalse("big" in columns.loading.solution.occupiedPositions && columns.loading.solution.occupiedPositions.any { it == "a" || it == "b" })
            if (fixed) assertEquals("b", columns.loading.solution.assignments["two"])
        }
    }

    @Test
    fun `global business shadow prices affect pricing and column removal rebuilds all row and objective coefficients`(): Unit = runBlocking {
        val registered = registration()
        val context = assertIs<Ok<PartitionLoadingContext<Flt64>, *, *>>(PartitionLoadingContext.create(registered.context, registered.model, partitions)).value
        val oracle = AllConfigurations()
        for (partition in partitions) {
            val generated = assertIs<Ok<PartitionPricingResult, *, *>>(oracle.price(context, request(context, partition.id, true))).value
            assertIs<Ok<*, *, *>>(context.register(generated.values.map { partition.id to it }))
        }
        assertTrue(context.columns.any { it.partitionId == "fore" && it.assignments.isEmpty() && it.occupiedPositions.isEmpty() })
        assertTrue(context.columns.any { it.assignments.size == 2 && it.assignments.values.toSet().size == 1 })
        val rowId = context.rows.single { it.sourceName == "business_resource" }.id
        val master = assertIs<Ok<PartitionMasterRegistration, *, *>>(context.master(false, false)).value
        assertIs<Ok<LinearMechanismModel<Flt64>, *, *>>(LinearMechanismModel(master.model)).value.use { expanded ->
            @Suppress("UNCHECKED_CAST")
            val dual = expanded.constraints.associate { row ->
                @Suppress("UNCHECKED_CAST")
                (row as Constraint<Flt64, Linear>) to if (row.name == rowId) Flt64(3) else Flt64.zero
            }
            assertIs<Ok<*, *, *>>(context.refreshShadowPrice(master, ColumnGenerationSolver.LPResult(
                SolverStatus.Optimal.toSolveReport(objective = Flt64.zero, values = emptyList(), solveTime = Duration.ZERO), dual
            )))
        }
        val priced = request(context, "aft", false)
        val assignmentIds = context.decisions.filter { it.partitionId == "aft" && it.variable in registered.context.aggregation.assignments.values }.map { it.id }
        assertTrue(assignmentIds.all { priced.coefficients[it] == Flt64(-4) })
        val removed = context.columns.first { it.assignments.isNotEmpty() }.id
        assertIs<Ok<*, *, *>>(context.removeColumns(setOf(removed)))
        val rebuilt = assertIs<Ok<PartitionMasterRegistration, *, *>>(context.master(false, true)).value
        assertFalse(removed in rebuilt.columnVariables)
        assertEquals(context.columns.size, rebuilt.columnVariables.size)
    }

    @Test
    fun `truncated partition pools never report global integer optimality`(): Unit = runBlocking {
        val result = assertIs<Ok<PartitionColumnGenerationResult<Flt64>, *, *>>(PartitionColumnGenerationApplicationService(EnumerationSolver(), AllConfigurations()).solve(
            registration = registration(), partitions = partitions,
            config = PartitionColumnGenerationConfig(iterationLimit = 1, columnLimit = 2)
        )).value
        assertEquals("Feasible", result.loading.status)
        assertFalse(result.lpConverged)
        assertEquals(2, result.columnCount)
    }

    @Test
    fun `restricted pool infeasibility does not claim original infeasibility`(): Unit = runBlocking {
        val result = PartitionColumnGenerationApplicationService(EnumerationSolver(), AllConfigurations()).solve(
            registration = registration(required = true),
            partitions = partitions,
            config = PartitionColumnGenerationConfig(iterationLimit = 1, columnLimit = 2)
        )
        assertEquals(ErrorCode.OREngineSolvingException, assertIs<Failed<*, *, *>>(result).error.code)
    }

    @Test
    fun `cross partition clique retains internal conflicts in local pricing and column validation`(): Unit = runBlocking {
        val registered = registration(internalConflict = true)
        val context = assertIs<Ok<PartitionLoadingContext<Flt64>, *, *>>(PartitionLoadingContext.create(registered.context, registered.model, partitions)).value
        val priced = assertIs<Ok<PartitionPricingResult, *, *>>(AllConfigurations().price(context, request(context, "aft", true))).value
        assertIs<Ok<*, *, *>>(context.register(priced.values.map { "aft" to it }))
        assertTrue(context.columns.all { it.occupiedPositions.size <= 1 })
        val invalid = context.decisions.filter { it.partitionId == "aft" }.associate { decision ->
            decision.id to if (decision.variable in registered.context.aggregation.occupied.values) Flt64.one else Flt64.zero
        }
        assertIs<Failed<*, *, *>>(context.addColumns(listOf("aft" to invalid)))
        assertIs<Failed<*, *, *>>(context.pricingRequest(partitionId = "missing", phaseOne = true, seed = true))
        assertIs<Failed<*, *, *>>(context.pricingModel(request(context, "aft", true).copy(coefficients = emptyMap())))
        assertIs<Failed<*, *, *>>(request(context, "aft", true).reducedCost(emptyMap()))
    }

    @Test
    fun `continuous estimated weight can occupy an otherwise empty partition`(): Unit = runBlocking {
        val registered = registration()
        val extra = URealVarForTest(registered)
        val context = assertIs<Ok<PartitionLoadingContext<Flt64>, *, *>>(PartitionLoadingContext.create(
            loading = registered.context, source = registered.model, partitions = partitions,
            extraDecisions = listOf(PartitionDecision("estimate", "fore", extra))
        )).value
        val generated = assertIs<Ok<PartitionPricingResult, *, *>>(AllConfigurations().price(context, request(context, "fore", true))).value
        assertIs<Ok<*, *, *>>(context.addColumns(generated.values.map { "fore" to it }))
        assertTrue(context.columns.any { it.assignments.isEmpty() && it.decisions.getValue("estimate") > Flt64.zero && "big" in it.occupiedPositions })
        val tiny = Flt64(1e-12)
        val tinyWeight = context.decisions.filter { it.partitionId == "fore" }.associate { decision ->
            decision.id to when {
                decision.id == "estimate" -> tiny
                decision.variable == registered.context.aggregation.occupied.getValue("big") -> Flt64.one
                else -> Flt64(-1e-10)
            }
        }
        assertIs<Ok<*, *, *>>(context.addColumns(listOf("fore" to tinyWeight)))
        val normalized = context.columns.single { it.decisions["estimate"] == tiny }
        assertEquals(setOf("big"), normalized.occupiedPositions)
        assertTrue(normalized.decisions.filterKeys { it != "estimate" }.values.all { it == Flt64.zero || it == Flt64.one })
    }

    private suspend fun request(context: PartitionLoadingContext<Flt64>, partitionId: String, seed: Boolean): PartitionPricingRequest {
        return assertIs<Ok<PartitionPricingRequest, *, *>>(context.pricingRequest(
            partitionId = partitionId,
            phaseOne = seed,
            seed = seed
        )).value
    }

    @Test
    fun `late business row is projected onto existing and new columns without changing application`(): Unit = runBlocking {
        val registered = registration()
        val context = assertIs<Ok<PartitionLoadingContext<Flt64>, *, *>>(PartitionLoadingContext.create(
            registered.context, registered.model, partitions
        )).value
        val oracle = AllConfigurations()
        for (partition in partitions) {
            val generated = assertIs<Ok<PartitionPricingResult, *, *>>(oracle.price(context, request(context, partition.id, true))).value
            assertIs<Ok<*, *, *>>(context.addColumns(generated.values.map { partition.id to it }))
        }
        val old = assertIs<Ok<PartitionMasterRegistration, *, *>>(context.master(false, false)).value
        val previousIds = context.columns.map { it.id }.toSet()
        assertIs<Ok<*, *, *>>(registered.model.state.registerRow(
            ConstraintId("late-occupancy-limit"),
            sum(registered.context.aggregation.occupied.values.toList()) leq Flt64.one
        ))
        assertIs<Failed<*, *, *>>(context.projection.requireCurrent(old))
        for (integer in listOf(false, true)) {
            val master = assertIs<Ok<PartitionMasterRegistration, *, *>>(context.master(false, integer)).value
            val snapshot = assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(master.model.state.refresh()).value
            val row = snapshot.rows.single { it.sourceName == "late-occupancy-limit" }
            for (column in context.columns) {
                assertEquals(Flt64(column.occupiedPositions.size), row.coefficients[master.columnVariables.getValue(column.id)] ?: Flt64.zero)
            }
        }
        assertEquals(previousIds, context.columns.map { it.id }.toSet())
        val generated = assertIs<Ok<PartitionPricingResult, *, *>>(oracle.price(context, request(context, "aft", true))).value
        assertIs<Ok<*, *, *>>(context.addColumns(generated.values.map { "aft" to it }))
        val final = assertIs<Ok<PartitionMasterRegistration, *, *>>(context.master(false, true)).value
        val solved = assertIs<Ok<SolveReport<Flt64>, *, *>>(EnumerationSolver().solveMILP(final.model)).value
        val direct = assertIs<Ok<SolveReport<Flt64>, *, *>>(EnumerationSolver().solveMILP(registered.model)).value
        assertEquals(direct.solution?.objective, solved.solution?.objective)
        val full = assertIs<Ok<List<Flt64>, *, *>>(context.extractSolution(final, solved.values)).value
        val occupied = registered.context.aggregation.occupied.values.count { full[registered.model.tokens.indexOf(it)!!] > Flt64(0.5) }
        assertTrue(occupied <= 1)
    }

    private fun URealVarForTest(registration: RegisteredDiscreteLoading<Flt64>): fuookami.ospf.kotlin.core.variable.URealVar {
        val variable = fuookami.ospf.kotlin.core.variable.URealVar("estimated")
        variable.range.leq(Flt64.one)
        registration.model.add(variable)
        registration.context.linkAdditionalWeight(positionId = "big", additionalWeight = LinearPolynomial(variable), model = registration.model)
        return variable
    }

    private class AllConfigurations : PartitionPricingOracle<Flt64> {
        override suspend fun price(context: PartitionLoadingContext<Flt64>, request: PartitionPricingRequest): Ret<PartitionPricingResult> {
            val model = assertIs<Ok<LinearMetaModel<Flt64>, *, *>>(context.pricingModel(request)).value
            val vectors = enumerate(model).map { (_, values) -> context.decisions.filter { it.partitionId == request.partitionId }.associate { it.id to values[model.tokens.indexOf(it.variable)!!] } }.distinct()
            return Ok(PartitionPricingResult(vectors, true))
        }
    }

    private class EnumerationSolver : ColumnGenerationSolver {
        override val name = "small-integer-enumeration"
        override suspend fun solveMILP(name: String, metaModel: LinearMetaModel<Flt64>, toLogModel: Boolean, registrationStatusCallBack: RegistrationStatusCallBack?, solvingStatusCallBack: SolvingStatusCallBack?): Ret<SolveReport<Flt64>> {
            val best = enumerate(metaModel).minByOrNull { it.first } ?: return Failed(ErrorCode.ORModelInfeasible, "enumerated infeasible")
            return Ok(SolverStatus.Optimal.toSolveReport(objective = best.first, values = best.second, solveTime = Duration.ZERO, bestBound = best.first, gap = Flt64.zero))
        }

        override suspend fun solveLP(name: String, metaModel: LinearMetaModel<Flt64>, toLogModel: Boolean, registrationStatusCallBack: RegistrationStatusCallBack?, solvingStatusCallBack: SolvingStatusCallBack?): Ret<ColumnGenerationSolver.LPResult> {
            val best = enumerate(metaModel).minByOrNull { it.first } ?: return Failed(ErrorCode.ORModelInfeasible, "enumerated infeasible")
            // 整数网格不是精确 LP；测试不得声称取得 LP 最优证书。
            // An integer grid is not an exact LP solver and must not claim an LP certificate.
            return Ok(ColumnGenerationSolver.LPResult(SolverStatus.Feasible.toSolveReport(objective = best.first, values = best.second, solveTime = Duration.ZERO), emptyMap()))
        }
    }

    companion object {
        private suspend fun enumerate(source: LinearMetaModel<Flt64>): List<Pair<Flt64, List<Flt64>>> {
            val expanded = assertIs<Ok<LinearMechanismModel<Flt64>, *, *>>(LinearMechanismModel(source)).value
            return expanded.use { model ->
                val tokens = model.tokens.tokensInSolver
                val bounds = tokens.map { token ->
                    val low = token.variable.lowerBound?.value?.unwrapOrNull()?.toDouble()?.toInt() ?: 0
                    val high = when {
                        token.name.startsWith("excess_") || token.name.startsWith("shortage_") -> 0
                        // 分区恰选一列已隐含 lambda <= 1，无需枚举无穷的原生上界。
                        // Exactly-one partition rows imply lambda <= 1 despite its unbounded native domain.
                        token.name.startsWith("partition_column_") -> 1
                        else -> token.variable.upperBound?.value?.unwrapOrNull()?.toDouble()?.toInt() ?: 1
                    }
                    low..high
                }
                val values = DoubleArray(tokens.size)
                val rows = model.constraints.map { row ->
                    Triple(row.lhs.map { cell ->
                        cell as LinearCell<Flt64>
                        model.tokens.indexOf(cell.token.variable)!! to cell.coefficient.toDouble()
                    }, row.sign, row.rhs.toDouble())
                }
                val objectiveTerms = model.objectFunction.subObjects.flatMap { part ->
                    part.linearTerms().map { (coefficient, variable) -> model.tokens.indexOf(variable)!! to coefficient.toDouble() }
                }
                val objectiveConstant = model.objectFunction.subObjects.fold(0.0) { total, part -> total + part.constant.toDouble() }
                val results = arrayListOf<Pair<Flt64, List<Flt64>>>()
                fun search(index: Int) {
                    for ((cells, sign, rhs) in rows) {
                        var minimum = 0.0; var maximum = 0.0
                        for ((i, coefficient) in cells) {
                            if (i < index) { minimum += coefficient * values[i]; maximum += coefficient * values[i] }
                            else {
                                val a = coefficient * bounds[i].first; val b = coefficient * bounds[i].last
                                minimum += minOf(a, b); maximum += maxOf(a, b)
                            }
                        }
                        if ((sign != ConstraintRelation.GreaterEqual && minimum > rhs + 1e-6) || (sign != ConstraintRelation.LessEqual && maximum < rhs - 1e-6)) return
                    }
                    if (index == tokens.size) {
                        val objective = objectiveTerms.fold(objectiveConstant) { total, (i, coefficient) -> total + coefficient * values[i] }
                        results.add(Flt64(objective) to values.map { Flt64(it) }); return
                    }
                    for (value in bounds[index]) { values[index] = value.toDouble(); search(index + 1) }
                }
                search(0)
                results
            }
        }
    }
}
