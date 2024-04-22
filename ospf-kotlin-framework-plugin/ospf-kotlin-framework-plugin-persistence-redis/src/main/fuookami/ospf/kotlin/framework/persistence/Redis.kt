package fuookami.ospf.kotlin.framework.persistence

import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import redis.clients.jedis.*
import redis.clients.jedis.util.*

data class RedisClientKey(
    val name: String,
    val database: Int
)

data class RedisConfigBuilder(
    var urls: List<String>? = null,
    var name: String? = null,
    var masterName: String? = null,
    var database: Int? = null,
    var password: String? = null
) {
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

data class RedisClient(
    val jedis: Jedis
) {
    protected fun finalize() {
        jedis.close()
    }
}

object Redis {
    @get:Synchronized
    private val clients: MutableMap<RedisClientKey, Pool<Jedis>> = HashMap()

    @Synchronized
    fun init(builder: RedisConfigBuilder.() -> Unit): RedisClient? {
        val config = RedisConfigBuilder()
        builder(config)
        return config()?.let { this(it) }
    }

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

    @Synchronized
    operator fun invoke(key: RedisClientKey? = null): RedisClient? {
        return if (key != null) {
            clients[key]?.resource?.let { RedisClient(it) }
        } else {
            null
        } ?: clients.values.firstOrNull()?.resource?.let { RedisClient(it) }
    }

    @Synchronized
    operator fun invoke(name: String, dataBase: Int? = null): RedisClient? {
        return if (dataBase != null) {
            clients[RedisClientKey(name = name, database = dataBase)]?.resource?.let { RedisClient(it) }
        } else {
            null
        } ?: clients.filterKeys { it.name == name }.entries.firstOrNull()?.value?.resource?.let { RedisClient(it) }
    }
}

fun RedisClient.set(name: String, value: String, ex: Duration = 1.days) {
    this.jedis.set(name, value)
    this.jedis.expire(name, ex.inWholeSeconds)
}

fun RedisClient.set(name: String, value: List<String>, ex: Duration = 1.days) {
    this.jedis.del(name)
    this.jedis.lpush(name, *value.toTypedArray())
    this.jedis.expire(name, ex.inWholeSeconds)
}

fun RedisClient.set(name: String, value: Set<String>, ex: Duration = 1.days) {
    this.jedis.del(name)
    this.jedis.sadd(name, *value.toTypedArray())
    this.jedis.expire(name, ex.inWholeSeconds)
}

fun RedisClient.set(name: String, value: Map<String, String>, ex: Duration = 1.days) {
    this.jedis.del(name)
    this.jedis.hset(name, value)
    this.jedis.expire(name, ex.inWholeSeconds)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T: Any> RedisClient.set(name: String, value: T, ex: Duration = 1.days) {
    val json = Json {
        ignoreUnknownKeys = true
    }
    set(name, { json.encodeToString(T::class.serializer(), it) }, value, ex)
}

fun <T> RedisClient.set(name: String, serializer: (T) -> String, value: T, ex: Duration = 1.days) {
    this.jedis.del(name)
    this.jedis.set(name, serializer(value))
    this.jedis.expire(name, ex.inWholeSeconds)
}

fun RedisClient.get(name: String): String? {
    return this.jedis.get(name)
}

fun RedisClient.getList(name: String): List<String>? {
    return this.jedis.lrange(name, 0L, -1L)
}

fun RedisClient.getSet(name: String): Set<String>? {
    return this.jedis.smembers(name)
}

fun RedisClient.getMap(name: String): Map<String, String>? {
    return this.jedis.hgetAll(name)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T: Any> RedisClient.getObj(name: String): T? {
    val json = Json {
        ignoreUnknownKeys = true
    }
    return getObj(name) { json.decodeFromString(T::class.serializer(), it) }
}

fun <T> RedisClient.getObj(name: String, deserializer: (String) -> T): T? {
    return this.jedis.get(name)?.let { deserializer(it) }
}

data class RedisContext(
    val client: RedisClient
) {
    protected fun finalize() {
        client.jedis.close()
    }
}

fun redisContext(context: RedisContext.() -> Unit) {
    Redis()?.let {
        val ctx = RedisContext(it)
        context(ctx)
    }
}

fun redisContext(key: RedisClientKey, context: RedisContext.() -> Unit) {
    Redis(key)?.let {
        val ctx = RedisContext(it)
        context(ctx)
    }
}

fun redisContext(name: String, context: RedisContext.() -> Unit) {
    Redis(name)?.let {
        val ctx = RedisContext(it)
        context(ctx)
    }
}

fun redisContext(name: String, database: Int, context: RedisContext.() -> Unit) {
    Redis(name, database)?.let {
        val ctx = RedisContext(it)
        context(ctx)
    }
}
