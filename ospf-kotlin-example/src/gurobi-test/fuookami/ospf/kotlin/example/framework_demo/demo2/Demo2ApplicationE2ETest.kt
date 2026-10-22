package fuookami.ospf.kotlin.example.framework_demo.demo2

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiLinearBendersDecompositionSolver
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiColumnGenerationSolver
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.framework.bpp.application.*
import fuookami.ospf.kotlin.framework.bpp.infrastructure.Flt64PackingValueAdapter
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.DiscreteLoadingContext
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.*
import fuookami.ospf.kotlin.framework.bpp.domain.partition_loading.*
import fuookami.ospf.kotlin.example.solveLinearMetaModel
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 验证 Demo2 应用在 Gurobi 下的端到端求解路径 / Verifies Demo2 application end-to-end solve paths with Gurobi. */
class Demo2ApplicationE2EIntegration {
    @Test
    fun `partition columns complete pure estimated and integer recommended weights without phantom empty occupancy`(): Unit = runBlocking {
        for (integer in listOf(false, true)) {
            fun registration(): RegisteredDiscreteLoading<Flt64> {
                val context = DiscreteLoadingContext(
                    problem = DiscreteLoadingProblem<Flt64>(
                        items = emptyList(), positions = listOf("a", "b").map { LoadingPosition(it, UInt64.one, Quantity(Flt64(3), Kilogram)) },
                        allowedAssignments = emptySet(), conflicts = emptySet(), layoutGroups = emptyList(), weightUnit = Kilogram
                    ), adapter = Flt64PackingValueAdapter
                )
                val model = LinearMetaModel<Flt64>(name = "pure-weight-$integer", converter = IntoValue.Identity)
                assertTrue(context.register(model) is Ok)
                val resources = listOf("a", "b").associateWith { id ->
                    val variable: AbstractVariableItem<*, *> = if (integer) UIntVar("weight_$id").also { it.range.leq(UInt64(3)) }
                        else URealVar("weight_$id").also { it.range.leq(Flt64(3)) }
                    model.add(variable)
                    context.linkAdditionalWeight(positionId = id, additionalWeight = LinearPolynomial(variable), model = model)
                    variable
                }
                model.addConstraint(sum(resources.values.toList()) eq Flt64(if (integer) 1.0 else 1.5), name = "global_weight_target")
                model.minimize(LinearPolynomial(resources.getValue("a")) + LinearPolynomial(Flt64(2) * resources.getValue("b")))
                return RegisteredDiscreteLoading(context = context, model = model, additionalWeights = { values ->
                    Ok(resources.mapValues { (_, variable) -> values[model.tokens.indexOf(variable)!!] })
                })
            }
            val direct = DiscreteLoadingApplicationService<Flt64>(milpSolver = { solveLinearMetaModel(LinearSolverBuilder(), it) }).solve(factory = { Ok(registration()) })
            assertTrue(direct is Ok, direct.toString())
            val registered = registration()
            val resources = registered.model.tokens.tokens.filter { it.name.startsWith("weight_") }.map { token ->
                val id = token.name.removePrefix("weight_")
                PartitionDecision("weight:$id", id, token.variable)
            }
            val generated = PartitionColumnGenerationApplicationService<Flt64>(GurobiColumnGenerationSolver(
                config = SolverConfig(dumpIntermediateModelBounds = false, dumpIntermediateModelForceBounds = false)
            )).solve(
                registration = registered,
                partitions = listOf(LoadingPartition("a", setOf("a")), LoadingPartition("b", setOf("b"))),
                extraDecisions = resources
            )
            assertTrue(generated is Ok, "integer=$integer: ${when (generated) { is Failed -> generated.error.message; is Fatal -> generated.errors; else -> generated }}")
            val solved = (generated as Ok<PartitionColumnGenerationResult<Flt64>, *, *>).value
            assertEquals((direct as Ok<DiscreteLoadingSolveResult<Flt64>, *, *>).value.objective.toDouble(), solved.loading.objective.toDouble(), 1e-6)
            assertEquals("Feasible", solved.loading.status)
            assertTrue(solved.loading.solution.occupiedPositions.isNotEmpty())
            assertTrue(solved.loading.solution.assignments.isEmpty())
            assertTrue(solved.trace.any { it.addedColumns > 0 })
            println("partition_resource,integer=$integer,columns=${solved.columnCount},iterations=${solved.trace.size},root_lp_converged=${solved.lpConverged},objective=${solved.loading.objective}")
        }
    }

