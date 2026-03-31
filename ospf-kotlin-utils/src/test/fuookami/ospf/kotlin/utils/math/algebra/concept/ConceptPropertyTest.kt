package fuookami.ospf.kotlin.utils.math.algebra.concept

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.Int32
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt32
import fuookami.ospf.kotlin.utils.operator.eq
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ConceptPropertyTest {
    private fun <T> assertNeutralElements(value: T)
            where T : RealNumber<T>, T : NumberField<T> {
        assertTrue((value + value.constants.zero) eq value)
        assertTrue((value * value.constants.one) eq value)
    }

    @Test
    fun neutralElementContractsShouldHoldForRepresentativeTypes() {
        assertNeutralElements(Int32(7))
        assertNeutralElements(Flt64(7.5))
    }

    @Test
    fun traitFlagsShouldMatchConstantCapabilities() {
        assertTrue(Flt64.one.isBounded)
        assertTrue(Flt64.one.supportsInfinity)
        assertTrue(Flt64.one.isFixed)

        assertTrue(Int32.one.isBounded)
        assertTrue(!Int32.one.supportsInfinity)
        assertTrue(!Int32.one.isFixed)

        assertTrue(UInt32.one.isBounded)
        assertTrue(!UInt32.one.supportsInfinity)
        assertTrue(!UInt32.one.isFixed)
    }

    @Test
    fun constantsShouldStayInternallyConsistent() {
        assertTrue((Flt64.one + Flt64.one) eq Flt64.two)
        assertTrue((Flt64.two + Flt64.one) eq Flt64.three)
        assertTrue((Flt64.five * Flt64.two) eq Flt64.ten)

        assertTrue((Int32.one + Int32.one) eq Int32.two)
        assertTrue((Int32.two + Int32.one) eq Int32.three)
        assertTrue((Int32.five * Int32.two) eq Int32.ten)

        assertTrue((Flt64.half + Flt64.half) eq Flt64.one)
    }
}
