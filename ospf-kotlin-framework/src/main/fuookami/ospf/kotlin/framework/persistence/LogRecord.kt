package fuookami.ospf.kotlin.framework.persistence

import java.io.ByteArrayInputStream
import kotlinx.datetime.*
import kotlinx.serialization.*
import org.ktorm.entity.*
import org.ktorm.schema.*
import fuookami.ospf.kotlin.utils.serialization.*
import fuookami.ospf.kotlin.framework.log.*

interface LogRecordRPO : Entity<LogRecordRPO> {
    companion object : Entity.Factory<LogRecordRPO>()

    var app: String
    var version: String
    var serviceId: String
    var step: String
    var time: java.time.LocalDateTime
    var availableTime: java.time.LocalDateTime
    var value: ByteArray
}

open class LogRecordRDAO(tableName: String) : Table<LogRecordRPO>(tableName) {
    val id = int("id").primaryKey()
    val app = varchar("app").bindTo { it.app }
    val version = varchar("version").bindTo { it.version }
    val serviceId = varchar("service_id").bindTo { it.serviceId }
    val step = varchar("step").bindTo { it.step }
    val time = datetime("time").bindTo { it.time }
    val availableTime = datetime("available_time").bindTo { it.availableTime }
    val value = blob("value").bindTo { it.value }
}

@OptIn(InternalSerializationApi::class)
inline operator fun <reified T : Any> LogRecordPO.Companion.invoke(rpo: LogRecordRPO): LogRecordPO<T>? {
    return this(rpo) {
        val stream = ByteArrayInputStream(it)
        readFromJson(T::class.serializer(), stream)
    }
}

inline operator fun <reified T : Any> LogRecordPO.Companion.invoke(
    rpo: LogRecordRPO,
    deserializer: (ByteArray) -> T
): LogRecordPO<T>? {
    return try {
        LogRecordPO(
            app = rpo.app,
            version = rpo.version,
            serviceId = rpo.serviceId,
            step = rpo.step,
            time = rpo.time.toKotlinLocalDateTime(),
            availableTime = rpo.availableTime.toKotlinLocalDateTime(),
            value = deserializer(rpo.value)
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
