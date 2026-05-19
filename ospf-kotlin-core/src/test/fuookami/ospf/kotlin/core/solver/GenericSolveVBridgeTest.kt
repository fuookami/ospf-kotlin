package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.BasicQuadraticTetradModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.LinearObjective
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticObjective
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModel
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseQuadraticMatrix
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class GenericSolveVBridgeTest {
    @Test
    fun linearSolveVShouldBridgeAllNumberTypesForTriadAndMechanismAndPool() = runBlocking {
        runLinearCase(GenericNumberCases.flt64)
        runLinearCase(GenericNumberCases.rtn64)
        runLinearCase(GenericNumberCases.fltX)
        runLinearCase(GenericNumberCases.rtnX)
    }

    @Test
    fun quadraticSolveVShouldBridgeAllNumberTypesForTetradAndMechanismAndPool() = runBlocking {
        runQuadraticCase(GenericNumberCases.flt64)
        runQuadraticCase(GenericNumberCases.rtn64)
        runQuadraticCase(GenericNumberCases.fltX)
        runQuadraticCase(GenericNumberCases.rtnX)
    }

    private suspend fun <V> runLinearCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val triad = linearTriadModel("lin_${numberCase.name.lowercase()}")
        val mechanism = linearMechanismModel(numberCase, "lin_${numberCase.name.lowercase()}")
        val solver = RecordingLinearSolveVBridgeSolver()

        var callbackCount = 0
        val callback: SolvingStatusCallBack = {
            callbackCount += 1
            ok
        }

        val triadRet = solver.solveV(
            model = triad,
            converter = numberCase.converter,
            solvingStatusCallBack = callback
        )
        assertFeasibleAndConverted(
            ret = triadRet,
            expectedFlt64 = solver.singleSolveFlt64,
            expectedObj = solver.singleObj,
            expectedPossibleBestObj = solver.singlePossibleBestObj,
            expectedBestBound = solver.singleBestBound,
            converterCase = numberCase,
        )

        val mechanismRet = solver.solveV(
            model = mechanism as MechanismModel<V>,
            converter = numberCase.converter,
            solvingStatusCallBack = callback
        )
        assertFeasibleAndConverted(
            ret = mechanismRet,
            expectedFlt64 = solver.singleSolveFlt64,
            expectedObj = solver.singleObj,
            expectedPossibleBestObj = solver.singlePossibleBestObj,
            expectedBestBound = solver.singleBestBound,
            converterCase = numberCase,
        )

        val amount = UInt64(2)
        val triadPoolRet = solver.solveV(
            model = triad,
            solutionAmount = amount,
            converter = numberCase.converter,
            solvingStatusCallBack = callback
        )
        assertPoolAndConverted(
            ret = triadPoolRet,
            expectedPrimaryFlt64 = solver.poolPrimaryFlt64,
            expectedPoolFlt64 = solver.poolSolutionsFlt64,
            expectedObj = solver.poolObj,
            expectedPossibleBestObj = solver.poolPossibleBestObj,
            expectedBestBound = solver.poolBestBound,
            converterCase = numberCase,
        )

        val mechanismPoolRet = solver.solveV(
            model = mechanism as MechanismModel<V>,
            solutionAmount = amount,
            converter = numberCase.converter,
            solvingStatusCallBack = callback
        )
        assertPoolAndConverted(
            ret = mechanismPoolRet,
            expectedPrimaryFlt64 = solver.poolPrimaryFlt64,
            expectedPoolFlt64 = solver.poolSolutionsFlt64,
            expectedObj = solver.poolObj,
            expectedPossibleBestObj = solver.poolPossibleBestObj,
            expectedBestBound = solver.poolBestBound,
            converterCase = numberCase,
        )
        assertEquals(4, callbackCount, "${numberCase.name}: solvingStatusCallBack invocation count mismatch")

        mechanism.close()
    }

    private suspend fun <V> runQuadraticCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val tetrad = quadraticTetradModel("quad_${numberCase.name.lowercase()}")
        val mechanism = quadraticMechanismModel(numberCase, "quad_${numberCase.name.lowercase()}")
        val solver = RecordingQuadraticSolveVBridgeSolver()

        var callbackCount = 0
        val callback: SolvingStatusCallBack = {
            callbackCount += 1
            ok
        }

        val tetradRet = solver.solveV(
            model = tetrad,
            converter = numberCase.converter,
            solvingStatusCallBack = callback
        )
        assertFeasibleAndConverted(
            ret = tetradRet,
            expectedFlt64 = solver.singleSolveFlt64,
            expectedObj = solver.singleObj,
            expectedPossibleBestObj = solver.singlePossibleBestObj,
            expectedBestBound = solver.singleBestBound,
            converterCase = numberCase,
        )

        val mechanismRet = solver.solveV(
            model = mechanism as MechanismModel<V>,
            converter = numberCase.converter,
            solvingStatusCallBack = callback
        )
        assertFeasibleAndConverted(
            ret = mechanismRet,
            expectedFlt64 = solver.singleSolveFlt64,
            expectedObj = solver.singleObj,
            expectedPossibleBestObj = solver.singlePossibleBestObj,
            expectedBestBound = solver.singleBestBound,
            converterCase = numberCase,
        )

        val amount = UInt64(2)
        val tetradPoolRet = solver.solveV(
            model = tetrad,
            solutionAmount = amount,
            converter = numberCase.converter,
            solvingStatusCallBack = callback
        )
        assertPoolAndConverted(
            ret = tetradPoolRet,
            expectedPrimaryFlt64 = solver.poolPrimaryFlt64,
            expectedPoolFlt64 = solver.poolSolutionsFlt64,
            expectedObj = solver.poolObj,
            expectedPossibleBestObj = solver.poolPossibleBestObj,
            expectedBestBound = solver.poolBestBound,
            converterCase = numberCase,
        )

        val mechanismPoolRet = solver.solveV(
            model = mechanism as MechanismModel<V>,
            solutionAmount = amount,
            converter = numberCase.converter,
            solvingStatusCallBack = callback
        )
        assertPoolAndConverted(
            ret = mechanismPoolRet,
            expectedPrimaryFlt64 = solver.poolPrimaryFlt64,
            expectedPoolFlt64 = solver.poolSolutionsFlt64,
            expectedObj = solver.poolObj,
            expectedPossibleBestObj = solver.poolPossibleBestObj,
            expectedBestBound = solver.poolBestBound,
            converterCase = numberCase,
        )
        assertEquals(4, callbackCount, "${numberCase.name}: solvingStatusCallBack invocation count mismatch")

        mechanism.close()
    }
    private fun <V> assertFeasibleAndConverted(
        ret: Ret<FeasibleSolverOutput<V>>,
        expectedFlt64: List<Flt64>,
        expectedObj: Flt64,
        expectedPossibleBestObj: Flt64,
        expectedBestBound: Flt64?,
        converterCase: GenericNumberCase<V>,
    ) where V : RealNumber<V>, V : NumberField<V> {
        assertTrue(ret is Ok, "${converterCase.name}: solveV should return Ok")
        val output = (ret as Ok).value
        val expected = expectedFlt64.map { converterCase.converter.intoValue(it) }
        val expectedObjValue = converterCase.converter.intoValue(expectedObj)
        val expectedPossibleBestObjValue = converterCase.converter.intoValue(expectedPossibleBestObj)
        val expectedBestBoundValue = expectedBestBound?.let { converterCase.converter.intoValue(it) }

        assertEquals(expected.size, output.solution.size, "${converterCase.name}: solution size mismatch")
        output.solution.withIndex().forEach { (i, value) ->
            val expectedValue = expected[i]
            assertEquals(
                converterCase.converter.fromValue(expectedValue),
                converterCase.converter.fromValue(value),
                "${converterCase.name}: solution[$i] value mismatch"
            )
            assertEquals(
                expectedValue::class,
                value::class,
                "${converterCase.name}: solution[$i] runtime type mismatch"
            )
        }

            assertEquals(expectedObj, output.obj, "${converterCase.name}: solver-boundary obj mismatch")
            assertEquals(expectedPossibleBestObj, output.possibleBestObj, "${converterCase.name}: solver-boundary possibleBestObj mismatch")
            assertEquals(expectedBestBound, output.bestBound, "${converterCase.name}: solver-boundary bestBound mismatch")
        assertEquals(expectedObjValue, output.objValue, "${converterCase.name}: objValue mismatch")
        assertEquals(expectedPossibleBestObjValue, output.possibleBestObjValue, "${converterCase.name}: possibleBestObjValue mismatch")
        assertEquals(expectedBestBoundValue, output.bestBoundValue, "${converterCase.name}: bestBoundValue mismatch")
    }
    private fun <V> assertPoolAndConverted(
        ret: Ret<Pair<FeasibleSolverOutput<V>, List<Solution<V>>>>,
        expectedPrimaryFlt64: List<Flt64>,
        expectedPoolFlt64: List<List<Flt64>>,
        expectedObj: Flt64,
        expectedPossibleBestObj: Flt64,
        expectedBestBound: Flt64?,
        converterCase: GenericNumberCase<V>,
    ) where V : RealNumber<V>, V : NumberField<V> {
        assertTrue(ret is Ok, "${converterCase.name}: solveV(pool) should return Ok")
        val (primary, pool) = (ret as Ok).value

        val expectedPrimary = expectedPrimaryFlt64.map { converterCase.converter.intoValue(it) }
        primary.solution.withIndex().forEach { (i, value) ->
            val expectedValue = expectedPrimary[i]
            assertEquals(
                converterCase.converter.fromValue(expectedValue),
                converterCase.converter.fromValue(value),
                "${converterCase.name}: primary solution[$i] mismatch"
            )
            assertEquals(
                expectedValue::class,
                value::class,
                "${converterCase.name}: primary solution[$i] runtime type mismatch"
            )
        }

        val expectedObjValue = converterCase.converter.intoValue(expectedObj)
        val expectedPossibleBestObjValue = converterCase.converter.intoValue(expectedPossibleBestObj)
        val expectedBestBoundValue = expectedBestBound?.let { converterCase.converter.intoValue(it) }
            assertEquals(expectedObj, primary.obj, "${converterCase.name}: pool solver-boundary obj mismatch")
            assertEquals(expectedPossibleBestObj, primary.possibleBestObj, "${converterCase.name}: pool solver-boundary possibleBestObj mismatch")
            assertEquals(expectedBestBound, primary.bestBound, "${converterCase.name}: pool solver-boundary bestBound mismatch")
        assertEquals(expectedObjValue, primary.objValue, "${converterCase.name}: pool objValue mismatch")
        assertEquals(expectedPossibleBestObjValue, primary.possibleBestObjValue, "${converterCase.name}: pool possibleBestObjValue mismatch")
        assertEquals(expectedBestBoundValue, primary.bestBoundValue, "${converterCase.name}: pool bestBoundValue mismatch")

        assertEquals(expectedPoolFlt64.size, pool.size, "${converterCase.name}: pool size mismatch")
        pool.withIndex().forEach { (rowIndex, row) ->
            val expectedRow = expectedPoolFlt64[rowIndex].map { converterCase.converter.intoValue(it) }
            assertEquals(expectedRow.size, row.size, "${converterCase.name}: pool[$rowIndex] width mismatch")
            row.withIndex().forEach { (colIndex, value) ->
                val expectedValue = expectedRow[colIndex]
                assertEquals(
                    converterCase.converter.fromValue(expectedValue),
                    converterCase.converter.fromValue(value),
                    "${converterCase.name}: pool[$rowIndex][$colIndex] value mismatch"
                )
                assertEquals(
                    expectedValue::class,
                    value::class,
                    "${converterCase.name}: pool[$rowIndex][$colIndex] runtime type mismatch"
                )
            }
        }
    }

    private fun linearTriadModel(name: String): LinearTriadModel {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64.ten,
            type = Continuous,
            origin = null,
            name = "${name}_x"
        )
        val constraints = LinearConstraintBatch(
            sparseLhs = SparseMatrix<Flt64>(),
            signs = emptyList(),
            rhs = emptyList(),
            names = emptyList(),
            sources = emptyList()
        )
        val basic = BasicLinearTriadModel(
            variables = listOf(variable),
            constraints = constraints,
            name = name
        )
        return LinearTriadModel(
            impl = basic,
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )
    }

    private fun quadraticTetradModel(name: String): QuadraticTetradModel {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64.ten,
            type = Continuous,
            origin = null,
            name = "${name}_x"
        )
        val constraints = QuadraticConstraintBatch(
            sparseLhs = SparseQuadraticMatrix(),
            signs = emptyList(),
            rhs = emptyList(),
            names = emptyList(),
            sources = emptyList()
        )
        val basic = BasicQuadraticTetradModel(
            variables = listOf(variable),
            constraints = constraints,
            name = name
        )
        return QuadraticTetradModel(
            impl = basic,
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )
    }

    private fun <V> linearMechanismModel(
        numberCase: GenericNumberCase<V>,
        name: String
    ): LinearMechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${name}_x")
        val model = LinearMetaModel(
            name = "${name}_meta",
            objectCategory = ObjectCategory.Minimum,
            converter = numberCase.converter
        )
        assertTrue(model.add(x) is Ok)
        val relation = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(numberCase.one, x)),
                constant = numberCase.zero
            ),
            rhs = LinearPolynomial(emptyList(), numberCase.ten),
            comparison = Comparison.LE
        )
        assertTrue(model.addConstraint(relation = relation, name = "c_${name}") is Ok)
        val mechanismRet = runBlocking { LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false) }
        assertTrue(mechanismRet is Ok, "${numberCase.name}: linear mechanism dump should be Ok")
        return mechanismRet.value
    }

    private fun <V> quadraticMechanismModel(
        numberCase: GenericNumberCase<V>,
        name: String
    ): QuadraticMechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${name}_x")
        val model = QuadraticMetaModel(
            name = "${name}_meta",
            objectCategory = ObjectCategory.Minimum,
            converter = numberCase.converter
        )
        assertTrue(model.add(x) is Ok)
        val relation = QuadraticInequalityOf(
            lhs = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.linear(numberCase.one, x)),
                constant = numberCase.zero
            ),
            rhs = QuadraticPolynomial(emptyList(), numberCase.ten),
            comparison = Comparison.LE
        )
        assertTrue(model.addConstraint(relation = relation, name = "qc_${name}") is Ok)
        val mechanismRet = runBlocking { QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false) }
        assertTrue(mechanismRet is Ok, "${numberCase.name}: quadratic mechanism dump should be Ok")
        return mechanismRet.value
    }
}

