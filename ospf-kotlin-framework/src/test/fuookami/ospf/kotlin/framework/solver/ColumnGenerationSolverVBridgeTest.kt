package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class ColumnGenerationSolverVBridgeTest {
    private class StubSolver(
        private val output: Flt64FeasibleSolverOutput
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
        ): Ret<Flt64FeasibleSolverOutput> {
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
        ): Ret<Pair<Flt64FeasibleSolverOutput, List<List<Flt64>>>> {
            milpPoolAmounts.add(amount)
            milpPoolToLogModelFlags.add(toLogModel)
            milpPoolRegistrationCallbacks.add(registrationStatusCallBack)
            milpPoolSolvingCallbacks.add(solvingStatusCallBack)
            return Ok(Pair(output, listOf(output.solution)))
        }
    }

    private fun model(): Flt64LinearMetaModel {
        return LinearMetaModel(name = "t", converter = IntoValue.Identity)
    }

    private fun output(): FeasibleSolverOutput<Flt64> {
        return FeasibleSolverOutput(
            obj = Flt64(10.0),
            solution = listOf(Flt64(1.0), Flt64(2.0)),
            time = Duration.ZERO,
            possibleBestObj = Flt64(9.0),
            gap = Flt64.zero
        )
    }

    private val plusOneConverter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64): Flt64 = value + Flt64.one
        override val zero: Flt64 get() = Flt64.zero
        override val one: Flt64 get() = Flt64.one
        override fun fromValue(value: Flt64): Flt64 = value
    }

    @Test
    fun solveMilpVUsesConverterPipeline() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveMILPV(
            metaModel = model(),
            converter = IntoValue.Identity
        )
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.solution)
        assertEquals(Flt64(10.0), value.obj)
    }

    @Test
    fun solveMilpVSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveMILPV(metaModel = model())
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.solution)
        assertEquals(Flt64(10.0), value.obj)
    }

    @Test
    fun solveMilpVAsyncSupportsGenericMetaModelInput() {
        val solver = StubSolver(output())
        val result = solver.solveMILPVAsync(metaModel = model()).get()
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.solution)
        assertEquals(Flt64(10.0), value.obj)
    }

    @Test
    fun solveMilpVAsyncUsesConverterPipeline() {
        val solver = StubSolver(output())
        val result = solver.solveMILPVAsync(
            metaModel = model(),
            converter = plusOneConverter
        ).get()
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(2.0), Flt64(3.0)), value.solution)
    }

    @Test
    fun solveMilpVAsyncUsesNameFromOptions() {
        val solver = StubSolver(output())
        solver.solveMILPVAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(name = "milp-v-async-options")
        ).get()
        assertEquals("milp-v-async-options", solver.milpNames.last())
    }

    @Test
    fun solveMilpVAsyncForwardsToLogModelFromOptions() {
        val solver = StubSolver(output())
        solver.solveMILPVAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.milpToLogModelFlags.last())
    }

    @Test
    fun solveMilpVAsyncForwardsCallbacksFromOptions() {
        val solver = StubSolver(output())
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMILPVAsync(
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
    fun solveMilpVForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubSolver(output())
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMILPV(
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
    fun solveLpVUsesConverterPipeline() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveLPV(
            metaModel = model(),
            converter = IntoValue.Identity
        )
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.solution)
        assertEquals(Flt64(10.0), value.obj)
    }

    @Test
    fun solveLpVSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveLPV(metaModel = model())
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.solution)
        assertEquals(Flt64(10.0), value.obj)
    }

    @Test
    fun solveLpVAsyncSupportsGenericMetaModelInput() {
        val solver = StubSolver(output())
        val result = solver.solveLPVAsync(metaModel = model()).get()
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.solution)
        assertEquals(Flt64(10.0), value.obj)
    }

    @Test
    fun solveLpVAsyncUsesConverterPipeline() {
        val solver = StubSolver(output())
        val result = solver.solveLPVAsync(
            metaModel = model(),
            converter = plusOneConverter
        ).get()
        val value = (result as Ok).value
        assertEquals(listOf(Flt64(2.0), Flt64(3.0)), value.solution)
    }

    @Test
    fun solveLpVAsyncUsesNameFromOptions() {
        val solver = StubSolver(output())
        solver.solveLPVAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(name = "lp-v-async-options")
        ).get()
        assertEquals("lp-v-async-options", solver.lpNames.last())
    }

    @Test
    fun solveLpVAsyncForwardsToLogModelFromOptions() {
        val solver = StubSolver(output())
        solver.solveLPVAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.lpToLogModelFlags.last())
    }

    @Test
    fun solveLpVAsyncForwardsCallbacksFromOptions() {
        val solver = StubSolver(output())
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveLPVAsync(
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
    fun solveLpVForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubSolver(output())
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveLPV(
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
    fun solveMilpVPoolUsesConverterPipeline() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveMILPVWithSolutionPool(
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
    fun solveMilpVPoolSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubSolver(output())
        val result = solver.solveMILPVWithSolutionPool(
            metaModel = model(),
            options = FrameworkSolveOptions(solutionAmount = UInt64.one)
        )
        val value = (result as Ok).value
        assertEquals(1, value.second.size)
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.second.first())
        assertEquals(UInt64.one, solver.milpPoolAmounts.last())
    }

    @Test
    fun solveMilpVPoolAsyncSupportsGenericMetaModelInput() {
        val solver = StubSolver(output())
        val result = solver.solveMILPVWithSolutionPoolAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(solutionAmount = UInt64.one)
        ).get()
        val value = (result as Ok).value
        assertEquals(1, value.second.size)
        assertEquals(listOf(Flt64(1.0), Flt64(2.0)), value.second.first())
        assertEquals(UInt64.one, solver.milpPoolAmounts.last())
    }

    @Test
    fun solveMilpVPoolAsyncUsesConverterPipeline() {
        val solver = StubSolver(output())
        val result = solver.solveMILPVWithSolutionPoolAsync(
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
    fun solveMilpVPoolUsesDefaultSolutionAmountWhenOptionMissing() = runBlocking {
        val solver = StubSolver(output())
        solver.solveMILPVWithSolutionPool(
            metaModel = model(),
            options = FrameworkSolveOptions()
        )
        assertEquals(UInt64.one, solver.milpPoolAmounts.last())
    }

    @Test
    fun solveMilpVPoolAsyncForwardsConfiguredSolutionAmount() {
        val solver = StubSolver(output())
        val solutionAmount = UInt64(3)
        solver.solveMILPVWithSolutionPoolAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(solutionAmount = solutionAmount)
        ).get()
        assertEquals(solutionAmount, solver.milpPoolAmounts.last())
    }

    @Test
    fun solveMilpVPoolAsyncForwardsToLogModelFromOptions() {
        val solver = StubSolver(output())
        solver.solveMILPVWithSolutionPoolAsync(
            metaModel = model(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.milpPoolToLogModelFlags.last())
    }

    @Test
    fun solveMilpVPoolAsyncForwardsCallbacksFromOptions() {
        val solver = StubSolver(output())
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMILPVWithSolutionPoolAsync(
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
    fun solveMilpVPoolForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubSolver(output())
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMILPVWithSolutionPool(
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