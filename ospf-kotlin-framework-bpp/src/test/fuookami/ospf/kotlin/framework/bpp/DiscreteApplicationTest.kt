package fuookami.ospf.kotlin.framework.bpp

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
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.framework.bpp.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp.application.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.*

class DiscreteApplicationTest {
    private fun registration(): RegisteredDiscreteLoading<Flt64> {
        val input = DiscreteLoadingProblem(
            items = listOf("a", "b").map { LoadingItem(it, Quantity(Flt64.one, Kilogram)) },
            positions = listOf("p", "q").map { LoadingPosition(it, UInt64(2), Quantity(Flt64(3), Kilogram)) },
            allowedAssignments = setOf(AssignmentKey("a", "p"), AssignmentKey("b", "p"), AssignmentKey("b", "q")),
            conflicts = emptySet(), layoutGroups = emptyList(), weightUnit = Kilogram
        )
        val context = DiscreteLoadingContext(input, Flt64PackingValueAdapter)
        val model = LinearMetaModel<Flt64>(name = "extension", converter = IntoValue.Identity)
        assertIs<Ok<*, *, *>>(context.register(model))
        // 下游同时追加目标和约束，不修改应用服务。
        // Downstream adds both an objective and a constraint without editing the application service.
        model.addConstraint(context.aggregation.loaded.getValue("a") eq Flt64.one, name = "downstream_required_a")
        model.minimize(-sum(context.aggregation.loaded.values.toList()))
        return RegisteredDiscreteLoading(context, model)
    }

    @Test
    fun `fallback rebuilds the full domain and preserves downstream constraints and objective`(): Unit = runBlocking {
        val registrations = arrayListOf<RegisteredDiscreteLoading<Flt64>>()
        val app = DiscreteLoadingApplicationService<Flt64>(milpSolver = ::enumerate, bendersSolver = FailingBackend())
        val solved = assertIs<Ok<DiscreteLoadingSolveResult<Flt64>, *, *>>(app.solve(
            factory = { Ok(registration().also { registrations.add(it) }) },
            config = DiscreteLoadingSolveConfig(preferBenders = true)
        )).value
        assertEquals(2, registrations.size)
        assertNotSame(registrations[0].context, registrations[1].context)
        assertNotSame(registrations[0].model, registrations[1].model)
        assertEquals("milp_fallback_after_benders", solved.path)
        assertEquals(Flt64(-2), solved.objective)
        assertEquals(2, solved.solution.assignments.size)
    }

    @Test
    fun `final verification rejects a solver vector violating an extra business constraint`(): Unit = runBlocking {
        val app = DiscreteLoadingApplicationService<Flt64>(milpSolver = { model ->
            Ok(SolverStatus.Optimal.toSolveReport(objective = Flt64.zero, values = List(model.tokens.tokens.size) { Flt64.zero }, solveTime = Duration.ZERO, bestBound = Flt64.zero, gap = Flt64.zero))
        })
        assertIs<Failed<*, *, *>>(app.solve(factory = { Ok(registration()) }))
    }

    @Test
    fun `a successful report without an incumbent preserves infeasible versus incomplete status`(): Unit = runBlocking {
        for (status in listOf(SolverStatus.Infeasible, SolverStatus.Feasible)) {
            val app = DiscreteLoadingApplicationService<Flt64>(milpSolver = {
                Ok(status.toSolveReport(solveTime = Duration.ZERO))
            })
            val result = assertIs<Failed<*, *, *>>(app.solve(factory = { Ok(registration()) }))
            assertEquals(if (status == SolverStatus.Infeasible) ErrorCode.ORModelInfeasible else ErrorCode.OREngineSolvingException, result.error.code)
        }
    }

    @Test
    fun `final verification rejects an incorrect objective even for a feasible solver vector`(): Unit = runBlocking {
        val app = DiscreteLoadingApplicationService<Flt64>(milpSolver = { model ->
            val solved = assertIs<Ok<SolveReport<Flt64>, *, *>>(enumerate(model)).value
            Ok(SolverStatus.Optimal.toSolveReport(
                objective = Flt64(999), values = solved.values, solveTime = Duration.ZERO,
                bestBound = Flt64(999), gap = Flt64.zero
            ))
        })
        assertIs<Failed<*, *, *>>(app.solve(factory = { Ok(registration()) }))
    }

    @Test
    fun `fallback rejects reuse of the rejected registration`(): Unit = runBlocking {
        val reused = registration()
        val app = DiscreteLoadingApplicationService<Flt64>(milpSolver = ::enumerate, bendersSolver = FailingBackend())
        assertIs<Failed<*, *, *>>(app.solve(factory = { Ok(reused) }, config = DiscreteLoadingSolveConfig(preferBenders = true)))
    }

    private suspend fun enumerate(model: LinearMetaModel<Flt64>): Ret<SolveReport<Flt64>> {
        val expanded = assertIs<Ok<LinearMechanismModel<Flt64>, *, *>>(LinearMechanismModel(model)).value
        return expanded.use { mechanism ->
            val tokens = mechanism.tokens.tokensInSolver
            var best: List<Flt64>? = null
            var bestValue = Flt64.infinity
            for (mask in 0 until (1 shl tokens.size)) {
                val values = tokens.indices.map { if (mask and (1 shl it) == 0) Flt64.zero else Flt64.one }
                if (mechanism.constraints.any { it.isTrue(values) != true }) continue
                val objective = mechanism.objectFunction.subObjects.fold(Flt64.zero) { total, part ->
                    total + part.linearTerms().fold(part.constant) { acc, (coefficient, variable) -> acc + coefficient * values[mechanism.tokens.indexOf(variable)!!] }
                }
                if (objective < bestValue) { bestValue = objective; best = values }
            }
            Ok(SolverStatus.Optimal.toSolveReport(objective = bestValue, values = best!!, solveTime = Duration.ZERO, bestBound = bestValue, gap = Flt64.zero))
        }
    }

    private class FailingBackend : LinearBendersDecompositionSolver {
        override val name = "controlled-failure"
        override suspend fun solveMaster(name: String, metaModel: LinearMetaModel<Flt64>, toLogModel: Boolean, registrationStatusCallBack: RegistrationStatusCallBack?, solvingStatusCallBack: SolvingStatusCallBack?): Ret<SolverOutput> = Failed(ErrorCode.OREngineSolvingException, "controlled master failure")
        override suspend fun solveSub(name: String, metaModel: LinearMetaModel<Flt64>, objectVariable: AbstractVariableItem<*, *>, fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>, toLogModel: Boolean, registrationStatusCallBack: RegistrationStatusCallBack?, solvingStatusCallBack: SolvingStatusCallBack?): Ret<LinearBendersDecompositionSolver.LinearSubResult> = error("unreachable")
    }
}
