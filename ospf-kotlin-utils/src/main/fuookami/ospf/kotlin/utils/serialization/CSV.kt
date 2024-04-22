package fuookami.ospf.kotlin.utils.serialization

import java.io.*
import kotlinx.serialization.*
import kotlinx.serialization.csv.*
import kotlinx.serialization.builtins.*

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromCSV(path: String): List<T> {
    return readFromCSV(T::class.serializer(), path)
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromCSV(serializer: KSerializer<T>, path: String): List<T> {
    val file = File(path)
    val csv = Csv {
        hasHeaderRecord = true
        ignoreUnknownColumns = true
    }
    return csv.decodeFromString(ListSerializer(serializer), String(file.readBytes()).replace("\r\n", "\n"))
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromCSV(stream: InputStream): List<T> {
    return readFromCSV(T::class.serializer(), stream)
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromCSV(serializer: KSerializer<T>, stream: InputStream): List<T> {
    val csv = Csv {
        hasHeaderRecord = true
        ignoreUnknownColumns = true
    }
    return csv.decodeFromString(ListSerializer(serializer), String(stream.readBytes()).replace("\r\n", "\n"))
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeCSVToFile(path: String, value: List<T>) {
    return writeCSVToFile(path, T::class.serializer(), value)
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> writeCSVToFile(path: String, serializer: KSerializer<T>, value: List<T>) {
    val csv = Csv {
        hasHeaderRecord = true
        ignoreUnknownColumns = true
    }
    File(path).writeText(csv.encodeToString(ListSerializer(serializer), value))
}
