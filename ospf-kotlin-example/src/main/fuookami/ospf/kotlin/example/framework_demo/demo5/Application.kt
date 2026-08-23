@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo5

import kotlin.time.DurationUnit
import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.Flt64NetworkSchedulingSolverValueAdapter
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.adapter.Demo17InstanceAdapter
import fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.dto.*

/**
 * demo5 VRPTW 分支定价求解应用。 / demo5 VRPTW branch-and-price solver application.
 *
 * 接线：VrptwApplicationService → BranchAndPriceAlgorithm → BranchNodeSolver → ColumnGenerationSolver
 * 不重新定义 Graph、Label、Pricer、RouteCompilation 或 BranchAndPriceAlgorithm。
 *
 * Wiring: VrptwApplicationService → BranchAndPriceAlgorithm → BranchNodeSolver → ColumnGenerationSolver
 * Does not redefine Graph, Label, Pricer, RouteCompilation or BranchAndPriceAlgorithm.
 */
object Application {
    /**
     * 默认业务时间轴。 / Default business timeline.
     *
     * 1 小时窗口，精度为秒，与 Demo17 原始数值 1:1 对应。 / 1-hour window, duration unit = seconds, 1:1 mapping with Demo17 raw values.
     */
    fun defaultSchedulingWindow(): SchedulingTimeWindow<Flt64> = SchedulingTimeWindow(
        window = TimeRange(
            start = Instant.parse("2026-01-01T00:00:00Z"),
            end = Instant.parse("2026-01-01T01:00:00Z")
        ),
        durationUnit = DurationUnit.SECONDS,
        fromDouble = { Flt64(it) },
        toDouble = { it.toDouble() }
    )

    /**
     * 求解 VRPTW 实例。 / Solve VRPTW instance.
     *
     * @param instance VRPTW 实例 / VRPTW instance
     * @param parameter 语义参数 / Semantic parameters
     * @return 求解结果 / Solve result
     */
    suspend fun solve(
        instance: VrptwInstance<Flt64>,
        parameter: SemanticParameter = SemanticParameter()
    ): Ret<VrptwSolveResult<Flt64>> {
        val schedulingWindow = instance.schedulingWindow

        // 构造策略 / Construct policies
        val distanceCalculator: DistanceCalculator<Flt64> = when (parameter.costPolicy) {
            "solomon" -> SolomonDistancePolicy(Meter)
            else -> EuclideanDistanceCalculator(Meter)
        }
        val travelTimeCalculator = DistanceAsTravelTimeCalculator<Flt64>(
            distanceCalculator, schedulingWindow
        )
        val arcCostCalculator = DistanceArcCostCalculator<Flt64>(Meter, NoneUnit)
        val routeCostPolicy: RouteCostPolicy<Flt64> = when (parameter.costPolicy) {
            "solomon" -> FixedPlusArcCostPolicy(NoneUnit)
            else -> Demo17CostPolicy(NoneUnit)
        }

        // 构造求解器 / Construct solver
        val solver = LinearSolverBuilder(
            solver = parameter.solver,
            config = SolverConfig(time = parameter.timeLimit)
        )

        // 构造应用服务 / Construct application service
        val service = VrptwApplicationService(
            instance = instance,
            solver = solver,
            configuration = BranchAndPriceAlgorithm.Configuration(
                timeLimit = parameter.timeLimit,
                nodeLimit = parameter.nodeLimit,
                relativeGapTolerance = parameter.relativeGapTolerance,
                maxColumnsPerPricing = parameter.maxColumnsPerPricing,
                maxCGIterationsPerNode = parameter.maxCGIterationsPerNode
            ),
            policy = BranchAndPriceAlgorithm.Policy(
                valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
                distanceCalculator = distanceCalculator,
                travelTimeCalculator = travelTimeCalculator,
                arcCostCalculator = arcCostCalculator,
                routeCostPolicy = routeCostPolicy
            )
        )

        return service.solve()
    }

    /**
     * 将求解结果转换为输出 DTO。 / Convert solve result to output DTO.
     */
    fun toOutput(result: VrptwSolveResult<Flt64>): Output {
        val solution = result.solution
        val routes = solution?.routes?.map { route ->
            RouteOutput(
                vehicleTypeId = route.vehicleTypeId.value,
                stops = route.stops.map { stop ->
                    StopOutput(
                        nodeId = stop.nodeId.value,
                        customerId = stop.customerId?.value,
                        arrival = stop.arrival.toString(),
                        serviceStart = stop.serviceStart.toString(),
                        departure = stop.departure.toString(),
                        accumulatedLoad = stop.accumulatedLoad.value.toDouble()
                    )
                },
                cost = route.cost.value.toDouble(),
                distance = route.distance.value.toDouble(),
                load = route.stops.lastOrNull()?.accumulatedLoad?.value?.toDouble() ?: 0.0
            )
        } ?: emptyList()

        return Output(
            status = result.status,
            routes = routes,
            totalCost = solution?.totalCost?.value?.toDouble() ?: 0.0,
            totalDistance = solution?.totalDistance?.value?.toDouble() ?: 0.0,
            vehiclesUsed = solution?.routes?.size ?: 0,
            trace = TraceOutput(
                nodesExplored = result.trace.nodesExplored,
                nodesPruned = result.trace.nodesPruned,
                globalLowerBound = result.trace.globalLowerBound.toDouble(),
                globalUpperBound = result.trace.globalUpperBound.toDouble(),
                relativeGap = result.trace.relativeGap.toDouble(),
                totalTimeMs = result.trace.totalTime.inWholeMilliseconds,
                totalIterations = result.trace.totalIterations
            )
        )
    }
}
