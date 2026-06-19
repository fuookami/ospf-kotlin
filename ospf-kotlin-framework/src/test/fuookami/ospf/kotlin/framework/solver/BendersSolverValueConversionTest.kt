package fuookami.ospf.kotlin.framework.solver

import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

class BendersSolverValueConversionTest {
    private class StubBendersSolver : QuadraticBendersDecompositionSolver {
        override val name: String = "stub-benders"
        val linearMasterNames = mutableListOf<String>()
        val linearSubNames = mutableListOf<String>()
        val quadraticMasterNames = mutableListOf<String>()
        val quadraticSubNames = mutableListOf<String>()
        val linearMasterToLogModelFlags = mutableListOf<Boolean>()
        val quadraticMasterToLogModelFlags = mutableListOf<Boolean>()
        val linearSubToLogModelFlags = mutableListOf<Boolean>()
        val quadraticSubToLogModelFlags = mutableListOf<Boolean>()
        val linearMasterRegistrationCallbacks = mutableListOf<RegistrationStatusCallBack?>()
        val linearMasterSolvingCallbacks = mutableListOf<SolvingStatusCallBack?>()
        val quadraticMasterRegistrationCallbacks = mutableListOf<RegistrationStatusCallBack?>()
        val quadraticMasterSolvingCallbacks = mutableListOf<SolvingStatusCallBack?>()
        val linearSubRegistrationCallbacks = mutableListOf<RegistrationStatusCallBack?>()
        val linearSubSolvingCallbacks = mutableListOf<SolvingStatusCallBack?>()
        val quadraticSubRegistrationCallbacks = mutableListOf<RegistrationStatusCallBack?>()
        val quadraticSubSolvingCallbacks = mutableListOf<SolvingStatusCallBack?>()

        private val output = FeasibleSolverOutput(
            obj = Flt64(12.0),
            solution = listOf(Flt64(5.0), Flt64(7.0)),
            time = Duration.ZERO,
            possibleBestObj = Flt64(11.0),
            gap = Flt64.zero
        )

        private val linearCut = LinearInequality(
            lhs = LinearPolynomial(emptyList(), Flt64(3.0)),
            rhs = LinearPolynomial(emptyList(), Flt64(1.0)),
            comparison = Comparison.LE
        )

        private val quadraticCut = QuadraticInequalityOf(
            lhs = QuadraticPolynomial(emptyList(), Flt64(4.0)),
            rhs = QuadraticPolynomial(emptyList(), Flt64(2.0)),
            comparison = Comparison.GE
        )

