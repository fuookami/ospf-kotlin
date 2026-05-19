@file:OptIn(kotlin.time.ExperimentalTime::class)

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

interface Authorization {
    suspend operator fun invoke(request: Request): Request
}

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

data class ResponseRetry(
    val times: Int,
    val delay: Duration = 1.seconds,
    val condition: (Response) -> Boolean = { it.status.successful }
)

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