@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.persistence

import fuookami.ospf.kotlin.framework.log.LogRecordPO
import fuookami.ospf.kotlin.framework.log.LogRecordType
import fuookami.ospf.kotlin.framework.log.Saving
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.serialization.readFromJson
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.ktorm.database.Database
import org.ktorm.database.use
import org.ktorm.entity.Entity
import org.ktorm.entity.add
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*
import org.ktorm.support.sqlite.SQLiteDialect
import java.io.ByteArrayInputStream
import kotlin.time.Duration.Companion.seconds

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
    val type = enum<LogRecordType>("log_type").bindTo { it.type }
    val time = kotlinDatetime("log_time").bindTo { it.time }
    val availableTime = kotlinDatetime("available_time").bindTo { it.availableTime }
    val value = blob("log_content").bindTo { it.value }
}

open class LogRecordStringRDAO(tableName: String) : Table<LogRecordStringRPO>(tableName) {
    val id = ui64("id").primaryKey()
    val app = varchar("app").bindTo { it.app }
    val version = varchar("version").bindTo { it.version }
    val serviceId = varchar("service_id").bindTo { it.serviceId }
    val step = varchar("step").bindTo { it.step }
    val type = enum<LogRecordType>("log_type").bindTo { it.type }
    val time = kotlinDatetime("log_time").bindTo { it.time }
    val availableTime = kotlinDatetime("available_time").bindTo { it.availableTime }
    val value = text("log_content").bindTo { it.value }
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
    fun <T : Any> insert(
        db: Database,
        tableName: String,
        value: LogRecordPO<T>
    ) {
        val table = LogRecordByteRDAO(tableName)
        db.useTransaction {
            db.sequenceOf(table).add(value.rpo)
        }
    }

    fun <T : Any> insert(
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
    fun <T : Any> insert(
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
    fun <T : Any> insert(
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
    fun <T : Any> insert(
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
    fun <T : Any> insert(
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

class LogRecordPersistenceSaving(
    private val db: Database,
    private val tableName: String,
    private val scope: CoroutineScope? = null
) : Saving, AutoCloseable {
    private val async: Boolean by lazy {
        scope != null && db.dialect !is SQLiteDialect
    }

    private val mutex = Mutex()
    private val stringQueue = ArrayList<LogRecordStringRPO>()
    private val byteQueue = ArrayList<LogRecordByteRPO>()

    private val job: Job? = if (this.async) {
        scope!!.launch {
            while (this.isActive) {
                mutex.withLock {
                    if (stringQueue.isNotEmpty()) {
                        val table = LogRecordStringRDAO(tableName)
                        db.useTransaction {
                            if (db.dialect is SQLiteDialect) {
                                it.connection.createStatement().use { stmt ->
                                    stmt.execute("PRAGMA busy_timeout = 30000;")
                                }
                            }

                            for (po in stringQueue) {
                                db.sequenceOf(table).add(po)
                            }
                        }
                        stringQueue.clear()
                    }
                    if (byteQueue.isNotEmpty()) {
                        val table = LogRecordByteRDAO(tableName)
                        db.useTransaction {
                            for (po in byteQueue) {
                                db.sequenceOf(table).add(po)
                            }
                        }
                        byteQueue.clear()
                    }
                }
                Thread.sleep(10.seconds.inWholeMilliseconds)
            }
        }
    } else {
        null
    }

    override fun close() {
        if (async) {
            try {
                job?.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            scope!!.launch {
                mutex.withLock {
                    if (stringQueue.isNotEmpty()) {
                        val table = LogRecordStringRDAO(tableName)
                        db.useTransaction {
                            if (db.dialect is SQLiteDialect) {
                                it.connection.createStatement().use { stmt ->
                                    stmt.execute("PRAGMA busy_timeout = 30000;")
                                }
                            }

                            for (po in stringQueue) {
                                db.sequenceOf(table).add(po)
                            }
                        }
                        stringQueue.clear()
                    }
                    if (byteQueue.isNotEmpty()) {
                        val table = LogRecordByteRDAO(tableName)
                        db.useTransaction {
                            for (po in byteQueue) {
                                db.sequenceOf(table).add(po)
                            }
                        }
                        byteQueue.clear()
                    }
                }
            }
        }
    }

    override fun <T : Any> invoke(value: LogRecordPO<T>, serializer: KSerializer<T>): Try {
        if (async) {
            scope!!.launch {
                mutex.withLock {
                    if (db.dialect is SQLiteDialect) {
                        stringQueue.add(value.stringRPO(serializer))
                    } else {
                        byteQueue.add(value.byteRPO(serializer))
                    }
                }
            }
        } else {
            if (db.dialect is SQLiteDialect) {
                val table = LogRecordStringRDAO(tableName)
                db.useTransaction {
                    it.connection.createStatement().use { stmt ->
                        stmt.execute("PRAGMA busy_timeout = 30000;")
                    }

                    db.sequenceOf(table).add(value.stringRPO(serializer))
                }
            } else {
                val table = LogRecordByteRDAO(tableName)
                db.useTransaction {
                    db.sequenceOf(table).add(value.byteRPO(serializer))
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("saveAsString")
    override operator fun <T : Any> invoke(value: LogRecordPO<T>, serializer: (T) -> String): Try {
        if (async) {
            scope!!.launch {
                mutex.withLock {
                    stringQueue.add(value.stringRPO(serializer))
                }
            }
        } else {
            val table = LogRecordStringRDAO(tableName)
            db.useTransaction {
                if (db.dialect is SQLiteDialect) {
                    it.connection.createStatement().use { stmt ->
                        stmt.execute("PRAGMA busy_timeout = 30000;")
                    }
                }

                db.sequenceOf(table).add(value.stringRPO(serializer))
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("saveAsBytes")
    @Synchronized
    override operator fun <T : Any> invoke(value: LogRecordPO<T>, serializer: (T) -> ByteArray): Try {
        if (async) {
            scope!!.launch {
                mutex.withLock {
                    byteQueue.add(value.byteRPO(serializer))
                }
            }
        } else {
            val table = LogRecordByteRDAO(tableName)
            db.useTransaction {
                if (db.dialect is SQLiteDialect) {
                    it.connection.createStatement().use { stmt ->
                        stmt.execute("PRAGMA busy_timeout = 30000;")
                    }
                }

                db.sequenceOf(table).add(value.byteRPO(serializer))
            }
        }
        return ok
    }
}
