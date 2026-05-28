@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.math

import kotlin.time.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 时间时长
 * Time Duration
 *
 * 为各种数值类型提供时长转换扩展函数和运算符，支持从纳秒到天的各种时间单位。
 * Provides duration conversion extension functions and operators for various numeric types, supporting time units from nanoseconds to days.
 */

/** Int32 转时长 / Convert Int32 to duration */
fun Int32.toDuration(unit: DurationUnit): Duration {
    return toLong().toDuration(unit)
}

/** UInt32 转时长 / Convert UInt32 to duration */
fun UInt32.toDuration(unit: DurationUnit): Duration {
    return toLong().toDuration(unit)
}

/** Int64 转时长 / Convert Int64 to duration */
fun Int64.toDuration(unit: DurationUnit): Duration {
    return toLong().toDuration(unit)
}

/** UInt64 转时长 / Convert UInt64 to duration */
fun UInt64.toDuration(unit: DurationUnit): Duration {
    return toLong().toDuration(unit)
}

/** IntX 转时长 / Convert IntX to duration */
fun IntX.toDuration(unit: DurationUnit): Duration {
    return toInt64().toDuration(unit)
}

/** UIntX 转时长 / Convert UIntX to duration */
fun UIntX.toDuration(unit: DurationUnit): Duration {
    return toInt64().toDuration(unit)
}

/** Flt32 转时长 / Convert Flt32 to duration */
fun Flt32.toDuration(unit: DurationUnit): Duration {
    return toFlt64().toDuration(unit)
}

/** Flt64 转时长 / Convert Flt64 to duration */
fun Flt64.toDuration(unit: DurationUnit): Duration {
    return toDouble().toDuration(unit)
}

/** FltX 转时长 / Convert FltX to duration */
fun FltX.toDuration(unit: DurationUnit): Duration {
    return toFlt64().toDuration(unit)
}

/** Int32 纳秒 / Int32 nanoseconds */
val Int32.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
/** UInt32 纳秒 / UInt32 nanoseconds */
val UInt32.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
/** Int64 纳秒 / Int64 nanoseconds */
val Int64.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
/** UInt64 纳秒 / UInt64 nanoseconds */
val UInt64.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
/** IntX 纳秒 / IntX nanoseconds */
val IntX.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
/** UIntX 纳秒 / UIntX nanoseconds */
val UIntX.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
/** Flt32 纳秒 / Flt32 nanoseconds */
val Flt32.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
/** Flt64 纳秒 / Flt64 nanoseconds */
val Flt64.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
/** FltX 纳秒 / FltX nanoseconds */
val FltX.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

/** Int32 微秒 / Int32 microseconds */
val Int32.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
/** UInt32 微秒 / UInt32 microseconds */
val UInt32.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
/** Int64 微秒 / Int64 microseconds */
val Int64.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
/** UInt64 微秒 / UInt64 microseconds */
val UInt64.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
/** IntX 微秒 / IntX microseconds */
val IntX.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
/** UIntX 微秒 / UIntX microseconds */
val UIntX.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
/** Flt32 微秒 / Flt32 microseconds */
val Flt32.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
/** Flt64 微秒 / Flt64 microseconds */
val Flt64.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
/** FltX 微秒 / FltX microseconds */
val FltX.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

/** Int32 毫秒 / Int32 milliseconds */
val Int32.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
/** UInt32 毫秒 / UInt32 milliseconds */
val UInt32.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
/** Int64 毫秒 / Int64 milliseconds */
val Int64.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
/** UInt64 毫秒 / UInt64 milliseconds */
val UInt64.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
/** IntX 毫秒 / IntX milliseconds */
val IntX.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
/** UIntX 毫秒 / UIntX milliseconds */
val UIntX.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
/** Flt32 毫秒 / Flt32 milliseconds */
val Flt32.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
/** Flt64 毫秒 / Flt64 milliseconds */
val Flt64.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
/** FltX 毫秒 / FltX milliseconds */
val FltX.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

/** Int32 秒 / Int32 seconds */
val Int32.seconds get() = toDuration(DurationUnit.SECONDS)
/** UInt32 秒 / UInt32 seconds */
val UInt32.seconds get() = toDuration(DurationUnit.SECONDS)
/** Int64 秒 / Int64 seconds */
val Int64.seconds get() = toDuration(DurationUnit.SECONDS)
/** UInt64 秒 / UInt64 seconds */
val UInt64.seconds get() = toDuration(DurationUnit.SECONDS)
/** IntX 秒 / IntX seconds */
val IntX.seconds get() = toDuration(DurationUnit.SECONDS)
/** UIntX 秒 / UIntX seconds */
val UIntX.seconds get() = toDuration(DurationUnit.SECONDS)
/** Flt32 秒 / Flt32 seconds */
val Flt32.seconds get() = toDuration(DurationUnit.SECONDS)
/** Flt64 秒 / Flt64 seconds */
val Flt64.seconds get() = toDuration(DurationUnit.SECONDS)
/** FltX 秒 / FltX seconds */
val FltX.seconds get() = toDuration(DurationUnit.SECONDS)

