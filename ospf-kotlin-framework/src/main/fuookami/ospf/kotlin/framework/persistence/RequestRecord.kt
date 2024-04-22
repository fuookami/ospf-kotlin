package fuookami.ospf.kotlin.framework.persistence

import java.io.*
import kotlin.reflect.*
import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.*
import org.ktorm.database.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.serialization.*

interface RequestRecordRPO : Entity<RequestRecordRPO> {
    companion object : Entity.Factory<RequestRecordRPO>()

    var requestId: String
    var app: String
    var requester: String
    var version: String
    var time: LocalDateTime
    var request: ByteArray
}

open class RequestRecordRDAO(tableName: String) : Table<RequestRecordRPO>(tableName) {
    val id = long("id").primaryKey()
    val requestId = varchar("request_id").bindTo { it.requestId }
    val app = varchar("app").bindTo { it.app }
    val requester = varchar("requester").bindTo { it.requester }
    val version = varchar("version").bindTo { it.version }
    val time = datetime("time").transform({ it.toKotlinLocalDateTime() }, { it.toJavaLocalDateTime() }).bindTo { it.time }
    val request = blob("request").bindTo { it.request }
}

interface ResponseRecordRPO : Entity<ResponseRecordRPO> {
    companion object : Entity.Factory<ResponseRecordRPO>()

    var requestId: String
    var app: String
    var code: UInt64
    var requester: String
    var version: String
    var msg: String
    var time: LocalDateTime
    var response: ByteArray
}

open class ResponseRecordRDAO(tableName: String) : Table<ResponseRecordRPO>(tableName) {
    val id = long("id").primaryKey()
    val requestId = varchar("request_id").bindTo { it.requestId }
    val app = varchar("app").bindTo { it.app }
    val requester = varchar("requester").bindTo { it.requester }
    val version = varchar("version").bindTo { it.version }
    val code = long("code").transform({ UInt64(it.toULong()) }, { it.toLong() }).bindTo { it.code }
    val msg = varchar("msg").bindTo { it.msg }
    val time = datetime("time").transform({ it.toKotlinLocalDateTime() }, { it.toJavaLocalDateTime() }).bindTo { it.time }
    val response = blob("response").bindTo { it.response }
}

