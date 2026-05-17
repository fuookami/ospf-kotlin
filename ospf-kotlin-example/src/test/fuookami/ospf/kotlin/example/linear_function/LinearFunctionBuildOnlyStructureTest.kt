package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
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
    private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

    @Test
    fun absAndSlackRangeShouldExposeExpectedConstraintShapeWithoutSolver() {
        val x = RealVar("example_linear_build_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val abs = fuookami.ospf.kotlin.core.intermediate_symbol.function.AbsFunction(
            polynomial = xPoly,
            name = "example_abs_build"
        )
        val slackRange = fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackRangeFunction(
            x = xPoly,
            lb = LinearPolynomial(emptyList(), -Flt64.two),
            ub = LinearPolynomial(emptyList(), Flt64.two),
            converter = flt64Converter,
            name = "example_slack_range_build"
        )

        val model = LinearMetaModel<Flt64>(
            name = "linear-build-only",
            converter = flt64Converter
        )

        try {
            assertTrue(model.add(x) is Ok)
            assertTrue(abs.registerAuxiliaryTokens(model.tokens) is Ok)
            assertTrue(slackRange.registerAuxiliaryTokens(model.tokens) is Ok)
            assertTrue(model.minimize(xPoly) is Ok)

            @Suppress("DEPRECATION")
            val mechanismRet = runBlocking {
                LinearMechanismModel.invoke<Flt64>(metaModel = model)
            }
            assertTrue(mechanismRet is Ok)
            val mechanismModel = requireNotNull(mechanismRet.value)

            val before = mechanismModel.constraints.size
            assertTrue(abs.registerConstraints(mechanismModel) is Ok)
            assertTrue(slackRange.registerConstraints(mechanismModel) is Ok)
            val appended = mechanismModel.constraints.subList(before, mechanismModel.constraints.size)

            assertEquals(4, appended.size, "abs(2条)+slackRange(2条) 应追加 4 条约束")
            val absRows = appended.filter { it.name.startsWith("example_abs_build") }
            val slackRows = appended.filter { it.name.startsWith("example_slack_range_build") }
            assertEquals(2, absRows.size)
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
