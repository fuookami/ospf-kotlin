package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.model.basic.*

class ConstraintRelationTest {
    @Test
    fun fromComparisonShouldMapCorrectly() {
        assertEquals(ConstraintRelation.LessEqual, ConstraintRelation.ofSafe(Comparison.LE).valueOrFail())
        assertEquals(ConstraintRelation.LessEqual, ConstraintRelation.ofSafe(Comparison.LT).valueOrFail())
        assertEquals(ConstraintRelation.Equal, ConstraintRelation.ofSafe(Comparison.EQ).valueOrFail())
        assertEquals(ConstraintRelation.GreaterEqual, ConstraintRelation.ofSafe(Comparison.GE).valueOrFail())
        assertEquals(ConstraintRelation.GreaterEqual, ConstraintRelation.ofSafe(Comparison.GT).valueOrFail())
    }

    @Test
    fun fromComparisonShouldFailOnNE() {
        assertNull(ConstraintRelation.ofOrNull(Comparison.NE))
        assertTrue(ConstraintRelation.ofSafe(Comparison.NE).failed)
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
            val roundTripped = ConstraintRelation.ofSafe(comparison).valueOrFail()
            assertEquals(relation, roundTripped, "Round trip failed for $relation")
        }
    }

    @Test
    fun signTypealiasShouldWork() {
        val sign: ConstraintRelation = ConstraintRelation.Equal
        assertEquals(ConstraintRelation.Equal, sign)
    }
}
