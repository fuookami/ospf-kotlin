package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.dto

import kotlinx.datetime.*
import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.serialization.*

enum class GanttRenderTaskCategory {
    Normal,
    Testing,
    Unavailable,
    Unknown
}

@Serializable
data class GanttRenderSubTaskDTO(
    val name: String,
    val category: String,
    @Serializable(with = DateTimeSerializer::class)
    val startTime: Instant,
    @Serializable(with = DateTimeSerializer::class)
    val endTime: Instant,
    val info: Map<String, String>
)

@Serializable
data class GanttRenderTaskDTO(
    val name: String,
    val category: GanttRenderTaskCategory,
    val executor: String,
    val order: String? = null,
    val produce: String? = null,
    val products: Map<String, String>? = null,
    val consumption: Map<String, String> = emptyMap(),
    @Serializable(with = DateTimeSerializer::class)
    val scheduledStartTime: Instant? = null,
    @Serializable(with = DateTimeSerializer::class)
    val scheduledEndTime: Instant? = null,
    @Serializable(with = DateTimeSerializer::class)
    val startTime: Instant,
    @Serializable(with = DateTimeSerializer::class)
    val endTime: Instant,
    val resources: Map<String, String> = emptyMap(),
    val info: Map<String, String> = emptyMap(),
    val subTasks: List<GanttRenderSubTaskDTO> = emptyList()
)

@Serializable
data class GanttRenderSchemaDTO(
    val tasks: List<GanttRenderTaskDTO>
)
