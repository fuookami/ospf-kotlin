package fuookami.ospf.kotlin.framework.csp1d.domain.produce

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage

/**
 * Input data for CSP1D produce context.
 * CSP1D 产出上下文的输入数据
 *
 * @property cuttingPlans 待使用的切割方案列表 / Cutting plans to be used
 * @property demands 待满足的产品需求列表 / Product demands to satisfy
 * @property materials 可用物料列表 / Available materials
 * @property machines 可用设备列表 / Available machines
 * @property warmStartPlanUsages 用于初始解的热启动方案使用量 / Warm start plan usages for initial solution
*/
data class ProduceInput<V : RealNumber<V>>(
    val cuttingPlans: List<CuttingPlan<V>>,
    val demands: List<ProductDemand<V>>,
    val materials: List<Material<V>>,
    val machines: List<Machine<V>> = emptyList(),
    val warmStartPlanUsages: List<CuttingPlanUsage<V>> = emptyList()
)
