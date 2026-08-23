@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.adapter

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.ServiceTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.example.core_demo.Demo17

/** 将仓库现有 Demo17 数据转换为可复现的 VRPTW fixture。 / Convert repository Demo17 data into reproducible VRPTW fixtures. */
object Demo17InstanceAdapter {
    /** @return 前 25 个客户的 Demo17 实例 / Demo17 instance with the first 25 customers */
    fun first25Customers(schedulingWindow: SchedulingTimeWindow<Flt64>): Ret<VrptwInstance<Flt64>> {
        return create(
            customerCount = 25,
            schedulingWindow = schedulingWindow
        )
    }

    /** @return 全部 100 个客户的 Demo17 实例 / Demo17 instance with all 100 customers */
    fun all100Customers(schedulingWindow: SchedulingTimeWindow<Flt64>): Ret<VrptwInstance<Flt64>> {
        return create(
            customerCount = 100,
            schedulingWindow = schedulingWindow
        )
    }

    /**
     * 从 Demo17 的前缀客户集创建实例。 / Create an instance from a prefix of Demo17 customers.
     *
     * @param customerCount 客户数量 / Number of customers
     * @param schedulingWindow 业务绝对时间轴与 solver 数值转换 / Business absolute timeline and solver numeric conversion
     * @return 对应实例或输入失败 / Matching instance or input failure
     */
    fun create(
        customerCount: Int,
        schedulingWindow: SchedulingTimeWindow<Flt64>
    ): Ret<VrptwInstance<Flt64>> {
        val origin = Demo17.nodes.filterIsInstance<Demo17.OriginNode>().singleOrNull()
            ?: return adapterFailure("起始仓库数量不是 1", "origin count is not one")
        val end = Demo17.nodes.filterIsInstance<Demo17.EndNode>().singleOrNull()
            ?: return adapterFailure("结束仓库数量不是 1", "end-depot count is not one")
        val demandNodes = Demo17.nodes.filterIsInstance<Demo17.DemandNode>()
        if (customerCount !in 1..demandNodes.size) {
            return adapterFailure(
                "客户数量 $customerCount 超出 1..${demandNodes.size}",
                "customer count $customerCount is outside 1..${demandNodes.size}"
            )
        }

        val units = VrptwUnits(
            distanceUnit = Meter,
            loadUnit = Kilogram,
            costUnit = NoneUnit
        ).value ?: return adapterFailure("单位配置非法", "unit configuration is invalid")
        val startNode = networkNode(origin, NetworkNodeId("demo17-start")).value
            ?: return adapterFailure("起始仓库节点创建失败", "origin node creation failed")
        val endNode = networkNode(end, NetworkNodeId("demo17-end")).value
            ?: return adapterFailure("结束仓库节点创建失败", "end-depot node creation failed")
        val startWindow = timeWindow(origin, schedulingWindow).value
            ?: return adapterFailure("起始仓库时间窗创建失败", "origin time-window creation failed")
        val endWindow = timeWindow(end, schedulingWindow).value
            ?: return adapterFailure("结束仓库时间窗创建失败", "end-depot time-window creation failed")

        val customers = mutableListOf<Customer<Flt64>>()
        for ((index, source) in demandNodes.take(customerCount).withIndex()) {
            val customerNumber = index + 1
            val node = networkNode(source, NetworkNodeId("demo17-customer-$customerNumber")).value
                ?: return adapterFailure(
                    "客户 $customerNumber 节点创建失败",
                    "node creation failed for customer $customerNumber"
                )
            val window = timeWindow(source, schedulingWindow).value
                ?: return adapterFailure(
                    "客户 $customerNumber 时间窗创建失败",
                    "time-window creation failed for customer $customerNumber"
                )
            val customer = Customer(
                id = CustomerId("demo17-customer-$customerNumber"),
                node = node,
                demand = Quantity(source.demand.toFlt64(), Kilogram),
                timeWindow = window,
                serviceTime = schedulingWindow.durationOf(source.serviceTime.toFlt64())
            ).value ?: return adapterFailure(
                "客户 $customerNumber 校验失败",
                "validation failed for customer $customerNumber"
            )
            customers.add(customer)
        }

        val vehicleTypes = Demo17.vehicles
            .groupBy { it.capacity to it.fixedUsedCost }
            .entries
            .mapIndexed { index, (specification, vehicles) ->
                VehicleType(
                    id = VehicleTypeId("demo17-vehicle-type-${index + 1}"),
                    capacity = Quantity(specification.first.toFlt64(), Kilogram),
                    fixedCost = Quantity(specification.second.toFlt64(), NoneUnit),
                    amount = vehicles.size
                ).value ?: return adapterFailure(
                    "车辆类型 ${index + 1} 校验失败",
                    "validation failed for vehicle type ${index + 1}"
                )
            }

        return VrptwInstance(
            name = "demo17-$customerCount",
            startDepot = Depot(startNode, startWindow),
            endDepot = Depot(endNode, endWindow),
            customers = customers,
            vehicleTypes = vehicleTypes,
            schedulingWindow = schedulingWindow,
            units = units,
            tolerances = VrptwTolerances.default
        )
    }

    private fun networkNode(source: Demo17.Node, id: NetworkNodeId): Ret<NetworkNode<Flt64>> {
        return NetworkNode(
            id = id,
            attributes = source.position.position.mapIndexed { index, value ->
                (if (index == 0) "x" else if (index == 1) "y" else "z") to Quantity(value, Meter)
            }.toMap()
        )
    }

    private fun timeWindow(
        source: Demo17.Node,
        schedulingWindow: SchedulingTimeWindow<Flt64>
    ): Ret<ServiceTimeWindow> {
        return ServiceTimeWindow(
            readyTime = schedulingWindow.instantOf(source.timeWindow.lowerBound.value.unwrap().toFlt64()),
            dueTime = schedulingWindow.instantOf(source.timeWindow.upperBound.value.unwrap().toFlt64())
        )
    }

    private fun <T> adapterFailure(chinese: String, english: String): Ret<T> {
        return networkSchedulingFailure(
            "转换 Demo17 数据失败：$chinese / Failed to adapt Demo17 data: $english"
        )
    }
}
