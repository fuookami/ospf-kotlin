package fuookami.ospf.kotlin.utils.serialization

import java.io.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import fuookami.ospf.kotlin.utils.meta_programming.*

@OptIn(ExperimentalSerializationApi::class)
class JsonNamingPolicy(
    val frontend: NamingSystem,
    val backend: NamingSystem
) : JsonNamingStrategy {
    val transfer = NameTransfer(frontend, backend)

    override fun serialNameForJson(descriptor: SerialDescriptor, elementIndex: Int, serialName: String): String {
        return descriptor.serialName.ifBlank {
            transfer(serialName)
        }
    }
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromJson(path: String, namingPolicy: JsonNamingPolicy? = null): T {
    return readFromJson(
        serializer = T::class.serializer(),
        path = path,
        namingPolicy = namingPolicy
    )
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromJsonList(path: String, namingPolicy: JsonNamingPolicy? = null): List<T> {
    return readFromJsonList(
        serializer = T::class.serializer(),
        path = path,
        namingPolicy = namingPolicy
    )
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromJson(serializer: KSerializer<T>, path: String, namingPolicy: JsonNamingPolicy? = null): T {
    val file = File(path)
    val json = Json {
        ignoreUnknownKeys = true
        if (namingPolicy != null) {
            namingStrategy = namingPolicy
        }
    }
    return json.decodeFromStream(serializer, FileInputStream(file))
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromJsonList(serializer: KSerializer<T>, path: String, namingPolicy: JsonNamingPolicy? = null): List<T> {
    return readFromJson(
        serializer = ListSerializer(serializer),
        path = path,
        namingPolicy = namingPolicy
    )
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromJson(stream: InputStream, namingPolicy: JsonNamingPolicy? = null): T {
    return readFromJson(
        serializer = T::class.serializer(),
        stream = stream,
        namingPolicy = namingPolicy
    )
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromJsonList(stream: InputStream, namingPolicy: JsonNamingPolicy? = null): List<T> {
    return readFromJsonList(
        serializer = ListSerializer(T::class.serializer()),
        stream = stream,
        namingPolicy = namingPolicy
    )
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromJson(serializer: KSerializer<T>, stream: InputStream, namingPolicy: JsonNamingPolicy? = null): T {
    val json = Json {
        ignoreUnknownKeys = true
        if (namingPolicy != null) {
            namingStrategy = namingPolicy
        }
    }
    return json.decodeFromStream(serializer, stream)
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromJsonList(serializer: KSerializer<List<T>>, stream: InputStream, namingPolicy: JsonNamingPolicy? = null): List<T> {
    val json = Json {
        ignoreUnknownKeys = true
        if (namingPolicy != null) {
            namingStrategy = namingPolicy
        }
    }
    return json.decodeFromStream(serializer, stream)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeJson(value: T, namingPolicy: JsonNamingPolicy? = null): String {
    val stream = ByteArrayOutputStream()
    writeJsonToStream(
        stream = stream,
        serializer = T::class.serializer(),
        value = value,
        namingPolicy = namingPolicy
    )
    return stream.toString()
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeJson(value: List<T>, namingPolicy: JsonNamingPolicy? = null): String {
    val stream = ByteArrayOutputStream()
    writeJsonToStream(
        stream = stream,
        serializer = ListSerializer(T::class.serializer()),
        value = value,
        namingPolicy = namingPolicy
    )
    return stream.toString()
}

fun <T> writeJson(serializer: KSerializer<T>, value: T, namingPolicy: JsonNamingPolicy? = null): String {
    val stream = ByteArrayOutputStream()
    writeJsonToStream(
        stream = stream,
        serializer = serializer,
        value = value,
        namingPolicy = namingPolicy
    )
    return stream.toString()
}

fun <T> writeJson(serializer: KSerializer<T>, value: List<T>, namingPolicy: JsonNamingPolicy? = null): String {
    val stream = ByteArrayOutputStream()
    writeJsonToStream(
        stream = stream,
        serializer = ListSerializer(serializer),
        value = value,
        namingPolicy = namingPolicy
    )
    return stream.toString()
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeJsonToFile(path: String, value: T, namingPolicy: JsonNamingPolicy? = null) {
    writeJsonToStream(
        stream = File(path).outputStream(),
        serializer = T::class.serializer(),
        value = value,
        namingPolicy = namingPolicy
    )
}

fun <T> writeJsonToFile(serializer: KSerializer<T>, path: String, value: List<T>, namingPolicy: JsonNamingPolicy? = null) {
    writeJsonToStream(
        stream = File(path).outputStream(),
        serializer = ListSerializer(serializer),
        value = value,
        namingPolicy = namingPolicy
    )
}

fun <T> writeJsonToFile(path: String, serializer: KSerializer<T>, value: T, namingPolicy: JsonNamingPolicy? = null) {
    writeJsonToStream(
        stream = File(path).outputStream(),
        serializer = serializer,
        value = value,
        namingPolicy = namingPolicy
    )
}

fun <T> writeJsonToFile(path: String, serializer: KSerializer<List<T>>, value: List<T>, namingPolicy: JsonNamingPolicy? = null) {
    writeJsonToStream(
        stream = File(path).outputStream(),
        serializer = serializer,
        value = value,
        namingPolicy = namingPolicy
    )
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeJsonToStream(
    stream: OutputStream,
    value: T,
    namingPolicy: JsonNamingPolicy? = null
) {
    writeJsonToStream(
        stream = stream,
        serializer = T::class.serializer(),
        value = value,
        namingPolicy = namingPolicy
    )
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeJsonToStream(
    stream: OutputStream,
    value: List<T>,
    namingPolicy: JsonNamingPolicy? = null
) {
    writeJsonToStream(
        stream = stream,
        serializer = ListSerializer(T::class.serializer()),
        value = value,
        namingPolicy = namingPolicy
    )
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> writeJsonToStream(
    stream: OutputStream,
    serializer: KSerializer<T>,
    value: T,
    namingPolicy: JsonNamingPolicy? = null
) {
    val json = Json {
        ignoreUnknownKeys = true
        if (namingPolicy != null) {
            namingStrategy = namingPolicy
        }
    }
    json.encodeToStream(
        serializer = serializer,
        value = value,
        stream = stream
    )
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> writeJsonToStream(
    stream: OutputStream,
    serializer: KSerializer<List<T>>,
    value: List<T>,
    namingPolicy: JsonNamingPolicy? = null
) {
    val json = Json {
        ignoreUnknownKeys = true
        if (namingPolicy != null) {
            namingStrategy = namingPolicy
        }
    }
    json.encodeToStream(
        serializer = serializer,
        value = value,
        stream = stream
    )
}