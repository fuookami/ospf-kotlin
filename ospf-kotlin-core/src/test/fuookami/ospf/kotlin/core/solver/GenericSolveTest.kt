package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.basic.Variable as SolverVariable
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.SolveValueConversionPolicy
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.functional.*

class GenericSolveTest {
    @Test
    fun linearSolveShouldConvertAllNumberTypesForTriadAndMechanismAndPool() = runBlocking {
        runLinearCase(GenericNumberCases.flt64)
        runLinearCase(GenericNumberCases.rtn64)
        runLinearCase(GenericNumberCases.fltX)
        runLinearCase(GenericNumberCases.rtnX)
    }

    @Test
    fun quadraticSolveShouldConvertAllNumberTypesForTetradAndMechanismAndPool() = runBlocking {
        runQuadraticCase(GenericNumberCases.flt64)
        runQuadraticCase(GenericNumberCases.rtn64)
        runQuadraticCase(GenericNumberCases.fltX)
        runQuadraticCase(GenericNumberCases.rtnX)
    }

    @Test
    fun solveReportEntrypointsShouldCarryPoolsAndGenericValues() = runBlocking {
        val linearModel = linearTriadModel("report-linear")
        val linearSolver = RecordingLinearSolveSolver()
        val linearReport = (linearSolver.solveReport(
            model = linearModel,
            converter = GenericNumberCases.rtn64.converter
        ) as Ok).value
        assertEquals(ProblemStatus.Feasible, linearReport.problemStatus)
        assertEquals(SolutionPresence.Incumbent, linearReport.solutionPresence)
        assertEquals(2, linearReport.solution?.values?.size)
        assertEquals(GenericNumberCases.rtn64.converter.intoValue(linearSolver.singleObj), linearReport.solution?.objective)

        val linearPoolReport = (linearSolver.solveReport(
            model = linearModel,
            solutionAmount = UInt64(2),
            converter = GenericNumberCases.rtn64.converter
        ) as Ok).value
        assertEquals(2, linearPoolReport.solution?.pool?.size)
        assertEquals(
            GenericNumberCases.rtn64.converter.intoValue(linearSolver.poolSolutionsFlt64[1][0]),
            linearPoolReport.solution?.pool?.get(1)?.get(0)
        )

        val linearOptionReport = (linearSolver.solveReport(
            model = linearModel,
            options = SolveOptions(solutionAmount = UInt64(2))
        ) as Ok).value
        assertEquals(2, linearOptionReport.solution?.pool?.size)

        val linearMechanism = linearMechanismModel(GenericNumberCases.rtn64, "report-linear-mechanism")
        val linearMechanismReport = (linearSolver.solveReport(
            model = linearMechanism,
            converter = GenericNumberCases.rtn64.converter
        ) as Ok).value
        assertEquals(2, linearMechanismReport.solution?.values?.size)
        linearMechanism.close()

        val quadraticModel = quadraticTetradModel("report-quadratic")
        val quadraticSolver = RecordingQuadraticSolveSolver()
        val quadraticReport = (quadraticSolver.solveReport(
            model = quadraticModel,
            converter = GenericNumberCases.rtn64.converter
        ) as Ok).value
        assertEquals(ProblemStatus.Feasible, quadraticReport.problemStatus)
        assertEquals(2, quadraticReport.solution?.values?.size)

        val quadraticPoolReport = (quadraticSolver.solveReport(
            model = quadraticModel,
            solutionAmount = UInt64(2),
            converter = GenericNumberCases.rtn64.converter
        ) as Ok).value
        assertEquals(2, quadraticPoolReport.solution?.pool?.size)

        val quadraticOptionReport = (quadraticSolver.solveReport(
            model = quadraticModel,
            options = SolveOptions(solutionAmount = UInt64(2))
        ) as Ok).value
        assertEquals(2, quadraticOptionReport.solution?.pool?.size)

        val quadraticMechanism = quadraticMechanismModel(GenericNumberCases.rtn64, "report-quadratic-mechanism")
        val quadraticMechanismReport = (quadraticSolver.solveReport(
            model = quadraticMechanism,
            converter = GenericNumberCases.rtn64.converter
        ) as Ok).value
        assertEquals(2, quadraticMechanismReport.solution?.values?.size)
        quadraticMechanism.close()
    }

