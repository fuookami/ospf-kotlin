package fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MaterialId
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dAggregation

/**
 * 浪费最小化聚合根 / Waste minimization aggregation root
 *
 * 管理浪费最小化的配置信息和目标项计算逻辑。
 * 浪费目标项直接作用于 x 变量（余宽*penalty*x、余料*penalty*x 等），
 * 不需要额外的 slack 变量。
 *
 * Manage waste minimization configuration and objective term computation.
 * Waste objective terms act directly on x variables (restWidth*penalty*x, restMaterial*penalty*x, etc.)
 * without additional slack variables.
 *
 * @param V 数值类型 / Numeric value type
 * @property cuttingPlans 切割方案列表 / Cutting plan list
 * @property trimWidthPenalty 余宽惩罚权重 / Trim width penalty weight
 * @property materialCostPenalty 按物料 ID 的单位成本惩罚 / Per-material unit cost penalty
 * @property overProductionAreaPenalty 超产面积惩罚权重 / Over-production area penalty weight
 * @property restMaterialPenalty 余料惩罚权重 / Rest material penalty weight
 */
class WasteAggregation<V : RealNumber<V>>(
    override val cuttingPlans: List<CuttingPlan<V>>,
    val trimWidthPenalty: V? = null,
    val materialCostPenalty: Map<MaterialId, V> = emptyMap(),
    val overProductionAreaPenalty: V? = null,
    val restMaterialPenalty: V? = null
) : Csp1dAggregation<V> {

    /**
     * 注册到元模型 / Register to meta model
     *
     * 浪费最小化不需要注册额外变量。
     * Waste minimization does not need to register additional variables.
     */
    override fun register(model: LinearMetaModel<Flt64>): Try = ok

    /**
     * 是否存在任何浪费惩罚配置 / Whether any waste penalty configuration exists
     */
    val hasAnyPenalty: Boolean get() = trimWidthPenalty != null ||
            materialCostPenalty.isNotEmpty() ||
            overProductionAreaPenalty != null ||
            restMaterialPenalty != null
}
