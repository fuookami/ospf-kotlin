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

open class DiscreteDurationSerializer(val unit: DurationUnit) : KSerializer<Duration> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Discrete kotlin.time.Duration", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeLong().toDuration(unit)
    }

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.toLong(unit))
    }
}

open class ContinuousDurationSerializer(val unit: DurationUnit) : KSerializer<Duration> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Discrete kotlin.time.Duration", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeDouble().toDuration(unit)
    }

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeDouble(value.toDouble(unit))
    }
}
