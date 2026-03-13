package fuookami.ospf.kotlin.utils.math.symbol.operation

import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.adapter.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*

class EvaluateTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun evaluateNullableShouldFollowMissingValuePolicy() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64.two, x),
                LinearMonomial(Flt64.one, y)
            ),
            constant = Flt64.one
        )

        val values = mapOf<Symbol, Flt64>(x to Flt64(3.0))
        val returnNull = polynomial.evaluate(values, MissingValuePolicy.ReturnNull)
        val asZero = polynomial.evaluate(values, MissingValuePolicy.AsZero)

        assertNull(returnNull)
        assertTrue(asZero == Flt64(7.0))
    }

    @Test
    fun evaluateRetShouldReturnFailedWhenMissingAndPolicyFail() {
        val x = TestSymbol("x")
        val monomial = LinearMonomial(Flt64.two, x)

        val result = monomial.evaluateRet(
            provider = MapValueProvider(emptyMap()),
            policy = MissingValuePolicy.Fail
        )
        assertTrue(result is Failed)
    }

    @Test
    fun evaluateRetShouldReturnOkWhenAsZeroPolicy() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64.two, x),
                LinearMonomial(Flt64.one, y)
            ),
            constant = Flt64(2.0)
        )

        val result = polynomial.evaluateRet(
            provider = MapValueProvider(mapOf(x to Flt64(4.0))),
            policy = MissingValuePolicy.AsZero
        )

        assertTrue(result is Ok)
        assertEquals(Flt64(10.0), (result as Ok).value)
    }

    @Test
    fun quadraticEvaluateShouldSupportSymmetricAndLinearTerms() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial(Flt64.two, x, y),
                QuadraticMonomial(Flt64(3.0), x, null)
            ),
            constant = Flt64.one
        )

        val value = polynomial.evaluate(
            values = mapOf<Symbol, Flt64>(
                x to Flt64(2.0),
                y to Flt64(4.0)
            ),
            policy = MissingValuePolicy.ReturnNull
        )
        assertEquals(Flt64(23.0), value)
    }

    @Test
    fun quadraticEvaluateRetShouldFailWhenValueMissing() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, y)
            )
        )

        val result = polynomial.evaluateRet(
            values = mapOf<Symbol, Flt64>(x to Flt64.one),
            policy = MissingValuePolicy.Fail
        )
        assertTrue(result is Failed)
    }
}
