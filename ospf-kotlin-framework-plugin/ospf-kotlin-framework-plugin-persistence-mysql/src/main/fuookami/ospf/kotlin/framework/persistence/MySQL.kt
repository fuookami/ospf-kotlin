/**
 * MySQL 数据库客户端管理
 * MySQL database client management
 *
 * 提供 MySQL 数据源的初始化和管理功能。
 * Provides MySQL datasource initialization and management functionality.
 */
package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.Serializable
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.logging.log4j.kotlin.logger
import org.ktorm.database.Database
import org.ktorm.support.mysql.MySqlDialect

/**
 * MySQL 客户端键
 * MySQL client key
 *
 * @property name 客户端名称 / Client name
 * @property database 数据库名称 / Database name
 */
data class MySQLClientKey(
    val name: String,
    val database: String
)

/**
 * MySQL 配置构建器
 * MySQL configuration builder
 *
 * @property url 数据库连接 URL / Database connection URL
 * @property name 客户端名称 / Client name
 * @property database 数据库名称 / Database name
 * @property userName 用户名 / Username
 * @property password 密码 / Password
 * @property properties 额外连接属性 / Additional connection properties
 * @property maxTotal 最大连接数 / Maximum total connections
 * @property maxIdle 最大空闲连接数 / Maximum idle connections
 * @property maxOpenPreparedStatements 最大预编译语句数 / Maximum open prepared statements
 */
data class MySQLConfigBuilder(
    var url: String? = null,
    var name: String? = null,
    var database: String? = null,
    var userName: String? = null,
    var password: String? = null,
    val properties: MutableMap<String, String> = mutableMapOf(),
    val maxTotal: Int = 20,
    val maxIdle: Int = 10,
    val maxOpenPreparedStatements: Int = 100
) {
    private val logger = logger()

    /**
     * 构建 MySQL 配置
     * Build MySQL configuration
     *
     * @return 配置实例，参数不完整时返回 null / Configuration instance, or null if parameters are incomplete
     */
    operator fun invoke(): MySQLConfig? {
        return try {
            MySQLConfig(
                url = url!!,
                name = name!!,
                database = database!!,
                userName = userName!!,
                password = password!!,
                properties = properties,
                maxTotal = maxTotal,
                maxIdle = maxIdle,
                maxOpenPreparedStatements = maxOpenPreparedStatements
            )
        } catch (e: Exception) {
            if (url == null) {
                logger.error("MySQL url is not set")
            }
            if (name == null) {
                logger.error("MySQL name is not set")
            }
            if (database == null) {
                logger.error("MySQL database is not set")
            }
            if (userName == null) {
                logger.error("MySQL user name is not set")
            }
            if (password == null) {
                logger.error("MySQL password is not set")
            }
            null
        }
    }
}

/**
 * MySQL 配置数据
 * MySQL configuration data
 *
 * @property url 数据库连接 URL / Database connection URL
 * @property name 客户端名称 / Client name
 * @property database 数据库名称 / Database name
 * @property userName 用户名 / Username
 * @property password 密码 / Password
 * @property properties 额外连接属性 / Additional connection properties
 * @property maxTotal 最大连接数 / Maximum total connections
 * @property maxIdle 最大空闲连接数 / Maximum idle connections
 * @property maxOpenPreparedStatements 最大预编译语句数 / Maximum open prepared statements
 */
@Serializable
data class MySQLConfig(
    val url: String,
    val name: String,
    val database: String,
    val userName: String,
    val password: String,
    val properties: Map<String, String> = emptyMap(),
    val maxTotal: Int = 20,
    val maxIdle: Int = 10,
    val maxOpenPreparedStatements: Int = 100
) {
    val key get() = MySQLClientKey(name = name, database = database)
}

/**
 * MySQL 客户端管理器
 * MySQL client manager
 *
 * 管理多个 MySQL 数据源实例，按名称和数据库索引。
 * Manages multiple MySQL datasource instances, indexed by name and database.
 */
object MySQL {
    @get:Synchronized
    private val clients: MutableMap<MySQLClientKey, BasicDataSource> = HashMap()

    /**
     * 初始化并获取 MySQL 数据库连接
     * Initialize and get MySQL database connection
     *
     * @param builder 配置构建器 lambda / Configuration builder lambda
     * @return Ktorm 数据库实例，初始化失败时返回 null / Ktorm database instance, or null if initialization fails
     */
    @Synchronized
    fun init(builder: MySQLConfigBuilder.() -> Unit): Database? {
        val config = MySQLConfigBuilder()
        builder(config)
        return config()?.let { this(it) }
    }

    /**
     * 获取或创建 MySQL 数据库连接
     * Get or create MySQL database connection
     *
     * @param config MySQL 配置 / MySQL configuration
     * @return Ktorm 数据库实例，创建失败时返回 null / Ktorm database instance, or null if creation fails
     */
    @Synchronized
    operator fun invoke(config: MySQLConfig): Database? {
        if (clients.containsKey(config.key)) {
            return Database.connect(clients[config.key]!!, MySqlDialect())
        }

        return try {
            val dataSource = BasicDataSource().apply {
                driverClassName = "com.mysql.cj.jdbc.Driver"
                url = "jdbc:mysql://${config.url}/${config.database}?charset=utf8mb4"
                username = config.userName
                password = config.password
                maxTotal = config.maxTotal
                maxIdle = config.maxIdle
                maxOpenPreparedStatements = config.maxOpenPreparedStatements
                for ((key, value) in config.properties) {
                    addConnectionProperty(key, value)
                }
            }
            clients[config.key] = dataSource
            Database.connect(dataSource, MySqlDialect())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 按键获取已注册的 MySQL 数据库连接
     * Get registered MySQL database connection by key
     *
     * @param key 客户端键（为 null 时返回第一个）/ Client key (returns first if null)
     * @return Ktorm 数据库实例，未找到时返回 null / Ktorm database instance, or null if not found
     */
    @Synchronized
    operator fun invoke(key: MySQLClientKey? = null): Database? {
        return (if (key != null) {
            clients[key]
        } else {
            null
        } ?: clients.values.firstOrNull())?.let {
            Database.connect(it, MySqlDialect())
        }
    }

    /**
     * 按名称获取已注册的 MySQL 数据库连接
     * Get registered MySQL database connection by name
     *
     * @param name 客户端名称 / Client name
     * @param dataBase 数据库名称（可选）/ Database name (optional)
     * @return Ktorm 数据库实例，未找到时返回 null / Ktorm database instance, or null if not found
     */
    @Synchronized
    operator fun invoke(name: String, dataBase: String? = null): Database? {
        return (if (dataBase != null) {
            clients[MySQLClientKey(name = name, database = dataBase)]
        } else {
            null
        } ?: clients.filterKeys { it.name == name }.entries.firstOrNull()?.value)?.let {
            Database.connect(it, MySqlDialect())
        }
    }
}