package fuookami.ospf.kotlin.core.symbol.function

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok

/** Dedicated contract test for the SlackFunction symbol. */
class SlackFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(SlackFunction::class.java))
    }

    @Test
    fun bothDirectionsRegisterExactAbsoluteValueRowsWithoutAnObjective() {
        val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        val metaModel = LinearMetaModel<Flt64>(
            name = "slack-threshold-both",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.minimize(zero) is Ok)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanism is Ok)
            val function = SlackFunction(
                x = zero,
                y = zero,
                withNegative = true,
                withPositive = true,
                threshold = true,
                converter = IntoValue.Identity,
                name = "slack_threshold"
            )
            assertEquals(3, function.helperVariables.size)
            val before = mechanism.value.constraints.size
            assertTrue(function.registerConstraints(mechanism.value) is Ok)
            assertEquals(before + 4, mechanism.value.constraints.size)
            val names = mechanism.value.constraints.map { it.name }
            assertTrue("slack_threshold_abs_ge_difference" in names)
            assertTrue("slack_threshold_abs_ge_negative_difference" in names)
            assertTrue("slack_threshold_abs_branch_positive" in names)
            assertTrue("slack_threshold_abs_branch_negative" in names)
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun evaluatesBothDirectionalAndOneSidedViolationSemantics() {
        val x = LinearPolynomial<Flt64>(emptyList(), Flt64(3.0))
        val y = LinearPolynomial<Flt64>(emptyList(), Flt64(1.0))
        val both = SlackFunction(
            x = x,
            y = y,
            converter = IntoValue.Identity,
            name = "slack_both"
        )
        val negativeOnly = SlackFunction(
            x = x,
            y = y,
            withNegative = true,
            withPositive = false,
            converter = IntoValue.Identity,
            name = "slack_negative"
        )
        val positiveOnly = SlackFunction(
            x = x,
            y = y,
            withNegative = false,
            withPositive = true,
            converter = IntoValue.Identity,
            name = "slack_positive"
        )
        assertEquals(Flt64(2.0), both.evaluate(emptyMap()))
        assertEquals(Flt64.zero, negativeOnly.evaluate(emptyMap()))
        assertEquals(Flt64(2.0), positiveOnly.evaluate(emptyMap()))
    }
}
