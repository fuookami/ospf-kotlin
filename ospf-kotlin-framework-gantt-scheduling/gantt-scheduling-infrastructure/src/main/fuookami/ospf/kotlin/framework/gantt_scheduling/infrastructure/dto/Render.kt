package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.dto

import kotlinx.datetime.*
import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.serialization.*

@Serializable
data class RenderNormalTaskDTO(
    val name: String,
    val category: String,
    val executor: String,
    val order: String? = null,
    val produce: String? = null,
    val products: Map<String, String>? = null,
    val material: Map<String, String>? = null,
    val resources: Map<String, String> = emptyMap(),
    @Serializable(with = DateTimeSerializer::class)
    val scheduledStartTime: Instant? = null,
    @Serializable(with = DateTimeSerializer::class)
    val scheduledEndTime: Instant? = null,
    @Serializable(with = DateTimeSerializer::class)
    val startTime: Instant,
    @Serializable(with = DateTimeSerializer::class)
    val endTime: Instant,
    val info: Map<String, String> = emptyMap(),
    val subTasks: List<RenderSubTaskDTO> = emptyList()
)

@Serializable
data class RenderSubTaskDTO(
    val name: String,
    val category: String,
    @Serializable(with = DateTimeSerializer::class)
    val startTime: Instant,
    @Serializable(with = DateTimeSerializer::class)
    val endTime: Instant,
    val info: Map<String, String>
)

@Serializable
data class RenderDTO(
    val tasks: List<RenderNormalTaskDTO>
)
