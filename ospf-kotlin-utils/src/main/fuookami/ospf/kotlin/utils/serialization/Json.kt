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
    return readFromJson(T::class.serializer(), path, namingPolicy)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromJsonList(path: String, namingPolicy: JsonNamingPolicy? = null): List<T> {
    return readFromJsonList(T::class.serializer(), path, namingPolicy)
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
    return readFromJson(ListSerializer(serializer), path, namingPolicy)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromJson(stream: InputStream, namingPolicy: JsonNamingPolicy? = null): T {
    return readFromJson(T::class.serializer(), stream, namingPolicy)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromJsonList(stream: InputStream, namingPolicy: JsonNamingPolicy? = null): List<T> {
    return readFromJsonList(ListSerializer(T::class.serializer()), stream, namingPolicy)
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
    writeJsonToStream(stream, T::class.serializer(), value, namingPolicy)
    return stream.toString()
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeJson(value: List<T>, namingPolicy: JsonNamingPolicy? = null): String {
    val stream = ByteArrayOutputStream()
    writeJsonToStream(stream, ListSerializer(T::class.serializer()), value, namingPolicy)
    return stream.toString()
}

fun <T> writeJson(serializer: KSerializer<T>, value: T, namingPolicy: JsonNamingPolicy? = null): String {
    val stream = ByteArrayOutputStream()
    writeJsonToStream(stream, serializer, value, namingPolicy)
    return stream.toString()
}

fun <T> writeJson(serializer: KSerializer<T>, value: List<T>, namingPolicy: JsonNamingPolicy? = null): String {
    val stream = ByteArrayOutputStream()
    writeJsonToStream(stream, ListSerializer(serializer), value, namingPolicy)
    return stream.toString()
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeJsonToFile(path: String, value: T, namingPolicy: JsonNamingPolicy? = null) {
    writeJsonToStream(File(path).outputStream(), T::class.serializer(), value, namingPolicy)
}

fun <T> writeJsonToFile(serializer: KSerializer<T>, path: String, value: List<T>, namingPolicy: JsonNamingPolicy? = null) {
    writeJsonToStream(File(path).outputStream(), ListSerializer(serializer), value, namingPolicy)
}

fun <T> writeJsonToFile(path: String, serializer: KSerializer<T>, value: T, namingPolicy: JsonNamingPolicy? = null) {
    writeJsonToStream(File(path).outputStream(), serializer, value, namingPolicy)
}

fun <T> writeJsonToFile(path: String, serializer: KSerializer<List<T>>, value: List<T>, namingPolicy: JsonNamingPolicy? = null) {
    writeJsonToStream(File(path).outputStream(), serializer, value, namingPolicy)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeJsonToStream(
    stream: OutputStream,
    value: T,
    namingPolicy: JsonNamingPolicy? = null
) {
    writeJsonToStream(stream, T::class.serializer(), value, namingPolicy)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeJsonToStream(
    stream: OutputStream,
    value: List<T>,
    namingPolicy: JsonNamingPolicy? = null
) {
    writeJsonToStream(stream, ListSerializer(T::class.serializer()), value, namingPolicy)
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
    json.encodeToStream(serializer, value, stream)
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
    json.encodeToStream(serializer, value, stream)
}
