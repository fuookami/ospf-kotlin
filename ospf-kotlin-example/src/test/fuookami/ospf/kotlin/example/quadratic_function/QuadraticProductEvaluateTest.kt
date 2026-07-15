package fuookami.ospf.kotlin.example.quadratic_function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.ProductFunction
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar

/** Tests direct evaluation of a product function (x+2)*(y-1) at known sample points. */
class QuadraticProductEvaluateTest {
    @Test
    fun productFunctionEvaluateShouldComputeCorrectValue() {
        val x = RealVar("p12_eval_x")
        val y = RealVar("p12_eval_y")
        val left = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.two
        )
        val right = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, y)),
            constant = -Flt64.one
        )
        val function = ProductFunction(left, right, converter = IntoValue.Identity, name = "p12_eval_product")

        val tokenTable = AutoTokenTable<Flt64>(Quadratic, false)
        tokenTable.add(listOf(x, y))
        val values = mapOf<Symbol, Flt64>(
            x to Flt64(2.0),
            y to Flt64(5.0)
        )

        val value = function.evaluate(values, tokenTable, IntoValue.Identity, zeroIfNone = false)

        assertNotNull(value, "ProductFunction evaluate should return non-null")
        assertEquals(Flt64(16.0), value, "(x+2)*(y-1) at x=2,y=5 should be 4*4=16")
    }
}
