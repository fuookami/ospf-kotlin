package fuookami.ospf.kotlin.framework.csp1d.domain.produce

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.UIntVariable1
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dAggregation

/**
 * 产出聚合根 / Produce aggregation root
 *
 * 管理 CSP1D 主问题的核心变量 x[0..n-1]（每个切割方案的使用车次）。
 * 替代原有 Csp1dAssignment 的职责，扩展为完整的建模组件。
 *
 * Manage the core variables x[0..n-1] (batch count per cutting plan) of the CSP1D master problem.
 * Replaces Csp1dAssignment with a full modeling component.
 *
 * @param V 数值类型 / Numeric value type
 * @property cuttingPlans 切割方案列表 / Cutting plan list
 * @property demands 需求列表 / Demand list
 * @property materials 物料列表 / Material list
 * @property machines 设备列表 / Machine list
 */
class ProduceAggregation<V : RealNumber<V>>(
    override val cuttingPlans: List<CuttingPlan<V>>,
    val demands: List<ProductDemand<V>>,
    val materials: List<Material<V>>,
    val machines: List<Machine<V>>
) : Csp1dAggregation<V> {

    /** 切割方案使用量变量 x[i] / Cutting plan batch count variable x[i] */
    val x: UIntVariable1 = UIntVariable1("x", Shape1(cuttingPlans.size))

    /** 方案数量 / Plan count */
    val planCount: Int get() = cuttingPlans.size

    /**
     * 获取指定索引的变量 / Get variable at specified index
     */
    operator fun get(index: Int) = x[index]

    /**
     * 注册到元模型 / Register to meta model
     *
     * 将 x 变量注册到元模型。
     * Register the x variable to the meta model.
     */
    override fun register(model: LinearMetaModel<Flt64>): Try {
        return when (val result = model.add(x)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
}