private class RecordingLinearSolveVBridgeSolver : AbstractLinearSolver {
    override val name: String = "recording-linear-solvev-bridge"

    val singleObj = Flt64(10.0)
    val singlePossibleBestObj = Flt64(9.5)
    val singleBestBound = Flt64(9.0)
    val singleSolveFlt64: List<Flt64> = listOf(Flt64(3.5), Flt64(-1.25))
    val poolObj = Flt64(20.0)
    val poolPossibleBestObj = Flt64(19.0)
    val poolBestBound = Flt64(18.0)
    val poolPrimaryFlt64: List<Flt64> = listOf(Flt64(9.0), Flt64(2.0))
    val poolSolutionsFlt64: List<List<Flt64>> = listOf(
        listOf(Flt64(9.0), Flt64(2.0)),
        listOf(Flt64(7.5), Flt64(-3.0))
    )

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(
            FeasibleSolverOutput(
                obj = singleObj,
                solution = singleSolveFlt64,
                time = 1.seconds,
                possibleBestObj = singlePossibleBestObj,
                gap = Flt64(0.05),
                bestBound = singleBestBound
            )
        )
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(
            FeasibleSolverOutput(
                obj = poolObj,
                solution = poolPrimaryFlt64,
                time = 2.seconds,
                possibleBestObj = poolPossibleBestObj,
                gap = Flt64(0.02),
                bestBound = poolBestBound
            ) to poolSolutionsFlt64
        )
    }
}

