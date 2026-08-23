package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.VehicleType

/** 路线总成本计算扩展点。 / Route total-cost calculation extension point. */
fun interface RouteCostPolicy<V : RealNumber<V>> {
    /**
     * 汇总固定成本与弧成本。 / Aggregate fixed and arc costs.
     *
     * @param vehicleType 车辆类型 / Vehicle type
     * @param arcCosts 弧成本 / Arc costs
     * @return 路线成本或计算失败 / Route cost or calculation failure
     */
    fun cost(vehicleType: VehicleType<V>, arcCosts: List<Quantity<V>>): Ret<Quantity<V>>
}

/** 固定成本加弧成本策略。 / Fixed-cost-plus-arc-cost policy. */
open class FixedPlusArcCostPolicy<V : RealNumber<V>>(
    private val costUnit: PhysicalUnit
) : RouteCostPolicy<V> {
    override fun cost(vehicleType: VehicleType<V>, arcCosts: List<Quantity<V>>): Ret<Quantity<V>> {
        val fixed = vehicleType.fixedCost.convertTo(costUnit)
            ?: return incompatibleCost("车辆固定成本", "vehicle fixed cost")
        var total = fixed.value
        for (arcCost in arcCosts) {
            val normalized = arcCost.convertTo(costUnit)
                ?: return incompatibleCost("弧成本", "arc cost")
            total += normalized.value
        }
        return ok(Quantity(total, costUnit))
    }

    private fun incompatibleCost(chinese: String, english: String): Ret<Quantity<V>> {
        return networkSchedulingFailure(
            "计算路线成本失败：$chinese 单位不兼容 / Failed to calculate route cost: incompatible $english unit"
        )
    }
}

/** Demo17 固定车辆成本加完整精度距离成本策略。 / Demo17 fixed-vehicle plus full-precision distance cost policy. */
class Demo17CostPolicy<V : RealNumber<V>>(costUnit: PhysicalUnit) : FixedPlusArcCostPolicy<V>(costUnit)
