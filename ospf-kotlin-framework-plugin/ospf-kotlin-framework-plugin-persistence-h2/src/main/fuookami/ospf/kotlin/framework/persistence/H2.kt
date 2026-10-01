/**
 * H2 JdbcClient 管理 / H2 JdbcClient management
 *
 * 使用 DBCP 管理连接池，并按客户端名称复用数据源。 / Uses DBCP to manage pools and reuses datasources by client name.
*/
package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.Serializable
import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database
import org.springframework.jdbc.core.simple.JdbcClient
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.persistence.query.RelationalQueryDialect
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientBackend
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisBackend
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisBackendFactory
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisDialect

/**
 * H2 客户端键 / H2 client key.
 *
 * @property name 客户端名称 / Client name
*/
data class H2ClientKey(
    val name: String
)

/**
 * H2 配置构建器 / H2 configuration builder.
 *
 * @property url JDBC URL / JDBC URL
 * @property name 客户端名称 / Client name
 * @property userName 用户名 / Username
 * @property password 密码 / Password
 * @property properties 额外连接属性 / Additional connection properties
 * @property maxTotal 最大连接数 / Maximum total connections
 * @property maxIdle 最大空闲连接数 / Maximum idle connections
 * @property maxOpenPreparedStatements 最大预编译语句数 / Maximum open prepared statements
*/
data class H2ConfigBuilder(
    var url: String? = null,
    var name: String? = null,
    var userName: String = "sa",
    var password: String = "",
    val properties: MutableMap<String, String> = mutableMapOf(),
    var maxTotal: Int = 20,
    var maxIdle: Int = 10,
    var maxOpenPreparedStatements: Int = 100
) {
    /**
     * 构建并校验 H2 配置 / Build and validate H2 configuration.
     *
     * @return 配置结果 / Configuration result
    */
    fun buildRet(): Ret<H2Config> {
        val url = url?.takeIf { it.startsWith("jdbc:h2:", ignoreCase = true) }
            ?: return Failed(ErrorCode.IllegalArgument, "H2 JDBC URL 未设置或无效 / H2 JDBC URL is missing or invalid")
        val name = name?.takeIf { it.isNotBlank() }
            ?: return Failed(ErrorCode.IllegalArgument, "H2 客户端名称未设置 / H2 client name is not set")
        if (maxTotal <= 0 || maxIdle < 0 || maxIdle > maxTotal || maxOpenPreparedStatements < 0) {
            return Failed(ErrorCode.IllegalArgument, "H2 连接池配置无效 / H2 connection pool settings are invalid")
        }
        return ok(
            H2Config(
                url = url,
                name = name,
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
    operator fun invoke(): H2Config? {
        return buildRet().value
    }
}

/**
 * H2 配置 / H2 configuration.
 *
 * @property url JDBC URL / JDBC URL
 * @property name 客户端名称 / Client name
 * @property userName 用户名 / Username
 * @property password 密码 / Password
 * @property properties 额外连接属性 / Additional connection properties
 * @property maxTotal 最大连接数 / Maximum total connections
 * @property maxIdle 最大空闲连接数 / Maximum idle connections
 * @property maxOpenPreparedStatements 最大预编译语句数 / Maximum open prepared statements
*/
@Serializable
data class H2Config(
    val url: String,
    val name: String,
    val userName: String = "sa",
    val password: String = "",
    val properties: Map<String, String> = emptyMap(),
    val maxTotal: Int = 20,
    val maxIdle: Int = 10,
    val maxOpenPreparedStatements: Int = 100
) {
    /** 客户端键 / Client key */
    val key get() = H2ClientKey(name = name)
}

/**
 * H2 客户端管理器 / H2 client manager.
 *
 * 管理 DBCP 数据源，并创建使用 H2 方言的 JdbcClient。 / Manages DBCP datasources and creates JdbcClient instances using the H2 dialect.
*/
object H2 {
    private val clients: MutableMap<H2ClientKey, BasicDataSource> = LinkedHashMap()

    private fun dataSource(config: H2Config): Ret<BasicDataSource> {
        if (!config.url.startsWith("jdbc:h2:", ignoreCase = true) || config.name.isBlank() ||
            config.maxTotal <= 0 || config.maxIdle < 0 || config.maxIdle > config.maxTotal ||
            config.maxOpenPreparedStatements < 0
        ) {
            return Failed(ErrorCode.IllegalArgument, "H2 配置无效 / H2 configuration is invalid")
        }
        clients[config.key]?.let { return ok(it) }
        return try {
            val dataSource = BasicDataSource().apply {
                driverClassName = "org.h2.Driver"
                url = config.url
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
            Failed(ErrorCode.ApplicationError, "无法创建 H2 数据源，请检查配置和驱动 / Unable to create H2 datasource; check configuration and driver")
        }
    }

    private fun jdbcClient(dataSource: BasicDataSource): Ret<JdbcClientBackend> {
        return try {
            ok(JdbcClientBackend(JdbcClient.create(dataSource), H2JdbcClientDialect))
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "无法创建 H2 JdbcClient / Unable to create H2 JdbcClient")
        }
    }

    /**
     * 通过配置构建器初始化 JdbcClient / Initialize a JdbcClient from a configuration builder.
     *
     * @param builder 配置构建器 lambda / Configuration builder lambda
     * @return JdbcClient 后端或结构化错误 / JdbcClient backend or structured error
    */
    @Synchronized
    fun initJdbcClient(builder: H2ConfigBuilder.() -> Unit): Ret<JdbcClientBackend> {
        val configBuilder = H2ConfigBuilder()
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
     * @param builder H2 配置构建器 / H2 configuration builder
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun initKtorm(builder: H2ConfigBuilder.() -> Unit): Ret<KtormBackend> {
        val configBuilder = H2ConfigBuilder()
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
     * @param config H2 配置 / H2 configuration
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun getKtormBackend(config: H2Config): Ret<KtormBackend> {
        return when (val source = dataSource(config)) {
            is Ok -> try {
                ok(KtormBackend(Database.connect(source.value, H2KtormDialect), RelationalQueryDialect.H2))
            } catch (e: Exception) {
                Failed(ErrorCode.ApplicationError, "无法创建 H2 Ktorm 后端 / Unable to create H2 Ktorm backend")
            }
            is Failed -> Failed(source.error)
            is Fatal -> Fatal(source.errors)
        }
    }

    /**
     * 按键获取 Ktorm 后端 / Get a Ktorm backend by key.
     *
     * @param key H2 客户端键 / H2 client key
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun getKtormBackend(key: H2ClientKey): Ret<KtormBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 H2 客户端 '${key.name}' / H2 client '${key.name}' was not found")
        return try {
            ok(KtormBackend(Database.connect(dataSource, H2KtormDialect), RelationalQueryDialect.H2))
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "无法创建 H2 Ktorm 后端 / Unable to create H2 Ktorm backend")
        }
    }

    /**
     * 按名称获取 Ktorm 后端 / Get a Ktorm backend by client name.
     *
     * @param name H2 客户端名称 / H2 client name
     * @return Ktorm 后端或结构化错误 / Ktorm backend or structured failure
    */
    @Synchronized
    fun getKtormBackend(name: String): Ret<KtormBackend> {
        return getKtormBackend(H2ClientKey(name))
    }

    /**
     * 初始化并获取 MyBatis 后端 / Initialize and get a MyBatis backend.
     *
     * @param builder H2 配置构建器 / H2 configuration builder
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun initMybatis(builder: H2ConfigBuilder.() -> Unit): Ret<MybatisBackend> {
        val configBuilder = H2ConfigBuilder()
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
     * @param config H2 配置 / H2 configuration
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun getMybatisBackend(config: H2Config): Ret<MybatisBackend> {
        return when (val source = dataSource(config)) {
            is Ok -> MybatisBackendFactory.create(source.value, MybatisDialect.H2)
            is Failed -> Failed(source.error)
            is Fatal -> Fatal(source.errors)
        }
    }

    /**
     * 按键获取 MyBatis 后端 / Get a MyBatis backend by key.
     *
     * @param key H2 客户端键 / H2 client key
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun getMybatisBackend(key: H2ClientKey): Ret<MybatisBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 H2 客户端 '${key.name}' / H2 client '${key.name}' was not found")
        return MybatisBackendFactory.create(dataSource, MybatisDialect.H2)
    }

    /**
     * 按名称获取 MyBatis 后端 / Get a MyBatis backend by client name.
     *
     * @param name H2 客户端名称 / H2 client name
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
    */
    @Synchronized
    fun getMybatisBackend(name: String): Ret<MybatisBackend> {
        return getMybatisBackend(H2ClientKey(name))
    }

    /**
     * 获取或创建 JdbcClient 后端 / Get or create a JdbcClient backend.
     *
     * @param config H2 配置 / H2 configuration
     * @return JdbcClient 后端或结构化错误 / JdbcClient backend or structured error
    */
    @Synchronized
    fun getJdbcClient(config: H2Config): Ret<JdbcClientBackend> {
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
    fun getJdbcClient(key: H2ClientKey): Ret<JdbcClientBackend> {
        val dataSource = clients[key]
            ?: return Failed(ErrorCode.DataNotFound, "未找到 H2 客户端 '${key.name}' / H2 client '${key.name}' was not found")
        return jdbcClient(dataSource)
    }

    /**
     * 按名称获取 JdbcClient 后端 / Get a JdbcClient backend by client name.
     *
     * @param name 客户端名称 / Client name
     * @return JdbcClient 后端或未找到错误 / JdbcClient backend or not-found error
    */
    @Synchronized
    fun getJdbcClient(name: String): Ret<JdbcClientBackend> {
        val key = H2ClientKey(name).takeIf { clients.containsKey(it) }
            ?: return Failed(ErrorCode.DataNotFound, "未找到 H2 客户端 '$name' / H2 client '$name' was not found")
        return getJdbcClient(key)
    }

    /**
     * 关闭并移除数据源；未注册时幂等成功 / Close and remove a datasource; succeeds when not registered.
     *
     * @param key 客户端键 / Client key
     * @return 关闭结果 / Close result
    */
    @Synchronized
    fun close(key: H2ClientKey): Try {
        val dataSource = clients[key] ?: return ok
        return try {
            dataSource.close()
            clients.remove(key)
            ok
        } catch (e: Exception) {
            Failed(ErrorCode.ApplicationError, "关闭 H2 数据源失败 / Failed to close H2 datasource")
        }
    }
}
