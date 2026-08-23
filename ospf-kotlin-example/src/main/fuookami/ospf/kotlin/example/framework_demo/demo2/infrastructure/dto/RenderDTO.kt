package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

/**
 * Data transfer object for rendering output.
 * 渲染输出的数据传输对象。
 *
 * @property positions 按领域装载顺序排列的位置渲染行 / position render rows in domain loading order
*/
@Serializable
data class RenderDTO(
    val positions: List<RenderPositionDTO> = emptyList()
)

/**
 * Renderable stowage details for one aircraft position.
 * 单个飞机舱位的可渲染配载明细。
 *
 * @property positionId 舱位稳定标识 / stable position identifier
 * @property positionName 舱位显示名称 / position display name
 * @property itemIds 该舱位中的货物标识 / item identifiers assigned to this position
 * @property predicateLoadWeight 谓词装载重量，不适用时为 null / predicate load weight, or null when not applicable
 * @property recommendedLoadWeight 推荐装载重量，不适用时为 null / recommended load weight, or null when not applicable
 */
@Serializable
data class RenderPositionDTO(
    val positionId: String,
    val positionName: String,
    val itemIds: List<String> = emptyList(),
    val predicateLoadWeight: RenderWeightDTO? = null,
    val recommendedLoadWeight: RenderWeightDTO? = null
)

/**
 * Serializable physical weight value for rendering.
 * 用于渲染的可序列化物理重量值。
 *
 * @property value 重量数值 / weight value
 * @property unit 重量单位符号或名称，不可用时为 null / weight unit symbol or name, or null when unavailable
 */
@Serializable
data class RenderWeightDTO(
    val value: Double,
    val unit: String?
)
