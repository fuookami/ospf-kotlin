package fuookami.ospf.kotlin.framework.solver

import kotlin.test.*
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

class LinearBendersAlgorithmTest {
    private fun model(name: String) = LinearMetaModel<Flt64>(name = name, converter = IntoValue.Identity)
    private fun evaluate(poly: LinearPolynomial<Flt64>, values: Map<AbstractVariableItem<*, *>, Flt64>): Flt64 =
        poly.monomials.fold(poly.constant) { acc, term -> acc + term.coefficient * values.getValue(term.symbol as AbstractVariableItem<*, *>) }

    @Test
    fun `anchored optimality cuts include objective constants bounds and direct fixed cost`(): Unit = runBlocking {
        val x = BinVar("x")
        val y = URealVar("y")
        y.range.geq(Flt64(2))
        val theta = URealVar("theta")
        val sub = model("optimal_cut")
        sub.add(x); sub.add(y)
        sub.addConstraint(LinearPolynomial(y) + LinearPolynomial(x) geq Flt64(3), name = "recourse")
        val expanded = assertIs<Ok<LinearMechanismModel<Flt64>, *, *>>(LinearMechanismModel(sub)).value
        expanded.use {
            val cut = LinearBendersCuts.optimal(
                theta = theta, fixed = mapOf(x to Flt64.zero), dual = mapOf((it.constraints.single() as Constraint<Flt64, fuookami.ospf.kotlin.math.symbol.Linear>) to Flt64.one),
                objective = LinearPolynomial(Flt64(2) * x) + LinearPolynomial(y) + Flt64(5), value = Flt64(8)
            )
            assertEquals(Flt64(8), evaluate(cut.rhs, mapOf(x to Flt64.zero)))
            assertEquals(Flt64(9), evaluate(cut.rhs, mapOf(x to Flt64.one)))
        }
    }

    @Test
    fun `Farkas cuts include supporting variable bounds and reject invalid ray signs`(): Unit = runBlocking {
        val x = BinVar("x")
        val y = URealVar("y")
        y.range.leq(Flt64(2))
        val sub = model("farkas")
        sub.add(x); sub.add(y)
        sub.addConstraint(LinearPolynomial(x) + LinearPolynomial(y) geq Flt64(3), name = "recourse")
        assertIs<Ok<LinearMechanismModel<Flt64>, *, *>>(LinearMechanismModel(sub)).value.use {
            val row = it.constraints.single()
            @Suppress("UNCHECKED_CAST")
            val dual = mapOf((row as Constraint<Flt64, fuookami.ospf.kotlin.math.symbol.Linear>) to -Flt64.one)
            val cut = assertIs<Ok<LinearInequality<Flt64>, *, *>>(LinearBendersCuts.feasible(mapOf(x to Flt64.zero), dual)).value
            assertTrue(evaluate(cut.lhs, mapOf(x to Flt64.zero)) > cut.rhs.constant)
            assertTrue(evaluate(cut.lhs, mapOf(x to Flt64.one)) <= cut.rhs.constant)
            assertIs<Failed<*, *, *>>(LinearBendersCuts.feasible(mapOf(x to Flt64.zero), dual.mapValues { Flt64.one }))
        }
    }

    @Test
    fun `partition binds every integer and remaps current values across different token orders`(): Unit = runBlocking {
        val x = BinVar("decision")
        val y = URealVar("recourse")
        y.range.leq(Flt64(2))
        val source = model("full")
        source.add(y); source.add(x)
        source.addConstraint(LinearPolynomial(x) + LinearPolynomial(y) geq Flt64(3), name = "recourse")
        source.minimize(LinearPolynomial(x))
        val partition = assertIs<Ok<LinearBendersModels, *, *>>(LinearBendersPartition.build(source)).value
        assertEquals(setOf(x), partition.bindings.values.toSet())
        val backend = FeasibilityBackend(x, y)
        val result = assertIs<Ok<LinearBendersResult, *, *>>(LinearBendersAlgorithm(backend).solve(partition)).value
        assertEquals(listOf(Flt64.zero, Flt64.one), backend.fixedHistory)
        assertEquals(Flt64.one, result.objective)
        assertEquals(Flt64(2), result.values[y])
        assertEquals("Optimal", result.status)
        assertEquals(1, result.iterations.fold(0) { acc, iteration -> acc + iteration.cuts })
        assertIs<Ok<*, *, *>>(partition.verify(result.values))
        assertIs<Failed<*, *, *>>(partition.verify(result.values + (y to Flt64(3))))
    }

