package fuookami.ospf.kotlin.framework.bpp1d.application

import kotlin.test.*
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.solver.Flt64SolveReport
import fuookami.ospf.kotlin.framework.bpp.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.packing.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.pattern_generation.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.pattern_assignment.*

class Bpp1dTest {
    private fun problem() = PackingProblem(
        items = listOf(Item(id = "a", length = Quantity(Flt64(40), Centimeter), weight = Quantity(Flt64(2), Kilogram), amount = UInt64(3), materials = mapOf("steel" to UInt64.one))),
        binTypes = listOf(
            BinType(id = "small", length = Quantity(Flt64(1), Meter), maxWeight = Quantity(Flt64(5), Kilogram), cost = Flt64.one, inventory = UInt64.one),
            BinType(id = "large", length = Quantity(Flt64(2), Meter), maxWeight = Quantity(Flt64(10), Kilogram), cost = Flt64(3), inventory = UInt64.one)
        ),
        spacing = Quantity(Flt64(20), Centimeter)
    )

    @Test
    fun `generation respects spacing weight multiple bins and truncation`() {
        val generator = PatternGenerationContext<Flt64>()
        val generated = assertIs<Ok<PatternGenerationResult<Flt64>, *, *>>(generator.generate(problem())).value
        assertTrue(generated.complete)
        assertEquals(setOf(1uL, 2uL), generated.patterns.filter { it.binTypeId == "small" }.map { it.amounts.getValue("a").toULong() }.toSet())
        assertTrue(generated.patterns.all { problem().validatePattern(it) is Ok })
        assertFalse(assertIs<Ok<PatternGenerationResult<Flt64>, *, *>>(generator.generate(problem(), PatternGenerationConfig(maxNodes = 2))).value.complete)
        assertTrue(assertIs<Ok<List<PackingPattern<Flt64>>, *, *>>(generator.initialPatterns(problem(), true)).value.isNotEmpty())
    }

    @Test
    fun `pattern lifecycle refreshes extensions duals tokens and final extraction`() {
        var registered = false
        var refreshed = 0
        val extension = object : PatternModelingExtension<Flt64> {
            override fun register(context: PatternAssignmentContext<Flt64>): Try { registered = true; return ok }
            override fun refresh(context: PatternAssignmentContext<Flt64>): Try { assertTrue(registered); refreshed++; return ok }
        }
        val model = LinearMetaModel<Flt64>(name = "lifecycle", converter = IntoValue.Identity)
        val context = PatternAssignmentContext(problem = problem(), adapter = Flt64PackingValueAdapter, model = model, stage = PackingModelStage.LP, extensions = listOf(extension))
        val first = PackingPattern<Flt64>("small", mapOf("a" to UInt64.one))
        val second = PackingPattern<Flt64>("small", mapOf("a" to UInt64(2)))
        assertIs<Ok<*, *, *>>(context.register(listOf(first)))
        assertIs<Ok<*, *, *>>(context.addColumns(listOf(second)))
        assertEquals(2, model.objectiveSnapshot().flatMap { it.linearTerms() }.size)
        val dual = MetaDualSolution(model.constraints.filter { it.args == "demand:a" }.associateWith { Flt64(2) }, emptyMap())
        assertEquals(Flt64(-3), context.reducedCost(second, dual))
        val removed = context.aggregation.variables.getValue(first.key)
        assertIs<Ok<*, *, *>>(context.removeColumns(setOf(first.key)))
        assertNull(model.tokens.find(removed))
        val objectiveTerms = model.objectiveSnapshot().flatMap { it.linearTerms() }
        assertEquals(listOf(Flt64.one to context.aggregation.variables.getValue(second.key)), objectiveTerms)
        assertEquals(2, refreshed)
        val values = List(model.tokens.tokens.size) { Flt64.one }
        assertEquals(UInt64.one, assertIs<Ok<Map<PackingPattern<Flt64>, UInt64>, *, *>>(context.extractSolution(values)).value[second])
    }

