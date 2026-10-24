package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 真实图入口的冷构建、反向复用及稳态读取。 / Production cold graph construction, reverse reuse and warm reads.
 * @property size 节点数，弧数为其四倍 / Node count, with four times as many arcs
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class NetworkIndexBuildBenchmark {
    @Param("256", "1024", "4096")
    var size = 0

    private lateinit var nodes: List<NetworkNodeId>
    private lateinit var arcs: List<Arc>
    private lateinit var graph: NetworkGraph<NetworkNodeId, Arc>
    private val access = NetworkGraphAccess<NetworkNodeId, Arc>(
        nodeId = { it },
        arcId = { it.id },
        from = { it.from },
        to = { it.to }
    )

    /** 固定输入和已使用的图在计时外创建。 / Prepare fixed inputs and an already-used graph outside timing. */
    @Setup
    fun setup() {
        nodes = List(size) { NetworkNodeId("n$it") }
        arcs = nodes.indices.flatMap { from ->
            (1..4).map { step ->
                Arc(
                    id = NetworkArcId("a${from}_$step"),
                    from = nodes[from],
                    to = nodes[(from + step) % size]
                )
            }
        }
        graph = create()
        NetworkTopology(graph).stronglyConnectedComponents()
    }

    /** @return 未消费索引的新图 / New graph without index consumption */
    @Benchmark
    fun constructOnly(): Any = create()

    /** @return 首次完整出向遍历结果 / First full outgoing traversal result */
    @Benchmark
    fun coldReachable(): Any = NetworkTopology(create()).reachable(setOf(nodes.first()))

    /** @return 首次双向遍历结果 / First bidirectional traversal result */
    @Benchmark
    fun coldComponents(): Any = NetworkTopology(create()).stronglyConnectedComponents()

    /** @return 反向图的出弧，与原图入弧共享 / Reverse outgoing arcs, shared with original incoming arcs */
    @Benchmark
    fun reverseAndRead(): Any = graph.reverse().outgoing(nodes.first())

    /** @return 不读取索引的反向视图 / Reverse view without index reads */
    @Benchmark
    fun reverseOnly(): Any = graph.reverse()

    /** @return 稳态真实遍历结果 / Warm production traversal result */
    @Benchmark
    fun warmReachable(): Any = NetworkTopology(graph).reachable(setOf(nodes.first()))

    /** @return 过滤视图上的真实遍历结果 / Production traversal on a filtered view */
    @Benchmark
    fun filteredReachable(): Any = NetworkTopology(graph).filtered(
        allowedNode = { it != nodes.last() },
        allowedArc = { it.to != nodes.last() }
    ).reachable(setOf(nodes.first()))

    private fun create() = NetworkGraph(nodes = nodes, arcs = arcs, access = access).value!!

    private data class Arc(val id: NetworkArcId, val from: NetworkNodeId, val to: NetworkNodeId)
}
