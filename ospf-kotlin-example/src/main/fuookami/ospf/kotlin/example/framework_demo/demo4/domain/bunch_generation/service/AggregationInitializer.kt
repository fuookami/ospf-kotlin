@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service

import kotlin.time.*
import kotlin.time.Duration.Companion.hours
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * Injectable aggregation initialization boundary for the bunch-generation context.
 * 批次生成上下文可注入的聚合初始化边界。
 */
fun interface AggregationInitializerProvider {
    /**
     * Initializes the bunch-generation aggregation.
     * 初始化批次生成聚合。
     */
    operator fun invoke(
        aircrafts: List<Aircraft>,
        aircraftUsability: Map<Aircraft, AircraftUsability>,
        flightTasks: List<FlightTask>,
        originBunches: List<FlightTaskBunch>,
        lock: Lock,
        flightTaskFeasibilityJudger: FlightTaskFeasibilityJudger,
        initialFlightTaskBunchGenerator: InitialFlightTaskBunchGenerator,
        withOrderChange: Boolean
    ): Ret<Aggregation>
}

/** 使用图、可反转对和初始批次初始化批次生成聚合。Initializes the bunch generation aggregation with graphs, reverse pairs, and initial bunches. */
class AggregationInitializer {
    private data class GraphGenerationResult(
        val graphs: Map<Aircraft, Graph>,
        val diagnostics: Map<Aircraft, RouteGraphDiagnostics>
    )

    companion object {
        val config = FlightTaskFeasibilityJudger.Config(
            checkEnabledTime = false,
            timeExtractor = { it.time }
        )
    }

    /**
     * 初始化批次生成的聚合。Initializes the aggregation for bunch generation.
     *
     * @param aircrafts 飞机列表 / list of aircraft
     * @param aircraftUsability 飞机可用性映射 / aircraft usability map
     * @param flightTasks 航班任务列表 / list of flight tasks
     * @param originBunches 原始航班任务束 / original flight task bunches
     * @param lock 锁定约束 / lock constraints
     * @param flightTaskFeasibilityJudger 航班任务可行性判断器 / flight task feasibility judger
     * @param initialFlightTaskBunchGenerator 初始航班任务束生成器 / initial flight task bunch generator
     * @param withOrderChange 是否启用换序 / whether order change is enabled
     * @return 初始化后的聚合 / the initialized aggregation
    */
    operator fun invoke(
        aircrafts: List<Aircraft>,
        aircraftUsability: Map<Aircraft, AircraftUsability>,
        flightTasks: List<FlightTask>,
        originBunches: List<FlightTaskBunch>,
        lock: Lock,
        flightTaskFeasibilityJudger: FlightTaskFeasibilityJudger,
        initialFlightTaskBunchGenerator: InitialFlightTaskBunchGenerator,
        withOrderChange: Boolean = false
    ): Ret<Aggregation> {
        val reverse = when (val ret = initReverseEnabledFlight(flightTasks, originBunches, lock, withOrderChange)) {
            is Ok -> ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }

        val flightTaskGroups = groupFlightTasks(flightTasks)
        val graphGenerator = RouteGraphGenerator(
            reverse,
            Configuration(withOrderChange = withOrderChange)
        ) { aircraft: Aircraft, prevFlightTask: FlightTask?, succFlightTask: FlightTask ->
            flightTaskFeasibilityJudger(aircraft, prevFlightTask, succFlightTask, config)
        }

        val graphGeneration = when (val ret = generateGraphSingleThread(aircrafts, aircraftUsability, flightTaskGroups, graphGenerator)) {
            is Ok -> ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }

        val initialFlightBunches = when (val ret = generateInitialFlightTaskBunches(aircrafts, aircraftUsability, flightTasks, originBunches, initialFlightTaskBunchGenerator)) {
            is Ok -> ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }

        return Ok(
            Aggregation(
                graphs = graphGeneration.graphs,
                reverse = reverse,
                initialFlightBunches = initialFlightBunches,
                routeGraphDiagnostics = graphGeneration.diagnostics
            )
        )
    }

