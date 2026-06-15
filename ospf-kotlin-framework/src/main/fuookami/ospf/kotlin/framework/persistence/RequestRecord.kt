@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 请求/响应记录数据模型
 * Request/Response Record Data Model
 *
 * 提供请求和响应记录的纯数据类定义。
 * Provides pure data class definitions for request and response records.
 *
 * ORM 特有的 Entity/Table/DAO 实现已迁移至 plugin-persistence-ktorm 模块。
 * ORM-specific Entity/Table/DAO implementations have been migrated to the plugin-persistence-ktorm module.
 */
package fuookami.ospf.kotlin.framework.persistence

import java.io.*
import kotlin.time.*
import kotlin.time.Clock
import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.serializer
import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.utils.serialization.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 获取运行时请求序列化器
 * Get runtime request serializer
 *
 * @param request 请求对象 / Request object
 * @param T 请求 DTO 类型 / Request DTO type
 * @return 序列化器 / Serializer
 */
@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
private fun <T : RequestDTO<T>> runtimeRequestSerializer(request: T): KSerializer<T> {
    // RequestDTO 使用 F-bounded 泛型约束，运行时 request 类型与 T 语义一致。
    // RequestDTO uses F-bounded generics; runtime request type is consistent with T semantics.
    return request::class.serializer() as KSerializer<T>
}

/**
 * 获取运行时响应序列化器
 * Get runtime response serializer
 *
 * @param response 响应对象 / Response object
 * @param T 响应 DTO 类型 / Response DTO type
 * @return 序列化器 / Serializer
 */
@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
private fun <T : ResponseDTO<T>> runtimeResponseSerializer(response: T): KSerializer<T> {
    // ResponseDTO 使用 F-bounded 泛型约束，运行时 response 类型与 T 语义一致。
    // ResponseDTO uses F-bounded generics; runtime response type is consistent with T semantics.
    return response::class.serializer() as KSerializer<T>
}

/**
 * 请求记录持久化数据类
 * Request record persistence data class
 *
 * @property requestId 请求标识 / Request identifier
 * @property app 应用名 / Application name
 * @property requester 请求者 / Requester
 * @property version 版本 / Version
 * @property time 时间 / Time
 * @property request 请求数据（字节） / Request data (bytes)
 */
