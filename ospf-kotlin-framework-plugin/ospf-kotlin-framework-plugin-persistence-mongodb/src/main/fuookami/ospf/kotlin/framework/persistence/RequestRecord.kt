package fuookami.ospf.kotlin.framework.persistence

import com.mongodb.client.*
import kotlinx.serialization.*

@OptIn(InternalSerializationApi::class)
inline fun <reified T> MongoDatabase.insertRequest(
    path: String,
    app: String,
    requester: String,
    version: String,
    request: T
) where T : RequestDTO<T>, T : Any {
    insertRequest(path, app, requester, version, T::class.serializer(), request)
}

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

@OptIn(InternalSerializationApi::class)
inline fun <reified T> MongoDatabase.getRequest(
    path: String,
    query: Map<String, String>
): List<T> where T : RequestDTO<T>, T : Any {
    return getRequest(path, T::class.serializer(), query)
}

fun <T : RequestDTO<T>> MongoDatabase.getRequest(
    path: String,
    serializer: KSerializer<T>,
    query: Map<String, String>
): List<T> {
    return this.get("${path.replace('/', '_')}_input", RequestRecordPO.serializer(serializer), query).map { it.request }
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T> MongoDatabase.insertResponse(
    path: String,
    app: String,
    requester: String,
    version: String,
    response: T
) where T : ResponseDTO<T>, T : Any {
    return insertResponse(path, app, requester, version, T::class.serializer(), response)
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

@OptIn(InternalSerializationApi::class)
inline fun <reified T> MongoDatabase.getResponse(
    path: String,
    query: Map<String, String>
): List<T> where T : ResponseDTO<T>, T : Any {
    return getResponse(path, T::class.serializer(), query)
}

fun <T : ResponseDTO<T>> MongoDatabase.getResponse(
    path: String,
    serializer: KSerializer<T>,
    query: Map<String, String>
): List<T> {
    return this.get("${path.replace('/', '_')}_output", ResponseRecordPO.serializer(serializer), query)
        .map { it.response }
}

inline fun <reified T : RequestDTO<T>> RequestRecordDAO.find(
    db: MongoDB,
    id: String? = null,
    requester: String? = null,
    time: String?
): List<RequestRecordPO<T>> {
    TODO("not implement yet")
}
