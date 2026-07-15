/**
 * Ktorm 请求/响应记录持久化实现
 * Ktorm Request/Response Record Persistence Implementation
 *
 * 提供基于 Ktorm 的请求和响应记录 Entity、Table 定义及 DAO 操作。
 * Provides Ktorm-based request and response record Entity, Table definitions, and DAO operations.
*/
package fuookami.ospf.kotlin.framework.persistence

import java.io.ByteArrayInputStream
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.*
import fuookami.ospf.kotlin.utils.serialization.*
import kotlinx.datetime.LocalDateTime
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * Ktorm 请求记录 Entity
 * Ktorm request record Entity
*/
interface KtormRequestRecordPO : Entity<KtormRequestRecordPO> {
    companion object : Entity.Factory<KtormRequestRecordPO>()

    /** Request identifier / 请求标识 */
    var requestId: String

    /** Application name / 应用名称 */
    var app: String

    /** Requester name / 请求者名称 */
    var requester: String

    /** Version string / 版本字符串 */
    var version: String

    /** Request timestamp / 请求时间戳 */
    var time: LocalDateTime

    /** Serialized request body / 序列化的请求体 */
    var request: ByteArray
}

/**
 * Ktorm 请求记录 DAO
 * Ktorm request record DAO
 *
 * @param tableName 表名 / Table name
*/
open class KtormRequestRecordDAO(tableName: String) : Table<KtormRequestRecordPO>(tableName) {
    val id = long("id").primaryKey()
    val requestId = varchar("request_id").bindTo { it.requestId }
    val app = varchar("app").bindTo { it.app }
    val requester = varchar("requester").bindTo { it.requester }
    val version = varchar("version").bindTo { it.version }
    val time = kotlinDatetime("time").bindTo { it.time }
    val request = blob("request").bindTo { it.request }
}

/**
 * Ktorm 响应记录 Entity
 * Ktorm response record Entity
*/
interface KtormResponseRecordPO : Entity<KtormResponseRecordPO> {
    companion object : Entity.Factory<KtormResponseRecordPO>()

    /** Request identifier / 请求标识 */
    var requestId: String

    /** Application name / 应用名称 */
    var app: String

    /** Requester name / 请求者名称 */
    var requester: String

    /** Version string / 版本字符串 */
    var version: String

    /** Response status code / 响应状态码 */
    var code: UInt64

    /** Response message / 响应消息 */
    var msg: String

    /** Response timestamp / 响应时间戳 */
    var time: LocalDateTime

    /** Serialized response body / 序列化的响应体 */
    var response: ByteArray
}

/**
 * Ktorm 响应记录 DAO
 * Ktorm response record DAO
 *
 * @param tableName 表名 / Table name
*/
open class KtormResponseRecordDAO(tableName: String) : Table<KtormResponseRecordPO>(tableName) {
    val id = long("id").primaryKey()
    val requestId = varchar("request_id").bindTo { it.requestId }
    val app = varchar("app").bindTo { it.app }
    val requester = varchar("requester").bindTo { it.requester }
    val version = varchar("version").bindTo { it.version }
    val code = long("code").transform({ UInt64(it.toULong()) }, { it.toLong() }).bindTo { it.code }
    val msg = varchar("msg").bindTo { it.msg }
    val time = kotlinDatetime("time").bindTo { it.time }
    val response = blob("response").bindTo { it.response }
}

/**
 * 将 RequestRecord 转换为 Ktorm Entity
 * Convert RequestRecord to Ktorm Entity
*/
fun <T : RequestDTO<T>> RequestRecord<T>.toKtormPO(): KtormRequestRecordPO {
    return KtormRequestRecordPO {
        requestId = this@toKtormPO.id
        app = this@toKtormPO.app
        requester = this@toKtormPO.requester
        version = this@toKtormPO.version
        time = this@toKtormPO.time
        request = this@toKtormPO.po.request
    }
}

/**
 * 将 RequestRecord 转换为 Ktorm Entity（使用自定义序列化函数）
 * Convert RequestRecord to Ktorm Entity (using custom serialization function)
*/
fun <T : RequestDTO<T>> RequestRecord<T>.toKtormPO(serializer: (T) -> ByteArray): KtormRequestRecordPO {
    return KtormRequestRecordPO {
        requestId = this@toKtormPO.id
        app = this@toKtormPO.app
        requester = this@toKtormPO.requester
        version = this@toKtormPO.version
        time = this@toKtormPO.time
        request = serializer(this@toKtormPO.request)
    }
}

/**
 * 将 ResponseRecord 转换为 Ktorm Entity
 * Convert ResponseRecord to Ktorm Entity
*/
fun <T : ResponseDTO<T>> ResponseRecord<T>.toKtormPO(): KtormResponseRecordPO {
    return KtormResponseRecordPO {
        requestId = this@toKtormPO.id
        app = this@toKtormPO.app
        requester = this@toKtormPO.requester
        version = this@toKtormPO.version
        code = this@toKtormPO.code
        msg = this@toKtormPO.response.msg
        time = this@toKtormPO.time
        response = this@toKtormPO.po.response
    }
}

