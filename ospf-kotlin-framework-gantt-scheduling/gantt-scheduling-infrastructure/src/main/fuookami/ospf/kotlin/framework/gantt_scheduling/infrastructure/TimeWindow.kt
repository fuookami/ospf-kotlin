@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 时间窗口模型，提供时间离散化和舍入功能 / Time window model providing time discretization and rounding capabilities
*/
package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlinx.datetime.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.toDuration
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.max
import fuookami.ospf.kotlin.utils.min
import fuookami.ospf.kotlin.utils.truncatedTo

/** 时间窗口数值物理量 / Time-window value quantity */
typealias TimeWindowValueQuantity<V> = Quantity<V>

private val nanosecondsPerNanosecond = BigDecimal.ONE
private val nanosecondsPerMicrosecond = BigDecimal.valueOf(1_000L)
private val nanosecondsPerMillisecond = BigDecimal.valueOf(1_000_000L)
private val nanosecondsPerSecond = BigDecimal.valueOf(1_000_000_000L)
private val nanosecondsPerMinute = BigDecimal.valueOf(60_000_000_000L)
private val nanosecondsPerHour = BigDecimal.valueOf(3_600_000_000_000L)
private val nanosecondsPerDay = BigDecimal.valueOf(86_400_000_000_000L)
private val maximumDurationNanoseconds = BigDecimal.valueOf(
    Long.MAX_VALUE / 2 / 1_000_000L * 1_000_000L - 1L
)
private val maximumDurationMilliseconds = BigDecimal.valueOf(Long.MAX_VALUE / 2)

private fun nanosecondsPer(unit: DurationUnit): BigDecimal {
    return when (unit) {
        DurationUnit.NANOSECONDS -> nanosecondsPerNanosecond
        DurationUnit.MICROSECONDS -> nanosecondsPerMicrosecond
        DurationUnit.MILLISECONDS -> nanosecondsPerMillisecond
        DurationUnit.SECONDS -> nanosecondsPerSecond
        DurationUnit.MINUTES -> nanosecondsPerMinute
        DurationUnit.HOURS -> nanosecondsPerHour
        DurationUnit.DAYS -> nanosecondsPerDay
    }
}

/**
 * 将 Duration 精确转换为时间窗口数值的十进制表示（精度边界为纳秒）。 / Convert a Duration to the decimal
 * time-window value without a binary floating-point intermediate (the precision boundary is one nanosecond).
 */
private fun Duration.toTimeWindowDecimal(unit: DurationUnit): BigDecimal {
    val nanoseconds = toComponents { seconds, remainderNanoseconds ->
        BigDecimal.valueOf(seconds)
            .multiply(nanosecondsPerSecond)
            .add(BigDecimal.valueOf(remainderNanoseconds.toLong()))
    }
    return nanoseconds.divide(nanosecondsPer(unit), MathContext.DECIMAL128)
}

private fun BigDecimal.roundToDurationInteger(): BigDecimal {
    return setScale(0, RoundingMode.HALF_UP)
}

/**
 * 将时间窗口十进制值转换为 Duration，并在 Kotlin Duration 的纳秒边界处舍入。 /
 * Convert a time-window decimal to Duration, rounding at Kotlin Duration's nanosecond boundary.
 */
private fun BigDecimal.toTimeWindowDuration(unit: DurationUnit): Duration {
    val nanoseconds = multiply(nanosecondsPer(unit)).roundToDurationInteger()
    if (nanoseconds >= maximumDurationNanoseconds.negate() && nanoseconds <= maximumDurationNanoseconds) {
        return nanoseconds.toLong().toDuration(DurationUnit.NANOSECONDS)
    }

    val milliseconds = nanoseconds
        .divide(nanosecondsPerMillisecond, MathContext.DECIMAL128)
        .roundToDurationInteger()
    return when {
        milliseconds >= maximumDurationMilliseconds -> Duration.INFINITE
        milliseconds <= maximumDurationMilliseconds.negate() -> -Duration.INFINITE
        else -> milliseconds.toLong().toDuration(DurationUnit.MILLISECONDS)
    }
}

