package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber

/**
 * 浪费最小化建模配置 / Waste minimization modeling configuration
 *
 * 当提供此配置时，Csp1dMilpSolver 在目标函数中加入余宽惩罚、物料成本惩罚和超产面积惩罚。
 * 所有权重为无量纲归一化系数，由调用方负责单位换算。
 *
 * @param V 数值类型 / Numeric value type
 * @property trimWidthPenalty 余宽惩罚权重（每单位余宽）/ Trim width penalty weight (per unit of trim width)
 * @property materialCostPenalty 按物料 ID 的单位成本惩罚 / Per-material unit cost penalty
 * @property overProductionAreaPenalty 超产面积惩罚权重 / Over-production area penalty weight
 */
data class WasteMinimizationConfig<V : RealNumber<V>>(
    val trimWidthPenalty: V? = null,
    val materialCostPenalty: Map<String, V> = emptyMap(),
    val overProductionAreaPenalty: V? = null
)

/**
 * 浪费最小化建模结果 / Waste minimization modeling result
 *
 * 从 solver solution 回填的浪费相关 KPI。
 *
 * @param V 数值类型 / Numeric value type
 * @property totalTrimWidth 总余宽 / Total trim width
 * @property materialCosts 按物料的成本 / Per-material costs
 * @property overProductionArea 总超产面积 / Total over-production area
 */
data class WasteMinimizationResult<V : RealNumber<V>>(
    val totalTrimWidth: V? = null,
    val materialCosts: List<ModeledMaterialCost<V>> = emptyList(),
    val overProductionArea: V? = null
)

/**
 * 物料成本建模结果 / Material cost modeling result
 *
 * @param V 数值类型 / Numeric value type
 * @property materialId 物料 ID / Material id
 * @property cost 成本值 / Cost value
 */
data class ModeledMaterialCost<V : RealNumber<V>>(
    val materialId: String,
    val cost: V
)