/**
 * 将 ResponseRecord 转换为 Ktorm Entity（使用自定义序列化函数）
 * Convert ResponseRecord to Ktorm Entity (using custom serialization function)
*/
fun <T : ResponseDTO<T>> ResponseRecord<T>.toKtormPO(serializer: (T) -> ByteArray): KtormResponseRecordPO {
    return KtormResponseRecordPO {
        requestId = this@toKtormPO.id
        app = this@toKtormPO.app
        requester = this@toKtormPO.requester
        version = this@toKtormPO.version
        code = this@toKtormPO.code
        msg = this@toKtormPO.response.msg
        time = this@toKtormPO.time
        response = serializer(this@toKtormPO.response)
    }
}

/**
 * Ktorm 请求记录数据访问对象
 * Ktorm request record data access object
*/
data object KtormRequestRecordDB {
    fun <T : RequestDTO<T>> insert(
        db: Database,
        tableName: String,
        record: RequestRecord<T>,
        serializer: ((T) -> ByteArray)? = null
    ) {
        val table = KtormRequestRecordDAO(tableName)
        db.useTransaction {
            db.sequenceOf(table).add(
                if (serializer == null) {
                    record.toKtormPO()
                } else {
                    record.toKtormPO(serializer)
                }
            )
        }
    }

    inline fun <reified T : RequestDTO<T>> get(
        db: Database,
        tableName: String,
        id: String? = null,
        app: String? = null,
        requester: String? = null,
        time: Pair<LocalDateTime, LocalDateTime>? = null
    ): List<RequestRecord<T>> {
        val table = KtormRequestRecordDAO(tableName)
        return query(
            db = db,
            table = table,
            id = id,
            app = app,
            requester = requester,
            time = time
        ).mapNotNull {
            RequestRecord<T>(table.createEntity(it))
        }
    }

    inline fun <reified T : RequestDTO<T>> get(
        db: Database,
        tableName: String,
        id: String? = null,
        app: String? = null,
        requester: String? = null,
        time: Pair<LocalDateTime, LocalDateTime>? = null,
        deserializer: (ByteArray) -> T
    ): List<RequestRecord<T>> {
        val table = KtormRequestRecordDAO(tableName)
        return query(
            db = db,
            table = table,
            id = id,
            app = app,
            requester = requester,
            time = time
        ).mapNotNull {
            RequestRecord<T>(table.createEntity(it), deserializer)
        }
    }

/**
 * Queries request records from the database with optional filters.
 * 使用可选过滤条件从数据库查询请求记录。
 * @param db The Ktorm database instance to query against / 要查询的 Ktorm 数据库实例
 * @param table The request record DAO table definition / 请求记录 DAO 表定义
 * @param id Optional request ID filter (LIKE match) / 可选的请求 ID 过滤条件（LIKE 匹配）
 * @param app Optional application name filter (LIKE match) / 可选的应用名称过滤条件（LIKE 匹配）
 * @param requester Optional requester name filter (LIKE match) / 可选的请求者名称过滤条件（LIKE 匹配）
 * @param time Optional time range filter (inclusive start, exclusive end) / 可选的时间范围过滤条件（含起始，不含结束）
 * @return The Ktorm query result set / Ktorm 查询结果集
*/
    fun query(
        db: Database,
        table: KtormRequestRecordDAO,
        id: String? = null,
        app: String? = null,
        requester: String? = null,
        time: Pair<LocalDateTime, LocalDateTime>? = null,
    ): Query {
        var query = db.from(table).select()
        if (id != null) {
            query = query.where { table.id like id }
        }
        if (app != null) {
            query = query.where { table.app like app }
        }
        if (requester != null) {
            query = query.where { table.requester like requester }
        }
        if (time != null) {
            query = query.where { (table.time greaterEq time.first) and (table.time less time.second) }
        }
        return query
    }
}

/**
 * Ktorm 响应记录数据访问对象
 * Ktorm response record data access object
*/
data object KtormResponseRecordDB {
    fun <T : ResponseDTO<T>> insert(
        db: Database,
        tableName: String,
        record: ResponseRecord<T>,
        serializer: ((T) -> ByteArray)? = null
    ) {
        val table = KtormResponseRecordDAO(tableName)
        db.useTransaction {
            db.sequenceOf(table).add(
                if (serializer == null) {
                    record.toKtormPO()
                } else {
                    record.toKtormPO(serializer)
                }
            )
        }
    }

