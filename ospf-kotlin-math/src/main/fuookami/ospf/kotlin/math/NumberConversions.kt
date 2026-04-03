package fuookami.ospf.kotlin.math

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import java.math.BigDecimal

fun Boolean.toInt8() = if (this) Int8.one else Int8.zero

fun String.toInt8() = Int8(toByte())
fun String.toInt8OrNull() = toByteOrNull()?.let { Int8(it) }
fun String.toInt16() = Int16(toShort())
fun String.toInt16OrNull() = toShortOrNull()?.let { Int16(it) }
fun String.toInt32() = Int32(toInt())
fun String.toInt32OrNull() = toIntOrNull()?.let { Int32(it) }
fun String.toInt64() = Int64(toLong())
fun String.toInt64OrNull() = toLongOrNull()?.let { Int64(it) }
fun String.toIntX(radix: Int = 10) = IntX(this, radix)
fun String.toIntXOrNull(radix: Int = 10) = runCatching { IntX(this, radix) }.getOrNull()

fun Boolean.toUInt8() = if (this) UInt8.one else UInt8.zero
fun Boolean.toUInt64() = if (this) UInt64.one else UInt64.zero

fun String.toUInt8() = UInt8(toUByte())
fun String.toUInt8OrNull() = toUByteOrNull()?.let { UInt8(it) }
fun String.toUInt16() = UInt16(toUShort())
fun String.toUInt16OrNull() = toUShortOrNull()?.let { UInt16(it) }
fun String.toUInt32() = UInt32(toUInt())
fun String.toUInt32OrNull() = toUIntOrNull()?.let { UInt32(it) }
fun String.toUInt64() = UInt64(toULong())
fun String.toUInt64OrNull() = toULongOrNull()?.let { UInt64(it) }
fun String.toUIntX(radix: Int = 10) = UIntX(this, radix)
fun String.toUIntXOrNull(radix: Int = 10) = runCatching { UIntX(this, radix) }.getOrNull()

fun Boolean.toFlt64() = if (this) Flt64.one else Flt64.zero

fun String.toFlt32() = Flt32(toFloat())
fun String.toFlt32OrNull() = toFloatOrNull()?.let { Flt32(it) }
fun String.toFlt64() = Flt64(toDouble())
fun String.toFlt64OrNull() = toDoubleOrNull()?.let { Flt64(it) }
fun String.toFltX() = FltX(toBigDecimal())
fun String.toFltXOrNull() = runCatching { FltX(BigDecimal(this)) }.getOrNull()




