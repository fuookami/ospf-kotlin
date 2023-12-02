package fuookami.ospf.kotlin.utils.serialization

import java.io.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.meta_programming.*
import kotlinx.serialization.descriptors.SerialDescriptor

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
fun <T> readFromJson(serializer: KSerializer<T>, stream: InputStream, namingPolicy: JsonNamingPolicy? = null): T {
    val json = Json {
        ignoreUnknownKeys = true
        if (namingPolicy != null) {
            namingStrategy = namingPolicy
        }
    }
    return json.decodeFromStream(serializer, stream)
}

fun <T> writeJsonToFile(path: String, serializer: KSerializer<T>, value: T, namingPolicy: JsonNamingPolicy? = null) {
    writeJsonToStream(File(path).outputStream(), serializer, value, namingPolicy)
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