/** Int32 分钟 / Int32 minutes */
val Int32.minutes get() = toDuration(DurationUnit.MINUTES)
/** UInt32 分钟 / UInt32 minutes */
val UInt32.minutes get() = toDuration(DurationUnit.MINUTES)
/** Int64 分钟 / Int64 minutes */
val Int64.minutes get() = toDuration(DurationUnit.MINUTES)
/** UInt64 分钟 / UInt64 minutes */
val UInt64.minutes get() = toDuration(DurationUnit.MINUTES)
/** IntX 分钟 / IntX minutes */
val IntX.minutes get() = toDuration(DurationUnit.MINUTES)
/** UIntX 分钟 / UIntX minutes */
val UIntX.minutes get() = toDuration(DurationUnit.MINUTES)
/** Flt32 分钟 / Flt32 minutes */
val Flt32.minutes get() = toDuration(DurationUnit.MINUTES)
/** Flt64 分钟 / Flt64 minutes */
val Flt64.minutes get() = toDuration(DurationUnit.MINUTES)
/** FltX 分钟 / FltX minutes */
val FltX.minutes get() = toDuration(DurationUnit.MINUTES)

/** Int32 小时 / Int32 hours */
val Int32.hours get() = toDuration(DurationUnit.HOURS)
/** UInt32 小时 / UInt32 hours */
val UInt32.hours get() = toDuration(DurationUnit.HOURS)
/** Int64 小时 / Int64 hours */
val Int64.hours get() = toDuration(DurationUnit.HOURS)
/** UInt64 小时 / UInt64 hours */
val UInt64.hours get() = toDuration(DurationUnit.HOURS)
/** IntX 小时 / IntX hours */
val IntX.hours get() = toDuration(DurationUnit.HOURS)
/** UIntX 小时 / UIntX hours */
val UIntX.hours get() = toDuration(DurationUnit.HOURS)
/** Flt32 小时 / Flt32 hours */
val Flt32.hours get() = toDuration(DurationUnit.HOURS)
/** Flt64 小时 / Flt64 hours */
val Flt64.hours get() = toDuration(DurationUnit.HOURS)

/** Int32 天 / Int32 days */
val Int32.days get() = toDuration(DurationUnit.DAYS)
/** UInt32 天 / UInt32 days */
val UInt32.days get() = toDuration(DurationUnit.DAYS)
/** Int64 天 / Int64 days */
val Int64.days get() = toDuration(DurationUnit.DAYS)
/** UInt64 天 / UInt64 days */
val UInt64.days get() = toDuration(DurationUnit.DAYS)
/** IntX 天 / IntX days */
val IntX.days get() = toDuration(DurationUnit.DAYS)
/** UIntX 天 / UIntX days */
val UIntX.days get() = toDuration(DurationUnit.DAYS)
/** Flt32 天 / Flt32 days */
val Flt32.days get() = toDuration(DurationUnit.DAYS)
/** Flt64 天 / Flt64 days */
val Flt64.days get() = toDuration(DurationUnit.DAYS)
/** FltX 天 / FltX days */
val FltX.days get() = toDuration(DurationUnit.DAYS)

