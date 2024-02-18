package fuookami.ospf.kotlin.framework.log

import java.io.ByteArrayOutputStream
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.reflect.*
import kotlinx.datetime.*
import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.serialization.*
import fuookami.ospf.kotlin.framework.persistence.*

interface LogRecord {
    val app: String
    val version: String
    val serviceId: String
    val step: String
    val time: LocalDateTime
    val availableTime: LocalDateTime
}

@Serializable
data class LogRecordPO<T : Any>(
    override val app: String,
    override val version: String,
    override val serviceId: String,
    override val step: String,
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
        availableTime: Duration = 90.days
    ) : this(
        app = app,
        version = version,
        serviceId = serviceId,
        step = step,
        time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        availableTime = (Clock.System.now() + availableTime).toLocalDateTime(TimeZone.currentSystemDefault()),
        value = value
    )

    @Suppress("UNCHECKED_CAST")
    @OptIn(InternalSerializationApi::class)
    val rpo by lazy {
        rpo {
            val stream = ByteArrayOutputStream()
            writeJsonToStream(stream, (value::class as KClass<T>).serializer(), value)
            stream.toByteArray()
        }
    }

    fun rpo(serializer: (T) -> ByteArray): LogRecordRPO {
        return LogRecordRPO {
            app = this@LogRecordPO.app
            version = this@LogRecordPO.version
            serviceId = this@LogRecordPO.serviceId
            step = this@LogRecordPO.step
            time = this@LogRecordPO.time.toJavaLocalDateTime()
            availableTime = this@LogRecordPO.availableTime.toJavaLocalDateTime()
            value = serializer(this@LogRecordPO.value)
        }
    }
}
