@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.dimension.Time
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 时间物理量与 kotlin.time.Duration 互转换扩展
 * Extension functions for converting between time Quantity and kotlin.time.Duration
 *
 * 提供物理量时间单位与 Kotlin Duration 类型之间的双向转换，支持纳秒到天的各种时间单位。
 * Provides bidirectional conversion between physical quantity time units and Kotlin Duration type,
 * supporting various time units from nanoseconds to days.
 *
 * 支持的数值类型 / Supported number types:
 * - Flt64, FltX (浮点数 / Floating-point)
 * - Int64, IntX (有符号整数 / Signed integer)
 * - UInt64, UIntX (无符号整数 / Unsigned integer)
 */

// ============================================================================
// 泛型接口定义 / Generic interface definitions
// ============================================================================

/**
 * Duration 转换器接口
 * Duration converter interface
 *
 * 定义从 Duration 转换为特定数值类型的方法
 * Defines methods for converting Duration to specific number types
 */
interface DurationConverter<V> {
    /**
     * 从 Double 创建数值
     * Create number from Double
     */
    fun fromDouble(value: Double): Ret<V>

    /**
     * 将数值转换为 Double（秒为单位）
     * Convert number to Double (in seconds)
     */
    fun toDoubleSeconds(value: V): Double

    /**
     * 获取数值的零值
     * Get zero value of the number type
     */
    fun zero(): V
}

// ============================================================================
// DurationConverter 实现 / DurationConverter implementations
// ============================================================================

/**
 * Flt64 转换器
 * Flt64 converter
 */
object Flt64DurationConverter : DurationConverter<Flt64> {
    override fun fromDouble(value: Double): Ret<Flt64> = Ok(Flt64(value))
    override fun toDoubleSeconds(value: Flt64) = value.toDouble()
    override fun zero() = Flt64.zero
}

/**
 * FltX 转换器
 * FltX converter
 */
object FltXDurationConverter : DurationConverter<FltX> {
    override fun fromDouble(value: Double): Ret<FltX> = Ok(FltX(value))
    override fun toDoubleSeconds(value: FltX) = value.toFlt64().toDouble()
    override fun zero() = FltX.zero
}

/**
 * Int64 转换器
 * Int64 converter
 */
object Int64DurationConverter : DurationConverter<Int64> {
    override fun fromDouble(value: Double): Ret<Int64> = Ok(Int64(value.toLong()))
    override fun toDoubleSeconds(value: Int64) = value.toFlt64().toDouble()
    override fun zero() = Int64.zero
}

/**
 * IntX 转换器
 * IntX converter
 */
object IntXDurationConverter : DurationConverter<IntX> {
    override fun fromDouble(value: Double): Ret<IntX> = Ok(IntX(value.toLong()))
    override fun toDoubleSeconds(value: IntX) = value.toFlt64().toDouble()
    override fun zero() = IntX.zero
}

/**
 * UInt64 转换器
 * UInt64 converter
 */
object UInt64DurationConverter : DurationConverter<UInt64> {
    override fun fromDouble(value: Double): Ret<UInt64> {
        return if (value < 0) {
            Failed(ErrorCode.IllegalArgument, "Cannot convert negative duration $value to UInt64")
        } else {
            Ok(UInt64(value.toULong()))
        }
    }
    override fun toDoubleSeconds(value: UInt64) = value.toFlt64().toDouble()
    override fun zero() = UInt64.zero
}

/**
 * UIntX 转换器
 * UIntX converter
 */
object UIntXDurationConverter : DurationConverter<UIntX> {
    override fun fromDouble(value: Double): Ret<UIntX> {
        return if (value < 0) {
            Failed(ErrorCode.IllegalArgument, "Cannot convert negative duration $value to UIntX")
        } else {
            Ok(UIntX(value.toLong()))
        }
    }
    override fun toDoubleSeconds(value: UIntX) = value.toFlt64().toDouble()
    override fun zero() = UIntX.zero
}

