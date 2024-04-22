package fuookami.ospf.kotlin.framework.network

import org.apache.logging.log4j.kotlin.*
import org.http4k.core.*
import org.http4k.client.*
import org.http4k.filter.ClientFilters.CustomBasicAuth.withBasicAuth
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

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

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T: Any> response(
    result: T,
    url: String,
    authorization: Authorization? = null
): Response? {
    val json = Json {
        ignoreUnknownKeys = true
    }
    return response(result, url, { json.encodeToString(T::class.serializer(), result) }, authorization)
}

suspend inline fun <reified T> response(
    result: T,
    url: String,
    serializer: (T) -> String,
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
            val response = client(request)
            logger("response").info { "status: ${response.status}, response: ${response.body}" }
            response
        }
    } else {
        null
    }
}
