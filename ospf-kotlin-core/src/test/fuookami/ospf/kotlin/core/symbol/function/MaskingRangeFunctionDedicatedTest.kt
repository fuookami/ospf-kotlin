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
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok

/** Dedicated contract test for the MaskingRangeFunction symbol. */
class MaskingRangeFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(MaskingRangeFunction::class.java))
    }

    @Test
    fun supportsSignedBoundsAndRequiresBothMaskAndResultForEvaluation() {
        val mask = BinVar("masking_range_mask")
        val function = MaskingRangeFunction(
            mask = LinearPolynomial(
                listOf(LinearMonomial(Flt64.one, mask)),
                Flt64.zero
            ),
            lower = Flt64(-2.0),
            upper = Flt64(3.0),
            converter = IntoValue.Identity,
            name = "signed_masking_range"
        )
        val on = mapOf<Symbol, Flt64>(
            mask to Flt64.one,
            function.resultVar to Flt64(-4.0)
        )
        assertEquals(Flt64(-2.0), function.evaluate(on))
        assertEquals(
            Flt64.zero,
            function.evaluate(mapOf<Symbol, Flt64>(mask to Flt64.zero, function.resultVar to Flt64(2.0)))
        )
        assertNull(function.evaluate(mapOf<Symbol, Flt64>(function.resultVar to Flt64(1.0))))
        assertNull(function.evaluate(mapOf<Symbol, Flt64>(mask to Flt64.one)))

        assertFailsWith<IllegalArgumentException> {
            MaskingRangeFunction(
                mask = LinearPolynomial(emptyList(), Flt64.zero),
                lower = Flt64(2.0),
                upper = Flt64(1.0),
                converter = IntoValue.Identity,
                name = "invalid_masking_range"
            )
        }
    }

    @Test
    fun registersSignedLowerAndUpperRows() {
        val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        val metaModel = LinearMetaModel<Flt64>(
            name = "masking-range-constraints",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.minimize(zero) is Ok)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanism is Ok)
            val function = MaskingRangeFunction(
                mask = zero,
                lower = Flt64(-2.0),
                upper = Flt64(3.0),
                converter = IntoValue.Identity,
                name = "signed_masking_rows"
            )
            val before = mechanism.value.constraints.size
            assertTrue(function.registerConstraints(mechanism.value) is Ok)
            assertEquals(before + 2, mechanism.value.constraints.size)
            val names = mechanism.value.constraints.map { it.name }
            assertTrue("signed_masking_rows_masking_range_ub" in names)
            assertTrue("signed_masking_rows_masking_range_lb" in names)
        } finally {
            metaModel.close()
        }
    }
}
