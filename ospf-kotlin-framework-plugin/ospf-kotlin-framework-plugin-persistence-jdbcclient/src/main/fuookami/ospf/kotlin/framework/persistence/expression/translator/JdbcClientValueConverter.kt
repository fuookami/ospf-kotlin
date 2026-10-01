/**
 * JdbcClient 值类型转换器 / JdbcClient value type converter
 *
 * 将 OSPF 数值、时间和枚举转换为常见 JDBC 驱动可绑定的 JVM 类型。
 * Converts OSPF numbers, temporal values, and enums to JVM types accepted by common JDBC drivers.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import fuookami.ospf.kotlin.math.algebra.number.*

/** 将 OSPF 自定义值转换为 JDBC 兼容值 / Convert OSPF custom values to JDBC-compatible values */
object JdbcClientValueConverter {
    /**
     * 转换单个绑定值 / Convert one bind value
     *
     * @param value 原始值 / Original value
     * @return JDBC 兼容值 / JDBC-compatible value
     */
    fun convert(value: Any?): Any? {
        if (value == null) return null
        return when (value) {
            is UInt32 -> value.toInt()
            is Int32 -> value.toInt()
            is UInt64 -> value.toLong()
            is Int64 -> value.toLong()
            is Flt32 -> value.toFloat()
            is Flt64 -> value.toDouble()
            is FltX -> value.toDecimal()
            is LocalDateTime -> value.toJavaLocalDateTime()
            is Duration -> value.toIsoString()
            is ZoneId -> value.id
            is ZoneOffset -> value.id
            is TimeZone -> value.id
            is Enum<*> -> value.name
            else -> value
        }
    }
}
