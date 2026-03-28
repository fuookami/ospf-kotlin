package fuookami.ospf.kotlin.core.frontend.symbol_migration.inequality_regression

import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomialSymbol
import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.frontend.inequality.Inequality
import fuookami.ospf.kotlin.core.frontend.inequality.LinearInequality
import fuookami.ospf.kotlin.core.frontend.inequality.QuadraticInequality
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.variable.RealVar
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InequalityNormalizeBaselineTest {
    @Test
    fun linearNormalizeToLessEqual_shouldFreezeCurrentGreaterEqualBehavior() {
        val x = RealVar("x")
        val y = RealVar("y")
        val inequality = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(2, x)),
                constant = Flt64(3.0)
            ),
            rhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(y)),
                constant = Flt64(-1.0)
            ),
            sign = Sign.GreaterEqual
        )

        val normalized = inequality.normalizeToLessEqual()

        assertEquals(Sign.Less, normalized.sign)
        assertNotNull(normalized.lhs.monomials.find { it.symbol.variable == x })
        assertNotNull(normalized.lhs.monomials.find { it.symbol.variable == y })
        assertTrue(normalized.rhs.constant eq Flt64(4.0))

        val lhsCoefficientByVariable = normalized.lhs.monomials.associate { it.symbol.variable!! to it.coefficient }
        assertTrue(lhsCoefficientByVariable[x]!! eq Flt64(-2.0))
        assertTrue(lhsCoefficientByVariable[y]!! eq Flt64(1.0))

        val values: Map<Symbol, Flt64> = mapOf(x to Flt64(2.0), y to Flt64(9.0))
        val originalTruth = inequalityTruthByValues(inequality, values)
        val normalizedTruth = inequalityTruthByValues(normalized, values)
        assertEquals(originalTruth, normalizedTruth)
    }

    @Test
    fun quadraticNormalizeToLessEqual_shouldFreezeCurrentGreaterBehavior() {
        val x = RealVar("x")
        val y = RealVar("y")
        val inequality = QuadraticInequality(
            lhs = QuadraticPolynomial(
                monomials = listOf(
                    x * x,
                    2 * (x * y),
                    QuadraticMonomial(3 * x)
                ),
                constant = Flt64(5.0)
            ),
            rhs = QuadraticPolynomial(
                monomials = listOf(y * y),
                constant = Flt64.one
            ),
            sign = Sign.Greater
        )

        val normalized = inequality.normalizeToLessEqual()

        assertEquals(Sign.LessEqual, normalized.sign)
        assertTrue(normalized.rhs.constant eq Flt64(4.0))

        val lhsCells = normalized.lhs.cells
        val xxCell = lhsCells.firstOrNull {
            it.isTriple &&
                    it.triple!!.variable1 == x &&
                    it.triple!!.variable2 == x
        }
        assertNotNull(xxCell)
        assertTrue(xxCell.triple!!.coefficient eq Flt64(-1.0))

        val yyCell = lhsCells.firstOrNull {
            it.isTriple &&
                    it.triple!!.variable1 == y &&
                    it.triple!!.variable2 == y
        }
        assertNotNull(yyCell)
        assertTrue(yyCell.triple!!.coefficient eq Flt64(1.0))

        val xyCell = lhsCells.firstOrNull {
            it.isTriple &&
                    it.triple!!.variable2 != null &&
                    setOf(it.triple!!.variable1, it.triple!!.variable2!!) == setOf(x, y)
        }
        assertNotNull(xyCell)
        assertTrue(xyCell.triple!!.coefficient eq Flt64(-2.0))

        val linearXCell = lhsCells.firstOrNull {
            it.isTriple &&
                    it.triple!!.variable1 == x &&
                    it.triple!!.variable2 == null
        }
        assertNotNull(linearXCell)
        assertTrue(linearXCell.triple!!.coefficient eq Flt64(-3.0))

        val values: Map<Symbol, Flt64> = mapOf(x to Flt64(2.0), y to Flt64(4.0))
        val originalTruth = inequalityTruthByValues(inequality, values)
        val normalizedTruth = inequalityTruthByValues(normalized, values)
        assertEquals(originalTruth, normalizedTruth)
    }

    @Test
    fun linearNormalizeToLessEqual_shouldSupportLinearIntermediateMonomial() {
        val x = RealVar("x")
        val expr = LinearExpressionSymbol(
            polynomial = LinearPolynomial(
                monomials = listOf(LinearMonomial(2, x)),
                constant = Flt64.one
            ),
            name = "expr_linear_norm"
        )
        val inequality = LinearInequality(
            lhs = LinearPolynomial(monomials = listOf(LinearMonomial(3, expr))),
            rhs = LinearPolynomial(monomials = listOf(LinearMonomial(expr))),
            sign = Sign.GreaterEqual
        )

        val normalized = inequality.normalizeToLessEqual()

        assertEquals(Sign.Less, normalized.sign)
        val exprMonomial = normalized.lhs.monomials.firstOrNull { it.symbol.exprSymbol == expr }
        assertNotNull(exprMonomial)
        assertTrue(exprMonomial.coefficient eq Flt64(-2.0))
    }

    @Test
    fun quadraticNormalizeToLessEqual_shouldSupportLinearIntermediateMonomial() {
        val x = RealVar("x")
        val expr = LinearExpressionSymbol(
            polynomial = LinearPolynomial(monomials = listOf(LinearMonomial(x))),
            name = "expr_quadratic_norm"
        )
        val inequality = QuadraticInequality(
            lhs = QuadraticPolynomial(monomials = listOf(QuadraticMonomial(3, x, expr))),
            rhs = QuadraticPolynomial(monomials = listOf(QuadraticMonomial(1, x, expr))),
            sign = Sign.Greater
        )

        val normalized = inequality.normalizeToLessEqual()

        assertEquals(Sign.LessEqual, normalized.sign)
        val target = QuadraticMonomialSymbol(x, expr)
        val exprMonomial = normalized.lhs.monomials.firstOrNull { it.symbol == target }
        assertNotNull(exprMonomial)
        assertTrue(exprMonomial.coefficient eq Flt64(-2.0))
    }

    private fun inequalityTruthByValues(
        inequality: Inequality<*, *>,
        values: Map<Symbol, Flt64>
    ): Boolean? {
        val lhs = inequality.lhs.evaluate(
            values = values,
            tokenList = null,
            zeroIfNone = false
        ) ?: return null
        val rhs = inequality.rhs.evaluate(
            values = values,
            tokenList = null,
            zeroIfNone = false
        ) ?: return null
        return inequality.sign(lhs, rhs)
    }
}




