package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int32
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ConstantProviderTest {
    private fun <T> addOne(
        value: T,
        constants: ArithmeticConst<T>,
        add: (T, T) -> T
    ): T {
        return add(value, constants.one)
    }

    private fun <T> clampByBounds(
        value: T,
        constants: RealConst<T>,
        ord: (T, T) -> Int
    ): T {
        return when {
            ord(value, constants.minimum) < 0 -> constants.minimum
            ord(value, constants.maximum) > 0 -> constants.maximum
            else -> value
        }
    }

    @Test
    fun arithmeticConstShouldWorkWithExplicitCompanionProvider() {
        val intResult = addOne(Int32(7), Int32) { lhs, rhs -> lhs + rhs }
        val fltResult = addOne(Flt64(7.5), Flt64) { lhs, rhs -> lhs + rhs }

        assertTrue(intResult eq Int32(8))
        assertTrue(fltResult eq Flt64(8.5))
    }

    @Test
    fun realConstShouldExposeBoundsAndScaleConstants() {
        val clamped = clampByBounds(Flt64.infinity!!, Flt64) { lhs, rhs ->
            when {
                lhs < rhs -> -1
                lhs > rhs -> 1
                else -> 0
            }
        }

        assertTrue((Flt64.one + Flt64.one) eq Flt64.two)
        assertTrue((Flt64.five * Flt64.two) eq Flt64.ten)
        assertTrue(clamped eq Flt64.maximum)
    }

    @Test
    fun floatingConstShouldExposeHalfAndTranscendentals() {
        assertTrue((Flt64.half + Flt64.half) eq Flt64.one)
        assertTrue(Flt64.lg2 > Flt64.zero)
        assertTrue(Flt64.lg2 < Flt64.one)
        assertTrue(Flt64.pi > Flt64.three)
        assertTrue(Flt64.e > Flt64.two)
    }
}