package fuookami.ospf.kotlin.framework.log

import java.io.*
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.reflect.*
import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.utils.serialization.*
import fuookami.ospf.kotlin.framework.persistence.*

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

    @Suppress("UNCHECKED_CAST")
    @OptIn(InternalSerializationApi::class)
    val rpo by lazy {
        byteRPO {
            val stream = ByteArrayOutputStream()
            writeJsonToStream(stream, (value::class as KClass<T>).serializer(), value)
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
            writeJsonToStream(stream, serializer, it)
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
