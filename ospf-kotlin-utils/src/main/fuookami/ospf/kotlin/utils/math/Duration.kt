package fuookami.ospf.kotlin.utils.math

import kotlin.time.*

fun Int32.toDuration(unit: DurationUnit): Duration {
    return toLong().toDuration(unit)
}

fun UInt32.toDuration(unit: DurationUnit): Duration {
    return toLong().toDuration(unit)
}

fun Int64.toDuration(unit: DurationUnit): Duration {
    return toLong().toDuration(unit)
}

fun UInt64.toDuration(unit: DurationUnit): Duration {
    return toLong().toDuration(unit)
}

fun IntX.toDuration(unit: DurationUnit): Duration {
    return toInt64().toDuration(unit)
}

fun UIntX.toDuration(unit: DurationUnit): Duration {
    return toInt64().toDuration(unit)
}

fun Flt32.toDuration(unit: DurationUnit): Duration {
    return toFlt64().toDuration(unit)
}

fun Flt64.toDuration(unit: DurationUnit): Duration {
    return toDouble().toDuration(unit)
}

fun FltX.toDuration(unit: DurationUnit): Duration {
    return toFlt64().toDuration(unit)
}

val Int32.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
val UInt32.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
val Int64.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
val UInt64.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
val IntX.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
val UIntX.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
val Flt32.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
val Flt64.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
val FltX.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

val Int32.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
val UInt32.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
val Int64.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
val UInt64.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
val IntX.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
val UIntX.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
val Flt32.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
val Flt64.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
val FltX.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

val Int32.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
val UInt32.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
val Int64.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
val UInt64.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
val IntX.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
val UIntX.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
val Flt32.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
val Flt64.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
val FltX.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

val Int32.seconds get() = toDuration(DurationUnit.SECONDS)
val UInt32.seconds get() = toDuration(DurationUnit.SECONDS)
val Int64.seconds get() = toDuration(DurationUnit.SECONDS)
val UInt64.seconds get() = toDuration(DurationUnit.SECONDS)
val IntX.seconds get() = toDuration(DurationUnit.SECONDS)
val UIntX.seconds get() = toDuration(DurationUnit.SECONDS)
val Flt32.seconds get() = toDuration(DurationUnit.SECONDS)
val Flt64.seconds get() = toDuration(DurationUnit.SECONDS)
val FltX.seconds get() = toDuration(DurationUnit.SECONDS)

val Int32.minutes get() = toDuration(DurationUnit.MINUTES)
val UInt32.minutes get() = toDuration(DurationUnit.MINUTES)
val Int64.minutes get() = toDuration(DurationUnit.MINUTES)
val UInt64.minutes get() = toDuration(DurationUnit.MINUTES)
val IntX.minutes get() = toDuration(DurationUnit.MINUTES)
val UIntX.minutes get() = toDuration(DurationUnit.MINUTES)
val Flt32.minutes get() = toDuration(DurationUnit.MINUTES)
val Flt64.minutes get() = toDuration(DurationUnit.MINUTES)
val FltX.minutes get() = toDuration(DurationUnit.MINUTES)

val Int32.hours get() = toDuration(DurationUnit.HOURS)
val UInt32.hours get() = toDuration(DurationUnit.HOURS)
val Int64.hours get() = toDuration(DurationUnit.HOURS)
val UInt64.hours get() = toDuration(DurationUnit.HOURS)
val IntX.hours get() = toDuration(DurationUnit.HOURS)
val UIntX.hours get() = toDuration(DurationUnit.HOURS)
val Flt32.hours get() = toDuration(DurationUnit.HOURS)
val Flt64.hours get() = toDuration(DurationUnit.HOURS)

val Int32.days get() = toDuration(DurationUnit.DAYS)
val UInt32.days get() = toDuration(DurationUnit.DAYS)
val Int64.days get() = toDuration(DurationUnit.DAYS)
val UInt64.days get() = toDuration(DurationUnit.DAYS)
val IntX.days get() = toDuration(DurationUnit.DAYS)
val UIntX.days get() = toDuration(DurationUnit.DAYS)
val Flt32.days get() = toDuration(DurationUnit.DAYS)
val Flt64.days get() = toDuration(DurationUnit.DAYS)
val FltX.days get() = toDuration(DurationUnit.DAYS)

