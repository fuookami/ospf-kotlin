package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*

/** 索引失效后的下一代首次读（单线程，每周期净结构变化为零）。 / First read of the next generation after index invalidation (single-threaded, zero net structural change per cycle).
 *
 * 现有 `TokenSharedReadBenchmark` 覆盖并发首次读与稳态读，但不覆盖“索引被结构变更失效后再次首读”的成本。
 * 这里每个周期先增删同一个 token 以触发缓存失效，然后对一个稳定 token 做首读迫使重建；
 * 修改只发生在调用线程，符合列表自身的单写锁契约，不涉及未支持的并发写。
 * 每操作 `cycles` 次该循环，因此每次操作的建表次数固定，便于把时间差异归到建表次数上而不是负载漂移。
 * The shared-read fixture covers concurrent first reads and steady reads but not the cost of the first read after the
 * index has been invalidated by a structural change. Each cycle adds then removes one token to invalidate the cache,
 * then performs a first read of a stable token to force a rebuild; mutation stays on the calling thread, matching the
 * list's own single-writer lock contract, with no unsupported concurrent writes. One operation runs `cycles` such
 * loops, so the number of index builds per operation is fixed and any time difference maps to build cost rather than
 * workload drift.
 * @property dataset 规模 small/medium/large = 1024/2048/4096 token / Scale
 * @property cycles 每操作的失效重建周期数 / Invalidation-rebuild cycles per operation
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class TokenInvalidateRebuildBenchmark {
    @Param("small", "medium", "large")
    lateinit var dataset: String

    @Param("16")
    var cycles = 0

    private var size = 0
    private lateinit var writer: ManualTokenList<Flt64>
    private lateinit var stable: List<Token<Flt64>>
    private lateinit var probes: List<RealVar>

    /** 固定结构、已就绪索引与探针变量均在计时外建立。 / Fixed structure, the ready index and probe variables are prepared outside timing. */
    @Setup
    fun setup() {
        size = when (dataset) {
            "medium" -> 2048
            "large" -> 4096
            else -> 1024
        }
        writer = ManualTokenList<Flt64>(checkTokenExisted = true)
        writer.add(List(size) { RealVar("ir_$it") })
        writer.indexOf(writer.tokensInSolver.last())
        stable = writer.tokensInSolver
        probes = List(64) { RealVar("probe_$it") }
    }

    /** 解除列表回调。 / Detach list callbacks. */
    @TearDown
    fun tearDown() {
        writer.close()
    }

    /**
     * 每周期：加入探针 token（使索引失效）→ 读取稳定 token（迫使重建）→ 移除探针 token。
     * Each cycle: add a probe token (invalidates the index), read a stable token (forces the rebuild), remove the probe.
     * @return 索引和 / Index sum
     */
    @Benchmark
    fun invalidateThenRebuild(): Int {
        var sum = 0
        for (cycle in 0 until cycles) {
            val probe = probes[cycle % probes.size]
            writer.add(probe)
            try {
                sum += writer.indexOf(stable[cycle % stable.size]) ?: -1
            } finally {
                writer.remove(probe)
            }
        }
        return sum
    }

    /** 对照：不做失效与重建，只做同量读取，用于分离读取成本与建表成本。 / Control: the same reads without invalidation, separating read cost from rebuild cost.
     * @return 索引和 / Index sum
     */
    @Benchmark
    fun warmReadsOnly(): Int {
        var sum = 0
        for (cycle in 0 until cycles) {
            sum += writer.indexOf(stable[cycle % stable.size]) ?: -1
        }
        return sum
    }

    /** 对照：只做增删而不读取，用于分离结构修改成本。 / Control: add/remove only, separating structural mutation cost.
     * @return 周期数 / Cycle count
     */
    @Benchmark
    fun mutationOnly(): Int {
        for (cycle in 0 until cycles) {
            val probe = probes[cycle % probes.size]
            writer.add(probe)
            writer.remove(probe)
        }
        return cycles
    }
}
