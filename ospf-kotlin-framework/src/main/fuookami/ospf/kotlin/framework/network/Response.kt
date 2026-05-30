@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 网络响应
 * Network Response
 *
 * 提供基于 http4k 的 HTTP 响应发送和重试机制。
 * Provides HTTP response sending and retry mechanism based on http4k.
 */
package fuookami.ospf.kotlin.framework.network

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.apache.logging.log4j.kotlin.logger
import org.http4k.client.ApacheClient
import org.http4k.core.*
import org.http4k.filter.ClientFilters.CustomBasicAuth.withBasicAuth
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 授权接口
 * Authorization interface
 *
 * 用于在发送请求前注入认证信息。
 * Used to inject authentication info before sending request.
 */
interface Authorization {
    /**
     * 对请求应用授权
     * Apply authorization to request
     *
     * @param request 原始请求 / Original request
     * @return 添加授权后的请求 / Request with authorization applied
     */
    suspend operator fun invoke(request: Request): Request
}

/**
 * 基本认证
 * Basic authorization
 *
 * @property username 用户名 / Username
 * @property password 密码 / Password
 */
data class BasicAuthorization(
    private val username: String,
    private val password: String
) : Authorization {
    override suspend fun invoke(request: Request): Request {
        return request.withBasicAuth(
            Credentials(
                user = username,
                password = password
            )
        )
    }
}

/**
 * 响应重试配置
 * Response retry configuration
 *
 * @property times 最大重试次数 / Maximum retry count
 * @property delay 重试间隔，默认 1 秒 / Retry delay, default 1 second
 * @property condition 响应成功判定条件 / Response success condition
 */
data class ResponseRetry(
    val times: Int,
    val delay: Duration = 1.seconds,
    val condition: (Response) -> Boolean = { it.status.successful }
)

/**
 * 发送 HTTP 响应（自动序列化）
 * Send HTTP response (auto serialization)
 *
 * @param result 要发送的数据 / Data to send
 * @param url 目标 URL / Target URL
 * @param retry 重试配置，可为 null / Retry configuration, nullable
 * @param authorization 授权，可为 null / Authorization, nullable
 * @param T 请求数据类型 / Request data type
 * @return HTTP 响应，URL 为空时返回 null / HTTP response, null if URL is empty
 */
@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : Any> response(
    result: T,
    url: String,
    retry: ResponseRetry? = null,
    authorization: Authorization? = null
): Response? {
    val json = Json {
        ignoreUnknownKeys = true
    }
    return response(
        result = result,
        url = url,
        serializer = { json.encodeToString(T::class.serializer(), result) },
        retry = retry,
        authorization = authorization
    )
}

/**
 * 发送 HTTP 响应（自定义序列化）
 * Send HTTP response (custom serialization)
 *
 * @param result 要发送的数据 / Data to send
 * @param url 目标 URL / Target URL
 * @param serializer 自定义序列化函数 / Custom serialization function
 * @param retry 重试配置，可为 null / Retry configuration, nullable
 * @param authorization 授权，可为 null / Authorization, nullable
 * @param T 请求数据类型 / Request data type
 * @return HTTP 响应，URL 为空时返回 null / HTTP response, null if URL is empty
 */
suspend inline fun <reified T> response(
    result: T,
    url: String,
    serializer: (T) -> String,
    retry: ResponseRetry? = null,
    authorization: Authorization? = null
): Response? {
    return if (url.trim().isNotBlank() && url.trim() != "None") {
        var request = Request(Method.POST, url.trim())
            .header("Content-Type", "application/json")
            .header("Charset", "UTF-8")
            .body(serializer(result))
        if (authorization != null) {
            request = authorization(request)
        }
        coroutineScope {
            val client: HttpHandler = ApacheClient()
            logger("response").info { "send response to ${url.trim()}, response: ${request.body}" }
            val response = if (retry != null) {
                var count = 0
                var response: Response? = null
                while (count < retry.times) {
                    if (count != 0) {
                        logger("response").info { "send response to ${url.trim()}, retry times: ${count}, response: ${request.body}" }
                    }
                    response = client(request)
                    logger("response").info { "status: ${response.status}, response: ${response.body}" }
                    if (retry.condition(response)) {
                        break
                    }
                    logger("response").info { "send response to ${url.trim()} failed, retrying..." }
                    delay(retry.delay)
                    count += 1
                }
                response
            } else {
                val response = client(request)
                logger("response").info { "status: ${response.status}, response: ${response.body}" }
                response
            }
            response
        }
    } else {
        null
    }
}