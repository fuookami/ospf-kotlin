package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.dto

import kotlinx.datetime.*
import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.serialization.*

@Serializable
data class RenderGanttSubItemDTO(
    val name: String,
    val category: String,
    @Serializable(with = DateTimeSerializer::class)
    val startTime: Instant,
    @Serializable(with = DateTimeSerializer::class)
    val endTime: Instant
) {
    companion object {
        operator fun invoke(task: RenderSubTaskDTO): RenderGanttSubItemDTO {
            return RenderGanttSubItemDTO(
                name = task.name,
                category = task.category,
                startTime = task.startTime,
                endTime = task.endTime
            )
        }
    }
}

@Serializable
data class RenderGanttItemInfoDTO(
    val key: String,
    val value: String
)

@Serializable
data class RenderGanttItemDTO(
    val name: String,
    val category: RenderTaskCategory,
    val subItems: List<RenderGanttSubItemDTO>,
    @Serializable(with = DateTimeSerializer::class)
    val scheduledStartTime: Instant,
    @Serializable(with = DateTimeSerializer::class)
    val scheduledEndTime: Instant,
    @Serializable(with = DateTimeSerializer::class)
    val startTime: Instant,
    @Serializable(with = DateTimeSerializer::class)
    val endTime: Instant,
    val produces: List<RenderGanttItemInfoDTO>,
    val resources: List<RenderGanttItemInfoDTO>,
    val info: List<RenderGanttItemInfoDTO>
) {
    companion object {
        operator fun invoke(task: RenderTaskDTO): RenderGanttItemDTO {
            val produces = HashMap<String, String>()
            when (val order = task.order) {
                is String -> produces["order"] = order
            }
            when (val produce = task.produce) {
                is String -> produces["produce"] = produce
            }
            when (val products = task.products) {
                is Map<String, String> -> produces.putAll(products.map { Pair("product", "${it.key}, ${it.value}") })
            }
            when (val materials = task.materials) {
                is Map<String, String> -> produces.putAll(materials.map { Pair("material", "${it.key}, ${it.value}") })
            }

            return RenderGanttItemDTO(
                name = task.name,
                category = task.category,
                subItems = task.subTasks.map { RenderGanttSubItemDTO.invoke(it) },
                scheduledStartTime = task.scheduledStartTime ?: task.startTime,
                scheduledEndTime = task.scheduledEndTime ?: task.endTime,
                startTime = task.startTime,
                endTime = task.endTime,
                produces = produces.map { RenderGanttItemInfoDTO(key = it.key, value = it.value) },
                resources = task.resources.map { RenderGanttItemInfoDTO(key = it.key, value = it.value) },
                info = task.info.map { RenderGanttItemInfoDTO(key = it.key, value = it.value) }
            )
        }
    }
}

@Serializable
data class RenderGanttLineDTO(
    val name: String,
    val category: String,
    val items: List<RenderGanttItemDTO>
) {
    companion object {
        operator fun invoke(name: String, tasks: List<RenderTaskDTO>): RenderGanttLineDTO {
            return RenderGanttLineDTO(
                name = name,
                category = "Normal",
                items = tasks.map { RenderGanttItemDTO(it) }
            )
        }
    }
}

@Serializable
data class RenderGanttDTO(
    val startTime: Instant,
    val endTime: Instant,
    val linkInfo: List<String>,
    val lines: List<RenderGanttLineDTO>
) {
    companion object {
        operator fun invoke(renderTasks: RenderDTO): RenderGanttDTO {
            val tasks = renderTasks.tasks.groupBy { it.executor }
            val linkInfo = renderTasks.tasks.flatMap { it.resources.keys }.toSet()
            val startTime = renderTasks.tasks.minOf { it.startTime }
            val endTime = renderTasks.tasks.maxOf { it.endTime }
            return RenderGanttDTO(
                startTime = startTime,
                endTime = endTime,
                linkInfo = linkInfo.toList(),
                lines = tasks.map { (executor, tasks) -> RenderGanttLineDTO(executor, tasks) }
            )
        }
    }
}
