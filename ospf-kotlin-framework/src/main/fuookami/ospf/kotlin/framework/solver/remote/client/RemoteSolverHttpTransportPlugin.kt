/**
 * 远程求解 HTTP 传输插件
 * Remote solver HTTP transport plugins
*/
package fuookami.ospf.kotlin.framework.solver.remote.client

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * HTTP 传输配置。
 * HTTP transport config.
 *
 * @property connectTimeout 连接超时 / Connect timeout
 * @property requestTimeout 请求超时 / Request timeout
 * @property headers 默认请求头 / Default request headers
 * @property properties 插件扩展配置 / Plugin extension properties
*/
data class RemoteSolverHttpTransportConfig(
    val connectTimeout: Duration? = null,
    val requestTimeout: Duration? = null,
    val headers: Map<String, String> = emptyMap(),
    val properties: Map<String, String> = emptyMap()
)

/**
 * HTTP 传输插件。
 * HTTP transport plugin.
*/
interface RemoteSolverHttpTransportPlugin {

    /** 插件名称 / Plugin name */
    val name: String

    /**
     * 创建 HTTP 传输。
     * Create HTTP transport.
     *
     * @param config HTTP 传输配置 / HTTP transport config
     * @return HTTP 传输 / HTTP transport
    */
    fun create(config: RemoteSolverHttpTransportConfig = RemoteSolverHttpTransportConfig()): RemoteSolverHttpTransport
}

/**
 * JDK HTTP 传输插件。
 * JDK HTTP transport plugin.
*/
object JavaNetRemoteSolverHttpTransportPlugin : RemoteSolverHttpTransportPlugin {
    override val name: String = "jdk"

    override fun create(config: RemoteSolverHttpTransportConfig): RemoteSolverHttpTransport {
        return JavaNetRemoteSolverHttpTransport(
            config = config
        )
    }
}

/**
 * HTTP 传输插件注册表。
 * HTTP transport plugin registry.
*/
object RemoteSolverHttpTransportPlugins {
    private val plugins = ConcurrentHashMap<String, RemoteSolverHttpTransportPlugin>()

    init {
        register(JavaNetRemoteSolverHttpTransportPlugin)
    }

    /**
     * 注册插件。
     * Register plugin.
     *
     * @param plugin HTTP 传输插件 / HTTP transport plugin
    */
    fun register(plugin: RemoteSolverHttpTransportPlugin) {
        val key = plugin.name.trim().lowercase()
        require(key.isNotBlank()) { "HTTP transport plugin name must not be blank." }
        plugins[key] = plugin
    }

    /**
     * 查找插件。
     * Resolve plugin.
     *
     * @param name 插件名称 / Plugin name
     * @return HTTP 传输插件 / HTTP transport plugin
    */
    fun resolve(name: String): Ret<RemoteSolverHttpTransportPlugin> {
        val key = name.trim().lowercase()
        val plugin = plugins[key]
        return if (plugin != null) {
            Ok(plugin)
        } else {
            Failed(ErrorCode.SolverNotFound, "HTTP transport plugin is not registered: $name")
        }
    }

    /**
     * 创建 HTTP 传输。
     * Create HTTP transport.
     *
     * @param name 插件名称 / Plugin name
     * @param config HTTP 传输配置 / HTTP transport config
     * @return HTTP 传输 / HTTP transport
    */
    fun create(
        name: String,
        config: RemoteSolverHttpTransportConfig = RemoteSolverHttpTransportConfig()
    ): Ret<RemoteSolverHttpTransport> {
        return when (val result = resolve(name)) {
            is Ok -> Ok(result.value.create(config))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 默认插件。
     * Default plugin.
     *
     * @return 默认 JDK HTTP 插件 / Default JDK HTTP plugin
    */
    fun default(): RemoteSolverHttpTransportPlugin {
        return JavaNetRemoteSolverHttpTransportPlugin
    }

    /**
     * 已注册插件名称。
     * Registered plugin names.
     *
     * @return 插件名称集合 / Plugin name set
    */
    fun names(): Set<String> {
        return plugins.keys.toSet()
    }
}
