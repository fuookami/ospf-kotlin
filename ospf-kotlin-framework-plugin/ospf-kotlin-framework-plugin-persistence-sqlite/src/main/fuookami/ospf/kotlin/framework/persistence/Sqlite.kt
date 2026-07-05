/**
 * SQLite 数据库客户端管理
 * SQLite database client management
 *
 * 提供 SQLite 数据源的初始化和管理功能。
 * Provides SQLite datasource initialization and management functionality.
 */
package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.Serializable
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.logging.log4j.kotlin.logger
import org.ktorm.database.Database
import org.ktorm.support.sqlite.SQLiteDialect

/**
 * SQLite 客户端键
 * SQLite client key
 *
 * @property name 客户端名称 / Client name
 */
data class SqliteClientKey(
    val name: String
)

/**
 * SQLite 配置构建器
 * SQLite configuration builder
 *
 * @property url 数据库文件路径 / Database file path
 * @property name 客户端名称 / Client name
 * @property properties 额外连接属性 / Additional connection properties
 * @property maxTotal 最大连接数 / Maximum total connections
 * @property maxIdle 最大空闲连接数 / Maximum idle connections
 * @property maxOpenPreparedStatements 最大预编译语句数 / Maximum open prepared statements
 */
data class SqliteConfigBuilder(
    var url: String? = null,
    var name: String? = null,
    val properties: MutableMap<String, String> = mutableMapOf(),
    var maxTotal: Int = 20,
    var maxIdle: Int = 10,
    var maxOpenPreparedStatements: Int = 100
) {
    private val logger = logger()

    /**
     * 构建 SQLite 配置
     * Build SQLite configuration
     *
     * @return 配置实例，参数不完整时返回 null / Configuration instance, or null if parameters are incomplete
     */
    operator fun invoke(): SqliteConfig? {
        return try {
            SqliteConfig(
                url = url!!,
                name = name ?: "",
                properties = properties,
                maxTotal = maxTotal,
                maxIdle = maxIdle,
                maxOpenPreparedStatements = maxOpenPreparedStatements
            )
        } catch (e: Exception) {
            if (url == null) {
                logger.error("url is not set")
            }
            null
        }
    }
}

/**
 * SQLite 配置数据
 * SQLite configuration data
 *
 * @property url 数据库文件路径 / Database file path
 * @property name 客户端名称 / Client name
 * @property properties 额外连接属性 / Additional connection properties
 * @property maxTotal 最大连接数 / Maximum total connections
 * @property maxIdle 最大空闲连接数 / Maximum idle connections
 * @property maxOpenPreparedStatements 最大预编译语句数 / Maximum open prepared statements
 */
@Serializable
data class SqliteConfig(
    val url: String,
    val name: String,
    val properties: Map<String, String> = emptyMap(),
    val maxTotal: Int = 20,
    val maxIdle: Int = 10,
    val maxOpenPreparedStatements: Int = 100
) {
    /** 客户端键 / Client key */
    val key get() = SqliteClientKey(name = name)
}

/**
 * SQLite 客户端管理器
 * SQLite client manager
 *
 * 管理多个 SQLite 数据源实例，按名称索引。
 * Manages multiple SQLite datasource instances, indexed by name.
 */
object Sqlite {
    @get:Synchronized
    private val clients: MutableMap<SqliteClientKey, BasicDataSource> = HashMap()

    /**
     * 初始化并获取 SQLite 数据库连接
     * Initialize and get SQLite database connection
     *
     * @param builder 配置构建器 lambda / Configuration builder lambda
     * @return Ktorm 数据库实例，初始化失败时返回 null / Ktorm database instance, or null if initialization fails
     */
    @Synchronized
    fun init(builder: SqliteConfigBuilder.() -> Unit): Database? {
        val config = SqliteConfigBuilder()
        builder(config)
        return config()?.let { this(it) }
    }

    /**
     * 获取或创建 SQLite 数据库连接
     * Get or create SQLite database connection
     *
     * @param config SQLite 配置 / SQLite configuration
     * @return Ktorm 数据库实例，创建失败时返回 null / Ktorm database instance, or null if creation fails
     */
    @Synchronized
    operator fun invoke(config: SqliteConfig): Database? {
        if (clients.containsKey(config.key)) {
            return Database.connect(clients[config.key]!!, SQLiteDialect())
        }

        return try {
            val dataSource = BasicDataSource().apply {
                driverClassName = "org.sqlite.JDBC"
                url = "jdbc:sqlite://${config.url}"
                maxTotal = config.maxTotal
                maxIdle = config.maxIdle
                maxOpenPreparedStatements = config.maxOpenPreparedStatements
                for ((key, value) in config.properties) {
                    addConnectionProperty(key, value)
                }
            }
            clients[config.key] = dataSource
            Database.connect(dataSource, SQLiteDialect())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 按键获取已注册的 SQLite 数据库连接
     * Get registered SQLite database connection by key
     *
     * @param key 客户端键（为 null 时返回第一个）/ Client key (returns first if null)
     * @return Ktorm 数据库实例，未找到时返回 null / Ktorm database instance, or null if not found
     */
    @Synchronized
    operator fun invoke(key: SqliteClientKey? = null): Database? {
        return (if (key != null) {
            clients[key]
        } else {
            null
        } ?: clients.values.firstOrNull())?.let {
            Database.connect(it, SQLiteDialect())
        }
    }

    /**
     * 按名称获取已注册的 SQLite 数据库连接
     * Get registered SQLite database connection by name
     *
     * @param name 客户端名称 / Client name
     * @return Ktorm 数据库实例，未找到时返回 null / Ktorm database instance, or null if not found
     */
    @Synchronized
    operator fun invoke(name: String): Database? {
        return clients.filterKeys { it.name == name }.entries.firstOrNull()?.value?.let {
            Database.connect(it, SQLiteDialect())
        }
    }
}