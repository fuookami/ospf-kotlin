package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.toSolverStatus
import fuookami.ospf.kotlin.core.solver.report.*
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.solver.report.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.core.solver.report.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.solver.report.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

class ColumnGenerationSolverValueConversionTest {
    private open class StubSolver(
        private val output: Flt64SolveReport
    ) : ColumnGenerationSolver {
        override val name: String = "stub"
        val milpNames = mutableListOf<String>()
        val milpPoolAmounts = mutableListOf<UInt64>()
        val milpToLogModelFlags = mutableListOf<Boolean>()
        val milpPoolToLogModelFlags = mutableListOf<Boolean>()
        val milpRegistrationCallbacks = mutableListOf<RegistrationStatusCallBack?>()
        val milpSolvingCallbacks = mutableListOf<SolvingStatusCallBack?>()
        val milpPoolRegistrationCallbacks = mutableListOf<RegistrationStatusCallBack?>()
        val milpPoolSolvingCallbacks = mutableListOf<SolvingStatusCallBack?>()
        val lpToLogModelFlags = mutableListOf<Boolean>()
        val lpRegistrationCallbacks = mutableListOf<RegistrationStatusCallBack?>()
        val lpSolvingCallbacks = mutableListOf<SolvingStatusCallBack?>()
        val lpNames = mutableListOf<String>()

        override suspend fun solveMILP(
            name: String,
            metaModel: Flt64LinearMetaModel,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Flt64SolveReport> {
            milpNames.add(name)
            milpToLogModelFlags.add(toLogModel)
            milpRegistrationCallbacks.add(registrationStatusCallBack)
            milpSolvingCallbacks.add(solvingStatusCallBack)
            return Ok(output)
        }

        override suspend fun solveLP(
            name: String,
            metaModel: Flt64LinearMetaModel,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<ColumnGenerationSolver.LPResult> {
            lpNames.add(name)
            lpToLogModelFlags.add(toLogModel)
            lpRegistrationCallbacks.add(registrationStatusCallBack)
            lpSolvingCallbacks.add(solvingStatusCallBack)
            return Ok(ColumnGenerationSolver.LPResult(output, emptyMap()))
        }

        override suspend fun solveMILP(
            name: String,
            metaModel: Flt64LinearMetaModel,
            amount: UInt64,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<Flt64SolveReport, List<List<Flt64>>>> {
            milpPoolAmounts.add(amount)
            milpPoolToLogModelFlags.add(toLogModel)
            milpPoolRegistrationCallbacks.add(registrationStatusCallBack)
            milpPoolSolvingCallbacks.add(solvingStatusCallBack)
            return Ok(Pair(output, listOf(output.values)))
        }
    }

    private fun model(): Flt64LinearMetaModel {
        return LinearMetaModel(name = "t", converter = IntoValue.Identity)
    }

    private fun output(status: SolverStatus = SolverStatus.Optimal): SolveReport<Flt64> {
        return status.toSolveReport(
            objective = Flt64(10.0),
            values = listOf(Flt64(1.0), Flt64(2.0)),
            solveTime = Duration.ZERO,
            bestBound = Flt64(9.0),
            gap = Flt64.zero
        )
    }

    private fun infeasibleOutput(): LinearInfeasibleSolverOutput {
        return LinearInfeasibleSolverOutput(
            iis = BasicLinearTriadModel(
                variables = emptyList(),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix<Flt64>(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "infeasible-iis"
            )
        )
    }

    private val plusOneConverter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64): Flt64 = value + Flt64.one
        override val zero: Flt64 get() = Flt64.zero
        override val one: Flt64 get() = Flt64.one
        override fun fromValue(value: Flt64): Flt64 = value
    }

    @Test
    fun solveMilpAsUsesConverterPipeline() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveMILPAs(
            metaModel = model(),
            converter = IntoValue.Identity
        )
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.values)
        assertEquals(Flt64(10.0), value.solution?.objective)
    }

