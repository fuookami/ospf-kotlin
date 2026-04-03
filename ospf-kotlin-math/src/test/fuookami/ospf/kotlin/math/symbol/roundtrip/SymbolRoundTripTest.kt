package fuookami.ospf.kotlin.math.symbol.roundtrip

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import fuookami.ospf.kotlin.math.symbol.parser.parseSymbolExpression
import fuookami.ospf.kotlin.math.symbol.serde.symbolExprFromJson
import fuookami.ospf.kotlin.math.symbol.serde.toCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.serde.toExpr
import fuookami.ospf.kotlin.math.symbol.serde.toJsonString
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertTrue

class SymbolRoundTripTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    private val x = TestSymbol("x")
    private val y = TestSymbol("y")
    private val z = TestSymbol("z")
    private val symbolByName = mapOf(
        "x" to x,
        "y" to y,
        "z" to z
    )

    private fun randomTerm(random: Random): String {
        val coefficient = sequence {
            while (true) {
                val c = random.nextInt(-5, 6)
                if (c != 0) {
                    yield(c)
                }
            }
        }.first()

        val exponents = listOf(
            "x" to random.nextInt(0, 4),
            "y" to random.nextInt(0, 4),
            "z" to random.nextInt(0, 4)
        )

        val factors = mutableListOf<String>()
        for ((name, exp) in exponents) {
            if (exp == 1) {
                factors.add(name)
            } else if (exp > 1) {
                factors.add("$name^$exp")
            }
        }

        return if (factors.isEmpty()) {
            coefficient.toString()
        } else {
            "${coefficient}*${factors.joinToString("*")}"
        }
    }

    private fun randomPolynomialExpression(seed: Int, termCount: Int): String {
        val random = Random(seed)
        val terms = (0 until termCount).map { randomTerm(random) }
        return terms.joinToString(" + ")
    }

    private fun assertRoundTripEquivalent(expressionText: String) {
        val parsed = parseSymbolExpression(expressionText)
        val canonical = parsed.toCanonicalPolynomial { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }.combineTerms()

        val json = canonical.toExpr().toJsonString()
        val restoredExpr = symbolExprFromJson(json)
        val restoredCanonical = restoredExpr.toCanonicalPolynomial { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }.combineTerms()

        val samples = listOf(
            mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64(-1.0), z to Flt64(0.5)),
            mapOf<Symbol, Flt64>(x to Flt64(-3.0), y to Flt64(4.0), z to Flt64(2.0)),
            mapOf<Symbol, Flt64>(x to Flt64.one, y to Flt64.one, z to Flt64.one)
        )

        for (values in samples) {
            val lhs = canonical.evaluate(values)!!
            val rhs = restoredCanonical.evaluate(values)!!
            assertTrue((lhs - rhs).abs() <= Flt64(1e-9))
        }
    }

    @Test
    fun randomPolynomialRoundTripShouldPreserveSemantics() {
        for (seed in 0 until 20) {
            val expression = randomPolynomialExpression(seed = seed, termCount = 6)
            assertRoundTripEquivalent(expression)
        }
    }

    @Test
    fun highOrderPolynomialRoundTripShouldPreserveSemantics() {
        assertRoundTripEquivalent("2*x^5*y^2 - 3*x^2*y^3 + 4*z^4 - 7*x*y*z + 11")
    }
}
