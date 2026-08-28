package fuookami.ospf.kotlin.example.linear_function

import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.minus
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.IfFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.OneOfFunction
import fuookami.ospf.kotlin.core.variable.RealVar

/** SCIP/Gurobi 端到端条件线性函数求解测试 / SCIP/Gurobi end-to-end solve tests for conditional linear functions. */
class ConditionalFunctionSolveTest {
    private val converter = IntoValue.Identity

    @Test
    fun ifFunctionShouldSolveCorrectly() {
        conditionalSolverCasesOrSkip().forEach { solverCase ->
            val x = RealVar("p11_if_${solverCase.name}_x")
            x.range.leq(Flt64(5.0))
            x.range.geq(Flt64.zero)
            val xPoly = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, x)),
                constant = Flt64.zero
            )
            val ifFn = IfFunction(
                condition = xPoly - Flt64(2.0),
                converter = converter,
                name = "p11_if_${solverCase.name}"
            )
            val ifSymbol = LinearFunctionSymbolAdapter(ifFn, converter)
            val model = LinearMetaModel<Flt64>(
                name = "p11_if_${solverCase.name}_solve",
                converter = converter
            )
            try {
                val solver = solverCase.create()
                assertEquals(solverCase.name, solver.name, "solver case should create the requested backend")
                assertTrue(model.add(x) is Ok, "${solverCase.name}: x should be accepted")
                assertTrue(model.add(ifSymbol) is Ok, "${solverCase.name}: ifFn symbol should be added to model")
                assertTrue(model.add(ifFn.helperVariables) is Ok, "${solverCase.name}: if helper variables should be accepted")
                assertTrue(model.minimize(ifSymbol) is Ok, "${solverCase.name}: if objective should be accepted")

                val result = runBlocking { solveConditionalMetaModel(solver, model) }
                val report = result.value ?: error("${solverCase.name}: solver should return a report")
                assertEquals(ProblemStatus.Feasible, report.problemStatus, "${solverCase.name}: solve status")
                val objective = report.solution?.objective
                    ?: error("${solverCase.name}: solver returned no incumbent objective")
                assertNumericallyEqual(Flt64.zero, objective, "${solverCase.name}: minimized if result")

                assertNotNull(model.tokens.find(x)?.result, "${solverCase.name}: x should appear in solution")
                assertNumericallyEqual(
                    Flt64.zero,
                    model.tokens.find(ifFn.resultVar)?.result,
                    "${solverCase.name}: IfFunction resultVar should be read from the solver solution"
                )
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun oneOfFunctionShouldSolveCorrectly() {
        conditionalSolverCasesOrSkip().forEach { solverCase ->
            val a = RealVar("p11_oneof_${solverCase.name}_a")
            a.range.leq(Flt64(10.0))
            a.range.geq(Flt64.zero)
            val b = RealVar("p11_oneof_${solverCase.name}_b")
            b.range.leq(Flt64(10.0))
            b.range.geq(Flt64.zero)
            val aPoly = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, a)),
                constant = Flt64.zero
            )
            val bPoly = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, b)),
                constant = Flt64.zero
            )
            val oneOfFn = OneOfFunction(
                polynomials = listOf(aPoly, bPoly),
                converter = converter,
                name = "p11_oneof_${solverCase.name}"
            )
            val oneOfSymbol = LinearFunctionSymbolAdapter(oneOfFn, converter)
            val model = LinearMetaModel<Flt64>(
                name = "p11_oneof_${solverCase.name}_solve",
                converter = converter
            )
            try {
                val solver = solverCase.create()
                assertEquals(solverCase.name, solver.name, "solver case should create the requested backend")
                assertTrue(model.add(a) is Ok, "${solverCase.name}: a should be accepted")
                assertTrue(model.add(b) is Ok, "${solverCase.name}: b should be accepted")
                assertTrue(model.add(oneOfSymbol) is Ok, "${solverCase.name}: oneOf symbol should be added to model")
                assertTrue(model.add(oneOfFn.helperVariables) is Ok, "${solverCase.name}: oneOf helper variables should be accepted")
                assertTrue(model.minimize(oneOfSymbol) is Ok, "${solverCase.name}: oneOf objective should be accepted")

                val result = runBlocking { solveConditionalMetaModel(solver, model) }
                val report = result.value ?: error("${solverCase.name}: solver should return a report")
                assertEquals(ProblemStatus.Feasible, report.problemStatus, "${solverCase.name}: solve status")
                val objective = report.solution?.objective
                    ?: error("${solverCase.name}: solver returned no incumbent objective")
                assertNumericallyEqual(Flt64.one, objective, "${solverCase.name}: oneOf result")

                assertNotNull(model.tokens.find(a)?.result, "${solverCase.name}: a should appear in solution")
                assertNotNull(model.tokens.find(b)?.result, "${solverCase.name}: b should appear in solution")
                assertNumericallyEqual(
                    Flt64.one,
                    model.tokens.find(oneOfFn.resultVar)?.result,
                    "${solverCase.name}: OneOfFunction resultVar should be read from the solver solution"
                )
            } finally {
                model.close()
            }
        }
    }

    private fun assertNumericallyEqual(expected: Flt64, actual: Flt64?, message: String) {
        val actualValue = actual ?: error(message)
        assertTrue(
            abs(actualValue.toDouble() - expected.toDouble()) <= 1e-6,
            "$message: expected=${expected.toDouble()}, actual=${actualValue.toDouble()}"
        )
    }
}
