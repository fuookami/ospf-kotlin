package fuookami.ospf.kotlin.core.symbol.function

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok

/** Dedicated contract test for the InStepRangeFunction symbol. */
class InStepRangeFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(InStepRangeFunction::class.java))
    }

    @Test
    fun returnsLargestSteppedValueNotGreaterThanUpperBound() {
        val upper = RealVar("step_upper")
        val function = InStepRangeFunction(
            lb = LinearPolynomial(emptyList(), Flt64.one),
            ub = LinearPolynomial(listOf(LinearMonomial(Flt64.one, upper)), Flt64.zero),
            step = Flt64(3.0),
            converter = IntoValue.Identity,
            name = "step_contract"
        )

        assertEquals(Flt64(10.0), function.evaluate(mapOf<Symbol, Flt64>(upper to Flt64(10.0))))
        assertEquals(Flt64(7.0), function.evaluate(mapOf<Symbol, Flt64>(upper to Flt64(9.0))))
        assertNull(function.evaluate(mapOf<Symbol, Flt64>(upper to Flt64.zero)))
    }

    @Test
    fun rejectsNonPositiveStep() {
        val bound = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        assertFailsWith<IllegalArgumentException> {
            InStepRangeFunction(bound, bound, Flt64.zero, converter = IntoValue.Identity)
        }
        assertFailsWith<IllegalArgumentException> {
            InStepRangeFunction(bound, bound, Flt64(-1.0), converter = IntoValue.Identity)
        }
    }

    @Test
    fun registersFloorHelpersAndBoundRowWithoutAnUnusedBigM() {
        val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        val function = InStepRangeFunction(
            lb = zero,
            ub = LinearPolynomial(emptyList(), Flt64(7.0)),
            step = Flt64(3.0),
            converter = IntoValue.Identity,
            name = "step_rows"
        )
        assertEquals(2, function.helperVariables.size, "floor integer and result helpers")

        val metaModel = LinearMetaModel<Flt64>(
            name = "step-constraints",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.minimize(zero) is Ok)
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            assertEquals(2, metaModel.tokens.tokens.size)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanism is Ok)

            val before = mechanism.value.constraints.size
            assertTrue(function.registerConstraints(mechanism.value) is Ok)
            val appended = mechanism.value.constraints.subList(before, mechanism.value.constraints.size)
            assertEquals(4, appended.size, "three Floor rows plus the range bound row")
            val names = appended.map { it.name }
            assertTrue("step_rows_q_floor_lb" in names)
            assertTrue("step_rows_q_floor_ub" in names)
            assertTrue("step_rows_q_floor_result" in names)
            assertTrue("step_rows_bounds" in names)
        } finally {
            metaModel.close()
        }
    }
}
