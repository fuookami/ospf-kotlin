package fuookami.ospf.kotlin.utils.serialization

import java.io.*
import java.nio.charset.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromJson(serializer: KSerializer<T>, path: String): T {
    val file = File(path)
    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromStream(serializer, FileInputStream(file))
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromJson(serializer: KSerializer<T>, stream: InputStream): T {
    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromStream(serializer, stream)
}

fun <T> writeJsonToFile(path: String, serializer: KSerializer<T>, value: T) {
    writeJsonToStream(File(path).outputStream(), serializer, value)
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> writeJsonToStream(stream: OutputStream, serializer: KSerializer<T>, value: T) {
    Json.encodeToStream(serializer, value, stream)
}