private class RecordingQuadraticSolveVBridgeSolver : AbstractQuadraticSolver {
    override val name: String = "recording-quadratic-solvev-bridge"

    val singleObj = Flt64(-5.0)
    val singlePossibleBestObj = Flt64(-4.5)
    val singleBestBound = Flt64(-4.0)
    val singleSolveFlt64: List<Flt64> = listOf(Flt64(-4.0), Flt64(6.25))
    val poolObj = Flt64(11.0)
    val poolPossibleBestObj = Flt64(10.5)
    val poolBestBound = Flt64(10.0)
    val poolPrimaryFlt64: List<Flt64> = listOf(Flt64(1.0), Flt64(8.0))
    val poolSolutionsFlt64: List<List<Flt64>> = listOf(
        listOf(Flt64(1.0), Flt64(8.0)),
        listOf(Flt64(-2.5), Flt64(3.0))
    )

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(
            FeasibleSolverOutput(
                obj = singleObj,
                solution = singleSolveFlt64,
                time = 1.seconds,
                possibleBestObj = singlePossibleBestObj,
                gap = Flt64(0.03),
                bestBound = singleBestBound
            )
        )
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(
            FeasibleSolverOutput(
                obj = poolObj,
                solution = poolPrimaryFlt64,
                time = 2.seconds,
                possibleBestObj = poolPossibleBestObj,
                gap = Flt64(0.01),
                bestBound = poolBestBound
            ) to poolSolutionsFlt64
        )
    }
}

private fun dummyStatus(solverName: String) = fuookami.ospf.kotlin.core.solver.output.SolvingStatus(
    solver = solverName,
    solverConfig = SolverConfig(),
    objectCategory = ObjectCategory.Minimum,
    time = 1.seconds,
    obj = Flt64.one,
    possibleBestObj = Flt64.one,
    initialBestObj = Flt64.one,
    gap = Flt64.zero
)