// ============================================================================
// 转换器注册表 / Converter registry
// ============================================================================

/**
 * 转换器缓存
 * Converter cache
 */
private val converters = mutableMapOf<Class<*>, DurationConverter<*>>(
    Flt64::class.java to Flt64DurationConverter,
    FltX::class.java to FltXDurationConverter,
    Int64::class.java to Int64DurationConverter,
    IntX::class.java to IntXDurationConverter,
    UInt64::class.java to UInt64DurationConverter,
    UIntX::class.java to UIntXDurationConverter
)

/**
 * 获取指定类型的转换器
 * Get converter for specified type
 */
fun <V> getConverter(valueClass: Class<V>): Ret<DurationConverter<V>> {
    val converter = converters[valueClass] as? DurationConverter<V>
    return if (converter != null) {
        Ok(converter)
    } else {
        Failed(ErrorCode.IllegalArgument, "No DurationConverter registered for ${valueClass.name}")
    }
}

// ============================================================================
// Quantity -> Duration 转换（泛型版本）/ Quantity to Duration conversion (generic)
// ============================================================================

/**
 * 将时间物理量转换为 kotlin.time.Duration
 * Convert time quantity to kotlin.time.Duration
 *
 * 示例 / Example:
 * ```kotlin
 * val time = Flt64(5.0) * Second
 * val result = time.toDuration()  // Ok(5 seconds)
 *
 * val time2 = Int64(1000) * Millisecond
 * val result2 = time2.toDuration()  // Ok(1 second)
 *
 * val time3 = Flt64(5.0) * Meter
 * val result3 = time3.toDuration()  // Failed(IllegalArgument)
 * ```
 *
 * @param V 数值类型
 * @return 对应的 Duration，或错误
 */
fun <V> Quantity<V>.toDuration(): Ret<Duration> where V : RealNumber<V> {
    // 检查量纲
    if (this.unit.quantity != Time) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert quantity with dimension ${this.unit.quantity.name ?: this.unit.quantity.dimensionSymbol()} to Duration. Expected Time dimension."
        )
    }

    // 获取转换器
    val converter = when (val result = getConverter(this.value::class.java)) {
        is Ok -> result.value as DurationConverter<V>
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    // 获取从当前单位到秒的转换因子
    val factor = this.unit.to(Second)?.value?.toFlt64()?.toDouble() ?: return Failed(
        ErrorCode.Other,
        "Failed to convert ${this.unit.name} to seconds"
    )

    // 计算秒值并创建 Duration
    val secondsValue = converter.toDoubleSeconds(this.value) * factor
    return Ok(secondsValue.toDuration(DurationUnit.SECONDS))
}

/**
 * 将时间物理量转换为指定单位的 Duration
 * Convert time quantity to kotlin.time.Duration in specified unit
 *
 * @param V 数值类型
 * @param unit 目标 Duration 单位
 * @return 对应的 Duration，或错误
 */
