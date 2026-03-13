package fuookami.ospf.kotlin.core.frontend.symbol_migration.inequality_regression

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*

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
