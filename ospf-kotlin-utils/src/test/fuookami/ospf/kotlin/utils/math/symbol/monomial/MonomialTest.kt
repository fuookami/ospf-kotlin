package fuookami.ospf.kotlin.utils.math.symbol.monomial

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonomialTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun linearMonomialDefaultCoefficient() {
        val x = TestSymbol("x")
        val monomial = LinearMonomial(symbol = x)

        assertEquals(x, monomial.symbol)
        assertTrue(monomial.coefficient == Flt64.one)
    }

    @Test
    fun quadraticMonomialFlagAndCategory() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val linearLike = QuadraticMonomial(symbol1 = x)
        assertFalse(linearLike.isQuadratic)
        assertEquals(Linear, linearLike.category)

        val quadratic = QuadraticMonomial(coefficient = Flt64.two, symbol1 = x, symbol2 = y)
        assertTrue(quadratic.isQuadratic)
        assertEquals(Quadratic, quadratic.category)
        assertTrue(quadratic.coefficient == Flt64.two)
    }
}
