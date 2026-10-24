package fuookami.ospf.kotlin.math.symbol.polynomial

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*

class MutableLinearPolynomialMutationTest {
    private data class TestSymbol(override val name: String) : Symbol {
        override val displayName: String? = null
    }

    @Test
    fun `every mutable entry point advances the version while reads and value equality do not`() {
        val term = LinearMonomial(Flt64.two, TestSymbol("x"))
        val polynomial = LinearPolynomial(listOf(term), Flt64.one)
        val mutable = MutableLinearPolynomial(listOf(term), Flt64.one)
        val writes: List<(MutableLinearPolynomial<Flt64>) -> Unit> = listOf(
            { it.addMonomial(term) },
            { it.addConstant(Flt64.one) },
            { it.setConstant(Flt64.two) },
            { it += term },
            { it += polynomial },
            { it += polynomial.toMutable() },
            { it += Flt64.one },
            { it -= term },
            { it -= polynomial },
            { it -= polynomial.toMutable() },
            { it -= Flt64.one },
            { it *= Flt64.two },
            { it /= Flt64.two },
            { it.combineTerms(Flt64.zero) },
            { it.addAssignAndCombine(polynomial, Flt64.zero) },
            { it.minusAssignAndCombine(polynomial, Flt64.zero) },
            { it.clear() }
        )
        for (write in writes) {
            val version = mutable.mutationVersion
            write(mutable)
            assertTrue(mutable.mutationVersion > version)
            val after = mutable.mutationVersion
            val copy = mutable.toLinearPolynomial().toMutable()
            assertEquals(mutable, copy)
            assertEquals(mutable.hashCode(), copy.hashCode())
            assertEquals(0L, copy.mutationVersion)
            mutable.monomials
            mutable.constant
            mutable.toString()
            assertEquals(after, mutable.mutationVersion)
        }
    }

    @Test
    fun `self assignment and clear preserve original arithmetic and constant behavior`() {
        val term = LinearMonomial(Flt64.two, TestSymbol("x"))
        val mutable = MutableLinearPolynomial(listOf(term), Flt64.one)
        mutable += mutable
        assertEquals(listOf(term, term), mutable.monomials)
        assertEquals(Flt64.two, mutable.constant)
        mutable -= mutable
        assertEquals(listOf(term, term, -term, -term), mutable.monomials)
        assertEquals(Flt64.zero, mutable.constant)
        mutable.setConstant(Flt64(7))
        mutable.clear()
        assertEquals(emptyList(), mutable.monomials)
        assertEquals(Flt64(7), mutable.constant)
    }
}
