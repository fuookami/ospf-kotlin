package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import org.junit.jupiter.api.Test

@OptIn(kotlin.time.ExperimentalTime::class)
class DurationExtensionsTest {

    // ========================================================================
    // Quantity -> Duration 测试
    // ========================================================================

    @Test
    fun `quantity toDuration converts seconds correctly`() {
        val quantity = Flt64(5.0) * Second
        val result = quantity.toDuration()
        assertTrue(result is Ok)
        assertEquals(5.0, result.value.toDouble(DurationUnit.SECONDS), 1e-10)
    }

    @Test
    fun `quantity toDuration converts milliseconds correctly`() {
        val quantity = Flt64(500.0) * Millisecond
        val result = quantity.toDuration()
        assertTrue(result is Ok)
        assertEquals(0.5, result.value.toDouble(DurationUnit.SECONDS), 1e-10)
        assertEquals(500.0, result.value.toDouble(DurationUnit.MILLISECONDS), 1e-10)
    }

    @Test
    fun `quantity toDuration converts minutes correctly`() {
        val quantity = Flt64(2.0) * Minute
        val result = quantity.toDuration()
        assertTrue(result is Ok)
        assertEquals(120.0, result.value.toDouble(DurationUnit.SECONDS), 1e-10)
    }

    @Test
    fun `quantity toDuration converts hours correctly`() {
        val quantity = Flt64(1.5) * Hour
        val result = quantity.toDuration()
        assertTrue(result is Ok)
        assertEquals(90.0, result.value.toDouble(DurationUnit.MINUTES), 1e-10)
    }

    @Test
    fun `quantity toDuration converts days correctly`() {
        val quantity = Flt64(2.0) * Day
        val result = quantity.toDuration()
        assertTrue(result is Ok)
        assertEquals(48.0, result.value.toDouble(DurationUnit.HOURS), 1e-10)
    }

    @Test
    fun `quantity toDuration converts nanoseconds correctly`() {
        val quantity = Flt64(1_000_000.0) * Nanosecond
        val result = quantity.toDuration()
        assertTrue(result is Ok)
        assertEquals(1.0, result.value.toDouble(DurationUnit.MILLISECONDS), 1e-10)
    }

    @Test
    fun `quantity toDuration fails for nonTime quantity`() {
        val quantity = Flt64(5.0) * Meter
        val result = quantity.toDuration()
        assertTrue(result.failed)
    }

    @Test
    fun `quantity toDuration with unit converts correctly`() {
        val quantity = Flt64(5000.0) * Millisecond
        val result = quantity.toDuration(DurationUnit.SECONDS)
        assertTrue(result is Ok)
        assertEquals(5.0, result.value.toDouble(DurationUnit.SECONDS), 1e-10)
    }

    // ========================================================================
    // Duration -> Quantity 测试
    // ========================================================================

    @Test
    fun `duration toQuantity converts seconds correctly`() {
        val duration = 5.seconds
        val result = duration.toQuantity<Flt64>(Second)
        assertTrue(result is Ok)
        assertEquals(5.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Second, result.value.unit)
    }

    @Test
    fun `duration toQuantity converts milliseconds correctly`() {
        val duration = 500.milliseconds
        val result = duration.toQuantity<Flt64>(Millisecond)
        assertTrue(result is Ok)
        assertEquals(500.0, result.value.value.toDouble(), 1e-10)
    }

    @Test
    fun `duration toQuantity converts hours correctly`() {
        val duration = 90.minutes
        val result = duration.toQuantity<Flt64>(Hour)
        assertTrue(result is Ok)
        assertEquals(1.5, result.value.value.toDouble(), 1e-10)
    }

    @Test
    fun `duration toQuantitySecondsFlt64 works correctly`() {
        val duration = 5000.milliseconds
        val result = duration.toQuantitySecondsFlt64()
        assertTrue(result is Ok)
        assertEquals(5.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Second, result.value.unit)
    }

