package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuadraticElasticModelTest {
    @Test
    fun elasticShouldKeepEmptyModelStable() {
        val model = QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = emptyList(),
                constraints = QuadraticConstraintBatch(
                    sparseLhs = SparseQuadraticMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "empty-quadratic"
            ),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(ObjectCategory.Minimum, emptyList())
        )

        val elastic = model.elastic()

        assertEquals(0, elastic.variables.size)
        assertEquals(0, elastic.constraints.size)
        assertEquals(0, elastic.objective.objective.size)
    }

    @Test
    fun elasticShouldAppendSlackVariablesForInequalityConstraint() {
        val variable = fuookami.ospf.kotlin.core.model.basic.Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64.one,
            type = Continuous,
            origin = null,
            name = "x"
        )
        val constraints = QuadraticConstraintBatch(
            sparseLhs = SparseQuadraticMatrix().also { matrix ->
                val row = SparseQuadraticVector()
                row.add(0, null, Flt64.one)
                matrix.addRow(row)
            },
            signs = listOf(ConstraintRelation.LessEqual),
            rhs = listOf(Flt64.zero),
            names = listOf("c0"),
            sources = listOf(ConstraintSource.Origin)
        )
        val model = QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = listOf(variable),
                constraints = constraints,
                name = "single-constraint"
            ),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(ObjectCategory.Minimum, emptyList())
        )

        val elastic = model.elastic()

        assertTrue(elastic.variables.size > model.variables.size)
        assertTrue(elastic.constraints.size >= model.constraints.size)
        assertTrue(elastic.objective.objective.isNotEmpty())
    }
}
