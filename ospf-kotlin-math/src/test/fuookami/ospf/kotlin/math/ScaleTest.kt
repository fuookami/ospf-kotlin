package fuookami.ospf.kotlin.math

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.math.algebra.number.*

class ScaleTest {
    @Test
    fun multiplyAndDivideSingleBaseShouldUpdateExponent() {
        val base = FltX(10L)
        val scale = assertNotNull(Scale(base, 2) * base / base)

        assertEquals(1, scale.scales.size)
        assertTrue(scale.scales.first().second eq FltX(2L))
    }

    @Test
    fun divideBySameBaseShouldTidyZeroExponent() {
        val base = FltX(10L)
        val unit = assertNotNull(Scale(base, 1) / base)

        assertTrue(unit.scales.isEmpty())
        assertTrue(unit.value!! eq FltX.one)
    }

    @Test
    fun divideByZeroShouldReturnNullableOrFailedResult() {
        val scale = Scale(10, 2)

        assertNull(scale.divOrNull(FltX.zero))
        assertTrue(scale.divSafe(FltX.zero).failed)
    }

    @Test
    fun multiplyScaleShouldMergeSameBases() {
        val merged = Scale(10, 2) * Scale(10, 3)

        assertEquals(1, merged.scales.size)
        assertTrue(merged.scales.first().second eq FltX(5L))
    }

    @Test
    fun divideScaleShouldSubtractExponents() {
        val merged = Scale(10, 5) / Scale(10, 3)

        assertEquals(1, merged.scales.size)
        assertTrue(merged.scales.first().second eq FltX(2L))
        assertTrue(merged.value!! eq FltX(100L))
    }

    @Test
    fun multiplyScaleShouldKeepFltAndRtnBasesSeparated() {
        val mixed = Scale(FltX(10L), 1) * Scale(RtnX(10, 1), 1)

        assertEquals(2, mixed.scales.size)
        assertEquals(1, mixed.scales.count { it.first is Either.Left<*, *> })
        assertEquals(1, mixed.scales.count { it.first is Either.Right<*, *> })
    }
}
