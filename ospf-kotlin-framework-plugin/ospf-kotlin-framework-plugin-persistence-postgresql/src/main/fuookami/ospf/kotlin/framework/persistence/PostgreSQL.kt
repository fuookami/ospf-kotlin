/**
 * PostgreSQL JdbcClient 管理 / PostgreSQL JdbcClient management
 *
 * 使用 DBCP 管理连接池，并按客户端键复用数据源。 / Uses DBCP to manage pools and reuses datasources by client key.
*/
package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.Serializable
import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database
import org.ktorm.support.postgresql.PostgreSqlDialect
import org.springframework.jdbc.core.simple.JdbcClient
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.persistence.query.RelationalQueryDialect
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientBackend
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisBackend
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisBackendFactory
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisDialect

/**
 * PostgreSQL 客户端键 / PostgreSQL client key.
 *
 * @property name 客户端名称 / Client name
 * @property database 数据库名称 / Database name
*/
data class PostgreSQLClientKey(
    val name: String,
    val database: String
)

/**
 * PostgreSQL 配置构建器 / PostgreSQL configuration builder.
 *
 * @property url 数据库主机和端口 / Database host and port
 * @property name 客户端名称 / Client name
 * @property database 数据库名称 / Database name
 * @property userName 用户名 / Username
 * @property password 密码 / Password
 * @property properties 额外连接属性 / Additional connection properties
 * @property maxTotal 最大连接数 / Maximum total connections
 * @property maxIdle 最大空闲连接数 / Maximum idle connections
 * @property maxOpenPreparedStatements 最大预编译语句数 / Maximum open prepared statements
*/
data class PostgreSQLConfigBuilder(
    var url: String? = null,
    var name: String? = null,
    var database: String? = null,
    var userName: String? = null,
    var password: String? = null,
    val properties: MutableMap<String, String> = mutableMapOf(),
    var maxTotal: Int = 20,
    var maxIdle: Int = 10,
    var maxOpenPreparedStatements: Int = 100
) {
    /**
     * 构建并校验 PostgreSQL 配置 / Build and validate PostgreSQL configuration.
     *
     * @return 配置结果 / Configuration result
    */
    fun buildRet(): Ret<PostgreSQLConfig> {
        val url = url?.takeIf { it.isNotBlank() }
            ?: return Failed(ErrorCode.IllegalArgument, "PostgreSQL URL 未设置 / PostgreSQL URL is not set")
        val name = name?.takeIf { it.isNotBlank() }
            ?: return Failed(ErrorCode.IllegalArgument, "PostgreSQL 客户端名称未设置 / PostgreSQL client name is not set")
        val database = database?.takeIf { it.isNotBlank() }
            ?: return Failed(ErrorCode.IllegalArgument, "PostgreSQL 数据库名称未设置 / PostgreSQL database name is not set")
        val userName = userName
            ?: return Failed(ErrorCode.IllegalArgument, "PostgreSQL 用户名未设置 / PostgreSQL username is not set")
        val password = password
            ?: return Failed(ErrorCode.IllegalArgument, "PostgreSQL 密码未设置 / PostgreSQL password is not set")
        if (maxTotal <= 0 || maxIdle < 0 || maxIdle > maxTotal || maxOpenPreparedStatements < 0) {
            return Failed(ErrorCode.IllegalArgument, "PostgreSQL 连接池配置无效 / PostgreSQL connection pool settings are invalid")
        }
        return ok(
            PostgreSQLConfig(
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

    /**
     * 兼容可空配置构建入口 / Nullable configuration builder for compatibility.
     *
     * @return 配置实例或 null / Configuration instance or null
    */
    operator fun invoke(): PostgreSQLConfig? {
        return buildRet().value
    }
}

/**
 * PostgreSQL 配置 / PostgreSQL configuration.
 *
 * @property url 数据库主机和端口 / Database host and port
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
data class PostgreSQLConfig(
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
    /** 客户端键 / Client key */
    val key get() = PostgreSQLClientKey(name = name, database = database)
}

/**
 * PostgreSQL 客户端管理器 / PostgreSQL client manager.
 *
 * 管理 DBCP 数据源，并创建使用 PostgreSQL 方言的 JdbcClient。 / Manages DBCP datasources and creates JdbcClient instances using the PostgreSQL dialect.
*/
object PostgreSQL {
    private val clients: MutableMap<PostgreSQLClientKey, BasicDataSource> = LinkedHashMap()

    private fun dataSource(config: PostgreSQLConfig): Ret<BasicDataSource> {
        if (config.url.isBlank() || config.name.isBlank() || config.database.isBlank() ||
            config.maxTotal <= 0 || config.maxIdle < 0 || config.maxIdle > config.maxTotal ||
            config.maxOpenPreparedStatements < 0
        ) {
            return Failed(ErrorCode.IllegalArgument, "PostgreSQL 配置无效 / PostgreSQL configuration is invalid")
        }
        clients[config.key]?.let { return ok(it) }
        return try {
            val dataSource = BasicDataSource().apply {
                driverClassName = "org.postgresql.Driver"
                url = "jdbc:postgresql://${config.url}/${config.database}"
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
            Failed(ErrorCode.ApplicationError, "无法创建 PostgreSQL 数据源，请检查配置和驱动 / Unable to create PostgreSQL datasource; check configuration and driver")
        }
    }

    private fun jdbcClient(dataSource: BasicDataSource): Ret<JdbcClientBackend> {
        return try {
            ok(JdbcClientBackend(JdbcClient.create(dataSource), PostgreSQLJdbcClientDialect))
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "无法创建 PostgreSQL JdbcClient / Unable to create PostgreSQL JdbcClient")
        }
    }

    /**
     * 通过配置构建器初始化 JdbcClient / Initialize a JdbcClient from a configuration builder.
     *
     * @param builder 配置构建器 lambda / Configuration builder lambda
     * @return JdbcClient 后端或结构化错误 / JdbcClient backend or structured error
    */
    @Synchronized
    fun initJdbcClient(builder: PostgreSQLConfigBuilder.() -> Unit): Ret<JdbcClientBackend> {
        val configBuilder = PostgreSQLConfigBuilder()
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
     * @param builder PostgreSQL 配置构建器 / PostgreSQL configuration builder
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun initKtorm(builder: PostgreSQLConfigBuilder.() -> Unit): Ret<KtormBackend> {
        val configBuilder = PostgreSQLConfigBuilder()
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
     * @param config PostgreSQL 配置 / PostgreSQL configuration
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun getKtormBackend(config: PostgreSQLConfig): Ret<KtormBackend> {
        return when (val source = dataSource(config)) {
            is Ok -> try {
                ok(KtormBackend(Database.connect(source.value, PostgreSqlDialect()), RelationalQueryDialect.PostgreSQL))
            } catch (e: Exception) {
                Failed(ErrorCode.ApplicationError, "无法创建 PostgreSQL Ktorm 后端 / Unable to create PostgreSQL Ktorm backend")
            }
            is Failed -> Failed(source.error)
            is Fatal -> Fatal(source.errors)
        }
    }

    /**
     * 按键获取 Ktorm 后端 / Get a Ktorm backend by key.
     *
     * @param key PostgreSQL 客户端键 / PostgreSQL client key
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun getKtormBackend(key: PostgreSQLClientKey): Ret<KtormBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 PostgreSQL 客户端 '${key.name}/${key.database}' / PostgreSQL client '${key.name}/${key.database}' was not found")
        return try {
            ok(KtormBackend(Database.connect(dataSource, PostgreSqlDialect()), RelationalQueryDialect.PostgreSQL))
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "无法创建 PostgreSQL Ktorm 后端 / Unable to create PostgreSQL Ktorm backend")
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
            PostgreSQLClientKey(name, database).takeIf { clients.containsKey(it) }
        } else {
            clients.keys.firstOrNull { it.name == name }
        } ?: return Failed(ErrorCode.DataNotFound, "未找到 PostgreSQL 客户端 '$name' / PostgreSQL client '$name' was not found")
        return getKtormBackend(key)
    }

    /**
     * 初始化并获取 MyBatis 后端 / Initialize and get a MyBatis backend.
     *
     * @param builder PostgreSQL 配置构建器 / PostgreSQL configuration builder
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun initMybatis(builder: PostgreSQLConfigBuilder.() -> Unit): Ret<MybatisBackend> {
        val configBuilder = PostgreSQLConfigBuilder()
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
     * @param config PostgreSQL 配置 / PostgreSQL configuration
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun getMybatisBackend(config: PostgreSQLConfig): Ret<MybatisBackend> {
        return when (val source = dataSource(config)) {
            is Ok -> MybatisBackendFactory.create(source.value, MybatisDialect.PostgreSQL)
            is Failed -> Failed(source.error)
            is Fatal -> Fatal(source.errors)
        }
    }

    /**
     * 按键获取 MyBatis 后端 / Get a MyBatis backend by key.
     *
     * @param key PostgreSQL 客户端键 / PostgreSQL client key
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun getMybatisBackend(key: PostgreSQLClientKey): Ret<MybatisBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 PostgreSQL 客户端 '${key.name}/${key.database}' / PostgreSQL client '${key.name}/${key.database}' was not found")
        return MybatisBackendFactory.create(dataSource, MybatisDialect.PostgreSQL)
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
            PostgreSQLClientKey(name, database).takeIf { clients.containsKey(it) }
        } else {
            clients.keys.firstOrNull { it.name == name }
        } ?: return Failed(ErrorCode.DataNotFound, "未找到 PostgreSQL 客户端 '$name' / PostgreSQL client '$name' was not found")
        return getMybatisBackend(key)
    }

    /**
     * 获取或创建 JdbcClient 后端 / Get or create a JdbcClient backend.
     *
     * @param config PostgreSQL 配置 / PostgreSQL configuration
     * @return JdbcClient 后端或结构化错误 / JdbcClient backend or structured error
    */
    @Synchronized
    fun getJdbcClient(config: PostgreSQLConfig): Ret<JdbcClientBackend> {
        return when (val source = dataSource(config)) {
            is Ok -> jdbcClient(source.value)
            is Failed -> Failed(source.error)
            is Fatal -> Fatal(source.errors)
        }
    }

    /**
     * 按客户端键获取 JdbcClient 后端 / Get a JdbcClient backend by client key.
     *
     * @param key 客户端键 / Client key
     * @return JdbcClient 后端或未找到错误 / JdbcClient backend or not-found error
    */
    @Synchronized
    fun getJdbcClient(key: PostgreSQLClientKey): Ret<JdbcClientBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 PostgreSQL 客户端 '${key.name}/${key.database}' / PostgreSQL client '${key.name}/${key.database}' was not found")
        return jdbcClient(dataSource)
    }

    /**
     * 按名称获取 JdbcClient 后端 / Get a JdbcClient backend by client name.
     *
     * @param name 客户端名称 / Client name
     * @param database 数据库名称，可省略 / Optional database name
     * @return JdbcClient 后端或未找到错误 / JdbcClient backend or not-found error
    */
    @Synchronized
    fun getJdbcClient(name: String, database: String? = null): Ret<JdbcClientBackend> {
        val key = if (database != null) {
            PostgreSQLClientKey(name, database).takeIf { clients.containsKey(it) }
        } else {
            clients.keys.firstOrNull { it.name == name }
        } ?: return Failed(ErrorCode.DataNotFound, "未找到 PostgreSQL 客户端 '$name' / PostgreSQL client '$name' was not found")
        return getJdbcClient(key)
    }

    /**
     * 关闭并移除数据源；未注册时幂等成功 / Close and remove a datasource; succeeds when not registered.
     *
     * @param key 客户端键 / Client key
     * @return 关闭结果 / Close result
    */
    @Synchronized
    fun close(key: PostgreSQLClientKey): Try {
        val dataSource = clients[key] ?: return ok
        return try {
            dataSource.close()
            clients.remove(key)
            ok
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "关闭 PostgreSQL 数据源失败 / Failed to close PostgreSQL datasource")
        }
    }
}
