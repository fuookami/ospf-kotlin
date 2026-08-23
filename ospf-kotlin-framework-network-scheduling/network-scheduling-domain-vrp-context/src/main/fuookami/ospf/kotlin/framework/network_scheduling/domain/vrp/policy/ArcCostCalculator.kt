package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.VehicleType

/** 弧成本计算扩展点。 / Arc-cost calculation extension point. */
fun interface ArcCostCalculator<V : RealNumber<V>> {
    /**
     * 计算弧成本。 / Calculate arc cost.
     *
     * @param from 起点 / Origin
     * @param to 终点 / Destination
     * @param distance 距离 / Distance
     * @param travelTime 行驶时间 / Travel time
     * @param vehicleType 车辆类型 / Vehicle type
     * @return 弧成本或计算失败 / Arc cost or calculation failure
     */
    fun cost(
        from: NetworkNode<V>,
        to: NetworkNode<V>,
        distance: Quantity<V>,
        travelTime: Duration,
        vehicleType: VehicleType<V>
    ): Ret<Quantity<V>>
}

/** 将距离数值映射到成本单位的 Demo17 策略。 / Demo17 policy mapping distance values to the cost unit. */
class DistanceArcCostCalculator<V : RealNumber<V>>(
    private val distanceUnit: PhysicalUnit,
    private val costUnit: PhysicalUnit
) : ArcCostCalculator<V> {
    override fun cost(
        from: NetworkNode<V>,
        to: NetworkNode<V>,
        distance: Quantity<V>,
        travelTime: Duration,
        vehicleType: VehicleType<V>
    ): Ret<Quantity<V>> {
        val normalized = distance.convertTo(distanceUnit)
            ?: return networkSchedulingFailure(
                "计算弧成本失败：距离单位无法归一化 / Failed to calculate arc cost: distance unit cannot be normalized"
            )
        return ok(Quantity(normalized.value, costUnit))
    }
}