/** 从 Int32 创建纳秒时长 / Create nanoseconds duration from Int32 */
fun Duration.Companion.nanoseconds(value: Int32) = value.toDuration(DurationUnit.NANOSECONDS)
/** 从 UInt32 创建纳秒时长 / Create nanoseconds duration from UInt32 */
fun Duration.Companion.nanoseconds(value: UInt32) = value.toDuration(DurationUnit.NANOSECONDS)
/** 从 Int64 创建纳秒时长 / Create nanoseconds duration from Int64 */
fun Duration.Companion.nanoseconds(value: Int64) = value.toDuration(DurationUnit.NANOSECONDS)
/** 从 UInt64 创建纳秒时长 / Create nanoseconds duration from UInt64 */
fun Duration.Companion.nanoseconds(value: UInt64) = value.toDuration(DurationUnit.NANOSECONDS)
/** 从 IntX 创建纳秒时长 / Create nanoseconds duration from IntX */
fun Duration.Companion.nanoseconds(value: IntX) = value.toDuration(DurationUnit.NANOSECONDS)
/** 从 UIntX 创建纳秒时长 / Create nanoseconds duration from UIntX */
fun Duration.Companion.nanoseconds(value: UIntX) = value.toDuration(DurationUnit.NANOSECONDS)
/** 从 Flt32 创建纳秒时长 / Create nanoseconds duration from Flt32 */
fun Duration.Companion.nanoseconds(value: Flt32) = value.toDuration(DurationUnit.NANOSECONDS)
/** 从 Flt64 创建纳秒时长 / Create nanoseconds duration from Flt64 */
fun Duration.Companion.nanoseconds(value: Flt64) = value.toDuration(DurationUnit.NANOSECONDS)
/** 从 FltX 创建纳秒时长 / Create nanoseconds duration from FltX */
fun Duration.Companion.nanoseconds(value: FltX) = value.toDuration(DurationUnit.NANOSECONDS)

/** 从 Int32 创建微秒时长 / Create microseconds duration from Int32 */
fun Duration.Companion.microseconds(value: Int32) = value.toDuration(DurationUnit.MICROSECONDS)
/** 从 UInt32 创建微秒时长 / Create microseconds duration from UInt32 */
fun Duration.Companion.microseconds(value: UInt32) = value.toDuration(DurationUnit.MICROSECONDS)
/** 从 Int64 创建微秒时长 / Create microseconds duration from Int64 */
fun Duration.Companion.microseconds(value: Int64) = value.toDuration(DurationUnit.MICROSECONDS)
/** 从 UInt64 创建微秒时长 / Create microseconds duration from UInt64 */
fun Duration.Companion.microseconds(value: UInt64) = value.toDuration(DurationUnit.MICROSECONDS)
/** 从 IntX 创建微秒时长 / Create microseconds duration from IntX */
fun Duration.Companion.microseconds(value: IntX) = value.toDuration(DurationUnit.MICROSECONDS)
/** 从 UIntX 创建微秒时长 / Create microseconds duration from UIntX */
fun Duration.Companion.microseconds(value: UIntX) = value.toDuration(DurationUnit.MICROSECONDS)
/** 从 Flt32 创建微秒时长 / Create microseconds duration from Flt32 */
fun Duration.Companion.microseconds(value: Flt32) = value.toDuration(DurationUnit.MICROSECONDS)
/** 从 Flt64 创建微秒时长 / Create microseconds duration from Flt64 */
fun Duration.Companion.microseconds(value: Flt64) = value.toDuration(DurationUnit.MICROSECONDS)
/** 从 FltX 创建微秒时长 / Create microseconds duration from FltX */
fun Duration.Companion.microseconds(value: FltX) = value.toDuration(DurationUnit.MICROSECONDS)

/** 从 Int32 创建毫秒时长 / Create milliseconds duration from Int32 */
fun Duration.Companion.milliseconds(value: Int32) = value.toDuration(DurationUnit.MILLISECONDS)
/** 从 UInt32 创建毫秒时长 / Create milliseconds duration from UInt32 */
fun Duration.Companion.milliseconds(value: UInt32) = value.toDuration(DurationUnit.MILLISECONDS)
/** 从 Int64 创建毫秒时长 / Create milliseconds duration from Int64 */
fun Duration.Companion.milliseconds(value: Int64) = value.toDuration(DurationUnit.MILLISECONDS)
/** 从 UInt64 创建毫秒时长 / Create milliseconds duration from UInt64 */
fun Duration.Companion.milliseconds(value: UInt64) = value.toDuration(DurationUnit.MILLISECONDS)
/** 从 IntX 创建毫秒时长 / Create milliseconds duration from IntX */
fun Duration.Companion.milliseconds(value: IntX) = value.toDuration(DurationUnit.MILLISECONDS)
/** 从 UIntX 创建毫秒时长 / Create milliseconds duration from UIntX */
fun Duration.Companion.milliseconds(value: UIntX) = value.toDuration(DurationUnit.MILLISECONDS)
/** 从 Flt32 创建毫秒时长 / Create milliseconds duration from Flt32 */
fun Duration.Companion.milliseconds(value: Flt32) = value.toDuration(DurationUnit.MILLISECONDS)
/** 从 Flt64 创建毫秒时长 / Create milliseconds duration from Flt64 */
fun Duration.Companion.milliseconds(value: Flt64) = value.toDuration(DurationUnit.MILLISECONDS)
/** 从 FltX 创建毫秒时长 / Create milliseconds duration from FltX */
fun Duration.Companion.milliseconds(value: FltX) = value.toDuration(DurationUnit.MILLISECONDS)

