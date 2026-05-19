package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.symbol.operation.parseLinear
import fuookami.ospf.kotlin.math.symbol.operation.parseQuadratic
import fuookami.ospf.kotlin.math.symbol.operation.parseCanonical
import fuookami.ospf.kotlin.math.symbol.operation.parseLinearInequality
import fuookami.ospf.kotlin.math.symbol.operation.parseQuadraticInequality
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParserTypedEntryTest {
    @Test
    fun parseTypedPolynomialEntriesShouldReturnRet() {
        val linear = parseLinear("2*x + 3")
        assertNotNull(linear)

        val quadratic = parseQuadratic("x^2 + 2*x + 1")
        assertNotNull(quadratic)

        val canonical = parseCanonical("x^3 + x^2 + x + 1")
        assertNotNull(canonical)

        val notLinear = parseLinear("x^2 + 1")
        assertNull(notLinear)

        val notQuadratic = parseQuadratic("x^3 + 1")
        assertNull(notQuadratic)
    }

    @Test
    fun parseTypedInequalityEntriesShouldReturnRet() {
        val linearInequality = parseLinearInequality("x + y <= 3")
        assertNotNull(linearInequality)

        val quadraticInequality = parseQuadraticInequality("x^2 + y <= 3")
        assertNotNull(quadraticInequality)

        val notLinearInequality = parseLinearInequality("x^2 + y <= 3")
        assertNull(notLinearInequality)

        val notQuadraticInequality = parseQuadraticInequality("x^3 + y <= 3")
        assertNull(notQuadraticInequality)
    }
}