    @Test
    fun solveMilpAsSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveMILPAs(metaModel = model())
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.values)
        assertEquals(Flt64(10.0), value.solution?.objective)
    }

    @Test
    fun solveMilpAsAsyncSupportsGenericMetaModelInput() {
        val solver = StubSolver(output())
        val result = solver.solveMILPAsAsync(metaModel = model()).get()
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.values)
        assertEquals(Flt64(10.0), value.solution?.objective)
    }

    @Test
    fun solveMilpAsAsyncUsesConverterPipeline() {
        val solver = StubSolver(output())
        val result = solver.solveMILPAsAsync(
            metaModel = model(),
            converter = plusOneConverter
        ).get()
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(2.0), Flt64(3.0)), value.values)
    }

    @Test
    fun solveMilpAsAsyncUsesNameFromOptions() {
        val solver = StubSolver(output())
        solver.solveMILPAsAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(name = "milp-as-async-options")
        ).get()
        assertEquals("milp-as-async-options", solver.milpNames.last())
    }

    @Test
    fun solveMilpAsAsyncForwardsToLogModelFromOptions() {
        val solver = StubSolver(output())
        solver.solveMILPAsAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.milpToLogModelFlags.last())
    }

    @Test
    fun solveMilpAsAsyncForwardsCallbacksFromOptions() {
        val solver = StubSolver(output())
        val registrationStatusCallBack = RegistrationStatusCallBack { _ -> ok }
        val solvingStatusCallBack = SolvingStatusCallBack { _ -> ok }
        solver.solveMILPAsAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        ).get()
        assertSame(registrationStatusCallBack, solver.milpRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.milpSolvingCallbacks.last())
    }

    @Test
    fun solveMilpAsForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubSolver(output())
        val registrationStatusCallBack = RegistrationStatusCallBack { _ -> ok }
        val solvingStatusCallBack = SolvingStatusCallBack { _ -> ok }
        solver.solveMILPAs(
            metaModel = model(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        )
        assertSame(registrationStatusCallBack, solver.milpRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.milpSolvingCallbacks.last())
    }

    @Test
    fun solveLpAsUsesConverterPipeline() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveLPAs(
            metaModel = model(),
            converter = IntoValue.Identity
        )
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.result.values)
        assertEquals(Flt64(10.0), value.result.solution?.objective)
    }

    @Test
    fun solveLpAsSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveLPAs(metaModel = model())
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.result.values)
        assertEquals(Flt64(10.0), value.result.solution?.objective)
    }

    @Test
    fun solveLpAsAsyncSupportsGenericMetaModelInput() {
        val solver = StubSolver(output())
        val result = solver.solveLPAsAsync(metaModel = model()).get()
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.result.values)
        assertEquals(Flt64(10.0), value.result.solution?.objective)
    }

    @Test
    fun solveLpAsAsyncUsesConverterPipeline() {
        val solver = StubSolver(output())
        val result = solver.solveLPAsAsync(
            metaModel = model(),
            converter = plusOneConverter
        ).get()
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(2.0), Flt64(3.0)), value.result.values)
    }

    @Test
    fun solveLpAsAsyncUsesNameFromOptions() {
        val solver = StubSolver(output())
        solver.solveLPAsAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(name = "lp-as-async-options")
        ).get()
        assertEquals("lp-as-async-options", solver.lpNames.last())
    }

    @Test
    fun solveLpAsAsyncForwardsToLogModelFromOptions() {
        val solver = StubSolver(output())
        solver.solveLPAsAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.lpToLogModelFlags.last())
    }

    @Test
    fun solveLpAsAsyncForwardsCallbacksFromOptions() {
        val solver = StubSolver(output())
        val registrationStatusCallBack = RegistrationStatusCallBack { _ -> ok }
        val solvingStatusCallBack = SolvingStatusCallBack { _ -> ok }
        solver.solveLPAsAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        ).get()
        assertSame(registrationStatusCallBack, solver.lpRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.lpSolvingCallbacks.last())
    }

    @Test
    fun solveLpAsForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubSolver(output())
        val registrationStatusCallBack = RegistrationStatusCallBack { _ -> ok }
        val solvingStatusCallBack = SolvingStatusCallBack { _ -> ok }
        solver.solveLPAs(
            metaModel = model(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        )
        assertSame(registrationStatusCallBack, solver.lpRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.lpSolvingCallbacks.last())
    }

    @Test
    fun solveLpWithStatusPreservesNonOptimalStatus() = runBlocking {
        val solver = StubSolver(output(status = SolverStatus.Feasible))
        val result = solver.solveLPWithStatus(
            name = "lp-status",
            metaModel = model()
        )

        val structured = (result as Ok).value
        assertTrue(structured is ColumnGenerationSolver.LPResultWithStatus.Feasible)
        assertEquals(
            SolverStatus.Feasible,
            (structured as ColumnGenerationSolver.LPResultWithStatus.Feasible).result.result.toSolverStatus()
        )
    }

    @Test
    fun solveLpWithStatusPreservesInfeasibleTerminalState() = runBlocking {
        val solver = InfeasibleLpSolver(output(), infeasibleOutput())
        val result = solver.solveLPWithStatus(
            name = "lp-infeasible",
            metaModel = model()
        )

        val structured = (result as Ok).value
        assertTrue(structured is ColumnGenerationSolver.LPResultWithStatus.Infeasible)
        assertEquals(
            "infeasible-iis",
            (structured as ColumnGenerationSolver.LPResultWithStatus.Infeasible).output.iis.name
        )
    }

    @Test
    fun solveMilpWithStatusPreservesNonOptimalStatus() = runBlocking {
        val solver = StubSolver(output(status = SolverStatus.Feasible))
        val result = solver.solveMILPWithStatus(
            name = "milp-status",
            metaModel = model()
        )

        val structured = (result as Ok).value
        assertTrue(structured is ColumnGenerationSolver.MILPSolveResult.Feasible)
        assertEquals(
            SolverStatus.Feasible,
            (structured as ColumnGenerationSolver.MILPSolveResult.Feasible).output.toSolverStatus()
        )
    }

    private class InfeasibleLpSolver(
        output: Flt64SolveReport,
        private val infeasible: LinearInfeasibleSolverOutput
    ) : StubSolver(output) {
        override suspend fun solveLPWithStatus(
            name: String,
            metaModel: Flt64LinearMetaModel,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?,
            iisConfig: IISConfig
        ): Ret<ColumnGenerationSolver.LPResultWithStatus> {
            return Ok(ColumnGenerationSolver.LPResultWithStatus.Infeasible(infeasible))
        }
    }

    @Test
    fun solveMilpAsPoolUsesConverterPipeline() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveMILPWithSolutionPoolAs(
            metaModel = model(),
            converter = IntoValue.Identity,
            options = FrameworkSolveOptions(solutionAmount = UInt64.one)
        )
        val value = (result as Ok).value
        assertEquals(1, value.second.size)
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.second.first())
        assertEquals(UInt64.one, solver.milpPoolAmounts.last())
    }

    @Test
    fun solveMilpAsPoolSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveMILPWithSolutionPoolAs(
            metaModel = model(),
            options = FrameworkSolveOptions(solutionAmount = UInt64.one)
        )
        val value = (result as Ok).value
        assertEquals(1, value.second.size)
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.second.first())
        assertEquals(UInt64.one, solver.milpPoolAmounts.last())
    }

    @Test
    fun solveMilpAsPoolAsyncSupportsGenericMetaModelInput() {
        val solver = StubSolver(output())
        val result = solver.solveMILPWithSolutionPoolAsAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(solutionAmount = UInt64.one)
        ).get()
        val value = (result as Ok).value
        assertEquals(1, value.second.size)
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.second.first())
        assertEquals(UInt64.one, solver.milpPoolAmounts.last())
    }

    @Test
    fun solveMilpAsPoolAsyncUsesConverterPipeline() {
        val solver = StubSolver(output())
        val result = solver.solveMILPWithSolutionPoolAsAsync(
            metaModel = model(),
            converter = plusOneConverter,
            options = FrameworkSolveOptions(solutionAmount = UInt64.one)
        ).get()
        val value = (result as Ok).value
        assertEquals(1, value.second.size)
        assertEquals(listOf(Flt64(2.0), Flt64(3.0)), value.second.first())
        assertEquals(UInt64.one, solver.milpPoolAmounts.last())
    }

    @Test
    fun solveMilpAsPoolUsesDefaultSolutionAmountWhenOptionMissing() = runBlocking {
        val solver = StubSolver(output())
        solver.solveMILPWithSolutionPoolAs(
            metaModel = model(),
            options = FrameworkSolveOptions()
        )
        assertEquals(UInt64.one, solver.milpPoolAmounts.last())
    }

    @Test
    fun solveMilpAsPoolAsyncForwardsConfiguredSolutionAmount() {
        val solver = StubSolver(output())
        val solutionAmount = UInt64(3)
        solver.solveMILPWithSolutionPoolAsAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(solutionAmount = solutionAmount)
        ).get()
        assertEquals(solutionAmount, solver.milpPoolAmounts.last())
    }

    @Test
    fun solveMilpAsPoolAsyncForwardsToLogModelFromOptions() {
        val solver = StubSolver(output())
        solver.solveMILPWithSolutionPoolAsAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.milpPoolToLogModelFlags.last())
    }

    @Test
    fun solveMilpAsPoolAsyncForwardsCallbacksFromOptions() {
        val solver = StubSolver(output())
        val registrationStatusCallBack = RegistrationStatusCallBack { _ -> ok }
        val solvingStatusCallBack = SolvingStatusCallBack { _ -> ok }
        solver.solveMILPWithSolutionPoolAsAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        ).get()
        assertSame(registrationStatusCallBack, solver.milpPoolRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.milpPoolSolvingCallbacks.last())
    }

    @Test
    fun solveMilpAsPoolForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubSolver(output())
        val registrationStatusCallBack = RegistrationStatusCallBack { _ -> ok }
        val solvingStatusCallBack = SolvingStatusCallBack { _ -> ok }
        solver.solveMILPWithSolutionPoolAs(
            metaModel = model(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        )
        assertSame(registrationStatusCallBack, solver.milpPoolRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.milpPoolSolvingCallbacks.last())
    }
}