    @Test
    fun `fixed seed sparse and dense baselines have equal real solve objectives`(): Unit = runBlocking {
        val random = kotlin.random.Random(28817)
        val items = List(10) { LoadingItem("cargo-$it", Quantity(Flt64(1 + random.nextInt(3)), Kilogram)) }
        val positions = List(20) { LoadingPosition("position-$it", UInt64(2), Quantity(Flt64(5), Kilogram)) }
        val legal = items.flatMapIndexed { i, item -> listOf(AssignmentKey(item.id, positions[i].id), AssignmentKey(item.id, positions[i + 1].id)) }.toSet()
        val conflicts = (0 until 10).map { PositionConflict(positions[it * 2].id, positions[it * 2 + 1].id) }.toSet()
        val objectives = arrayListOf<Double>()
        for (dense in listOf(false, true)) {
            for (benders in listOf(false, true)) {
                var variables = 0
                var constraints = 0
                var buildNanos = 0L
                val app = DiscreteLoadingApplicationService<Flt64>(
                    milpSolver = { model -> solveLinearMetaModel(LinearSolverBuilder(), model) },
                    bendersSolver = GurobiLinearBendersDecompositionSolver()
                )
                val start = System.nanoTime()
                val result = app.solve(
                    factory = {
                        val buildStart = System.nanoTime()
                        val context = DiscreteLoadingContext(
                            problem = DiscreteLoadingProblem(
                                items = items, positions = positions,
                                allowedAssignments = if (dense) items.flatMap { item -> positions.map { AssignmentKey(item.id, it.id) } }.toSet() else legal,
                                conflicts = conflicts, layoutGroups = emptyList(), weightUnit = Kilogram
                            ),
                            adapter = Flt64PackingValueAdapter
                        )
                        val model = LinearMetaModel<Flt64>(name = "scale-$dense-$benders", converter = IntoValue.Identity)
                        when (val registered = context.register(model)) {
                            is Failed -> return@solve Failed(registered.error)
                            is Fatal -> return@solve Fatal(registered.errors)
                            is Ok -> {}
                        }
                        context.aggregation.assignments.filterKeys { it !in legal }.values.forEach { it.range.eq(false) }
                        model.minimize(-sum(context.aggregation.loaded.values.toList()))
                        variables = model.tokens.tokens.size
                        constraints = model.constraints.size
                        buildNanos = System.nanoTime() - buildStart
                        Ok(RegisteredDiscreteLoading(context, model))
                    },
                    config = DiscreteLoadingSolveConfig(preferBenders = benders, fallbackToMilp = false)
                )
                assertTrue(result is Ok, result.toString())
                val solved = (result as Ok<DiscreteLoadingSolveResult<Flt64>, *, *>).value
                objectives.add(solved.objective.toDouble())
                assertTrue(solved.solution.assignments.all { AssignmentKey(it.key, it.value) in legal })
                println("bpp_solve_scale,seed=28817,dense=$dense,benders=$benders,legal=${legal.size},variables=$variables,constraints=$constraints,conflict_constraints=${conflicts.size},build_ns=$buildNanos,elapsed_ns=${System.nanoTime() - start},iterations=${solved.trace.size},cuts=${solved.trace.fold(0) { n, it -> n + it.cuts }},objective=${solved.objective}")
            }
        }
        assertTrue(objectives.all { kotlin.math.abs(it - objectives.first()) < 1e-6 })
    }

