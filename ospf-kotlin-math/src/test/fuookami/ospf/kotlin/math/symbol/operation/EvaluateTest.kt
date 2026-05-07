package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.MapValueProvider
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.MissingValuePolicy
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.evaluate
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.evaluateRet
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.evaluateOrdered
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.partialEvaluate
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.evaluateIntervalExtremum
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EvaluateTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    private fun closedRange(lb: Double, ub: Double): ValueRange<Flt64> {
        val rangeRet = ValueRange(
            lb = Flt64(lb),
            ub = Flt64(ub),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = Flt64
        )
        return (rangeRet as Ok).value
    }

    @Test
    fun evaluateNullableShouldFollowMissingValuePolicy() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.two, x),
                LinearMonomial<Flt64>(Flt64.one, y)
            ),
            constant = Flt64.one
        )

        val values = mapOf<Symbol, Flt64>(x to Flt64(3.0))
        val returnNull = polynomial.evaluate(MapValueProvider(values), MissingValuePolicy.ReturnNull)
        val asZero = polynomial.evaluate(MapValueProvider(values), MissingValuePolicy.AsZero)

        assertNull(returnNull)
        assertTrue(asZero == Flt64(7.0))
    }

    @Test
    fun evaluateRetShouldReturnFailedWhenMissingAndPolicyFail() {
        val x = TestSymbol("x")
        val monomial = LinearMonomial<Flt64>(Flt64.two, x)

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
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.two, x),
                LinearMonomial<Flt64>(Flt64.one, y)
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
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64.two, x, y),
                QuadraticMonomial<Flt64>(Flt64(3.0), x, null)
            ),
            constant = Flt64.one
        )

        val value = polynomial.evaluate(
            provider = MapValueProvider(mapOf<Symbol, Flt64>(
                x to Flt64(2.0),
                y to Flt64(4.0)
            )),
            policy = MissingValuePolicy.ReturnNull
        )
        assertEquals(Flt64(23.0), value)
    }

    @Test
    fun quadraticEvaluateRetShouldFailWhenValueMissing() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64.one, x, y)
            ),
            constant = Flt64.zero
        )

        val result = polynomial.evaluateRet(
            values = mapOf<Symbol, Flt64>(x to Flt64.one),
            policy = MissingValuePolicy.Fail
        )
        assertTrue(result is Failed)
    }

    @Test
    fun evaluateOrderedShouldMatchMapEvaluate() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")

        val linear = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.two, x),
                LinearMonomial<Flt64>(Flt64(-1.0), z)
            ),
            constant = Flt64.one
        )
        val quadratic = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64(3.0), x, y),
                QuadraticMonomial<Flt64>(Flt64(-2.0), z, null)
            ),
            constant = Flt64(4.0)
        )
        val canonical = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64(5.0), listOf(x, y, z))
            ),
            constant = Flt64(-1.0)
        )

        val order = listOf(z, x, y)
        val orderedValues = listOf(Flt64(7.0), Flt64(2.0), Flt64(3.0))
        val mappedValues = mapOf<Symbol, Flt64>(
            z to Flt64(7.0),
            x to Flt64(2.0),
            y to Flt64(3.0)
        )

        assertEquals(linear.evaluate(MapValueProvider(mappedValues)), linear.evaluateOrdered(order, orderedValues))
        assertEquals(quadratic.evaluate(MapValueProvider(mappedValues)), quadratic.evaluateOrdered(order, orderedValues))
        assertEquals(canonical.evaluate(MapValueProvider(mappedValues)), canonical.evaluateOrdered(order, orderedValues))
    }

    @Test
    fun evaluateOrderedShouldFailForDimensionMismatch() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(LinearMonomial<Flt64>(Flt64.one, x)),
            constant = Flt64.zero
        )

        assertFailsWith<IllegalArgumentException> {
            polynomial.evaluateOrdered(order = listOf(x), values = emptyList())
        }
    }

    @Test
    fun linearPartialEvaluateShouldFoldKnownSymbols() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.two, x),
                LinearMonomial<Flt64>(Flt64(-3.0), y)
            ),
            constant = Flt64.one
        )

        val partial = polynomial.partialEvaluate(mapOf<Symbol, Flt64>(x to Flt64(4.0)))

        assertEquals(Flt64(9.0), partial.constant)
        assertEquals(1, partial.monomials.size)
        assertEquals(y, partial.monomials.first().symbol)
        assertEquals(Flt64(-3.0), partial.monomials.first().coefficient)
    }

    @Test
    fun quadraticPartialEvaluateShouldReduceToLinearWhenOneFactorKnown() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64.two, x, y),
                QuadraticMonomial<Flt64>(Flt64(3.0), x, null)
            ),
            constant = Flt64(5.0)
        )

        val partial = polynomial.partialEvaluate(mapOf<Symbol, Flt64>(x to Flt64(2.0)))
        val terms = partial.monomials.associate { Pair(it.symbol1, it.symbol2) to it.coefficient }

        assertEquals(Flt64(11.0), partial.constant)
        assertEquals(Flt64(4.0), terms[y to null])
    }

    @Test
    fun canonicalPartialEvaluateShouldBeConsistentBetweenMapAndProvider() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64.two, listOf(x, y)),
                CanonicalMonomial<Flt64>(Flt64(3.0), listOf(y, z))
            ),
            constant = Flt64.one
        )
        val values = mapOf<Symbol, Flt64>(x to Flt64(4.0))

        val byMap = polynomial.partialEvaluate(values)
        val byProvider = polynomial.partialEvaluate(MapValueProvider(values))

        assertEquals(byMap, byProvider)
        assertEquals(Flt64.one, byMap.constant)
        assertEquals(2, byMap.monomials.size)
    }

    @Test
    fun evaluateIntervalExtremumShouldMatchManualBounds() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(-3.0), y)
            ),
            constant = Flt64.one
        )

        val extremum = polynomial.evaluateIntervalExtremum(
            intervals = mapOf(
                x to closedRange(1.0, 3.0),
                y to closedRange(2.0, 5.0)
            )
        )
        assertEquals(Flt64(-12.0), extremum?.lowerBound?.value?.unwrapOrNull())
        assertEquals(Flt64.one, extremum?.upperBound?.value?.unwrapOrNull())
    }

    @Test
    fun evaluateIntervalExtremumShouldChooseBoundsByCoefficientSign() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(4.0), x),
                LinearMonomial(Flt64(-5.0), y)
            ),
            constant = Flt64(2.0)
        )

        val extremum = polynomial.evaluateIntervalExtremum(
            intervals = mapOf(
                x to closedRange(-1.0, 3.0),
                y to closedRange(0.0, 4.0)
            )
        )
        assertEquals(Flt64(-22.0), extremum?.lowerBound?.value?.unwrapOrNull())
        assertEquals(Flt64(14.0), extremum?.upperBound?.value?.unwrapOrNull())
    }

    @Test
    fun evaluateIntervalExtremumShouldReturnNullWhenIntervalMissing() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(-3.0), y)
            ),
            constant = Flt64.zero
        )

        val extremum = polynomial.evaluateIntervalExtremum(
            intervals = mapOf(
                x to closedRange(1.0, 3.0)
            )
        )
        assertNull(extremum)
    }
}

