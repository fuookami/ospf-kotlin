package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.ServiceTimeWindow

/**
 * VRPTW 起止仓库。 / VRPTW start or end depot.
 *
 * @property node 网络节点 / Network node
 * @property timeWindow 仓库开放时间窗 / Depot opening time window
 */
data class Depot<V : RealNumber<V>>(
    val node: NetworkNode<V>,
    val timeWindow: ServiceTimeWindow
)
