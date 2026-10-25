package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*

/** 共享固定 token 列表的并发首次读、稳态读与真实建表工作量。 / Concurrent first reads, steady reads and actual index-build work on one shared fixed token list.
 *
 * 现有 `PreparationHotPathBenchmark.coldTokenIndices` 是单线程整体成本哨兵；这里所有 reader 共享同一列表与同一份 token。
 * 冷批次作为一次完整操作计时，包含执行器调度、屏障对齐和全部 reader 完成，`dispatchOnly` 是同协议的空调度对照。
 * 建表工作量由变量标识符读取次数给出：单次完整建表约等于 token 数，查询本身另计一次读取，因此超出 token 数 + 查询数的部分即重复建表。
 * The existing single-threaded sentinel measures whole cold cost; here every reader shares one list and one token set.
 * A cold batch is timed as one operation including dispatch, barrier alignment and completion of all readers, with
 * `dispatchOnly` as the empty-dispatch control. Actual build work is counted variable-identifier reads: one full build
 * costs about one read per token plus one per query, so reads above that are repeated builds. Internal concurrency is
 * the `readers` parameter, so JMH caller threads stay at 1 (`@Threads(1)`, and keep `-t 1` if overridden).
 * @property dataset 规模 small/medium/large = 1024/2048/4096 token / Scale
 * @property readers 同时首次访问的 reader 数 / Readers performing the first accesses together
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
open class TokenSharedReadBenchmark {
    @Param("small", "medium", "large")
    lateinit var dataset: String

    @Param("1", "2", "4", "8")
    var readers = 0

    private var size = 0
    private lateinit var tokenMap: Map<VariableItemKey, Token<Flt64>>
    private lateinit var ordered: List<Token<Flt64>>
    private lateinit var warmList: TokenList<Flt64>
    private lateinit var countingList: TokenList<Flt64>
    private lateinit var countingOrdered: List<Token<Flt64>>
    private lateinit var writer: ManualTokenList<Flt64>
    private lateinit var countingWriter: ManualTokenList<Flt64>
    private lateinit var pool: ExecutorService
    private val identifierReads = AtomicInteger()
    private var lastBuildReads = 0
    private var lastIndexSum = 0

    /** 共享列表、已就绪索引和线程池均在计时外建立。 / Shared lists, the ready index and the pool are prepared outside timing. */
    @Setup
    fun setup() {
        size = when (dataset) {
            "medium" -> 2048
            "large" -> 4096
            else -> 1024
        }
        writer = ManualTokenList<Flt64>(checkTokenExisted = true)
        writer.add(List(size) { RealVar("shared_$it") })
        tokenMap = writer.tokens.associateBy { it.key }
        warmList = TokenList(tokenMap)
        ordered = warmList.tokensInSolver
        warmList.indexOf(ordered.last())
        countingWriter = ManualTokenList<Flt64>(checkTokenExisted = true)
        countingWriter.add(List(size) { index ->
            CountingVariable(fixed = 1_000_000 + index, name = "counted_$index", reads = identifierReads)
        })
        countingList = TokenList(countingWriter.tokens.associateBy { it.key })
        countingOrdered = countingList.tokensInSolver
        pool = Executors.newFixedThreadPool(readers)
    }

    /** 释放共享执行器并解除列表回调；打印最后一次冷建表诊断值（trial 级，不进入计时路径）。 / Release the pool, detach callbacks and print the last cold build diagnostic (trial level, outside timing). */
    @TearDown
    fun tearDown() {
        pool.shutdownNow()
        warmList.close()
        countingList.close()
        countingWriter.close()
        writer.close()
        println("TOKEN_SHARED_DIAG readers=$readers dataset=$dataset size=$size lastReads=$lastBuildReads indexSum=$lastIndexSum")
    }

    /** @return 已就绪共享列表上的稳态读索引和 / Index sum of steady reads on the ready shared list */
    @Benchmark
    fun warmSharedRead(): Int {
        return runBatch { slot ->
            var sum = 0
            for (step in 0 until 64) {
                sum += warmList.indexOf(ordered[(slot * 64 + step) % size]) ?: -1
            }
            sum
        }
    }

    /** @return 一次完整冷并发首次读批次的索引和 / Index sum of one complete cold concurrent first-read batch */
    @Benchmark
    fun coldFirstReadBatch(): Int {
        val list = TokenList(tokenMap)
        try {
            return runBatch { slot -> list.indexOf(ordered[(slot * 131 + 7) % size]) ?: -1 }
        } finally {
            list.close()
        }
    }

    /** @return 全部 reader 查询不同 key 的冷批次索引和 / Cold batch where every reader queries distinct keys */
    @Benchmark
    fun coldDistinctKeyBatch(): Int {
        val list = TokenList(tokenMap)
        try {
            return runBatch { slot ->
                var sum = 0
                for (step in 0 until 16) {
                    sum += list.indexOf(ordered[(slot * 16 + step) % size]) ?: -1
                }
                sum
            }
        } finally {
            list.close()
        }
    }

    /** @return 相同调度协议下的空调度对照 / Empty-dispatch control under the same scheduling protocol */
    @Benchmark
    fun dispatchOnly(): Int = runBatch { slot -> slot }

    /** 诊断入口：一次冷并发批次的标识符读取次数与索引和，用于统计实际建表次数，不参与加速结论。 / Diagnostic entry: identifier reads and index sum of one cold batch for actual build counts; excluded from speedup conclusions.
     * @return 读取次数与索引和 / Read count and index sum
     */
    @Benchmark
    fun coldBuildWork(): String {
        identifierReads.set(0)
        val list = TokenList(countingList.list)
        try {
            val sum = runBatch { slot -> list.indexOf(countingOrdered[(slot * 131 + 7) % size]) ?: -1 }
            lastBuildReads = identifierReads.get()
            lastIndexSum = sum
            return "reads=$lastBuildReads;indexSum=$sum"
        } finally {
            list.close()
        }
    }

    private fun runBatch(body: (Int) -> Int): Int {
        val barrier = CyclicBarrier(readers)
        val futures = (0 until readers).map { slot ->
            pool.submit(Callable {
                barrier.await()
                body(slot)
            })
        }
        return futures.sumOf { it.get() }
    }

    /** 标识符读取可计数的变量项，标识符由夹具自身固定分配，不依赖内部生成器。 / Variable item with counted identifier reads, using fixture-assigned identifiers.
     * @property fixed 固定标识符 / Fixed identifier
     * @property name 变量名称 / Variable name
     * @property reads 读取计数 / Read counter
     */
    private class CountingVariable(
        fixed: Int,
        name: String,
        private val reads: AtomicInteger
    ) : AbstractVariableItem<Flt64, Continuous>(
        type = Continuous,
        name = name,
        constants = Flt64
    ) {
        private val fixedIdentifier = UInt64(fixed)

        override val identifier: UInt64 get() {
            reads.incrementAndGet()
            return fixedIdentifier
        }

        override val dimension = 0

        override val index = 0

        override val vectorView = intArrayOf(0)
    }
}
