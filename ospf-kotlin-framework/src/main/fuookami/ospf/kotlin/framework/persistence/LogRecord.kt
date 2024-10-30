package fuookami.ospf.kotlin.framework.persistence

import java.io.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import kotlinx.serialization.*
import org.ktorm.entity.*
import org.ktorm.schema.*
import org.ktorm.database.*
import org.ktorm.support.sqlite.*
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

@OptIn(DelicateCoroutinesApi::class)
class LogRecordPersistenceSaving(
    private val db: Database,
    private val tableName: String,
    async: Boolean? = null
) : Saving {
    private val async: Boolean by lazy {
        async ?: (db.dialect !is SQLiteDialect)
    }

    private val mutex = Mutex()
    private val stringQueue = ArrayList<LogRecordStringRPO>()
    private val byteQueue = ArrayList<LogRecordByteRPO>()
    private val job: Job? = if (this.async) {
        GlobalScope.launch {
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

    protected fun finalize() {
        try {
            job?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        runBlocking {
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

    override fun <T : Any> invoke(serializer: KSerializer<T>, value: LogRecordPO<T>): Try {
        if (async) {
            runBlocking {
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
    override operator fun <T : Any> invoke(serializer: (T) -> String, value: LogRecordPO<T>): Try {
        if (async) {
            runBlocking {
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
    override operator fun <T : Any> invoke(serializer: (T) -> ByteArray, value: LogRecordPO<T>): Try {
        if (async) {
            runBlocking {
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
