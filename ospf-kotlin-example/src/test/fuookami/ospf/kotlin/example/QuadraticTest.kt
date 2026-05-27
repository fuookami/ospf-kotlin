package fuookami.ospf.kotlin.example

import fuookami.ospf.kotlin.example.test.flt64TestConverter
import fuookami.ospf.kotlin.core.symbol.function.ProductFunction
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class QuadraticTest {
    @Test
    fun smoke() {
        val x = RealVar("quadratic_demo_x")
        val y = RealVar("quadratic_demo_y")
        val left = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.two
        )
        val right = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, y)),
            constant = -Flt64.one
        )
        val function = ProductFunction(
            left = left,
            right = right,
            converter = flt64TestConverter,
            name = "example_quadratic_product"
        )

        val tokenTable = AutoTokenTable<Flt64>(Quadratic, false)
        tokenTable.add(listOf(x, y))
        val values = mapOf<Symbol, Flt64>(
            x to Flt64(2.0),
            y to Flt64(5.0)
        )

        val value = function.evaluate(values, tokenTable, flt64TestConverter, zeroIfNone = false)

        assertNotNull(value)
        assertEquals(Flt64(16.0), value)
    }
}