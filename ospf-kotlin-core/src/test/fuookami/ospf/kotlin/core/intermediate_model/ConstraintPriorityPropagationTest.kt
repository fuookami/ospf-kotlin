package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ConstraintSource
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseQuadraticMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseQuadraticVector
import fuookami.ospf.kotlin.core.model.intermediate.SparseVector
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConstraintPriorityPropagationTest {
    @Test
    fun linearConstraintShouldKeepPriorityThroughCopyAndFilter() {
        val constraint = LinearConstraintBatch(
            sparseLhs = SparseMatrix<Flt64>().also { mat ->
                val sv = SparseVector<Flt64>()
                sv.add(0, Flt64.one)
                mat.addRow(sv)
            },
            signs = listOf(ConstraintRelation.LessEqual),
            rhs = listOf(Flt64.one),
            names = listOf("c0"),
            sources = listOf(ConstraintSource.Origin),
            priorities = listOf(7)
        )

        assertEquals(7, constraint.priorities.first())
        assertEquals(7, constraint.copy().priorities.first())
        assertEquals(7, constraint.filter { it == 0 }.priorities.first())
    }

    @Test
    fun linearConstraintShouldUseNullPriorityAsDefault() {
        val constraint = LinearConstraintBatch(
            sparseLhs = SparseMatrix<Flt64>().also { mat ->
                val sv = SparseVector<Flt64>()
                sv.add(0, Flt64.one)
                mat.addRow(sv)
            },
            signs = listOf(ConstraintRelation.LessEqual),
            rhs = listOf(Flt64.one),
            names = listOf("c0"),
            sources = listOf(ConstraintSource.Origin)
        )

        assertNull(constraint.priorities.first())
    }

    @Test
    fun quadraticConstraintShouldKeepPriorityThroughCopy() {
        val constraint = QuadraticConstraintBatch(
            sparseLhs = SparseQuadraticMatrix().also { mat ->
                val sv = SparseQuadraticVector()
                sv.add(0, null, Flt64.one)
                mat.addRow(sv)
            },
            signs = listOf(ConstraintRelation.LessEqual),
            rhs = listOf(Flt64.one),
            names = listOf("qc0"),
            sources = listOf(ConstraintSource.Origin),
            priorities = listOf(11)
        )

        assertEquals(11, constraint.priorities.first())
        assertEquals(11, constraint.copy().priorities.first())
    }
}