    @Test
    fun `proven infeasible master reports are not mistaken for missing time limited incumbents`(): Unit = runBlocking {
        val x = BinVar("infeasible-decision")
        val y = URealVar("unused-recourse")
        val source = model("infeasible-master")
        source.add(x); source.add(y)
        source.minimize(LinearPolynomial(x))
        val partition = assertIs<Ok<LinearBendersModels, *, *>>(LinearBendersPartition.build(source)).value
        val backend = object : LinearBendersDecompositionSolver {
            override val name = "infeasible-master-report"
            override suspend fun solveMaster(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<SolverOutput> = Ok(SolverStatus.Infeasible.toSolveReport(solveTime = Duration.ZERO))

            override suspend fun solveSub(
                name: String,
                metaModel: LinearMetaModel<Flt64>,
                objectVariable: AbstractVariableItem<*, *>,
                fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<LinearBendersDecompositionSolver.LinearSubResult> = error("An infeasible master must never reach recourse")
        }
        val result = assertIs<Failed<*, *, *>>(LinearBendersAlgorithm(backend).solve(partition))
        assertEquals(fuookami.ospf.kotlin.utils.error.ErrorCode.ORModelInfeasible, result.error.code)
    }

    @Test
    fun `extensions explicitly place continuous objectives in recourse but never integers`(): Unit = runBlocking {
        val x = BinVar("master-extension")
        val y = URealVar("recourse-extension")
        y.range.leq(Flt64(3))
        val source = model("staged-extension")
        source.add(y); source.add(x)
        source.minimize(LinearPolynomial(Flt64(5)) + LinearPolynomial(x) - LinearPolynomial(Flt64(2) * y))
        val partition = assertIs<Ok<LinearBendersModels, *, *>>(LinearBendersPartition.build(source, mapOf(y to LinearBendersStage.ContinuousRecourse))).value
        assertFalse(partition.feasibilityOnly)
        assertEquals(setOf(x), partition.bindings.values.toSet())
        assertEquals(Flt64(-6), partition.theta.lowerBound!!.value.unwrapOrNull())
        assertEquals(listOf(Flt64(-2) to y), partition.sub.objectiveSnapshot().flatMap { it.linearTerms() })
        assertEquals(Flt64(5), partition.master.objectiveSnapshot().single().constant)
        assertIs<Failed<*, *, *>>(LinearBendersPartition.build(source, mapOf(x to LinearBendersStage.ContinuousRecourse)))
    }

    @Test
    fun `repeated infeasibility certificates are deduplicated and trigger a stall`(): Unit = runBlocking {
        val x = BinVar("decision")
        val y = URealVar("recourse")
        y.range.leq(Flt64(2))
        val source = model("stall")
        source.add(x); source.add(y)
        source.addConstraint(LinearPolynomial(x) + LinearPolynomial(y) geq Flt64(3), name = "recourse")
        source.minimize(LinearPolynomial(x))
        val partition = assertIs<Ok<LinearBendersModels, *, *>>(LinearBendersPartition.build(source)).value
        val backend = FeasibilityBackend(x, y, repeatInfeasible = true)
        assertIs<Failed<*, *, *>>(LinearBendersAlgorithm(backend).solve(partition, LinearBendersConfig(stallLimit = 2)))
        assertEquals(3, backend.calls)
        assertEquals(1, partition.master.constraints.filterIsInstance<LinearInequalityConstraint<Flt64>>().count { it.constraintName.startsWith("benders_cut_") })
    }

    @Test
    fun `time-limited feasible master is never promoted to optimal`(): Unit = runBlocking {
        val x = BinVar("decision")
        val y = URealVar("recourse")
        y.range.leq(Flt64(2))
        val source = model("limited")
        source.add(x); source.add(y)
        source.addConstraint(LinearPolynomial(x) + LinearPolynomial(y) geq Flt64(3), name = "recourse")
        source.minimize(LinearPolynomial(x))
        val partition = assertIs<Ok<LinearBendersModels, *, *>>(LinearBendersPartition.build(source)).value
        val result = assertIs<Ok<LinearBendersResult, *, *>>(LinearBendersAlgorithm(FeasibilityBackend(x, y, limited = true)).solve(partition)).value
        assertEquals("Feasible", result.status)
        assertEquals(1.0, result.gap)
    }

    @Test
    fun `backend optimal status does not discard a nonzero proven bound gap`(): Unit = runBlocking {
        val x = BinVar("decision")
        val y = URealVar("recourse")
        y.range.leq(Flt64(2))
        val source = model("backend-gap")
        source.add(x); source.add(y)
        source.addConstraint(LinearPolynomial(x) + LinearPolynomial(y) geq Flt64(3), name = "recourse")
        source.minimize(LinearPolynomial(x))
        val partition = assertIs<Ok<LinearBendersModels, *, *>>(LinearBendersPartition.build(source)).value
        val result = assertIs<Ok<LinearBendersResult, *, *>>(LinearBendersAlgorithm(FeasibilityBackend(x, y, approximateOptimal = true)).solve(
            models = partition, config = LinearBendersConfig(iterationLimit = 2)
        )).value
        assertEquals("Feasible", result.status)
        assertEquals(1.0, result.gap)
    }

    @Test
    fun `time-limited feasible recourse preserves verified incumbent without an optimality cut`(): Unit = runBlocking {
        val x = BinVar("decision")
        val y = URealVar("recourse")
        y.range.leq(Flt64(2))
        val source = model("limited-recourse")
        source.add(x); source.add(y)
        source.addConstraint(LinearPolynomial(x) + LinearPolynomial(y) geq Flt64(3), name = "recourse")
        source.minimize(LinearPolynomial(x))
        val partition = assertIs<Ok<LinearBendersModels, *, *>>(LinearBendersPartition.build(source)).value
        val result = assertIs<Ok<LinearBendersResult, *, *>>(LinearBendersAlgorithm(FeasibilityBackend(x, y, limitedRecourse = true)).solve(partition)).value
        assertEquals("Feasible", result.status)
        assertEquals("nonoptimal recourse termination", result.termination)
        assertEquals(0, result.iterations.last().cuts)
        assertIs<Ok<*, *, *>>(partition.verify(result.values))
    }

    private class FeasibilityBackend(
        val x: BinVar, val y: URealVar, val repeatInfeasible: Boolean = false, val limited: Boolean = false, val limitedRecourse: Boolean = false,
        val approximateOptimal: Boolean = false
    ) : LinearBendersDecompositionSolver {
        override val name = "test-certificate-backend"
        var calls = 0
        val fixedHistory = arrayListOf<Flt64>()
        override suspend fun solveMaster(name: String, metaModel: LinearMetaModel<Flt64>, toLogModel: Boolean, registrationStatusCallBack: RegistrationStatusCallBack?, solvingStatusCallBack: SolvingStatusCallBack?): Ret<SolverOutput> {
            calls++
            val value = if (limited || approximateOptimal || (!repeatInfeasible && calls > 1)) Flt64.one else Flt64.zero
            val values = metaModel.tokens.tokensInSolver.map { if (it.variable == x) value else Flt64.zero }
            return Ok((if (limited) SolverStatus.Feasible else SolverStatus.Optimal).toSolveReport(
                objective = value, values = values, solveTime = Duration.ZERO, bestBound = if (limited || approximateOptimal) Flt64.zero else value, gap = if (limited || approximateOptimal) Flt64.one else Flt64.zero
            ))
        }

        override suspend fun solveSub(name: String, metaModel: LinearMetaModel<Flt64>, objectVariable: AbstractVariableItem<*, *>, fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>, toLogModel: Boolean, registrationStatusCallBack: RegistrationStatusCallBack?, solvingStatusCallBack: SolvingStatusCallBack?): Ret<LinearBendersDecompositionSolver.LinearSubResult> {
            val current = fixedVariables.getValue(x)
            fixedHistory.add(current)
            if (current == Flt64.zero) {
                return assertIs<Ok<LinearMechanismModel<Flt64>, *, *>>(LinearMechanismModel(metaModel)).value.use { model ->
                    @Suppress("UNCHECKED_CAST")
                    val row = model.constraints.first { it.name == "recourse" } as Constraint<Flt64, fuookami.ospf.kotlin.math.symbol.Linear>
                    Ok(LinearBendersDecompositionSolver.LinearInfeasibleResult(mapOf(row to -Flt64.one), null))
                }
            }
            metaModel.tokens.setSolution(fixedVariables + (y to Flt64(2)))
            return Ok(LinearBendersDecompositionSolver.LinearFeasibleResult(
                result = (if (limitedRecourse) SolverStatus.Feasible else SolverStatus.Optimal).toSolveReport(objective = Flt64.zero, values = listOf(Flt64(2)), solveTime = Duration.ZERO, bestBound = Flt64.zero, gap = Flt64.zero),
                dualSolution = emptyMap(), cuts = null
            ))
        }
    }
}
