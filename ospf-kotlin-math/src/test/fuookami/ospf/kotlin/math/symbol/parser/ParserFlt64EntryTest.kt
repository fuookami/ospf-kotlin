package fuookami.ospf.kotlin.math.symbol.parser

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.symbol.operation.*

class ParserFlt64EntryTest {
    @Test
    fun parseFlt64PolynomialEntriesShouldReturnRet() {
        val linear = parseLinearFlt64("2*x + 3")
        assertTrue(linear is Ok)

        val quadratic = parseQuadraticFlt64("x^2 + 2*x + 1")
        assertTrue(quadratic is Ok)

        val canonical = parseCanonicalFlt64("x^3 + x^2 + x + 1")
        assertTrue(canonical is Ok)

        val notLinear = parseLinearFlt64("x^2 + 1")
        assertTrue(notLinear is Failed)

        val notQuadratic = parseQuadraticFlt64("x^3 + 1")
        assertTrue(notQuadratic is Failed)
    }

    @Test
    fun parseFlt64InequalityEntriesShouldReturnRet() {
        val linearInequality = parseLinearInequalityFlt64("x + y <= 3")
        assertTrue(linearInequality is Ok)

        val quadraticInequality = parseQuadraticInequalityFlt64("x^2 + y <= 3")
        assertTrue(quadraticInequality is Ok)

        val notLinearInequality = parseLinearInequalityFlt64("x^2 + y <= 3")
        assertTrue(notLinearInequality is Failed)

        val notQuadraticInequality = parseQuadraticInequalityFlt64("x^3 + y <= 3")
        assertTrue(notQuadraticInequality is Failed)
    }
}
