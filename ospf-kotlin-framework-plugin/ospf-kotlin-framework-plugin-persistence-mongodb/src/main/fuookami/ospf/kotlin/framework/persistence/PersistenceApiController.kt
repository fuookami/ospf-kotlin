package fuookami.ospf.kotlin.framework.persistence

import com.mongodb.client.MongoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

interface MongoPersistenceApiController {
    val mongoClient: MongoDatabase?

    @OptIn(InternalSerializationApi::class)
    fun <Req : RequestDTO<Req>, Rep : ResponseDTO<Rep>> persistenceApiImpl(
        api: String,
        app: String,
        requester: String,
        version: String,
        request: Req,
        process: (Req) -> Rep
    ): Rep {
        frameworkPluginAsyncScope.launch(Dispatchers.IO) {
            val cls = request::class
            val serializer = cls.serializer() as KSerializer<Req>
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
            val cls = response::class
            val serializer = cls.serializer() as KSerializer<Rep>
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

    @OptIn(InternalSerializationApi::class)
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
            val cls = request::class
            val serializer = cls.serializer() as KSerializer<Req>
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
                val cls = response::class
                val serializer = cls.serializer() as KSerializer<Rep>
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