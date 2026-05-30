@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * Redis 客户端管理
 * Redis client management
 *
 * 提供 Redis 客户端的初始化、管理和常用数据结构操作扩展函数。
 * Provides Redis client initialization, management, and common data structure operation extension functions.
 */
package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisSentinelPool
import redis.clients.jedis.util.Pool
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Redis 客户端键
 * Redis client key
 *
 * @property name 客户端名称 / Client name
 * @property database 数据库编号 / Database number
 */
data class RedisClientKey(
    val name: String,
    val database: Int
)

/**
 * Redis 配置构建器
 * Redis configuration builder
 *
 * @property urls Redis 哨兵地址列表 / Redis sentinel URL list
 * @property name 客户端名称 / Client name
 * @property masterName 主节点名称（可选）/ Master name (optional)
 * @property database 数据库编号 / Database number
 * @property password 密码 / Password
 */
data class RedisConfigBuilder(
    var urls: List<String>? = null,
    var name: String? = null,
    var masterName: String? = null,
    var database: Int? = null,
    var password: String? = null
) {
    /**
     * 构建 Redis 配置
     * Build Redis configuration
     *
     * @return 配置实例，参数不完整时返回 null / Configuration instance, or null if parameters are incomplete
     */
    operator fun invoke(): RedisConfig? {
        return try {
            RedisConfig(
                urls = urls!!,
                name = name!!,
                masterName = masterName,
                database = database!!,
                password = password!!
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Redis 配置数据
 * Redis configuration data
 *
 * @property urls Redis 哨兵地址列表 / Redis sentinel URL list
 * @property name 客户端名称 / Client name
 * @property masterName 主节点名称（可选）/ Master name (optional)
 * @property database 数据库编号 / Database number
 * @property password 密码 / Password
 */
@Serializable
data class RedisConfig(
    val urls: List<String>,
    val name: String,
    val masterName: String?,
    val database: Int,
    val password: String
) {
    val key get() = RedisClientKey(name = name, database = database)
}

/**
 * Redis 客户端
 * Redis client
 *
 * @property jedis Jedis 连接实例 / Jedis connection instance
 */
data class RedisClient(
    val jedis: Jedis
) : AutoCloseable {
    override fun close() {
        jedis.close()
    }
}

/**
 * Redis 客户端管理器
 * Redis client manager
 *
 * 管理多个 Redis 连接池实例，按名称和数据库编号索引。
 * Manages multiple Redis connection pool instances, indexed by name and database number.
 */
object Redis {
    @get:Synchronized
    private val clients: MutableMap<RedisClientKey, Pool<Jedis>> = HashMap()

    /**
     * 初始化并获取 Redis 客户端
     * Initialize and get Redis client
     *
     * @param builder 配置构建器 lambda / Configuration builder lambda
     * @return Redis 客户端实例，初始化失败时返回 null / Redis client instance, or null if initialization fails
     */
    @Synchronized
    fun init(builder: RedisConfigBuilder.() -> Unit): RedisClient? {
        val config = RedisConfigBuilder()
        builder(config)
        return config()?.let { this(it) }
    }

    /**
     * 获取或创建 Redis 客户端
     * Get or create Redis client
     *
     * @param config Redis 配置 / Redis configuration
     * @return Redis 客户端实例，创建失败时返回 null / Redis client instance, or null if creation fails
     */
    @Synchronized
    operator fun invoke(config: RedisConfig): RedisClient? {
        if (clients.containsKey(config.key)) {
            return clients[config.key]?.resource?.let { RedisClient(it) }
        }

        return try {
            val pool = JedisSentinelPool(
                config.masterName ?: "master",
                config.urls.toSet(),
                // todo: pool config
                JedisPoolConfig(),
                3600,
                config.password,
                config.database
            )
            clients[config.key] = pool
            pool.resource?.let { RedisClient(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 按键获取已注册的 Redis 客户端
     * Get registered Redis client by key
     *
     * @param key 客户端键（为 null 时返回第一个）/ Client key (returns first if null)
     * @return Redis 客户端实例，未找到时返回 null / Redis client instance, or null if not found
     */
    @Synchronized
    operator fun invoke(key: RedisClientKey? = null): RedisClient? {
        return if (key != null) {
            clients[key]?.resource?.let { RedisClient(it) }
        } else {
            null
        } ?: clients.values.firstOrNull()?.resource?.let { RedisClient(it) }
    }

    /**
     * 按名称获取已注册的 Redis 客户端
     * Get registered Redis client by name
     *
     * @param name 客户端名称 / Client name
     * @param dataBase 数据库编号（可选）/ Database number (optional)
     * @return Redis 客户端实例，未找到时返回 null / Redis client instance, or null if not found
     */
    @Synchronized
    operator fun invoke(name: String, dataBase: Int? = null): RedisClient? {
        return if (dataBase != null) {
            clients[RedisClientKey(name = name, database = dataBase)]?.resource?.let { RedisClient(it) }
        } else {
            null
        } ?: clients.filterKeys { it.name == name }.entries.firstOrNull()?.value?.resource?.let { RedisClient(it) }
    }
}

/**
 * 设置字符串键值
 * Set string key-value
 *
 * @param name 键名 / Key name
 * @param value 值 / Value
 * @param ex 过期时间 / Expiration duration
 */
fun RedisClient.set(name: String, value: String, ex: Duration = 1.days) {
    this.jedis.set(name, value)
    this.jedis.expire(name, ex.inWholeSeconds)
}

/**
 * 设置列表键值
 * Set list key-value
 *
 * @param name 键名 / Key name
 * @param value 值列表 / Value list
 * @param ex 过期时间 / Expiration duration
 */
fun RedisClient.set(name: String, value: List<String>, ex: Duration = 1.days) {
    this.jedis.del(name)
    this.jedis.lpush(name, *value.toTypedArray())
    this.jedis.expire(name, ex.inWholeSeconds)
}

/**
 * 设置集合键值
 * Set set key-value
 *
 * @param name 键名 / Key name
 * @param value 值集合 / Value set
 * @param ex 过期时间 / Expiration duration
 */
fun RedisClient.set(name: String, value: Set<String>, ex: Duration = 1.days) {
    this.jedis.del(name)
    this.jedis.sadd(name, *value.toTypedArray())
    this.jedis.expire(name, ex.inWholeSeconds)
}

/**
 * 设置哈希表键值
 * Set hash map key-value
 *
 * @param name 键名 / Key name
 * @param value 值映射 / Value map
 * @param ex 过期时间 / Expiration duration
 */
fun RedisClient.set(name: String, value: Map<String, String>, ex: Duration = 1.days) {
    this.jedis.del(name)
    this.jedis.hset(name, value)
    this.jedis.expire(name, ex.inWholeSeconds)
}

/**
 * 设置序列化对象键值（使用默认序列化器）
 * Set serialized object key-value (using default serializer)
 *
 * @param T 对象类型 / Object type
 * @param name 键名 / Key name
 * @param value 对象值 / Object value
 * @param ex 过期时间 / Expiration duration
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> RedisClient.set(name: String, value: T, ex: Duration = 1.days) {
    val json = Json {
        ignoreUnknownKeys = true
    }
    set(name, { json.encodeToString(T::class.serializer(), it) }, value, ex)
}

/**
 * 设置序列化对象键值（使用自定义序列化函数）
 * Set serialized object key-value (using custom serialization function)
 *
 * @param T 对象类型 / Object type
 * @param name 键名 / Key name
 * @param serializer 序列化函数 / Serialization function
 * @param value 对象值 / Object value
 * @param ex 过期时间 / Expiration duration
 */
fun <T> RedisClient.set(name: String, serializer: (T) -> String, value: T, ex: Duration = 1.days) {
    this.jedis.del(name)
    this.jedis.set(name, serializer(value))
    this.jedis.expire(name, ex.inWholeSeconds)
}

/**
 * 获取字符串值
 * Get string value
 *
 * @param name 键名 / Key name
 * @return 值，不存在时返回 null / Value, or null if not exists
 */
fun RedisClient.get(name: String): String? {
    return this.jedis.get(name)
}

/**
 * 获取列表值
 * Get list value
 *
 * @param name 键名 / Key name
 * @return 值列表，不存在时返回 null / Value list, or null if not exists
 */
fun RedisClient.getList(name: String): List<String>? {
    return this.jedis.lrange(name, 0L, -1L)
}

/**
 * 获取集合值
 * Get set value
 *
 * @param name 键名 / Key name
 * @return 值集合，不存在时返回 null / Value set, or null if not exists
 */
fun RedisClient.getSet(name: String): Set<String>? {
    return this.jedis.smembers(name)
}

/**
 * 获取哈希表值
 * Get hash map value
 *
 * @param name 键名 / Key name
 * @return 值映射，不存在时返回 null / Value map, or null if not exists
 */
fun RedisClient.getMap(name: String): Map<String, String>? {
    return this.jedis.hgetAll(name)
}

/**
 * 获取反序列化对象（使用默认反序列化器）
 * Get deserialized object (using default deserializer)
 *
 * @param T 对象类型 / Object type
 * @param name 键名 / Key name
 * @return 对象值，不存在时返回 null / Object value, or null if not exists
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> RedisClient.getObj(name: String): T? {
    val json = Json {
        ignoreUnknownKeys = true
    }
    return getObj(name) { json.decodeFromString(T::class.serializer(), it) }
}

/**
 * 获取反序列化对象（使用自定义反序列化函数）
 * Get deserialized object (using custom deserialization function)
 *
 * @param T 对象类型 / Object type
 * @param name 键名 / Key name
 * @param deserializer 反序列化函数 / Deserialization function
 * @return 对象值，不存在时返回 null / Object value, or null if not exists
 */
fun <T> RedisClient.getObj(name: String, deserializer: (String) -> T): T? {
    return this.jedis.get(name)?.let { deserializer(it) }
}

/**
 * Redis 上下文
 * Redis context
 *
 * @property client Redis 客户端实例 / Redis client instance
 */
data class RedisContext(
    val client: RedisClient
) : AutoCloseable {
    override fun close() {
        client.jedis.close()
    }
}

/**
 * 使用默认 Redis 客户端执行操作
 * Execute operation using default Redis client
 *
 * @param block 操作块 / Operation block
 */
fun useRedis(block: RedisContext.() -> Unit) {
    Redis()?.let {
        RedisContext(it).use { ctx ->
            block(ctx)
        }
    }
}

/**
 * 使用指定键的 Redis 客户端执行操作
 * Execute operation using Redis client by key
 *
 * @param key 客户端键 / Client key
 * @param block 操作块 / Operation block
 */
fun useRedis(key: RedisClientKey, block: RedisContext.() -> Unit) {
    Redis(key)?.let {
        RedisContext(it).use { ctx ->
            block(ctx)
        }
    }
}

/**
 * 使用指定名称的 Redis 客户端执行操作
 * Execute operation using Redis client by name
 *
 * @param name 客户端名称 / Client name
 * @param block 操作块 / Operation block
 */
fun useRedis(name: String, block: RedisContext.() -> Unit) {
    Redis(name)?.let {
        RedisContext(it).use { ctx ->
            block(ctx)
        }
    }
}

/**
 * 使用指定名称和数据库编号的 Redis 客户端执行操作
 * Execute operation using Redis client by name and database number
 *
 * @param name 客户端名称 / Client name
 * @param database 数据库编号 / Database number
 * @param block 操作块 / Operation block
 */
fun useRedis(name: String, database: Int, block: RedisContext.() -> Unit) {
    Redis(name, database)?.let {
        RedisContext(it).use { ctx ->
            block(ctx)
        }
    }
}