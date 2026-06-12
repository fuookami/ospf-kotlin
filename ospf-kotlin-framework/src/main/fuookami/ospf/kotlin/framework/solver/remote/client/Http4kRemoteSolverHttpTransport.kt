/**
 * HTTP4K 远程求解 HTTP 传输
 * HTTP4K remote solver HTTP transport
 */
package fuookami.ospf.kotlin.framework.solver.remote.client

import org.http4k.client.ApacheClient
import org.http4k.core.*

/**
 * HTTP4K HTTP 传输实现。
 * HTTP4K HTTP transport implementation.
 *
 * @property http HTTP4K 处理器 / HTTP4K handler
 * @property config HTTP 传输配置 / HTTP transport config
 */
class Http4kRemoteSolverHttpTransport(
    private val http: HttpHandler = ApacheClient(),
    private val config: RemoteSolverHttpTransportConfig = RemoteSolverHttpTransportConfig()
) : RemoteSolverHttpTransport {
    override fun send(request: RemoteSolverHttpRequest): RemoteSolverHttpResponse {
        var httpRequest = Request(Method.valueOf(request.method.uppercase()), request.url)
        (config.headers + request.headers).forEach { (name, value) ->
            httpRequest = httpRequest.header(name, value)
        }
        request.body?.let {
            httpRequest = httpRequest.body(it)
        }
        val response = http(httpRequest)
        return RemoteSolverHttpResponse(
            statusCode = response.status.code,
            body = response.bodyString()
        )
    }
}

/**
 * HTTP4K HTTP 传输插件。
 * HTTP4K HTTP transport plugin.
 */
object Http4kRemoteSolverHttpTransportPlugin : RemoteSolverHttpTransportPlugin {
    override val name: String = "http4k"

    override fun create(config: RemoteSolverHttpTransportConfig): RemoteSolverHttpTransport {
        return Http4kRemoteSolverHttpTransport(config = config)
    }
}
