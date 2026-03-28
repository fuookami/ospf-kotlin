package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MultiCoefficientGenericTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    private data class CoefOps<T>(
        val zero: T,
        val one: T,
        val two: T,
        val three: T,
        val four: T,
        val five: T,
        val six: T
    ) where T : Ring<T>

    private fun <T> runSuite(ops: CoefOps<T>) where T : Ring<T> {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val linear = GenericLinearPolynomial(
            monomials = listOf(
                GenericLinearMonomial(ops.two, x),
                GenericLinearMonomial(ops.three, x),
                GenericLinearMonomial(ops.four, y)
            ),
            constant = ops.one
        ).combineTerms(ops.zero)
        assertEquals(2, linear.monomials.size)
        val linearValue = linear.evaluate(
            mapOf(x to ops.two, y to ops.three)
        )
        assertNotNull(linearValue)
        assertEquals(ops.twentyThree(), linearValue)

        val quadratic = GenericQuadraticPolynomial(
            monomials = listOf(
                GenericQuadraticMonomial(ops.two, x, y),
                GenericQuadraticMonomial(ops.three, y, x),
                GenericQuadraticMonomial(ops.four, x, null)
            ),
            constant = ops.one
        ).combineTerms(ops.zero)
        assertEquals(2, quadratic.monomials.size)
        val quadraticValue = quadratic.evaluate(
            mapOf(x to ops.two, y to ops.three)
        )
        assertNotNull(quadraticValue)
        assertEquals(ops.thirtyNine(), quadraticValue)

        val canonical = GenericCanonicalPolynomial(
            monomials = listOf(
                GenericCanonicalMonomial(ops.two, listOf(x, y)),
                GenericCanonicalMonomial(ops.three, listOf(y, x)),
                GenericCanonicalMonomial(ops.four, listOf(x))
            ),
            constant = ops.one
        ).combineTerms(ops.zero)
        assertEquals(2, canonical.monomials.size)
        val canonicalValue = canonical.evaluate(
            mapOf(x to ops.two, y to ops.three)
        )
        assertNotNull(canonicalValue)
        assertEquals(ops.thirtyNine(), canonicalValue)
    }

    private fun <T> CoefOps<T>.twentyThree(): T where T : Ring<T> {
        return six + six + six + five
    }

    private fun <T> CoefOps<T>.thirtyNine(): T where T : Ring<T> {
        return six + six + six + six + six + six + three
    }

    @Test
    fun sameSuiteShouldPassForInt64() {
        runSuite(
            CoefOps(
                zero = Int64.zero,
                one = Int64.one,
                two = Int64.two,
                three = Int64.three,
                four = Int64(4L),
                five = Int64(5L),
                six = Int64(6L)
            )
        )
    }

    @Test
    fun sameSuiteShouldPassForFlt64() {
        runSuite(
            CoefOps(
                zero = Flt64.zero,
                one = Flt64.one,
                two = Flt64.two,
                three = Flt64.three,
                four = Flt64(4.0),
                five = Flt64(5.0),
                six = Flt64(6.0)
            )
        )
    }
}




