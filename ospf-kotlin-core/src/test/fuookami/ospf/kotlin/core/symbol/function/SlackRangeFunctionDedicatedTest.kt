package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

/** [SlackRangeFunction] 契约测试。 / Dedicated contract tests. */
class SlackRangeFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(SlackRangeFunction::class.java))
    }

    @Test
    fun distanceIsExactOnAllThreeRegions() {
        val x = RealVar("slack_range_contract_x")
        val function = SlackRangeFunction(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            lower = Flt64.one,
            upper = Flt64(3.0),
            converter = IntoValue.Identity,
            name = "slack_range_contract"
        )

        assertEquals(Flt64.one, function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero)))
        assertEquals(Flt64.zero, function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.two)))
        assertEquals(Flt64.one, function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(4.0))))
        assertEquals(4, function.helperVariables.size)
    }

    @Test
    fun rejectsReversedBounds() {
        assertFailsWith<IllegalArgumentException> {
            SlackRangeFunction(
                input = LinearPolynomial<Flt64>(emptyList(), Flt64.zero),
                lower = Flt64.two,
                upper = Flt64.one,
                converter = IntoValue.Identity,
                name = "invalid_slack_range"
            )
        }
    }

    @Test
    fun registersExactThreeCandidateMaxRowsWithExplicitBigM() {
        val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        val function = SlackRangeFunction(
            input = LinearPolynomial(emptyList(), Flt64(4.0)),
            lower = Flt64.one,
            upper = Flt64(3.0),
            bigM = Flt64(20.0),
            converter = IntoValue.Identity,
            name = "slack_rows"
        )
        assertEquals(4, function.helperVariables.size, "result plus three max selectors")

        val metaModel = LinearMetaModel<Flt64>(
            name = "slack-range-constraints",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.minimize(zero) is Ok)
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            assertEquals(4, metaModel.tokens.tokens.size)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanism is Ok)

            val before = mechanism.value.constraints.size
            assertTrue(function.registerConstraints(mechanism.value) is Ok)
            val appended = mechanism.value.constraints.subList(before, mechanism.value.constraints.size)
            assertEquals(7, appended.size, "three lower, three upper, and one selector-sum row")
            assertEquals(3, appended.count { it.sign == ConstraintRelation.GreaterEqual })
            assertEquals(3, appended.count { it.sign == ConstraintRelation.LessEqual })
            assertEquals(1, appended.count { it.sign == ConstraintRelation.Equal })
            assertTrue(appended.any { row ->
                row.lhs.filterIsInstance<LinearCell<Flt64>>()
                    .any { cell -> cell.coefficient == Flt64(20.0) }
            })
            assertEquals(Flt64.one, function.evaluate(emptyMap()))
        } finally {
            metaModel.close()
        }
    }
}
