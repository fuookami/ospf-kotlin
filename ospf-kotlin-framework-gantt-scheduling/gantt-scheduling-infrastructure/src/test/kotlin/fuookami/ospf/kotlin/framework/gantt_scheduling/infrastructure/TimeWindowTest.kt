
@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Unit tests for TimeWindow class.
 * TimeWindow 类的单元测试。
 */
class TimeWindowTest {
    @Test
    fun quantityOfShouldReturnGenericTimeWindowValueQuantity() {
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.HOURS,
            interval = 1.toDuration(DurationUnit.HOURS),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        val durationQuantity = timeWindow.quantityOf(2.toDuration(DurationUnit.HOURS))
        val instantQuantity = timeWindow.quantityOf(Instant.parse("2020-08-30T11:00:00Z"))

        assert(durationQuantity.value eq FltX("2.0"))
        assert(instantQuantity.value eq FltX("3.0"))
    }

    @Test
    fun quantityFactoryShouldCreateGenericTimeWindow() {
        val timeWindow = TimeWindow.minutes(
            timeWindow = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            ),
            dateOffset = Quantity(FltX("0.0"), NoneUnit),
            continues = true,
            interval = Quantity(FltX("15.0"), NoneUnit),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        assert(timeWindow.interval == 15.toDuration(DurationUnit.MINUTES))
        assert(timeWindow.valueOf(30.toDuration(DurationUnit.MINUTES)) eq FltX("30.0"))
        assert(timeWindow.valueOf(Instant.parse("2020-08-30T08:45:00Z")) eq FltX("45.0"))
    }

    /**
     * Test basic time slot generation without excluded times.
     * 测试无排除时间的基本时段生成。
     */
    @Test
    fun testBasicRoundTimeSlotsOf() {
        // Time window: 2020-08-30T08:30:00Z to 2020-08-30T12:30:00Z (4 hours)
        // 时间窗：2020-08-30T08:30:00Z 到 2020-08-30T12:30:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:30:00Z"),
                end = Instant.parse("2020-08-30T12:30:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Generate time slots with 1-hour interval
        // 使用1小时粒度生成时段
        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS)
        )

        // Verify: should have multiple time slots
        // 验证：应该有多个时段
        assert(timeSlots.isNotEmpty()) { "Time slots should not be empty" }
        
        // Verify: first time slot starts at window start
        // 验证：第一个时段从时间窗开始
        assert(timeSlots.first().start == Instant.parse("2020-08-30T08:30:00Z")) {
            "First time slot should start at window start"
        }
        
