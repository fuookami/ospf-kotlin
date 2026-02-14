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
    insertRequest(
        path = path,
        app = app,
        requester = requester,
        version = version,
        serializer = T::class.serializer(),
        request = request
    )
}

fun <T : RequestDTO<T>> MongoDatabase.insertRequest(
    path: String,
    app: String,
    requester: String,
    version: String,
    serializer: KSerializer<T>,
    request: T
) {
    val record = RequestRecordPO(
        app = app,
        requester = requester,
        version = version,
        request = request
    )
    this.insert(
        collection = "${path.replace('/', '_')}_input",
        serializer = RequestRecordPO.serializer(serializer),
        data = record
    )
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T> MongoDatabase.getRequest(
    path: String,
    query: Map<String, String>
): List<T> where T : RequestDTO<T>, T : Any {
    return getRequest(
        path = path,
        serializer = T::class.serializer(),
        query = query
    )
}

fun <T : RequestDTO<T>> MongoDatabase.getRequest(
    path: String,
    serializer: KSerializer<T>,
    query: Map<String, String>
): List<T> {
    return this.get(
        collectionName = "${path.replace('/', '_')}_input",
        deserializer = RequestRecordPO.serializer(serializer),
        query = query
    ).map { it.request }
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T> MongoDatabase.insertResponse(
    path: String,
    app: String,
    requester: String,
    version: String,
    response: T
) where T : ResponseDTO<T>, T : Any {
    return insertResponse(
        path = path,
        app = app,
        requester = requester,
        version = version,
        serializer = T::class.serializer(),
        response = response
    )
}

fun <T : ResponseDTO<T>> MongoDatabase.insertResponse(
    path: String,
    app: String,
    requester: String,
    version: String,
    serializer: KSerializer<T>,
    response: T
) {
    val record = ResponseRecordPO(
        app = app,
        requester = requester,
        version = version,
        response = response
    )
    this.insert(
        collection = "${path.replace('/', '_')}_output",
        serializer = ResponseRecordPO.serializer(serializer),
        data = record
    )
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T> MongoDatabase.getResponse(
    path: String,
    query: Map<String, String>
): List<T> where T : ResponseDTO<T>, T : Any {
    return getResponse(
        path = path,
        serializer = T::class.serializer(),
        query = query
    )
}

fun <T : ResponseDTO<T>> MongoDatabase.getResponse(
    path: String,
    serializer: KSerializer<T>,
    query: Map<String, String>
): List<T> {
    return this.get(
        collectionName = "${path.replace('/', '_')}_output",
        deserializer = ResponseRecordPO.serializer(serializer),
        query = query
    ).map { it.response }
}

inline fun <reified T : RequestDTO<T>> RequestRecordDAO.find(
    db: MongoDB,
    id: String? = null,
    requester: String? = null,
    time: String?
): List<RequestRecordPO<T>> {
    TODO("not implement yet")
}