    @Test
    fun `MILP and column finalization match exhaustive integer optimum and exact materials`(): Unit = runBlocking {
        val solver = ExhaustiveSolver()
        val direct = assertIs<Ok<Bpp1dSolution<Flt64>, *, *>>(Bpp1dMilp(solver, Flt64PackingValueAdapter).solve(problem())).value
        val columns = assertIs<Ok<Bpp1dSolution<Flt64>, *, *>>(Bpp1dColumnGeneration(solver, Flt64PackingValueAdapter).solve(problem())).value
        assertEquals(Flt64.one, direct.objective)
        assertEquals(direct.objective, columns.objective)
        assertEquals("Optimal", direct.status)
        assertEquals(UInt64(3), direct.packing.materials["steel"])
        assertEquals(1, direct.packing.remnants.size)
        val render = assertIs<Ok<RenderSchema1DTO, *, *>>(PackingRendererAdapter(Flt64PackingValueAdapter).toSchema(direct.packing, Centimeter)).value
        listOf(0.0, 60.0, 120.0).zip(render.bins.single().items).forEach { (expected, item) -> assertEquals(expected, item.start, 1e-9) }
        assertEquals(200.0, render.bins.single().length)
    }

    @Test
    fun `no feasible candidate and stock exhaustion fail without an optimal claim`(): Unit = runBlocking {
        val impossible = problem().copy(binTypes = problem().binTypes.map { it.copy(maxWeight = Quantity(Flt64.one, Kilogram)) })
        val candidates = assertIs<Ok<PatternGenerationResult<Flt64>, *, *>>(PatternGenerationContext<Flt64>().generate(impossible)).value
        assertTrue(candidates.patterns.isEmpty())
        assertIs<Failed<*, *, *>>(Bpp1dMilp(ExhaustiveSolver(), Flt64PackingValueAdapter).solve(impossible))
        val constrained = problem().copy(binTypes = problem().binTypes.take(1))
        assertIs<Failed<*, *, *>>(Bpp1dMilp(ExhaustiveSolver(), Flt64PackingValueAdapter).solve(constrained))
    }

    @Test
    fun `FltX solver boundary and downstream extensions participate in every model stage`(): Unit = runBlocking {
        val stages = arrayListOf<PackingModelStage>()
        val extension = object : PatternModelingExtension<FltX> {
            override fun register(context: PatternAssignmentContext<FltX>): Try {
                stages.add(context.stage)
                val bins = sum(context.aggregation.variables.values.toList())
                val limit = context.model.addConstraint(bins leq Flt64.one, name = "downstream_one_bin")
                if (limit !is Ok) return limit
                return context.model.minimize(Flt64(0.25) * bins, name = "downstream_cost")
            }

            override fun extractSolution(context: PatternAssignmentContext<FltX>, values: List<Flt64>): Ret<Map<String, String>> = Ok(mapOf("business" to context.stage.name))
        }
        val input = PackingProblem(
            items = listOf(Item(id = "a", length = Quantity(FltX(40), Centimeter), weight = Quantity(FltX(2), Kilogram), amount = UInt64(3))),
            binTypes = listOf(BinType(id = "bin", length = Quantity(FltX(2), Meter), maxWeight = Quantity(FltX(10), Kilogram), cost = FltX.one)),
            spacing = Quantity(FltX(20), Centimeter)
        )
        val config = Bpp1dSolveConfig(extensions = listOf(extension))
        val direct = assertIs<Ok<Bpp1dSolution<FltX>, *, *>>(Bpp1dMilp(ExhaustiveSolver(), FltXPackingValueAdapter).solve(input, config)).value
        val columns = assertIs<Ok<Bpp1dSolution<FltX>, *, *>>(Bpp1dColumnGeneration(ExhaustiveSolver(), FltXPackingValueAdapter).solve(input, config)).value
        assertEquals(1.25, direct.objective.toDouble(), 1e-9)
        assertEquals(direct.objective, columns.objective)
        assertEquals(listOf(PackingModelStage.MILP, PackingModelStage.LP, PackingModelStage.FINAL_MILP), stages)
        assertEquals("FINAL_MILP", columns.enhancements["business"])
    }