    @Test
    fun `real backend preserves fixed decision objective and native recourse bounds`(): Unit = runBlocking {
        val x = BinVar("decision")
        val theta = URealVar("theta")
        val y = URealVar("recourse")
        y.range.leq(Flt64(3))
        val master = LinearMetaModel<Flt64>(name = "optimal-master", converter = IntoValue.Identity)
        val sub = LinearMetaModel<Flt64>(name = "optimal-sub", converter = IntoValue.Identity)
        master.add(x); master.add(theta)
        master.minimize(LinearPolynomial(theta))
        // 不同注册顺序必须由变量身份绑定。 / Bind by variable identity despite different registration orders.
        sub.add(y); sub.add(x)
        sub.addConstraint(LinearPolynomial(y) geq LinearPolynomial(Flt64(2)) - LinearPolynomial(x))
        sub.minimize(LinearPolynomial(Flt64(3)) + LinearPolynomial(Flt64(2) * x) + LinearPolynomial(Flt64(4) * y))
        val result = LinearBendersAlgorithm(GurobiLinearBendersDecompositionSolver()).solve(LinearBendersModels(
            master = master, sub = sub, bindings = mapOf("cargo-decision" to x), theta = theta
        ))
        assertTrue(result is Ok, result.toString())
        val solved = (result as Ok<LinearBendersResult, *, *>).value
        assertEquals(9.0, solved.objective.toDouble(), 1e-6)
        assertEquals(Flt64.one, solved.values[x])
        assertEquals(Flt64.one, solved.values[y])
        assertTrue(solved.iterations.any { it.cuts > 0 })
    }

    @Test
    fun `real backend Farkas cut excludes infeasible master using native upper bound`(): Unit = runBlocking {
        val x = BinVar("feasible-decision")
        val theta = URealVar("unused-projection")
        theta.range.eq(Flt64.zero)
        val y = URealVar("bounded-recourse")
        y.range.leq(Flt64.one)
        val master = LinearMetaModel<Flt64>(name = "feasible-master", converter = IntoValue.Identity)
        val sub = LinearMetaModel<Flt64>(name = "feasible-sub", converter = IntoValue.Identity)
        master.add(x); master.add(theta)
        master.minimize(LinearPolynomial(x))
        sub.add(y); sub.add(x)
        sub.addConstraint(LinearPolynomial(y) geq LinearPolynomial(Flt64(2)) - LinearPolynomial(Flt64(2) * x))
        sub.minimize(LinearPolynomial())
        val result = LinearBendersAlgorithm(GurobiLinearBendersDecompositionSolver()).solve(LinearBendersModels(
            master = master, sub = sub, bindings = mapOf("fixed-load" to x), theta = theta, feasibilityOnly = true
        ))
        assertTrue(result is Ok, result.toString())
        val solved = (result as Ok<LinearBendersResult, *, *>).value
        assertEquals(1.0, solved.objective.toDouble(), 1e-6)
        assertEquals(Flt64.one, solved.values[x])
        assertTrue(solved.iterations.first().cuts > 0)
    }

    @Test
    fun `all applications solve a minimal feasible request through direct MILP`() = runBlocking {
        for ((path, result) in applications(directFeasibleRequest(), withRender = true)) {
            val (response, render) = result

            assertTrue(response.succeed, "$path direct MILP failed: ${response.notes}")
            assertEquals("Optimal", response.status)
            if (path != "predistribution") {
                assertTrue(response.assignments.isNotEmpty(), "$path direct MILP assignments: $response")
            }
            assertTrue(response.notes.contains("solver_path=milp_direct"), "$path solver notes: ${response.notes}")
            assertNotNull(render)
            if (path != "predistribution") {
                assertTrue(render?.positions?.isNotEmpty() == true)
            }
        }
    }

