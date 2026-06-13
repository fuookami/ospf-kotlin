/**
 * MongoDB 数据库客户端管理
 * MongoDB database client management
 *
 * 提供 MongoDB 客户端的初始化、管理和扩展函数。
 * Provides MongoDB client initialization, management, and extension functions.
 */
package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import com.mongodb.*
import com.mongodb.client.*
import org.bson.Document

/**
 * MongoDB 查询键 / MongoDB query key
 *
 * @property name 客户端名称 / Client name
 * @property database 数据库名称 / Database name
 */
data class MongoClientKey(
    val name: String,
    val database: String
)

/**
 * MongoDB 配置构建器
 * MongoDB configuration builder
 *
 * @property urls MongoDB 代理地址列表 / MongoDB broker URL list
 * @property name 客户端名称 / Client name
 * @property database 数据库名称 / Database name
 * @property userName 用户名 / Username
 * @property password 密码 / Password
 */
data class MongoDBConfigBuilder(
    var urls: List<String>? = null,
    var name: String? = null,
    var database: String? = null,
    var userName: String? = null,
    var password: String? = null
) {
    /**
     * 构建 MongoDB 配置
     * Build MongoDB configuration
     *
     * @return 配置实例，参数不完整时返回 null / Configuration instance, or null if parameters are incomplete
     */
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

/**
 * MongoDB 配置数据
 * MongoDB configuration data
 *
 * @property urls MongoDB 代理地址列表 / MongoDB broker URL list
 * @property name 客户端名称 / Client name
 * @property database 数据库名称 / Database name
 * @property userName 用户名 / Username
 * @property password 密码 / Password
 */
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

/**
 * MongoDB 客户端管理器
 * MongoDB client manager
 *
 * 管理多个 MongoDB 客户端实例，按名称和数据库索引。
 * Manages multiple MongoDB client instances, indexed by name and database.
 */
object MongoDB {
    @get:Synchronized
    private val clients: MutableMap<MongoClientKey, MongoClient> = HashMap()

    /**
     * 初始化并获取 MongoDB 客户端
     * Initialize and get MongoDB client
     *
     * @param builder 配置构建器 lambda / Configuration builder lambda
     * @return MongoDB 客户端实例，初始化失败时返回 null / MongoDB client instance, or null if initialization fails
     */
    @Synchronized
    fun init(builder: MongoDBConfigBuilder.() -> Unit): MongoClient? {
        val config = MongoDBConfigBuilder()
        builder(config)
        return config()?.let { this(it) }
    }

    /**
     * 获取或创建 MongoDB 客户端
     * Get or create MongoDB client
     *
     * @param config MongoDB 配置 / MongoDB configuration
     * @return MongoDB 客户端实例，创建失败时返回 null / MongoDB client instance, or null if creation fails
     */
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

    /**
     * 按键获取已注册的 MongoDB 客户端
     * Get registered MongoDB client by key
     *
     * @param key 客户端键（为 null 时返回第一个）/ Client key (returns first if null)
     * @return MongoDB 客户端实例，未找到时返回 null / MongoDB client instance, or null if not found
     */
    @Synchronized
    operator fun invoke(key: MongoClientKey? = null): MongoClient? {
        return if (key != null) {
            clients[key]
        } else {
            null
        }
            ?: clients.values.firstOrNull()
    }

    /**
     * 按名称获取已注册的 MongoDB 客户端
     * Get registered MongoDB client by name
     *
     * @param name 客户端名称 / Client name
     * @param dataBase 数据库名称（可选）/ Database name (optional)
     * @return MongoDB 客户端实例，未找到时返回 null / MongoDB client instance, or null if not found
     */
    @Synchronized
    operator fun invoke(name: String, dataBase: String? = null): MongoClient? {
        return if (dataBase != null) {
            clients[MongoClientKey(name = name, database = dataBase)]
        } else {
            null
        }
            ?: clients.filterKeys { it.name == name }.entries.firstOrNull()?.value
    }

    /**
     * 按数据库名称获取已注册的 MongoDB 客户端
     * Get registered MongoDB client by database name
     *
     * @param dataBase 数据库名称 / Database name
     * @return MongoDB 客户端实例，未找到时返回 null / MongoDB client instance, or null if not found
     */
    @Synchronized
    fun get(dataBase: String): MongoClient? {
        return clients.filterKeys { it.database == dataBase }.entries.firstOrNull()?.value
    }
}

/**
 * 向集合中插入数据（使用默认序列化器）
 * Insert data into collection (using default serializer)
 *
 * @param T 数据类型 / Data type
 * @param collection 集合名称 / Collection name
 * @param data 要插入的数据 / Data to insert
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> MongoDatabase.insert(collection: String, data: T) {
    insert(
        collection = collection,
        serializer = T::class.serializer(),
        data = data
    )
}

/**
 * 向集合中插入数据（使用指定序列化器）
 * Insert data into collection (using specified serializer)
 *
 * @param T 数据类型 / Data type
 * @param collection 集合名称 / Collection name
 * @param serializer 序列化器 / Serializer
 * @param data 要插入的数据 / Data to insert
 */
fun <T> MongoDatabase.insert(collection: String, serializer: KSerializer<T>, data: T) {
    val json = Json {
        ignoreUnknownKeys = true
    }
    insert(
        collection = collection,
        serializer = { json.encodeToString(serializer, it) },
        data = data
    )
}

/**
 * 向集合中插入数据（使用自定义序列化函数）
 * Insert data into collection (using custom serialization function)
 *
 * @param T 数据类型 / Data type
 * @param collection 集合名称 / Collection name
 * @param serializer 序列化函数 / Serialization function
 * @param data 要插入的数据 / Data to insert
 */
@Synchronized
fun <T> MongoDatabase.insert(collection: String, serializer: (T) -> String, data: T) {
    this.getCollection(collection)
        .insertOne(Document.parse(serializer(data)))
}

/**
 * 从集合中查询数据（使用默认反序列化器）
 * Query data from collection (using default deserializer)
 *
 * @param T 数据类型 / Data type
 * @param collectionName 集合名称 / Collection name
 * @param query 查询条件 / Query conditions
 * @return 查询结果列表 / Query result list
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> MongoDatabase.get(collectionName: String, query: Map<String, String>): List<T> {
    return get(
        collectionName = collectionName,
        deserializer = T::class.serializer(),
        query = query
    )
}

/**
 * 从集合中查询数据（使用指定反序列化器）
 * Query data from collection (using specified deserializer)
 *
 * @param T 数据类型 / Data type
 * @param collectionName 集合名称 / Collection name
 * @param deserializer 反序列化器 / Deserializer
 * @param query 查询条件 / Query conditions
 * @return 查询结果列表 / Query result list
 */
fun <T> MongoDatabase.get(collectionName: String, deserializer: KSerializer<T>, query: Map<String, String>): List<T> {
    val json = Json {
        ignoreUnknownKeys = true
    }
    return get(
        collectionName = collectionName,
        deserializer = { json.decodeFromString(deserializer, it) },
        query = query
    )
}

/**
 * 从集合中查询数据（使用自定义反序列化函数）
 * Query data from collection (using custom deserialization function)
 *
 * @param T 数据类型 / Data type
 * @param collectionName 集合名称 / Collection name
 * @param deserializer 反序列化函数 / Deserialization function
 * @param query 查询条件 / Query conditions
 * @return 查询结果列表 / Query result list
 */
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