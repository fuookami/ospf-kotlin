package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/** [BalanceTernaryzationFunction] 契约测试。 / Dedicated contract tests. */
class BalanceTernaryzationFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(BalanceTernaryzationFunction::class.java))
    }

    @Test
    fun evaluatesZeroBandAndRegistersExactThreeStateModel() {
        for ((input, expected) in listOf(
            Flt64(-2.0) to Flt64(-1.0),
            Flt64(-0.1) to Flt64.zero,
            Flt64.zero to Flt64.zero,
            Flt64(0.1) to Flt64.zero,
            Flt64(2.0) to Flt64.one
        )) {
            val function = BalanceTernaryzationFunction(
                x = LinearPolynomial(emptyList(), input),
                epsilon = Flt64(0.1),
                converter = IntoValue.Identity,
                name = "bter_$input"
            )
            assertEquals(expected, function.evaluate(emptyMap()))
            assertEquals(3, function.helperVariables.size)
        }

        val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        val metaModel = LinearMetaModel<Flt64>(
            name = "bter-exact-model",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.minimize(zero) is Ok)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanism is Ok)
            val function = BalanceTernaryzationFunction(
                x = zero,
                epsilon = Flt64(0.1),
                converter = IntoValue.Identity,
                name = "bter_constraints"
            )
            val before = mechanism.value.constraints.size
            assertTrue(function.registerConstraints(mechanism.value) is Ok)
            assertEquals(before + 6, mechanism.value.constraints.size)
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun evaluatorRejectsTheOpenStrictTransitionGap() {
        val epsilon = Flt64(0.1)
        val delta = Flt64(1e-10)
        val positiveBoundary = BalanceTernaryzationFunction(
            x = LinearPolynomial(emptyList(), epsilon + delta),
            epsilon = epsilon,
            converter = IntoValue.Identity,
            name = "bter_positive_boundary"
        )
        val negativeBoundary = BalanceTernaryzationFunction(
            x = LinearPolynomial(emptyList(), -epsilon - delta),
            epsilon = epsilon,
            converter = IntoValue.Identity,
            name = "bter_negative_boundary"
        )
        val positiveGap = BalanceTernaryzationFunction(
            x = LinearPolynomial(emptyList(), epsilon + delta * Flt64(0.5)),
            epsilon = epsilon,
            converter = IntoValue.Identity,
            name = "bter_positive_gap"
        )
        assertEquals(Flt64.one, positiveBoundary.evaluate(emptyMap()))
        assertEquals(Flt64(-1.0), negativeBoundary.evaluate(emptyMap()))
        assertNull(positiveGap.evaluate(emptyMap()))
    }

    @Test
    fun rejectsInvalidConfiguration() {
        val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        assertFailsWith<IllegalArgumentException> {
            BalanceTernaryzationFunction(
                x = zero,
                epsilon = Flt64(-0.1),
                converter = IntoValue.Identity,
                name = "negative_epsilon"
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BalanceTernaryzationFunction(
                x = zero,
                converter = IntoValue.Identity,
                name = "invalid_big_m",
                fallbackBigM = Flt64.zero
            )
        }
    }
}
