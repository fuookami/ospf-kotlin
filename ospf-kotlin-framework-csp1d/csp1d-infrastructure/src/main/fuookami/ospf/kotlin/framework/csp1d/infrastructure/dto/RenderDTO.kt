package fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto

import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * Render production type.
 * 中文渲染生产类型
 */
enum class RenderProductionType {
    /** Product / 产品 */
    Product,
    /** Co-product / 联副产品 */
    Costar
}

/**
 * Render cutting plan production DTO.
 * 中文渲染切割方案生产项DTO
 *
 * @property x X coordinate / X坐标
 * @property width Width / 宽度
 * @property unitLength Unit length / 单位长度
 * @property productionType Production type / 生产类型
 * @property info Additional info / 附加信息
 */
@Serializable
data class RenderCuttingPlanProductionDTO(
    val name: String,
    val x: FltX,
    val width: FltX,
    val unitLength: FltX?,
    val productionType: RenderProductionType,
    val info: Map<String, String>
)

/**
 * Render cutting plan DTO.
 * 中文渲染切割方案DTO
 *
 * @property group Group identifiers / 分组标识
 * @property productions List of productions / 生产项列表
 * @property width Width / 宽度
 * @property standardWidth Standard width / 标准宽度
 * @property amount Amount / 数量
 * @property info Additional info / 附加信息
 */
@Serializable
data class RenderCuttingPlanDTO(
    val group: List<String>,
    val productions: List<RenderCuttingPlanProductionDTO>,
    val width: FltX,
    val standardWidth: FltX,
    val amount: UInt64,
    val info: Map<String, String>
)

/**
 * Render schema DTO.
 * 中文渲染方案DTO
 *
 * @property kpi KPI metrics / KPI指标
 * @property cuttingPlans List of cutting plans / 切割方案列表
 */
@Serializable
data class RenderSchemaDTO(
    val kpi: Map<String, String>,
    val cuttingPlans: List<RenderCuttingPlanDTO>,
)
