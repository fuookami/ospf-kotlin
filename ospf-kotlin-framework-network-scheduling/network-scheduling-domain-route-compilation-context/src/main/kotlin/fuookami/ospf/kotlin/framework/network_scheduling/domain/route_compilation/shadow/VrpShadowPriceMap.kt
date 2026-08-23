package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow

import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * VRP 影子价格映射。 / VRP shadow price map.
 *
 * 扩展框架 AbstractShadowPriceMap，提供按客户和车辆类型读取对偶的稳定 API，
 * 并输出不可变 PricingDuals 快照，保持 generation 与 compilation 单向解耦。
 *
 * Extends the framework AbstractShadowPriceMap, provides a stable API for reading
 * duals by customer and vehicle type, and outputs an immutable PricingDuals snapshot,
 * maintaining one-way decoupling between generation and compilation.
 */
class VrpShadowPriceMap : AbstractShadowPriceMap<VrpShadowPriceArguments, VrpShadowPriceMap>() {

    /**
     * 获取客户覆盖对偶。 / Get customer-coverage dual.
     *
     * @param customerId 客户标识 / Customer identifier
     * @return 对偶值，不存在时返回 0 / Dual value, 0 if not found
     */
    fun customerDual(customerId: CustomerId): Flt64 {
        return map[CustomerCoverageShadowPriceKey(customerId)]?.price ?: Flt64.zero
    }

    /**
     * 获取车队数量对偶。 / Get fleet-size dual.
     *
     * 返回 solver 提供的 <= 车队约束的框架有符号 LP 对偶变量值。
     * 当前最小化实现与测试使用非正值；reduced cost 公式直接按
     * `reducedCost = objectiveCost - customerDual - fleetDual` 使用该值。
     * 这与 Gantt 模块的 `reducedCost` 函数（`cost -= dual`）符号约定一致。
     *
     * Returns the framework-signed LP dual value for the <= fleet constraint.
     * The current minimization implementation and tests use a non-positive value;
     * the reduced-cost formula consumes that signed value directly.
     * This is consistent with the Gantt module's `reducedCost` function (`cost -= dual`).
     *
     * @param vehicleTypeId 车辆类型标识 / Vehicle type identifier
     * @return 对偶值，不存在时返回 0 / Dual value, 0 if not found
     */
    fun fleetDual(vehicleTypeId: VehicleTypeId): Flt64 {
        return map[FleetSizeShadowPriceKey(vehicleTypeId)]?.price ?: Flt64.zero
    }

    /**
     * 输出不可变定价对偶快照。 / Output immutable pricing-dual snapshot.
     *
     * @param phase 当前定价阶段 / Current pricing phase
     * @return 不可变 PricingDuals / Immutable PricingDuals
     */
    fun toPricingDuals(phase: PricingPhase): PricingDuals {
        val customerDuals = mutableMapOf<CustomerId, Flt64>()
        val fleetDuals = mutableMapOf<VehicleTypeId, Flt64>()

        for ((key, shadowPrice) in map) {
            when (key) {
                is CustomerCoverageShadowPriceKey -> {
                    customerDuals[key.customerId] = shadowPrice.price
                }
                is FleetSizeShadowPriceKey -> {
                    fleetDuals[key.vehicleTypeId] = shadowPrice.price
                }
            }
        }

        return PricingDuals(
            phase = phase,
            customer = customerDuals,
            fleet = fleetDuals
        )
    }
}

/**
 * VRP 影子价格参数。 / VRP shadow price arguments.
 *
 * 用于 AbstractShadowPriceMap 的 invoke(arg) 计算 reduced cost。
 * Used by AbstractShadowPriceMap's invoke(arg) for reduced cost computation.
 */
data class VrpShadowPriceArguments(
    val vehicleTypeId: VehicleTypeId,
    val visitedCustomerIds: Set<CustomerId>
)
