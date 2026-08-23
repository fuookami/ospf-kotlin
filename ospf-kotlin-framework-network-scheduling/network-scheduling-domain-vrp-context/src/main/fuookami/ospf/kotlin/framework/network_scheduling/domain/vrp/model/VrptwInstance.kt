@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.VrptwValidator

/**
 * 已完成单位与引用校验的 VRPTW 实例。 / VRPTW instance with validated units and references.
 *
 * @property name 实例名称 / Instance name
 * @property startDepot 起始仓库 / Start depot
 * @property endDepot 结束仓库 / End depot
 * @property customers 客户快照 / Customer snapshot
 * @property vehicleTypes 车辆类型快照 / Vehicle-type snapshot
 * @property schedulingWindow 业务绝对时间轴与 solver 数值转换 / Business absolute timeline and solver numeric conversion
 * @property units 归一化单位 / Normalized units
 * @property tolerances 算法容差 / Algorithm tolerances
 */
class VrptwInstance<V : RealNumber<V>> private constructor(
    val name: String,
    val startDepot: Depot<V>,
    val endDepot: Depot<V>,
    customers: List<Customer<V>>,
    vehicleTypes: List<VehicleType<V>>,
    val schedulingWindow: SchedulingTimeWindow<V>,
    val units: VrptwUnits,
    val tolerances: VrptwTolerances
) {
    val customers: List<Customer<V>> = customers.toList()
    val vehicleTypes: List<VehicleType<V>> = vehicleTypes.toList()
    val customerById: Map<CustomerId, Customer<V>> = this.customers.associateBy { it.id }
    val customerByNodeId = this.customers.associateBy { it.node.id }
    val vehicleTypeById: Map<VehicleTypeId, VehicleType<V>> = this.vehicleTypes.associateBy { it.id }

    companion object {
        /** 由校验器创建已验证实例。 / Create an already validated instance from the validator. */
        internal fun <V : RealNumber<V>> validated(
            name: String,
            startDepot: Depot<V>,
            endDepot: Depot<V>,
            customers: List<Customer<V>>,
            vehicleTypes: List<VehicleType<V>>,
            schedulingWindow: SchedulingTimeWindow<V>,
            units: VrptwUnits,
            tolerances: VrptwTolerances
        ): VrptwInstance<V> {
            return VrptwInstance(
                name = name,
                startDepot = startDepot,
                endDepot = endDepot,
                customers = customers,
                vehicleTypes = vehicleTypes,
                schedulingWindow = schedulingWindow,
                units = units,
                tolerances = tolerances
            )
        }

        /**
         * 创建并完整校验实例。 / Create and fully validate an instance.
         *
         * @param name 实例名称 / Instance name
         * @param startDepot 起始仓库 / Start depot
         * @param endDepot 结束仓库 / End depot
         * @param customers 客户 / Customers
         * @param vehicleTypes 车辆类型 / Vehicle types
         * @param schedulingWindow 业务绝对时间轴与 solver 数值转换 / Business absolute timeline and solver numeric conversion
         * @param units 归一化单位 / Normalized units
         * @param tolerances 算法容差 / Algorithm tolerances
         * @return 实例或校验失败 / Instance or validation failure
         */
        operator fun <V : RealNumber<V>> invoke(
            name: String,
            startDepot: Depot<V>,
            endDepot: Depot<V>,
            customers: List<Customer<V>>,
            vehicleTypes: List<VehicleType<V>>,
            schedulingWindow: SchedulingTimeWindow<V>,
            units: VrptwUnits,
            tolerances: VrptwTolerances
        ): Ret<VrptwInstance<V>> {
            return VrptwValidator.create(
                name = name,
                startDepot = startDepot,
                endDepot = endDepot,
                customers = customers,
                vehicleTypes = vehicleTypes,
                schedulingWindow = schedulingWindow,
                units = units,
                tolerances = tolerances
            )
        }
    }
}
