package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Linear
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.Quadratic
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class BendersSolverVBridgeTest {
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
    fun linearSolveSubVConvertsSolutionAndCuts() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveSubV(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap(),
            converter = IntoValue.Identity
        )
        val value = (result as Ok).value as LinearBendersDecompositionSolver.LinearFeasibleResultV<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(listOf(Flt64(5.0), Flt64(7.0)), value.solution)
        assertEquals(Flt64(3.0), value.cuts!!.first().lhs.constant)
    }

    @Test
    fun linearSolveSubVSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveSubV(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap()
        )
        val value = (result as Ok).value as LinearBendersDecompositionSolver.LinearFeasibleResultV<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(Flt64(3.0), value.cuts!!.first().lhs.constant)
    }

    @Test
    fun quadraticSolveSubVConvertsSolutionAndCuts() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveSubV(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap(),
            converter = IntoValue.Identity
        )
        val value = (result as Ok).value as QuadraticBendersDecompositionSolver.QuadraticFeasibleResultV<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(listOf(Flt64(5.0), Flt64(7.0)), value.solution)
        assertEquals(Flt64(3.0), value.linearCuts!!.first().lhs.constant)
        assertEquals(Flt64(4.0), value.quadraticCuts!!.first().lhs.constant)
    }

    @Test
    fun quadraticSolveSubVSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveSubV(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap()
        )
        val value = (result as Ok).value as QuadraticBendersDecompositionSolver.QuadraticFeasibleResultV<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(Flt64(3.0), value.linearCuts!!.first().lhs.constant)
        assertEquals(Flt64(4.0), value.quadraticCuts!!.first().lhs.constant)
    }

    @Test
    fun linearSolveSubVAsyncSupportsGenericMetaModelInput() {
        val solver = StubBendersSolver()
        val result = solver.solveSubVAsync(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap()
        ).get()
        val value = (result as Ok).value as LinearBendersDecompositionSolver.LinearFeasibleResultV<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(Flt64(3.0), value.cuts!!.first().lhs.constant)
    }

    @Test
    fun linearSolveSubVAsyncUsesConverterPipeline() {
        val solver = StubBendersSolver()
        val result = solver.solveSubVAsync(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap(),
            converter = plusOneConverter
        ).get()
        val value = (result as Ok).value as LinearBendersDecompositionSolver.LinearFeasibleResultV<Flt64>
        assertEquals(listOf(Flt64(6.0), Flt64(8.0)), value.solution)
    }

    @Test
    fun quadraticSolveSubVAsyncSupportsGenericMetaModelInput() {
        val solver = StubBendersSolver()
        val result = solver.solveSubVAsync(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap()
        ).get()
        val value = (result as Ok).value as QuadraticBendersDecompositionSolver.QuadraticFeasibleResultV<Flt64>
        assertEquals(Flt64(12.0), value.obj)
        assertEquals(Flt64(3.0), value.linearCuts!!.first().lhs.constant)
        assertEquals(Flt64(4.0), value.quadraticCuts!!.first().lhs.constant)
    }

    @Test
    fun quadraticSolveSubVAsyncUsesConverterPipeline() {
        val solver = StubBendersSolver()
        val result = solver.solveSubVAsync(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap(),
            converter = plusOneConverter
        ).get()
        val value = (result as Ok).value as QuadraticBendersDecompositionSolver.QuadraticFeasibleResultV<Flt64>
        assertEquals(listOf(Flt64(6.0), Flt64(8.0)), value.solution)
    }

    @Test
    fun linearSolveMasterVConvertsFeasibleOutput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveMasterV(
            metaModel = linearModel(),
            converter = plusOneConverter
        )
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(6.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(8.0), feasible.solution[1] as Flt64)
        assertEquals(Flt64(13.0), feasible.objValue as Flt64)
        assertEquals(Flt64(12.0), feasible.possibleBestObjValue as Flt64)
    }

    @Test
    fun linearSolveMasterVSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveMasterV(metaModel = linearModel())
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(5.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(7.0), feasible.solution[1] as Flt64)
    }

    @Test
    fun linearSolveMasterVAsyncSupportsGenericMetaModelInput() {
        val solver = StubBendersSolver()
        val result = solver.solveMasterVAsync(metaModel = linearModel()).get()
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(5.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(7.0), feasible.solution[1] as Flt64)
    }

    @Test
    fun linearSolveMasterVAsyncUsesConverterPipeline() {
        val solver = StubBendersSolver()
        val result = solver.solveMasterVAsync(
            metaModel = linearModel(),
            converter = plusOneConverter
        ).get()
        val output = (result as Ok).value as FeasibleSolverOutput<*>
        assertEquals(Flt64(6.0), output.solution[0] as Flt64)
        assertEquals(Flt64(8.0), output.solution[1] as Flt64)
        assertEquals(Flt64(13.0), output.objValue as Flt64)
        assertEquals(Flt64(12.0), output.possibleBestObjValue as Flt64)
    }

    @Test
    fun linearSolveMasterVAsyncUsesNameFromOptions() {
        val solver = StubBendersSolver()
        solver.solveMasterVAsync(
            metaModel = linearModel(),
            options = FrameworkSolveOptions(name = "linear-master-v-async-options")
        ).get()
        assertEquals("linear-master-v-async-options", solver.linearMasterNames.last())
    }

    @Test
    fun linearSolveMasterVAsyncForwardsToLogModelFromOptions() {
        val solver = StubBendersSolver()
        solver.solveMasterVAsync(
            metaModel = linearModel(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.linearMasterToLogModelFlags.last())
    }

    @Test
    fun linearSolveMasterVAsyncForwardsCallbacksFromOptions() {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMasterVAsync(
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
    fun linearSolveMasterVForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMasterV(
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
    fun quadraticSolveMasterVConvertsFeasibleOutput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveMasterV(
            metaModel = quadraticModel(),
            converter = plusOneConverter
        )
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(6.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(8.0), feasible.solution[1] as Flt64)
        assertEquals(Flt64(13.0), feasible.objValue as Flt64)
        assertEquals(Flt64(12.0), feasible.possibleBestObjValue as Flt64)
    }

    @Test
    fun quadraticSolveMasterVSupportsGenericMetaModelInput() = runBlocking {
        val solver = StubBendersSolver()
        val result = solver.solveMasterV(metaModel = quadraticModel())
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(5.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(7.0), feasible.solution[1] as Flt64)
    }

    @Test
    fun quadraticSolveMasterVAsyncSupportsGenericMetaModelInput() {
        val solver = StubBendersSolver()
        val result = solver.solveMasterVAsync(metaModel = quadraticModel()).get()
        val output = (result as Ok).value
        assertTrue(output is FeasibleSolverOutput<*>)
        val feasible = output as FeasibleSolverOutput<*>
        assertEquals(Flt64(5.0), feasible.solution[0] as Flt64)
        assertEquals(Flt64(7.0), feasible.solution[1] as Flt64)
    }

    @Test
    fun quadraticSolveMasterVAsyncUsesConverterPipeline() {
        val solver = StubBendersSolver()
        val result = solver.solveMasterVAsync(
            metaModel = quadraticModel(),
            converter = plusOneConverter
        ).get()
        val output = (result as Ok).value as FeasibleSolverOutput<*>
        assertEquals(Flt64(6.0), output.solution[0] as Flt64)
        assertEquals(Flt64(8.0), output.solution[1] as Flt64)
        assertEquals(Flt64(13.0), output.objValue as Flt64)
        assertEquals(Flt64(12.0), output.possibleBestObjValue as Flt64)
    }

    @Test
    fun quadraticSolveMasterVAsyncUsesNameFromOptions() {
        val solver = StubBendersSolver()
        solver.solveMasterVAsync(
            metaModel = quadraticModel(),
            options = FrameworkSolveOptions(name = "quadratic-master-v-async-options")
        ).get()
        assertEquals("quadratic-master-v-async-options", solver.quadraticMasterNames.last())
    }

    @Test
    fun quadraticSolveMasterVAsyncForwardsToLogModelFromOptions() {
        val solver = StubBendersSolver()
        solver.solveMasterVAsync(
            metaModel = quadraticModel(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.quadraticMasterToLogModelFlags.last())
    }

    @Test
    fun quadraticSolveMasterVAsyncForwardsCallbacksFromOptions() {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMasterVAsync(
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
    fun quadraticSolveMasterVForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveMasterV(
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
    fun linearSolveSubVAsyncUsesNameFromOptions() {
        val solver = StubBendersSolver()
        solver.solveSubVAsync(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(name = "linear-sub-v-async-options")
        ).get()
        assertEquals("linear-sub-v-async-options", solver.linearSubNames.last())
    }

    @Test
    fun linearSolveSubVAsyncForwardsToLogModelFromOptions() {
        val solver = StubBendersSolver()
        solver.solveSubVAsync(
            metaModel = linearModel(),
            objectVariable = RealVar("x"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.linearSubToLogModelFlags.last())
    }

    @Test
    fun linearSolveSubVAsyncForwardsCallbacksFromOptions() {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveSubVAsync(
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
    fun linearSolveSubVForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveSubV(
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
    fun quadraticSolveSubVAsyncUsesNameFromOptions() {
        val solver = StubBendersSolver()
        solver.solveSubVAsync(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(name = "quadratic-sub-v-async-options")
        ).get()
        assertEquals("quadratic-sub-v-async-options", solver.quadraticSubNames.last())
    }

    @Test
    fun quadraticSolveSubVAsyncForwardsToLogModelFromOptions() {
        val solver = StubBendersSolver()
        solver.solveSubVAsync(
            metaModel = quadraticModel(),
            objectVariable = RealVar("y"),
            fixedVariables = emptyMap(),
            options = FrameworkSolveOptions(toLogModel = true)
        ).get()
        assertEquals(true, solver.quadraticSubToLogModelFlags.last())
    }

    @Test
    fun quadraticSolveSubVAsyncForwardsCallbacksFromOptions() {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveSubVAsync(
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
    fun quadraticSolveSubVForwardsCallbacksFromOptions() = runBlocking {
        val solver = StubBendersSolver()
        val registrationStatusCallBack: RegistrationStatusCallBack = { _ -> ok }
        val solvingStatusCallBack: SolvingStatusCallBack = { _ -> ok }
        solver.solveSubV(
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