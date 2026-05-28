/**
 * CSV 序列化工具
 *
 * CSV serialization utilities for Kotlin.
 * 提供 CSV 文件的读取和写入功能，基于 kotlinx.serialization。
 *
 * Provides CSV file reading and writing capabilities based on kotlinx.serialization.
 */
package fuookami.ospf.kotlin.utils.serialization

import java.io.*
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.*

/**
 * 从 CSV 文件读取数据
 *
 * Reads data from a CSV file.
 * 从 CSV 文件读取数据列表，自动推断类型。
 *
 * CSV 格式要求：
 * CSV format requirements:
 * - 第一行为表头 / First row is header
 * - 忽略未知列 / Ignores unknown columns
 * - 支持 CRLF 和 LF 换行 / Supports CRLF and LF line endings
 *
 * @param T 数据类型 / Data type
 * @param path CSV 文件路径 / CSV file path
 * @return 解析后的数据列表 / Parsed data list
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromCSV(path: String): List<T> {
    return readFromCSV(T::class.serializer(), path)
}

/**
 * 从 CSV 文件读取数据（使用序列化器）
 *
 * Reads data from a CSV file using a serializer.
 * 从 CSV 文件读取数据列表，使用指定的序列化器。
 *
 * CSV 格式要求：
 * CSV format requirements:
 * - 第一行为表头 / First row is header
 * - 忽略未知列 / Ignores unknown columns
 * - 支持 CRLF 和 LF 换行 / Supports CRLF and LF line endings
 *
 * @param T 数据类型 / Data type
 * @param serializer 数据序列化器 / Data serializer
 * @param path CSV 文件路径 / CSV file path
 * @return 解析后的数据列表 / Parsed data list
 */
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

/**
 * 从输入流读取 CSV 数据
 *
 * Reads CSV data from an input stream.
 * 从输入流读取 CSV 数据列表，自动推断类型。
 *
 * CSV 格式要求：
 * CSV format requirements:
 * - 第一行为表头 / First row is header
 * - 忽略未知列 / Ignores unknown columns
 * - 支持 CRLF 和 LF 换行 / Supports CRLF and LF line endings
 *
 * @param T 数据类型 / Data type
 * @param stream 输入流 / Input stream
 * @return 解析后的数据列表 / Parsed data list
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromCSV(stream: InputStream): List<T> {
    return readFromCSV(T::class.serializer(), stream)
}

/**
 * 从输入流读取 CSV 数据（使用序列化器）
 *
 * Reads CSV data from an input stream using a serializer.
 * 从输入流读取 CSV 数据列表，使用指定的序列化器。
 *
 * CSV 格式要求：
 * CSV format requirements:
 * - 第一行为表头 / First row is header
 * - 忽略未知列 / Ignores unknown columns
 * - 支持 CRLF 和 LF 换行 / Supports CRLF and LF line endings
 *
 * @param T 数据类型 / Data type
 * @param serializer 数据序列化器 / Data serializer
 * @param stream 输入流 / Input stream
 * @return 解析后的数据列表 / Parsed data list
 */
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

/**
 * 将数据列表写入 CSV 文件
 *
 * Writes a data list to a CSV file.
 * 将数据列表写入 CSV 文件，自动推断类型。
 *
 * CSV 格式：
 * CSV format:
 * - 第一行为表头 / First row is header
 * - 忽略未知列 / Ignores unknown columns
 *
 * @param T 数据类型 / Data type
 * @param path CSV 文件路径 / CSV file path
 * @param value 要写入的数据列表 / Data list to write
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeCSVToFile(path: String, value: List<T>) {
    return writeCSVToFile(
        path = path,
        serializer = T::class.serializer(),
        value = value
    )
}

/**
 * 将数据列表写入 CSV 文件（使用序列化器）
 *
 * Writes a data list to a CSV file using a serializer.
 * 将数据列表写入 CSV 文件，使用指定的序列化器。
 *
 * CSV 格式：
 * CSV format:
 * - 第一行为表头 / First row is header
 * - 忽略未知列 / Ignores unknown columns
 *
 * @param T 数据类型 / Data type
 * @param path CSV 文件路径 / CSV file path
 * @param serializer 数据序列化器 / Data serializer
 * @param value 要写入的数据列表 / Data list to write
 */
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