fun Duration.Companion.nanoseconds(value: Int32) = value.toDuration(DurationUnit.NANOSECONDS)
fun Duration.Companion.nanoseconds(value: UInt32) = value.toDuration(DurationUnit.NANOSECONDS)
fun Duration.Companion.nanoseconds(value: Int64) = value.toDuration(DurationUnit.NANOSECONDS)
fun Duration.Companion.nanoseconds(value: UInt64) = value.toDuration(DurationUnit.NANOSECONDS)
fun Duration.Companion.nanoseconds(value: IntX) = value.toDuration(DurationUnit.NANOSECONDS)
fun Duration.Companion.nanoseconds(value: UIntX) = value.toDuration(DurationUnit.NANOSECONDS)
fun Duration.Companion.nanoseconds(value: Flt32) = value.toDuration(DurationUnit.NANOSECONDS)
fun Duration.Companion.nanoseconds(value: Flt64) = value.toDuration(DurationUnit.NANOSECONDS)
fun Duration.Companion.nanoseconds(value: FltX) = value.toDuration(DurationUnit.NANOSECONDS)

fun Duration.Companion.microseconds(value: Int32) = value.toDuration(DurationUnit.MICROSECONDS)
fun Duration.Companion.microseconds(value: UInt32) = value.toDuration(DurationUnit.MICROSECONDS)
fun Duration.Companion.microseconds(value: Int64) = value.toDuration(DurationUnit.MICROSECONDS)
fun Duration.Companion.microseconds(value: UInt64) = value.toDuration(DurationUnit.MICROSECONDS)
fun Duration.Companion.microseconds(value: IntX) = value.toDuration(DurationUnit.MICROSECONDS)
fun Duration.Companion.microseconds(value: UIntX) = value.toDuration(DurationUnit.MICROSECONDS)
fun Duration.Companion.microseconds(value: Flt32) = value.toDuration(DurationUnit.MICROSECONDS)
fun Duration.Companion.microseconds(value: Flt64) = value.toDuration(DurationUnit.MICROSECONDS)
fun Duration.Companion.microseconds(value: FltX) = value.toDuration(DurationUnit.MICROSECONDS)

fun Duration.Companion.milliseconds(value: Int32) = value.toDuration(DurationUnit.MILLISECONDS)
fun Duration.Companion.milliseconds(value: UInt32) = value.toDuration(DurationUnit.MILLISECONDS)
fun Duration.Companion.milliseconds(value: Int64) = value.toDuration(DurationUnit.MILLISECONDS)
fun Duration.Companion.milliseconds(value: UInt64) = value.toDuration(DurationUnit.MILLISECONDS)
fun Duration.Companion.milliseconds(value: IntX) = value.toDuration(DurationUnit.MILLISECONDS)
fun Duration.Companion.milliseconds(value: UIntX) = value.toDuration(DurationUnit.MILLISECONDS)
fun Duration.Companion.milliseconds(value: Flt32) = value.toDuration(DurationUnit.MILLISECONDS)
fun Duration.Companion.milliseconds(value: Flt64) = value.toDuration(DurationUnit.MILLISECONDS)
fun Duration.Companion.milliseconds(value: FltX) = value.toDuration(DurationUnit.MILLISECONDS)

fun Duration.Companion.seconds(value: Int32) = value.toDuration(DurationUnit.SECONDS)
fun Duration.Companion.seconds(value: UInt32) = value.toDuration(DurationUnit.SECONDS)
fun Duration.Companion.seconds(value: Int64) = value.toDuration(DurationUnit.SECONDS)
fun Duration.Companion.seconds(value: UInt64) = value.toDuration(DurationUnit.SECONDS)
fun Duration.Companion.seconds(value: IntX) = value.toDuration(DurationUnit.SECONDS)
fun Duration.Companion.seconds(value: UIntX) = value.toDuration(DurationUnit.SECONDS)
fun Duration.Companion.seconds(value: Flt32) = value.toDuration(DurationUnit.SECONDS)
fun Duration.Companion.seconds(value: Flt64) = value.toDuration(DurationUnit.SECONDS)
fun Duration.Companion.seconds(value: FltX) = value.toDuration(DurationUnit.SECONDS)

