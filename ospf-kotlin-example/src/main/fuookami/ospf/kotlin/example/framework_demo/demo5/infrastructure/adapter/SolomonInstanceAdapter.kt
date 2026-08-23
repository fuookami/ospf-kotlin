@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.adapter

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.ServiceTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.VrptwValidator
import fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.io.*

/** Solomon 原始数值转换扩展点。 / Solomon raw-value conversion extension point. */
fun interface SolomonValueConverter<V : RealNumber<V>> {
    /**
     * 将 FltX 原始值转换为领域值。 / Convert a raw FltX value to a domain value.
     *
     * @param value 原始值 / Raw value
     * @return 转换后的值或失败 / Converted value or failure
     */
    fun convert(value: FltX): Ret<V>
}

/** Solomon Flt64 数值转换器。 / Solomon Flt64 value converter. */
object SolomonFlt64ValueConverter : SolomonValueConverter<Flt64> {
    override fun convert(value: FltX): Ret<Flt64> = ok(value.toFlt64())
}

/** Solomon FltX 数值转换器。 / Solomon FltX value converter. */
object SolomonFltXValueConverter : SolomonValueConverter<FltX> {
    override fun convert(value: FltX): Ret<FltX> = ok(value)
}

/**
 * 将 demo5 的 Solomon 输入装配为 framework VRPTW 实例。 / Assemble demo5 Solomon input into a framework VRPTW instance.
 *
 * @property converter 原始数值转换器 / Raw-value converter
 */
class SolomonInstanceAdapter<V : RealNumber<V>>(
    private val converter: SolomonValueConverter<V>
) {
    /**
     * 创建 VRPTW 实例。 / Create a VRPTW instance.
     *
     * @param data Solomon 原始数据 / Solomon raw data
     * @param units 实例单位 / Instance units
     * @param fixedVehicleCost 固定车辆成本 / Fixed vehicle cost
     * @param schedulingWindow 业务绝对时间轴与 solver 数值转换 / Business absolute timeline and solver numeric conversion
     * @param tolerances 算法容差 / Algorithm tolerances
     * @return VRPTW 实例或装配失败 / VRPTW instance or assembly failure
     */
    fun create(
        data: SolomonData,
        units: VrptwUnits,
        fixedVehicleCost: Quantity<V>,
        schedulingWindow: SchedulingTimeWindow<V>,
        tolerances: VrptwTolerances = VrptwTolerances.default
    ): Ret<VrptwInstance<V>> {
        val startNode = node(
            row = data.depot,
            id = NetworkNodeId("depot-start"),
            units = units
        ).value ?: return assemblyFailure("起始仓库节点创建失败", "start-depot node creation failed")
        val endNode = node(
            row = data.depot,
            id = NetworkNodeId("depot-end"),
            units = units
        ).value ?: return assemblyFailure("结束仓库节点创建失败", "end-depot node creation failed")
        val depotWindow = timeWindow(data.depot, schedulingWindow).value
            ?: return assemblyFailure("仓库时间窗创建失败", "depot time-window creation failed")
        val customers = mutableListOf<Customer<V>>()
        for (row in data.customers) {
            val customerNode = node(
                row = row,
                id = NetworkNodeId("customer-${row.id}"),
                units = units
            ).value ?: return assemblyFailure(
                "客户 ${row.id} 节点创建失败",
                "node creation failed for customer ${row.id}"
            )
            val demand = value(row.demand, units.loadUnit).value
                ?: return assemblyFailure(
                    "客户 ${row.id} 需求转换失败",
                    "demand conversion failed for customer ${row.id}"
                )
            val window = timeWindow(row, schedulingWindow).value
                ?: return assemblyFailure(
                    "客户 ${row.id} 时间窗创建失败",
                    "time-window creation failed for customer ${row.id}"
                )
            val serviceTime = duration(row.serviceTime, schedulingWindow).value
                ?: return assemblyFailure(
                    "客户 ${row.id} 服务时间转换失败",
                    "service-time conversion failed for customer ${row.id}"
                )
            val customer = Customer(
                id = CustomerId(row.id),
                node = customerNode,
                demand = demand,
                timeWindow = window,
                serviceTime = serviceTime
            ).value ?: return assemblyFailure(
                "客户 ${row.id} 校验失败",
                "validation failed for customer ${row.id}"
            )
            customers.add(customer)
        }
        val capacity = value(data.vehicle.capacity, units.loadUnit).value
            ?: return assemblyFailure("车辆容量转换失败", "vehicle-capacity conversion failed")
        val vehicleType = VehicleType(
            id = VehicleTypeId("solomon-vehicle"),
            capacity = capacity,
            fixedCost = fixedVehicleCost,
            amount = data.vehicle.amount
        ).value ?: return assemblyFailure("车辆类型创建失败", "vehicle-type creation failed")
        return VrptwValidator.create(
            name = data.name,
            startDepot = Depot(startNode, depotWindow),
            endDepot = Depot(endNode, depotWindow),
            customers = customers,
            vehicleTypes = listOf(vehicleType),
            schedulingWindow = schedulingWindow,
            units = units,
            tolerances = tolerances
        )
    }

    private fun node(
        row: SolomonNodeData,
        id: NetworkNodeId,
        units: VrptwUnits
    ): Ret<NetworkNode<V>> {
        val x = value(row.x, units.distanceUnit).value
            ?: return assemblyFailure("X 坐标转换失败", "X-coordinate conversion failed")
        val y = value(row.y, units.distanceUnit).value
            ?: return assemblyFailure("Y 坐标转换失败", "Y-coordinate conversion failed")
        return NetworkNode(
            id = id,
            attributes = mapOf("x" to x, "y" to y)
        )
    }

    private fun timeWindow(
        row: SolomonNodeData,
        schedulingWindow: SchedulingTimeWindow<V>
    ): Ret<ServiceTimeWindow> {
        val readyTime = instant(row.readyTime, schedulingWindow).value
            ?: return assemblyFailure("最早时刻转换失败", "ready-time conversion failed")
        val dueTime = instant(row.dueTime, schedulingWindow).value
            ?: return assemblyFailure("最晚时刻转换失败", "due-time conversion failed")
        return ServiceTimeWindow(readyTime, dueTime)
    }

    private fun instant(raw: FltX, schedulingWindow: SchedulingTimeWindow<V>): Ret<Instant> {
        val converted = converter.convert(raw).value
            ?: return assemblyFailure("数值转换失败", "numeric conversion failed")
        if (!converted.isFinite()) {
            return assemblyFailure("数值必须有限", "numeric value must be finite")
        }
        return ok(schedulingWindow.instantOf(converted))
    }

    private fun duration(raw: FltX, schedulingWindow: SchedulingTimeWindow<V>): Ret<Duration> {
        val converted = converter.convert(raw).value
            ?: return assemblyFailure("数值转换失败", "numeric conversion failed")
        if (!converted.isFinite()) {
            return assemblyFailure("数值必须有限", "numeric value must be finite")
        }
        return ok(schedulingWindow.durationOf(converted))
    }

    private fun value(raw: FltX, unit: PhysicalUnit): Ret<Quantity<V>> {
        val converted = converter.convert(raw).value
            ?: return assemblyFailure("数值转换失败", "numeric conversion failed")
        if (!converted.isFinite()) {
            return assemblyFailure("数值必须有限", "numeric value must be finite")
        }
        return ok(Quantity(converted, unit))
    }

    private fun <T> assemblyFailure(chinese: String, english: String): Ret<T> {
        return networkSchedulingFailure(
            "装配 Solomon 实例失败：$chinese / Failed to assemble Solomon instance: $english"
        )
    }
}
