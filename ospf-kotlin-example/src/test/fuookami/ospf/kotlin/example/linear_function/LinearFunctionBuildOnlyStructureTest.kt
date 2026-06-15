package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.example.test.flt64TestConverter
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LinearFunctionBuildOnlyStructureTest {
    @Test
    fun absAndSlackRangeShouldExposeExpectedConstraintShapeWithoutSolver() {
        val x = RealVar("example_linear_build_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val abs = fuookami.ospf.kotlin.core.symbol.function.AbsFunction(
            polynomial = xPoly,
            converter = flt64TestConverter,
            name = "example_abs_build"
        )
        val slackRange = fuookami.ospf.kotlin.core.symbol.function.SlackRangeFunction(
            x = xPoly,
            lb = LinearPolynomial(emptyList(), -Flt64.two),
            ub = LinearPolynomial(emptyList(), Flt64.two),
            converter = flt64TestConverter,
            name = "example_slack_range_build"
        )

        val model = LinearMetaModel<Flt64>(
            name = "linear-build-only",
            converter = flt64TestConverter
        )

        try {
            assertTrue(model.add(x) is Ok)
            assertTrue(abs.registerAuxiliaryTokens(model.tokens) is Ok)
            assertTrue(slackRange.registerAuxiliaryTokens(model.tokens) is Ok)
            assertTrue(model.minimize(xPoly) is Ok)
            val mechanismRet = runBlocking {
                LinearMechanismModel.invoke<Flt64>(metaModel = model)
            }
            assertTrue(mechanismRet is Ok)
            val mechanismModel = requireNotNull(mechanismRet.value)

            val before = mechanismModel.constraints.size
            assertTrue(abs.registerConstraints(mechanismModel) is Ok)
            assertTrue(slackRange.registerConstraints(mechanismModel) is Ok)
            val appended = mechanismModel.constraints.subList(before, mechanismModel.constraints.size)

            assertEquals(6, appended.size, "abs(4条)+slackRange(2条) 应追加 6 条约束")
            val absRows = appended.filter { it.name.startsWith("example_abs_build") }
            val slackRows = appended.filter { it.name.startsWith("example_slack_range_build") }
            assertEquals(4, absRows.size)
            assertEquals(2, slackRows.size)

            assertTrue(absRows.any { it.sign == ConstraintRelation.Equal })
            assertTrue(slackRows.any { it.sign == ConstraintRelation.GreaterEqual })
            assertNotNull(abs.resultVar)
            assertNotNull(slackRange.pos)
            assertNotNull(slackRange.neg)
        } finally {
            model.close()
        }
    }
}
