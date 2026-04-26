@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure

import java.time.format.*
import kotlinx.datetime.*

private val shortTimeFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("MMddHHmm")
        .withZone(TimeZone.currentSystemDefault().toJavaZoneId())

fun Instant.toShortString(): String = shortTimeFormatter.format(java.time.Instant.ofEpochMilli(this.toEpochMilliseconds()))
