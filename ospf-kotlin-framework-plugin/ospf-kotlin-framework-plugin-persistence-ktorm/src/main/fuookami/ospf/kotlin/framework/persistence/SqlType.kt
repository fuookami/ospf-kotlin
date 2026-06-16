/**
 * SQL 类型扩展
 * SQL Type Extensions
 *
 * 为 Ktorm 表定义提供自定义数值和日期类型映射。
 * Provides custom numeric and date type mappings for Ktorm table definitions.
 */
package fuookami.ospf.kotlin.framework.persistence

import java.math.*
import java.time.*
import java.time.ZoneOffset
import kotlin.time.*
import kotlin.time.Instant
import kotlin.time.Duration
import kotlinx.datetime.*
import kotlinx.datetime.LocalDateTime
import org.ktorm.schema.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 定义 UInt32 类型列
 * Define UInt32 type column
 *
 * @param name 列名 / Column name
 * @return UInt32 类型的列 / Column of UInt32 type
 */
fun BaseTable<*>.ui32(name: String): Column<UInt32> {
    return int(name).transform({ UInt32(it.toUInt()) }, { it.toInt() })
}

/**
 * 定义 Int32 类型列
 * Define Int32 type column
 *
 * @param name 列名 / Column name
 * @return Int32 类型的列 / Column of Int32 type
 */
fun BaseTable<*>.i32(name: String): Column<Int32> {
    return int(name).transform({ Int32(it) }, { it.toInt() })
}

/**
 * 定义 UInt64 类型列
 * Define UInt64 type column
 *
 * @param name 列名 / Column name
 * @return UInt64 类型的列 / Column of UInt64 type
 */
fun BaseTable<*>.ui64(name: String): Column<UInt64> {
    return long(name).transform({ UInt64(it.toULong()) }, { it.toLong() })
}

/**
 * 定义 Int64 类型列
 * Define Int64 type column
 *
 * @param name 列名 / Column name
 * @return Int64 类型的列 / Column of Int64 type
 */
fun BaseTable<*>.i64(name: String): Column<Int64> {
    return long(name).transform({ Int64(it) }, { it.toLong() })
}

/**
 * 定义 Flt32 类型列
 * Define Flt32 type column
 *
 * @param name 列名 / Column name
 * @return Flt32 类型的列 / Column of Flt32 type
 */
fun BaseTable<*>.f32(name: String): Column<Flt32> {
    return float(name).transform({ Flt32(it) }, { it.toFloat() })
}

/**
 * 定义 Flt64 类型列
 * Define Flt64 type column
 *
 * @param name 列名 / Column name
 * @return Flt64 类型的列 / Column of Flt64 type
 */
fun BaseTable<*>.f64(name: String): Column<Flt64> {
    return double(name).transform({ Flt64(it) }, { it.toDouble() })
}

/**
 * 定义 FltX 类型列（默认精度）
 * Define FltX type column (default precision)
 *
 * @param name 列名 / Column name
 * @param scale 小数精度，默认 2 / Decimal scale, default 2
 * @return FltX 类型的列 / Column of FltX type
 */
fun BaseTable<*>.fltx(name: String, scale: Int = 2): Column<FltX> {
    return decimal(name).transform({ FltX(it).withScale(scale) }, { it.toDecimal() })
}

/**
 * 定义 FltX 类型列（指定舍入模式）
 * Define FltX type column (specified rounding mode)
 *
 * @param name 列名 / Column name
 * @param roundingMode 舍入模式 / Rounding mode
 * @param scale 小数精度，默认 2 / Decimal scale, default 2
 * @return FltX 类型的列 / Column of FltX type
 */
fun BaseTable<*>.fltx(name: String, roundingMode: RoundingMode, scale: Int = 2): Column<FltX> {
    return decimal(name).transform({ FltX(it).withScale(scale, roundingMode) }, { it.toDecimal() })
}

/**
 * 定义 Kotlin LocalDateTime 类型列
 * Define Kotlin LocalDateTime type column
 *
 * @param name 列名 / Column name
 * @return LocalDateTime 类型的列 / Column of LocalDateTime type
 */
fun BaseTable<*>.kotlinDatetime(name: String): Column<LocalDateTime> {
    return datetime(name).transform({ it.toKotlinLocalDateTime() }, { it.toJavaLocalDateTime() })
}

/**
 * 定义 java.time.Instant 类型列（使用 TIMESTAMP 类型存储）
 * Define java.time.Instant type column (stored as TIMESTAMP)
 *
 * @param name 列名 / Column name
 * @return Instant 类型的列 / Column of Instant type
 */
@OptIn(ExperimentalTime::class)
fun BaseTable<*>.instant(name: String): Column<Instant> {
    return timestamp(name).transform({ it.toKotlinInstant() }, { it.toJavaInstant() })
}

/**
 * 定义 kotlin.time.Duration 类型列（以 ISO-8601 字符串存储）
 * Define kotlin.time.Duration type column (stored as ISO-8601 string)
 *
 * @param name 列名 / Column name
 * @return Duration 类型的列 / Column of Duration type
 */
fun BaseTable<*>.duration(name: String): Column<Duration> {
    return varchar(name).transform({ Duration.parseIsoString(it) }, { it.toIsoString() })
}

/**
 * 定义 java.time.ZoneId 类型列（以字符串存储）
 * Define java.time.ZoneId type column (stored as string)
 *
 * @param name 列名 / Column name
 * @return ZoneId 类型的列 / Column of ZoneId type
 */
fun BaseTable<*>.zoneId(name: String): Column<ZoneId> {
    return varchar(name).transform({ ZoneId.of(it) }, { it.id })
}

/**
 * 定义 java.time.ZoneOffset 类型列（以字符串存储）
 * Define java.time.ZoneOffset type column (stored as string)
 *
 * @param name 列名 / Column name
 * @return ZoneOffset 类型的列 / Column of ZoneOffset type
 */
fun BaseTable<*>.zoneOffset(name: String): Column<ZoneOffset> {
    return varchar(name).transform({ ZoneOffset.of(it) }, { it.id })
}

/**
 * 定义 kotlinx.datetime.TimeZone 类型列（以字符串存储）
 * Define kotlinx.datetime.TimeZone type column (stored as string)
 *
 * @param name 列名 / Column name
 * @return TimeZone 类型的列 / Column of TimeZone type
 */
fun BaseTable<*>.kotlinTimeZone(name: String): Column<TimeZone> {
    return varchar(name).transform({ TimeZone.of(it) }, { it.id })
}

/**
 * 定义枚举列表类型列（逗号分隔存储）
 * Define enum list type column (comma-separated storage)
 *
 * @param name 列名 / Column name
 * @param T 枚举类型 / Enum type
 * @return 枚举列表类型的列 / Column of enum list type
 */
inline fun <reified T : Enum<T>> BaseTable<*>.enums(name: String): Column<List<T>> {
    return varchar(name).transform({ it.split(',').map { value -> java.lang.Enum.valueOf(T::class.java, value) } }, { it.joinToString(",") { value -> value.name } })
}
