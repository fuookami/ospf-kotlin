package fuookami.ospf.kotlin.math

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

/**
 * 序列化协议失败。
 * Serialization protocol failure.
 * @param message 失败信息 / The failure message
 * @return 永不返回，始终抛出异常 / Never returns, always throws an exception
 */
internal fun serializationFailure(message: String): Nothing {
    throw SerializationException(message)
}

/**
 * 要求 JSON 编码器。
 * Requires a JSON encoder.
 * @param encoder 待检查的编码器 / The encoder to check
 * @param serializerName 序列化器名称，用于错误提示 / The serializer name used in error messages
 * @return 转换后的 JsonEncoder / The casted JsonEncoder
 */
internal fun requireJsonEncoder(encoder: Encoder, serializerName: String): JsonEncoder {
    return encoder as? JsonEncoder ?: serializationFailure(
        "$serializerName can be used only with Json format. Expected Encoder to be JsonEncoder, got ${encoder::class}."
    )
}

/**
 * 要求 JSON 解码器。
 * Requires a JSON decoder.
 * @param decoder 待检查的解码器 / The decoder to check
 * @param serializerName 序列化器名称，用于错误提示 / The serializer name used in error messages
 * @return 转换后的 JsonDecoder / The casted JsonDecoder
 */
internal fun requireJsonDecoder(decoder: Decoder, serializerName: String): JsonDecoder {
    return decoder as? JsonDecoder ?: serializationFailure(
        "$serializerName can be used only with Json format. Expected Decoder to be JsonDecoder, got ${decoder::class}."
    )
}

/**
 * 要求 JSON 对象。
 * Requires a JSON object.
 * @param element 待检查的 JSON 元素 / The JSON element to check
 * @param serializerName 序列化器名称，用于错误提示 / The serializer name used in error messages
 * @return 转换后的 JsonObject / The casted JsonObject
 */
internal fun requireJsonObject(element: JsonElement, serializerName: String): JsonObject {
    return element as? JsonObject ?: serializationFailure(
        "$serializerName expects a JSON object, got ${element::class}."
    )
}

/**
 * 要求 JSON 字段完整。
 * Requires all JSON fields.
 * @param element 待检查的 JSON 对象 / The JSON object to check
 * @param fields 必须存在的字段名列表 / The field names that must be present
 * @param serializerName 序列化器名称，用于错误提示 / The serializer name used in error messages
 */
internal fun requireJsonFields(element: JsonObject, fields: Iterable<String>, serializerName: String) {
    val missingFields = fields.filter { it !in element }
    if (missingFields.isNotEmpty()) {
        serializationFailure(
            "$serializerName missing required JSON fields: ${missingFields.joinToString()}."
        )
    }
}
