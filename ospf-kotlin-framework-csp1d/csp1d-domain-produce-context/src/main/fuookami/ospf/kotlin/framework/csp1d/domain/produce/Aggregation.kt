package fuookami.ospf.kotlin.framework.csp1d.domain.produce

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage

/**
 * Input data for CSP1D produce context.
 * CSP1D 产出上下文的输入数据
 *
 * @property cuttingPlans Cutting plans to be used / 待使用的切割方案列表
 * @property demands Product demands to satisfy / 待满足的产品需求列表
 * @property materials Available materials / 可用物料列表
 * @property machines Available machines / 可用设备列表
 * @property warmStartPlanUsages Warm start plan usages for initial solution / 用于初始解的热启动方案使用量
*/
data class ProduceInput<V : RealNumber<V>>(
    val cuttingPlans: List<CuttingPlan<V>>,
    val demands: List<ProductDemand<V>>,
    val materials: List<Material<V>>,
    val machines: List<Machine<V>> = emptyList(),
    val warmStartPlanUsages: List<CuttingPlanUsage<V>> = emptyList()
)
