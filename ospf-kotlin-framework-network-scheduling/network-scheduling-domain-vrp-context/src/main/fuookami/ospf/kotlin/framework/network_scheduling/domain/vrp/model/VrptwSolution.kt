package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 已校验路线集合及其汇总指标。 / Validated route set and aggregate metrics.
 *
 * @property routes 路线快照 / Route snapshot
 * @property totalDistance 总距离 / Total distance
 * @property totalCost 总成本 / Total cost
 */
class VrptwSolution<V : RealNumber<V>>(
    routes: List<Route<V>>,
    val totalDistance: Quantity<V>,
    val totalCost: Quantity<V>
) {
    val routes: List<Route<V>> = routes.toList()
}
