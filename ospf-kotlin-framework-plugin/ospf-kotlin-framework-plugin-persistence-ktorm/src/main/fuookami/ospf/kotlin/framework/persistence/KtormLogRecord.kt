/**
 * Ktorm 日志记录持久化实现
 * Ktorm Log Record Persistence Implementation
 *
 * 提供基于 Ktorm 的日志记录 Entity、Table 定义、DAO 操作及持久化保存。
 * Provides Ktorm-based log record Entity, Table definitions, DAO operations, and persistence saving.
*/
package fuookami.ospf.kotlin.framework.persistence

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.ktorm.database.*
import org.ktorm.entity.*
import org.ktorm.schema.*
import org.ktorm.support.sqlite.SQLiteDialect
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.serialization.writeJson
import fuookami.ospf.kotlin.utils.serialization.writeJsonToStream
import fuookami.ospf.kotlin.framework.log.*

/**
 * Ktorm 字节数组日志记录 Entity
 * Ktorm byte array log record Entity
*/
interface KtormLogRecordBytePO : Entity<KtormLogRecordBytePO> {
    companion object : Entity.Factory<KtormLogRecordBytePO>()

    /** Application name / 应用名称 */
    var app: String

    /** Version string / 版本字符串 */
    var version: String

    /** Service identifier / 服务标识 */
    var serviceId: String

    /** Processing step / 处理步骤 */
    var step: String
    var type: LogRecordType

    /** Log timestamp / 日志时间戳 */
    var time: LocalDateTime

    /** Available timestamp / 可用时间戳 */
    var availableTime: LocalDateTime
    var value: ByteArray
}

/**
 * Ktorm 字符串日志记录 Entity
 * Ktorm string log record Entity
*/
interface KtormLogRecordStringPO : Entity<KtormLogRecordStringPO> {
    companion object : Entity.Factory<KtormLogRecordStringPO>()

    /** Application name / 应用名称 */
    var app: String

    /** Version string / 版本字符串 */
    var version: String

    /** Service identifier / 服务标识 */
    var serviceId: String

    /** Processing step / 处理步骤 */
    var step: String
    var type: LogRecordType

    /** Log timestamp / 日志时间戳 */
    var time: LocalDateTime

    /** Available timestamp / 可用时间戳 */
    var availableTime: LocalDateTime
    var value: String
}

