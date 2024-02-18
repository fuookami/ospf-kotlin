package fuookami.ospf.kotlin.utils.serialization

import kotlin.time.*
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

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
