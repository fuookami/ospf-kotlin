package fuookami.ospf.kotlin.core.backend.intermediate_model

import fuookami.ospf.kotlin.core.frontend.model.mechanism.Sign
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConstraintPriorityPropagationTest {
    @Test
    fun linearConstraintShouldKeepPriorityThroughCopyAndFilter() {
        val constraint = LinearConstraint(
            lhs = listOf(listOf(LinearConstraintCell(rowIndex = 0, colIndex = 0, coefficient = Flt64.one))),
            signs = listOf(Sign.LessEqual),
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
        val constraint = LinearConstraint(
            lhs = listOf(listOf(LinearConstraintCell(rowIndex = 0, colIndex = 0, coefficient = Flt64.one))),
            signs = listOf(Sign.LessEqual),
            rhs = listOf(Flt64.one),
            names = listOf("c0"),
            sources = listOf(ConstraintSource.Origin)
        )

        assertNull(constraint.priorities.first())
    }

    @Test
    fun quadraticConstraintShouldKeepPriorityThroughCopy() {
        val constraint = QuadraticConstraint(
            lhs = listOf(
                listOf(
                    QuadraticConstraintCell(
                        rowIndex = 0,
                        colIndex1 = 0,
                        colIndex2 = null,
                        coefficient = Flt64.one
                    )
                )
            ),
            signs = listOf(Sign.LessEqual),
            rhs = listOf(Flt64.one),
            names = listOf("qc0"),
            sources = listOf(ConstraintSource.Origin),
            priorities = listOf(11)
        )

        assertEquals(11, constraint.priorities.first())
        assertEquals(11, constraint.copy().priorities.first())
    }
}

