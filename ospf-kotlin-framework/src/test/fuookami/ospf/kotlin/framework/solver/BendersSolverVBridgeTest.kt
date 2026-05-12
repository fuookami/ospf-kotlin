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
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class BendersSolverVBridgeTest {
    private class StubBendersSolver : QuadraticBendersDecompositionSolver {
        override val name: String = "stub-benders"

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
    }
}