fun <V> Quantity<V>.toDuration(unit: DurationUnit): Ret<Duration> where V : RealNumber<V> {
    return when (val result = this.toDuration()) {
        is Ok -> {
            val duration = result.value
            Ok(duration.toDouble(unit).toDuration(unit))
        }
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

// ============================================================================
// Duration -> Quantity 转换（泛型版本）/ Duration to Quantity conversion (generic)
// ============================================================================

/**
 * 将 Duration 转换为指定时间单位的时间物理量（泛型版本）
 * Convert Duration to time quantity in specified unit (generic version)
 *
 * 示例 / Example:
 * ```kotlin
 * val duration = 5.seconds
 * val result1 = duration.toQuantity<Flt64>(Second)  // Ok(5.0 s)
 * val result2 = duration.toQuantity<Int64>(Millisecond)  // Ok(5000 ms)
 * ```
 *
 * @param V 数值类型
 * @param unit 目标物理量单位
 * @return 对应的时间物理量，或错误
 */
inline fun <reified V> Duration.toQuantity(unit: PhysicalUnit): Ret<Quantity<V>> where V : RealNumber<V> {
    // 检查量纲
    if (unit.quantity != Time) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert Duration to quantity with dimension ${unit.quantity.name ?: unit.quantity.dimensionSymbol()}. Expected Time unit."
        )
    }

    // 获取转换器
    val converter = when (val result = getConverter(V::class.java)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    // 获取秒值
    val secondsValue = this.toDouble(DurationUnit.SECONDS)

    // 获取从秒到目标单位的转换因子
    val factor = Second.to(unit)?.value?.toFlt64()?.toDouble()
    if (factor == null) {
        return Failed(
            ErrorCode.Other,
            "Failed to convert seconds to ${unit.name}"
        )
    }

    // 计算目标单位下的值
    return when (val valueResult = converter.fromDouble(secondsValue * factor)) {
        is Ok -> {
            Ok((valueResult.value * unit) as Quantity<V>)
        }
        is Failed -> Failed(valueResult.error)
        is Fatal -> Fatal(valueResult.errors)
    }
}

/**
 * 将 Duration 转换为指定时间单位的时间物理量（显式转换器版本）
 * Convert Duration to time quantity with explicit converter
 *
 * 用于非 reified 场景 / For non-reified scenarios
 *
 * @param V 数值类型
 * @param unit 目标物理量单位
 * @param converter 数值类型转换器
 * @return 对应的时间物理量，或错误
 */
fun <V> Duration.toQuantity(unit: PhysicalUnit, converter: DurationConverter<V>): Ret<Quantity<V>> where V : RealNumber<V> {
    // 检查量纲
    if (unit.quantity != Time) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert Duration to quantity with dimension ${unit.quantity.name ?: unit.quantity.dimensionSymbol()}. Expected Time unit."
        )
    }

    // 获取秒值
    val secondsValue = this.toDouble(DurationUnit.SECONDS)

    // 获取从秒到目标单位的转换因子
    val factor = Second.to(unit)?.value?.toFlt64()?.toDouble() ?: return Failed(
        ErrorCode.Other,
        "Failed to convert seconds to ${unit.name}"
    )

    // 计算目标单位下的值
    return when (val valueResult = converter.fromDouble(secondsValue * factor)) {
        is Ok -> {
            Ok((valueResult.value * unit) as Quantity<V>)
        }
        is Failed -> Failed(valueResult.error)
        is Fatal -> Fatal(valueResult.errors)
    }
}

// ============================================================================
// Duration -> Quantity 便捷方法（特定类型）/ Duration to Quantity convenience methods
// ============================================================================

/**
 * 将 Duration 转换为 Flt64 时间物理量
 * Convert Duration to Flt64 time quantity
 */
fun Duration.toQuantityFlt64(unit: PhysicalUnit): Ret<Quantity<Flt64>> {
    return this.toQuantity(unit, Flt64DurationConverter)
}

/**
 * 将 Duration 转换为 FltX 时间物理量
 * Convert Duration to FltX time quantity
 */
fun Duration.toQuantityFltX(unit: PhysicalUnit): Ret<Quantity<FltX>> {
    return this.toQuantity(unit, FltXDurationConverter)
}

/**
 * 将 Duration 转换为 Int64 时间物理量
 * Convert Duration to Int64 time quantity
 */
fun Duration.toQuantityInt64(unit: PhysicalUnit): Ret<Quantity<Int64>> {
    return this.toQuantity(unit, Int64DurationConverter)
}

/**
 * 将 Duration 转换为 IntX 时间物理量
 * Convert Duration to IntX time quantity
 */
fun Duration.toQuantityIntX(unit: PhysicalUnit): Ret<Quantity<IntX>> {
    return this.toQuantity(unit, IntXDurationConverter)
}

