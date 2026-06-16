@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service

import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*

/** Configuration for route graph generation. */
data class Configuration(
    val withOrderChange: Boolean = false
)

/** Generates route graphs for bunch generation. */
class RouteGraphGenerator(
    private val reverse: FlightTaskReverse,
    private val configuration: Configuration,
    private val feasibilityJudger: (Aircraft, FlightTask?, FlightTask) -> Boolean,
) {
    /** Generates a route graph for the given aircraft. */
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
