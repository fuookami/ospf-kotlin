package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.basic.Variable as SolverVariable
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.variable.*

class NativeMaxReportTest {
    private val input = RealVar("max_input").also {
        it.range.geq(Flt64(-2.0))
        it.range.leq(Flt64(2.0))
    }
    private val result = RealVar("max_result")
    private val first = BinVar("first")
    private val second = BinVar("second")
    private val snapshot = MaxStructure(
        inputs = listOf(
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            LinearPolynomial(listOf(LinearMonomial(-Flt64.one, input)), Flt64.zero)
        ),
        resultVariable = result,
        selectorVariables = listOf(first, second),
        bigMValues = listOf(Flt64(4.0), Flt64(4.0)),
        converter = IntoValue.Identity,
        name = "max_report"
    )
    private val tokens = listOf(input, result).mapIndexed { index, variable -> token(variable, index) }
    private val original = listOf(second, input, first, result).mapIndexed { index, variable -> token(variable, index) }

    @Test
    fun restoresAffineSelectorsAndTiePoolInOriginalOrder() {
        val restored = restore(report(Flt64(2.0)))
        assertTrue(restored is Ok)
        val solution = assertNotNull(restored.value.solution)
        assertEquals(listOf(Flt64.one, Flt64(-2.0), Flt64.zero, Flt64(2.0)), solution.values)
        assertEquals(listOf(Flt64.zero, Flt64.zero, Flt64.one, Flt64.zero), solution.pool.single())
        assertTrue(restored.value.diagnostics.constraintEvaluations.single().satisfied)
        assertEquals("test-functions-max-1", restored.value.fingerprints.model?.schemaVersion)
    }

    @Test
    fun recordsViolationInsteadOfAssumingGeneralConstraintSatisfied() {
        val restored = restore(report(Flt64(3.0)))
        assertTrue(restored is Ok)
        val evaluation = restored.value.diagnostics.constraintEvaluations.single()
        assertFalse(evaluation.satisfied)
        assertEquals(Flt64.one, evaluation.violation)
    }

    @Test
    fun fingerprintIncludesAffineInputsAndRejectsDuplicateOwnership() {
        val baseline = restore(report(Flt64(2.0)))
        val changed = restore(report(Flt64(2.0)), listOf(snapshot.copy(inputs = listOf(
            snapshot.inputs.first(),
            LinearPolynomial(emptyList(), Flt64.zero)
        ))))
        assertTrue(baseline is Ok)
        assertTrue(changed is Ok)
        assertNotEquals(baseline.value.fingerprints.model, changed.value.fingerprints.model)
        assertTrue(restore(report(Flt64(2.0)), listOf(snapshot, snapshot)) is Failed)
    }

    private fun restore(
        report: SolveReport<Flt64>,
        structures: List<MaxStructure<*>> = listOf(snapshot)
    ): Ret<SolveReport<Flt64>> {
        return restoreNativePiecewiseSolution(
            report = report,
            nativeModel = nativeModel(),
            originalTokens = original,
            structures = emptyList(),
            backendName = "test",
            maxStructures = structures
        )
    }

    private fun nativeModel(): LinearTriadModel {
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = tokens.mapIndexed { index, token ->
                    SolverVariable(
                        index = index,
                        lowerBound = Flt64(-4.0),
                        upperBound = Flt64(4.0),
                        type = token.type,
                        origin = token.variable,
                        name = token.name
                    )
                },
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix<Flt64>(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "native-max"
            ),
            tokensInSolver = tokens,
            objective = LinearObjective(ObjectCategory.Minimum, emptyList()),
            functionExpansionPolicy = FunctionExpansionPolicy.AUTO,
            deferredFunctionStructures = listOf(snapshot)
        )
    }

    private fun token(variable: AbstractVariableItem<*, *>, index: Int): Token<Flt64> {
        return Token(variable, index, mutableMapOf(), IntoValue.Identity)
    }

    private fun report(result: Flt64): SolveReport<Flt64> = SolveReport(
        problemStatus = ProblemStatus.Feasible,
        terminationReason = TerminationReason.Completed,
        solutionPresence = SolutionPresence.Optimal,
        solution = SolveSolution(
            values = listOf(Flt64(-2.0), result),
            objective = Flt64.zero,
            pool = listOf(listOf(Flt64.zero, Flt64.zero))
        ),
        fingerprints = SolveFingerprints(model = SolveFingerprinting.sha256("linear", "test"))
    )
}
