@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.log

import fuookami.ospf.kotlin.framework.persistence.LogRecordByteRPO
import fuookami.ospf.kotlin.framework.persistence.LogRecordStringRPO
import fuookami.ospf.kotlin.utils.serialization.LocalDateTimeSerializer
import fuookami.ospf.kotlin.utils.serialization.writeJson
import fuookami.ospf.kotlin.utils.serialization.writeJsonToStream
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.io.ByteArrayOutputStream
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
private fun <T : Any> runtimeLogSerializer(value: T): KSerializer<T> {
    // LogRecordPO 的 value 按同一泛型 T 写入与读取，此处将运行时序列化器收敛到最小作用域。
    // LogRecordPO writes/reads value under the same generic T; keep runtime serializer cast in minimal scope.
    return value::class.serializer() as KSerializer<T>
}

enum class LogRecordType {
    Info,
    Warning,
    Error,
    Fetal
}

interface LogRecord {
    val app: String
    val version: String
    val serviceId: String
    val step: String
    val type: LogRecordType
    val time: LocalDateTime
    val availableTime: LocalDateTime
}

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
    @OptIn(InternalSerializationApi::class)
    val rpo by lazy {
        byteRPO {
            val stream = ByteArrayOutputStream()
            writeJsonToStream(
                stream = stream,
                serializer = runtimeLogSerializer(value),
                value = value
            )
            stream.toByteArray()
        }
    }

    fun stringRPO(serializer: KSerializer<T>): LogRecordStringRPO {
        return stringRPO { writeJson(serializer, it) }
    }

    fun stringRPO(serializer: (T) -> String): LogRecordStringRPO {
        return LogRecordStringRPO {
            app = this@LogRecordPO.app
            version = this@LogRecordPO.version
            serviceId = this@LogRecordPO.serviceId
            step = this@LogRecordPO.step
            type = this@LogRecordPO.type
            time = this@LogRecordPO.time
            availableTime = this@LogRecordPO.availableTime
            value = serializer(this@LogRecordPO.value)
        }
    }

    fun byteRPO(serializer: KSerializer<T>): LogRecordByteRPO {
        return byteRPO {
            val stream = ByteArrayOutputStream()
            writeJsonToStream(
                stream = stream,
                serializer = serializer,
                value = it
            )
            stream.toByteArray()
        }
    }

    fun byteRPO(serializer: (T) -> ByteArray): LogRecordByteRPO {
        return LogRecordByteRPO {
            app = this@LogRecordPO.app
            version = this@LogRecordPO.version
            serviceId = this@LogRecordPO.serviceId
            step = this@LogRecordPO.step
            type = this@LogRecordPO.type
            time = this@LogRecordPO.time
            availableTime = this@LogRecordPO.availableTime
            value = serializer(this@LogRecordPO.value)
        }
    }
}
