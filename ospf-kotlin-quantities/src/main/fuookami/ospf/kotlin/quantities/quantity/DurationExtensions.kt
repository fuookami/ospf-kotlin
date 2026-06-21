@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.quantities.quantity

import kotlin.time.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.dimension.Time

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
 * 定义从 Duration 转换为特定数值类型的方法。
 * Defines methods for converting Duration to specific number types.
 *
 * @param V 数值类型 / The number type
 */
interface DurationConverter<V> {
    /**
     * 从 Double 创建数值
     * Create number from Double
     *
     * @param value Double 值 / The Double value
     * @return 转换结果 / The conversion result
     */
    fun fromDouble(value: Double): Ret<V>

    /**
     * 将数值转换为 Double（秒为单位）
     * Convert number to Double (in seconds)
     *
     * @param value 数值 / The number value
     * @return Double 值（秒） / The Double value in seconds
     */
    fun toDoubleSeconds(value: V): Double

    /**
     * 获取数值的零值
     * Get zero value of the number type
     *
     * @return 零值 / The zero value
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

@Suppress("UNCHECKED_CAST")
private fun <V> DurationConverter<*>.asDurationConverter(): DurationConverter<V> {
    // converters 的 key 是转换器支持的值类型 Class，读取时由同一个 Class 约束 V。
    // The converters key is the supported value Class, so lookup by that Class constrains V.
    return this as DurationConverter<V>
}

/**
 * 获取指定类型的转换器
 * Get converter for specified type
 *
 * @param V 数值类型 / The number type
 * @param valueClass 数值类型的 Class 对象 / The Class object of the number type
 * @return 转换器，或错误 / The converter, or an error
 */
fun <V> getConverter(valueClass: Class<V>): Ret<DurationConverter<V>> {
    val converter = converters[valueClass]?.asDurationConverter<V>()
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
 * @param V 数值类型 / The number type
 * @return 对应的 Duration，或错误 / The corresponding Duration, or an error
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
        is Ok -> result.value.asDurationConverter<V>()
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    // 获取从当前单位到秒的转换因子
    val factor = this.unit.to(Second)?.value?.toFlt64()?.toDouble() ?: return Failed(
        ErrorCode.IllegalArgument,
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
 * @param V 数值类型 / The number type
 * @param unit 目标 Duration 单位 / The target Duration unit
 * @return 对应的 Duration，或错误 / The corresponding Duration, or an error
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
    val factor = Second.to(unit)?.value?.toFlt64()?.toDouble() ?: return Failed(
        ErrorCode.IllegalArgument,
        "Failed to convert seconds to ${unit.name}"
    )

    // 计算目标单位下的值
    return when (val valueResult = converter.fromDouble(secondsValue * factor)) {
        is Ok -> {
            Ok(valueResult.value * unit)
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
        ErrorCode.IllegalArgument,
        "Failed to convert seconds to ${unit.name}"
    )

    // 计算目标单位下的值
    return when (val valueResult = converter.fromDouble(secondsValue * factor)) {
        is Ok -> {
            Ok((valueResult.value * unit))
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
 *
 * @param unit 目标物理量单位 / The target physical unit
 * @return 对应的时间物理量，或错误 / The corresponding time quantity, or an error
 */
fun Duration.toQuantityFlt64(unit: PhysicalUnit): Ret<Quantity<Flt64>> {
    return this.toQuantity(unit, Flt64DurationConverter)
}

/**
 * 将 Duration 转换为 FltX 时间物理量
 * Convert Duration to FltX time quantity
 *
 * @param unit 目标物理量单位 / The target physical unit
 * @return 对应的时间物理量，或错误 / The corresponding time quantity, or an error
 */
fun Duration.toQuantityFltX(unit: PhysicalUnit): Ret<Quantity<FltX>> {
    return this.toQuantity(unit, FltXDurationConverter)
}

/**
 * 将 Duration 转换为 Int64 时间物理量
 * Convert Duration to Int64 time quantity
 *
 * @param unit 目标物理量单位 / The target physical unit
 * @return 对应的时间物理量，或错误 / The corresponding time quantity, or an error
 */
fun Duration.toQuantityInt64(unit: PhysicalUnit): Ret<Quantity<Int64>> {
    return this.toQuantity(unit, Int64DurationConverter)
}

/**
 * 将 Duration 转换为 IntX 时间物理量
 * Convert Duration to IntX time quantity
 *
 * @param unit 目标物理量单位 / The target physical unit
 * @return 对应的时间物理量，或错误 / The corresponding time quantity, or an error
 */
fun Duration.toQuantityIntX(unit: PhysicalUnit): Ret<Quantity<IntX>> {
    return this.toQuantity(unit, IntXDurationConverter)
}

/**
 * 将 Duration 转换为 UInt64 时间物理量
 * Convert Duration to UInt64 time quantity
 *
 * @param unit 目标物理量单位 / The target physical unit
 * @return 对应的时间物理量，或错误 / The corresponding time quantity, or an error
 */
fun Duration.toQuantityUInt64(unit: PhysicalUnit): Ret<Quantity<UInt64>> {
    return this.toQuantity(unit, UInt64DurationConverter)
}

/**
 * 将 Duration 转换为 UIntX 时间物理量
 * Convert Duration to UIntX time quantity
 *
 * @param unit 目标物理量单位 / The target physical unit
 * @return 对应的时间物理量，或错误 / The corresponding time quantity, or an error
 */
fun Duration.toQuantityUIntX(unit: PhysicalUnit): Ret<Quantity<UIntX>> {
    return this.toQuantity(unit, UIntXDurationConverter)
}

// ============================================================================
// 便捷方法：秒 / Convenience methods: Seconds
// ============================================================================

/** 将 Duration 转换为秒的 Flt64 物理量 / Convert Duration to Flt64 quantity in seconds. @return 秒物理量或错误 / Seconds quantity or error */
fun Duration.toQuantitySecondsFlt64(): Ret<Quantity<Flt64>> = toQuantityFlt64(Second)
/** 将 Duration 转换为秒的 FltX 物理量 / Convert Duration to FltX quantity in seconds. @return 秒物理量或错误 / Seconds quantity or error */
fun Duration.toQuantitySecondsFltX(): Ret<Quantity<FltX>> = toQuantityFltX(Second)
/** 将 Duration 转换为秒的 Int64 物理量 / Convert Duration to Int64 quantity in seconds. @return 秒物理量或错误 / Seconds quantity or error */
fun Duration.toQuantitySecondsInt64(): Ret<Quantity<Int64>> = toQuantityInt64(Second)
/** 将 Duration 转换为秒的 IntX 物理量 / Convert Duration to IntX quantity in seconds. @return 秒物理量或错误 / Seconds quantity or error */
fun Duration.toQuantitySecondsIntX(): Ret<Quantity<IntX>> = toQuantityIntX(Second)
/** 将 Duration 转换为秒的 UInt64 物理量 / Convert Duration to UInt64 quantity in seconds. @return 秒物理量或错误 / Seconds quantity or error */
fun Duration.toQuantitySecondsUInt64(): Ret<Quantity<UInt64>> = toQuantityUInt64(Second)
/** 将 Duration 转换为秒的 UIntX 物理量 / Convert Duration to UIntX quantity in seconds. @return 秒物理量或错误 / Seconds quantity or error */
fun Duration.toQuantitySecondsUIntX(): Ret<Quantity<UIntX>> = toQuantityUIntX(Second)

// ============================================================================
// 便捷方法：毫秒 / Convenience methods: Milliseconds
// ============================================================================

/** 将 Duration 转换为毫秒的 Flt64 物理量 / Convert Duration to Flt64 quantity in milliseconds. @return 毫秒物理量或错误 / Milliseconds quantity or error */
fun Duration.toQuantityMillisecondsFlt64(): Ret<Quantity<Flt64>> = toQuantityFlt64(Millisecond)
/** 将 Duration 转换为毫秒的 FltX 物理量 / Convert Duration to FltX quantity in milliseconds. @return 毫秒物理量或错误 / Milliseconds quantity or error */
fun Duration.toQuantityMillisecondsFltX(): Ret<Quantity<FltX>> = toQuantityFltX(Millisecond)
/** 将 Duration 转换为毫秒的 Int64 物理量 / Convert Duration to Int64 quantity in milliseconds. @return 毫秒物理量或错误 / Milliseconds quantity or error */
fun Duration.toQuantityMillisecondsInt64(): Ret<Quantity<Int64>> = toQuantityInt64(Millisecond)
/** 将 Duration 转换为毫秒的 IntX 物理量 / Convert Duration to IntX quantity in milliseconds. @return 毫秒物理量或错误 / Milliseconds quantity or error */
fun Duration.toQuantityMillisecondsIntX(): Ret<Quantity<IntX>> = toQuantityIntX(Millisecond)
/** 将 Duration 转换为毫秒的 UInt64 物理量 / Convert Duration to UInt64 quantity in milliseconds. @return 毫秒物理量或错误 / Milliseconds quantity or error */
fun Duration.toQuantityMillisecondsUInt64(): Ret<Quantity<UInt64>> = toQuantityUInt64(Millisecond)
/** 将 Duration 转换为毫秒的 UIntX 物理量 / Convert Duration to UIntX quantity in milliseconds. @return 毫秒物理量或错误 / Milliseconds quantity or error */
fun Duration.toQuantityMillisecondsUIntX(): Ret<Quantity<UIntX>> = toQuantityUIntX(Millisecond)

// ============================================================================
// 便捷方法：分钟 / Convenience methods: Minutes
// ============================================================================

/** 将 Duration 转换为分钟的 Flt64 物理量 / Convert Duration to Flt64 quantity in minutes. @return 分钟物理量或错误 / Minutes quantity or error */
fun Duration.toQuantityMinutesFlt64(): Ret<Quantity<Flt64>> = toQuantityFlt64(Minute)
/** 将 Duration 转换为分钟的 FltX 物理量 / Convert Duration to FltX quantity in minutes. @return 分钟物理量或错误 / Minutes quantity or error */
fun Duration.toQuantityMinutesFltX(): Ret<Quantity<FltX>> = toQuantityFltX(Minute)
/** 将 Duration 转换为分钟的 Int64 物理量 / Convert Duration to Int64 quantity in minutes. @return 分钟物理量或错误 / Minutes quantity or error */
fun Duration.toQuantityMinutesInt64(): Ret<Quantity<Int64>> = toQuantityInt64(Minute)
/** 将 Duration 转换为分钟的 IntX 物理量 / Convert Duration to IntX quantity in minutes. @return 分钟物理量或错误 / Minutes quantity or error */
fun Duration.toQuantityMinutesIntX(): Ret<Quantity<IntX>> = toQuantityIntX(Minute)
/** 将 Duration 转换为分钟的 UInt64 物理量 / Convert Duration to UInt64 quantity in minutes. @return 分钟物理量或错误 / Minutes quantity or error */
fun Duration.toQuantityMinutesUInt64(): Ret<Quantity<UInt64>> = toQuantityUInt64(Minute)
/** 将 Duration 转换为分钟的 UIntX 物理量 / Convert Duration to UIntX quantity in minutes. @return 分钟物理量或错误 / Minutes quantity or error */
fun Duration.toQuantityMinutesUIntX(): Ret<Quantity<UIntX>> = toQuantityUIntX(Minute)

// ============================================================================
// 便捷方法：小时 / Convenience methods: Hours
// ============================================================================

/** 将 Duration 转换为小时的 Flt64 物理量 / Convert Duration to Flt64 quantity in hours. @return 小时物理量或错误 / Hours quantity or error */
fun Duration.toQuantityHoursFlt64(): Ret<Quantity<Flt64>> = toQuantityFlt64(Hour)
/** 将 Duration 转换为小时的 FltX 物理量 / Convert Duration to FltX quantity in hours. @return 小时物理量或错误 / Hours quantity or error */
fun Duration.toQuantityHoursFltX(): Ret<Quantity<FltX>> = toQuantityFltX(Hour)
/** 将 Duration 转换为小时的 Int64 物理量 / Convert Duration to Int64 quantity in hours. @return 小时物理量或错误 / Hours quantity or error */
fun Duration.toQuantityHoursInt64(): Ret<Quantity<Int64>> = toQuantityInt64(Hour)
/** 将 Duration 转换为小时的 IntX 物理量 / Convert Duration to IntX quantity in hours. @return 小时物理量或错误 / Hours quantity or error */
fun Duration.toQuantityHoursIntX(): Ret<Quantity<IntX>> = toQuantityIntX(Hour)
/** 将 Duration 转换为小时的 UInt64 物理量 / Convert Duration to UInt64 quantity in hours. @return 小时物理量或错误 / Hours quantity or error */
fun Duration.toQuantityHoursUInt64(): Ret<Quantity<UInt64>> = toQuantityUInt64(Hour)
/** 将 Duration 转换为小时的 UIntX 物理量 / Convert Duration to UIntX quantity in hours. @return 小时物理量或错误 / Hours quantity or error */
fun Duration.toQuantityHoursUIntX(): Ret<Quantity<UIntX>> = toQuantityUIntX(Hour)

// ============================================================================
// 便捷方法：天 / Convenience methods: Days
// ============================================================================

/** 将 Duration 转换为天的 Flt64 物理量 / Convert Duration to Flt64 quantity in days. @return 天物理量或错误 / Days quantity or error */
fun Duration.toQuantityDaysFlt64(): Ret<Quantity<Flt64>> = toQuantityFlt64(Day)
/** 将 Duration 转换为天的 FltX 物理量 / Convert Duration to FltX quantity in days. @return 天物理量或错误 / Days quantity or error */
fun Duration.toQuantityDaysFltX(): Ret<Quantity<FltX>> = toQuantityFltX(Day)
/** 将 Duration 转换为天的 Int64 物理量 / Convert Duration to Int64 quantity in days. @return 天物理量或错误 / Days quantity or error */
fun Duration.toQuantityDaysInt64(): Ret<Quantity<Int64>> = toQuantityInt64(Day)
/** 将 Duration 转换为天的 IntX 物理量 / Convert Duration to IntX quantity in days. @return 天物理量或错误 / Days quantity or error */
fun Duration.toQuantityDaysIntX(): Ret<Quantity<IntX>> = toQuantityIntX(Day)
/** 将 Duration 转换为天的 UInt64 物理量 / Convert Duration to UInt64 quantity in days. @return 天物理量或错误 / Days quantity or error */
fun Duration.toQuantityDaysUInt64(): Ret<Quantity<UInt64>> = toQuantityUInt64(Day)
/** 将 Duration 转换为天的 UIntX 物理量 / Convert Duration to UIntX quantity in days. @return 天物理量或错误 / Days quantity or error */
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
 * @param V 数值类型 / The number type
 * @param threshold 单位切换阈值，默认为 1000.0 / Unit switching threshold, defaults to 1000.0
 * @return 以最佳单位表示的时间物理量，或错误 / Time quantity in best fit unit, or an error
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
            Ok(r.value * unit)
        }
        is Failed -> Failed(r.error)
        is Fatal -> Fatal(r.errors)
    }
}

/**
 * 将 Duration 转换为 Flt64 最佳单位时间物理量
 * Convert Duration to Flt64 time quantity with best fit unit
 *
 * @param threshold 单位切换阈值，默认为 1000.0 / Unit switching threshold, defaults to 1000.0
 * @return 以最佳单位表示的 Flt64 时间物理量，或错误 / Flt64 time quantity in best fit unit, or an error
 */
fun Duration.toQuantityBestFitFlt64(threshold: Double = 1000.0): Ret<Quantity<Flt64>> {
    return toQuantityBestFit<Flt64>(threshold)
}

/**
 * 将 Duration 转换为 FltX 最佳单位时间物理量
 * Convert Duration to FltX time quantity with best fit unit
 *
 * @param threshold 单位切换阈值，默认为 1000.0 / Unit switching threshold, defaults to 1000.0
 * @return 以最佳单位表示的 FltX 时间物理量，或错误 / FltX time quantity in best fit unit, or an error
 */
fun Duration.toQuantityBestFitFltX(threshold: Double = 1000.0): Ret<Quantity<FltX>> {
    return toQuantityBestFit<FltX>(threshold)
}

/**
 * 将 Duration 转换为 Int64 最佳单位时间物理量
 * Convert Duration to Int64 time quantity with best fit unit
 *
 * @param threshold 单位切换阈值，默认为 1000.0 / Unit switching threshold, defaults to 1000.0
 * @return 以最佳单位表示的 Int64 时间物理量，或错误 / Int64 time quantity in best fit unit, or an error
 */
fun Duration.toQuantityBestFitInt64(threshold: Double = 1000.0): Ret<Quantity<Int64>> {
    return toQuantityBestFit<Int64>(threshold)
}

/**
 * 将 Duration 转换为 UInt64 最佳单位时间物理量
 * Convert Duration to UInt64 time quantity with best fit unit
 *
 * @param threshold 单位切换阈值，默认为 1000.0 / Unit switching threshold, defaults to 1000.0
 * @return 以最佳单位表示的 UInt64 时间物理量，或错误 / UInt64 time quantity in best fit unit, or an error
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

/** Duration 的 FltX 纳秒值作为时间物理量 / Duration value in nanoseconds as FltX time quantity */
val Duration.inNanosecondsQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Nanosecond)
/** Duration 的 FltX 微秒值作为时间物理量 / Duration value in microseconds as FltX time quantity */
val Duration.inMicrosecondsQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Microsecond)
/** Duration 的 FltX 毫秒值作为时间物理量 / Duration value in milliseconds as FltX time quantity */
val Duration.inMillisecondsQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Millisecond)
/** Duration 的 FltX 秒值作为时间物理量 / Duration value in seconds as FltX time quantity */
val Duration.inSecondsQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Second)
/** Duration 的 FltX 分钟值作为时间物理量 / Duration value in minutes as FltX time quantity */
val Duration.inMinutesQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Minute)
/** Duration 的 FltX 小时值作为时间物理量 / Duration value in hours as FltX time quantity */
val Duration.inHoursQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Hour)
/** Duration 的 FltX 天值作为时间物理量 / Duration value in days as FltX time quantity */
val Duration.inDaysQuantityFltX: Ret<Quantity<FltX>> get() = toQuantityFltX(Day)

