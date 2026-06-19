package fuookami.ospf.kotlin.math

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

/**
 * 序列化协议失败。
 * Serialization protocol failure.
 */
internal fun serializationFailure(message: String): Nothing {
    throw SerializationException(message)
}

/**
 * 要求 JSON 编码器。
 * Requires a JSON encoder.
 */
internal fun requireJsonEncoder(encoder: Encoder, serializerName: String): JsonEncoder {
    return encoder as? JsonEncoder ?: serializationFailure(
        "$serializerName can be used only with Json format. Expected Encoder to be JsonEncoder, got ${encoder::class}."
    )
}

/**
 * 要求 JSON 解码器。
 * Requires a JSON decoder.
 */
internal fun requireJsonDecoder(decoder: Decoder, serializerName: String): JsonDecoder {
    return decoder as? JsonDecoder ?: serializationFailure(
        "$serializerName can be used only with Json format. Expected Decoder to be JsonDecoder, got ${decoder::class}."
    )
}

/**
 * 要求 JSON 对象。
 * Requires a JSON object.
 */
internal fun requireJsonObject(element: JsonElement, serializerName: String): JsonObject {
    return element as? JsonObject ?: serializationFailure(
        "$serializerName expects a JSON object, got ${element::class}."
    )
}

/**
 * 要求 JSON 字段完整。
 * Requires all JSON fields.
 */
internal fun requireJsonFields(element: JsonObject, fields: Iterable<String>, serializerName: String) {
    val missingFields = fields.filter { it !in element }
    if (missingFields.isNotEmpty()) {
        serializationFailure(
            "$serializerName missing required JSON fields: ${missingFields.joinToString()}."
        )
    }
}
