package fuookami.ospf.kotlin.framework.network

import org.http4k.*
import org.http4k.client.*
import org.http4k.core.*
import org.apache.logging.log4j.kotlin.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.math.*

data class Authorization(
    val url: String? = null,
    val appKey: String? = null,
    val appSecret: String? = null,
    val headers: Map<String, String> = emptyMap()
) {
    @Serializable
    private data class AuthorizationResponse(
        val key: String,
        val token: String
    )

    @OptIn(ExperimentalSerializationApi::class)
    suspend operator fun invoke(request: Request): Request {
        try {
            if (url != null && appKey != null && appSecret != null) {
                val tokenRequest = Request(Method.GET, url.trim())
                    .query("appKey", appKey)
                    .query("appSecret", appSecret)
                val token = coroutineScope {
                    val client: HttpHandler = ApacheClient()
                    val response = client(tokenRequest)
                    val json = Json { ignoreUnknownKeys = true }
                    json.decodeFromStream(AuthorizationResponse.serializer(), response.body.stream).token
                }
                return request
                    .header("X-Agp-Token", token)
                    .headers(headers.toList())
            } else if (appKey != null && appSecret != null) {
                return request
                    .header("X-Agp-AppKey", appKey)
                    .header("X-Agp-AppSecret", appSecret)
                    .headers(headers.toList())
            } else if (appKey != null) {
                return request
                    .header("X-Agp-AppKey", appKey)
                    .headers(headers.toList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return request.headers(headers.toList())
    }
}

suspend inline fun <reified T> response(
    result: T,
    url: String,
    authorization: Authorization? = null
): Response? {
    return if (url.trim().isNotBlank() && url.trim() != "None") {
        var request = Request(Method.POST, url.trim())
            .header("Content-Type", "application/json")
            .header("Charset", "UTF-8")
            .body(Json.encodeToString(result))
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
