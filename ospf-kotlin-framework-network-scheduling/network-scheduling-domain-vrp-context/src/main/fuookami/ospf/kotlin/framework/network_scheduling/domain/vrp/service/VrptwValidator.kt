@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.vrpCoordinateAttributes
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.vrpCoordinates

/** VRPTW 实例一致性校验器。 / VRPTW instance consistency validator. */
object VrptwValidator {
    /**
     * 校验输入并创建不可变实例。 / Validate inputs and create an immutable instance.
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
    fun <V : RealNumber<V>> create(
        name: String,
        startDepot: Depot<V>,
        endDepot: Depot<V>,
        customers: List<Customer<V>>,
        vehicleTypes: List<VehicleType<V>>,
        schedulingWindow: SchedulingTimeWindow<V>,
        units: VrptwUnits,
        tolerances: VrptwTolerances
    ): Ret<VrptwInstance<V>> {
        val failure = firstFailure(
            name = name,
            startDepot = startDepot,
            endDepot = endDepot,
            customers = customers,
            vehicleTypes = vehicleTypes,
            schedulingWindow = schedulingWindow,
            units = units
        )
        if (failure != null) {
            return networkSchedulingFailure(failure)
        }
        return ok(
            VrptwInstance.validated(
                name = name,
                startDepot = startDepot,
                endDepot = endDepot,
                customers = customers,
                vehicleTypes = vehicleTypes,
                schedulingWindow = schedulingWindow,
                units = units,
                tolerances = tolerances
            )
        )
    }

    /**
     * 重新校验已有实例。 / Revalidate an existing instance.
     *
     * @param instance VRPTW 实例 / VRPTW instance
     * @return 成功或校验失败 / Success or validation failure
     */
    fun <V : RealNumber<V>> validate(instance: VrptwInstance<V>): Try {
        val failure = firstFailure(
            name = instance.name,
            startDepot = instance.startDepot,
            endDepot = instance.endDepot,
            customers = instance.customers,
            vehicleTypes = instance.vehicleTypes,
            schedulingWindow = instance.schedulingWindow,
            units = instance.units
        )
        return failure?.let { networkSchedulingFailure(it) } ?: ok
    }

    private fun <V : RealNumber<V>> firstFailure(
        name: String,
        startDepot: Depot<V>,
        endDepot: Depot<V>,
        customers: List<Customer<V>>,
        vehicleTypes: List<VehicleType<V>>,
        schedulingWindow: SchedulingTimeWindow<V>,
        units: VrptwUnits
    ): String? {
        if (name.isBlank()) {
            return "创建 VRPTW 实例失败：实例名称不能为空 / Failed to create VRPTW instance: instance name cannot be blank"
        }
        if (customers.isEmpty()) {
            return "创建 VRPTW 实例失败：至少需要一个客户 / Failed to create VRPTW instance: at least one customer is required"
        }
        if (vehicleTypes.isEmpty()) {
            return "创建 VRPTW 实例失败：至少需要一种车辆类型 / Failed to create VRPTW instance: at least one vehicle type is required"
        }
        if (startDepot.node.id == endDepot.node.id) {
            return "创建 VRPTW 实例失败：起止仓库必须使用不同节点 ID / " +
                    "Failed to create VRPTW instance: start and end depots must use different node IDs"
        }
        val customerIds = customers.map { it.id }
        if (customerIds.toSet().size != customerIds.size) {
            return "创建 VRPTW 实例失败：客户 ID 重复 / Failed to create VRPTW instance: duplicate customer ID"
        }
        val vehicleTypeIds = vehicleTypes.map { it.id }
        if (vehicleTypeIds.toSet().size != vehicleTypeIds.size) {
            return "创建 VRPTW 实例失败：车辆类型 ID 重复 / Failed to create VRPTW instance: duplicate vehicle-type ID"
        }
        val allNodeIds = listOf(startDepot.node.id, endDepot.node.id) + customers.map { it.node.id }
        if (allNodeIds.toSet().size != allNodeIds.size) {
            return "创建 VRPTW 实例失败：网络节点 ID 重复 / Failed to create VRPTW instance: duplicate network node ID"
        }
        val nodes = listOf(startDepot.node, endDepot.node) + customers.map { it.node }
        val coordinateAxes = startDepot.node.vrpCoordinateAttributes().map { it.first }
        if (coordinateAxes.isEmpty() || nodes.any {
                it.vrpCoordinateAttributes().map { coordinate -> coordinate.first } != coordinateAxes
            }) {
            return "创建 VRPTW 实例失败：所有节点必须具有相同且非空的坐标轴 / " +
                    "Failed to create VRPTW instance: all nodes must have the same non-empty coordinate axes"
        }
        if (nodes.any { node -> node.vrpCoordinates().any { it.convertTo(units.distanceUnit) == null } }) {
            return "创建 VRPTW 实例失败：节点坐标无法转换到距离单位 / " +
                    "Failed to create VRPTW instance: node coordinates cannot be converted to the distance unit"
        }
        if (schedulingWindow.empty) {
            return "创建 VRPTW 实例失败：业务时间轴不能为空 / " +
                    "Failed to create VRPTW instance: business scheduling window cannot be empty"
        }
        val serviceWindows = listOf(startDepot.timeWindow, endDepot.timeWindow) + customers.map { it.timeWindow }
        if (serviceWindows.any { it.readyTime < schedulingWindow.start || it.dueTime > schedulingWindow.end }) {
            return "创建 VRPTW 实例失败：服务时间窗超出业务时间轴 / " +
                    "Failed to create VRPTW instance: service time window is outside the business scheduling window"
        }
        for (customer in customers) {
            val demand = customer.demand.convertTo(units.loadUnit)
                ?: return "创建 VRPTW 实例失败：客户 ${customer.id.value} 的需求单位不兼容 / " +
                        "Failed to create VRPTW instance: demand unit of customer ${customer.id.value} is incompatible"
            val supported = vehicleTypes.any { vehicle ->
                val capacity = vehicle.capacity.convertTo(units.loadUnit)
                capacity != null && demand.value.leq(capacity.value)
            }
            if (!supported) {
                return "创建 VRPTW 实例失败：客户 ${customer.id.value} 的需求超过所有车辆容量 / " +
                        "Failed to create VRPTW instance: demand of customer ${customer.id.value} exceeds every vehicle capacity"
            }
        }
        for (vehicleType in vehicleTypes) {
            if (vehicleType.capacity.convertTo(units.loadUnit) == null) {
                return "创建 VRPTW 实例失败：车辆类型 ${vehicleType.id.value} 的容量单位不兼容 / " +
                        "Failed to create VRPTW instance: capacity unit of vehicle type ${vehicleType.id.value} is incompatible"
            }
            if (vehicleType.fixedCost.convertTo(units.costUnit) == null) {
                return "创建 VRPTW 实例失败：车辆类型 ${vehicleType.id.value} 的成本单位不兼容 / " +
                        "Failed to create VRPTW instance: cost unit of vehicle type ${vehicleType.id.value} is incompatible"
            }
        }
        return null
    }
}