/**
 * FltX 与 Kotlin 时间类型之间的精确转换器。 / Exact converters between FltX and Kotlin time types.
 */
object TimeWindowValueConverters {
    /**
     * 将 Duration 转为 FltX，保留 Duration 可表达的纳秒精度。 / Convert Duration to FltX while preserving
     * the nanosecond precision representable by Duration.
     *
     * @param duration 持续时间 / Duration
     * @param durationUnit 时间窗口单位 / Time-window unit
     * @return FltX 时间值 / FltX time value
     */
    fun durationToFltX(duration: Duration, durationUnit: DurationUnit): FltX {
        return FltX(duration.toTimeWindowDecimal(durationUnit).toPlainString())
    }

    /**
     * 将 FltX 转为 Duration，舍入到最近纳秒。 / Convert FltX to Duration, rounding to the nearest nanosecond.
     *
     * @param value FltX 时间值 / FltX time value
     * @param durationUnit 时间窗口单位 / Time-window unit
     * @return Kotlin Duration / Kotlin Duration
     */
    fun fltXToDuration(value: FltX, durationUnit: DurationUnit): Duration {
        return value.toDecimal().toTimeWindowDuration(durationUnit)
    }
}

/**
 * 泛型时间窗口，提供时间离散化和舍入功能 / Generic time window providing time discretization and rounding capabilities
 *
 * @property window 基础时间范围 / The underlying time range
 * @property continues 是否连续 / Whether the time window is continuous
 * @property durationUnit 持续时间单位 / The duration unit
 * @property dateOffset 日期偏移量 / The date offset duration
 * @property interval 时间间隔 / The time interval
 * @property fromDouble 从 Double 转换为 V / Convert from Double to V
 * @property toDouble 从 V 转换为 Double / Convert from V to Double
 * @property fromDuration 从 Duration 转换为 V 的可选精确转换器 / Optional exact converter from Duration to V
 * @property toDuration 从 V 转换为 Duration 的可选精确转换器 / Optional exact converter from V to Duration
 */
