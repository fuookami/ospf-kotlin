package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing

import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.ResourceArc
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.Route
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.VehicleTypeId
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber

/** 提供与路线弧可加的扩展主问题列系数。 / Provides additive extra-master coefficients for route arcs. */
interface RouteArcCoefficientProvider<V : RealNumber<V>> {
    /** 返回该弧在各扩展主问题行中的系数。 / Return coefficients of this arc in extra master rows.
     * @param arc 资源弧 / Resource arc
     * @return 按稳定行键索引的领域数值系数 / Domain-valued coefficients keyed by stable row IDs
     */
    fun coefficients(arc: ResourceArc<VehicleTypeId>): Map<String, V>

    /** 汇总路线全部弧系数；用于确保 RMP 新列与定价使用同一系数定义。
     * Sum the arc coefficients of a route so RMP columns and pricing share one definition.
     * @param route 路线 / Route
     * @return 稳定行键到路线系数的映射 / Route coefficients keyed by stable row IDs
     */
    fun routeCoefficients(route: Route<V>): Map<String, V> {
        val result = mutableMapOf<String, V>()
        val zero = route.cost.value.constants.zero
        for (index in route.arcIds.indices) {
            val arc = ResourceArc(
                resourceKey = route.vehicleTypeId,
                from = route.stops[index].nodeId,
                to = route.stops[index + 1].nodeId,
                id = route.arcIds[index]
            )
            for ((key, value) in coefficients(arc)) {
                result[key] = (result[key] ?: zero) + value
            }
        }
        return result.toMap()
    }
}
