package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure

import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId

/**
 * 车辆资源维度的有向弧。 / Directed arc in a vehicle-resource dimension.
 *
 * @property resourceKey 资源稳定键 / Stable resource key
 * @property from 起点 / Origin
 * @property to 终点 / Destination
 */
data class ResourceArc<K>(
    val resourceKey: K,
    val from: NetworkNodeId,
    val to: NetworkNodeId
)
