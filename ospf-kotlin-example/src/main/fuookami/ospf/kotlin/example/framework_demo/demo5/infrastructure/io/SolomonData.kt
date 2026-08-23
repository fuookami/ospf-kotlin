package fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.io

import fuookami.ospf.kotlin.math.algebra.number.FltX

/** Solomon 车辆段原始数据。 / Raw Solomon vehicle-section data. */
data class SolomonVehicleData(
    val amount: Int,
    val capacity: FltX
)

/** Solomon 节点行原始数据。 / Raw Solomon node-row data. */
data class SolomonNodeData(
    val id: String,
    val x: FltX,
    val y: FltX,
    val demand: FltX,
    val readyTime: FltX,
    val dueTime: FltX,
    val serviceTime: FltX
)

/** Solomon 示例输入。 / Solomon demo input. */
data class SolomonData(
    val name: String,
    val vehicle: SolomonVehicleData,
    val depot: SolomonNodeData,
    val customers: List<SolomonNodeData>
)
