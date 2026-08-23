@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model

import kotlin.time.Instant
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId

/**
 * 路线停靠点的完整资源状态。 / Complete resource state at a route stop.
 *
 * @property nodeId 网络节点 ID / Network node ID
 * @property customerId 客户 ID，仓库处为空 / Customer ID, null at depots
 * @property arrival 到达绝对时刻 / Absolute arrival instant
 * @property serviceStart 服务开始绝对时刻 / Absolute service-start instant
 * @property departure 离开绝对时刻 / Absolute departure instant
 * @property accumulatedLoad 累计负载 / Accumulated load
 */
data class RouteStop<V : RealNumber<V>>(
    val nodeId: NetworkNodeId,
    val customerId: CustomerId?,
    val arrival: Instant,
    val serviceStart: Instant,
    val departure: Instant,
    val accumulatedLoad: Quantity<V>
)