    /**
     * Groups flight tasks by departure airport. / 按出发机场对航班任务分组。
     *
     * @param flightTasks 要分组的航班任务列表 / list of flight tasks to group
     * @return 按出发机场分组的航班任务 / flight tasks grouped by departure airport
    */
    private fun groupFlightTasks(flightTasks: List<FlightTask>): Map<Airport, List<FlightTask>> {
        val flightTaskGroups = HashMap<Airport, MutableList<FlightTask>>()
        for (flightTask in flightTasks) {
            if (!flightTaskGroups.containsKey(flightTask.dep)) {
                flightTaskGroups[flightTask.dep] = ArrayList()
            }
            flightTaskGroups[flightTask.dep]!!.add(flightTask)

            for (dep in flightTask.depBackup) {
                if (!flightTaskGroups.containsKey(dep)) {
                    flightTaskGroups[dep] = ArrayList()
                }
                flightTaskGroups[dep]!!.add(flightTask)
            }
        }
        for ((_, thisFlightTasks) in flightTaskGroups) {
            thisFlightTasks.sortBy { it.time?.start ?: it.timeWindow?.start ?: Instant.DISTANT_FUTURE }
        }
        return flightTaskGroups
    }

    /**
     * Initializes reverse-enabled flight pairs. / 初始化可反转航班对。
     *
     * @param flightTasks 航班任务列表 / list of flight tasks
     * @param originBunches 原始航班任务束 / original flight task bunches
     * @param lock 锁定约束 / lock constraints
     * @param withOrderChange 是否启用换序 / whether order change is enabled
     * @return 航班任务反转对象 / the flight task reverse object
    */
    private fun initReverseEnabledFlight(
        flightTasks: List<FlightTask>,
        originBunches: List<FlightTaskBunch>,
        lock: Lock,
        withOrderChange: Boolean
    ): Ret<FlightTaskReverse> {
        var timeDifferenceLimit = FlightTaskReverse.defaultTimeDifferenceLimit
        val pairs = ArrayList<Pair<FlightTask, FlightTask>>()
        if (withOrderChange && flightTasks.size >= 2) {
            while (true) {
                pairs.clear()
                for (i in flightTasks.indices) {
                    for (j in (i + 1) until flightTasks.size) {
                        if (FlightTaskReverse.symmetrical(flightTasks[i], flightTasks[j], lock, timeDifferenceLimit + (FlightTaskReverse.defaultTimeDifferenceLimit / 2.0))) {
                            pairs.add(Pair(flightTasks[i], flightTasks[j]))
                        } else {
                            if (FlightTaskReverse.reverseEnabled(flightTasks[i], flightTasks[j], lock, timeDifferenceLimit)) {
                                pairs.add(Pair(flightTasks[i], flightTasks[j]))
                            }
                            if (FlightTaskReverse.reverseEnabled(flightTasks[j], flightTasks[i], lock, timeDifferenceLimit)) {
                                pairs.add(Pair(flightTasks[j], flightTasks[i]))
                            }
                        }
                    }
                }

                if (timeDifferenceLimit == 0.hours) {
                    break
                } else if (pairs.size <= FlightTaskReverse.criticalSize.toInt() && timeDifferenceLimit <= 3.hours) {
                    break
                } else {
                    timeDifferenceLimit -= 1.hours
                }
            }
        }
        return Ok(FlightTaskReverse(pairs, originBunches, lock, timeDifferenceLimit))
    }

