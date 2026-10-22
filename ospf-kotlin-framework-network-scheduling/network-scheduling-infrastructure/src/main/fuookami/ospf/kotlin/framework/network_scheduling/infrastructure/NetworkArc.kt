package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 带物理量的有向网络弧。 / Directed network arc with physical quantities.
 *
 * @property from 起点 / Origin
 * @property id 稳定弧标识 / Stable arc identifier
 * @property to 终点 / Destination
 * @property cost 弧成本 / Arc cost
 * @property attributes 通用弧属性 / Generic arc attributes
 */
class NetworkArc<V : RealNumber<V>> private constructor(
    val id: NetworkArcId,
    val from: NetworkNodeId,
    val to: NetworkNodeId,
    val cost: Quantity<V>,
    attributes: Map<String, Quantity<V>>
) {
    val attributes: Map<String, Quantity<V>> = attributes.toMap()

    companion object {
        /**
         * 创建并校验网络弧。 / Create and validate a network arc.
         *
         * @param from 起点 / Origin
         * @param to 终点 / Destination
         * @param cost 弧成本 / Arc cost
         * @param attributes 通用弧属性 / Generic arc attributes
         * @param id 稳定弧标识，平行弧必须显式指定 / Stable identity, required explicitly for parallel arcs
         * @return 网络弧或校验失败 / Network arc or validation failure
         */
        operator fun <V : RealNumber<V>> invoke(
            from: NetworkNodeId,
            to: NetworkNodeId,
            cost: Quantity<V>,
            attributes: Map<String, Quantity<V>> = emptyMap(),
            id: NetworkArcId = NetworkArcId("${from.value.length}:${from.value}${to.value.length}:${to.value}")
        ): Ret<NetworkArc<V>> {
            if (id.value.isBlank()) {
                return networkSchedulingFailure(
                    "创建网络弧失败：弧标识不能为空 / Failed to create network arc: blank identifier"
                )
            }
            return ok(NetworkArc(id = id, from = from, to = to, cost = cost, attributes = attributes))
        }
    }
}
