package fuookami.ospf.kotlin.utils.serialization

import java.time.format.*
import java.time.temporal.*
import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

data object DateTimeSerializer : KSerializer<Instant> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(TimeZone.currentSystemDefault().toJavaZoneId())

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant =
        java.time.Instant.from(formatter.parse(decoder.decodeString())).truncatedTo(ChronoUnit.SECONDS)
            .toKotlinInstant()

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(formatter.format(value.toJavaInstant()))
    }
}

data object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }
}

data object LocalMonthSerializer : KSerializer<LocalDate> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate {
        return java.time.LocalDate.parse(decoder.decodeString(), formatter).toKotlinLocalDate()
    }

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(formatter.format(value.toJavaLocalDate()))
    }
}

data object LocalDateSerializer : KSerializer<LocalDate> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate {
        return java.time.LocalDate.parse(decoder.decodeString(), formatter).toKotlinLocalDate()
    }

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(formatter.format(value.toJavaLocalDate()))
    }
}

data object LocalTimeSerializer : KSerializer<LocalTime> {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalTime {
        return java.time.LocalTime.parse(decoder.decodeString(), formatter).toKotlinLocalTime()
    }

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString(formatter.format(value.toJavaLocalTime()))
    }
}