/** Duration 的 Int64 纳秒值作为时间物理量 / Duration value in nanoseconds as Int64 time quantity */
val Duration.inNanosecondsQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Nanosecond)
/** Duration 的 Int64 微秒值作为时间物理量 / Duration value in microseconds as Int64 time quantity */
val Duration.inMicrosecondsQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Microsecond)
/** Duration 的 Int64 毫秒值作为时间物理量 / Duration value in milliseconds as Int64 time quantity */
val Duration.inMillisecondsQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Millisecond)
/** Duration 的 Int64 秒值作为时间物理量 / Duration value in seconds as Int64 time quantity */
val Duration.inSecondsQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Second)
/** Duration 的 Int64 分钟值作为时间物理量 / Duration value in minutes as Int64 time quantity */
val Duration.inMinutesQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Minute)
/** Duration 的 Int64 小时值作为时间物理量 / Duration value in hours as Int64 time quantity */
val Duration.inHoursQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Hour)
/** Duration 的 Int64 天值作为时间物理量 / Duration value in days as Int64 time quantity */
val Duration.inDaysQuantityInt64: Ret<Quantity<Int64>> get() = toQuantityInt64(Day)

/** Duration 的 UInt64 纳秒值作为时间物理量 / Duration value in nanoseconds as UInt64 time quantity */
val Duration.inNanosecondsQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Nanosecond)
/** Duration 的 UInt64 微秒值作为时间物理量 / Duration value in microseconds as UInt64 time quantity */
val Duration.inMicrosecondsQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Microsecond)
/** Duration 的 UInt64 毫秒值作为时间物理量 / Duration value in milliseconds as UInt64 time quantity */
val Duration.inMillisecondsQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Millisecond)
/** Duration 的 UInt64 秒值作为时间物理量 / Duration value in seconds as UInt64 time quantity */
val Duration.inSecondsQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Second)
/** Duration 的 UInt64 分钟值作为时间物理量 / Duration value in minutes as UInt64 time quantity */
val Duration.inMinutesQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Minute)
/** Duration 的 UInt64 小时值作为时间物理量 / Duration value in hours as UInt64 time quantity */
val Duration.inHoursQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Hour)
/** Duration 的 UInt64 天值作为时间物理量 / Duration value in days as UInt64 time quantity */
val Duration.inDaysQuantityUInt64: Ret<Quantity<UInt64>> get() = toQuantityUInt64(Day)
