package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.dimension.Length
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkArcId
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.defaultResourceArcId
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/**
 * 单车 elementary 路线。 / Elementary single-vehicle route.
 *
 * @property vehicleTypeId 车辆类型 / Vehicle type
 * @property stops 有序停靠点 / Ordered stops
 * @property arcIds 有序独立弧标识 / Ordered independent arc identities
 * @property distance 总距离 / Total distance
 * @property cost 总成本 / Total cost
 */
class Route<V : RealNumber<V>> private constructor(
    val vehicleTypeId: VehicleTypeId,
    stops: List<RouteStop<V>>,
    arcIds: List<NetworkArcId>,
    val distance: Quantity<V>,
    val cost: Quantity<V>
) {
    val stops: List<RouteStop<V>> = stops.toList()
    val arcIds: List<NetworkArcId> = arcIds.toList()

    /**
     * 路线去重签名。 / Route deduplication signature.
     *
     * 签名同时编码有序节点和稳定弧 ID，允许同端点平行弧形成不同路线列。
     * The signature encodes ordered nodes and stable arc IDs so parallel arcs remain distinct columns.
     */
    val signature: String = buildString {
        append(vehicleTypeId.value.length).append(':').append(vehicleTypeId.value)
        stops.forEach { stop ->
            append("|n").append(stop.nodeId.value.length).append(':').append(stop.nodeId.value)
        }
        arcIds.forEach { arcId ->
            append("|a").append(arcId.value.length).append(':').append(arcId.value)
        }
    }

    companion object {
        /**
         * 创建路线值对象。 / Create a route value object.
         *
         * @param vehicleTypeId 车辆类型 / Vehicle type
         * @param stops 有序停靠点 / Ordered stops
         * @param arcIds 有序独立弧标识，省略时按端点派生兼容 ID / Ordered independent arc identities; defaults to endpoint-derived IDs for compatibility
         * @param distance 总距离 / Total distance
         * @param cost 总成本 / Total cost
         * @return 路线或基础校验失败 / Route or basic validation failure
         */
        operator fun <V : RealNumber<V>> invoke(
            vehicleTypeId: VehicleTypeId,
            stops: List<RouteStop<V>>,
            distance: Quantity<V>,
            cost: Quantity<V>,
            arcIds: List<NetworkArcId> = stops.zipWithNext { from, to ->
                defaultResourceArcId(from.nodeId, to.nodeId)
            }
        ): Ret<Route<V>> {
            if (stops.size < 2) {
                return networkSchedulingFailure(
                    "创建路线失败：路线至少包含起止仓库 / Failed to create route: start and end depots are required"
                )
            }
            if (arcIds.size != stops.size - 1 || arcIds.any { it.value.isBlank() }) {
                return networkSchedulingFailure(
                    "创建路线失败：弧 ID 数量或内容无效 / Failed to create route: invalid arc-ID count or value"
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
                    arcIds = arcIds,
                    distance = distance,
                    cost = cost
                )
            )
        }
    }
}
