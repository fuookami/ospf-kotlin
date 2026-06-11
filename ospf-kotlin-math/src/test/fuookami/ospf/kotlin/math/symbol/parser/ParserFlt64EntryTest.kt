package fuookami.ospf.kotlin.math.symbol.parser

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.operation.*

class ParserFlt64EntryTest {
    @Test
    fun parseFlt64PolynomialEntriesShouldReturnRet() {
        val linear = parseLinearFlt64("2*x + 3")
        assertNotNull(linear)

        val quadratic = parseQuadraticFlt64("x^2 + 2*x + 1")
        assertNotNull(quadratic)

        val canonical = parseCanonicalFlt64("x^3 + x^2 + x + 1")
        assertNotNull(canonical)

        val notLinear = parseLinearFlt64("x^2 + 1")
        assertNull(notLinear)

        val notQuadratic = parseQuadraticFlt64("x^3 + 1")
        assertNull(notQuadratic)
    }

    @Test
    fun parseFlt64InequalityEntriesShouldReturnRet() {
        val linearInequality = parseLinearInequalityFlt64("x + y <= 3")
        assertNotNull(linearInequality)

        val quadraticInequality = parseQuadraticInequalityFlt64("x^2 + y <= 3")
        assertNotNull(quadraticInequality)

        val notLinearInequality = parseLinearInequalityFlt64("x^2 + y <= 3")
        assertNull(notLinearInequality)

        val notQuadraticInequality = parseQuadraticInequalityFlt64("x^3 + y <= 3")
        assertNull(notQuadraticInequality)
    }
}
