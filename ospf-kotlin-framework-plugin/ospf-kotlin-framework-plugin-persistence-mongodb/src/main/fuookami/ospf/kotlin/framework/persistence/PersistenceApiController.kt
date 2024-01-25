package fuookami.ospf.kotlin.framework.persistence

import kotlinx.coroutines.*
import kotlinx.serialization.*
import com.mongodb.client.*

interface MongoPersistenceApiController {
    val mongoClient: MongoDatabase?

    @OptIn(InternalSerializationApi::class, DelicateCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <Req : RequestDTO<Req>, Rep : ResponseDTO<Rep>> persistenceApiImpl(
        api: String,
        app: String,
        requester: String,
        version: String,
        request: Req,
        process: (Req) -> Rep
    ): Rep {
        GlobalScope.launch(Dispatchers.IO) {
            val cls = request::class
            val serializer = cls.serializer() as KSerializer<Req>
            mongoClient?.insertRequest(api, "", "", "", serializer, request)
        }
        val response = process(request)
        GlobalScope.launch(Dispatchers.IO) {
            val cls = response::class
            val serializer = cls.serializer() as KSerializer<Rep>
            mongoClient?.insertResponse(api, app, requester, version, serializer, response)
        }
        return response
    }

    @OptIn(InternalSerializationApi::class, DelicateCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
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
        GlobalScope.launch(Dispatchers.IO) {
            val cls = request::class
            val serializer = cls.serializer() as KSerializer<Req>
            mongoClient?.insertRequest(api, "", "", "", serializer, request)
        }
        GlobalScope.launch(Dispatchers.Default) {
            val response = process(request)
            GlobalScope.launch(Dispatchers.IO) {
                val cls = response::class
                val serializer = cls.serializer() as KSerializer<Rep>
                mongoClient?.insertResponse(api, app, requester, version, serializer, response)
            }
            asyncResponse(response)
        }
        return syncResponse
    }
}

// todo: make aop api
//@Target(AnnotationTarget.FUNCTION)
//@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
//annotation class SyncPersistenceInterface<Req: RequestDTO<Req>, Rep: ResponseDTO<Rep>>(
//    val name: String = "",
//    val dataBase: String = ""
//)
//
//@Target(AnnotationTarget.FUNCTION)
//@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
//annotation class AsyncPersistenceInterface<Req: RequestDTO<Req>, Rep: ResponseDTO<Rep>, SynRep: ResponseDTO<SynRep>>(
//    val syncResponse: SynRep,
//    val response: (Rep) -> Unit,
//    val name: String = "",
//    val dataBase: String = "",
//)
//
//@Aspect
//@Component
//class PersistenceInterfaceAspect {
//    @OptIn(DelicateCoroutinesApi::class, InternalSerializationApi::class)
//    @Around("@annotation(annotation)")
//    @Suppress("UNCHECKED_CAST")
//    fun <Req: RequestDTO<Req>, Rep: ResponseDTO<Rep>> syncProcess(point: ProceedingJoinPoint, annotation: SyncPersistenceInterface<Req, Rep>): Any {
//        val args = point.args
//        val request = args.find { it is RequestDTO<*> }?.let { it as Req }
//        if (request != null) {
//            GlobalScope.launch(Dispatchers.IO) {
//                val cls = request::class
//                val serializer = cls.serializer() as KSerializer<Req>
//                if (annotation.name.isNotEmpty() && annotation.dataBase.isNotEmpty()) {
//                    MongoDB(MongoClientKey(annotation.name, annotation.dataBase))
//                } else if (annotation.name.isNotEmpty()) {
//                    MongoDB(annotation.name)
//                } else if (annotation.dataBase.isNotEmpty()) {
//                    MongoDB.get(annotation.dataBase)
//                } else {
//                    MongoDB()
//                }
//                    ?.getDatabase(annotation.dataBase)
//                    ?.insertRequest("hztv_offline", "", "", serializer, request)
//            }
//        }
//        val response = point.proceed(args)
//        if (response is ResponseDTO<*>) {
//            GlobalScope.launch(Dispatchers.IO) {
//                val cls = response::class
//                val serializer = cls.serializer() as KSerializer<Rep>
//                if (annotation.name.isNotEmpty() && annotation.dataBase.isNotEmpty()) {
//                    MongoDB(MongoClientKey(annotation.name, annotation.dataBase))
//                } else if (annotation.name.isNotEmpty()) {
//                    MongoDB(annotation.name)
//                } else if (annotation.dataBase.isNotEmpty()) {
//                    MongoDB.get(annotation.dataBase)
//                } else {
//                    MongoDB()
//                }
//                    ?.getDatabase(annotation.dataBase)
//                    ?.insertResponse("hztv_offline", "", serializer, response as Rep)
//            }
//        }
//        return response
//    }
//
//    @OptIn(DelicateCoroutinesApi::class, InternalSerializationApi::class)
//    @Around("@annotation(annotation)")
//    @Suppress("UNCHECKED_CAST")
//    fun <Req: RequestDTO<Req>, Rep: ResponseDTO<Rep>, SynRep: ResponseDTO<SynRep>> asyncProcess(point: ProceedingJoinPoint, annotation: AsyncPersistenceInterface<Req, Rep, SynRep>): Any {
//        val args = point.args
//        val request = args.find { it is RequestDTO<*> }?.let { it as Req }
//        if (request != null) {
//            GlobalScope.launch(Dispatchers.IO) {
//                val cls = request::class
//                val serializer = cls.serializer() as KSerializer<Req>
//                if (annotation.name.isNotEmpty() && annotation.dataBase.isNotEmpty()) {
//                    MongoDB(MongoClientKey(annotation.name, annotation.dataBase))
//                } else if (annotation.name.isNotEmpty()) {
//                    MongoDB(annotation.name)
//                } else if (annotation.dataBase.isNotEmpty()) {
//                    MongoDB.get(annotation.dataBase)
//                } else {
//                    MongoDB()
//                }
//                    ?.getDatabase("bpp3d")
//                    ?.insertRequest("hztv_scm", "", "", serializer, request)
//            }
//        }
//        GlobalScope.launch {
//            val response = point.proceed(args)
//            if (response is ResponseDTO<*>) {
//                response as Rep
//                val cls = response::class
//                val serializer = cls.serializer() as KSerializer<Rep>
//                if (annotation.name.isNotEmpty() && annotation.dataBase.isNotEmpty()) {
//                    MongoDB(MongoClientKey(annotation.name, annotation.dataBase))
//                } else if (annotation.name.isNotEmpty()) {
//                    MongoDB(annotation.name)
//                } else if (annotation.dataBase.isNotEmpty()) {
//                    MongoDB.get(annotation.dataBase)
//                } else {
//                    MongoDB()
//                }
//                    ?.getDatabase("bpp3d")
//                    ?.insertResponse("hztv_scm", "", serializer, response)
//                annotation.response(response)
//            }
//        }
//        return annotation.syncResponse
//    }
//}
