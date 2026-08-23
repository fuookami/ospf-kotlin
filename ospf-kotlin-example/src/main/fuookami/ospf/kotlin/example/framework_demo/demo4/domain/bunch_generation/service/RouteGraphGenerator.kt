@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service

import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * Configuration for route graph generation.
 * 路线图生成配置。
 *
 * @property withOrderChange 路线生成中是否启用顺序变更 / Whether order change is enabled in route generation
*/
data class Configuration(
    val withOrderChange: Boolean = false
)

/**
 * Generates route graphs for bunch generation.
 * 为批次生成生成路线图。
 *
 * @property reverse 用于顺序变更操作的航班任务反转管理器 / The flight task reverse manager for order change operations
 * @property configuration 路线图生成配置 / The route graph generation configuration
 * @property feasibilityJudger 检查航班任务对飞机是否可行的函数 / Function to check if a flight task is feasible for an aircraft
*/
class RouteGraphGenerator(
    private val reverse: FlightTaskReverse,
    private val configuration: Configuration,
    private val feasibilityJudger: (Aircraft, FlightTask?, FlightTask) -> Boolean,
) {

    /** 最近一次路线图构建统计 / Diagnostics from the most recent graph construction. */
    var diagnostics: RouteGraphDiagnostics = RouteGraphDiagnostics()
        private set

    /**
     * Generates a route graph for the given aircraft.
     * 为给定飞机生成路线图。
     *
     * @param aircraft 要生成路线图的飞机 / The aircraft for which to generate the route graph
     * @param aircraftUsability 飞机的可用性约束 / The usability constraints of the aircraft
     * @param flightTasks 机场到可用航班任务的映射 / Map of airports to available flight tasks
     * @return 生成的路线图，或错误 / The generated route graph, or an error
    */
    operator fun invoke(
        aircraft: Aircraft,
        aircraftUsability: AircraftUsability,
        flightTasks: Map<Airport, List<FlightTask>>
    ): Ret<Graph> {
        val currentDiagnostics = RouteGraphDiagnostics()
        diagnostics = currentDiagnostics
        val graph = Graph()
        graph.put(RootNode)
        graph.put(EndNode)
        val location = aircraftUsability.location
        val nodes = arrayListOf<Pair<Airport, Node>>(Pair(location, RootNode))
        val nodeMap = HashMap<FlightTask, Node>()
        // BFS
        while (nodes.isNotEmpty()) {
            val (airport, node) = nodes.first()
            nodes.removeFirst()

            if (!flightTasks.containsKey(airport)) {
                graph.put(node, EndNode)
                continue
            }
            searchAndInsertFlightTasks(
                graph,
                nodes,
                nodeMap,
                node,
                aircraft,
                flightTasks[airport]!!,
                currentDiagnostics
            )
        }
        return Ok(graph)
    }

/**
 * Searches for feasible flight tasks at the current airport and inserts them into the route graph.
 * 在当前机场搜索可行的航班任务并将其插入路线图。
 * @param graph 正在构建的路线图 / The route graph being constructed
 * @param nodes 待处理的机场-节点对BFS队列 / The BFS queue of airport-node pairs to process
 * @param nodeMap 航班任务到其图节点的映射 / The mapping from flight tasks to their graph nodes
 * @param node 图中的当前节点 / The current node in the graph
 * @param aircraft 要检查可行性的飞机 / The aircraft for which to check feasibility
 * @param flightTasks 当前机场可用的航班任务 / The available flight tasks at the current airport
*/
    private fun searchAndInsertFlightTasks(
        graph: Graph,
        nodes: MutableList<Pair<Airport, Node>>,
        nodeMap: MutableMap<FlightTask, Node>,
        node: Node,
        aircraft: Aircraft,
        flightTasks: List<FlightTask>,
        diagnostics: RouteGraphDiagnostics
    ) {
        if (node is RootNode) {
            var flag = false
            for (flightTask in flightTasks) {
                diagnostics.candidateTaskCount += 1
                if (feasibilityJudger(aircraft, null, flightTask)) {
                    diagnostics.feasibleExpansionCount += 1
                    flag = true
                    insertFlightTask(graph, nodes, nodeMap, node, flightTask)
                } else {
                    diagnostics.infeasibleExpansionCount += 1
                }
            }
            if (!flag) {
                graph.put(node, EndNode)
            }
        } else if (configuration.withOrderChange) {
            node as TaskNode

            val prevFlightTask = node.task
            for (flightTask in flightTasks) {
                diagnostics.candidateTaskCount += 1
                if (feasibilityJudger(aircraft, prevFlightTask, flightTask)) {
                    diagnostics.feasibleExpansionCount += 1
                    insertFlightTask(graph, nodes, nodeMap, node, flightTask)
                } else {
                    diagnostics.infeasibleExpansionCount += 1
                }

                if (reverse.contains(flightTask, prevFlightTask)) {
                    diagnostics.candidateTaskCount += 1
                    if (feasibilityJudger(aircraft, flightTask, prevFlightTask)) {
                        diagnostics.feasibleExpansionCount += 1
                        insertFlightTask(graph, nodes, nodeMap, node, flightTask)
                    } else {
                        diagnostics.infeasibleExpansionCount += 1
                    }
                }
            }
            graph.put(node, EndNode)
        } else {
            node as TaskNode

            val prevFlightTask = node.task
            for (flightTask in flightTasks) {
                diagnostics.candidateTaskCount += 1
                if (feasibilityJudger(aircraft, prevFlightTask, flightTask)) {
                    diagnostics.feasibleExpansionCount += 1
                    insertFlightTask(graph, nodes, nodeMap, node, flightTask)
                } else {
                    diagnostics.infeasibleExpansionCount += 1
                }
            }
            graph.put(node, EndNode)
        }
    }

/**
 * Inserts a flight task into the route graph, creating a new node or connecting to an existing one.
 * 将航班任务插入路线图，创建新节点或连接到已有节点。
 * @param graph 正在构建的路线图 / The route graph being constructed
 * @param nodes 待处理的机场-节点对BFS队列 / The BFS queue of airport-node pairs to process
 * @param nodeMap 航班任务到其图节点的映射 / The mapping from flight tasks to their graph nodes
 * @param prevNode 要连接的前序节点 / The predecessor node to connect from
 * @param flightTask 要插入的航班任务 / The flight task to insert
*/
    private fun insertFlightTask(
        graph: Graph,
        nodes: MutableList<Pair<Airport, Node>>,
        nodeMap: MutableMap<FlightTask, Node>,
        prevNode: Node,
        flightTask: FlightTask
    ) {
        if (!nodeMap.containsKey(flightTask)) {
            val index = UInt64(graph.nodes.size.toULong())
            val depTime = flightTask.time?.start ?: flightTask.timeWindow!!.start
            val node = TaskNode(flightTask, depTime, index)
            graph.put(node)
            graph.put(prevNode, node)
            nodeMap[flightTask] = node
            nodes.add(Pair(flightTask.arr, node))
            for (dep in flightTask.depBackup) {
                flightTask.actualArr(dep)?.let { nodes.add(Pair(it, node)) }
            }
        } else {
            graph.put(prevNode, nodeMap[flightTask]!!)
        }
    }
}
