/**
 * Duration 序列化器
 *
 * Duration serializers for Kotlin serialization.
 * 提供 kotlin.time.Duration 的序列化和反序列化支持。
 *
 * Provides serialization and deserialization support for kotlin.time.Duration.
 *
 * 包含两种序列化器：
 * Contains two serializers:
 * - [DiscreteDurationSerializer]: 离散时间单位序列化器，使用 Long 类型
 *   - [DiscreteDurationSerializer]: Discrete time unit serializer using Long type
 * - [ContinuousDurationSerializer]: 连续时间单位序列化器，使用 Double 类型
 *   - [ContinuousDurationSerializer]: Continuous time unit serializer using Double type
 */
@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.utils.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * 离散 Duration 序列化器
 *
 * Discrete Duration serializer using Long values.
 * 离散时间单位的 Duration 序列化器，使用 Long 类型存储。
 *
 * 适用于不需要小数精度的时间单位（如毫秒、秒等）。
 * Suitable for time units that don't require fractional precision (e.g., milliseconds, seconds).
 *
 * 使用示例：
 * Usage example:
 * ```kotlin
 * @Serializable(with = DiscreteDurationSerializer::class)
 * data class Event(
 *     val duration: Duration = 1000.toDuration(DurationUnit.MILLISECONDS)
 * )
 *
 * // 或者使用自定义单位
 * // Or use custom unit
 * object MillisecondsSerializer : DiscreteDurationSerializer(DurationUnit.MILLISECONDS)
 * ```
 *
 * @property unit 时间单位 / Time unit
 */
open class DiscreteDurationSerializer(val unit: DurationUnit) : KSerializer<Duration> {
    /**
     * 序列化描述符
     *
     * Serialization descriptor.
     * 描述序列化的原始类型为 LONG。
     * Describes the serialization primitive type as LONG.
     */
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Discrete kotlin.time.Duration", PrimitiveKind.LONG)

    /**
     * 反序列化
     *
     * Deserializes a Duration from a Long value.
     * 将 Long 值反序列化为 Duration。
     *
     * @param decoder 解码器 / Decoder
     * @return 反序列化的 Duration / Deserialized Duration
     */
    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeLong().toDuration(unit)
    }

    /**
     * 序列化
     *
     * Serializes a Duration to a Long value.
     * 将 Duration 序列化为 Long 值。
     *
     * @param encoder 编码器 / Encoder
     * @param value 要序列化的 Duration / Duration to serialize
     */
    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.toLong(unit))
    }
}

/**
 * 连续 Duration 序列化器
 *
 * Continuous Duration serializer using Double values.
 * 连续时间单位的 Duration 序列化器，使用 Double 类型存储。
 *
 * 适用于需要小数精度的时间单位（如秒、分钟等）。
 * Suitable for time units that require fractional precision (e.g., seconds, minutes).
 *
 * 使用示例：
 * Usage example:
 * ```kotlin
 * @Serializable(with = ContinuousDurationSerializer::class)
 * data class Process(
 *     val duration: Duration = 1.5.toDuration(DurationUnit.SECONDS)
 * )
 *
 * // 或者使用自定义单位
 * // Or use custom unit
 * object SecondsSerializer : ContinuousDurationSerializer(DurationUnit.SECONDS)
 * ```
 *
 * @property unit 时间单位 / Time unit
 */
open class ContinuousDurationSerializer(val unit: DurationUnit) : KSerializer<Duration> {
    /**
     * 序列化描述符
     *
     * Serialization descriptor.
     * 描述序列化的原始类型为 DOUBLE。
     * Describes the serialization primitive type as DOUBLE.
     */
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Discrete kotlin.time.Duration", PrimitiveKind.DOUBLE)

    /**
     * 反序列化
     *
     * Deserializes a Duration from a Double value.
     * 将 Double 值反序列化为 Duration。
     *
     * @param decoder 解码器 / Decoder
     * @return 反序列化的 Duration / Deserialized Duration
     */
    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeDouble().toDuration(unit)
    }

    /**
     * 序列化
     *
     * Serializes a Duration to a Double value.
     * 将 Duration 序列化为 Double 值。
     *
     * @param encoder 编码器 / Encoder
     * @param value 要序列化的 Duration / Duration to serialize
     */
    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeDouble(value.toDouble(unit))
    }
}
