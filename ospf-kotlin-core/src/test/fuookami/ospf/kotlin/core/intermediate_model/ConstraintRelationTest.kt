package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.InvalidConstraintSignFromComparison
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConstraintRelationTest {
    @Test
    fun fromComparisonShouldMapCorrectly() {
        assertEquals(ConstraintRelation.LessEqual, ConstraintRelation(Comparison.LE))
        assertEquals(ConstraintRelation.LessEqual, ConstraintRelation(Comparison.LT))
        assertEquals(ConstraintRelation.Equal, ConstraintRelation(Comparison.EQ))
        assertEquals(ConstraintRelation.GreaterEqual, ConstraintRelation(Comparison.GE))
        assertEquals(ConstraintRelation.GreaterEqual, ConstraintRelation(Comparison.GT))
    }

    @Test
    fun fromComparisonShouldThrowOnNE() {
        try {
            ConstraintRelation(Comparison.NE)
            assertTrue(false, "Should have thrown")
        } catch (_: InvalidConstraintSignFromComparison) {
            // expected
        }
    }

    @Test
    fun toComparisonShouldBeBijective() {
        assertEquals(Comparison.LE, ConstraintRelation.LessEqual.toComparison())
        assertEquals(Comparison.EQ, ConstraintRelation.Equal.toComparison())
        assertEquals(Comparison.GE, ConstraintRelation.GreaterEqual.toComparison())
    }

    @Test
    fun reverseShouldFlip() {
        assertEquals(ConstraintRelation.GreaterEqual, ConstraintRelation.LessEqual.reverse)
        assertEquals(ConstraintRelation.LessEqual, ConstraintRelation.GreaterEqual.reverse)
        assertEquals(ConstraintRelation.Equal, ConstraintRelation.Equal.reverse)
    }

    @Test
    fun roundTripShouldPreserveRelation() {
        for (relation in ConstraintRelation.entries) {
            val comparison = relation.toComparison()
            val roundTripped = ConstraintRelation(comparison)
            assertEquals(relation, roundTripped, "Round trip failed for $relation")
        }
    }

    @Test
    fun signTypealiasShouldWork() {
        val sign: ConstraintRelation = ConstraintRelation.Equal
        assertEquals(ConstraintRelation.Equal, sign)
    }
}
