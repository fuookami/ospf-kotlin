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
    fun init(builder: MongoDBConfigBuilder.() -> Unit): MongoClient? {
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

            val credential =
                MongoCredential.createScramSha1Credential(config.userName, "admin", config.password.toCharArray())

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
        return if (key != null) {
            clients[key]
        } else {
            null
        }
            ?: clients.values.firstOrNull()
    }

    @Synchronized
    operator fun invoke(name: String, dataBase: String? = null): MongoClient? {
        return if (dataBase != null) {
            clients[MongoClientKey(name = name, database = dataBase)]
        } else {
            null
        }
            ?: clients.filterKeys { it.name == name }.entries.firstOrNull()?.value
    }

    @Synchronized
    fun get(dataBase: String): MongoClient? {
        return clients.filterKeys { it.database == dataBase }.entries.firstOrNull()?.value
    }
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> MongoDatabase.insert(collection: String, data: T) {
    insert(collection, T::class.serializer(), data)
}

fun <T> MongoDatabase.insert(collection: String, serializer: KSerializer<T>, data: T) {
    val json = Json {
        ignoreUnknownKeys = true
    }
    insert(collection, { json.encodeToString(serializer, it) }, data)
}

@Synchronized
fun <T> MongoDatabase.insert(collection: String, serializer: (T) -> String, data: T) {
    this.getCollection(collection)
        .insertOne(Document.parse(serializer(data)))
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> MongoDatabase.get(collectionName: String, query: Map<String, String>): List<T> {
    return get(collectionName, T::class.serializer(), query)
}

fun <T> MongoDatabase.get(collectionName: String, deserializer: KSerializer<T>, query: Map<String, String>): List<T> {
    val json = Json {
        ignoreUnknownKeys = true
    }
    return get(collectionName, { json.decodeFromString(deserializer, it) }, query)
}

@Synchronized
fun <T> MongoDatabase.get(collectionName: String, deserializer: (String) -> T, query: Map<String, String>): List<T> {
    val collection = this.getCollection(collectionName)
    val iterator =
        collection.find(Document.parse("{${query.entries.joinToString(", ") { "\"${it.key}\": { \$regex: /${it.value}/ }" }}}"))
    val cursor = iterator.iterator()
    val results = ArrayList<T>()
    while (cursor.hasNext()) {
        results.add(deserializer(cursor.next().toJson()))
    }
    return results
}
