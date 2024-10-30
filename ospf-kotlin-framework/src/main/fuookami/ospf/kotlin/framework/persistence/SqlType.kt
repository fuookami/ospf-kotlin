package fuookami.ospf.kotlin.framework.persistence

import java.math.*
import kotlinx.datetime.*
import org.ktorm.schema.*
import fuookami.ospf.kotlin.utils.math.*

fun BaseTable<*>.ui32(name: String): Column<UInt32> {
    return int(name).transform({ UInt32(it.toUInt()) }, { it.toInt() })
}

fun BaseTable<*>.i32(name: String): Column<Int32> {
    return int(name).transform({ Int32(it) }, { it.toInt() })
}

fun BaseTable<*>.ui64(name: String): Column<UInt64> {
    return long(name).transform({ UInt64(it.toULong()) }, { it.toLong() })
}

fun BaseTable<*>.i64(name: String): Column<Int64> {
    return long(name).transform({ Int64(it) }, { it.toLong() })
}

fun BaseTable<*>.f32(name: String): Column<Flt32> {
    return float(name).transform({ Flt32(it) }, { it.toFloat() })
}

fun BaseTable<*>.f64(name: String): Column<Flt64> {
    return double(name).transform({ Flt64(it) }, { it.toDouble() })
}

fun BaseTable<*>.fltx(name: String, scale: Int = 2): Column<FltX> {
    return decimal(name).transform({ FltX(it).withScale(scale) }, { it.toDecimal() })
}

fun BaseTable<*>.fltx(name: String, roundingMode: RoundingMode, scale: Int = 2): Column<FltX> {
    return decimal(name).transform({ FltX(it).withScale(scale, roundingMode) }, { it.toDecimal() })
}

fun BaseTable<*>.kotlinDatetime(name: String): Column<LocalDateTime> {
    return datetime(name).transform({ it.toKotlinLocalDateTime() }, { it.toJavaLocalDateTime() })
}

inline fun <reified T : Enum<T>> BaseTable<*>.enums(name: String): Column<List<T>> {
    return varchar(name).transform({ it.split(',').map { value -> java.lang.Enum.valueOf(T::class.java, value) } }, { it.joinToString(",") { value -> value.name } })
}