/** 从 Int32 创建秒时长 / Create seconds duration from Int32 */
fun Duration.Companion.seconds(value: Int32) = value.toDuration(DurationUnit.SECONDS)
/** 从 UInt32 创建秒时长 / Create seconds duration from UInt32 */
fun Duration.Companion.seconds(value: UInt32) = value.toDuration(DurationUnit.SECONDS)
/** 从 Int64 创建秒时长 / Create seconds duration from Int64 */
fun Duration.Companion.seconds(value: Int64) = value.toDuration(DurationUnit.SECONDS)
/** 从 UInt64 创建秒时长 / Create seconds duration from UInt64 */
fun Duration.Companion.seconds(value: UInt64) = value.toDuration(DurationUnit.SECONDS)
/** 从 IntX 创建秒时长 / Create seconds duration from IntX */
fun Duration.Companion.seconds(value: IntX) = value.toDuration(DurationUnit.SECONDS)
/** 从 UIntX 创建秒时长 / Create seconds duration from UIntX */
fun Duration.Companion.seconds(value: UIntX) = value.toDuration(DurationUnit.SECONDS)
/** 从 Flt32 创建秒时长 / Create seconds duration from Flt32 */
fun Duration.Companion.seconds(value: Flt32) = value.toDuration(DurationUnit.SECONDS)
/** 从 Flt64 创建秒时长 / Create seconds duration from Flt64 */
fun Duration.Companion.seconds(value: Flt64) = value.toDuration(DurationUnit.SECONDS)
/** 从 FltX 创建秒时长 / Create seconds duration from FltX */
fun Duration.Companion.seconds(value: FltX) = value.toDuration(DurationUnit.SECONDS)

/** 从 Int32 创建分钟时长 / Create minutes duration from Int32 */
fun Duration.Companion.minutes(value: Int32) = value.toDuration(DurationUnit.MINUTES)
/** 从 UInt32 创建分钟时长 / Create minutes duration from UInt32 */
fun Duration.Companion.minutes(value: UInt32) = value.toDuration(DurationUnit.MINUTES)
/** 从 Int64 创建分钟时长 / Create minutes duration from Int64 */
fun Duration.Companion.minutes(value: Int64) = value.toDuration(DurationUnit.MINUTES)
/** 从 UInt64 创建分钟时长 / Create minutes duration from UInt64 */
fun Duration.Companion.minutes(value: UInt64) = value.toDuration(DurationUnit.MINUTES)
/** 从 IntX 创建分钟时长 / Create minutes duration from IntX */
fun Duration.Companion.minutes(value: IntX) = value.toDuration(DurationUnit.MINUTES)
/** 从 UIntX 创建分钟时长 / Create minutes duration from UIntX */
fun Duration.Companion.minutes(value: UIntX) = value.toDuration(DurationUnit.MINUTES)
/** 从 Flt32 创建分钟时长 / Create minutes duration from Flt32 */
fun Duration.Companion.minutes(value: Flt32) = value.toDuration(DurationUnit.MINUTES)
/** 从 Flt64 创建分钟时长 / Create minutes duration from Flt64 */
fun Duration.Companion.minutes(value: Flt64) = value.toDuration(DurationUnit.MINUTES)
/** 从 FltX 创建分钟时长 / Create minutes duration from FltX */
fun Duration.Companion.minutes(value: FltX) = value.toDuration(DurationUnit.MINUTES)

/** 从 Int32 创建小时时长 / Create hours duration from Int32 */
fun Duration.Companion.hours(value: Int32) = value.toDuration(DurationUnit.HOURS)
/** 从 UInt32 创建小时时长 / Create hours duration from UInt32 */
fun Duration.Companion.hours(value: UInt32) = value.toDuration(DurationUnit.HOURS)
/** 从 Int64 创建小时时长 / Create hours duration from Int64 */
fun Duration.Companion.hours(value: Int64) = value.toDuration(DurationUnit.HOURS)
/** 从 UInt64 创建小时时长 / Create hours duration from UInt64 */
fun Duration.Companion.hours(value: UInt64) = value.toDuration(DurationUnit.HOURS)
/** 从 IntX 创建小时时长 / Create hours duration from IntX */
fun Duration.Companion.hours(value: IntX) = value.toDuration(DurationUnit.HOURS)
/** 从 UIntX 创建小时时长 / Create hours duration from UIntX */
fun Duration.Companion.hours(value: UIntX) = value.toDuration(DurationUnit.HOURS)
/** 从 Flt32 创建小时时长 / Create hours duration from Flt32 */
fun Duration.Companion.hours(value: Flt32) = value.toDuration(DurationUnit.HOURS)
/** 从 Flt64 创建小时时长 / Create hours duration from Flt64 */
fun Duration.Companion.hours(value: Flt64) = value.toDuration(DurationUnit.HOURS)
/** 从 FltX 创建小时时长 / Create hours duration from FltX */
fun Duration.Companion.hours(value: FltX) = value.toDuration(DurationUnit.HOURS)

