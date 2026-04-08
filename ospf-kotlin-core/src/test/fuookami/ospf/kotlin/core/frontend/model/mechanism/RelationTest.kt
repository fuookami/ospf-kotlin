package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.inequality.LinearInequality
import fuookami.ospf.kotlin.core.frontend.inequality.QuadraticInequality
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for LinearRelation and QuadraticRelation types
 *
 * These tests verify that the new Relation types can:
 * 1. Be created from flatten data
 * 2. Convert from legacy Inequality types (adapter)
 * 3. Normalize correctly
 * 4. Produce constraints via the new constructors
 */
class RelationTest {

    // ========== LinearRelation Tests ==========

    @Test
    fun linearRelation_fromFlattenData_shouldWorkCorrectly() {
        val x = RealVar("x")
        val y = RealVar("y")

        val flattenData = LinearFlattenData(
            monomials = listOf(
                fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(Flt64(2.0), x),
                fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(Flt64(3.0), y)
            ),
            constant = Flt64(5.0)
        )

        val relation = LinearRelationImpl(
            flattenData = flattenData,
            sign = Sign.LessEqual,
            name = "test_relation"
        )

        assertEquals(2, relation.flattenData.monomials.size)
        assertTrue(relation.flattenData.constant eq Flt64(5.0))
        assertEquals(Sign.LessEqual, relation.sign)
    }

    @Test
    fun linearRelation_normalize_shouldConvertGreaterToLess() {
        val x = RealVar("x")

        val flattenData = LinearFlattenData(
            monomials = listOf(
                fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(Flt64(2.0), x)
            ),
            constant = Flt64(5.0)
        )

        val relation = LinearRelationImpl(
            flattenData = flattenData,
            sign = Sign.Greater,
            name = "test_relation"
        )

        val normalized = relation.normalize()

        assertEquals(Sign.Less, normalized.sign)
        assertTrue(normalized.flattenData.monomials[0].coefficient eq Flt64(-2.0))
        assertTrue(normalized.flattenData.constant eq Flt64(-5.0))
    }

    @Suppress("DEPRECATION")
    @Test
    fun linearRelation_fromInequality_shouldPreserveData() {
        val x = RealVar("x")
        val y = RealVar("y")

        val inequality = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(2.0, x)),
                constant = Flt64(3.0)
            ),
            rhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(y)),
                constant = Flt64(1.0)
            ),
            sign = Sign.LessEqual
        )

        val relation = LinearRelationImpl.from(inequality)

        assertEquals(inequality.sign, relation.sign)
        assertEquals(inequality.flattenedMonomials, relation.flattenData)
    }

    // ========== QuadraticRelation Tests ==========

    @Test
    fun quadraticRelation_fromFlattenData_shouldWorkCorrectly() {
        val x = RealVar("x")
        val y = RealVar("y")

        val flattenData = QuadraticFlattenData(
            monomials = listOf(
                fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial(Flt64(2.0), x, y)
            ),
            constant = Flt64(5.0)
        )

        val relation = QuadraticRelationImpl(
            flattenData = flattenData,
            sign = Sign.LessEqual,
            name = "test_relation"
        )

        assertEquals(1, relation.flattenData.monomials.size)
        assertTrue(relation.flattenData.constant eq Flt64(5.0))
        assertEquals(Sign.LessEqual, relation.sign)
    }

    @Test
    fun quadraticRelation_normalize_shouldConvertGreaterEqualToLessEqual() {
        val x = RealVar("x")
        val y = RealVar("y")

        val flattenData = QuadraticFlattenData(
            monomials = listOf(
                fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial(Flt64(2.0), x, y)
            ),
            constant = Flt64(5.0)
        )

        val relation = QuadraticRelationImpl(
            flattenData = flattenData,
            sign = Sign.GreaterEqual,
            name = "test_relation"
        )

        val normalized = relation.normalize()

        assertEquals(Sign.LessEqual, normalized.sign)
        assertTrue(normalized.flattenData.monomials[0].coefficient eq Flt64(-2.0))
        assertTrue(normalized.flattenData.constant eq Flt64(-5.0))
    }

    @Suppress("DEPRECATION")
    @Test
    fun quadraticRelation_fromInequality_shouldPreserveData() {
        val x = RealVar("x")
        val y = RealVar("y")

        val inequality = QuadraticInequality(
            lhs = QuadraticPolynomial(
                monomials = listOf(x * y, QuadraticMonomial(2.0, x)),
                constant = Flt64(5.0)
            ),
            rhs = QuadraticPolynomial(
                monomials = listOf(x * x),
                constant = Flt64.one
            ),
            sign = Sign.LessEqual
        )

        val relation = QuadraticRelationImpl.from(inequality)

        assertEquals(inequality.sign, relation.sign)
        assertEquals(inequality.flattenedMonomials, relation.flattenData)
    }

    // ========== Adapter Extension Tests ==========

    @Suppress("DEPRECATION")
    @Test
    fun linearInequality_toRelation_shouldWork() {
        val x = RealVar("x")

        val inequality = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(2.0, x)),
                constant = Flt64.zero
            ),
            rhs = LinearPolynomial(constant = Flt64(10.0)),
            sign = Sign.LessEqual
        )

        val relation = inequality.toRelation()

        assertTrue(relation is LinearRelation)
        assertEquals(Sign.LessEqual, relation.sign)
    }

    @Suppress("DEPRECATION")
    @Test
    fun quadraticInequality_toRelation_shouldWork() {
        val x = RealVar("x")
        val y = RealVar("y")

        val inequality = QuadraticInequality(
            lhs = QuadraticPolynomial(
                monomials = listOf(x * x, y * y),
                constant = Flt64.zero
            ),
            rhs = QuadraticPolynomial(constant = Flt64(25.0)),
            sign = Sign.LessEqual
        )

        val relation = inequality.toRelation()

        assertTrue(relation is QuadraticRelation)
        assertEquals(Sign.LessEqual, relation.sign)
    }
}