/**
 * 将 Duration 转换为 UInt64 时间物理量
 * Convert Duration to UInt64 time quantity
 */
fun Duration.toQuantityUInt64(unit: PhysicalUnit): Ret<Quantity<UInt64>> {
    return this.toQuantity(unit, UInt64DurationConverter)
}

/**
 * 将 Duration 转换为 UIntX 时间物理量
 * Convert Duration to UIntX time quantity
 */
fun Duration.toQuantityUIntX(unit: PhysicalUnit): Ret<Quantity<UIntX>> {
    return this.toQuantity(unit, UIntXDurationConverter)
}

// ============================================================================
// 便捷方法：秒 / Convenience methods: Seconds
// ============================================================================

fun Duration.toQuantitySecondsFlt64(): Ret<Quantity<Flt64>> = toQuantityFlt64(Second)
fun Duration.toQuantitySecondsFltX(): Ret<Quantity<FltX>> = toQuantityFltX(Second)
fun Duration.toQuantitySecondsInt64(): Ret<Quantity<Int64>> = toQuantityInt64(Second)
fun Duration.toQuantitySecondsIntX(): Ret<Quantity<IntX>> = toQuantityIntX(Second)
fun Duration.toQuantitySecondsUInt64(): Ret<Quantity<UInt64>> = toQuantityUInt64(Second)
fun Duration.toQuantitySecondsUIntX(): Ret<Quantity<UIntX>> = toQuantityUIntX(Second)

// ============================================================================
// 便捷方法：毫秒 / Convenience methods: Milliseconds
// ============================================================================

fun Duration.toQuantityMillisecondsFlt64(): Ret<Quantity<Flt64>> = toQuantityFlt64(Millisecond)
fun Duration.toQuantityMillisecondsFltX(): Ret<Quantity<FltX>> = toQuantityFltX(Millisecond)
fun Duration.toQuantityMillisecondsInt64(): Ret<Quantity<Int64>> = toQuantityInt64(Millisecond)
fun Duration.toQuantityMillisecondsIntX(): Ret<Quantity<IntX>> = toQuantityIntX(Millisecond)
fun Duration.toQuantityMillisecondsUInt64(): Ret<Quantity<UInt64>> = toQuantityUInt64(Millisecond)
fun Duration.toQuantityMillisecondsUIntX(): Ret<Quantity<UIntX>> = toQuantityUIntX(Millisecond)

// ============================================================================
// 便捷方法：分钟 / Convenience methods: Minutes
// ============================================================================

fun Duration.toQuantityMinutesFlt64(): Ret<Quantity<Flt64>> = toQuantityFlt64(Minute)
fun Duration.toQuantityMinutesFltX(): Ret<Quantity<FltX>> = toQuantityFltX(Minute)
fun Duration.toQuantityMinutesInt64(): Ret<Quantity<Int64>> = toQuantityInt64(Minute)
fun Duration.toQuantityMinutesIntX(): Ret<Quantity<IntX>> = toQuantityIntX(Minute)
fun Duration.toQuantityMinutesUInt64(): Ret<Quantity<UInt64>> = toQuantityUInt64(Minute)
fun Duration.toQuantityMinutesUIntX(): Ret<Quantity<UIntX>> = toQuantityUIntX(Minute)

// ============================================================================
// 便捷方法：小时 / Convenience methods: Hours
// ============================================================================

fun Duration.toQuantityHoursFlt64(): Ret<Quantity<Flt64>> = toQuantityFlt64(Hour)
fun Duration.toQuantityHoursFltX(): Ret<Quantity<FltX>> = toQuantityFltX(Hour)
fun Duration.toQuantityHoursInt64(): Ret<Quantity<Int64>> = toQuantityInt64(Hour)
fun Duration.toQuantityHoursIntX(): Ret<Quantity<IntX>> = toQuantityIntX(Hour)
fun Duration.toQuantityHoursUInt64(): Ret<Quantity<UInt64>> = toQuantityUInt64(Hour)
fun Duration.toQuantityHoursUIntX(): Ret<Quantity<UIntX>> = toQuantityUIntX(Hour)

