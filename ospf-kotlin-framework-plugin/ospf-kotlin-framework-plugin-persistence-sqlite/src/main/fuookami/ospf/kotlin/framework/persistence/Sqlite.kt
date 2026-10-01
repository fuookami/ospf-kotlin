/**
 * SQLite 数据库客户端管理 / SQLite database client management
 *
 * 提供 SQLite 数据源的初始化和管理功能。 / Provides SQLite datasource initialization and management functionality.
*/
package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.Serializable
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.logging.log4j.kotlin.logger
import org.ktorm.database.Database
import org.ktorm.support.sqlite.SQLiteDialect
import org.springframework.jdbc.core.simple.JdbcClient
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientBackend
import fuookami.ospf.kotlin.framework.persistence.query.RelationalQueryDialect
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisBackend
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisBackendFactory
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisDialect
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

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
     * 构建并校验 SQLite 配置 / Build and validate the SQLite configuration.
     *
     * @return 配置结果 / Configuration result
    */
    fun buildRet(): Ret<SqliteConfig> {
        val url = url?.takeIf { it.isNotBlank() }
            ?: return Failed(ErrorCode.IllegalArgument, "SQLite 文件路径未设置 / SQLite file path is not set")
        val name = name?.takeIf { it.isNotBlank() }
            ?: return Failed(ErrorCode.IllegalArgument, "SQLite 客户端名称未设置 / SQLite client name is not set")
        if (maxTotal <= 0 || maxIdle < 0 || maxIdle > maxTotal || maxOpenPreparedStatements < 0) {
            return Failed(ErrorCode.IllegalArgument, "SQLite 连接池配置无效 / SQLite connection pool settings are invalid")
        }
        return ok(
            SqliteConfig(
                url = url,
                name = name,
                properties = properties.toMap(),
                maxTotal = maxTotal,
                maxIdle = maxIdle,
                maxOpenPreparedStatements = maxOpenPreparedStatements
            )
        )
    }

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
 * SQLite 客户端管理器 / SQLite client manager
 *
 * 管理多个 SQLite 数据源实例，按名称索引。 / Manages multiple SQLite datasource instances, indexed by name.
*/
object Sqlite {
    @get:Synchronized
    private val clients: MutableMap<SqliteClientKey, BasicDataSource> = HashMap()

    private fun jdbcUrl(path: String): String {
        return if (path.startsWith("jdbc:sqlite:", ignoreCase = true)) {
            path
        } else {
            "jdbc:sqlite:${path.replace('\\', '/')}"
        }
    }

    private fun dataSource(config: SqliteConfig): Ret<BasicDataSource> {
        if (config.url.isBlank() || config.name.isBlank() || config.maxTotal <= 0 ||
            config.maxIdle < 0 || config.maxIdle > config.maxTotal || config.maxOpenPreparedStatements < 0
        ) {
            return Failed(ErrorCode.IllegalArgument, "SQLite 配置无效 / SQLite configuration is invalid")
        }
        clients[config.key]?.let { return ok(it) }
        return try {
            val dataSource = BasicDataSource().apply {
                driverClassName = "org.sqlite.JDBC"
                url = jdbcUrl(config.url)
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
            Failed(ErrorCode.ApplicationError, "无法创建 SQLite 数据源，请检查路径和驱动 / Unable to create SQLite datasource; check path and driver")
        }
    }

    private fun jdbcClient(dataSource: BasicDataSource): Ret<JdbcClientBackend> {
        return try {
            ok(JdbcClientBackend(JdbcClient.create(dataSource), SqliteJdbcClientDialect))
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "无法创建 SQLite JdbcClient / Unable to create SQLite JdbcClient")
        }
    }

    private fun mybatisBackend(dataSource: BasicDataSource): Ret<MybatisBackend> {
        return MybatisBackendFactory.create(dataSource, MybatisDialect.SQLite)
    }

    /**
     * 初始化并获取 SQLite 数据库连接 / Initialize and get SQLite database connection
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
     * 初始化并获取 JdbcClient 后端 / Initialize and get a JdbcClient backend.
     *
     * @param builder 配置构建器 lambda / Configuration builder lambda
     * @return JdbcClient 后端或结构化错误 / JdbcClient backend or structured failure
    */
    @Synchronized
    fun initJdbcClient(builder: SqliteConfigBuilder.() -> Unit): Ret<JdbcClientBackend> {
        val configBuilder = SqliteConfigBuilder()
        builder(configBuilder)
        return when (val config = configBuilder.buildRet()) {
            is Ok -> getJdbcClient(config.value)
            is Failed -> Failed(config.error)
            is Fatal -> Fatal(config.errors)
        }
    }

    /**
     * 按配置获取 JdbcClient 后端 / Get a JdbcClient backend by configuration.
     *
     * @param config SQLite 配置 / SQLite configuration
     * @return JdbcClient 后端或结构化错误 / JdbcClient backend or structured failure
    */
    @Synchronized
    fun getJdbcClient(config: SqliteConfig): Ret<JdbcClientBackend> {
        return when (val source = dataSource(config)) {
            is Ok -> jdbcClient(source.value)
            is Failed -> Failed(source.error)
            is Fatal -> Fatal(source.errors)
        }
    }

