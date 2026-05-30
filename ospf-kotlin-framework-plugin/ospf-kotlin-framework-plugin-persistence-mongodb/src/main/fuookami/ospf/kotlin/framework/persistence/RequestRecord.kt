/**
 * MongoDB 请求/响应记录持久化
 * MongoDB request/response record persistence
 *
 * 提供将 API 请求和响应记录插入和查询 MongoDB 的扩展函数。
 * Provides extension functions for inserting and querying API request/response records in MongoDB.
 */
package fuookami.ospf.kotlin.framework.persistence

import com.mongodb.client.MongoDatabase
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * 插入请求记录（使用默认序列化器）
 * Insert request record (using default serializer)
 *
 * @param T 请求类型 / Request type
 * @param path API 路径 / API path
 * @param app 应用名称 / Application name
 * @param requester 请求者标识 / Requester identifier
 * @param version API 版本 / API version
 * @param request 请求对象 / Request object
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T> MongoDatabase.insertRequest(
    path: String,
    app: String,
    requester: String,
    version: String,
    request: T
) where T : RequestDTO<T>, T : Any {
    insertRequest(
        path = path,
        app = app,
        requester = requester,
        version = version,
        serializer = T::class.serializer(),
        request = request
    )
}

/**
 * 插入请求记录（使用指定序列化器）
 * Insert request record (using specified serializer)
 *
 * @param T 请求类型 / Request type
 * @param path API 路径 / API path
 * @param app 应用名称 / Application name
 * @param requester 请求者标识 / Requester identifier
 * @param version API 版本 / API version
 * @param serializer 序列化器 / Serializer
 * @param request 请求对象 / Request object
 */
fun <T : RequestDTO<T>> MongoDatabase.insertRequest(
    path: String,
    app: String,
    requester: String,
    version: String,
    serializer: KSerializer<T>,
    request: T
) {
    val record = RequestRecordPO(
        app = app,
        requester = requester,
        version = version,
        request = request
    )
    this.insert(
        collection = "${path.replace('/', '_')}_input",
        serializer = RequestRecordPO.serializer(serializer),
        data = record
    )
}

/**
 * 查询请求记录（使用默认反序列化器）
 * Query request records (using default deserializer)
 *
 * @param T 请求类型 / Request type
 * @param path API 路径 / API path
 * @param query 查询条件 / Query conditions
 * @return 请求记录列表 / Request record list
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T> MongoDatabase.getRequest(
    path: String,
    query: Map<String, String>
): List<T> where T : RequestDTO<T>, T : Any {
    return getRequest(
        path = path,
        serializer = T::class.serializer(),
        query = query
    )
}

/**
 * 查询请求记录（使用指定序列化器）
 * Query request records (using specified serializer)
 *
 * @param T 请求类型 / Request type
 * @param path API 路径 / API path
 * @param serializer 序列化器 / Serializer
 * @param query 查询条件 / Query conditions
 * @return 请求记录列表 / Request record list
 */
fun <T : RequestDTO<T>> MongoDatabase.getRequest(
    path: String,
    serializer: KSerializer<T>,
    query: Map<String, String>
): List<T> {
    return this.get(
        collectionName = "${path.replace('/', '_')}_input",
        deserializer = RequestRecordPO.serializer(serializer),
        query = query
    ).map { it.request }
}

/**
 * 插入响应记录（使用默认序列化器）
 * Insert response record (using default serializer)
 *
 * @param T 响应类型 / Response type
 * @param path API 路径 / API path
 * @param app 应用名称 / Application name
 * @param requester 请求者标识 / Requester identifier
 * @param version API 版本 / API version
 * @param response 响应对象 / Response object
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T> MongoDatabase.insertResponse(
    path: String,
    app: String,
    requester: String,
    version: String,
    response: T
) where T : ResponseDTO<T>, T : Any {
    return insertResponse(
        path = path,
        app = app,
        requester = requester,
        version = version,
        serializer = T::class.serializer(),
        response = response
    )
}

/**
 * 插入响应记录（使用指定序列化器）
 * Insert response record (using specified serializer)
 *
 * @param T 响应类型 / Response type
 * @param path API 路径 / API path
 * @param app 应用名称 / Application name
 * @param requester 请求者标识 / Requester identifier
 * @param version API 版本 / API version
 * @param serializer 序列化器 / Serializer
 * @param response 响应对象 / Response object
 */
fun <T : ResponseDTO<T>> MongoDatabase.insertResponse(
    path: String,
    app: String,
    requester: String,
    version: String,
    serializer: KSerializer<T>,
    response: T
) {
    val record = ResponseRecordPO(
        app = app,
        requester = requester,
        version = version,
        response = response
    )
    this.insert(
        collection = "${path.replace('/', '_')}_output",
        serializer = ResponseRecordPO.serializer(serializer),
        data = record
    )
}

/**
 * 查询响应记录（使用默认反序列化器）
 * Query response records (using default deserializer)
 *
 * @param T 响应类型 / Response type
 * @param path API 路径 / API path
 * @param query 查询条件 / Query conditions
 * @return 响应记录列表 / Response record list
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T> MongoDatabase.getResponse(
    path: String,
    query: Map<String, String>
): List<T> where T : ResponseDTO<T>, T : Any {
    return getResponse(
        path = path,
        serializer = T::class.serializer(),
        query = query
    )
}

/**
 * 查询响应记录（使用指定序列化器）
 * Query response records (using specified serializer)
 *
 * @param T 响应类型 / Response type
 * @param path API 路径 / API path
 * @param serializer 序列化器 / Serializer
 * @param query 查询条件 / Query conditions
 * @return 响应记录列表 / Response record list
 */
fun <T : ResponseDTO<T>> MongoDatabase.getResponse(
    path: String,
    serializer: KSerializer<T>,
    query: Map<String, String>
): List<T> {
    return this.get(
        collectionName = "${path.replace('/', '_')}_output",
        deserializer = ResponseRecordPO.serializer(serializer),
        query = query
    ).map { it.response }
}

/**
 * 查询请求记录（DAO 扩展方法）
 * Query request records (DAO extension method)
 *
 * @param T 请求类型 / Request type
 * @param db MongoDB 管理器实例 / MongoDB manager instance
 * @param id 记录 ID（可选）/ Record ID (optional)
 * @param requester 请求者标识（可选）/ Requester identifier (optional)
 * @param time 时间戳（可选）/ Timestamp (optional)
 * @return 请求记录列表 / Request record list
 */
inline fun <reified T : RequestDTO<T>> RequestRecordDAO.find(
    db: MongoDB,
    id: String? = null,
    requester: String? = null,
    time: String?
): List<RequestRecordPO<T>> {
    TODO("not implement yet")
}