package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ConstraintSource
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl

class LinearTriadDualSolutionTest {
    @Test
    fun tidyDualSolutionShouldPreserveZeroDualWithConstraintOrigin() {
        val origin = LinearConstraintImpl<Flt64>(
            lhs = emptyList(),
            sign = ConstraintRelation.LessEqual,
            rhs = Flt64.zero,
            name = "zero-dual-origin"
        )
        val sparseLhs = SparseMatrix<Flt64>().also { matrix ->
            matrix.addRow(SparseVector())
        }
        val constraints = LinearConstraintBatch(
            sparseLhs = sparseLhs,
            signs = listOf(ConstraintRelation.LessEqual),
            rhs = listOf(Flt64.zero),
            names = listOf("zero-dual-origin"),
            sources = listOf(ConstraintSource.Origin),
            origins = listOf(origin)
        )
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = emptyList(),
                constraints = constraints,
                name = "zero-dual-model"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )

        val dualSolution = model.tidyDualSolution(listOf(Flt64.zero))

        assertTrue(dualSolution.containsKey(origin))
        assertEquals(Flt64.zero, dualSolution[origin])
    }
}
