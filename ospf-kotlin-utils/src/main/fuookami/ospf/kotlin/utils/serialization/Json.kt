/**
 * This file provides JSON serialization utilities with naming strategy support.
 * 本文件提供 JSON 序列化工具，支持命名策略自动转换。
*/
package fuookami.ospf.kotlin.utils.serialization

import java.io.*
import kotlinx.serialization.json.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import fuookami.ospf.kotlin.utils.meta_programming.*

/**
 * A JSON naming strategy for automatic field name conversion during serialization/deserialization.
 * Converts frontend naming system (e.g., camelCase) to backend naming system (e.g., snake_case).
 * JSON 命名策略，用于在序列化/反序列化时自动转换字段名的命名格式，将前端命名系统（如 camelCase）转换为后端命名系统（如 snake_case）。
 *
 * @property frontend frontend naming system / 前端命名系统
 * @property backend backend naming system / 后端命名系统
*/
@OptIn(ExperimentalSerializationApi::class)
class JsonNamingPolicy(
    val frontend: NamingSystem,
    val backend: NamingSystem
) : JsonNamingStrategy {

    /**
     * The name transfer for converting between naming systems.
     * 名称转换器，用于在命名系统之间进行转换。
    */
    val transfer = NameTransfer(frontend, backend)

    /**
     * Get the JSON field name for a given serial name.
     * Converts the Kotlin property name (serialName) from frontend format to backend format.
     * 获取 JSON 字段名，将 Kotlin 属性名（serialName）从前端格式转换为后端格式。
     *
     * BUG FIX: Original code used descriptor.serialName (class name), which is incorrect.
     * Should directly convert serialName (field name).
     * BUG 修复：原始代码使用 descriptor.serialName（类名），这是错误的，应该直接转换 serialName（字段名）。
     *
     * @param descriptor serialization descriptor / 序列化描述符
     * @param elementIndex element index / 元素索引
     * @param serialName Kotlin property name / Kotlin 属性名
     * @return converted JSON field name / 转换后的 JSON 字段名
    */
    override fun serialNameForJson(descriptor: SerialDescriptor, elementIndex: Int, serialName: String): String {
        return transfer(serialName)
    }
}

/**
 * Read a JSON object from a file.
 * 从文件读取 JSON 对象。
 *
 * @param T object type / 对象类型
 * @param path file path / 文件路径
 * @param namingPolicy naming policy / 命名策略
 * @return parsed object / 解析后的对象
*/
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromJson(path: String, namingPolicy: JsonNamingPolicy? = null): T {
    return readFromJson(
        serializer = T::class.serializer(),
        path = path,
        namingPolicy = namingPolicy
    )
}

/**
 * Read a JSON list from a file.
 * 从文件读取 JSON 列表。
 *
 * @param T list element type / 列表元素类型
 * @param path file path / 文件路径
 * @param namingPolicy naming policy / 命名策略
 * @return parsed list / 解析后的列表
*/
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromJsonList(path: String, namingPolicy: JsonNamingPolicy? = null): List<T> {
    return readFromJsonList(
        serializer = T::class.serializer(),
        path = path,
        namingPolicy = namingPolicy
    )
}

/**
 * Read a JSON object from a file using a serializer.
 * 从文件读取 JSON 对象（使用序列化器）。
 *
 * @param T object type / 对象类型
 * @param serializer serializer / 序列化器
 * @param path file path / 文件路径
 * @param namingPolicy naming policy / 命名策略
 * @return parsed object / 解析后的对象
*/
@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromJson(serializer: KSerializer<T>, path: String, namingPolicy: JsonNamingPolicy? = null): T {
    val file = File(path)
    val json = Json {
        ignoreUnknownKeys = true
        if (namingPolicy != null) {
            namingStrategy = namingPolicy
        }
    }
    // UTL-006: 应使用 use {} 确保资源关闭
    // Should use use {} to ensure resource closure
    return FileInputStream(file).use { stream ->
        json.decodeFromStream(serializer, stream)
    }
}