@Serializable
data class RequestRecordPO<T>(
    val id: String,
    val app: String,
    val requester: String,
    val version: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val time: LocalDateTime,
    val request: T
) where T : RequestDTO<T> {
    companion object {
        @OptIn(InternalSerializationApi::class)
        inline operator fun <reified T : RequestDTO<T>> invoke(rpo: RequestRecordRPO): RequestRecordPO<T>? {
            return this(rpo) {
                val stream = ByteArrayInputStream(it)
                readFromJson(T::class.serializer(), stream)
            }
        }

        inline operator fun <reified T : RequestDTO<T>> invoke(
            rpo: RequestRecordRPO,
            deserializer: (ByteArray) -> T
        ): RequestRecordPO<T>? {
            return try {
                RequestRecordPO(
                    id = rpo.requestId,
                    app = rpo.app,
                    requester = rpo.requester,
                    version = rpo.version,
                    time = rpo.time,
                    request = deserializer(rpo.request)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    constructor(app: String, requester: String, version: String, request: T) : this(
        id = request.id,
        app = app,
        requester = requester,
        version = version,
        time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        request = request
    )

    @Suppress("UNCHECKED_CAST")
    @OptIn(InternalSerializationApi::class)
    val rpo: RequestRecordRPO by lazy {
        rpo {
            val stream = ByteArrayOutputStream()
            writeJsonToStream(stream, (request::class as KClass<T>).serializer(), request)
            stream.toByteArray()
        }
    }

    fun rpo(serializer: (T) -> ByteArray): RequestRecordRPO {
        return RequestRecordRPO {
            requestId = this@RequestRecordPO.id
            app = this@RequestRecordPO.app
            requester = this@RequestRecordPO.requester
            version = this@RequestRecordPO.version
            time = this@RequestRecordPO.time
            request = serializer(this@RequestRecordPO.request)
        }
    }
}

data object RequestRecordDAO {
    fun <T : RequestDTO<T>> insert(
        db: Database,
        tableName: String,
        record: RequestRecordPO<T>,
        serializer: ((T) -> ByteArray)? = null
    ) {
        val table = RequestRecordRDAO(tableName)
        db.useTransaction {
            db.sequenceOf(table).add(
                if (serializer == null) {
                    record.rpo
                } else {
                    record.rpo(serializer)
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
    ): List<RequestRecordPO<T>> {
        val table = RequestRecordRDAO(tableName)
        return query(db, table, id, app, requester, time)
            .mapNotNull { RequestRecordPO<T>(table.createEntity(it)) }
    }

    inline fun <reified T : RequestDTO<T>> get(
        db: Database,
        tableName: String,
        id: String? = null,
        app: String? = null,
        requester: String? = null,
        time: Pair<LocalDateTime, LocalDateTime>? = null,
        deserializer: (ByteArray) -> T
    ): List<RequestRecordPO<T>> {
        val table = RequestRecordRDAO(tableName)
        return query(db, table, id, app, requester, time)
            .mapNotNull { RequestRecordPO<T>(table.createEntity(it), deserializer) }
    }

    fun query(
        db: Database,
        table: RequestRecordRDAO,
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
            query =
                query.where { (table.time greaterEq time.first) and (table.time less time.second) }
        }
        return query
    }
}

@Serializable
data class ResponseRecordPO<T>(
    val id: String,
    val app: String,
    val requester: String,
    val version: String,
    val code: UInt64,
    @Serializable(with = LocalDateTimeSerializer::class)
    val time: LocalDateTime,
    val response: T
) where T : ResponseDTO<T> {
    companion object {
        @OptIn(InternalSerializationApi::class)
        inline operator fun <reified T : ResponseDTO<T>> invoke(rpo: ResponseRecordRPO): ResponseRecordPO<T>? {
            return this(rpo) {
                val stream = ByteArrayInputStream(it)
                readFromJson(T::class.serializer(), stream)
            }
        }

        inline operator fun <reified T : ResponseDTO<T>> invoke(
            rpo: ResponseRecordRPO,
            deserializer: (ByteArray) -> T
        ): ResponseRecordPO<T>? {
            return try {
                ResponseRecordPO(
                    id = rpo.requestId,
                    app = rpo.app,
                    requester = rpo.requester,
                    version = rpo.version,
                    code = rpo.code,
                    time = rpo.time,
                    response = deserializer(rpo.response)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    constructor(app: String, requester: String, version: String, response: T) : this(
        id = response.id,
        app = app,
        requester = requester,
        version = version,
        code = response.code,
        time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        response = response
    )

    @Suppress("UNCHECKED_CAST")
    @OptIn(InternalSerializationApi::class)
    val rpo: ResponseRecordRPO by lazy {
        rpo {
            val stream = ByteArrayOutputStream()
            writeJsonToStream(stream, (response::class as KClass<T>).serializer(), response)
            stream.toByteArray()
        }
    }

    fun rpo(serializer: (T) -> ByteArray): ResponseRecordRPO {
        return ResponseRecordRPO {
            requestId = this@ResponseRecordPO.id
            app = this@ResponseRecordPO.app
            requester = this@ResponseRecordPO.requester
            version = this@ResponseRecordPO.version
            code = this@ResponseRecordPO.code
            msg = this@ResponseRecordPO.response.msg
            time = this@ResponseRecordPO.time
            response = serializer(this@ResponseRecordPO.response)
        }
    }
}

data object ResponseRecordDAO {
    fun <T : ResponseDTO<T>> insert(
        db: Database,
        tableName: String,
        record: ResponseRecordPO<T>,
        serializer: ((T) -> ByteArray)? = null
    ) {
        val table = ResponseRecordRDAO(tableName)
        db.useTransaction {
            db.sequenceOf(table).add(
                if (serializer == null) {
                    record.rpo
                } else {
                    record.rpo(serializer)
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
    ): List<ResponseRecordPO<T>> {
        val requestTable = RequestRecordRDAO(tableName.replace("response", "request"))
        val responseTable = ResponseRecordRDAO(tableName)
        return query(db, requestTable, responseTable, id, app, requester, time)
            .mapNotNull { ResponseRecordPO(responseTable.createEntity(it)) }
    }

    inline fun <reified T : ResponseDTO<T>> get(
        db: Database,
        tableName: String,
        id: String? = null,
        app: String? = null,
        requester: String? = null,
        time: Pair<LocalDateTime, LocalDateTime>? = null,
        deserializer: (ByteArray) -> T
    ): List<ResponseRecordPO<T>> {
        val requestTable = RequestRecordRDAO(tableName.replace("response", "request"))
        val responseTable = ResponseRecordRDAO(tableName)
        return query(db, requestTable, responseTable, id, app, requester, time)
            .mapNotNull { ResponseRecordPO(responseTable.createEntity(it), deserializer) }
    }

    fun query(
        db: Database,
        requestTable: RequestRecordRDAO,
        responseTable: ResponseRecordRDAO,
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
