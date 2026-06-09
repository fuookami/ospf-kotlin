
@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 甘特图渲染任务DTO / Gantt chart rendering task DTOs
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.dto

import fuookami.ospf.kotlin.utils.serialization.DateTimeSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * 甘特图渲染任务类别 / Gantt render task category
 */
enum class GanttRenderTaskCategory {
    /** 正常 / Normal */
    Normal,
    /** 测试 / Testing */
    Testing,
    /** 不可用 / Unavailable */
    Unavailable,
    /** 未知 / Unknown */
    Unknown
}

/**
 * 甘特图渲染子任务DTO / Gantt render sub-task DTO
 *
 * @property name 子任务名称 / Sub-task name
 * @property category 任务类别名称 / Task category name
 * @property startTime 开始时间 / Start time
 * @property endTime 结束时间 / End time
 * @property info 附加信息 / Additional info
 */
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

/**
 * 甘特图渲染任务DTO / Gantt render task DTO
 *
 * @property name 任务名称 / Task name
 * @property category 任务类别 / Task category
 * @property executor 执行者 / Executor
 * @property order 订单 / Order
 * @property produce 产品 / Product
 * @property products 产品映射 / Products mapping
 * @property consumption 消耗映射 / Consumption mapping
 * @property scheduledStartTime 计划开始时间 / Scheduled start time
 * @property scheduledEndTime 计划结束时间 / Scheduled end time
 * @property startTime 实际开始时间 / Actual start time
 * @property endTime 实际结束时间 / Actual end time
 * @property resources 资源映射 / Resources mapping
 * @property info 附加信息 / Additional info
 * @property subTasks 子任务列表 / Sub-tasks list
 */
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

/**
 * 甘特图渲染模式DTO / Gantt render schema DTO
 *
 * @property tasks 任务列表 / Tasks list
 */
@Serializable
data class GanttRenderSchemaDTO(
    val tasks: List<GanttRenderTaskDTO> = emptyList()
)