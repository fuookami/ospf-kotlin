/**
 * MongoDB 持久化 API 控制器
 * MongoDB persistence API controller
 *
 * 提供 API 请求和响应的异步持久化记录功能。
 * Provides asynchronous persistence recording of API requests and responses.
 */
package fuookami.ospf.kotlin.framework.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import com.mongodb.client.MongoDatabase

/** 获取运行时 DTO 序列化器 / Get runtime DTO serializer */
@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
private fun <T : Any> serializerOf(value: T): KSerializer<T> {
    return value::class.serializer() as KSerializer<T>
}

/**
 * MongoDB 持久化 API 控制器接口
 * MongoDB persistence API controller interface
 *
 * 实现此接口的控制器可自动将 API 请求和响应持久化到 MongoDB。
 * Controllers implementing this interface can automatically persist API requests and responses to MongoDB.
 */
interface MongoPersistenceApiController {
    /**
     * MongoDB 客户端实例
     * MongoDB client instance
     */
    val mongoClient: MongoDatabase?

    /**
     * 持久化 API 实现（同步响应）
     * Persistence API implementation (synchronous response)
     *
     * @param Req 请求类型 / Request type
     * @param Rep 响应类型 / Response type
     * @param api API 路径 / API path
     * @param app 应用名称 / Application name
     * @param requester 请求者标识 / Requester identifier
     * @param version API 版本 / API version
     * @param request 请求对象 / Request object
     * @param process 请求处理函数 / Request processing function
     * @return 响应对象 / Response object
     */
    fun <Req : RequestDTO<Req>, Rep : ResponseDTO<Rep>> persistenceApiImpl(
        api: String,
        app: String,
        requester: String,
        version: String,
        request: Req,
        process: (Req) -> Rep
    ): Rep {
        frameworkPluginAsyncScope.launch(Dispatchers.IO) {
            val serializer = serializerOf(request)
            mongoClient?.insertRequest(
                path = api,
                app = "",
                requester = requester,
                version = version,
                serializer = serializer,
                request = request
            )
        }
        val response = process(request)
        frameworkPluginAsyncScope.launch(Dispatchers.IO) {
            val serializer = serializerOf(response)
            mongoClient?.insertResponse(
                path = api,
                app = app,
                requester = requester,
                version = version,
                serializer = serializer,
                response = response
            )
        }
        return response
    }

    /**
     * 持久化 API 实现（异步响应 + 同步响应）
     * Persistence API implementation (async response + sync response)
     *
     * @param Req 请求类型 / Request type
     * @param Rep 异步响应类型 / Async response type
     * @param SyncRep 同步响应类型 / Sync response type
     * @param api API 路径 / API path
     * @param app 应用名称 / Application name
     * @param requester 请求者标识 / Requester identifier
     * @param version API 版本 / API version
     * @param request 请求对象 / Request object
     * @param process 请求处理函数 / Request processing function
     * @param asyncResponse 异步响应回调 / Async response callback
     * @param syncResponse 同步响应对象 / Sync response object
     * @return 同步响应对象 / Sync response object
     */
    fun <Req : RequestDTO<Req>, Rep : ResponseDTO<Rep>, SyncRep : ResponseDTO<Rep>> persistenceApiImpl(
        api: String,
        app: String,
        requester: String,
        version: String,
        request: Req,
        process: (Req) -> Rep,
        asyncResponse: (Rep) -> Unit,
        syncResponse: SyncRep
    ): SyncRep {
        frameworkPluginAsyncScope.launch(Dispatchers.IO) {
            val serializer = serializerOf(request)
            mongoClient?.insertRequest(
                path = api,
                app = "",
                requester = requester,
                version = version,
                serializer = serializer,
                request = request
            )
        }
        frameworkPluginAsyncScope.launch(Dispatchers.Default) {
            val response = process(request)
            frameworkPluginAsyncScope.launch(Dispatchers.IO) {
                val serializer = serializerOf(response)
                mongoClient?.insertResponse(
                    path = api,
                    app = app,
                    requester = requester,
                    version = version,
                    serializer = serializer,
                    response = response
                )
            }
            asyncResponse(response)
        }
        return syncResponse
    }
}

// TODO: 如需跨切面持久化日志再实现 AOP API. / Implement AOP persistence API if cross-cutting logging is required.
