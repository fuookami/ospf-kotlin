package fuookami.ospf.kotlin.math.algebra.concept

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*

class ConceptPropertyTest {
    private data class BoundedBox(
        val value: Int
    ) : Bounded<BoundedBox>, Comparable<BoundedBox> {
        companion object {
            val lower = BoundedBox(0)
            val upper = BoundedBox(10)
        }

        override val isBounded: Boolean = true
        override val minBound: BoundedBox
            get() = lower
        override val maxBound: BoundedBox
            get() = upper

        override fun compareTo(other: BoundedBox): Int = value.compareTo(other.value)
    }

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

    @Test
    fun derivedTraitHelpersShouldStayConsistent() {
        val bounded = BoundedBox(5)
        assertTrue(bounded.isWithinBounds(BoundedBox(5)))
        assertEquals(BoundedBox(0), bounded.clampToBounds(BoundedBox(-3)))
        assertTrue(Flt64.one.hasEpsilon)
        assertTrue(Flt64.infinity.isPositiveInfinity())
        assertTrue(Flt64.negativeInfinity.isNegativeInfinity())
        assertTrue(Flt64.negativeInfinity.isInfinite())
        assertFalse(Flt64.one.isInfinite())
        assertFalse(Flt64.one.isDegenerate)
    }
}
