package fuookami.ospf.kotlin.math.algebra.concept

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*

/** 有限值必须排除 NaN 与无穷，而不限制任意精度实数的范围。 / Finiteness excludes NaN and infinity without bounding arbitrary-precision reals. */
class RealNumberFinitenessTest {
    @Test
    fun floatingNaNAndInfinitiesAreNotFinite() {
        assertFalse(Flt64(Double.NaN).isFinite())
        assertFalse(Flt64(Double.POSITIVE_INFINITY).isFinite())
        assertFalse(Flt64(Double.NEGATIVE_INFINITY).isFinite())
        assertFalse(Flt32(Float.NaN).isFinite())
        assertFalse(Flt32(Float.POSITIVE_INFINITY).isFinite())
        assertFalse(Flt32(Float.NEGATIVE_INFINITY).isFinite())
    }

    @Test
    fun ordinaryRealValuesRemainFinite() {
        assertTrue(Flt64.zero.isFinite())
        assertTrue(Flt64(-1.0).isFinite())
        assertTrue(Flt32.one.isFinite())
        assertTrue(FltX.one.isFinite())
        assertTrue(FltX("1e400").isFinite())
    }
}