data class RequestRecordPO(
    val requestId: String,
    val app: String,
    val requester: String,
    val version: String,
    val time: LocalDateTime,
    val request: ByteArray
) {
    companion object {
        /**
         * 从请求记录创建 PO
         * Create PO from request record
         *
         * @param record 请求记录 / Request record
         * @param serializer 序列化函数 / Serialization function
         * @param T 请求 DTO 类型 / Request DTO type
         * @return 请求记录 PO / Request record PO
         */
        operator fun <T : RequestDTO<T>> invoke(
            record: RequestRecord<T>,
            serializer: (T) -> ByteArray
        ): RequestRecordPO {
            return RequestRecordPO(
                requestId = record.id,
                app = record.app,
                requester = record.requester,
                version = record.version,
                time = record.time,
                request = serializer(record.request)
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RequestRecordPO) return false
        return requestId == other.requestId &&
               app == other.app &&
               requester == other.requester &&
               version == other.version &&
               time == other.time &&
               request.contentEquals(other.request)
    }

    override fun hashCode(): Int {
        var result = requestId.hashCode()
        result = 31 * result + app.hashCode()
        result = 31 * result + requester.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + request.contentHashCode()
        return result
    }
}

/**
 * 响应记录持久化数据类
 * Response record persistence data class
 *
 * @property requestId 请求标识 / Request identifier
 * @property app 应用名 / Application name
 * @property requester 请求者 / Requester
 * @property version 版本 / Version
 * @property code 状态码 / Status code
 * @property msg 消息 / Message
 * @property time 时间 / Time
 * @property response 响应数据（字节） / Response data (bytes)
 */
data class ResponseRecordPO(
    val requestId: String,
    val app: String,
    val requester: String,
    val version: String,
    val code: UInt64,
    val msg: String,
    val time: LocalDateTime,
    val response: ByteArray
) {
    companion object {
        /**
         * 从响应记录创建 PO
         * Create PO from response record
         *
         * @param record 响应记录 / Response record
         * @param serializer 序列化函数 / Serialization function
         * @param T 响应 DTO 类型 / Response DTO type
         * @return 响应记录 PO / Response record PO
         */
        operator fun <T : ResponseDTO<T>> invoke(
            record: ResponseRecord<T>,
            serializer: (T) -> ByteArray
        ): ResponseRecordPO {
            return ResponseRecordPO(
                requestId = record.id,
                app = record.app,
                requester = record.requester,
                version = record.version,
                code = record.code,
                msg = record.response.msg,
                time = record.time,
                response = serializer(record.response)
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResponseRecordPO) return false
        return requestId == other.requestId &&
               app == other.app &&
               requester == other.requester &&
               version == other.version &&
               code == other.code &&
               msg == other.msg &&
               time == other.time &&
               response.contentEquals(other.response)
    }

    override fun hashCode(): Int {
        var result = requestId.hashCode()
        result = 31 * result + app.hashCode()
        result = 31 * result + requester.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + code.hashCode()
        result = 31 * result + msg.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + response.contentHashCode()
        return result
    }
}

/**
 * 请求记录
 * Request record
 *
 * @property id 请求标识 / Request identifier
 * @property app 应用名 / Application name
 * @property requester 请求者 / Requester
 * @property version 版本 / Version
 * @property time 时间 / Time
 * @property request 请求数据 / Request data
 * @param T 请求 DTO 类型 / Request DTO type
 */
@OptIn(ExperimentalTime::class)
@Serializable
data class RequestRecord<T>(
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
        inline operator fun <reified T : RequestDTO<T>> invoke(po: RequestRecordPO): RequestRecord<T>? {
            return this(po) {
                val stream = ByteArrayInputStream(it)
                readFromJson(T::class.serializer(), stream)
            }
        }

        inline operator fun <reified T : RequestDTO<T>> invoke(
            po: RequestRecordPO,
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
    }

    constructor(app: String, requester: String, version: String, request: T) : this(
        id = request.id,
        app = app,
        requester = requester,
        version = version,
        time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        request = request
    )

    @OptIn(InternalSerializationApi::class)
    val po: RequestRecordPO by lazy {
        po {
            val stream = ByteArrayOutputStream()
            writeJsonToStream(
                stream = stream,
                serializer = runtimeRequestSerializer(request),
                value = request
            )
            stream.toByteArray()
        }
    }

    fun po(serializer: (T) -> ByteArray): RequestRecordPO {
        return RequestRecordPO(
            requestId = this.id,
            app = this.app,
            requester = this.requester,
            version = this.version,
            time = this.time,
            request = serializer(this.request)
        )
    }
}

/**
 * 响应记录
 * Response record
 *
 * @property id 响应标识 / Response identifier
 * @property app 应用名 / Application name
 * @property requester 请求者 / Requester
 * @property version 版本 / Version
 * @property code 状态码 / Status code
 * @property time 时间 / Time
 * @property response 响应数据 / Response data
 * @param T 响应 DTO 类型 / Response DTO type
 */
@OptIn(ExperimentalTime::class)
@Serializable
data class ResponseRecord<T>(
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
        inline operator fun <reified T : ResponseDTO<T>> invoke(po: ResponseRecordPO): ResponseRecord<T>? {
            return this(po) {
                val stream = ByteArrayInputStream(it)
                readFromJson(T::class.serializer(), stream)
            }
        }

        inline operator fun <reified T : ResponseDTO<T>> invoke(
            po: ResponseRecordPO,
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

    @OptIn(InternalSerializationApi::class)
    val po: ResponseRecordPO by lazy {
        po {
            val stream = ByteArrayOutputStream()
            writeJsonToStream(stream, runtimeResponseSerializer(response), response)
            stream.toByteArray()
        }
    }

    fun po(serializer: (T) -> ByteArray): ResponseRecordPO {
        return ResponseRecordPO(
            requestId = this.id,
            app = this.app,
            requester = this.requester,
            version = this.version,
            code = this.code,
            msg = this.response.msg,
            time = this.time,
            response = serializer(this.response)
        )
    }
}