    @Test
    fun solveReportSolutionPoolShouldHonorStrictValueConversionPolicy() = runBlocking {
        val invalidModel = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = listOf(
                    SolverVariable(
                        index = 0,
                        lowerBound = Flt64.nan,
                        upperBound = Flt64.ten,
                        type = Continuous,
                        origin = null,
                        name = "invalid_x"
                    )
                ),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "invalid-report-model"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )

        val result = RecordingLinearSolveSolver().solveReport(
            model = invalidModel,
            options = SolveOptions(
                solutionAmount = UInt64(2),
                valueConversionPolicy = SolveValueConversionPolicy.Strict
            )
        )

        assertTrue(result is Failed)
    }

    /**
     * 验证解池报告入口传播取消令牌，并在预取消时不启动 backend。
     * Verifies solution-pool report entry points propagate cancellation and do not start a backend when pre-cancelled.
     */
    @Test
    fun solveReportSolutionPoolShouldPropagateCancellationToken() = runBlocking {
        val linearHandle = SolveHandle.create()
        val linearSolver = RecordingLinearSolveSolver()
        assertTrue(
            linearSolver.solveReport(
                model = linearTriadModel("cancel-linear"),
                options = SolveOptions(
                    solutionAmount = UInt64(2),
                    cancellationToken = linearHandle.token
                )
            ) is Ok
        )
        assertSame(linearHandle.token, linearSolver.lastPoolCancellationToken)
        assertEquals(1, linearSolver.poolInvocationCount)

        val quadraticHandle = SolveHandle.create()
        val quadraticSolver = RecordingQuadraticSolveSolver()
        assertTrue(
            quadraticSolver.solveReport(
                model = quadraticTetradModel("cancel-quadratic"),
                options = SolveOptions(
                    solutionAmount = UInt64(2),
                    cancellationToken = quadraticHandle.token
                )
            ) is Ok
        )
        assertSame(quadraticHandle.token, quadraticSolver.lastPoolCancellationToken)
        assertEquals(1, quadraticSolver.poolInvocationCount)

        val cancelledLinearHandle = SolveHandle.create()
        assertTrue(cancelledLinearHandle.cancel(reason = "pre-cancelled").ok)
        val cancelledLinearSolver = RecordingLinearSolveSolver()
        val cancelledLinearResult = cancelledLinearSolver.solveReport(
            model = linearTriadModel("pre-cancel-linear"),
            options = SolveOptions(
                solutionAmount = UInt64(2),
                cancellationToken = cancelledLinearHandle.token
            )
        )
        assertEquals(
            TerminationReason.Cancelled,
            (cancelledLinearResult as Ok).value.terminationReason
        )
        assertEquals(0, cancelledLinearSolver.poolInvocationCount)

        val cancelledQuadraticHandle = SolveHandle.create()
        assertTrue(cancelledQuadraticHandle.cancel(reason = "pre-cancelled").ok)
        val cancelledQuadraticSolver = RecordingQuadraticSolveSolver()
        val cancelledQuadraticResult = cancelledQuadraticSolver.solveReport(
            model = quadraticTetradModel("pre-cancel-quadratic"),
            options = SolveOptions(
                solutionAmount = UInt64(2),
                cancellationToken = cancelledQuadraticHandle.token
            )
        )
        assertEquals(
            TerminationReason.Cancelled,
            (cancelledQuadraticResult as Ok).value.terminationReason
        )
        assertEquals(0, cancelledQuadraticSolver.poolInvocationCount)
    }

    private suspend fun <V> runLinearCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val triad = linearTriadModel("lin_${numberCase.name.lowercase()}")
        val mechanism = linearMechanismModel(numberCase, "lin_${numberCase.name.lowercase()}")
        val solver = RecordingLinearSolveSolver()

        var callbackCount = 0
        val callback = SolvingStatusCallBack {
            callbackCount += 1
            ok
        }

        val triadRet = solver.solve(
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

        val mechanismRet = solver.solve(
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
        val triadPoolRet = solver.solve(
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

        val mechanismPoolRet = solver.solve(
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
        val solver = RecordingQuadraticSolveSolver()

        var callbackCount = 0
        val callback = SolvingStatusCallBack {
            callbackCount += 1
            ok
        }

        val tetradRet = solver.solve(
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

        val mechanismRet = solver.solve(
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
        val tetradPoolRet = solver.solve(
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

        val mechanismPoolRet = solver.solve(
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
        ret: Ret<SolveReport<V>>,
        expectedFlt64: List<Flt64>,
        expectedObj: Flt64,
        expectedPossibleBestObj: Flt64,
        expectedBestBound: Flt64?,
        converterCase: GenericNumberCase<V>,
    ) where V : RealNumber<V>, V : NumberField<V> {
        assertTrue(ret is Ok, "${converterCase.name}: solve should return Ok")
        val output = (ret as Ok).value
        val expected = expectedFlt64.map { converterCase.converter.intoValue(it) }
        val expectedObjValue = converterCase.converter.intoValue(expectedObj)
        val expectedPossibleBestObjValue = converterCase.converter.intoValue(expectedPossibleBestObj)
        val expectedBestBoundValue = expectedBestBound?.let { converterCase.converter.intoValue(it) }

        assertEquals(expected.size, output.values.size, "${converterCase.name}: solution size mismatch")
        output.values.withIndex().forEach { (i, value) ->
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

        assertEquals(expectedObjValue, output.solution?.objective, "${converterCase.name}: objective mismatch")
        assertEquals(expectedBestBound, output.statistics.bestBound, "${converterCase.name}: bestBound mismatch")
    }
    private fun <V> assertPoolAndConverted(
        ret: Ret<Pair<SolveReport<V>, List<Solution<V>>>>,
        expectedPrimaryFlt64: List<Flt64>,
        expectedPoolFlt64: List<List<Flt64>>,
        expectedObj: Flt64,
        expectedPossibleBestObj: Flt64,
        expectedBestBound: Flt64?,
        converterCase: GenericNumberCase<V>,
    ) where V : RealNumber<V>, V : NumberField<V> {
        assertTrue(ret is Ok, "${converterCase.name}: solve(pool) should return Ok")
        val (primary, pool) = (ret as Ok).value

        val expectedPrimary = expectedPrimaryFlt64.map { converterCase.converter.intoValue(it) }
        primary.values.withIndex().forEach { (i, value) ->
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
        assertEquals(expectedObjValue, primary.solution?.objective, "${converterCase.name}: pool objective mismatch")
        assertEquals(expectedBestBound, primary.statistics.bestBound, "${converterCase.name}: pool bestBound mismatch")

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
        val variable = SolverVariable(
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
        val variable = SolverVariable(
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

private class RecordingLinearSolveSolver : AbstractLinearSolver {
    override val name: String = "recording-linear-solve"

    var lastPoolCancellationToken: CancellationToken? = null
    var poolInvocationCount: Int = 0

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
    ): Ret<SolveReport<Flt64>> {
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(
            SolverStatus.Feasible.toSolveReport(
                objective = singleObj,
                values = singleSolveFlt64,
                solveTime = 1.seconds,
                bestBound = singleBestBound,
                gap = Flt64(0.05)
            )
        )
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        poolInvocationCount += 1
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(
            SolverStatus.Feasible.toSolveReport(
                objective = poolObj,
                values = poolPrimaryFlt64,
                solveTime = 2.seconds,
                bestBound = poolBestBound,
                gap = Flt64(0.02)
            ) to poolSolutionsFlt64
        )
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        lastPoolCancellationToken = cancellationToken
        return invoke(model, solutionAmount, solvingStatusCallBack)
    }
}

private class RecordingQuadraticSolveSolver : AbstractQuadraticSolver {
    override val name: String = "recording-quadratic-solve"

    var lastPoolCancellationToken: CancellationToken? = null
    var poolInvocationCount: Int = 0

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
    ): Ret<SolveReport<Flt64>> {
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(
            SolverStatus.Feasible.toSolveReport(
                objective = singleObj,
                values = singleSolveFlt64,
                solveTime = 1.seconds,
                bestBound = singleBestBound,
                gap = Flt64(0.03)
            )
        )
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        poolInvocationCount += 1
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(
            SolverStatus.Feasible.toSolveReport(
                objective = poolObj,
                values = poolPrimaryFlt64,
                solveTime = 2.seconds,
                bestBound = poolBestBound,
                gap = Flt64(0.01)
            ) to poolSolutionsFlt64
        )
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        lastPoolCancellationToken = cancellationToken
        return invoke(model, solutionAmount, solvingStatusCallBack)
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