/** 从 Int32 创建天时长 / Create days duration from Int32 */
fun Duration.Companion.days(value: Int32) = value.toDuration(DurationUnit.DAYS)
/** 从 UInt32 创建天时长 / Create days duration from UInt32 */
fun Duration.Companion.days(value: UInt32) = value.toDuration(DurationUnit.DAYS)
/** 从 Int64 创建天时长 / Create days duration from Int64 */
fun Duration.Companion.days(value: Int64) = value.toDuration(DurationUnit.DAYS)
/** 从 UInt64 创建天时长 / Create days duration from UInt64 */
fun Duration.Companion.days(value: UInt64) = value.toDuration(DurationUnit.DAYS)
/** 从 IntX 创建天时长 / Create days duration from IntX */
fun Duration.Companion.days(value: IntX) = value.toDuration(DurationUnit.DAYS)
/** 从 UIntX 创建天时长 / Create days duration from UIntX */
fun Duration.Companion.days(value: UIntX) = value.toDuration(DurationUnit.DAYS)
/** 从 Flt32 创建天时长 / Create days duration from Flt32 */
fun Duration.Companion.days(value: Flt32) = value.toDuration(DurationUnit.DAYS)
/** 从 Flt64 创建天时长 / Create days duration from Flt64 */
fun Duration.Companion.days(value: Flt64) = value.toDuration(DurationUnit.DAYS)
/** 从 FltX 创建天时长 / Create days duration from FltX */
fun Duration.Companion.days(value: FltX) = value.toDuration(DurationUnit.DAYS)

/** Int32 乘以时长 / Int32 times duration */
operator fun Int32.times(duration: Duration) = this.toFlt64().times(duration)
/** UInt32 乘以时长 / UInt32 times duration */
operator fun UInt32.times(duration: Duration) = this.toFlt64().times(duration)
/** Int64 乘以时长 / Int64 times duration */
operator fun Int64.times(duration: Duration) = this.toFlt64().times(duration)
/** UInt64 乘以时长 / UInt64 times duration */
operator fun UInt64.times(duration: Duration) = this.toFlt64().times(duration)
/** IntX 乘以时长 / IntX times duration */
operator fun IntX.times(duration: Duration) = this.toFlt64().times(duration)
/** UIntX 乘以时长 / UIntX times duration */
operator fun UIntX.times(duration: Duration) = this.toFlt64().times(duration)
/** Flt32 乘以时长 / Flt32 times duration */
operator fun Flt32.times(duration: Duration) = this.toFlt64().times(duration)
/** Flt64 乘以时长 / Flt64 times duration */
operator fun Flt64.times(duration: Duration) = this.toDouble().times(duration)
/** FltX 乘以时长 / FltX times duration */
operator fun FltX.times(duration: Duration) = this.toFlt64().times(duration)

/** 时长除以 Int32 / Duration div Int32 */
operator fun Duration.div(value: Int32) = this.div(value.toFlt64())
/** 时长除以 UInt32 / Duration div UInt32 */
operator fun Duration.div(value: UInt32) = this.div(value.toFlt64())
/** 时长除以 Int64 / Duration div Int64 */
operator fun Duration.div(value: Int64) = this.div(value.toFlt64())
/** 时长除以 UInt64 / Duration div UInt64 */
operator fun Duration.div(value: UInt64) = this.div(value.toFlt64())
/** 时长除以 IntX / Duration div IntX */
operator fun Duration.div(value: IntX) = this.div(value.toFlt64())
/** 时长除以 UIntX / Duration div UIntX */
operator fun Duration.div(value: UIntX) = this.div(value.toFlt64())
/** 时长除以 Flt32 / Duration div Flt32 */
operator fun Duration.div(value: Flt32) = this.div(value.toFlt64())
/** 时长除以 Flt64 / Duration div Flt64 */
operator fun Duration.div(value: Flt64) = this.div(value.toDouble())
/** 时长除以 FltX / Duration div FltX */
operator fun Duration.div(value: FltX) = this.div(value.toFlt64())
