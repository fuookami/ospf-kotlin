/**
 * MySQL 数据库客户端管理 / MySQL database client management
 *
 * 提供 MySQL 数据源的初始化和管理功能。 / Provides MySQL datasource initialization and management functionality.
*/
package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.Serializable
import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database
import org.ktorm.support.mysql.MySqlDialect
import org.springframework.jdbc.core.simple.JdbcClient
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.SortDirection
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientBackend
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientDialect
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientSqlFragment
import fuookami.ospf.kotlin.framework.persistence.query.RelationalQueryDialect
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisBackend
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisBackendFactory
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisDialect

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
 * MySQL 配置构建器 / MySQL configuration builder
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
    /**
     * 构建 MySQL 配置
     * Build MySQL configuration
     *
     * @return 配置实例，参数不完整时返回 null / Configuration instance, or null if parameters are incomplete
    */
    operator fun invoke(): MySQLConfig? {
        return buildRet().value
    }

    /**
     * 构建并校验 MySQL 配置 / Build and validate MySQL configuration.
     *
     * @return 配置结果 / Configuration result
    */
    fun buildRet(): Ret<MySQLConfig> {
        val url = url?.takeIf { it.isNotBlank() }
            ?: return Failed(ErrorCode.IllegalArgument, "MySQL URL 未设置 / MySQL URL is not set")
        val name = name?.takeIf { it.isNotBlank() }
            ?: return Failed(ErrorCode.IllegalArgument, "MySQL 客户端名称未设置 / MySQL client name is not set")
        val database = database?.takeIf { it.isNotBlank() }
            ?: return Failed(ErrorCode.IllegalArgument, "MySQL 数据库名称未设置 / MySQL database name is not set")
        val userName = userName
            ?: return Failed(ErrorCode.IllegalArgument, "MySQL 用户名未设置 / MySQL username is not set")
        val password = password
            ?: return Failed(ErrorCode.IllegalArgument, "MySQL 密码未设置 / MySQL password is not set")
        if (maxTotal <= 0 || maxIdle < 0 || maxOpenPreparedStatements < 0) {
            return Failed(ErrorCode.IllegalArgument, "MySQL 连接池配置无效 / MySQL connection pool settings are invalid")
        }
        return ok(
            MySQLConfig(
                url = url,
                name = name,
                database = database,
                userName = userName,
                password = password,
                properties = properties.toMap(),
                maxTotal = maxTotal,
                maxIdle = maxIdle,
                maxOpenPreparedStatements = maxOpenPreparedStatements
            )
        )
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
 * MySQL 客户端管理器 / MySQL client manager
 *
 * 管理多个 MySQL 数据源实例，按名称和数据库索引。 / Manages multiple MySQL datasource instances, indexed by name and database.
*/
object MySQL {
    @get:Synchronized
    private val clients: MutableMap<MySQLClientKey, BasicDataSource> = LinkedHashMap()

    private fun dataSource(config: MySQLConfig): Ret<BasicDataSource> {
        if (config.url.isBlank() || config.name.isBlank() || config.database.isBlank() ||
            config.maxTotal <= 0 || config.maxIdle < 0 || config.maxIdle > config.maxTotal ||
            config.maxOpenPreparedStatements < 0
        ) {
            return Failed(ErrorCode.IllegalArgument, "MySQL 配置无效 / MySQL configuration is invalid")
        }
        clients[config.key]?.let { return ok(it) }
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
            ok(dataSource)
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "无法创建 MySQL 数据源，请检查配置和驱动 / Unable to create MySQL datasource; check configuration and driver")
        }
    }

    private fun jdbcClient(dataSource: BasicDataSource): Ret<JdbcClientBackend> {
        return try {
            ok(JdbcClientBackend(JdbcClient.create(dataSource), MySQLJdbcClientDialect))
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "无法创建 MySQL JdbcClient / Unable to create MySQL JdbcClient")
        }
    }

    /**
     * 初始化并获取 MySQL 数据库连接 / Initialize and get MySQL database connection
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
     * 初始化并获取 JdbcClient / Initialize and get a JdbcClient.
     *
     * @param builder 配置构建器 lambda / Configuration builder lambda
     * @return JdbcClient 后端或结构化错误 / JdbcClient backend or structured error
    */
    @Synchronized
    fun initJdbcClient(builder: MySQLConfigBuilder.() -> Unit): Ret<JdbcClientBackend> {
        val configBuilder = MySQLConfigBuilder()
        builder(configBuilder)
        return when (val config = configBuilder.buildRet()) {
            is Ok -> getJdbcClient(config.value)
            is Failed -> Failed(config.error)
            is Fatal -> Fatal(config.errors)
        }
    }

    /**
     * 初始化并获取 Ktorm 后端 / Initialize and get a Ktorm backend.
     *
     * @param builder MySQL 配置构建器 / MySQL configuration builder
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun initKtorm(builder: MySQLConfigBuilder.() -> Unit): Ret<KtormBackend> {
        val configBuilder = MySQLConfigBuilder()
        builder(configBuilder)
        return when (val config = configBuilder.buildRet()) {
            is Ok -> getKtormBackend(config.value)
            is Failed -> Failed(config.error)
            is Fatal -> Fatal(config.errors)
        }
    }

    /**
     * 按配置获取 Ktorm 后端 / Get a Ktorm backend by configuration.
     *
     * @param config MySQL 配置 / MySQL configuration
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun getKtormBackend(config: MySQLConfig): Ret<KtormBackend> {
        return when (val source = dataSource(config)) {
            is Ok -> try {
                ok(KtormBackend(Database.connect(source.value, MySqlDialect()), RelationalQueryDialect.MySQL))
            } catch (e: Exception) {
                Failed(ErrorCode.ApplicationError, "无法创建 MySQL Ktorm 后端 / Unable to create MySQL Ktorm backend")
            }
            is Failed -> Failed(source.error)
            is Fatal -> Fatal(source.errors)
        }
    }

    /**
     * 按键获取 Ktorm 后端 / Get a Ktorm backend by key.
     *
     * @param key MySQL 客户端键 / MySQL client key
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun getKtormBackend(key: MySQLClientKey): Ret<KtormBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 MySQL 客户端 '${key.name}/${key.database}' / MySQL client '${key.name}/${key.database}' was not found")
        return try {
            ok(KtormBackend(Database.connect(dataSource, MySqlDialect()), RelationalQueryDialect.MySQL))
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "无法创建 MySQL Ktorm 后端 / Unable to create MySQL Ktorm backend")
        }
    }

    /**
     * 按名称获取 Ktorm 后端 / Get a Ktorm backend by client name.
     *
     * @param name 客户端名称 / Client name
     * @param database 数据库名称，可省略 / Optional database name
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun getKtormBackend(name: String, database: String? = null): Ret<KtormBackend> {
        val key = if (database != null) {
            MySQLClientKey(name, database).takeIf { clients.containsKey(it) }
        } else {
            clients.keys.firstOrNull { it.name == name }
        } ?: return Failed(ErrorCode.DataNotFound, "未找到 MySQL 客户端 '$name' / MySQL client '$name' was not found")
        return getKtormBackend(key)
    }

    /**
     * 初始化并获取 MyBatis 后端 / Initialize and get a MyBatis backend.
     *
     * @param builder MySQL 配置构建器 / MySQL configuration builder
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun initMybatis(builder: MySQLConfigBuilder.() -> Unit): Ret<MybatisBackend> {
        val configBuilder = MySQLConfigBuilder()
        builder(configBuilder)
        return when (val config = configBuilder.buildRet()) {
            is Ok -> getMybatisBackend(config.value)
            is Failed -> Failed(config.error)
            is Fatal -> Fatal(config.errors)
        }
    }

    /**
     * 按配置获取 MyBatis 后端 / Get a MyBatis backend by configuration.
     *
     * @param config MySQL 配置 / MySQL configuration
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun getMybatisBackend(config: MySQLConfig): Ret<MybatisBackend> {
        return when (val source = dataSource(config)) {
            is Ok -> MybatisBackendFactory.create(source.value, MybatisDialect.MySQL)
            is Failed -> Failed(source.error)
            is Fatal -> Fatal(source.errors)
        }
    }

    /**
     * 按键获取 MyBatis 后端 / Get a MyBatis backend by key.
     *
     * @param key MySQL 客户端键 / MySQL client key
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun getMybatisBackend(key: MySQLClientKey): Ret<MybatisBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 MySQL 客户端 '${key.name}/${key.database}' / MySQL client '${key.name}/${key.database}' was not found")
        return MybatisBackendFactory.create(dataSource, MybatisDialect.MySQL)
    }

    /**
     * 按名称获取 MyBatis 后端 / Get a MyBatis backend by client name.
     *
     * @param name 客户端名称 / Client name
     * @param database 数据库名称，可省略 / Optional database name
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun getMybatisBackend(name: String, database: String? = null): Ret<MybatisBackend> {
        val key = if (database != null) {
            MySQLClientKey(name, database).takeIf { clients.containsKey(it) }
        } else {
            clients.keys.firstOrNull { it.name == name }
        } ?: return Failed(ErrorCode.DataNotFound, "未找到 MySQL 客户端 '$name' / MySQL client '$name' was not found")
        return getMybatisBackend(key)
    }

    /**
     * 获取或创建 JdbcClient 后端 / Get or create a JdbcClient backend.
     *
     * @param config MySQL 配置 / MySQL configuration
     * @return JdbcClient 后端或结构化错误 / JdbcClient backend or structured error
    */
    @Synchronized
    fun getJdbcClient(config: MySQLConfig): Ret<JdbcClientBackend> {
        return when (val source = dataSource(config)) {
            is Ok -> jdbcClient(source.value)
            is Failed -> Failed(source.error)
            is Fatal -> Fatal(source.errors)
        }
    }

    /**
     * 按键获取已注册的 JdbcClient 后端 / Get a registered JdbcClient backend by key.
     *
     * @param key 客户端键 / Client key
     * @return JdbcClient 后端或未找到错误 / JdbcClient backend or not-found error
    */
    @Synchronized
    fun getJdbcClient(key: MySQLClientKey): Ret<JdbcClientBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 MySQL 客户端 '${key.name}/${key.database}' / MySQL client '${key.name}/${key.database}' was not found")
        return jdbcClient(dataSource)
    }

    /**
     * 按名称获取已注册的 JdbcClient 后端 / Get a registered JdbcClient backend by name.
     *
     * @param name 客户端名称 / Client name
     * @param database 数据库名称，可省略 / Optional database name
     * @return JdbcClient 后端或未找到错误 / JdbcClient backend or not-found error
    */
    @Synchronized
    fun getJdbcClient(name: String, database: String? = null): Ret<JdbcClientBackend> {
        val key = if (database != null) {
            MySQLClientKey(name, database).takeIf { clients.containsKey(it) }
        } else {
            clients.keys.firstOrNull { it.name == name }
        } ?: return Failed(ErrorCode.DataNotFound, "未找到 MySQL 客户端 '$name' / MySQL client '$name' was not found")
        return getJdbcClient(key)
    }

    /**
     * 关闭并移除已注册的数据源 / Close and remove a registered datasource.
     *
     * @param key 客户端键 / Client key
     * @return 关闭结果；未注册时也返回成功 / Close result; succeeds when the key is not registered
    */
    @Synchronized
    fun close(key: MySQLClientKey): Try {
        val dataSource = clients[key] ?: return ok
        return try {
            dataSource.close()
            clients.remove(key)
            ok
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "关闭 MySQL 数据源失败 / Failed to close MySQL datasource")
        }
    }

    /**
     * 获取或创建 MySQL 数据库连接 / Get or create MySQL database connection
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
     * 按键获取已注册的 MySQL 数据库连接 / Get registered MySQL database connection by key
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
     * 按名称获取已注册的 MySQL 数据库连接 / Get registered MySQL database connection by name
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