// ============================================================================
// 便捷方法：天 / Convenience methods: Days
// ============================================================================

fun Duration.toQuantityDaysFlt64(): Ret<Quantity<Flt64>> = toQuantityFlt64(Day)
fun Duration.toQuantityDaysFltX(): Ret<Quantity<FltX>> = toQuantityFltX(Day)
fun Duration.toQuantityDaysInt64(): Ret<Quantity<Int64>> = toQuantityInt64(Day)
fun Duration.toQuantityDaysIntX(): Ret<Quantity<IntX>> = toQuantityIntX(Day)
fun Duration.toQuantityDaysUInt64(): Ret<Quantity<UInt64>> = toQuantityUInt64(Day)
fun Duration.toQuantityDaysUIntX(): Ret<Quantity<UIntX>> = toQuantityUIntX(Day)

// ============================================================================
// 智能单位选择 / Best fit unit selection
// ============================================================================

/**
 * 将 Duration 转换为最佳时间单位的时间物理量（泛型版本）
 * Convert Duration to time quantity in the best fitting unit (generic version)
 *
 * 自动选择最合适的单位以避免数值过大或过小。
 * Automatically selects the most appropriate unit to avoid very large or small values.
 *
 * 示例 / Example:
 * ```kotlin
 * val duration1 = 0.001.seconds
 * val result1 = duration1.toQuantityBestFit<Flt64>()  // Ok(1.0 ms)
 *
 * val duration2 = 3600.seconds
 * val result2 = duration2.toQuantityBestFit<Int64>()  // Ok(60 min)
 * ```
 *
 * @param V 数值类型
 * @param threshold 单位切换阈值，默认为 1000.0
 * @return 以最佳单位表示的时间物理量，或错误
 */
