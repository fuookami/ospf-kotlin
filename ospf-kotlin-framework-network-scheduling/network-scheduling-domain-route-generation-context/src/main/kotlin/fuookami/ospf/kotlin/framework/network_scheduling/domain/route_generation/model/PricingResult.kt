package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.Route

/**
 * 定价结果。 / Pricing result.
 *
 * 不可变输出合同，包含本次定价找到的路线、最小 reduced cost
 * 以及精确定价是否已完成。 / Immutable output contract containing the routes found in this pricing call,
 * the minimum reduced cost, and whether exact pricing is complete.
 *
 * 列数截断只能限制加入 RMP 的列，不能把"找到但未返回的负列"
 * 误报为定价收敛。 / Column count truncation can only limit the columns added to the RMP;
 * it must NOT misreport "found but unreturned negative columns" as
 * pricing convergence.
 *
 * @property routes 找到的负 reduced-cost 路线 / Found negative reduced-cost routes
 * @property minReducedCost 本次最小 reduced cost / Minimum reduced cost in this call
 * @property exactPricingComplete 精确定价是否完成（所有车辆类型均无负列） / Whether exact pricing is complete
 * @property interrupted 定价是否被资源上限中断 / Whether pricing was interrupted by a resource limit
 */
data class PricingResult<V : RealNumber<V>>(
    val routes: List<Route<V>>,
    val minReducedCost: V,
    val exactPricingComplete: Boolean,
    val interrupted: Boolean = false
)
