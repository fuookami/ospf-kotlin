@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.dto

import fuookami.ospf.kotlin.utils.serialization.DateTimeSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class GanttRenderTaskCategory {
    Normal,
    Testing,
    Unavailable,
    Unknown
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Serializable
data class GanttRenderSubTaskDTO(
    val name: String,
    val category: String = GanttRenderTaskCategory.Normal.name,
    @Serializable(with = DateTimeSerializer::class)
    val startTime: Instant,
    @Serializable(with = DateTimeSerializer::class)
    val endTime: Instant,
    val info: Map<String, String> = emptyMap()
)

@OptIn(kotlin.time.ExperimentalTime::class)
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
    val tasks: List<GanttRenderTaskDTO> = emptyList()
)