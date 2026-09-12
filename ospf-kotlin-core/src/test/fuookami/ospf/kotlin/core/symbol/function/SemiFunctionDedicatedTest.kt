package fuookami.ospf.kotlin.core.symbol.function

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok

/** Dedicated contract test for the SemiFunction symbol. */
class SemiFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(SemiFunction::class.java))
        assertTrue(HasResultVariable::class.java.isAssignableFrom(SemiFunction::class.java))
        assertTrue(HasResultPolynomial::class.java.isAssignableFrom(SemiFunction::class.java))
    }

    @Test
    fun exposesSharedDefaultBoundsAndRegistersBothDomainRows() {
        val default = SemiFunction<Flt64>(converter = IntoValue.Identity, name = "semi_default")
        assertEquals(Flt64.zero, default.lb)
        assertEquals(Flt64(1e6), default.ub)
        assertEquals(2, default.helperVariables.size)

        val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        val metaModel = LinearMetaModel<Flt64>(
            name = "semi-constraints",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.minimize(zero) is Ok)
            val function = SemiFunction(
                lb = Flt64(2.0),
                ub = Flt64(5.0),
                converter = IntoValue.Identity,
                name = "semi_rows"
            )
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            assertEquals(2, metaModel.tokens.tokens.size)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanism is Ok)

            val before = mechanism.value.constraints.size
            assertTrue(function.registerConstraints(mechanism.value) is Ok)
            val appended = mechanism.value.constraints.subList(before, mechanism.value.constraints.size)
            assertEquals(2, appended.size)
            val names = appended.map { it.name }
            assertTrue("semi_rows_semi_upper" in names)
            assertTrue("semi_rows_semi_lower" in names)
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun rejectsReversedOrNonFiniteBoundsBeforeRegistration() {
        assertFailsWith<IllegalArgumentException> {
            SemiFunction(
                lb = Flt64(5.0),
                ub = Flt64(2.0),
                converter = IntoValue.Identity,
                name = "semi_reversed"
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SemiFunction(
                lb = Flt64.infinity,
                ub = Flt64(2.0),
                converter = IntoValue.Identity,
                name = "semi_infinite"
            )
        }
    }
}
