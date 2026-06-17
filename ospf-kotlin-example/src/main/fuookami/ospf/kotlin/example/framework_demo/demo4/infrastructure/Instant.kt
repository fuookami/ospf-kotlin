@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import kotlin.time.Instant
import java.time.format.*

private val shortTimeFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("MMddHHmm")
        .withZone(TimeZone.currentSystemDefault().toJavaZoneId())

/**
 * 使用 "MMddHHmm" 模式将 [Instant] 格式化为短字符串。
 * Formats an [Instant] as a short string using the "MMddHHmm" pattern.
 *
 * @return 格式化后的短字符串。
 */
fun Instant.toShortString(): String = shortTimeFormatter.format(java.time.Instant.ofEpochMilli(this.toEpochMilliseconds()))