/**
 * Read a JSON list from a file using a serializer.
 * 从文件读取 JSON 列表（使用序列化器）。
 *
 * @param T list element type / 列表元素类型
 * @param serializer element serializer / 元素序列化器
 * @param path file path / 文件路径
 * @param namingPolicy naming policy / 命名策略
 * @return parsed list / 解析后的列表
*/
@OptIn(ExperimentalSerializationApi::class)
fun <T> readFromJsonList(serializer: KSerializer<T>, path: String, namingPolicy: JsonNamingPolicy? = null): List<T> {
    return readFromJson(
        serializer = ListSerializer(serializer),
        path = path,
        namingPolicy = namingPolicy
    )
}

/**
 * Read a JSON object from a stream.
 * 从流读取 JSON 对象。
 *
 * @param T object type / 对象类型
 * @param stream input stream / 输入流
 * @param namingPolicy naming policy / 命名策略
 * @return parsed object / 解析后的对象
*/
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromJson(stream: InputStream, namingPolicy: JsonNamingPolicy? = null): T {
    return readFromJson(
        serializer = T::class.serializer(),
        stream = stream,
        namingPolicy = namingPolicy
    )
}

/**
 * Read a JSON list from a stream.
 * 从流读取 JSON 列表。
 *
 * @param T list element type / 列表元素类型
 * @param stream input stream / 输入流
 * @param namingPolicy naming policy / 命名策略
 * @return parsed list / 解析后的列表
*/
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readFromJsonList(stream: InputStream, namingPolicy: JsonNamingPolicy? = null): List<T> {
    return readFromJsonList(
        serializer = ListSerializer(T::class.serializer()),
        stream = stream,
        namingPolicy = namingPolicy
    )
}

/**
 * Read a JSON object from a stream using a serializer.
 * 从流读取 JSON 对象（使用序列化器）。
 *
 * @param T object type / 对象类型
 * @param serializer serializer / 序列化器
 * @param stream input stream / 输入流
 * @param namingPolicy naming policy / 命名策略
 * @return parsed object / 解析后的对象
*/
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

/**
 * 从流读取 JSON 列表（使用序列化器）
 *
 * Read a JSON list from a stream using a serializer.
 *
 * @param T 列表元素类型 / List element type
 * @param serializer 列表序列化器 / List serializer
 * @param stream 输入流 / Input stream
 * @param namingPolicy 命名策略 / Naming policy
 * @return 解析后的列表 / Parsed list
*/
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

/**
 * 将对象序列化为 JSON 字符串
 *
 * Serialize an object to a JSON string.
 *
 * @param T 对象类型 / Object type
 * @param value 要序列化的对象 / Object to serialize
 * @param namingPolicy 命名策略 / Naming policy
 * @return JSON 字符串 / JSON string
*/
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

/**
 * 将列表序列化为 JSON 字符串
 *
 * Serialize a list to a JSON string.
 *
 * @param T 列表元素类型 / List element type
 * @param value 要序列化的列表 / List to serialize
 * @param namingPolicy 命名策略 / Naming policy
 * @return JSON 字符串 / JSON string
*/
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

/**
 * 将对象序列化为 JSON 字符串（使用序列化器）
 *
 * Serialize an object to a JSON string using a serializer.
 *
 * @param T 对象类型 / Object type
 * @param serializer 序列化器 / Serializer
 * @param value 要序列化的对象 / Object to serialize
 * @param namingPolicy 命名策略 / Naming policy
 * @return JSON 字符串 / JSON string
*/
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

/**
 * 将列表序列化为 JSON 字符串（使用序列化器）
 *
 * Serialize a list to a JSON string using a serializer.
 *
 * @param T 列表元素类型 / List element type
 * @param serializer 元素序列化器 / Element serializer
 * @param value 要序列化的列表 / List to serialize
 * @param namingPolicy 命名策略 / Naming policy
 * @return JSON 字符串 / JSON string
*/
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

