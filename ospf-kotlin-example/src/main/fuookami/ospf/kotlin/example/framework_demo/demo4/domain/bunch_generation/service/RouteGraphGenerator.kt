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
 * @property withOrderChange Whether order change is enabled in route generation / 路线生成中是否启用顺序变更
*/
data class Configuration(
    val withOrderChange: Boolean = false
)

/**
 * Generates route graphs for bunch generation.
 * 为批次生成生成路线图。
 *
 * @property reverse The flight task reverse manager for order change operations / 用于顺序变更操作的航班任务反转管理器
 * @property configuration The route graph generation configuration / 路线图生成配置
 * @property feasibilityJudger Function to check if a flight task is feasible for an aircraft / 检查航班任务对飞机是否可行的函数
*/
class RouteGraphGenerator(
    private val reverse: FlightTaskReverse,
    private val configuration: Configuration,
    private val feasibilityJudger: (Aircraft, FlightTask?, FlightTask) -> Boolean,
) {

    /**
     * Generates a route graph for the given aircraft.
     * 为给定飞机生成路线图。
     *
     * @param aircraft The aircraft for which to generate the route graph / 要生成路线图的飞机
     * @param aircraftUsability The usability constraints of the aircraft / 飞机的可用性约束
     * @param flightTasks Map of airports to available flight tasks / 机场到可用航班任务的映射
     * @return The generated route graph, or an error / 生成的路线图，或错误
    */
    operator fun invoke(
        aircraft: Aircraft,
        aircraftUsability: AircraftUsability,
        flightTasks: Map<Airport, List<FlightTask>>
    ): Ret<Graph> {
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
            searchAndInsertFlightTasks(graph, nodes, nodeMap, node, aircraft, flightTasks[airport]!!)
        }
        return Ok(graph)
    }

/**
 * Searches for feasible flight tasks at the current airport and inserts them into the route graph.
 * 在当前机场搜索可行的航班任务并将其插入路线图。
 * @param graph The route graph being constructed / 正在构建的路线图
 * @param nodes The BFS queue of airport-node pairs to process / 待处理的机场-节点对BFS队列
 * @param nodeMap The mapping from flight tasks to their graph nodes / 航班任务到其图节点的映射
 * @param node The current node in the graph / 图中的当前节点
 * @param aircraft The aircraft for which to check feasibility / 要检查可行性的飞机
 * @param flightTasks The available flight tasks at the current airport / 当前机场可用的航班任务
*/
    private fun searchAndInsertFlightTasks(
        graph: Graph,
        nodes: MutableList<Pair<Airport, Node>>,
        nodeMap: MutableMap<FlightTask, Node>,
        node: Node,
        aircraft: Aircraft,
        flightTasks: List<FlightTask>
    ) {
        if (node is RootNode) {
            var flag = false
            for (flightTask in flightTasks) {
                if (feasibilityJudger(aircraft, null, flightTask)) {
                    flag = true
                    insertFlightTask(graph, nodes, nodeMap, node, flightTask)
                }
            }
            if (!flag) {
                graph.put(node, EndNode)
            }
        } else if (configuration.withOrderChange) {
            node as TaskNode

            val prevFlightTask = node.task
            for (flightTask in flightTasks) {
                if (feasibilityJudger(aircraft, prevFlightTask, flightTask)) {
                    insertFlightTask(graph, nodes, nodeMap, node, flightTask)
                }

                if (reverse.contains(flightTask, prevFlightTask)
                    && feasibilityJudger(aircraft, flightTask, prevFlightTask)
                ) {
                    insertFlightTask(graph, nodes, nodeMap, node, flightTask)
                }
            }
            graph.put(node, EndNode)
        } else {
            node as TaskNode

            val prevFlightTask = node.task
            for (flightTask in flightTasks) {
                if (feasibilityJudger(aircraft, prevFlightTask, flightTask)) {
                    insertFlightTask(graph, nodes, nodeMap, node, flightTask)
                }
            }
            graph.put(node, EndNode)
        }
    }

/**
 * Inserts a flight task into the route graph, creating a new node or connecting to an existing one.
 * 将航班任务插入路线图，创建新节点或连接到已有节点。
 * @param graph The route graph being constructed / 正在构建的路线图
 * @param nodes The BFS queue of airport-node pairs to process / 待处理的机场-节点对BFS队列
 * @param nodeMap The mapping from flight tasks to their graph nodes / 航班任务到其图节点的映射
 * @param prevNode The predecessor node to connect from / 要连接的前序节点
 * @param flightTask The flight task to insert / 要插入的航班任务
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