    @Test
    fun `all direct applications retain the default minimum payload ratio`() = runBlocking {
        val request = RequestDTO.sample().copy(
            id = "demo2-default-min-payload",
            cargos = listOf(
                CargoInput(
                    name = "C-default-min",
                    weight = 8.0,
                    priority = 10,
                    source = "S1",
                    destination = "D1"
                )
            ),
            payloadUpperBound = 10.0
        )
        assertEquals(0.6, request.minPayloadRatio, 1e-9)

        for ((path, result) in applications(request)) {
            val response = result.first
            assertTrue(response.succeed, "$path default minimum payload failed: ${response.notes}")
            assertEquals("Optimal", response.status)
            assertTrue(response.notes.contains("solver_path=milp_direct"))
        }
    }

    @Test
    fun `all applications fall back to direct MILP after a Benders failure`() = runBlocking {
        for ((path, result) in applications(bendersFailureRequest(fallbackToMilp = true), withRender = true)) {
            val (response, render) = result

            assertTrue(response.succeed, "$path Benders fallback failed: ${response.notes}")
            assertEquals("Optimal", response.status)
            assertTrue(response.notes.any { it.contains("Benders failed, falling back to MILP") })
            assertTrue(response.notes.contains("solver_path=milp_fallback_after_benders"))
            assertNotNull(render)
        }
    }

    @Test
    fun `all applications report Benders failure when fallback is disabled`() = runBlocking {
        for ((path, result) in applications(bendersFailureRequest(fallbackToMilp = false))) {
            val (response, render) = result

            assertFalse(response.succeed, "$path should expose the Benders failure")
            assertEquals("NotConverged", response.status)
            assertTrue(response.notes.any { it.contains("Benders requested") })
            assertNull(render)
        }
    }

    @Test
    fun `full-load application solves a minimal request through Benders`() = runBlocking {
        val (response, render) = FullLoadAlgorithm(bendersSuccessRequest(), withRender = true)

        assertTrue(response.succeed, "Benders full-load failed: ${response.notes}")
        assertEquals("Optimal", response.status)
        assertTrue(response.notes.contains("solver_path=benders"))
        assertNotNull(render)
    }

    @Test
    fun `all applications solve a minimal request through Benders`() = runBlocking {
        for ((path, result) in applications(bendersSuccessRequest(), withRender = true)) {
            val (response, render) = result

            assertTrue(response.succeed, "$path Benders solve failed: ${response.notes}")
            assertEquals("Optimal", response.status)
            assertTrue(response.notes.contains("solver_path=benders"))
            assertNotNull(render)
        }
    }

