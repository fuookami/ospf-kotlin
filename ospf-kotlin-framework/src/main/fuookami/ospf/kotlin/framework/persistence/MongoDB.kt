package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.bson.*
import com.mongodb.*
import com.mongodb.client.*
import kotlinx.coroutines.*

/**
 * MongoDB 查询键
 *
 * @property name       库名
 * @property database   数据库名
 */
data class MongoClientKey(
    val name: String,
    val database: String
)

data class MongoDBConfigBuilder(
    var urls: List<String>? = null,
    var name: String? = null,
    var database: String? = null,
    var userName: String? = null,
    var password: String? = null
) {
    operator fun invoke(): MongoDBConfig? {
        return try {
            MongoDBConfig(
                urls = urls!!,
                name = name!!,
                database = database!!,
                userName = userName!!,
                password = password!!
            )
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class MongoDBConfig(
    val urls: List<String>,
    val name: String,
    val database: String,
    val userName: String,
    val password: String
) {
    val key get() = MongoClientKey(name = name, database = database)
}

object MongoDB {
    @get:Synchronized
    private val clients: MutableMap<MongoClientKey, MongoClient> = HashMap()

    @Synchronized
    fun Init(builder: MongoDBConfigBuilder.() -> Unit): MongoClient? {
        val config = MongoDBConfigBuilder()
        builder(config)
        return config()?.let { this(it) }
    }

    @Synchronized
    operator fun invoke(config: MongoDBConfig): MongoClient? {
        if (clients.containsKey(config.key)) {
            return clients[config.key]
        }

        return try {
            val hosts = config.urls.map { ulr ->
                val parts = ulr.split(":")
                ServerAddress(parts[0], parts[1].toInt())
            }

            val credential = MongoCredential.createScramSha1Credential(config.userName, "admin", config.password.toCharArray())

            val settings = MongoClientSettings.builder()
                .applyToClusterSettings { it.hosts(hosts) }
                .credential(credential)
                .build()
            // MongoClients.create("mongodb://${username}:${password}@${url.joinToString(",")}/?authMechanism=SCRAM-SHA-1")
            val client = MongoClients.create(settings)
            client.getDatabase(config.key.database)
            clients[config.key] = client
            client
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Synchronized
    operator fun invoke(key: MongoClientKey? = null): MongoClient? {
        return if (key != null) { clients[key] } else { null }
            ?: clients.values.firstOrNull()
    }

    @Synchronized
    operator fun invoke(name: String, dataBase: String? = null): MongoClient? {
        return if (dataBase != null) { clients[MongoClientKey(name = name, database = dataBase)] } else { null }
            ?: clients.filterKeys { it.name == name }.entries.firstOrNull()?.value
    }

    @Synchronized
    fun get(dataBase: String): MongoClient? {
        return clients.filterKeys { it.database == dataBase }.entries.firstOrNull()?.value
    }
}

private val json = Json { ignoreUnknownKeys = true }


@Synchronized
fun <T> MongoDatabase.insert(collection: String, serializer: KSerializer<T>, data: T) {
    this.getCollection(collection)
        .insertOne(Document.parse(json.encodeToString(serializer, data)))
}

fun <T : RequestDTO<T>> MongoDatabase.insertRequest(path: String, requester: String, version: String, serializer: KSerializer<T>, request: T) {
    val record = RequestRecordPO(requester, version, request)
    this.insert("${path.replace('/', '_')}_input", RequestRecordPO.serializer(serializer), record)
}

fun <T : ResponseDTO<T>> MongoDatabase.insertResponse(path: String, version: String, serializer: KSerializer<T>, response: T) {
    val record = ResponseRecordPO(version, response)
    this.insert("${path.replace('/', '_')}_output", ResponseRecordPO.serializer(serializer), record)
}

val enabledQueryKeys = setOf("id", "time", "version")

@Synchronized
fun <T> MongoDatabase.get(collection_name: String, serializer: KSerializer<T>, query: Map<String, String>): List<T> {
    val collection = this.getCollection(collection_name)
    val iter = collection.find(Document.parse("{${query.filterKeys { enabledQueryKeys.contains(it) }.entries.joinToString(", ") { "\"${it.key}\": { \$regex: /${it.value}/ }" }}}"))
    val cursor = iter.iterator()
    val results = ArrayList<T>()
    while (cursor.hasNext()) {
        results.add(json.decodeFromString(serializer, cursor.next().toJson()))
    }
    return results
}

fun <T : RequestDTO<T>> MongoDatabase.getRequest(path: String, serializer: KSerializer<T>, query: Map<String, String>): List<T> {
    return this.get("${path.replace('/', '_')}_input", RequestRecordPO.serializer(serializer), query).map { it.request }
}

fun <T : ResponseDTO<T>> MongoDatabase.getResponse(path: String, serializer: KSerializer<T>, query: Map<String, String>): List<T> {
    return this.get("${path.replace('/', '_')}_output", ResponseRecordPO.serializer(serializer), query).map { it.response }
}

interface PersistenceApiController {
    val mongoClient: MongoDatabase?

    @OptIn(InternalSerializationApi::class, DelicateCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <Req: RequestDTO<Req>, Rep: ResponseDTO<Rep>> persistenceApiImpl(
        api: String,
        request: Req,
        process: (Req) -> Rep
    ): Rep {
        GlobalScope.launch(Dispatchers.IO) {
            val cls = request::class
            val serializer = cls.serializer() as KSerializer<Req>
            mongoClient?.insertRequest(api, "", "", serializer, request)
        }
        val response = process(request)
        GlobalScope.launch(Dispatchers.IO) {
            val cls = response::class
            val serializer = cls.serializer() as KSerializer<Rep>
            mongoClient?.insertResponse(api, "", serializer, response)
        }
        return response
    }

    @OptIn(InternalSerializationApi::class, DelicateCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <Req: RequestDTO<Req>, Rep: ResponseDTO<Rep>, SyncRep: ResponseDTO<Rep>> persistenceApiImpl(
        api: String,
        request: Req,
        process: (Req) -> Rep,
        async_response: (Rep) -> Unit,
        sync_response: SyncRep
    ): SyncRep {
        GlobalScope.launch(Dispatchers.IO) {
            val cls = request::class
            val serializer = cls.serializer() as KSerializer<Req>
            mongoClient?.insertRequest(api, "", "", serializer, request)
        }
        GlobalScope.launch(Dispatchers.Default) {
            val response = process(request)
            GlobalScope.launch(Dispatchers.IO) {
                val cls = response::class
                val serializer = cls.serializer() as KSerializer<Rep>
                mongoClient?.insertResponse(api, "", serializer, response)
            }
        }
        return sync_response
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