    @Test
    fun `duration toQuantityMillisecondsFlt64 works correctly`() {
        val duration = 5.seconds
        val result = duration.toQuantityMillisecondsFlt64()
        assertTrue(result is Ok)
        assertEquals(5000.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Millisecond, result.value.unit)
    }

    @Test
    fun `duration toQuantityMinutesFlt64 works correctly`() {
        val duration = 120.seconds
        val result = duration.toQuantityMinutesFlt64()
        assertTrue(result is Ok)
        assertEquals(2.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Minute, result.value.unit)
    }

    @Test
    fun `duration toQuantityHoursFlt64 works correctly`() {
        val duration = 90.minutes
        val result = duration.toQuantityHoursFlt64()
        assertTrue(result is Ok)
        assertEquals(1.5, result.value.value.toDouble(), 1e-10)
        assertEquals(Hour, result.value.unit)
    }

    @Test
    fun `duration toQuantityDaysFlt64 works correctly`() {
        val duration = 48.hours
        val result = duration.toQuantityDaysFlt64()
        assertTrue(result is Ok)
        assertEquals(2.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Day, result.value.unit)
    }

    // ========================================================================
    // toQuantityBestFit 测试
    // ========================================================================

    @Test
    fun `toQuantityBestFit selects nanoseconds for very small values`() {
        val duration = 500.nanoseconds
        val result = duration.toQuantityBestFit<Flt64>()
        assertTrue(result is Ok)
        assertEquals(Nanosecond, result.value.unit)
        assertEquals(500.0, result.value.value.toDouble(), 1e-10)
    }

    @Test
    fun `toQuantityBestFit selects milliseconds for subSecond values`() {
        val duration = 500.milliseconds
        val result = duration.toQuantityBestFit<Flt64>()
        assertTrue(result is Ok)
        assertEquals(Millisecond, result.value.unit)
        assertEquals(500.0, result.value.value.toDouble(), 1e-10)
    }

    @Test
    fun `toQuantityBestFit selects seconds for small values`() {
        val duration = 30.seconds
        val result = duration.toQuantityBestFit<Flt64>()
        assertTrue(result is Ok)
        assertEquals(Second, result.value.unit)
        assertEquals(30.0, result.value.value.toDouble(), 1e-10)
    }

    @Test
    fun `toQuantityBestFit selects minutes for medium values`() {
        val duration = 5.minutes
        val result = duration.toQuantityBestFit<Flt64>()
        assertTrue(result is Ok)
        assertEquals(Minute, result.value.unit)
        assertEquals(5.0, result.value.value.toDouble(), 1e-10)
    }

    @Test
    fun `toQuantityBestFit selects hours for larger values`() {
        val duration = 2.hours
        val result = duration.toQuantityBestFit<Flt64>()
        assertTrue(result is Ok)
        assertEquals(Hour, result.value.unit)
        assertEquals(2.0, result.value.value.toDouble(), 1e-10)
    }

    @Test
    fun `toQuantityBestFit selects days for large values`() {
        val duration = 2.days
        val result = duration.toQuantityBestFit<Flt64>()
        assertTrue(result is Ok)
        assertEquals(Day, result.value.unit)
        assertEquals(2.0, result.value.value.toDouble(), 1e-10)
    }

    @Test
    fun `toQuantityBestFit handles zero duration`() {
        val duration = Duration.ZERO
        val result = duration.toQuantityBestFit<Flt64>()
        assertTrue(result is Ok)
        assertEquals(0.0, result.value.value.toDouble(), 1e-10)
    }

    // ========================================================================
    // 扩展属性测试
    // ========================================================================

    @Test
    fun `inNanosecondsQuantityFlt64 works correctly`() {
        val duration = 1000.nanoseconds
        val result = duration.inNanosecondsQuantityFlt64
        assertTrue(result is Ok)
        assertEquals(1000.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Nanosecond, result.value.unit)
    }

