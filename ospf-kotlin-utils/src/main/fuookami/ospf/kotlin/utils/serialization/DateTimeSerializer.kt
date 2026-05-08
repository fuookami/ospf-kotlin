@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.utils.serialization

import kotlinx.datetime.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/**
 * Instant 序列化器
 *
 * 用于将 Instant 序列化为 ISO 8601 格式的字符串（如 "2024-01-15T08:30:00Z"）。
 *
 * Serializes Instant to ISO 8601 format string (e.g. "2024-01-15T08:30:00Z").
 */
@OptIn(ExperimentalTime::class)
data object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }
}

/**
 * Instant 序列化器
 *
 * 用于将 Instant 序列化为 "yyyy-MM-dd HH:mm:ss" 格式的字符串。
 *
 * Serializes Instant to "yyyy-MM-dd HH:mm:ss" format string.
 */
@OptIn(ExperimentalTime::class)
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

/**
 * LocalDateTime 序列化器
 *
 * 用于将 LocalDateTime 序列化为 ISO 格式的字符串。
 *
 * Serializes LocalDateTime to ISO format string.
 */
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

/**
 * LocalMonth 序列化器
 *
 * 用于将 LocalDate 序列化为 "yyyy-MM" 格式的字符串（仅年月）。
 * 反序列化时将日设置为 1。
 *
 * Serializes LocalDate to "yyyy-MM" format string (year-month only).
 * Sets day to 1 during deserialization.
 *
 * BUG FIX: 原始代码使用 LocalDate.parse 解析 "yyyy-MM" 格式会失败。
 * FIX: Original code using LocalDate.parse for "yyyy-MM" format would fail.
 * 应使用 YearMonth.parse 然后设置日为 1。
 * Should use YearMonth.parse then set day to 1.
 */
data object LocalMonthSerializer : KSerializer<LocalDate> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate {
        // BUG FIX: 使用 YearMonth 解析，然后设置日为 1
        // FIX: Use YearMonth to parse, then set day to 1
        val yearMonth = java.time.YearMonth.parse(decoder.decodeString(), formatter)
        return yearMonth.atDay(1).toKotlinLocalDate()
    }

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(formatter.format(value.toJavaLocalDate()))
    }
}

/**
 * LocalDate 序列化器
 *
 * 用于将 LocalDate 序列化为 "yyyy-MM-dd" 格式的字符串。
 *
 * Serializes LocalDate to "yyyy-MM-dd" format string.
 */
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

/**
 * LocalTime 序列化器
 *
 * 用于将 LocalTime 序列化为 "HH:mm:ss" 格式的字符串。
 *
 * Serializes LocalTime to "HH:mm:ss" format string.
 */
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