        override suspend fun solveMaster(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<fuookami.ospf.kotlin.core.solver.output.SolverOutput> {
            linearMasterNames.add(name)
            linearMasterToLogModelFlags.add(toLogModel)
            linearMasterRegistrationCallbacks.add(registrationStatusCallBack)
            linearMasterSolvingCallbacks.add(solvingStatusCallBack)
            return Ok(output)
        }

        override suspend fun solveSub(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            objectVariable: AbstractVariableItem<*, *>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<LinearBendersDecompositionSolver.LinearSubResult> {
            linearSubNames.add(name)
            linearSubToLogModelFlags.add(toLogModel)
            linearSubRegistrationCallbacks.add(registrationStatusCallBack)
            linearSubSolvingCallbacks.add(solvingStatusCallBack)
            val dual = emptyMap<Constraint<Flt64, Linear>, Flt64>()
            return Ok(
                LinearBendersDecompositionSolver.LinearFeasibleResult(
                    result = output,
                    dualSolution = dual,
                    cuts = listOf(linearCut)
                )
            )
        }

        override suspend fun solveMaster(
            name: String,
            metaModel: QuadraticMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<fuookami.ospf.kotlin.core.solver.output.SolverOutput> {
            quadraticMasterNames.add(name)
            quadraticMasterToLogModelFlags.add(toLogModel)
            quadraticMasterRegistrationCallbacks.add(registrationStatusCallBack)
            quadraticMasterSolvingCallbacks.add(solvingStatusCallBack)
            return Ok(output)
        }

        override suspend fun solveSub(
            name: String,
            metaModel: QuadraticMetaModel<Flt64>,
            objectVariable: AbstractVariableItem<*, *>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<QuadraticBendersDecompositionSolver.QuadraticSubResult> {
            quadraticSubNames.add(name)
            quadraticSubToLogModelFlags.add(toLogModel)
            quadraticSubRegistrationCallbacks.add(registrationStatusCallBack)
            quadraticSubSolvingCallbacks.add(solvingStatusCallBack)
            val dual = emptyMap<Constraint<Flt64, Quadratic>, Flt64>()
            return Ok(
                QuadraticBendersDecompositionSolver.QuadraticFeasibleResult(
                    result = output,
                    dualSolution = dual,
                    linearCuts = listOf(linearCut),
                    quadraticCuts = listOf(quadraticCut)
                )
            )
        }
    }

    private fun linearModel(): LinearMetaModel<Flt64> {
        return LinearMetaModel(name = "linear", converter = IntoValue.Identity)
    }

    private fun quadraticModel(): QuadraticMetaModel<Flt64> {
        return QuadraticMetaModel(name = "quadratic", converter = IntoValue.Identity)
    }

    private val plusOneConverter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64): Flt64 = value + Flt64.one
        override val zero: Flt64 get() = Flt64.zero
        override val one: Flt64 get() = Flt64.one
        override fun fromValue(value: Flt64): Flt64 = value
    }

    @Test
    fun linearSolveSubAsConvertsSolutionAndCuts() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveSubAs(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap(),
            converter = IntoValue.Identity
        )
        val value = (result as Ok).value as LinearBendersDecompositionSolver.LinearFeasibleResultOf<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(listOf(Flt64(5.0), Flt64(7.0)), value.solution)
        assertEquals(Flt64(3.0), value.cuts!!.first().lhs.constant)
    }

    @Test
    fun linearSolveSubAsSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveSubAs(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap()
        )
        val value = (result as Ok).value as LinearBendersDecompositionSolver.LinearFeasibleResultOf<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(Flt64(3.0), value.cuts!!.first().lhs.constant)
    }

