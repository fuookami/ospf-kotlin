package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.solver.value.IntoValue

class ConstraintSignedZeroNormalizationTest {
    @Test
    fun linearConstraintTranslationNormalizesZeroAndPreservesNonzeroRhs() {
        val tokens = AutoTokenTable<Flt64>(Linear, false)
        try {
            val zero = Flt64.zero
            val negativeZero = Flt64(-0.0)
            val lessEqualZero = assertOk(
                LinearConstraintImpl(
                    relation = linearRelation(zero, Comparison.LE),
                    tokens = tokens,
                    converter = IntoValue.Identity
                )
            )
            val equalZero = assertOk(
                LinearConstraintImpl(
                    relation = linearRelation(negativeZero, Comparison.EQ),
                    tokens = tokens,
                    converter = IntoValue.Identity
                )
            )

            assertCanonicalZero(lessEqualZero.rhs.toDouble())
            assertCanonicalZero(equalZero.rhs.toDouble())
            assertEquals(true, lessEqualZero.isTrue())
            assertEquals(true, equalZero.isTrue())

            val positiveRhs = assertOk(
                LinearConstraintImpl(
                    relation = linearRelation(Flt64(-3.0), Comparison.LE),
                    tokens = tokens,
                    converter = IntoValue.Identity
                )
            )
            val negativeRhs = assertOk(
                LinearConstraintImpl(
                    relation = linearRelation(Flt64(4.0), Comparison.GE),
                    tokens = tokens,
                    converter = IntoValue.Identity
                )
            )
            assertEquals(3.0, positiveRhs.rhs.toDouble())
            assertEquals(-4.0, negativeRhs.rhs.toDouble())
            assertEquals(true, positiveRhs.isTrue())
            assertEquals(true, negativeRhs.isTrue())
        } finally {
            tokens.close()
        }
    }

    @Test
    fun quadraticConstraintTranslationNormalizesZeroAndPreservesNonzeroRhs() {
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        try {
            val zero = Flt64.zero
            val negativeZero = Flt64(-0.0)
            val lessEqualZero = assertOk(
                QuadraticConstraintImpl(
                    relation = quadraticRelation(zero, Comparison.LE),
                    tokens = tokens,
                    converter = IntoValue.Identity
                )
            )
            val equalZero = assertOk(
                QuadraticConstraintImpl(
                    relation = quadraticRelation(negativeZero, Comparison.EQ),
                    tokens = tokens,
                    converter = IntoValue.Identity
                )
            )

            assertCanonicalZero(lessEqualZero.rhs.toDouble())
            assertCanonicalZero(equalZero.rhs.toDouble())
            assertEquals(true, lessEqualZero.isTrue())
            assertEquals(true, equalZero.isTrue())

            val positiveRhs = assertOk(
                QuadraticConstraintImpl(
                    relation = quadraticRelation(Flt64(-3.0), Comparison.LE),
                    tokens = tokens,
                    converter = IntoValue.Identity
                )
            )
            val negativeRhs = assertOk(
                QuadraticConstraintImpl(
                    relation = quadraticRelation(Flt64(4.0), Comparison.GE),
                    tokens = tokens,
                    converter = IntoValue.Identity
                )
            )
            assertEquals(3.0, positiveRhs.rhs.toDouble())
            assertEquals(-4.0, negativeRhs.rhs.toDouble())
            assertEquals(true, positiveRhs.isTrue())
            assertEquals(true, negativeRhs.isTrue())
        } finally {
            tokens.close()
        }
    }

    private fun linearRelation(constant: Flt64, sign: Comparison) = LinearRelationImpl(
        flattenData = LinearFlattenData(
            monomials = emptyList(),
            constant = constant
        ),
        sign = sign
    )

    private fun quadraticRelation(constant: Flt64, sign: Comparison) = QuadraticRelationImpl(
        flattenData = QuadraticFlattenData(
            monomials = emptyList(),
            constant = constant
        ),
        sign = sign
    )

    private fun assertCanonicalZero(value: Double) {
        assertEquals(0.0.toRawBits(), value.toRawBits())
    }

    private fun <T> assertOk(result: Ret<T>): T = when (result) {
        is Ok -> result.value
        is Failed -> error(result.error.message)
        is Fatal -> error(result.errors.joinToString { it.message })
    }
}