    inline fun <reified T : ResponseDTO<T>> get(
        db: Database,
        tableName: String,
        id: String? = null,
        app: String? = null,
        requester: String? = null,
        time: Pair<LocalDateTime, LocalDateTime>? = null
    ): List<ResponseRecord<T>> {
        val requestTable = KtormRequestRecordDAO(tableName.replace("response", "request"))
        val responseTable = KtormResponseRecordDAO(tableName)
        return query(
            db = db,
            requestTable = requestTable,
            responseTable = responseTable,
            id = id,
            app = app,
            requester = requester,
            time = time
        ).mapNotNull {
            ResponseRecord(responseTable.createEntity(it))
        }
    }

    inline fun <reified T : ResponseDTO<T>> get(
        db: Database,
        tableName: String,
        id: String? = null,
        app: String? = null,
        requester: String? = null,
        time: Pair<LocalDateTime, LocalDateTime>? = null,
        deserializer: (ByteArray) -> T
    ): List<ResponseRecord<T>> {
        val requestTable = KtormRequestRecordDAO(tableName.replace("response", "request"))
        val responseTable = KtormResponseRecordDAO(tableName)
        return query(
            db = db,
            requestTable = requestTable,
            responseTable = responseTable,
            id = id,
            app = app,
            requester = requester,
            time = time
        ).mapNotNull {
            ResponseRecord(responseTable.createEntity(it), deserializer)
        }
    }

/**
 * Queries response records from the database by joining request and response tables with optional filters.
 * 通过连接请求和响应表并使用可选过滤条件从数据库查询响应记录。
 * @param db The Ktorm database instance to query against / 要查询的 Ktorm 数据库实例
 * @param requestTable The request record DAO table definition for the join / 用于连接的请求记录 DAO 表定义
 * @param responseTable The response record DAO table definition for the join / 用于连接的响应记录 DAO 表定义
 * @param id Optional request ID filter (LIKE match) / 可选的请求 ID 过滤条件（LIKE 匹配）
 * @param app Optional application name filter (LIKE match) / 可选的应用名称过滤条件（LIKE 匹配）
 * @param requester Optional requester name filter (LIKE match) / 可选的请求者名称过滤条件（LIKE 匹配）
 * @param time Optional time range filter applied to both request and response times / 应用于请求和响应时间的可选时间范围过滤条件
 * @return The Ktorm query result set / Ktorm 查询结果集
*/
    fun query(
        db: Database,
        requestTable: KtormRequestRecordDAO,
        responseTable: KtormResponseRecordDAO,
        id: String? = null,
        app: String? = null,
        requester: String? = null,
        time: Pair<LocalDateTime, LocalDateTime>? = null,
    ): Query {
        var query = db.from(responseTable)
            .leftJoin(
                requestTable,
                on = (responseTable.requestId eq requestTable.requestId) and
                        (responseTable.app eq requestTable.app) and
                        (responseTable.requester eq requestTable.requester) and
                        (responseTable.version eq requestTable.version)
            )
            .select()
        if (id != null) {
            query = query.where { responseTable.id like id }
        }
        if (app != null) {
            query = query.where { responseTable.app like app }
        }
        if (requester != null) {
            query = query.where { responseTable.requester like requester }
        }
        if (time != null) {
            query = query.where {
                ((requestTable.time greaterEq time.first) and (requestTable.time less time.second)) or
                        ((responseTable.time greaterEq time.first) and (responseTable.time less time.second))
            }
        }
        return query
    }
}

/**
 * 从 KtormRequestRecordPO 创建 RequestRecord
 * Create RequestRecord from KtormRequestRecordPO
*/
@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
inline operator fun <reified T : RequestDTO<T>> RequestRecord.Companion.invoke(po: KtormRequestRecordPO): RequestRecord<T>? {
    return this(po) {
        val stream = ByteArrayInputStream(it)
        readFromJson(T::class.serializer(), stream)
    }
}

inline operator fun <reified T : RequestDTO<T>> RequestRecord.Companion.invoke(
    po: KtormRequestRecordPO,
    deserializer: (ByteArray) -> T
): RequestRecord<T>? {
    return try {
        RequestRecord(
            id = po.requestId,
            app = po.app,
            requester = po.requester,
            version = po.version,
            time = po.time,
            request = deserializer(po.request)
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 从 KtormResponseRecordPO 创建 ResponseRecord
 * Create ResponseRecord from KtormResponseRecordPO
*/
@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
inline operator fun <reified T : ResponseDTO<T>> ResponseRecord.Companion.invoke(po: KtormResponseRecordPO): ResponseRecord<T>? {
    return this(po) {
        val stream = ByteArrayInputStream(it)
        readFromJson(T::class.serializer(), stream)
    }
}

inline operator fun <reified T : ResponseDTO<T>> ResponseRecord.Companion.invoke(
    po: KtormResponseRecordPO,
    deserializer: (ByteArray) -> T
): ResponseRecord<T>? {
    return try {
        ResponseRecord(
            id = po.requestId,
            app = po.app,
            requester = po.requester,
            version = po.version,
            code = po.code,
            time = po.time,
            response = deserializer(po.response)
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
