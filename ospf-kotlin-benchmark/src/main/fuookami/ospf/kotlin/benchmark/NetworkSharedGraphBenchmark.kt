package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 共享不可变图上的并发首次消费与稳态读取。 / Concurrent first consumption and steady reads on one shared immutable graph.
 *
 * 与 `NetworkIndexBuildBenchmark`（`Scope.Thread`，每线程独立图）互补：所有 JMH 线程共享同一张图。
 * 冷批次作为一次完整操作计时，包含建图、执行器调度、屏障对齐和全部 reader 完成；`dispatchOnly` 给出同一协议的空调度对照。
 * Complements the per-thread fixture: every JMH thread shares one graph. A cold batch is timed as one operation
 * including construction, executor dispatch, barrier alignment and completion of all readers; `dispatchOnly` is the
 * empty-dispatch control under the same protocol. Internal concurrency is the `readers` parameter, so JMH caller
 * threads stay at 1 (`@Threads(1)`, and keep `-t 1` if overridden on the command line).
 * @property shape 图形态 / Graph shape
 * @property size 节点数 / Node count
 * @property readers 冷批次内同时首次消费的 reader 数 / Readers arriving together for the cold batch
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
open class NetworkSharedGraphBenchmark {
    @Param("ring4", "sparse2", "highOut", "empty")
    lateinit var shape: String

    @Param("256", "1024", "4096")
    var size = 0

    @Param("1", "2", "4", "8")
    var readers = 0

    private lateinit var nodes: List<NetworkNodeId>
    private lateinit var warmGraph: NetworkGraph<NetworkNodeId, Arc>
    private lateinit var warm: NetworkTopology<NetworkNodeId, Arc>
    private var warmArcCount = 0
    private lateinit var pool: ExecutorService
    private val access = NetworkGraphAccess<NetworkNodeId, Arc>(
        nodeId = { it },
        arcId = { it.id },
        from = { it.from },
        to = { it.to }
    )

    /** 建池、共享已消费图和固定输入均在计时外。 / Pool creation, the consumed shared graph and fixed inputs stay outside timing. */
    @Setup
    fun setup() {
        nodes = List(size) { NetworkNodeId("n$it") }
        val arcs = buildArcs()
        pool = Executors.newFixedThreadPool(readers)
        warmGraph = create(nodes = nodes, arcs = arcs)
        warmArcCount = warmGraph.outgoing(nodes.first()).size
        warm = NetworkTopology(warmGraph)
        warm.reachable(setOf(nodes.first()))
    }

    /** 释放共享执行器。 / Release the shared executor. */
    @TearDown
    fun tearDown() {
        pool.shutdownNow()
    }

    /** @return 共享稳态出弧与入弧读取规模 / Steady shared outgoing and incoming read sizes */
    @Benchmark
    fun warmSharedRead(): Int {
        var sum = warmArcCount
        for (slot in 0 until readers) {
            sum += warmGraph.outgoing(nodes[slot % size]).size
            sum += warmGraph.incoming(nodes[slot % size]).size
        }
        return sum
    }

    /** @return 共享图上的多源稳态遍历规模 / Multi-source steady traversal size on the shared graph */
    @Benchmark
    fun warmSharedReachable(): Int {
        val sources = (0 until readers).map { nodes[it % size] }.toSet()
        return warm.reachable(sources).value!!.size
    }

    /** @return 一次完整冷并发首次消费批次的可达规模之和 / Reachability totals of one complete cold concurrent batch */
    @Benchmark
    fun coldFirstReadBatch(): Int {
        val topology = NetworkTopology(create(nodes = nodes, arcs = buildArcs()))
        return runBatch { slot -> topology.reachable(setOf(nodes[slot % size])).value!!.size }
    }

    /** @return 一次完整冷并发首次反向视图消费批次 / Reverse-view totals of one complete cold concurrent batch */
    @Benchmark
    fun coldReverseBatch(): Int {
        val reverse = create(nodes = nodes, arcs = buildArcs()).reverse()
        return runBatch { slot -> reverse.outgoing(nodes[slot % size]).size + reverse.incoming(nodes[slot % size]).size }
    }

    /** @return 相同调度协议下的空调度对照 / Empty-dispatch control under the same scheduling protocol */
    @Benchmark
    fun dispatchOnly(): Int = runBatch { slot -> slot }

    private fun runBatch(body: (Int) -> Int): Int {
        val barrier = CyclicBarrier(readers)
        val arrivals = AtomicInteger()
        val futures = (0 until readers).map {
            pool.submit(Callable {
                barrier.await()
                body(arrivals.getAndIncrement())
            })
        }
        var sum = 0
        for (future in futures) {
            sum += future.get()
        }
        return sum
    }

    private fun buildArcs(): List<Arc> {
        val degree = when (shape) {
            "empty" -> 0
            "sparse2" -> 2
            "highOut" -> 8
            else -> 4
        }
        return nodes.indices.flatMap { from ->
            (1..degree).map { step ->
                Arc(
                    id = NetworkArcId("a${from}_$step"),
                    from = nodes[from],
                    to = nodes[(from + step) % size]
                )
            }
        }
    }

    private fun create(nodes: List<NetworkNodeId>, arcs: List<Arc>) =
        NetworkGraph(nodes = nodes, arcs = arcs, access = access).value!!

    /** @property id 弧标识 / Arc identity
     * @property from 起点 / Tail
     * @property to 终点 / Head
     */
    private data class Arc(val id: NetworkArcId, val from: NetworkNodeId, val to: NetworkNodeId)
}
