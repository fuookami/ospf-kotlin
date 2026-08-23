package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/** 车辆类型稳定标识。 / Stable vehicle-type identifier. */
@JvmInline
value class VehicleTypeId(val value: String)

/**
 * 有限车队中的车辆类型。 / Vehicle type in a finite fleet.
 *
 * @property id 车辆类型稳定标识 / Stable vehicle-type identifier
 * @property capacity 单车容量 / Per-vehicle capacity
 * @property fixedCost 固定使用成本 / Fixed usage cost
 * @property amount 可用数量 / Available amount
 */
class VehicleType<V : RealNumber<V>> private constructor(
    val id: VehicleTypeId,
    val capacity: Quantity<V>,
    val fixedCost: Quantity<V>,
    val amount: Int
) {
    companion object {
        /**
         * 创建车辆类型。 / Create a vehicle type.
         *
         * @param id 车辆类型稳定标识 / Stable vehicle-type identifier
         * @param capacity 正容量 / Positive capacity
         * @param fixedCost 非负固定成本 / Non-negative fixed cost
         * @param amount 正可用数量 / Positive available amount
         * @return 车辆类型或校验失败 / Vehicle type or validation failure
         */
        operator fun <V : RealNumber<V>> invoke(
            id: VehicleTypeId,
            capacity: Quantity<V>,
            fixedCost: Quantity<V>,
            amount: Int
        ): Ret<VehicleType<V>> {
            if (id.value.isBlank()) {
                return networkSchedulingFailure(
                    "创建车辆类型失败：车辆类型 ID 不能为空 / Failed to create vehicle type: vehicle type ID cannot be blank"
                )
            }
            if (capacity.value.leq(capacity.value.constants.zero)) {
                return networkSchedulingFailure(
                    "创建车辆类型失败：车辆容量必须为正 / Failed to create vehicle type: vehicle capacity must be positive"
                )
            }
            if (fixedCost.value ls fixedCost.value.constants.zero) {
                return networkSchedulingFailure(
                    "创建车辆类型失败：固定成本不能为负 / Failed to create vehicle type: fixed cost cannot be negative"
                )
            }
            if (amount <= 0) {
                return networkSchedulingFailure(
                    "创建车辆类型失败：可用数量必须为正 / Failed to create vehicle type: available amount must be positive"
                )
            }
            return ok(
                VehicleType(
                    id = id,
                    capacity = capacity,
                    fixedCost = fixedCost,
                    amount = amount
                )
            )
        }
    }
}
