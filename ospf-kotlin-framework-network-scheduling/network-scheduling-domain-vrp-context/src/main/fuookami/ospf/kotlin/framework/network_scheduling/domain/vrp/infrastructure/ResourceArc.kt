package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure

import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkArcId
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId

/**
 * 车辆资源维度的有向弧。 / Directed arc in a vehicle-resource dimension.
 *
 * @property resourceKey 资源稳定键 / Stable resource key
 * @property from 起点 / Origin
 * @property to 终点 / Destination
 * @property id 独立稳定弧标识 / Independent stable arc identity
 */
data class ResourceArc<K>(
    val resourceKey: K,
    val from: NetworkNodeId,
    val to: NetworkNodeId,
    val id: NetworkArcId = defaultResourceArcId(from, to)
)

/** 为端点唯一的旧式弧创建确定性 ID。 / Derive a deterministic ID for legacy endpoint-unique arcs. */
fun defaultResourceArcId(from: NetworkNodeId, to: NetworkNodeId): NetworkArcId =
    NetworkArcId("${from.value.length}:${from.value}${to.value.length}:${to.value}")
