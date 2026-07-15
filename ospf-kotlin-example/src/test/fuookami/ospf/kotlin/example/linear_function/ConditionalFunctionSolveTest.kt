package fuookami.ospf.kotlin.example.linear_function

import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import kotlinx.coroutines.runBlocking

import fuookami.ospf.kotlin.example.core_demo.ScipAvailability
import fuookami.ospf.kotlin.example.solveLinearMetaModel
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

import fuookami.ospf.kotlin.utils.functional.Ok

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.minus

import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.scip.ScipLinearSolver
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.IfFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.OneOfFunction
import fuookami.ospf.kotlin.core.variable.RealVar

/** End-to-end solve tests for if and one-of conditional linear functions using SCIP. */
class ConditionalFunctionSolveTest {
    private val converter = IntoValue.Identity

    @Test
    fun ifFunctionShouldSolveCorrectly() {
        assumeTrue(ScipAvailability.isAvailable(), "SCIP runtime not available in current environment")

        val x = RealVar("p11_if_x")
        x.range.leq(Flt64(5.0))
        x.range.geq(Flt64.zero)
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val ifFn = IfFunction(
            condition = xPoly - Flt64(2.0),
            converter = converter,
            name = "p11_if"
        )
        val ifSymbol = LinearFunctionSymbolAdapter(ifFn, converter)

        val model = LinearMetaModel<Flt64>(
            name = "p11_if_solve",
            converter = converter
        )
        try {
            assertTrue(model.add(x) is Ok, "x should be accepted")
            assertTrue(model.add(ifSymbol) is Ok, "ifFn symbol should be added to model")
            assertTrue(model.minimize(ifSymbol) is Ok, "minimize if(x-2) objective should be accepted")

            val solver = ScipLinearSolver()
            val result = runBlocking { solveLinearMetaModel(solver, model) }
            assertNotNull(result.value, "Solver should return a feasible solution")
            val obj = result.value!!.obj
            assertTrue(obj geq Flt64.zero, "if function result should be non-negative")
            assertTrue(obj leq Flt64(3.0), "if function result should be <= max possible (x=5, threshold=2, result=3)")

            model.setSolution(result.value!!.solution)
            val xVal = model.tokens.find(x)?.result
            assertNotNull(xVal, "x should appear in solution")
        } finally {
            model.close()
        }
    }

    @Test
    fun oneOfFunctionShouldSolveCorrectly() {
        assumeTrue(ScipAvailability.isAvailable(), "SCIP runtime not available in current environment")

        val a = RealVar("p11_oneof_a")
        a.range.leq(Flt64(10.0))
        a.range.geq(Flt64.zero)
        val b = RealVar("p11_oneof_b")
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
            name = "p11_oneof"
        )
        val oneOfSymbol = LinearFunctionSymbolAdapter(oneOfFn, converter)

        val model = LinearMetaModel<Flt64>(
            name = "p11_oneof_solve",
            converter = converter
        )
        try {
            assertTrue(model.add(a) is Ok, "a should be accepted")
            assertTrue(model.add(b) is Ok, "b should be accepted")
            assertTrue(model.add(oneOfSymbol) is Ok, "oneOf symbol should be added to model")
            assertTrue(model.minimize(oneOfSymbol) is Ok, "minimize oneOf(a,b) objective should be accepted")

            val solver = ScipLinearSolver()
            val result = runBlocking { solveLinearMetaModel(solver, model) }
            assertNotNull(result.value, "Solver should return a feasible solution")
            val obj = result.value!!.obj
            assertTrue(obj geq Flt64.zero, "oneOf result should be non-negative")
            assertTrue(obj leq Flt64.one, "oneOf result should be 0 or 1")

            model.setSolution(result.value!!.solution)
            val aVal = model.tokens.find(a)?.result
            val bVal = model.tokens.find(b)?.result
            assertNotNull(aVal, "a should appear in solution")
            assertNotNull(bVal, "b should appear in solution")
        } finally {
            model.close()
        }
    }
}