fun Duration.Companion.minutes(value: Int32) = value.toDuration(DurationUnit.MINUTES)
fun Duration.Companion.minutes(value: UInt32) = value.toDuration(DurationUnit.MINUTES)
fun Duration.Companion.minutes(value: Int64) = value.toDuration(DurationUnit.MINUTES)
fun Duration.Companion.minutes(value: UInt64) = value.toDuration(DurationUnit.MINUTES)
fun Duration.Companion.minutes(value: IntX) = value.toDuration(DurationUnit.MINUTES)
fun Duration.Companion.minutes(value: UIntX) = value.toDuration(DurationUnit.MINUTES)
fun Duration.Companion.minutes(value: Flt32) = value.toDuration(DurationUnit.MINUTES)
fun Duration.Companion.minutes(value: Flt64) = value.toDuration(DurationUnit.MINUTES)
fun Duration.Companion.minutes(value: FltX) = value.toDuration(DurationUnit.MINUTES)

fun Duration.Companion.hours(value: Int32) = value.toDuration(DurationUnit.HOURS)
fun Duration.Companion.hours(value: UInt32) = value.toDuration(DurationUnit.HOURS)
fun Duration.Companion.hours(value: Int64) = value.toDuration(DurationUnit.HOURS)
fun Duration.Companion.hours(value: UInt64) = value.toDuration(DurationUnit.HOURS)
fun Duration.Companion.hours(value: IntX) = value.toDuration(DurationUnit.HOURS)
fun Duration.Companion.hours(value: UIntX) = value.toDuration(DurationUnit.HOURS)
fun Duration.Companion.hours(value: Flt32) = value.toDuration(DurationUnit.HOURS)
fun Duration.Companion.hours(value: Flt64) = value.toDuration(DurationUnit.HOURS)
fun Duration.Companion.hours(value: FltX) = value.toDuration(DurationUnit.HOURS)

fun Duration.Companion.days(value: Int32) = value.toDuration(DurationUnit.DAYS)
fun Duration.Companion.days(value: UInt32) = value.toDuration(DurationUnit.DAYS)
fun Duration.Companion.days(value: Int64) = value.toDuration(DurationUnit.DAYS)
fun Duration.Companion.days(value: UInt64) = value.toDuration(DurationUnit.DAYS)
fun Duration.Companion.days(value: IntX) = value.toDuration(DurationUnit.DAYS)
fun Duration.Companion.days(value: UIntX) = value.toDuration(DurationUnit.DAYS)
fun Duration.Companion.days(value: Flt32) = value.toDuration(DurationUnit.DAYS)
fun Duration.Companion.days(value: Flt64) = value.toDuration(DurationUnit.DAYS)
fun Duration.Companion.days(value: FltX) = value.toDuration(DurationUnit.DAYS)

operator fun Int32.times(duration: Duration) = this.toFlt64().times(duration)
operator fun UInt32.times(duration: Duration) = this.toFlt64().times(duration)
operator fun Int64.times(duration: Duration) = this.toFlt64().times(duration)
operator fun UInt64.times(duration: Duration) = this.toFlt64().times(duration)
operator fun IntX.times(duration: Duration) = this.toFlt64().times(duration)
operator fun UIntX.times(duration: Duration) = this.toFlt64().times(duration)
operator fun Flt32.times(duration: Duration) = this.toFlt64().times(duration)
operator fun Flt64.times(duration: Duration) = this.toDouble().times(duration)
operator fun FltX.times(duration: Duration) = this.toFlt64().times(duration)

operator fun Duration.div(value: Int32) = this.div(value.toFlt64())
operator fun Duration.div(value: UInt32) = this.div(value.toFlt64())
operator fun Duration.div(value: Int64) = this.div(value.toFlt64())
operator fun Duration.div(value: UInt64) = this.div(value.toFlt64())
operator fun Duration.div(value: IntX) = this.div(value.toFlt64())
operator fun Duration.div(value: UIntX) = this.div(value.toFlt64())
operator fun Duration.div(value: Flt32) = this.div(value.toFlt64())
operator fun Duration.div(value: Flt64) = this.div(value.toDouble())
operator fun Duration.div(value: FltX) = this.div(value.toFlt64())