/**
 * Ktorm 字节数组日志记录 DAO
 * Ktorm byte array log record DAO
 *
 * @param tableName 表名 / Table name
*/
open class KtormLogRecordByteDAO(tableName: String) : Table<KtormLogRecordBytePO>(tableName) {
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

/**
 * Ktorm 字符串日志记录 DAO
 * Ktorm string log record DAO
 *
 * @param tableName 表名 / Table name
*/
open class KtormLogRecordStringDAO(tableName: String) : Table<KtormLogRecordStringPO>(tableName) {
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

/**
 * 将 LogRecordPO 转换为 Ktorm 字节数组 Entity
 * Convert LogRecordPO to Ktorm byte array Entity
*/
fun <T : Any> LogRecordPO<T>.toKtormBytePO(serializer: (T) -> ByteArray): KtormLogRecordBytePO {
    return KtormLogRecordBytePO {
        app = this@toKtormBytePO.app
        version = this@toKtormBytePO.version
        serviceId = this@toKtormBytePO.serviceId
        step = this@toKtormBytePO.step
        type = this@toKtormBytePO.type
        time = this@toKtormBytePO.time
        availableTime = this@toKtormBytePO.availableTime
        value = serializer(this@toKtormBytePO.value)
    }
}

/**
 * 将 LogRecordPO 转换为 Ktorm 字节数组 Entity（使用 KSerializer）
 * Convert LogRecordPO to Ktorm byte array Entity (using KSerializer)
*/
fun <T : Any> LogRecordPO<T>.toKtormBytePO(serializer: KSerializer<T>): KtormLogRecordBytePO {
    return toKtormBytePO { record ->
        val stream = java.io.ByteArrayOutputStream()
        writeJsonToStream(
            stream = stream,
            serializer = serializer,
            value = record
        )
        stream.toByteArray()
    }
}

/**
 * 将 LogRecordPO 转换为 Ktorm 字符串 Entity
 * Convert LogRecordPO to Ktorm string Entity
*/
fun <T : Any> LogRecordPO<T>.toKtormStringPO(serializer: (T) -> String): KtormLogRecordStringPO {
    return KtormLogRecordStringPO {
        app = this@toKtormStringPO.app
        version = this@toKtormStringPO.version
        serviceId = this@toKtormStringPO.serviceId
        step = this@toKtormStringPO.step
        type = this@toKtormStringPO.type
        time = this@toKtormStringPO.time
        availableTime = this@toKtormStringPO.availableTime
        value = serializer(this@toKtormStringPO.value)
    }
}

/**
 * 将 LogRecordPO 转换为 Ktorm 字符串 Entity（使用 KSerializer）
 * Convert LogRecordPO to Ktorm string Entity (using KSerializer)
*/
fun <T : Any> LogRecordPO<T>.toKtormStringPO(serializer: KSerializer<T>): KtormLogRecordStringPO {
    return toKtormStringPO { record ->
        writeJson(serializer, record)
    }
}

/**
 * 从 KtormLogRecordBytePO 创建 LogRecordPO
 * Create LogRecordPO from KtormLogRecordBytePO
*/
@OptIn(InternalSerializationApi::class)
inline operator fun <reified T : Any> LogRecordPO.Companion.invoke(po: KtormLogRecordBytePO): LogRecordPO<T>? {
    return this(po) {
        val stream = java.io.ByteArrayInputStream(it)
        fuookami.ospf.kotlin.utils.serialization.readFromJson(T::class.serializer(), stream)
    }
}

inline operator fun <reified T : Any> LogRecordPO.Companion.invoke(
    po: KtormLogRecordBytePO,
    deserializer: (ByteArray) -> T
): LogRecordPO<T>? {
    return try {
        LogRecordPO(
            app = po.app,
            version = po.version,
            serviceId = po.serviceId,
            step = po.step,
            type = po.type,
            time = po.time,
            availableTime = po.availableTime,
            value = deserializer(po.value)
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Ktorm 日志记录数据访问对象
 * Ktorm log record data access object
*/
data object KtormLogRecordDB {
    fun <T : Any> insert(
        db: Database,
        tableName: String,
        value: LogRecordPO<T>
    ) {
        val table = KtormLogRecordByteDAO(tableName)
        db.useTransaction {
            db.sequenceOf(table).add(value.toKtormBytePO { value.po.value })
        }
    }

    fun <T : Any> insert(
        db: Database,
        tableName: String,
        values: List<LogRecordPO<T>>
    ) {
        val table = KtormLogRecordByteDAO(tableName)
        db.useTransaction {
            for (po in values) {
                db.sequenceOf(table).add(po.toKtormBytePO { po.po.value })
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
        val table = KtormLogRecordStringDAO(tableName)
        db.useTransaction {
            db.sequenceOf(table).add(value.toKtormStringPO(serializer))
        }
    }

    @JvmName("insertAsString")
    fun <T : Any> insert(
        db: Database,
        tableName: String,
        values: List<LogRecordPO<T>>,
        serializer: (T) -> String
    ) {
        val table = KtormLogRecordStringDAO(tableName)
        db.useTransaction {
            for (po in values) {
                db.sequenceOf(table).add(po.toKtormStringPO(serializer))
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
        val table = KtormLogRecordByteDAO(tableName)
        db.useTransaction {
            db.sequenceOf(table).add(value.toKtormBytePO(serializer))
        }
    }

    @JvmName("insertAsBytes")
    fun <T : Any> insert(
        db: Database,
        tableName: String,
        values: List<LogRecordPO<T>>,
        serializer: (T) -> ByteArray
    ) {
        val table = KtormLogRecordByteDAO(tableName)
        db.useTransaction {
            for (po in values) {
                db.sequenceOf(table).add(po.toKtormBytePO(serializer))
            }
        }
    }
}

/**
 * Ktorm 日志记录持久化保存
 * Ktorm log record persistence saving
 *
 * @property db 数据库连接 / Database connection
 * @property tableName 表名 / Table name
 * @property scope 协程作用域，可为 null（同步模式） / Coroutine scope, nullable (sync mode)
*/
class KtormLogPersistenceSaving(
    private val db: Database,
    private val tableName: String,
    private val scope: CoroutineScope? = null
) : Saving, AutoCloseable {
    private val async: Boolean by lazy {
        scope != null && db.dialect !is SQLiteDialect
    }

    private val mutex = Mutex()
    private val stringQueue = ArrayList<KtormLogRecordStringPO>()
    private val byteQueue = ArrayList<KtormLogRecordBytePO>()

    private val job: Job? = if (this.async) {
        scope!!.launch {
            while (this.isActive) {
                mutex.withLock {
                    if (stringQueue.isNotEmpty()) {
                        val table = KtormLogRecordStringDAO(tableName)
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
                        val table = KtormLogRecordByteDAO(tableName)
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
                        val table = KtormLogRecordStringDAO(tableName)
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
                        val table = KtormLogRecordByteDAO(tableName)
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
                        stringQueue.add(value.toKtormStringPO(serializer))
                    } else {
                        byteQueue.add(value.toKtormBytePO(serializer))
                    }
                }
            }
        } else {
            if (db.dialect is SQLiteDialect) {
                val table = KtormLogRecordStringDAO(tableName)
                db.useTransaction {
                    it.connection.createStatement().use { stmt ->
                        stmt.execute("PRAGMA busy_timeout = 30000;")
                    }

                    db.sequenceOf(table).add(value.toKtormStringPO(serializer))
                }
            } else {
                val table = KtormLogRecordByteDAO(tableName)
                db.useTransaction {
                    db.sequenceOf(table).add(value.toKtormBytePO(serializer))
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
                    stringQueue.add(value.toKtormStringPO(serializer))
                }
            }
        } else {
            val table = KtormLogRecordStringDAO(tableName)
            db.useTransaction {
                if (db.dialect is SQLiteDialect) {
                    it.connection.createStatement().use { stmt ->
                        stmt.execute("PRAGMA busy_timeout = 30000;")
                    }
                }

                db.sequenceOf(table).add(value.toKtormStringPO(serializer))
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
                    byteQueue.add(value.toKtormBytePO(serializer))
                }
            }
        } else {
            val table = KtormLogRecordByteDAO(tableName)
            db.useTransaction {
                if (db.dialect is SQLiteDialect) {
                    it.connection.createStatement().use { stmt ->
                        stmt.execute("PRAGMA busy_timeout = 30000;")
                    }
                }

                db.sequenceOf(table).add(value.toKtormBytePO(serializer))
            }
        }
        return ok
    }
}
