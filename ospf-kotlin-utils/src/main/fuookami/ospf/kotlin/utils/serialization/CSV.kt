package fuookami.ospf.kotlin.utils.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.serializer
import java.io.File
import java.io.InputStream

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
    return csv.decodeFromString(
        deserializer = ListSerializer(serializer),
        string = String(file.readBytes()).replace("\r\n", "\n")
    )
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
    return csv.decodeFromString(
        deserializer = ListSerializer(serializer),
        string = String(stream.readBytes()).replace("\r\n", "\n")
    )
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeCSVToFile(path: String, value: List<T>) {
    return writeCSVToFile(
        path = path,
        serializer = T::class.serializer(),
        value = value
    )
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> writeCSVToFile(path: String, serializer: KSerializer<T>, value: List<T>) {
    val csv = Csv {
        hasHeaderRecord = true
        ignoreUnknownColumns = true
    }
    File(path).writeText(
        csv.encodeToString(
            serializer = ListSerializer(serializer),
            value = value
        )
    )
}
