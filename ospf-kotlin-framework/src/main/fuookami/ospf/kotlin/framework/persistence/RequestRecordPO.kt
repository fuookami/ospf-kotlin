package fuookami.ospf.kotlin.framework.persistence

import kotlinx.datetime.*
import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.math.*

@Serializable
data class RequestRecordPO<T>(
    val id: String,
    val requester: String,
    val version: String,
    val time: String,
    val request: T
) where T : RequestDTO<T> {
    constructor(requester: String, version: String, request: T) : this(
        id = request.id,
        requester = requester,
        version = version,
        time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
        request = request
    )
}

@Serializable
data class ResponseRecordPO<T>(
    val id: String,
    val code: UInt64,
    val version: String,
    val time: String,
    val response: T
) where T : ResponseDTO<T> {
    constructor(version: String, response: T) : this(
        id = response.id,
        code = response.code,
        version = version,
        time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
        response = response
    )
}
