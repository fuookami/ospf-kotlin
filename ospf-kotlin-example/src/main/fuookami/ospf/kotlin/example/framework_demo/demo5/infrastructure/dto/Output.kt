package fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.dto

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.BranchAndPriceStatus
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.BranchAndPriceTrace

/**
 * demo5 输出 DTO。 / demo5 output DTO.
 *
 * @property status 求解状态 / Solve status
 * @property routes 路线列表 / Route list
 * @property totalCost 总成本 / Total cost
 * @property totalDistance 总距离 / Total distance
 * @property vehiclesUsed 使用的车辆数 / Vehicles used
 * @property trace 执行轨迹 / Execution trace
 */
data class Output(
    val status: BranchAndPriceStatus,
    val routes: List<RouteOutput>,
    val totalCost: Double,
    val totalDistance: Double,
    val vehiclesUsed: Int,
    val trace: TraceOutput
)

/**
 * 路线输出 DTO。 / Route output DTO.
 */
data class RouteOutput(
    val vehicleTypeId: String,
    val stops: List<StopOutput>,
    val cost: Double,
    val distance: Double,
    val load: Double
)

/**
 * 停靠点输出 DTO。 / Stop output DTO.
 */
data class StopOutput(
    val nodeId: String,
    val customerId: String?,
    val arrival: String,
    val serviceStart: String,
    val departure: String,
    val accumulatedLoad: Double
)

/**
 * 轨迹输出 DTO。 / Trace output DTO.
 */
data class TraceOutput(
    val nodesExplored: Int,
    val nodesPruned: Int,
    val globalLowerBound: Double,
    val globalUpperBound: Double,
    val relativeGap: Double,
    val totalTimeMs: Long,
    val totalIterations: Int
)