    @Test
    fun `inMicrosecondsQuantityFlt64 works correctly`() {
        val duration = 500.microseconds
        val result = duration.inMicrosecondsQuantityFlt64
        assertTrue(result is Ok)
        assertEquals(500.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Microsecond, result.value.unit)
    }

    @Test
    fun `inMillisecondsQuantityFlt64 works correctly`() {
        val duration = 250.milliseconds
        val result = duration.inMillisecondsQuantityFlt64
        assertTrue(result is Ok)
        assertEquals(250.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Millisecond, result.value.unit)
    }

    @Test
    fun `inSecondsQuantityFlt64 works correctly`() {
        val duration = 45.seconds
        val result = duration.inSecondsQuantityFlt64
        assertTrue(result is Ok)
        assertEquals(45.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Second, result.value.unit)
    }

    @Test
    fun `inMinutesQuantityFlt64 works correctly`() {
        val duration = 15.minutes
        val result = duration.inMinutesQuantityFlt64
        assertTrue(result is Ok)
        assertEquals(15.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Minute, result.value.unit)
    }

    @Test
    fun `inHoursQuantityFlt64 works correctly`() {
        val duration = 8.hours
        val result = duration.inHoursQuantityFlt64
        assertTrue(result is Ok)
        assertEquals(8.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Hour, result.value.unit)
    }

    @Test
    fun `inDaysQuantityFlt64 works correctly`() {
        val duration = 7.days
        val result = duration.inDaysQuantityFlt64
        assertTrue(result is Ok)
        assertEquals(7.0, result.value.value.toDouble(), 1e-10)
        assertEquals(Day, result.value.unit)
    }

    // ========================================================================
    // 往返转换测试
    // ========================================================================

    @Test
    fun `roundTrip conversion preserves value`() {
        val original = Flt64(42.0) * Second
        val durationResult = original.toDuration()
        assertTrue(durationResult is Ok)
        val convertedResult = durationResult.value.toQuantity<Flt64>(Second)
        assertTrue(convertedResult is Ok)
        assertEquals(original.value.toDouble(), convertedResult.value.value.toDouble(), 1e-10)
        assertEquals(original.unit, convertedResult.value.unit)
    }

    @Test
    fun `roundTrip conversion with different units`() {
        val original = Flt64(2.5) * Hour
        val durationResult = original.toDuration()
        assertTrue(durationResult is Ok)
        val convertedResult = durationResult.value.toQuantity<Flt64>(Minute)
        assertTrue(convertedResult is Ok)
        assertEquals(150.0, convertedResult.value.value.toDouble(), 1e-10)
        assertEquals(Minute, convertedResult.value.unit)
    }

    // ========================================================================
    // 边界情况测试
    // ========================================================================

    @Test
    fun `handles negative durations`() {
        val duration = (-5).seconds
        val result = duration.toQuantity<Flt64>(Second)
        assertTrue(result is Ok)
        assertEquals(-5.0, result.value.value.toDouble(), 1e-10)
    }

    @Test
    fun `handles negative quantities`() {
        val quantity = Flt64(-3.0) * Minute
        val result = quantity.toDuration()
        assertTrue(result is Ok)
        assertEquals(-180.0, result.value.toDouble(DurationUnit.SECONDS), 1e-10)
    }

    @Test
    fun `handles fractional values`() {
        val quantity = Flt64(0.123) * Second
        val result = quantity.toDuration()
        assertTrue(result is Ok)
        assertEquals(123.0, result.value.toDouble(DurationUnit.MILLISECONDS), 1e-10)
    }

    @Test
    fun `handles large values`() {
        val quantity = Flt64(365.25) * Day
        val result = quantity.toDuration()
        assertTrue(result is Ok)
        assertEquals(1.0, result.value.toDouble(DurationUnit.DAYS) / 365.25, 1e-6)
    }

    @Test
    fun `negative to unsigned fails`() {
        val duration = (-5).seconds
        val result = duration.toQuantityUInt64(Second)
        assertTrue(result.failed)
    }
}