        // Verify: last time slot ends at window end
        // 验证：最后一个时段在时间窗结束
        assert(timeSlots.last().end == Instant.parse("2020-08-30T12:30:00Z")) {
            "Last time slot should end at window end"
        }
    }

    /**
     * Test time slot generation with start aligned to upper unit.
     * 测试开始时间与上级单位对齐的时段生成。
     */
    @Test
    fun testRoundTimeSlotsOfWithAlignedStart() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours, aligned to hour)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时，对齐到小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS)
        )

        // Verify: each time slot should be exactly 1 hour
        // 验证：每个时段应该正好1小时
        assert(timeSlots.all { it.duration == 1.toDuration(DurationUnit.HOURS) }) {
            "All time slots should be exactly 1 hour when aligned"
        }
        
        // Verify: should have 4 time slots
        // 验证：应该有4个时段
        assert(timeSlots.size == 4) {
            "Should have 4 time slots for 4-hour window with 1-hour interval"
        }
    }

    /**
     * Test time slot generation with excluded times.
     * 测试带排除时间的时段生成。
     */
    @Test
    fun testRoundTimeSlotsOfWithExcludedTimes() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Exclude 09:00-10:00
        // 排除 09:00-10:00
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T09:00:00Z"),
                end = Instant.parse("2020-08-30T10:00:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: no time slot should overlap with excluded time
        // 验证：没有时段应该与排除时间重叠
        assert(timeSlots.none { slot -> 
            excludedTimes.any { excluded -> slot.withIntersection(excluded) }
        }) {
            "No time slot should overlap with excluded times"
        }
        
        // Verify: time slots should cover the remaining time
        // 验证：时段应该覆盖剩余时间
        val totalDuration = timeSlots.fold(0L) { total, slot -> total + slot.duration.inWholeSeconds }
        // 4 hours - 1 hour excluded = 3 hours
        // 4小时 - 1小时排除 = 3小时
        assert(totalDuration == 3 * 60 * 60L) {
            "Total duration should be 3 hours (4 - 1 excluded)"
        }
    }

    /**
     * Test time slot generation with multiple excluded times.
     * 测试带多个排除时间的时段生成。
     */
    @Test
    fun testRoundTimeSlotsOfWithMultipleExcludedTimes() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T16:00:00Z (8 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T16:00:00Z (8小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T16:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Exclude 09:00-10:00 and 13:00-14:00
        // 排除 09:00-10:00 和 13:00-14:00
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T09:00:00Z"),
                end = Instant.parse("2020-08-30T10:00:00Z")
            ),
            TimeRange(
                start = Instant.parse("2020-08-30T13:00:00Z"),
                end = Instant.parse("2020-08-30T14:00:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: no time slot should overlap with any excluded time
        // 验证：没有时段应该与任何排除时间重叠
        assert(timeSlots.none { slot -> 
            excludedTimes.any { excluded -> slot.withIntersection(excluded) }
        }) {
            "No time slot should overlap with excluded times"
        }
        
        // Verify: total duration should be 8 - 2 = 6 hours
        // 验证：总时长应该是 8 - 2 = 6小时
        val totalDuration = timeSlots.fold(0L) { total, slot -> total + slot.duration.inWholeSeconds }
        assert(totalDuration == 6 * 60 * 60L) {
            "Total duration should be 6 hours (8 - 2 excluded)"
        }
    }

    /**
     * Test time slot generation with intervals map.
     * 测试带时间粒度映射的时段生成。
     */
    @Test
    fun testRoundTimeSlotsOfWithIntervalsMap() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Use 30-minute interval for 10:00-11:00, 1-hour for rest
        // 10:00-11:00使用30分钟粒度，其余使用1小时
        val intervals = mapOf<TimeRange?, Duration>(
            null to 1.toDuration(DurationUnit.HOURS),
            TimeRange(
                start = Instant.parse("2020-08-30T10:00:00Z"),
                end = Instant.parse("2020-08-30T11:00:00Z")
            ) to 30.toDuration(DurationUnit.MINUTES)
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(intervals = intervals)

        // Verify: time slots are generated
        // 验证：时段已生成
        assert(timeSlots.isNotEmpty()) {
            "Time slots should not be empty"
        }
        
        // Verify: time slots in the special range should be 30 minutes
        // 验证：特殊范围内的时段应该是30分钟
        val specialSlots = timeSlots.filter { 
            it.start >= Instant.parse("2020-08-30T10:00:00Z") && 
            it.end <= Instant.parse("2020-08-30T11:00:00Z") 
        }
        assert(specialSlots.all { it.duration == 30.toDuration(DurationUnit.MINUTES) }) {
            "Time slots in special range should be 30 minutes"
        }
    }

    /**
     * Test time slot generation spanning multiple days.
     * 测试跨多天的时段生成。
     */
    @Test
    fun testRoundTimeSlotsOfSpanningMultipleDays() {
        // Time window: 2020-08-30T08:00:00Z to 2020-09-01T08:00:00Z (2 days)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-09-01T08:00:00Z (2天)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-09-01T08:00:00Z")
            ),
            durationUnit = DurationUnit.HOURS,
            interval = 1.toDuration(DurationUnit.HOURS),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS)
        )

        // Verify: time slots are generated
        // 验证：时段已生成
        assert(timeSlots.isNotEmpty()) {
            "Time slots should not be empty"
        }
        
        // Verify: total duration should be 48 hours
        // 验证：总时长应该是48小时
        val totalDuration = timeSlots.fold(0L) { total, slot -> total + slot.duration.inWholeSeconds }
        assert(totalDuration == 48 * 60 * 60L) {
            "Total duration should be 48 hours"
        }
    }

    /**
     * Test time slot generation with excluded time at the beginning.
     * 测试开始时间被排除的时段生成。
     */
    @Test
    fun testRoundTimeSlotsOfWithExcludedTimeAtBeginning() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Exclude 08:00-09:00 (at the beginning)
        // 排除 08:00-09:00 (在开始)
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: first time slot should start at 09:00
        // 验证：第一个时段应该从09:00开始
        assert(timeSlots.first().start == Instant.parse("2020-08-30T09:00:00Z")) {
            "First time slot should start at 09:00"
        }
    }

    /**
     * Test time slot generation with excluded time at the end.
     * 测试结束时间被排除的时段生成。
     */
    @Test
    fun testRoundTimeSlotsOfWithExcludedTimeAtEnd() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Exclude 11:00-12:00 (at the end)
        // 排除 11:00-12:00 (在末尾)
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T11:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: last time slot should end at 11:00
        // 验证：最后一个时段应该在11:00结束
        assert(timeSlots.last().end == Instant.parse("2020-08-30T11:00:00Z")) {
            "Last time slot should end at 11:00"
        }
    }

    /**
     * Test that time slots are not merged automatically (business layer responsibility).
     * 测试时段不会自动合并（由业务层负责）。
     */
    @Test
    fun testTimeSlotsNotMerged() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Exclude a small portion in the middle
        // 在中间排除一小部分
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T09:30:00Z"),
                end = Instant.parse("2020-08-30T10:30:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: time slots are NOT merged (there should be small pieces)
        // 验证：时段没有合并（应该有小片段）
        // The result should contain split pieces, not merged ones
        // 结果应该包含分割的片段，而不是合并的
        assert(timeSlots.size > 2) {
            "Time slots should not be merged automatically, expected multiple pieces"
        }
    }

    /**
     * Test time slot generation with partially overlapping excluded time.
     * 测试带部分重叠排除时间的时段生成。
     */
    @Test
    fun testRoundTimeSlotsOfWithPartialOverlapExcludedTime() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Exclude 07:00-09:00 (partially overlaps before window)
        // 排除 07:00-09:00 (部分在时间窗之前重叠)
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T07:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: first time slot should start at 09:00
        // 验证：第一个时段应该从09:00开始
        assert(timeSlots.first().start == Instant.parse("2020-08-30T09:00:00Z")) {
            "First time slot should start at 09:00"
        }
    }

    /**
     * Test time slot generation with excluded time extending beyond window.
     * 测试带超出时间窗的排除时间的时段生成。
     */
    @Test
    fun testRoundTimeSlotsOfWithExcludedTimeExtendingBeyond() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Exclude 11:00-13:00 (extends beyond window end)
        // 排除 11:00-13:00 (超出时间窗结束)
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T11:00:00Z"),
                end = Instant.parse("2020-08-30T13:00:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: last time slot should end at 11:00
        // 验证：最后一个时段应该在11:00结束
        assert(timeSlots.last().end == Instant.parse("2020-08-30T11:00:00Z")) {
            "Last time slot should end at 11:00"
        }
    }

    /**
     * Test time slot generation with excluded time that results in zero duration pieces.
     * 测试排除时间导致零时长片段的情况。
     */
    @Test
    fun testRoundTimeSlotsOfWithExcludedTimeAtSlotBoundary() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Exclude exactly 09:00-10:00 (aligned with slot boundaries)
        // 排除正好 09:00-10:00 (与时段边界对齐)
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T09:00:00Z"),
                end = Instant.parse("2020-08-30T10:00:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: no time slot with zero duration
        // 验证：没有零时长的时段
        assert(timeSlots.all { it.duration > Duration.ZERO }) {
            "No time slot should have zero duration"
        }

        // Verify: no time slot overlaps with excluded time
        // 验证：没有时段与排除时间重叠
        assert(timeSlots.none { slot ->
            excludedTimes.any { excluded -> slot.withIntersection(excluded) }
        }) {
            "No time slot should overlap with excluded times"
        }
    }

    /**
     * Test time slot generation where excluded time covers entire slot.
     * 测试排除时间覆盖整个时段的情况。
     */
    @Test
    fun testRoundTimeSlotsOfWithExcludedTimeCoveringEntireSlot() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Exclude entire 09:00-10:00 slot plus more
        // 排除整个 09:00-10:00 时段及更多
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T08:30:00Z"),
                end = Instant.parse("2020-08-30T11:30:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: no time slot with zero duration
        // 验证：没有零时长的时段
        assert(timeSlots.all { it.duration > Duration.ZERO }) {
            "No time slot should have zero duration"
        }

        // Verify: remaining slots are only at the edges
        // 验证：剩余时段只在边缘
        assert(timeSlots.size == 2) {
            "Should have 2 remaining time slots at edges"
        }

        // Verify: first slot is 08:00-08:30
        // 验证：第一个时段是 08:00-08:30
        assert(timeSlots.first().start == Instant.parse("2020-08-30T08:00:00Z")) {
            "First slot should start at 08:00"
        }
        assert(timeSlots.first().end == Instant.parse("2020-08-30T08:30:00Z")) {
            "First slot should end at 08:30"
        }

        // Verify: last slot is 11:30-12:00
        // 验证：最后一个时段是 11:30-12:00
        assert(timeSlots.last().start == Instant.parse("2020-08-30T11:30:00Z")) {
            "Last slot should start at 11:30"
        }
        assert(timeSlots.last().end == Instant.parse("2020-08-30T12:00:00Z")) {
            "Last slot should end at 12:00"
        }
    }

    /**
     * Test time slot generation with very small interval.
     * 测试非常小的时间粒度。
     */
    @Test
    fun testRoundTimeSlotsOfWithSmallInterval() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T08:05:00Z (5 minutes)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T08:05:00Z (5分钟)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T08:05:00Z")
            ),
            durationUnit = DurationUnit.SECONDS,
            interval = 10.toDuration(DurationUnit.SECONDS),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.MINUTES)
        )

        // Verify: time slots are generated
        // 验证：时段已生成
        assert(timeSlots.isNotEmpty()) {
            "Time slots should not be empty"
        }

        // Verify: no time slot with zero duration
        // 验证：没有零时长的时段
        assert(timeSlots.all { it.duration > Duration.ZERO }) {
            "No time slot should have zero duration"
        }

        // Verify: total duration equals window duration
        // 验证：总时长等于时间窗时长
        val totalDuration = timeSlots.fold(0L) { total, slot -> total + slot.duration.inWholeSeconds }
        assert(totalDuration == 5 * 60L) {
            "Total duration should be 5 minutes"
        }
    }

    /**
     * Test time slot generation with empty excluded times list.
     * 测试空的排除时间列表。
     */
    @Test
    fun testRoundTimeSlotsOfWithEmptyExcludedTimes() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = emptyList()
        )

        // Verify: time slots are generated normally
        // 验证：正常生成时段
        assert(timeSlots.isNotEmpty()) {
            "Time slots should not be empty"
        }

        // Verify: each slot is exactly 1 hour
        // 验证：每个时段正好1小时
        assert(timeSlots.all { it.duration == 1.toDuration(DurationUnit.HOURS) }) {
            "All time slots should be exactly 1 hour"
        }
    }

    /**
     * Test time slot generation where all time is excluded.
     * 测试所有时间都被排除的情况。
     */
    @Test
    fun testRoundTimeSlotsOfWithAllTimeExcluded() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Exclude entire window
        // 排除整个时间窗
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T07:00:00Z"),
                end = Instant.parse("2020-08-30T13:00:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: no time slots when all time is excluded
        // 验证：当所有时间被排除时，没有时段
        assert(timeSlots.isEmpty()) {
            "Time slots should be empty when all time is excluded"
        }
    }

    /**
     * Test time slot generation with adjacent excluded times.
     * 测试相邻排除时间的情况。
     */
    @Test
    fun testRoundTimeSlotsOfWithAdjacentExcludedTimes() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Exclude two adjacent time ranges: 09:00-10:00 and 10:00-11:00
        // 排除两个相邻的时间范围：09:00-10:00 和 10:00-11:00
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T09:00:00Z"),
                end = Instant.parse("2020-08-30T10:00:00Z")
            ),
            TimeRange(
                start = Instant.parse("2020-08-30T10:00:00Z"),
                end = Instant.parse("2020-08-30T11:00:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: no time slot overlaps with excluded times
        // 验证：没有时段与排除时间重叠
        assert(timeSlots.none { slot ->
            excludedTimes.any { excluded -> slot.withIntersection(excluded) }
        }) {
            "No time slot should overlap with excluded times"
        }

        // Verify: remaining slots are 08:00-09:00 and 11:00-12:00
        // 验证：剩余时段是 08:00-09:00 和 11:00-12:00
        assert(timeSlots.size == 2) {
            "Should have 2 remaining time slots"
        }
    }

    /**
     * Test time slot generation with misaligned window start and end.
     * 测试时间窗开始和结束不对齐的情况。
     */
    @Test
    fun testRoundTimeSlotsOfWithMisalignedWindow() {
        // Time window: 2020-08-30T08:17:00Z to 2020-08-30T11:43:00Z (not aligned to hour)
        // 时间窗：2020-08-30T08:17:00Z 到 2020-08-30T11:43:00Z (不对齐到小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:17:00Z"),
                end = Instant.parse("2020-08-30T11:43:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS)
        )

        // Verify: time slots are generated
        // 验证：时段已生成
        assert(timeSlots.isNotEmpty()) {
            "Time slots should not be empty"
        }

        // Verify: first time slot starts at window start
        // 验证：第一个时段从时间窗开始
        assert(timeSlots.first().start == Instant.parse("2020-08-30T08:17:00Z")) {
            "First time slot should start at window start"
        }

        // Verify: last time slot ends at window end
        // 验证：最后一个时段在时间窗结束
        assert(timeSlots.last().end == Instant.parse("2020-08-30T11:43:00Z")) {
            "Last time slot should end at window end"
        }

        // Verify: no time slot with zero duration
        // 验证：没有零时长的时段
        assert(timeSlots.all { it.duration > Duration.ZERO }) {
            "No time slot should have zero duration"
        }
    }

    /**
     * Test time slot generation with very short window.
     * 测试非常短的时间窗。
     */
    @Test
    fun testRoundTimeSlotsOfWithVeryShortWindow() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T08:00:01Z (1 second)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T08:00:01Z (1秒)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T08:00:01Z")
            ),
            durationUnit = DurationUnit.SECONDS,
            interval = 1.toDuration(DurationUnit.SECONDS),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.MINUTES)
        )

        // Verify: at least one time slot is generated
        // 验证：至少生成一个时段
        assert(timeSlots.isNotEmpty()) {
            "Time slots should not be empty even for very short window"
        }

        // Verify: no time slot with zero duration
        // 验证：没有零时长的时段
        assert(timeSlots.all { it.duration > Duration.ZERO }) {
            "No time slot should have zero duration"
        }
    }

    /**
     * Test time slot generation with overlapping excluded times.
     * 测试重叠的排除时间。
     */
    @Test
    fun testRoundTimeSlotsOfWithOverlappingExcludedTimes() {
        // Time window: 2020-08-30T08:00:00Z to 2020-08-30T12:00:00Z (4 hours)
        // 时间窗：2020-08-30T08:00:00Z 到 2020-08-30T12:00:00Z (4小时)
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 30.toDuration(DurationUnit.MINUTES),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        // Overlapping excluded times: 09:00-10:30 and 10:00-11:00
        // 重叠的排除时间：09:00-10:30 和 10:00-11:00
        val excludedTimes = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T09:00:00Z"),
                end = Instant.parse("2020-08-30T10:30:00Z")
            ),
            TimeRange(
                start = Instant.parse("2020-08-30T10:00:00Z"),
                end = Instant.parse("2020-08-30T11:00:00Z")
            )
        )

        val timeSlots = timeWindow.roundTimeSlotsOf(
            interval = 1.toDuration(DurationUnit.HOURS),
            excludedTimes = excludedTimes
        )

        // Verify: no time slot overlaps with excluded times
        // 验证：没有时段与排除时间重叠
        assert(timeSlots.none { slot ->
            excludedTimes.any { excluded -> slot.withIntersection(excluded) }
        }) {
            "No time slot should overlap with excluded times"
        }

        // Verify: total duration should be 4 hours minus excluded duration (2 hours: 09:00-11:00)
        // 验证：总时长应该是4小时减去排除时长（2小时：09:00-11:00）
        val totalDuration = timeSlots.fold(0L) { total, slot -> total + slot.duration.inWholeSeconds }
        assert(totalDuration == 2 * 60 * 60L) {
            "Total duration should be 2 hours (4 - 2 excluded)"
        }
    }

    /**
     * Test unsupported duration unit gives explicit exception.
     * 测试不支持的时间单位会抛出明确异常。
     */
    @Test
    fun testUpperIntervalUnsupportedDurationUnitThrowsReadableError() {
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T10:00:00Z")
            ),
            durationUnit = DurationUnit.MILLISECONDS,
            interval = 1.toDuration(DurationUnit.MILLISECONDS),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        val result = timeWindow.upperInterval()
        assert(result is Failed)
        assert((result as Failed).error.message.contains("TimeWindow.upperInterval"))
    }
}
