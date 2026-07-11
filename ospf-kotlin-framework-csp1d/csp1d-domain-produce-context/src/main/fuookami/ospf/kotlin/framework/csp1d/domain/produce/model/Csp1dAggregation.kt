package fuookami.ospf.kotlin.framework.csp1d.domain.produce.model

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan

/**
 * CSP1D 聚合根接口 / CSP1D aggregation root interface
 *
 * 定义聚合根的基本契约：注册变量和中间值到元模型。
 * 各 domain context 的 Aggregation 实现此接口，管理自己的变量集合。
 *
 * Define the basic contract for aggregation roots: register variables and intermediate
 * values to the meta model. Each domain context's Aggregation implements this interface,
 * managing its own variable set.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dAggregation<V : RealNumber<V>> {

    /**
     * 注册到元模型 / Register to meta model
     *
     * 将聚合根管理的变量和中间值注册到元模型。
     * Register the variables and intermediate values managed by this aggregation root to the meta model.
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
    */
    fun register(model: LinearMetaModel<Flt64>): Try

    /**
     * 聚合根管理的切割方案列表 / Cutting plans managed by this aggregation
    */
    val cuttingPlans: List<CuttingPlan<V>>
}
