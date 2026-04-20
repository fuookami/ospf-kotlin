package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.core.variable.Variable
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.test.Test
import kotlin.test.assertFailsWith

class QuadraticDualUnsupportedTest {
    @Test
    fun dualShouldThrowUnsupportedOperationException() {
        val model = QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = listOf(
                    Variable(
                        index = 0,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.one,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = null,
                        name = "x",
                        initialResult = Flt64.zero
                    )
                ),
                constraints = QuadraticConstraintBatch(
                    sparseLhs = SparseQuadraticMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "dual-unsupported-test"
            ),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(ObjectCategory.Minimum, emptyList())
        )

        assertFailsWith<UnsupportedOperationException> {
            kotlinx.coroutines.runBlocking { model.dual() }
        }
    }

    @Test
    fun farkasDualShouldThrowUnsupportedOperationException() {
        val model = QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = listOf(
                    Variable(
                        index = 0,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.one,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = null,
                        name = "x",
                        initialResult = Flt64.zero
                    )
                ),
                constraints = QuadraticConstraintBatch(
                    sparseLhs = SparseQuadraticMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "farkas-dual-unsupported-test"
            ),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(ObjectCategory.Minimum, emptyList())
        )

        assertFailsWith<UnsupportedOperationException> {
            kotlinx.coroutines.runBlocking { model.farkasDual() }
        }
    }
}
