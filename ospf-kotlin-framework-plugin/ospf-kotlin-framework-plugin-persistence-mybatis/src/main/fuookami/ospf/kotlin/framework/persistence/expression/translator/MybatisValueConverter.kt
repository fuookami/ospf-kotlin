/**
 * MyBatis 值类型转换器
 * MyBatis Value Type Converter
 *
 * 将 OSPF 自定义类型转换为 JDBC 兼容类型。
 * Converts OSPF custom types to JDBC-compatible types.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * MyBatis 值类型转换器
 * MyBatis Value Type Converter
 *
 * 将 OSPF 数学库和 kotlinx.datetime 的自定义类型转换为 JDBC 兼容的标准类型。
 * 在表达式翻译器（ScalarTranslator / BooleanTranslator / UpdateTranslator）传递参数前统一调用，
 * 确保 MyBatis-Plus Wrapper 收到的参数值均为 JDBC 驱动可识别的类型。
 *
 * Converts custom types from the OSPF math library and kotlinx.datetime to standard JDBC-compatible types.
 * Called uniformly before expression translators (ScalarTranslator / BooleanTranslator / UpdateTranslator)
 * pass parameters, ensuring that MyBatis-Plus Wrapper receives only JDBC-driver-recognizable types.
 */
object MybatisValueConverter {

    /**
     * 将任意值转换为 JDBC 兼容类型
     * Convert any value to JDBC-compatible type
     *
     * 支持的转换：
     * - UInt32 → Int
     * - Int32 → Int
     * - UInt64 → Long
     * - Int64 → Long
     * - Flt32 → Float
     * - Flt64 → Double
     * - FltX → BigDecimal
     * - kotlinx.datetime.LocalDateTime → java.time.LocalDateTime
     * - kotlin.time.Duration → ISO-8601 字符串 / String
     * - java.time.ZoneId → String (zone id)
     * - java.time.ZoneOffset → String (offset id)
     * - kotlinx.datetime.TimeZone → String (zone id)
     * - List<Enum<*>> → 逗号分隔字符串 / comma-separated String
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
            is List<*> -> {
                if (value.isNotEmpty() && value.first() is Enum<*>) {
                    value.joinToString(",") { (it as Enum<*>).name }
                } else {
                    value
                }
            }
            else -> value
        }
    }
}