    @Test
    fun `CSV FltX units and malformed contracts are handled at the boundary`() {
        val loader = Bpp1dCsvDatasetLoader<FltX> { text -> text.toDoubleOrNull()?.takeIf { it.isFinite() }?.let { Ok(FltX(it)) } ?: Failed(ErrorCode.IllegalArgument, "number") }
        val input = assertIs<Ok<PackingProblem<FltX>, *, *>>(loader.load(
            itemsText = "id,length,weight,amount\n\"quoted,id\",40,2,2\n",
            binsText = "id,length,max_weight,cost,inventory\nbin,100,5,1,1\n",
            lengthUnit = Centimeter, weightUnit = Kilogram, spacing = Quantity(FltX(20), Centimeter)
        )).value
        val pattern = PackingPattern<FltX>("bin", mapOf("quoted,id" to UInt64(2)))
        val result = assertIs<Ok<PackingResult<FltX>, *, *>>(PackingContext<FltX>().pack(input, mapOf(pattern to UInt64.one))).value
        assertTrue(result.remnants.isEmpty())
        assertIs<Failed<*, *, *>>(loader.load(itemsText = "id,id,weight,amount", binsText = "", lengthUnit = Meter, weightUnit = Kilogram, spacing = Quantity(FltX.zero, Meter)))
    }

    @Test
    fun `tail rearrangement reduces bin count without changing exact demand`(): Unit = runBlocking {
        val input = problem().copy(binTypes = problem().binTypes.map { it.copy(inventory = UInt64(4)) })
        val current = assertIs<Ok<PackingResult<Flt64>, *, *>>(PackingContext<Flt64>().pack(input, mapOf(PackingPattern<Flt64>("small", mapOf("a" to UInt64.one)) to UInt64(3)))).value
        val optimized = assertIs<Ok<PackingResult<Flt64>, *, *>>(TailBinOptimizer(Bpp1dMilp(ExhaustiveSolver(), Flt64PackingValueAdapter)).optimize(problem = input, current = current, window = 3)).value
        assertEquals(1, optimized.plans.size)
        assertEquals(current.materials, optimized.materials)
    }

    private class ExhaustiveSolver : ColumnGenerationSolver {
        override val name = "bounded-test-enumeration"
        override suspend fun solveMILP(name: String, metaModel: LinearMetaModel<Flt64>, toLogModel: Boolean, registrationStatusCallBack: RegistrationStatusCallBack?, solvingStatusCallBack: SolvingStatusCallBack?): Ret<Flt64SolveReport> {
            val mechanism = when (val result = LinearMechanismModel(metaModel)) { is Ok -> result.value; is Failed -> return Failed(result.error); is Fatal -> return Fatal(result.errors) }
            return mechanism.use { model ->
                val tokens = model.tokens.tokensInSolver
                val values = MutableList(tokens.size) { Flt64.zero }
                var best: List<Flt64>? = null
                var objective = Double.POSITIVE_INFINITY
                fun enumerate(i: Int) {
                    if (i == tokens.size) {
                        if (model.constraints.any { it.isTrue(values) != true }) return
                        val cost = model.objectFunction.subObjects.fold(Flt64.zero) { acc, part ->
                            acc + part.linearTerms().fold(part.constant) { total, (coefficient, variable) -> total + coefficient * values[model.tokens.indexOf(variable)!!] }
                        }.toDouble()
                        if (cost < objective) { objective = cost; best = values.toList() }
                        return
                    }
                    val lower = tokens[i].variable.lowerBound?.value?.unwrapOrNull()?.toDouble()?.toInt() ?: 0
                    val upper = tokens[i].variable.upperBound?.value?.unwrapOrNull()?.toDouble()?.toInt() ?: 0
                    require(upper <= 20)
                    for (value in lower..upper) { values[i] = Flt64(value); enumerate(i + 1) }
                }
                enumerate(0)
                val incumbent = best ?: return@use Failed(ErrorCode.ORModelInfeasible, "enumerated infeasible")
                Ok(SolverStatus.Optimal.toSolveReport(objective = Flt64(objective), values = incumbent, solveTime = Duration.ZERO, bestBound = Flt64(objective), gap = Flt64.zero))
            }
        }

        override suspend fun solveLP(name: String, metaModel: LinearMetaModel<Flt64>, toLogModel: Boolean, registrationStatusCallBack: RegistrationStatusCallBack?, solvingStatusCallBack: SolvingStatusCallBack?): Ret<ColumnGenerationSolver.LPResult> {
            return when (val result = solveMILP(name, metaModel, toLogModel, registrationStatusCallBack, solvingStatusCallBack)) {
                is Ok -> Ok(ColumnGenerationSolver.LPResult(result.value, emptyMap()))
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
    }
}
