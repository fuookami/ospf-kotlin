@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 日志记录模型
 * Log Record Model
 *
 * 定义日志记录类型、接口和持久化对象。
 * Defines log record types, interfaces, and persistence objects.
*/
package fuookami.ospf.kotlin.framework.log

import java.io.ByteArrayOutputStream
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import fuookami.ospf.kotlin.utils.serialization.*
import fuookami.ospf.kotlin.framework.persistence.*

/**
 * 获取运行时日志序列化器
 * Get runtime log serializer
 *
 * @param value 日志值 / Log value
 * @param T 日志值类型 / Log value type
 * @return 序列化器 / Serializer
*/
@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
private fun <T : Any> runtimeLogSerializer(value: T): KSerializer<T> {
    // LogRecordPO 的 value 按同一泛型 T 写入与读取，此处将运行时序列化器收敛到最小作用域。
    // LogRecordPO writes/reads value under the same generic T; keep runtime serializer cast in minimal scope.
    return value::class.serializer() as KSerializer<T>
}

/**
 * 日志记录类型
 * Log record type
*/
enum class LogRecordType {
    /** 信息 / Info */
    Info,
    /** 警告 / Warning */
    Warning,
    /** 错误 / Error */
    Error,
    /** 致命 / Fatal */
    Fetal
}

/**
 * 日志记录接口
 * Log record interface
*/
interface LogRecord {
    companion object {}

    /** 应用名 / Application name */
    val app: String

    /** 版本 / Version */
    val version: String

    /** 服务标识 / Service identifier */
    val serviceId: String

    /** 步骤 / Step */
    val step: String

    /** 日志类型 / Log type */
    val type: LogRecordType

    /** 时间 / Time */
    val time: LocalDateTime

    /** 有效时间 / Available time */
    val availableTime: LocalDateTime
}

/**
 * 日志记录持久化对象
 * Log record persistence object
 *
 * @param T 日志值类型 / Log value type
*/
@OptIn(ExperimentalTime::class)
@Serializable
data class LogRecordPO<T : Any>(
    override val app: String,
    override val version: String,
    override val serviceId: String,
    override val step: String,
    override val type: LogRecordType,
    @Serializable(with = LocalDateTimeSerializer::class)
    override val time: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    override val availableTime: LocalDateTime,
    val value: T
) : LogRecord {
    companion object {}

    /**
     * 便捷构造函数（自动设置时间和可用时间）
     * Convenience constructor (auto-sets time and available time)
     *
     * @param app 应用名 / Application name
     * @param version 版本 / Version
     * @param serviceId 服务标识 / Service identifier
     * @param step 步骤 / Step
     * @param value 日志值 / Log value
     * @param type 日志类型，默认 Info / Log type, default Info
     * @param availableTime 可用时长，默认 90 天 / Available duration, default 90 days
    */
    constructor(
        app: String,
        version: String,
        serviceId: String,
        step: String,
        value: T,
        type: LogRecordType = LogRecordType.Info,
        availableTime: Duration = 90.days
    ) : this(
        app = app,
        version = version,
        serviceId = serviceId,
        step = step,
        type = type,
        time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        availableTime = (Clock.System.now() + availableTime).toLocalDateTime(TimeZone.currentSystemDefault()),
        value = value
    )

    /** 字节数组持久化对象（懒加载） / Byte array persistence object (lazy loaded) */
    @OptIn(InternalSerializationApi::class)
    val po by lazy {
        bytePO {
            val stream = ByteArrayOutputStream()
            writeJsonToStream(
                stream = stream,
                serializer = runtimeLogSerializer(value),
                value = value
            )
            stream.toByteArray()
        }
    }

    /**
     * 转换为字符串持久化对象（使用序列化器）
     * Convert to string persistence object (using serializer)
     *
     * @param serializer 序列化器 / Serializer
     * @return 字符串持久化对象 / String persistence object
    */
    fun stringPO(serializer: KSerializer<T>): LogRecordStringPO {
        return stringPO { writeJson(serializer, it) }
    }

    /**
     * 转换为字符串持久化对象（使用自定义序列化函数）
     * Convert to string persistence object (using custom serialization function)
     *
     * @param serializer 自定义序列化函数 / Custom serialization function
     * @return 字符串持久化对象 / String persistence object
    */
    fun stringPO(serializer: (T) -> String): LogRecordStringPO {
        return LogRecordStringPO(
            app = this@LogRecordPO.app,
            version = this@LogRecordPO.version,
            serviceId = this@LogRecordPO.serviceId,
            step = this@LogRecordPO.step,
            type = this@LogRecordPO.type,
            time = this@LogRecordPO.time,
            availableTime = this@LogRecordPO.availableTime,
            value = serializer(this@LogRecordPO.value)
        )
    }

    /**
     * 转换为字节数组持久化对象（使用序列化器）
     * Convert to byte array persistence object (using serializer)
     *
     * @param serializer 序列化器 / Serializer
     * @return 字节数组持久化对象 / Byte array persistence object
    */
    fun bytePO(serializer: KSerializer<T>): LogRecordBytePO {
        return bytePO {
            val stream = ByteArrayOutputStream()
            writeJsonToStream(
                stream = stream,
                serializer = serializer,
                value = it
            )
            stream.toByteArray()
        }
    }

    /**
     * 转换为字节数组持久化对象（使用自定义序列化函数）
     * Convert to byte array persistence object (using custom serialization function)
     *
     * @param serializer 自定义序列化函数 / Custom serialization function
     * @return 字节数组持久化对象 / Byte array persistence object
    */
    fun bytePO(serializer: (T) -> ByteArray): LogRecordBytePO {
        return LogRecordBytePO(
            app = this@LogRecordPO.app,
            version = this@LogRecordPO.version,
            serviceId = this@LogRecordPO.serviceId,
            step = this@LogRecordPO.step,
            type = this@LogRecordPO.type,
            time = this@LogRecordPO.time,
            availableTime = this@LogRecordPO.availableTime,
            value = serializer(this@LogRecordPO.value)
        )
    }
}