    @Test
    fun `four aircraft and three modes have equivalent MILP Benders and partition CG objectives`() = runBlocking {
        for (type in AircraftTypeInput.entries.filter { it != AircraftTypeInput.Unknown }) {
            val narrow = type == AircraftTypeInput.B737 || type == AircraftTypeInput.B757
            val sample = directFeasibleRequest()
            val first = sample.positions[0].copy(
                geometry = PositionGeometryInput(0.0, 2.0, -2.0, 0.0),
                maxLoadCount = 2
            )
            val second = sample.positions[1].copy(
                geometry = if (narrow) PositionGeometryInput(2.0, 4.0, -2.0, 0.0) else PositionGeometryInput(0.0, 2.0, 0.0, 2.0),
                maxLoadCount = 2
            )
            val large = first.copy(
                name = "large",
                geometry = if (narrow) PositionGeometryInput(0.0, 4.0, -2.0, 0.0) else PositionGeometryInput(0.0, 2.0, -2.0, 2.0),
                longitudinalArm = 0.0,
                lateralArm = 0.0,
                loadingOrder = 2
            )
            val request = sample.copy(
                id = "equivalence-${type.name}",
                aircraftType = type,
                positions = listOf(first, second, large),
                cargos = sample.cargos + sample.cargos.single().copy(name = "C-second"),
                layoutGroups = listOf(LayoutGroupInput(
                    id = "deck",
                    positions = setOf("P1", "P2", "large"),
                    layouts = listOf(LoadingLayoutInput("small", setOf("P1", "P2")), LoadingLayoutInput("large", setOf("large")))
                ))
            )
            val direct = applications(request).toMap()
            val decomposed = applications(request.copy(
                solvePolicy = SolvePolicy(preferBenders = true, bendersFallbackToMilp = false),
                bendersAdaptive = BendersAdaptiveConfig(minBinaryVariables = 1, maxIterations = 64, tolerance = 1e-6)
            )).toMap()
            val generated = applications(request.copy(solvePolicy = SolvePolicy(partitionColumnGeneration = true))).toMap()
            for ((mode, result) in direct) {
                val milp = result.first
                val benders = decomposed.getValue(mode).first
                val columns = generated.getValue(mode).first
                assertTrue(milp.succeed, "$type/$mode MILP: ${milp.notes}")
                assertTrue(benders.succeed, "$type/$mode Benders: ${benders.notes}")
                assertTrue(columns.succeed, "$type/$mode partition CG: ${columns.notes}")
                assertEquals("Optimal", milp.status)
                assertEquals("Optimal", benders.status)
                assertEquals(milp.objective!!, benders.objective!!, 1e-5, "$type/$mode objective")
                assertEquals(milp.objective!!, columns.objective!!, 1e-5, "$type/$mode CG objective")
                assertEquals("Feasible", columns.status)
                assertEquals(benders.frameworkPath, columns.frameworkPath)
                assertTrue(columns.notes.contains("solver_path=partition_column_generation"))
                assertEquals(if (narrow) "bpp1d" else "bpp2d", benders.frameworkPath)
                assertTrue(benders.notes.contains("solver_path=benders"))
                assertEquals(setOf("deck"), benders.selectedLayouts.keys)
                assertFalse("large" in benders.occupiedPositions && benders.occupiedPositions.any { it == "P1" || it == "P2" })
                println("demo2_equivalence,aircraft=$type,mode=$mode,milp=${milp.objective},benders=${benders.objective},cg=${columns.objective},cg_trace=${columns.notes},trace=${benders.notes}")
            }
        }
    }

    private suspend fun applications(
        request: RequestDTO,
        withRender: Boolean = false
    ): List<Pair<String, Pair<ResponseDTO, RenderDTO?>>> {
        return listOf(
            "full-load" to FullLoadAlgorithm(request, withRender = withRender),
            "predistribution" to PredistributionApplication()(request, withRender = withRender),
            "weight-recommendation" to WeightRecommendationApplication()(request, withRender = withRender)
        )
    }

    private fun directFeasibleRequest(): RequestDTO {
        return RequestDTO.sample().copy(
            id = "demo2-direct-feasible",
            cargos = listOf(
                CargoInput(
                    name = "C-direct",
                    weight = 1.0,
                    priority = 10,
                    source = "S1",
                    destination = "D1"
                )
            ),
            payloadUpperBound = 10.0,
            minPayloadRatio = 0.0
        )
    }

    private fun bendersFailureRequest(fallbackToMilp: Boolean): RequestDTO {
        return directFeasibleRequest().copy(
            solvePolicy = SolvePolicy(
                preferBenders = true,
                bendersFallbackToMilp = fallbackToMilp
            ),
            bendersAdaptive = BendersAdaptiveConfig(
                minBinaryVariables = 1,
                maxIterations = 0,
                tolerance = 1e-6
            )
        )
    }

    private fun bendersSuccessRequest(): RequestDTO {
        return directFeasibleRequest().copy(
            solvePolicy = SolvePolicy(
                preferBenders = true,
                bendersFallbackToMilp = false
            ),
            bendersAdaptive = BendersAdaptiveConfig(
                minBinaryVariables = 1,
                maxIterations = 64,
                tolerance = 1e-6
            )
        )
    }
}
