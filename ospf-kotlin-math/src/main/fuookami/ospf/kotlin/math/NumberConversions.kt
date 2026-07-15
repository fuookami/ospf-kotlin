/**
 * 数字类型转换
 * Number Type Conversions
 *
 * 提供布尔值和字符串到各种数值类型 (Int8, Int16, Int32, Int64, IntX, UInt8, UInt16, UInt32, UInt64, UIntX, Flt32, Flt64, FltX) 的转换函数。
 * Provides conversion functions from Boolean and String to various numeric types (Int8, Int16, Int32, Int64, IntX, UInt8, UInt16, UInt32, UInt64, UIntX, Flt32, Flt64, FltX).
*/
package fuookami.ospf.kotlin.math

import java.math.BigDecimal
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

/** Boolean 转 Int8 / Convert Boolean to Int8 */
fun Boolean.toInt8() = if (this) Int8.one else Int8.zero

/** String 转 Int8 / Convert String to Int8 */
fun String.toInt8() = Int8(toByte())

/** String 转 Int8，失败返回 null / Convert String to Int8, return null on failure */
fun String.toInt8OrNull() = toByteOrNull()?.let { Int8(it) }

/** String 转 Int16 / Convert String to Int16 */
fun String.toInt16() = Int16(toShort())

/** String 转 Int16，失败返回 null / Convert String to Int16, return null on failure */
fun String.toInt16OrNull() = toShortOrNull()?.let { Int16(it) }

/** String 转 Int32 / Convert String to Int32 */
fun String.toInt32() = Int32(toInt())

/** String 转 Int32，失败返回 null / Convert String to Int32, return null on failure */
fun String.toInt32OrNull() = toIntOrNull()?.let { Int32(it) }

/** String 转 Int64 / Convert String to Int64 */
fun String.toInt64() = Int64(toLong())

/** String 转 Int64，失败返回 null / Convert String to Int64, return null on failure */
fun String.toInt64OrNull() = toLongOrNull()?.let { Int64(it) }

/**
 * String 转 IntX
 * Convert String to IntX
 *
 * @param radix 进制基数，默认为 10 / Radix base, defaults to 10
 * @return 转换后的 IntX 值 / Converted IntX value
*/
fun String.toIntX(radix: Int = 10) = IntX(this, radix)

/**
 * String 转 IntX，失败返回 null
 * Convert String to IntX, return null on failure
 *
 * @param radix 进制基数，默认为 10 / Radix base, defaults to 10
 * @return 转换后的 IntX 值，失败时返回 null / Converted IntX value, or null on failure
*/
fun String.toIntXOrNull(radix: Int = 10) = runCatching { IntX(this, radix) }.getOrNull()

/** Boolean 转 UInt8 / Convert Boolean to UInt8 */
fun Boolean.toUInt8() = if (this) UInt8.one else UInt8.zero

/** Boolean 转 UInt64 / Convert Boolean to UInt64 */
fun Boolean.toUInt64() = if (this) UInt64.one else UInt64.zero

/** String 转 UInt8 / Convert String to UInt8 */
fun String.toUInt8() = UInt8(toUByte())

/** String 转 UInt8，失败返回 null / Convert String to UInt8, return null on failure */
fun String.toUInt8OrNull() = toUByteOrNull()?.let { UInt8(it) }

/** String 转 UInt16 / Convert String to UInt16 */
fun String.toUInt16() = UInt16(toUShort())

/** String 转 UInt16，失败返回 null / Convert String to UInt16, return null on failure */
fun String.toUInt16OrNull() = toUShortOrNull()?.let { UInt16(it) }

/** String 转 UInt32 / Convert String to UInt32 */
fun String.toUInt32() = UInt32(toUInt())

/** String 转 UInt32，失败返回 null / Convert String to UInt32, return null on failure */
fun String.toUInt32OrNull() = toUIntOrNull()?.let { UInt32(it) }

/** String 转 UInt64 / Convert String to UInt64 */
fun String.toUInt64() = UInt64(toULong())

/** String 转 UInt64，失败返回 null / Convert String to UInt64, return null on failure */
fun String.toUInt64OrNull() = toULongOrNull()?.let { UInt64(it) }

/**
 * String 转 UIntX
 * Convert String to UIntX
 *
 * @param radix 进制基数，默认为 10 / Radix base, defaults to 10
 * @return UIntX 转换结果 / UIntX conversion result
*/
fun String.toUIntX(radix: Int = 10) = UIntX.of(this, radix)

/**
 * String 转 UIntX，失败返回 null
 * Convert String to UIntX, return null on failure
 *
 * @param radix 进制基数，默认为 10 / Radix base, defaults to 10
 * @return 转换后的 UIntX 值，失败时返回 null / Converted UIntX value, or null on failure
*/
fun String.toUIntXOrNull(radix: Int = 10) = UIntX.ofOrNull(this, radix)

/** Boolean 转 Flt64 / Convert Boolean to Flt64 */
fun Boolean.toFlt64() = if (this) Flt64.one else Flt64.zero

/** String 转 Flt32 / Convert String to Flt32 */
fun String.toFlt32() = Flt32(toFloat())

/** String 转 Flt32，失败返回 null / Convert String to Flt32, return null on failure */
fun String.toFlt32OrNull() = toFloatOrNull()?.let { Flt32(it) }

/** String 转 Flt64 / Convert String to Flt64 */
fun String.toFlt64() = Flt64(toDouble())

/** String 转 Flt64，失败返回 null / Convert String to Flt64, return null on failure */
fun String.toFlt64OrNull() = toDoubleOrNull()?.let { Flt64(it) }

/** String 转 FltX / Convert String to FltX */
fun String.toFltX() = FltX(toBigDecimal())

/** String 转 FltX，失败返回 null / Convert String to FltX, return null on failure */
fun String.toFltXOrNull() = runCatching { FltX(BigDecimal(this)) }.getOrNull()
