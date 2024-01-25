package fuookami.ospf.kotlin.utils.serialization

import java.io.*
import kotlinx.serialization.*
import kotlinx.serialization.csv.*
import kotlinx.serialization.builtins.*

@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromCSV(serializer: KSerializer<T>, path: String): List<T> {
    val file = File(path)
    val csv = Csv {
        hasHeaderRecord = true
        ignoreUnknownColumns = true
    }
    return csv.decodeFromString(ListSerializer(serializer), String(file.readBytes()).replace("\r\n", "\n"))
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromCSV(serializer: KSerializer<T>, stream: InputStream): List<T> {
    val csv = Csv {
        hasHeaderRecord = true
        ignoreUnknownColumns = true
    }
    return csv.decodeFromString(ListSerializer(serializer), String(stream.readBytes()).replace("\r\n", "\n"))
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> writeCSVToFile(path: String, serializer: KSerializer<T>, value: List<T>) {
    File(path).writeText(Csv.encodeToString(ListSerializer(serializer), value))
}
