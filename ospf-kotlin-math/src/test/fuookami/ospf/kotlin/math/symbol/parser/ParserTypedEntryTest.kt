package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParserTypedEntryTest {
    @Test
    fun parseSymbolExpressionRetShouldReturnStructuredFailure() {
        val result = parseLegacySymbolExpressionRet("x + )")
        assertTrue(result is Failed<*, *, *>)
        val issue = (result as Failed<*, *, *>).errValue as? ParseIssue
        assertEquals(ParseIssueType.Syntax, issue?.type)
    }

    @Test
    fun parseSymbolInequalityRetShouldReturnStructuredFailure() {
        val result = parseLegacySymbolInequalityRet("x <=")
        assertTrue(result is Failed<*, *, *>)
        val issue = (result as Failed<*, *, *>).errValue as? ParseIssue
        assertEquals(ParseIssueType.Syntax, issue?.type)
    }

    @Test
    fun parseTypedPolynomialEntriesShouldReturnRet() {
        val linear = parseLinear("2*x + 3")
        assertTrue(linear is Ok<*, *, *>)

        val quadratic = parseQuadratic("x^2 + 2*x + 1")
        assertTrue(quadratic is Ok<*, *, *>)

        val canonical = parseCanonical("x^3 + x^2 + x + 1")
        assertTrue(canonical is Ok<*, *, *>)

        val notLinear = parseLinear("x^2 + 1")
        assertTrue(notLinear is Failed<*, *, *>)

        val notQuadratic = parseQuadratic("x^3 + 1")
        assertTrue(notQuadratic is Failed<*, *, *>)
    }

    @Test
    fun parseTypedInequalityEntriesShouldReturnRet() {
        val linearInequality = parseLinearInequality("x + y <= 3")
        assertTrue(linearInequality is Ok<*, *, *>)

        val quadraticInequality = parseQuadraticInequality("x^2 + y <= 3")
        assertTrue(quadraticInequality is Ok<*, *, *>)

        val notLinearInequality = parseLinearInequality("x^2 + y <= 3")
        assertTrue(notLinearInequality is Failed<*, *, *>)

        val notQuadraticInequality = parseQuadraticInequality("x^3 + y <= 3")
        assertTrue(notQuadraticInequality is Failed<*, *, *>)
    }
}
