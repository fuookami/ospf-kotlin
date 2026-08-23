package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.dimension.Length
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/**
 * 单车 elementary 路线。 / Elementary single-vehicle route.
 *
 * @property vehicleTypeId 车辆类型 / Vehicle type
 * @property stops 有序停靠点 / Ordered stops
 * @property distance 总距离 / Total distance
 * @property cost 总成本 / Total cost
 */
class Route<V : RealNumber<V>> private constructor(
    val vehicleTypeId: VehicleTypeId,
    stops: List<RouteStop<V>>,
    val distance: Quantity<V>,
    val cost: Quantity<V>
) {
    val stops: List<RouteStop<V>> = stops.toList()

    /**
     * 路线去重签名。 / Route deduplication signature.
     *
     * 格式为"车辆类型 ID | 有序节点 ID 序列"。
     * 在无平行弧前提下，有序节点序列唯一确定了弧序列，因此节点序列去重
     * 与"车辆类型 ID + 有序节点/弧序列"去重语义等价。 / Format: "vehicle type ID | ordered node ID sequence".
     * Without parallel arcs, an ordered node sequence uniquely determines
     * the arc sequence, making node-sequence deduplication equivalent to
     * "vehicle type ID + ordered node/arc sequence" deduplication.
     */
    val signature: String = buildString {
        append(vehicleTypeId.value)
        stops.forEach { append('|').append(it.nodeId.value) }
    }

    companion object {
        /**
         * 创建路线值对象。 / Create a route value object.
         *
         * @param vehicleTypeId 车辆类型 / Vehicle type
         * @param stops 有序停靠点 / Ordered stops
         * @param distance 总距离 / Total distance
         * @param cost 总成本 / Total cost
         * @return 路线或基础校验失败 / Route or basic validation failure
         */
        operator fun <V : RealNumber<V>> invoke(
            vehicleTypeId: VehicleTypeId,
            stops: List<RouteStop<V>>,
            distance: Quantity<V>,
            cost: Quantity<V>
        ): Ret<Route<V>> {
            if (stops.size < 2) {
                return networkSchedulingFailure(
                    "创建路线失败：路线至少包含起止仓库 / Failed to create route: start and end depots are required"
                )
            }
            if (distance.unit.quantity != Length || distance.value ls distance.value.constants.zero) {
                return networkSchedulingFailure(
                    "创建路线失败：总距离必须是非负长度量 / Failed to create route: total distance must be a non-negative length quantity"
                )
            }
            if (cost.value ls cost.value.constants.zero) {
                return networkSchedulingFailure(
                    "创建路线失败：总成本不能为负 / Failed to create route: total cost cannot be negative"
                )
            }
            return ok(
                Route(
                    vehicleTypeId = vehicleTypeId,
                    stops = stops,
                    distance = distance,
                    cost = cost
                )
            )
        }
    }
}
