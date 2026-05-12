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
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class ColumnGenerationSolverVBridgeTest {
    private class StubSolver(
        private val output: Flt64FeasibleSolverOutput
    ) : ColumnGenerationSolver {
        override val name: String = "stub"

        override suspend fun solveMILP(
            name: String,
            metaModel: Flt64LinearMetaModel,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Flt64FeasibleSolverOutput> {
            return Ok(output)
        }

        override suspend fun solveLP(
            name: String,
            metaModel: Flt64LinearMetaModel,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<ColumnGenerationSolver.LPResult> {
            return Ok(ColumnGenerationSolver.LPResult(output, emptyMap()))
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
    }
}
