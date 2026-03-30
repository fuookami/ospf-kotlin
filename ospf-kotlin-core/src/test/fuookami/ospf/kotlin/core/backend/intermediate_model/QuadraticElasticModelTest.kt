package fuookami.ospf.kotlin.core.backend.intermediate_model

import fuookami.ospf.kotlin.core.frontend.model.mechanism.ObjectCategory
import fuookami.ospf.kotlin.core.frontend.model.mechanism.Sign
import fuookami.ospf.kotlin.core.frontend.variable.Continuous
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class QuadraticElasticModelTest {
    @Test
    fun elasticShouldKeepEmptyModelStable() {
        val model = QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = emptyList(),
                constraints = QuadraticConstraint(
                    lhs = emptyList(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "empty-quadratic"
            ),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )

        val elastic = model.elastic()

        assertEquals(0, elastic.variables.size)
        assertEquals(0, elastic.constraints.size)
        assertEquals(0, elastic.objective.objective.size)
    }

    @Test
    fun elasticShouldAppendConstraintAndBoundSlackVariables() {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64.one,
            type = Continuous,
            origin = null,
            name = "x"
        )
        val constraints = QuadraticConstraint(
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
            objective = QuadraticObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )

        val elastic = model.elastic()

        assertEquals(4, elastic.variables.size)
        assertEquals(3, elastic.constraints.size)
        assertEquals(3, elastic.objective.objective.size)
        assertEquals(ConstraintSource.Elastic, elastic.constraints.sources[0])
        assertEquals(ConstraintSource.ElasticLowerBound, elastic.constraints.sources[1])
        assertEquals(ConstraintSource.ElasticUpperBound, elastic.constraints.sources[2])
    }
}