    @Test
    fun quadraticSolveSubAsConvertsSolutionAndCuts() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveSubAs(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap(),
            converter = IntoValue.Identity
        )
        val value = (result as Ok).value as QuadraticBendersDecompositionSolver.QuadraticFeasibleResultOf<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(listOf(Flt64(5.0), Flt64(7.0)), value.solution)
        assertEquals(Flt64(3.0), value.linearCuts!!.first().lhs.constant)
        assertEquals(Flt64(4.0), value.quadraticCuts!!.first().lhs.constant)
    }

    @Test
    fun quadraticSolveSubAsSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveSubAs(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap()
        )
        val value = (result as Ok).value as QuadraticBendersDecompositionSolver.QuadraticFeasibleResultOf<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(Flt64(3.0), value.linearCuts!!.first().lhs.constant)
        assertEquals(Flt64(4.0), value.quadraticCuts!!.first().lhs.constant)
    }

    @Test
    fun linearSolveSubAsAsyncSupportsGenericMetaModelInput() {
        val solver = StubBendersSolver()
        val result = solver.solveSubAsAsync(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap()
        ).get()
        val value = (result as Ok).value as LinearBendersDecompositionSolver.LinearFeasibleResultOf<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(Flt64(3.0), value.cuts!!.first().lhs.constant)
    }

    @Test
    fun linearSolveSubAsAsyncUsesConverterPipeline() {
        val solver = StubBendersSolver()
        val result = solver.solveSubAsAsync(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap(),
            converter = plusOneConverter
        ).get()
        val value = (result as Ok).value as LinearBendersDecompositionSolver.LinearFeasibleResultOf<Flt64>
        assertEquals(listOf(Flt64(6.0), Flt64(8.0)), value.solution)
    }

    @Test
    fun quadraticSolveSubAsAsyncSupportsGenericMetaModelInput() {
        val solver = StubBendersSolver()
        val result = solver.solveSubAsAsync(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap()
        ).get()
        val value = (result as Ok).value as QuadraticBendersDecompositionSolver.QuadraticFeasibleResultOf<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(Flt64(3.0), value.linearCuts!!.first().lhs.constant)
        assertEquals(Flt64(4.0), value.quadraticCuts!!.first().lhs.constant)
    }

    @Test
    fun quadraticSolveSubAsAsyncUsesConverterPipeline() {
        val solver = StubBendersSolver()
        val result = solver.solveSubAsAsync(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap(),
            converter = plusOneConverter
        ).get()
        val value = (result as Ok).value as QuadraticBendersDecompositionSolver.QuadraticFeasibleResultOf<Flt64>
        assertEquals(listOf(Flt64(6.0), Flt64(8.0)), value.solution)
    }

    @Test
    fun linearSolveMasterAsConvertsFeasibleOutput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveMasterAs(
            metaModel = linearModel(),
            converter = plusOneConverter
        )
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(6.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(8.0), feasible.solution[1] as Flt64)
        assertEquals(Flt64(13.0), feasible.objValueOrNull!! as Flt64)
        assertEquals(Flt64(12.0), feasible.possibleBestObjValueOrNull!! as Flt64)
    }

    @Test
    fun linearSolveMasterAsSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveMasterAs(metaModel = linearModel())
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(5.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(7.0), feasible.solution[1] as Flt64)
    }

    @Test
    fun linearSolveMasterAsAsyncSupportsGenericMetaModelInput() {
        val solver = StubBendersSolver()
        val result = solver.solveMasterAsAsync(metaModel = linearModel()).get()
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(5.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(7.0), feasible.solution[1] as Flt64)
    }

    @Test
    fun linearSolveMasterAsAsyncUsesConverterPipeline() {
        val solver = StubBendersSolver()
        val result = solver.solveMasterAsAsync(
            metaModel = linearModel(),
            converter = plusOneConverter
        ).get()
        val output = (result as Ok).value as FeasibleSolverOutput<*>
        assertEquals(Flt64(6.0), output.solution[0] as Flt64)
        assertEquals(Flt64(8.0), output.solution[1] as Flt64)
        assertEquals(Flt64(13.0), output.objValueOrNull!! as Flt64)
        assertEquals(Flt64(12.0), output.possibleBestObjValueOrNull!! as Flt64)
    }

    @Test
    fun linearSolveMasterAsAsyncUsesNameFromOptions() {
        val solver = StubBendersSolver()
        solver.solveMasterAsAsync(
            metaModel = linearModel(),
            options = FrameworkSolveOptions(name = "linear-master-as-async-options")
        ).get()
        assertEquals("linear-master-as-async-options", solver.linearMasterNames.last())
    }

    @Test
    fun linearSolveMasterAsAsyncForwardsToLogModelFromOptions() {
        val solver = StubBendersSolver()
        solver.solveMasterAsAsync(
            metaModel = linearModel(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.linearMasterToLogModelFlags.last())
    }

    @Test
    fun linearSolveMasterAsAsyncForwardsCallbacksFromOptions() {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMasterAsAsync(
            metaModel = linearModel(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        ).get()
        assertSame(registrationStatusCallBack, solver.linearMasterRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.linearMasterSolvingCallbacks.last())
    }

    @Test
    fun linearSolveMasterAsForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMasterAs(
            metaModel = linearModel(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        )
        assertSame(registrationStatusCallBack, solver.linearMasterRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.linearMasterSolvingCallbacks.last())
    }

    @Test
    fun quadraticSolveMasterAsConvertsFeasibleOutput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveMasterAs(
            metaModel = quadraticModel(),
            converter = plusOneConverter
        )
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(6.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(8.0), feasible.solution[1] as Flt64)
        assertEquals(Flt64(13.0), feasible.objValueOrNull!! as Flt64)
        assertEquals(Flt64(12.0), feasible.possibleBestObjValueOrNull!! as Flt64)
    }

    @Test
    fun quadraticSolveMasterAsSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveMasterAs(metaModel = quadraticModel())
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(5.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(7.0), feasible.solution[1] as Flt64)
    }

    @Test
    fun quadraticSolveMasterAsAsyncSupportsGenericMetaModelInput() {
        val solver = StubBendersSolver()
        val result = solver.solveMasterAsAsync(metaModel = quadraticModel()).get()
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(5.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(7.0), feasible.solution[1] as Flt64)
    }

    @Test
    fun quadraticSolveMasterAsAsyncUsesConverterPipeline() {
        val solver = StubBendersSolver()
        val result = solver.solveMasterAsAsync(
            metaModel = quadraticModel(),
            converter = plusOneConverter
        ).get()
        val output = (result as Ok).value as FeasibleSolverOutput<*>
        assertEquals(Flt64(6.0), output.solution[0] as Flt64)
        assertEquals(Flt64(8.0), output.solution[1] as Flt64)
        assertEquals(Flt64(13.0), output.objValueOrNull!! as Flt64)
        assertEquals(Flt64(12.0), output.possibleBestObjValueOrNull!! as Flt64)
    }

    @Test
    fun quadraticSolveMasterAsAsyncUsesNameFromOptions() {
        val solver = StubBendersSolver()
        solver.solveMasterAsAsync(
            metaModel = quadraticModel(),
            options = FrameworkSolveOptions(name = "quadratic-master-as-async-options")
        ).get()
        assertEquals("quadratic-master-as-async-options", solver.quadraticMasterNames.last())
    }

    @Test
    fun quadraticSolveMasterAsAsyncForwardsToLogModelFromOptions() {
        val solver = StubBendersSolver()
        solver.solveMasterAsAsync(
            metaModel = quadraticModel(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.quadraticMasterToLogModelFlags.last())
    }

    @Test
    fun quadraticSolveMasterAsAsyncForwardsCallbacksFromOptions() {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMasterAsAsync(
            metaModel = quadraticModel(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        ).get()
        assertSame(registrationStatusCallBack, solver.quadraticMasterRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.quadraticMasterSolvingCallbacks.last())
    }

    @Test
    fun quadraticSolveMasterAsForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMasterAs(
            metaModel = quadraticModel(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        )
        assertSame(registrationStatusCallBack, solver.quadraticMasterRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.quadraticMasterSolvingCallbacks.last())
    }

    @Test
    fun linearSolveSubAsAsyncUsesNameFromOptions() {
        val solver = StubBendersSolver()
        solver.solveSubAsAsync(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(name = "linear-sub-as-async-options")
        ).get()
        assertEquals("linear-sub-as-async-options", solver.linearSubNames.last())
    }

    @Test
    fun linearSolveSubAsAsyncForwardsToLogModelFromOptions() {
        val solver = StubBendersSolver()
        solver.solveSubAsAsync(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.linearSubToLogModelFlags.last())
    }

    @Test
    fun linearSolveSubAsAsyncForwardsCallbacksFromOptions() {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveSubAsAsync(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        ).get()
        assertSame(registrationStatusCallBack, solver.linearSubRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.linearSubSolvingCallbacks.last())
    }

    @Test
    fun linearSolveSubAsForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveSubAs(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        )
        assertSame(registrationStatusCallBack, solver.linearSubRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.linearSubSolvingCallbacks.last())
    }

    @Test
    fun quadraticSolveSubAsAsyncUsesNameFromOptions() {
        val solver = StubBendersSolver()
        solver.solveSubAsAsync(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(name = "quadratic-sub-as-async-options")
        ).get()
        assertEquals("quadratic-sub-as-async-options", solver.quadraticSubNames.last())
    }

    @Test
    fun quadraticSolveSubAsAsyncForwardsToLogModelFromOptions() {
        val solver = StubBendersSolver()
        solver.solveSubAsAsync(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.quadraticSubToLogModelFlags.last())
    }

    @Test
    fun quadraticSolveSubAsAsyncForwardsCallbacksFromOptions() {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveSubAsAsync(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        ).get()
        assertSame(registrationStatusCallBack, solver.quadraticSubRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.quadraticSubSolvingCallbacks.last())
    }

    @Test
    fun quadraticSolveSubAsForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveSubAs(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        )
        assertSame(registrationStatusCallBack, solver.quadraticSubRegistrationCallbacks.last())
        assertSame(solvingStatusCallBack, solver.quadraticSubSolvingCallbacks.last())
    }
}
