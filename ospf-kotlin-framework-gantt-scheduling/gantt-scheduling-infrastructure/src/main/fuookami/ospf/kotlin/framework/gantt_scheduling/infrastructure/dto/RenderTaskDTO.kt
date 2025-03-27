package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.dto

import kotlinx.datetime.*
import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.serialization.*

enum class RenderTaskCategory {
    Normal,
    Testing,
    Unavailable,
    Unknown
}

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

sealed interface RenderTaskDTO {
    val name: String
    val category: RenderTaskCategory
    val startTime: Instant
    val endTime: Instant
    val info: Map<String, String>

    val executor: String
    val order: String?
    val produce: String?
    val products: Map<String, String>?
    val materials: Map<String, String>?
    val resources: Map<String, String>
    val scheduledStartTime: Instant?
    val scheduledEndTime: Instant?
    val subTasks: List<RenderSubTaskDTO>
}

@Serializable
data class RenderNormalTaskDTO(
    override val name: String,
    override val category: RenderTaskCategory,
    override val executor: String,
    override val order: String? = null,
    override val produce: String? = null,
    override val products: Map<String, String>? = null,
    override val materials: Map<String, String>? = null,
    override val resources: Map<String, String> = emptyMap(),
    @Serializable(with = DateTimeSerializer::class)
    override val scheduledStartTime: Instant? = null,
    @Serializable(with = DateTimeSerializer::class)
    override val scheduledEndTime: Instant? = null,
    @Serializable(with = DateTimeSerializer::class)
    override val startTime: Instant,
    @Serializable(with = DateTimeSerializer::class)
    override val endTime: Instant,
    override val info: Map<String, String> = emptyMap(),
    override val subTasks: List<RenderSubTaskDTO> = emptyList()
) : RenderTaskDTO

@Serializable
data class RenderDTO(
    val tasks: List<RenderNormalTaskDTO>
)