inline fun <reified V> Duration.toQuantityBestFit(threshold: Double = 1000.0): Ret<Quantity<V>> where V : RealNumber<V> {
    val converter = when (val result = getConverter(V::class.java)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    val secondsValue = this.toDouble(DurationUnit.SECONDS)
    val absSeconds = kotlin.math.abs(secondsValue)

    val (factor, unit) = when {
        absSeconds == 0.0 -> 1.0 to Second
        absSeconds < 1e-9 -> 1e9 to Nanosecond
        absSeconds < 1e-6 -> 1e9 to Nanosecond
        absSeconds < 1e-3 -> 1e6 to Microsecond
        absSeconds < 1.0 -> 1e3 to Millisecond
        absSeconds < 60.0 -> 1.0 to Second
        absSeconds < 3600.0 -> (1.0 / 60.0) to Minute
        absSeconds < 86400.0 -> (1.0 / 3600.0) to Hour
        absSeconds < 86400.0 * 365.25 -> (1.0 / 86400.0) to Day
        else -> (1.0 / (86400.0 * 365.25)) to Year
    }

    return when (val r = converter.fromDouble(secondsValue * factor)) {
        is Ok -> {
            Ok((r.value * unit) as Quantity<V>)
        }
        is Failed -> Failed(r.error)
        is Fatal -> Fatal(r.errors)
    }
}

/**
 * 将 Duration 转换为 Flt64 最佳单位时间物理量
 * Convert Duration to Flt64 time quantity with best fit unit
 */
fun Duration.toQuantityBestFitFlt64(threshold: Double = 1000.0): Ret<Quantity<Flt64>> {
    return toQuantityBestFit<Flt64>(threshold)
}

/**
 * 将 Duration 转换为 FltX 最佳单位时间物理量
 * Convert Duration to FltX time quantity with best fit unit
 */
fun Duration.toQuantityBestFitFltX(threshold: Double = 1000.0): Ret<Quantity<FltX>> {
    return toQuantityBestFit<FltX>(threshold)
}

/**
 * 将 Duration 转换为 Int64 最佳单位时间物理量
 * Convert Duration to Int64 time quantity with best fit unit
 */
fun Duration.toQuantityBestFitInt64(threshold: Double = 1000.0): Ret<Quantity<Int64>> {
    return toQuantityBestFit<Int64>(threshold)
}

/**
 * 将 Duration 转换为 UInt64 最佳单位时间物理量
 * Convert Duration to UInt64 time quantity with best fit unit
 */
fun Duration.toQuantityBestFitUInt64(threshold: Double = 1000.0): Ret<Quantity<UInt64>> {
    return toQuantityBestFit<UInt64>(threshold)
}

// ============================================================================
// 扩展属性 / Extension properties
// ============================================================================

/**
 * Duration 的 Flt64 纳秒值作为时间物理量
 * Duration value in nanoseconds as Flt64 time quantity
 */
val Duration.inNanosecondsQuantityFlt64: Ret<Quantity<Flt64>>
    get() = toQuantityFlt64(Nanosecond)

/**
 * Duration 的 Flt64 微秒值作为时间物理量
 * Duration value in microseconds as Flt64 time quantity
 */
val Duration.inMicrosecondsQuantityFlt64: Ret<Quantity<Flt64>>
    get() = toQuantityFlt64(Microsecond)

/**
 * Duration 的 Flt64 毫秒值作为时间物理量
 * Duration value in milliseconds as Flt64 time quantity
 */
val Duration.inMillisecondsQuantityFlt64: Ret<Quantity<Flt64>>
    get() = toQuantityFlt64(Millisecond)

/**
 * Duration 的 Flt64 秒值作为时间物理量
 * Duration value in seconds as Flt64 time quantity
 */
val Duration.inSecondsQuantityFlt64: Ret<Quantity<Flt64>>
    get() = toQuantityFlt64(Second)

/**
 * Duration 的 Flt64 分钟值作为时间物理量
 * Duration value in minutes as Flt64 time quantity
 */
val Duration.inMinutesQuantityFlt64: Ret<Quantity<Flt64>>
    get() = toQuantityFlt64(Minute)

/**
 * Duration 的 Flt64 小时值作为时间物理量
 * Duration value in hours as Flt64 time quantity
 */
val Duration.inHoursQuantityFlt64: Ret<Quantity<Flt64>>
    get() = toQuantityFlt64(Hour)

/**
 * Duration 的 Flt64 天值作为时间物理量
 * Duration value in days as Flt64 time quantity
 */
val Duration.inDaysQuantityFlt64: Ret<Quantity<Flt64>>
    get() = toQuantityFlt64(Day)

// FltX 扩展属性
val Duration.inNanosecondsQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Nanosecond)
val Duration.inMicrosecondsQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Microsecond)
val Duration.inMillisecondsQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Millisecond)
val Duration.inSecondsQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Second)
val Duration.inMinutesQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Minute)
val Duration.inHoursQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Hour)
val Duration.inDaysQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Day)

// Int64 扩展属性
val Duration.inNanosecondsQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Nanosecond)
val Duration.inMicrosecondsQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Microsecond)
val Duration.inMillisecondsQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Millisecond)
val Duration.inSecondsQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Second)
val Duration.inMinutesQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Minute)
val Duration.inHoursQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Hour)
val Duration.inDaysQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Day)

// UInt64 扩展属性
val Duration.inNanosecondsQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Nanosecond)
val Duration.inMicrosecondsQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Microsecond)
val Duration.inMillisecondsQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Millisecond)
val Duration.inSecondsQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Second)
val Duration.inMinutesQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Minute)
val Duration.inHoursQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Hour)
val Duration.inDaysQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Day)