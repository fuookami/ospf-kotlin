package fuookami.ospf.kotlin.framework.persistence

import kotlinx.datetime.*
import com.mongodb.client.*
import kotlinx.serialization.*

fun <T : RequestDTO<T>> MongoDatabase.insertRequest(
    path: String,
    app: String,
    requester: String,
    version: String,
    serializer: KSerializer<T>,
    request: T
) {
    val record = RequestRecordPO(app, requester, version, request)
    this.insert("${path.replace('/', '_')}_input", RequestRecordPO.serializer(serializer), record)
}

fun <T : RequestDTO<T>> MongoDatabase.getRequest(
    path: String,
    serializer: KSerializer<T>,
    query: Map<String, String>
): List<T> {
    return this.get("${path.replace('/', '_')}_input", RequestRecordPO.serializer(serializer), query).map { it.request }
}

fun <T : ResponseDTO<T>> MongoDatabase.getResponse(
    path: String,
    serializer: KSerializer<T>,
    query: Map<String, String>
): List<T> {
    return this.get("${path.replace('/', '_')}_output", ResponseRecordPO.serializer(serializer), query)
        .map { it.response }
}

fun <T : ResponseDTO<T>> MongoDatabase.insertResponse(
    path: String,
    app: String,
    requester: String,
    version: String,
    serializer: KSerializer<T>,
    response: T
) {
    val record = ResponseRecordPO(app, requester, version, response)
    this.insert("${path.replace('/', '_')}_output", ResponseRecordPO.serializer(serializer), record)
}

inline fun <reified T : RequestDTO<T>> RequestRecordDAO.find(
    db: MongoDB,
    id: String? = null,
    requester: String? = null,
    time: String?
): List<RequestRecordPO<T>> {
    TODO("not implement yet")
}
