package fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto

import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.math.algebra.number.*

/** 渲染生产类型 / Render production type */
enum class RenderProductionType {
    /** 产品 / Product */
    Product,
    /** 联副产品 / Co-product */
    Costar
}

/** 渲染切割方案生产项DTO / Render cutting plan production DTO */
@Serializable
data class RenderCuttingPlanProductionDTO(
    /** 名称 / Name */
    val name: String,
    /** X坐标 / X coordinate */
    val x: FltX,
    /** 宽度 / Width */
    val width: FltX,
    /** 单位长度 / Unit length */
    val unitLength: FltX?,
    /** 生产类型 / Production type */
    val productionType: RenderProductionType,
    /** 附加信息 / Additional info */
    val info: Map<String, String>
)

/** 渲染切割方案DTO / Render cutting plan DTO */
@Serializable
data class RenderCuttingPlanDTO(
    /** 分组标识 / Group identifiers */
    val group: List<String>,
    /** 生产项列表 / List of productions */
    val productions: List<RenderCuttingPlanProductionDTO>,
    /** 宽度 / Width */
    val width: FltX,
    /** 标准宽度 / Standard width */
    val standardWidth: FltX,
    /** 数量 / Amount */
    val amount: UInt64,
    /** 附加信息 / Additional info */
    val info: Map<String, String>
)

/** 渲染方案DTO / Render schema DTO */
@Serializable
data class RenderSchemaDTO(
    /** KPI指标 / KPI metrics */
    val kpi: Map<String, String>,
    /** 切割方案列表 / List of cutting plans */
    val cuttingPlans: List<RenderCuttingPlanDTO>,
)
