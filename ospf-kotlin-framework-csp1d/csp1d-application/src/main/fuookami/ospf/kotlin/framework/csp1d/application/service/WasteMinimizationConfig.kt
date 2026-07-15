package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MaterialId

/**
 * 超产面积度量口径 / Over-production area measure policy
*/
enum class OverProductionAreaMeasure {
    /** 使用产品最大宽度代理: over_i * product.maxWidth() / Use product max width proxy: over_i * product.maxWidth() */
    ProductMaxWidthProxy
}

/**
 * 余料度量口径 / Rest material measure policy
*/
enum class RestMaterialMeasure {
    /** 使用余宽乘物料长度代理: restWidth * material.length / Use rest width by material length proxy: restWidth * material.length */
    RestWidthByMaterialLengthProxy
}

/**
 * 浪费最小化建模配置 / Waste minimization modeling configuration
 *
 * 当提供此配置时，Csp1dMilpSolver 在目标函数中加入余宽惩罚、余料惩罚、物料成本惩罚和超产面积惩罚。
 * 所有权重为无量纲归一化系数，由调用方负责单位换算。
 *
 * @param V 数值类型 / Numeric value type
 * @property trimWidthPenalty 余宽惩罚权重（每单位余宽）/ Trim width penalty weight (per unit of trim width)
 * @property materialCostPenalty 按物料 ID 的单位成本惩罚 / Per-material unit cost penalty
 * @property overProductionAreaPenalty 超产面积惩罚权重 / Over-production area penalty weight
 * @property restMaterialPenalty 余料惩罚权重（每单位余料面积代理）/ Rest material penalty weight (per unit of rest material area proxy)
 * @property overProductionAreaMeasure 超产面积度量口径 / Over-production area measure policy
 * @property restMaterialMeasure 余料度量口径 / Rest material measure policy
*/
data class WasteMinimizationConfig<V : RealNumber<V>>(
    val trimWidthPenalty: V? = null,
    val materialCostPenalty: Map<MaterialId, V> = emptyMap(),
    val overProductionAreaPenalty: V? = null,
    val restMaterialPenalty: V? = null,
    val overProductionAreaMeasure: OverProductionAreaMeasure = OverProductionAreaMeasure.ProductMaxWidthProxy,
    val restMaterialMeasure: RestMaterialMeasure = RestMaterialMeasure.RestWidthByMaterialLengthProxy
)

/**
 * 浪费最小化建模结果 / Waste minimization modeling result
 *
 * 从 solver solution 回填的浪费相关 KPI。
 *
 * @param V 数值类型 / Numeric value type
 * @property totalTrimWidth 总余宽 / Total trim width
 * @property materialCosts 按物料的成本 / Per-material costs
 * @property overProductionArea 总超产面积代理 / Total over-production area proxy
 * @property totalRestMaterial 总余料面积代理 / Total rest material area proxy
 * @property overProductionAreaMeasure 超产面积度量口径 / Over-production area measure policy
 * @property restMaterialMeasure 余料度量口径 / Rest material measure policy
*/
data class WasteMinimizationResult<V : RealNumber<V>>(
    val totalTrimWidth: V? = null,
    val materialCosts: List<ModeledMaterialCost<V>> = emptyList(),
    val overProductionArea: V? = null,
    val totalRestMaterial: V? = null,
    val overProductionAreaMeasure: OverProductionAreaMeasure = OverProductionAreaMeasure.ProductMaxWidthProxy,
    val restMaterialMeasure: RestMaterialMeasure = RestMaterialMeasure.RestWidthByMaterialLengthProxy
)

/**
 * 物料成本建模结果 / Material cost modeling result
 *
 * @param V 数值类型 / Numeric value type
 * @property materialId 物料 ID / Material id
 * @property cost 成本值 / Cost value
*/
data class ModeledMaterialCost<V : RealNumber<V>>(
    val materialId: MaterialId,
    val cost: V
)
