package fuookami.ospf.kotlin.framework.persistence

import java.io.*
import kotlinx.datetime.*
import kotlinx.serialization.*
import org.ktorm.entity.*
import org.ktorm.schema.*
import org.ktorm.database.*
import org.ktorm.support.sqlite.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.serialization.*
import fuookami.ospf.kotlin.framework.log.*

interface LogRecordByteRPO : Entity<LogRecordByteRPO> {
    companion object : Entity.Factory<LogRecordByteRPO>()

    var app: String
    var version: String
    var serviceId: String
    var step: String
    var type: LogRecordType
    var time: LocalDateTime
    var availableTime: LocalDateTime
    var value: ByteArray
}

interface LogRecordStringRPO : Entity<LogRecordStringRPO> {
    companion object : Entity.Factory<LogRecordStringRPO>()

    var app: String
    var version: String
    var serviceId: String
    var step: String
    var type: LogRecordType
    var time: LocalDateTime
    var availableTime: LocalDateTime
    var value: String
}

open class LogRecordByteRDAO(tableName: String) : Table<LogRecordByteRPO>(tableName) {
    val id = ui64("id").primaryKey()
    val app = varchar("app").bindTo { it.app }
    val version = varchar("version").bindTo { it.version }
    val serviceId = varchar("service_id").bindTo { it.serviceId }
    val step = varchar("step").bindTo { it.step }
    val type = enum<LogRecordType>("type").bindTo { it.type }
    val time = kotlinDatetime("time").bindTo { it.time }
    val availableTime = kotlinDatetime("available_time").bindTo { it.availableTime }
    val value = blob("value").bindTo { it.value }
}

open class LogRecordStringRDAO(tableName: String) : Table<LogRecordStringRPO>(tableName) {
    val id = ui64("id").primaryKey()
    val app = varchar("app").bindTo { it.app }
    val version = varchar("version").bindTo { it.version }
    val serviceId = varchar("service_id").bindTo { it.serviceId }
    val step = varchar("step").bindTo { it.step }
    val type = enum<LogRecordType>("type").bindTo { it.type }
    val time = kotlinDatetime("time").bindTo { it.time }
    val availableTime = kotlinDatetime("available_time").bindTo { it.availableTime }
    val value = varchar("value").bindTo { it.value }
}

@OptIn(InternalSerializationApi::class)
inline operator fun <reified T : Any> LogRecordPO.Companion.invoke(rpo: LogRecordByteRPO): LogRecordPO<T>? {
    return this(rpo) {
        val stream = ByteArrayInputStream(it)
        readFromJson(T::class.serializer(), stream)
    }
}

inline operator fun <reified T : Any> LogRecordPO.Companion.invoke(
    rpo: LogRecordByteRPO,
    deserializer: (ByteArray) -> T
): LogRecordPO<T>? {
    return try {
        LogRecordPO(
            app = rpo.app,
            version = rpo.version,
            serviceId = rpo.serviceId,
            step = rpo.step,
            type = rpo.type,
            time = rpo.time,
            availableTime = rpo.availableTime,
            value = deserializer(rpo.value)
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

data object LogRecordDAO {
    fun <T: Any> insert(
        db: Database,
        tableName: String,
        value: LogRecordPO<T>
    ) {
        val table = LogRecordByteRDAO(tableName)
        db.useTransaction {
            db.sequenceOf(table).add(value.rpo)
        }
    }

    fun <T: Any> insert(
        db: Database,
        tableName: String,
        values: List<LogRecordPO<T>>
    ) {
        val table = LogRecordByteRDAO(tableName)
        db.useTransaction {
            for (po in values) {
                db.sequenceOf(table).add(po.rpo)
            }
        }
    }

    @JvmName("insertAsString")
    fun <T: Any> insert(
        db: Database,
        tableName: String,
        value: LogRecordPO<T>,
        serializer: (T) -> String
    ) {
        val table = LogRecordStringRDAO(tableName)
        db.useTransaction {
            db.sequenceOf(table).add(value.stringRPO(serializer))
        }
    }

    @JvmName("insertAsString")
    fun <T: Any> insert(
        db: Database,
        tableName: String,
        values: List<LogRecordPO<T>>,
        serializer: (T) -> String
    ) {
        val table = LogRecordStringRDAO(tableName)
        db.useTransaction {
            for (po in values) {
                db.sequenceOf(table).add(po.stringRPO(serializer))
            }
        }
    }

    @JvmName("insertAsBytes")
    fun <T: Any> insert(
        db: Database,
        tableName: String,
        value: LogRecordPO<T>,
        serializer: (T) -> ByteArray
    ) {
        val table = LogRecordByteRDAO(tableName)
        db.useTransaction {
            db.sequenceOf(table).add(value.byteRPO(serializer))
        }
    }

    @JvmName("insertAsBytes")
    fun <T: Any> insert(
        db: Database,
        tableName: String,
        values: List<LogRecordPO<T>>,
        serializer: (T) -> ByteArray
    ) {
        val table = LogRecordByteRDAO(tableName)
        db.useTransaction {
            for (po in values) {
                db.sequenceOf(table).add(po.byteRPO(serializer))
            }
        }
    }
}

data class LogRecordPersistenceSaving(
    val db: Database,
    val tableName: String
): Saving {
    @Synchronized
    override fun <T : Any> invoke(serializer: KSerializer<T>, value: LogRecordPO<T>): Try {
        if (db.dialect is SQLiteDialect) {
            val table = LogRecordStringRDAO(tableName)
            db.useTransaction {
                it.connection.createStatement().use { stmt ->
                    stmt.executeUpdate("PRAGMA busy_timeout = 30000;")
                }

                db.sequenceOf(table).add(value.stringRPO(serializer))
            }
        } else {
            val table = LogRecordByteRDAO(tableName)
            db.useTransaction {
                db.sequenceOf(table).add(value.byteRPO(serializer))
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("saveAsString")
    @Synchronized
    override operator fun <T: Any> invoke(serializer: (T) -> String, value: LogRecordPO<T>): Try {
        val table = LogRecordStringRDAO(tableName)
        db.useTransaction {
            if (db.dialect is SQLiteDialect) {
                it.connection.createStatement().use { stmt ->
                    stmt.executeUpdate("PRAGMA busy_timeout = 30000;")
                }
            }

            db.sequenceOf(table).add(value.stringRPO(serializer))
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("saveAsBytes")
    @Synchronized
    override operator fun <T: Any> invoke(serializer: (T) -> ByteArray, value: LogRecordPO<T>): Try {
        val table = LogRecordByteRDAO(tableName)
        db.useTransaction {
            if (db.dialect is SQLiteDialect) {
                it.connection.createStatement().use { stmt ->
                    stmt.executeUpdate("PRAGMA busy_timeout = 30000;")
                }
            }
            db.sequenceOf(table).add(value.byteRPO(serializer))
        }
        return ok
    }
}