    /**
     * 按键获取 JdbcClient 后端 / Get a JdbcClient backend by key.
     *
     * @param key SQLite 客户端键 / SQLite client key
     * @return JdbcClient 后端或结构化错误 / JdbcClient backend or structured failure
    */
    @Synchronized
    fun getJdbcClient(key: SqliteClientKey): Ret<JdbcClientBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 SQLite 客户端 '${key.name}' / SQLite client '${key.name}' was not found")
        return jdbcClient(dataSource)
    }

    /**
     * 按名称获取 JdbcClient 后端 / Get a JdbcClient backend by name.
     *
     * @param name SQLite 客户端名称 / SQLite client name
     * @return JdbcClient 后端或结构化错误 / JdbcClient backend or structured failure
    */
    @Synchronized
    fun getJdbcClient(name: String): Ret<JdbcClientBackend> {
        return getJdbcClient(SqliteClientKey(name))
    }

    /**
     * 初始化并获取 Ktorm 后端 / Initialize and get a Ktorm backend.
     *
     * @param builder 配置构建器 lambda / Configuration builder lambda
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun initKtorm(builder: SqliteConfigBuilder.() -> Unit): Ret<KtormBackend> {
        val configBuilder = SqliteConfigBuilder()
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
     * @param config SQLite 配置 / SQLite configuration
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun getKtormBackend(config: SqliteConfig): Ret<KtormBackend> {
        return when (val source = dataSource(config)) {
            is Ok -> try {
                ok(KtormBackend(Database.connect(source.value, SQLiteDialect()), RelationalQueryDialect.SQLite))
            } catch (e: Exception) {
                Failed(ErrorCode.ApplicationError, "无法创建 SQLite Ktorm 后端 / Unable to create SQLite Ktorm backend")
            }
            is Failed -> Failed(source.error)
            is Fatal -> Fatal(source.errors)
        }
    }

    /**
     * 按键获取 Ktorm 后端 / Get a Ktorm backend by key.
     *
     * @param key SQLite 客户端键 / SQLite client key
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun getKtormBackend(key: SqliteClientKey): Ret<KtormBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 SQLite 客户端 '${key.name}' / SQLite client '${key.name}' was not found")
        return try {
            ok(KtormBackend(Database.connect(dataSource, SQLiteDialect()), RelationalQueryDialect.SQLite))
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "无法创建 SQLite Ktorm 后端 / Unable to create SQLite Ktorm backend")
        }
    }

    /**
     * 按名称获取 Ktorm 后端 / Get a Ktorm backend by name.
     *
     * @param name SQLite 客户端名称 / SQLite client name
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun getKtormBackend(name: String): Ret<KtormBackend> {
        return getKtormBackend(SqliteClientKey(name))
    }

    /**
     * 初始化并获取 MyBatis 后端 / Initialize and get a MyBatis backend.
     *
     * @param builder 配置构建器 lambda / Configuration builder lambda
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun initMybatis(builder: SqliteConfigBuilder.() -> Unit): Ret<MybatisBackend> {
        val configBuilder = SqliteConfigBuilder()
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
     * @param config SQLite 配置 / SQLite configuration
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun getMybatisBackend(config: SqliteConfig): Ret<MybatisBackend> {
        return when (val source = dataSource(config)) {
            is Ok -> mybatisBackend(source.value)
            is Failed -> Failed(source.error)
            is Fatal -> Fatal(source.errors)
        }
    }

    /**
     * 按键获取 MyBatis 后端 / Get a MyBatis backend by key.
     *
     * @param key SQLite 客户端键 / SQLite client key
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun getMybatisBackend(key: SqliteClientKey): Ret<MybatisBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 SQLite 客户端 '${key.name}' / SQLite client '${key.name}' was not found")
        return mybatisBackend(dataSource)
    }

    /**
     * 按名称获取 MyBatis 后端 / Get a MyBatis backend by name.
     *
     * @param name SQLite 客户端名称 / SQLite client name
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun getMybatisBackend(name: String): Ret<MybatisBackend> {
        return getMybatisBackend(SqliteClientKey(name))
    }

    /**
     * 关闭并移除 SQLite 数据源 / Close and remove a SQLite datasource.
     *
     * @param key SQLite 客户端键 / SQLite client key
     * @return 关闭结果，未注册时也成功 / Close result; succeeds when the key is not registered
    */
    @Synchronized
    fun close(key: SqliteClientKey): Try {
        val dataSource = clients[key] ?: return ok
        return try {
            dataSource.close()
            clients.remove(key)
            ok
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "关闭 SQLite 数据源失败 / Failed to close SQLite datasource")
        }
    }

    /**
     * 获取或创建 SQLite 数据库连接 / Get or create SQLite database connection
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
                url = jdbcUrl(config.url)
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
     * 按键获取已注册的 SQLite 数据库连接 / Get registered SQLite database connection by key
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
     * 按名称获取已注册的 SQLite 数据库连接 / Get registered SQLite database connection by name
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