    /**
     * Generates route graphs for all aircraft in a single thread. / 单线程生成所有飞机的路线图。
     *
     * @param aircrafts 飞机列表 / list of aircraft
     * @param aircraftUsability 飞机可用性映射 / aircraft usability map
     * @param flightTaskGroups 按出发机场分组的航班任务 / flight tasks grouped by departure airport
     * @param graphGenerator 路线图生成器 / route graph generator
     * @return 按飞机索引的路线图 / route graphs keyed by aircraft
    */
    private fun generateGraphSingleThread(
        aircrafts: List<Aircraft>,
        aircraftUsability: Map<Aircraft, AircraftUsability>,
        flightTaskGroups: Map<Airport, List<FlightTask>>,
        graphGenerator: RouteGraphGenerator
    ): Ret<GraphGenerationResult> {
        val graphs = HashMap<Aircraft, Graph>()
        val diagnostics = HashMap<Aircraft, RouteGraphDiagnostics>()
        for (aircraft in aircrafts) {
            val usability = aircraftUsability[aircraft]
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "路线图初始化失败：缺少飞机可用性 / Route graph initialization failed: aircraft usability is missing"
                )
            when (val ret = graphGenerator(aircraft, usability, flightTaskGroups)) {
                is Ok -> {
                    graphs[aircraft] = ret.value
                    diagnostics[aircraft] = graphGenerator.diagnostics
                }

                is Failed -> {
                    return Failed(ret.error)
                }

                is Fatal -> {
                    return Fatal(ret.errors)
                }
            }
        }
        return Ok(GraphGenerationResult(graphs = graphs, diagnostics = diagnostics))
    }

    /**
     * Generates initial flight task bunches for all aircraft. / 为所有飞机生成初始航班任务束。
     *
     * @param aircrafts 飞机列表 / list of aircraft
     * @param aircraftUsability 飞机可用性映射 / aircraft usability map
     * @param flightTasks 航班任务列表 / list of flight tasks
     * @param originBunches 原始航班任务束 / original flight task bunches
     * @param generator 初始航班任务束生成器 / initial flight task bunch generator
     * @return 生成的初始航班任务束 / the generated initial flight task bunches
    */
    private fun generateInitialFlightTaskBunches(
        aircrafts: List<Aircraft>,
        aircraftUsability: Map<Aircraft, AircraftUsability>,
        flightTasks: List<FlightTask>,
        originBunches: List<FlightTaskBunch>,
        generator: InitialFlightTaskBunchGenerator
    ): Ret<List<FlightTaskBunch>> {
        val generatedAircraft = HashSet<Aircraft>()
        val bunches = ArrayList<FlightTaskBunch>()

        for (bunch in originBunches) {
            if (!aircrafts.contains(bunch.aircraft)) {
                continue
            }

            val aircraft = bunch.aircraft
            val usability = aircraftUsability[aircraft]
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "初始批次初始化失败：缺少飞机可用性 / Initial bunch initialization failed: aircraft usability is missing"
                )
            val lockedFlightTasks = flightTasks.filter { isLocked(it, bunch.aircraft) }.sortedBy { it.time?.start }
            val newBunch = generator(aircraft, usability, lockedFlightTasks, bunch) ?: continue
            generatedAircraft.add(bunch.aircraft)
            bunches.add(newBunch)
        }

        for (aircraft in aircrafts) {
            if (generatedAircraft.contains(aircraft)) {
                continue
            }

            val usability = aircraftUsability[aircraft]
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "初始批次初始化失败：缺少飞机可用性 / Initial bunch initialization failed: aircraft usability is missing"
                )
            val lockedFlightTasks = flightTasks.filter { isLocked(it, aircraft) }.sortedBy { it.time?.start }
            val newBunch = generator.emptyBunch(aircraft, usability, lockedFlightTasks)
            if (newBunch != null) {
                bunches.add(newBunch)
            }
        }

        return Ok(bunches)
    }

    /**
     * Checks if a flight task is locked to a specific aircraft. / 检查航班任务是否锁定到指定飞机。
     *
     * @param flightTask 要检查的航班任务 / the flight task to check
     * @param aircraft 要对照检查的飞机 / the aircraft to check against
     * @return 航班任务是否锁定到该飞机 / true if the flight task is locked to the aircraft
    */
    private fun isLocked(flightTask: FlightTask, aircraft: Aircraft): Boolean {
        return !flightTask.cancelEnabled && !flightTask.aircraftChangeEnabled && flightTask.aircraft == aircraft
    }
}
