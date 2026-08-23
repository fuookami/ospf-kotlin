package fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto

import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * Render production type.
 * 渲染生产类型
*/
enum class RenderProductionType {
    /** Product / 产品 */
    Product,
    /** Co-product / 联副产品 */
    Costar
}

/**
 * Render cutting plan production DTO.
 * 渲染切割方案生产项DTO
 *
 * @property x X coordinate / X坐标
 * @property width 宽度 / Width
 * @property unitLength 单位长度 / Unit length
 * @property productionType 生产类型 / Production type
 * @property info 附加信息 / Additional info
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
 * 渲染切割方案DTO
 *
 * @property group 分组标识 / Group identifiers
 * @property productions 生产项列表 / List of productions
 * @property width 宽度 / Width
 * @property standardWidth 标准宽度 / Standard width
 * @property amount 数量 / Amount
 * @property info 附加信息 / Additional info
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
 * 渲染方案DTO
 *
 * @property kpi KPI metrics / KPI指标
 * @property cuttingPlans 切割方案列表 / List of cutting plans
*/
@Serializable
data class RenderSchemaDTO(
    val kpi: Map<String, String>,
    val cuttingPlans: List<RenderCuttingPlanDTO>,
)