/**
 * 将对象写入 JSON 文件
 *
 * Write an object to a JSON file.
 *
 * @param T 对象类型 / Object type
 * @param path 文件路径 / File path
 * @param value 要写入的对象 / Object to write
 * @param namingPolicy 命名策略 / Naming policy
*/
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeJsonToFile(path: String, value: T, namingPolicy: JsonNamingPolicy? = null) {
    File(path).outputStream().use { stream ->
        writeJsonToStream(
            stream = stream,
            serializer = T::class.serializer(),
            value = value,
            namingPolicy = namingPolicy
        )
    }
}

/**
 * 将列表写入 JSON 文件（使用序列化器）
 *
 * Write a list to a JSON file using a serializer.
 *
 * @param T 列表元素类型 / List element type
 * @param serializer 元素序列化器 / Element serializer
 * @param path 文件路径 / File path
 * @param value 要写入的列表 / List to write
 * @param namingPolicy 命名策略 / Naming policy
*/
fun <T> writeJsonToFile(serializer: KSerializer<T>, path: String, value: List<T>, namingPolicy: JsonNamingPolicy? = null) {
    File(path).outputStream().use { stream ->
        writeJsonToStream(
            stream = stream,
            serializer = ListSerializer(serializer),
            value = value,
            namingPolicy = namingPolicy
        )
    }
}

/**
 * 将对象写入 JSON 文件（使用序列化器）
 *
 * Write an object to a JSON file using a serializer.
 *
 * @param T 对象类型 / Object type
 * @param path 文件路径 / File path
 * @param serializer 序列化器 / Serializer
 * @param value 要写入的对象 / Object to write
 * @param namingPolicy 命名策略 / Naming policy
*/
fun <T> writeJsonToFile(path: String, serializer: KSerializer<T>, value: T, namingPolicy: JsonNamingPolicy? = null) {
    File(path).outputStream().use { stream ->
        writeJsonToStream(
            stream = stream,
            serializer = serializer,
            value = value,
            namingPolicy = namingPolicy
        )
    }
}

/**
 * 将列表写入 JSON 文件
 *
 * Write a list to a JSON file.
 *
 * @param T 列表元素类型 / List element type
 * @param path 文件路径 / File path
 * @param serializer 列表序列化器 / List serializer
 * @param value 要写入的列表 / List to write
 * @param namingPolicy 命名策略 / Naming policy
*/
fun <T> writeJsonToFile(path: String, serializer: KSerializer<List<T>>, value: List<T>, namingPolicy: JsonNamingPolicy? = null) {
    File(path).outputStream().use { stream ->
        writeJsonToStream(
            stream = stream,
            serializer = serializer,
            value = value,
            namingPolicy = namingPolicy
        )
    }
}

/**
 * 将对象写入输出流
 *
 * Write an object to an output stream.
 *
 * @param T 对象类型 / Object type
 * @param stream 输出流 / Output stream
 * @param value 要写入的对象 / Object to write
 * @param namingPolicy 命名策略 / Naming policy
*/
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

/**
 * 将列表写入输出流
 *
 * Write a list to an output stream.
 *
 * @param T 列表元素类型 / List element type
 * @param stream 输出流 / Output stream
 * @param value 要写入的列表 / List to write
 * @param namingPolicy 命名策略 / Naming policy
*/
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

/**
 * 将对象写入输出流（使用序列化器）
 *
 * Write an object to an output stream using a serializer.
 *
 * @param T 对象类型 / Object type
 * @param stream 输出流 / Output stream
 * @param serializer 序列化器 / Serializer
 * @param value 要写入的对象 / Object to write
 * @param namingPolicy 命名策略 / Naming policy
*/
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

/**
 * 将列表写入输出流（使用序列化器）
 *
 * Write a list to an output stream using a serializer.
 *
 * @param T 列表元素类型 / List element type
 * @param stream 输出流 / Output stream
 * @param serializer 列表序列化器 / List serializer
 * @param value 要写入的列表 / List to write
 * @param namingPolicy 命名策略 / Naming policy
*/
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