data class TimeWindow<V : RealNumber<V>>(
    val window: TimeRange,
    val continues: Boolean = true,
    val durationUnit: DurationUnit = DurationUnit.SECONDS,
    val dateOffset: Duration = Duration.ZERO,
    val interval: Duration = 1.toDuration(durationUnit),
    val fromDouble: (Double) -> V,
    val toDouble: (V) -> Double,
    val fromDuration: ((Duration, DurationUnit) -> V)? = null,
    val toDuration: ((V, DurationUnit) -> Duration)? = null
) {
    companion object {
        /**
         * 创建秒级泛型时间窗口 / Create a seconds-level generic time window
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量 / Date offset
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔 / Time interval
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
        */
        fun <V : RealNumber<V>> seconds(
            timeWindow: TimeRange,
            dateOffset: V,
            continues: Boolean = true,
            interval: V,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return TimeWindow(
                window = timeWindow,
                dateOffset = toDouble(dateOffset).toDuration(DurationUnit.SECONDS),
                continues = continues,
                durationUnit = DurationUnit.SECONDS,
                interval = toDouble(interval).toDuration(DurationUnit.SECONDS),
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

        /**
         * 通过物理量创建秒级泛型时间窗口 / Create a seconds-level generic time window from quantities
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量物理量 / Date offset quantity
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔物理量 / Time interval quantity
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
        */
        fun <V : RealNumber<V>> seconds(
            timeWindow: TimeRange,
            dateOffset: TimeWindowValueQuantity<V>,
            continues: Boolean = true,
            interval: TimeWindowValueQuantity<V>,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return seconds(
                timeWindow = timeWindow,
                dateOffset = dateOffset.value,
                continues = continues,
                interval = interval.value,
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

        /**
         * 创建分钟级泛型时间窗口 / Create a minutes-level generic time window
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量 / Date offset
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔 / Time interval
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
        */
        fun <V : RealNumber<V>> minutes(
            timeWindow: TimeRange,
            dateOffset: V,
            continues: Boolean = true,
            interval: V,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return TimeWindow(
                window = timeWindow,
                dateOffset = toDouble(dateOffset).toDuration(DurationUnit.MINUTES),
                continues = continues,
                durationUnit = DurationUnit.MINUTES,
                interval = toDouble(interval).toDuration(DurationUnit.MINUTES),
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

        /**
         * 通过物理量创建分钟级泛型时间窗口 / Create a minutes-level generic time window from quantities
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量物理量 / Date offset quantity
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔物理量 / Time interval quantity
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
        */
        fun <V : RealNumber<V>> minutes(
            timeWindow: TimeRange,
            dateOffset: TimeWindowValueQuantity<V>,
            continues: Boolean = true,
            interval: TimeWindowValueQuantity<V>,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return minutes(
                timeWindow = timeWindow,
                dateOffset = dateOffset.value,
                continues = continues,
                interval = interval.value,
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

        /**
         * 创建小时级泛型时间窗口 / Create an hours-level generic time window
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量 / Date offset
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔 / Time interval
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
        */
        fun <V : RealNumber<V>> hours(
            timeWindow: TimeRange,
            dateOffset: V,
            continues: Boolean = true,
            interval: V,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return TimeWindow(
                window = timeWindow,
                dateOffset = toDouble(dateOffset).toDuration(DurationUnit.HOURS),
                continues = continues,
                durationUnit = DurationUnit.HOURS,
                interval = toDouble(interval).toDuration(DurationUnit.HOURS),
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

        /**
         * 通过物理量创建小时级泛型时间窗口 / Create an hours-level generic time window from quantities
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量物理量 / Date offset quantity
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔物理量 / Time interval quantity
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
        */
        fun <V : RealNumber<V>> hours(
            timeWindow: TimeRange,
            dateOffset: TimeWindowValueQuantity<V>,
            continues: Boolean = true,
            interval: TimeWindowValueQuantity<V>,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return hours(
                timeWindow = timeWindow,
                dateOffset = dateOffset.value,
                continues = continues,
                interval = interval.value,
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

    }

    /**
     * 转换为日历数值边界，用于仍固定 solver 数值类型的日历或 solver 内部 / / Convert to a calendar numeric boundary for calendar or solver internals that still use the solver numeric type
     *
     * @return 日历数值时间窗口边界 / The calendar numeric time-window boundary
    */
    fun toFlt64Boundary(): TimeWindow<Flt64> {
        return TimeWindow(
            window = window,
            continues = continues,
            durationUnit = durationUnit,
            dateOffset = dateOffset,
            interval = interval,
            fromDouble = { Flt64(it) },
            toDouble = { it.toDouble() }
        )
    }

    /** 将持续时间转换为 V 数值 / Convert duration to V numeric value */
    val Duration.value: V get() = fromDuration?.invoke(this, durationUnit)
        ?: fromDouble(this.toDouble(durationUnit))
    val Duration.round: Duration get() = round(this.toDouble(durationUnit)).toDuration(durationUnit)
    val Duration.floor: Duration get() = floor(this.toDouble(durationUnit)).toDuration(durationUnit)
    val Duration.ceil: Duration get() = ceil(this.toDouble(durationUnit)).toDuration(durationUnit)

    /**
     * 获取持续时间的 V 数值 / Get the V numeric value of a duration
     * @param duration 持续时间 / The duration
     * @return V 数值 / The V numeric value
    */
    fun valueOf(duration: Duration): V = duration.value

    /**
     * 获取持续时间的物理量数值 / Get the quantity value of a duration
     *
     * @param duration 持续时间 / Duration
     * @param unit 时间数值单位 / Time value unit
     * @return 持续时间物理量数值 / Duration quantity value
    */
    fun quantityOf(
        duration: Duration,
        unit: PhysicalUnit = NoneUnit
    ): TimeWindowValueQuantity<V> {
        return Quantity(valueOf(duration), unit)
    }

    /**
     * 舍入持续时间 / Round a duration
     * @param duration 持续时间 / The duration
     * @return 舍入后的持续时间 / The rounded duration
    */
    fun round(duration: Duration) = duration.round

    /**
     * 向下取整持续时间 / Floor a duration
     * @param duration 持续时间 / The duration
     * @return 向下取整后的持续时间 / The floored duration
    */
    fun floor(duration: Duration) = duration.floor

    /**
     * 向上取整持续时间 / Ceil a duration
     * @param duration 持续时间 / The duration
     * @return 向上取整后的持续时间 / The ceiled duration
    */
    fun ceil(duration: Duration) = duration.ceil

    /** 将时间点转换为相对于窗口起始的 V 数值 / Convert an instant to a V value relative to window start */
    val Instant.value: V get() = (this - window.start).value
    val Instant.round: Instant get() = window.start + (this - window.start).round
    val Instant.floor: Instant get() = window.start + (this - window.start).floor
    val Instant.ceil: Instant get() = window.start + (this - window.start).ceil

    /**
     * 获取时间点的 V 数值 / Get the V numeric value of an instant
     * @param instant 时间点 / The instant
     * @return V 数值 / The V numeric value
    */
    fun valueOf(instant: Instant): V = instant.value

    /**
     * 获取时间点相对窗口起点的物理量数值 / Get the quantity value of an instant relative to window start
     *
     * @param instant 时间点 / Instant
     * @param unit 时间数值单位 / Time value unit
     * @return 时间点物理量数值 / Instant quantity value
    */
    fun quantityOf(
        instant: Instant,
        unit: PhysicalUnit = NoneUnit
    ): TimeWindowValueQuantity<V> {
        return Quantity(valueOf(instant), unit)
    }

    /**
     * 舍入时间点 / Round an instant
     * @param instant 时间点 / The instant
     * @return 舍入后的时间点 / The rounded instant
    */
    fun round(instant: Instant) = instant.round

    /**
     * 向下取整时间点 / Floor an instant
     * @param instant 时间点 / The instant
     * @return 向下取整后的时间点 / The floored instant
    */
    fun floor(instant: Instant) = instant.floor

    /**
     * 向上取整时间点 / Ceil an instant
     * @param instant 时间点 / The instant
     * @return 向上取整后的时间点 / The ceiled instant
    */
    fun ceil(instant: Instant) = instant.ceil

    /** 将 V 数值转换为持续时间 / Convert V numeric value to duration */
    val V.duration: Duration get() = toDuration?.invoke(this, durationUnit)
        ?: toDouble(this).toDuration(durationUnit)
    val Int64.duration: Duration get() = timeWindowValue().toDuration(durationUnit)
    val UInt64.duration: Duration get() = timeWindowValue().toDuration(durationUnit)

    /**
     * 从 V 数值创建持续时间 / Create a duration from a V value
     * @param duration V 数值 / The V numeric value
     * @return 持续时间 / The duration
    */
    fun durationOf(duration: V) = duration.duration

    /**
     * 从整数值创建持续时间 / Create a duration from an integer value
     * @param duration 整数值 / The integer value
     * @return 持续时间 / The duration
    */
    fun durationOf(duration: Int64) = duration.duration

    /**
     * 从无符号整数值创建持续时间 / Create a duration from an unsigned integer value
     * @param duration 无符号整数值 / The unsigned integer value
     * @return 持续时间 / The duration
    */
    fun durationOf(duration: UInt64) = duration.duration

    /** 将 V 数值转换为时间点 / Convert V numeric value to an instant */
    val V.instant: Instant get() = window.start + this.duration
    val Int64.instant: Instant get() = window.start + timeWindowValue().toDuration(durationUnit)
    val UInt64.instant: Instant get() = window.start + timeWindowValue().toDuration(durationUnit)

    /**
     * 从 V 数值创建时间点 / Create an instant from a V value
     * @param instant V 数值 / The V numeric value
     * @return 时间点 / The instant
    */
    fun instantOf(instant: V) = instant.instant

    /**
     * 从整数值创建时间点 / Create an instant from an integer value
     * @param instant 整数值 / The integer value
     * @return 时间点 / The instant
    */
    fun instantOf(instant: Int64) = instant.instant

    /**
     * 从无符号整数值创建时间点 / Create an instant from an unsigned integer value
     * @param instant 无符号整数值 / The unsigned integer value
     * @return 时间点 / The instant
    */
    fun instantOf(instant: UInt64) = instant.instant

    /** 是否为空窗口 / Whether this is an empty window */
    val empty: Boolean by window::empty

    /** 开始时间 / Start time */
    val start: Instant by window::start

    /** 结束时间 / End time */
    val end: Instant by window::end

    /** 持续时间 / Duration */
    val duration: Duration by window::duration

    /** 上级时间间隔（nullable） / The upper-level time interval (nullable) */
    val upperIntervalOrNull: Duration? by lazy {
        when (durationUnit) {
            DurationUnit.SECONDS -> {
                1.toDuration(DurationUnit.MINUTES)
            }

            DurationUnit.MINUTES -> {
                1.toDuration(DurationUnit.HOURS)
            }

            DurationUnit.HOURS -> {
                1.toDuration(DurationUnit.DAYS)
            }

            else -> {
                null
            }
        }
    }

    /** 上级时间间隔 / The upper-level time interval
     * @return 上级时间间隔 / upper-level time interval
    */
    fun upperInterval(): Ret<Duration> {
        return when (durationUnit) {
            DurationUnit.SECONDS -> Ok(1.toDuration(DurationUnit.MINUTES))
            DurationUnit.MINUTES -> Ok(1.toDuration(DurationUnit.HOURS))
            DurationUnit.HOURS -> Ok(1.toDuration(DurationUnit.DAYS))
            else -> Failed(ErrorCode.IllegalArgument, "TimeWindow.upperInterval 暂不支持 durationUnit=$durationUnit，仅支持 SECONDS/MINUTES/HOURS。")
        }
    }

/**
 * Int64.
 * Int64。
 * @return Double 数值 / The Double value
*/
    private fun Int64.timeWindowValue() = toLong().toDouble()

/**
 * UInt64.
 * UInt64。
 * @return Double 数值 / The Double value
*/
    private fun UInt64.timeWindowValue() = toLong().toDouble()

    /** 上级时间窗口（nullable） / The upper-level time window (nullable) */
    val upperOrNull: TimeWindow<V>? by lazy {
        when (durationUnit) {
            DurationUnit.SECONDS -> DurationUnit.MINUTES
            DurationUnit.MINUTES -> DurationUnit.HOURS
            DurationUnit.HOURS -> DurationUnit.DAYS
            else -> null
        }?.let { upperUnit ->
            TimeWindow(
                window = window,
                continues = continues,
                durationUnit = upperUnit,
                interval = upperIntervalOrNull ?: interval,
                fromDouble = fromDouble,
                toDouble = toDouble,
                fromDuration = fromDuration,
                toDuration = toDuration
            )
        }
    }

    /**
     * 上级时间窗口 / The upper-level time window
     *
     * @return 上级时间窗口，若不受支持则返回错误 / The upper-level time window, or an error if not supported
    */
    fun upper(): Ret<TimeWindow<V>> {
        val upperUnit = when (durationUnit) {
            DurationUnit.SECONDS -> DurationUnit.MINUTES
            DurationUnit.MINUTES -> DurationUnit.HOURS
            DurationUnit.HOURS -> DurationUnit.DAYS
            else -> return Failed(ErrorCode.IllegalArgument, "TimeWindow.upper 暂不支持 durationUnit=$durationUnit，仅支持 SECONDS/MINUTES/HOURS。")
        }
        return Ok(TimeWindow(
            window = window,
            continues = continues,
            durationUnit = upperUnit,
            interval = upperIntervalOrNull!!,
            fromDouble = fromDouble,
            toDouble = toDouble,
            fromDuration = fromDuration,
            toDuration = toDuration
        ))
    }

    /**
     * 按缩放比例计算上级时间间隔 / Calculate upper interval by scale
     * @param scale 缩放比例 / Scale factor
     * @return 上级时间间隔 / The upper-level time interval
    */
    fun upperIntervalByScale(scale: UInt64): Duration {
        val upperInterval = interval * scale.toInt().toDouble()
        return if (upperInterval > 1.toDuration(DurationUnit.DAYS) && durationUnit.ordinal < DurationUnit.DAYS.ordinal) {
            1.toDuration(DurationUnit.DAYS)
        } else if (upperInterval > 1.toDuration(DurationUnit.HOURS) && durationUnit.ordinal < DurationUnit.HOURS.ordinal) {
            1.toDuration(DurationUnit.HOURS)
        } else if (upperInterval > 1.toDuration(DurationUnit.MINUTES) && durationUnit.ordinal < DurationUnit.MINUTES.ordinal) {
            1.toDuration(DurationUnit.MINUTES)
        } else {
            upperInterval
        }
    }

    /**
     * 按缩放比例创建上级时间窗口 / Create upper time window by scale
     *
     * @param scale 缩放比例 / Scale factor
     * @return 上级时间窗口 / The upper-level time window
    */
    fun upperByScale(scale: UInt64): TimeWindow<V> {
        val scaleInterval = interval * scale.toInt().toDouble()
        val (upperUnit, upperInterval) = if (scaleInterval > 1.toDuration(DurationUnit.DAYS) && durationUnit.ordinal < DurationUnit.DAYS.ordinal) {
            Pair(DurationUnit.DAYS, 1.toDuration(DurationUnit.DAYS))
        } else if (scaleInterval > 1.toDuration(DurationUnit.HOURS) && durationUnit.ordinal < DurationUnit.HOURS.ordinal) {
            Pair(DurationUnit.HOURS, 1.toDuration(DurationUnit.HOURS))
        } else if (scaleInterval > 1.toDuration(DurationUnit.MINUTES) && durationUnit.ordinal < DurationUnit.MINUTES.ordinal) {
            Pair(DurationUnit.MINUTES, 1.toDuration(DurationUnit.MINUTES))
        } else {
            Pair(durationUnit, scaleInterval)
        }
        return TimeWindow(
            window = window,
            continues = continues,
            durationUnit = upperUnit,
            interval = upperInterval,
            fromDouble = fromDouble,
            toDouble = toDouble,
            fromDuration = fromDuration,
            toDuration = toDuration
        )
    }

    /** 按默认间隔划分的时间段列表 / List of time slots divided by default interval */
    val timeSlots: List<TimeRange> by lazy {
        timeSlotsOf(interval)
    }

    /**
     * 按指定间隔划分时间段 / Divide time slots by the specified interval
     * @param interval 时间间隔 / The time interval
     * @return 时间段列表 / The list of time ranges
    */
    fun timeSlotsOf(interval: Duration): List<TimeRange> {
        val timeSlots = ArrayList<TimeRange>()
        var current = start
        while (current != end) {
            val duration = min(end - current, interval)
            timeSlots.add(
                TimeRange(
                    start = current,
                    end = current + duration
                )
            )
            current += duration
        }
        return timeSlots
    }

    /** 按上级间隔划分的舍入时间段列表 / List of rounded time slots divided by upper interval */
    val roundTimeSlots: List<TimeRange> by lazy {
        roundTimeSlotsOf(upperOrNull?.interval ?: interval)
    }

    /**
     * 按指定间隔生成舍入时间段 / Generate rounded time slots by the specified interval
     * @param interval 时间间隔 / The time interval
     * @param excludedTimes 排除的时间段列表 / The list of excluded time ranges
     * @return 舍入时间段列表 / The list of rounded time ranges
    */
    fun roundTimeSlotsOf(
        interval: Duration,
        excludedTimes: List<TimeRange> = emptyList()
    ): List<TimeRange> {
        return roundTimeSlotsOf(
            intervals = mapOf(null to interval),
            excludedTimes = excludedTimes
        )
    }

    /**
     * Generate rounded time slots based on intervals and excluded times.
     * 生成基于时间粒度和排除时间的舍入时间段。
     * @param intervals 时间范围到间隔的映射 / The mapping from time ranges to intervals
     * @param excludedTimes 排除的时间段列表 / The list of excluded time ranges
     * @return 舍入时间段列表 / The list of rounded time ranges
    */
    fun roundTimeSlotsOf(
        intervals: Map<TimeRange?, Duration>,
        excludedTimes: List<TimeRange> = emptyList()
    ): List<TimeRange> {
        val timeSlots = ArrayList<TimeRange>()
        val defaultInterval = intervals[null] ?: upperIntervalOrNull ?: interval
        val specificIntervals = ArrayList<Pair<TimeRange, Duration>>(intervals.size)
        for ((timeRange, thisInterval) in intervals) {
            if (timeRange != null) {
                specificIntervals.add(timeRange to thisInterval)
            }
        }
        var current = start

/**
 * intervalAt.
 * intervalAt。
 * @param time 时间点 / The instant
 * @return 时间间隔 / The time interval
*/
        fun intervalAt(time: Instant): Duration {
            for ((timeRange, thisInterval) in specificIntervals) {
                if (timeRange.contains(time)) {
                    return thisInterval
                }
            }
            return defaultInterval
        }
        var currentInterval = intervalAt(start)
        val upperDurationUnit = upperOrNull?.durationUnit ?: durationUnit
        val upperIntervalVal = upperOrNull?.interval ?: interval
        val end1 = (start.truncatedTo(upperDurationUnit) + currentInterval * kotlin.math.ceil(upperIntervalVal / currentInterval).toInt())
            .let {
                if ((it - start) < currentInterval) {
                    it + upperIntervalVal
                } else {
                    it
                }
            }
        val firstStageEnd = min(end1, end)
        while (current < firstStageEnd) {
            var duration = if (current == start) {
                val durationDiff = (end1 - start)
                val floorRatio = kotlin.math.floor(durationDiff / currentInterval)
                durationDiff - (floorRatio * currentInterval.toDouble(DurationUnit.SECONDS)).toDuration(DurationUnit.SECONDS)
            } else {
                min(firstStageEnd - current, currentInterval)
            }
            if (duration <= Duration.ZERO) {
                duration = min(firstStageEnd - current, currentInterval)
            }
            if (duration <= Duration.ZERO) {
                break
            }
            timeSlots.add(
                TimeRange(
                    start = max(start, current),
                    end = current + duration
                )
            )
            current += duration
            currentInterval = intervalAt(current)
        }
        val end2 = end.truncatedTo(upperDurationUnit)
        while (current < end2) {
            val duration = min(end2 - current, currentInterval)
            if (duration <= Duration.ZERO) {
                break
            }
            timeSlots.add(
                TimeRange(
                    start = current,
                    end = current + duration
                )
            )
            current += duration
            currentInterval = intervalAt(current)
        }
        while (current < end) {
            val duration = min(end - current, currentInterval)
            if (duration <= Duration.ZERO) {
                break
            }
            timeSlots.add(
                TimeRange(
                    start = current,
                    end = current + duration
                )
            )
            current += duration
            currentInterval = intervalAt(current)
        }
        if (excludedTimes.isEmpty()) {
            return timeSlots
        }
        val clippedExcludedTimes = ArrayList<TimeRange>(excludedTimes.size)
        for (excludedTime in excludedTimes) {
            if (!excludedTime.withIntersection(window)) {
                continue
            }
            clippedExcludedTimes.add(
                TimeRange(
                    start = max(excludedTime.start, start),
                    end = min(excludedTime.end, end)
                )
            )
        }
        if (clippedExcludedTimes.isEmpty()) {
            return timeSlots
        }
        val normalizedExcludedTimes = clippedExcludedTimes.merge()
        if (normalizedExcludedTimes.isEmpty()) {
            return timeSlots
        }

        val pieces = ArrayList<TimeRange>()
        for (slot in timeSlots) {
            pieces.addAll(slot.differenceWith(normalizedExcludedTimes))
        }
        return pieces
    }

    /**
     * 判断是否与另一时间范围有交集 / Check whether this window intersects with another range
     * @param ano 另一时间范围 / The other time range
     * @return 是否有交集 / Whether there is an intersection
    */
    fun withIntersection(ano: TimeRange): Boolean {
        return window.withIntersection(ano)
    }

    /**
     * 判断是否包含指定时间点 / Check whether this window contains the specified instant
     * @param time 时间点 / The instant
     * @return 是否包含 / Whether the instant is contained
    */
    fun contains(time: Instant): Boolean {
        return window.contains(time)
    }

    /**
     * 判断是否包含指定时间范围 / Check whether this window contains the specified time range
     * @param time 时间范围 / The time range
     * @return 是否包含 / Whether the time range is contained
    */
    fun contains(time: TimeRange): Boolean {
        return window.contains(time)
    }

    /**
     * 按指定时间点拆分窗口 / Split the window at the specified instants
     * @param times 时间点列表 / The list of instants
     * @return 拆分后的时间范围列表 / The list of split time ranges
    */
    fun split(
        times: List<Instant>
    ): List<TimeRange> {
        return window.split(times)
    }

    /**
     * 按持续时间单元拆分窗口 / Split the window by duration unit
     * @param unit 持续时间范围 / The duration range
     * @param currentDuration 当前已消耗的持续时间 / The current consumed duration
     * @param maxDuration 最大持续时间 / The maximum duration
     * @param breakTime 休息时间 / The break time
     * @return 拆分后的时间范围 / The split time ranges
    */
    fun split(
        unit: DurationRange,
        currentDuration: Duration = Duration.ZERO,
        maxDuration: Duration? = null,
        breakTime: Duration? = null
    ): TimeRange.SplitTimeRanges {
        return window.split(
            unit = unit,
            currentDuration = currentDuration,
            maxDuration = maxDuration,
            breakTime = breakTime
        )
    }

    /**
     * 反向按持续时间单元拆分窗口 / Reverse split the window by duration unit
     * @param unit 持续时间范围 / The duration range
     * @param maxDuration 最大持续时间 / The maximum duration
     * @param breakTime 休息时间 / The break time
     * @return 拆分后的时间范围 / The split time ranges
    */
    fun rsplit(
        unit: DurationRange,
        maxDuration: Duration? = null,
        breakTime: Duration? = null
    ): Ret<TimeRange.SplitTimeRanges> {
        return window.rsplit(
            unit = unit,
            maxDuration = maxDuration,
            breakTime = breakTime
        )
    }

    /**
     * 计算给定时间所属的日期 / Calculate the date to which the given time belongs
     * @param time 本地日期时间 / The local date-time
     * @param timeZone 时区 / The time zone
     * @return 日期 / The date
    */
    fun date(
        time: LocalDateTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): LocalDate {
        return if (time.toInstant(timeZone) < (time.date.atStartOfDayIn(timeZone) + dateOffset)) {
            time.date.minus(DatePeriod(days = 1))
        } else {
            time.date
        }
    }

    /**
     * 创建新的时间窗口 / Create a new time window
     * @param window 时间范围 / The time range
     * @param continues 是否连续 / Whether continuous
     * @return 新的时间窗口 / The new time window
    */
    fun new(window: TimeRange, continues: Boolean): TimeWindow<V> {
        return TimeWindow(
            window = window,
            continues = continues,
            durationUnit = durationUnit,
            fromDouble = fromDouble,
            toDouble = toDouble,
            fromDuration = fromDuration,
            toDuration = toDuration
        )
    }
}
