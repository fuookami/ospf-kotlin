package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.ordinary.PrimeCache

/** reader 与一条明确 writer 扩容序列共享同一生产缓存。 / Readers sharing one production cache with one explicit writer expansion sequence.
 *
 * 现有 `SharedPrimeReadBenchmark.readyRead` 只覆盖已就绪范围读取，不含并发扩容。每个操作使用全新缓存代并固定完成协议：
 * reader 只查询构造时即覆盖的范围（初始筛上界 1000），调用线程执行固定的显式扩容序列，批次计时到全部参与者完成，
 * 返回 reader 命中数与 writer 进度，便于检查 writer 饥饿或停滞。`readyControl` 不扩容，作为同协议对照。
 * The existing ready-read fixture excludes concurrent expansion. Each operation starts a fresh generation with a fixed
 * completion protocol: readers only query the initially covered range (sieve bound 1000), the calling thread runs a
 * fixed explicit expansion sequence, and the batch is timed until every participant completes, returning reader hits
 * and writer progress so writer starvation is visible. `readyControl` performs no expansion as the control. Internal
 * concurrency is the `readers` parameter, so JMH caller threads stay at 1 (`@Threads(1)`); extra caller threads would
 * oversubscribe the shared pool and block its barrier.
 * @property pattern writer 扩容形态 / Writer expansion pattern
 * @property readers 并发 reader 数 / Concurrent reader count
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
open class PrimeMixedExpansionBenchmark {
    @Param("readyControl", "fewLarge", "consecutive", "smallSteps")
    lateinit var pattern: String

    @Param("1", "2", "4", "8")
    var readers = 0

    private val coveredRange = 1000
    private lateinit var pool: ExecutorService

    /** 线程池在计时外创建；缓存代在每个操作内重建。 / The pool is created outside timing; each operation builds a fresh generation. */
    @Setup
    fun setup() {
        pool = Executors.newFixedThreadPool(readers)
    }

    /** 释放线程池。 / Release the pool. */
    @TearDown
    fun tearDown() {
        pool.shutdownNow()
    }

    /** @return reader 命中数与 writer 最终范围 / Reader hits and the writer final range */
    @Benchmark
    fun mixedBatch(): String {
        val cache = PrimeCache()
        val barrier = CyclicBarrier(readers)
        val futures = (0 until readers).map { slot ->
            pool.submit(Callable {
                barrier.await()
                var hits = 0
                for (step in 0 until 128) {
                    if (cache.isPrime(UInt64((slot * 128 + step) % coveredRange))) hits++
                }
                hits
            })
        }
        val writerProgress = expand(cache)
        val readerHits = futures.sumOf { it.get() }
        return "readerHits=$readerHits;writer=$writerProgress"
    }

    /** 单线程扩容序列，作为没有 reader 并发的 writer 基线。 / The expansion sequence alone, as the writer baseline without concurrent readers.
     * @return writer 进度 / Writer progress
     */
    @Benchmark
    fun writerOnly(): Int {
        val cache = PrimeCache()
        return expand(cache = cache)
    }

    /** 仅 reader 的共享冷批次，用于分离 reader 自身的建代成本。 / Reader-only cold batch separating the generation-build cost of readers.
     * @return reader 命中数 / Reader hits
     */
    @Benchmark
    fun readerOnly(): Int {
        val cache = PrimeCache()
        val barrier = CyclicBarrier(readers)
        val futures = (0 until readers).map { slot ->
            pool.submit(Callable {
                barrier.await()
                var hits = 0
                for (step in 0 until 128) {
                    if (cache.isPrime(UInt64((slot * 128 + step) % coveredRange))) hits++
                }
                hits
            })
        }
        return futures.sumOf { it.get() }
    }

    private fun expand(cache: PrimeCache): Int = when (pattern) {
        "fewLarge" -> listOf(50_000, 200_000, 500_000).sumOf { cache.getPrimes(UInt64(it)).size }
        "consecutive" -> (coveredRange until coveredRange + 256).count { cache.isPrime(UInt64(it)) }
        "smallSteps" -> {
            var limit = coveredRange
            var progress = 0
            repeat(48) {
                limit += 211
                progress += cache.getPrimes(UInt64(limit)).size
            }
            progress
        }
        else -> cache.getPrimes(UInt64(coveredRange)).size
    }
}

/** reader 尾延迟：已就绪读与持续并发扩容下的范围内读。 / Reader tail latency: ready reads and covered reads under sustained concurrent expansion.
 *
 * 每操作一次生产查询，`Mode.SampleTime` 提供 p95/p99 样本。writer 是唯一的扩容者：它在每个测量迭代内循环推进扩容序列，
 * 到达上界后重建一整代并从头继续，因此扩容压力贯穿整个计时窗，不会因为序列跑完而退化为纯就绪读；
 * 重建成本只落在 writer 线程，reader 热路径不加锁、不新增原子写，只按 volatile 引用读取当前已发布代。
 * reader 始终只查询构造时即覆盖的范围（初始筛上界 1000），不会成为第二个 writer。
 * 每轮打印步数、整代重建次数与最终范围，用于检查 writer 饥饿、停滞或退化。`readyOnly` 不启动 writer，作为同夹具的已就绪对照。
 * 注意：代引用由 writer 在线更换，因此 `cache` 必须 volatile 发布；旧代发布后不可变，读到旧代仍是完整合法的快照。
 * Each operation performs one production query and SampleTime yields p95/p99. The writer is the only expander: it loops
 * the expansion sequence inside every measurement iteration and rebuilds a whole generation at the ceiling, so expansion
 * pressure spans the entire window instead of finishing early and degrading into ready reads. Rebuild cost lands on the
 * writer thread only; the reader hot path adds no lock and no atomic write, it only loads the volatile published
 * reference. Readers query the initially covered range (sieve bound 1000) and never become a second writer.
 * Each iteration prints steps, generation rebuilds and the final bound to reveal starvation or degradation.
 * `readyOnly` starts no writer as the ready control inside the same fixture.
 * Because the writer swaps generations mid-iteration, the reference must be published as volatile; an old generation is
 * immutable after publication, so reading it is still a complete, valid snapshot.
 * Unlike the pooled mixed fixture, JMH `-t` here is the reader count (1,2,4,8): this class has no internal pool or
 * barrier, and each caller thread records its own latency samples against the single writer.
 * @property pattern writer 扩容形态 / Writer expansion pattern
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class PrimeReaderLatencyBenchmark {
    @Param("readyOnly", "smallSteps", "consecutive", "fewLarge")
    lateinit var pattern: String

    private val coveredRange = 1000
    private val expansionCeiling = 200_000

    @Volatile
    private var cache = PrimeCache()
    private val running = AtomicBoolean(false)
    private val writerSteps = AtomicLong(0)
    private val writerRebuilds = AtomicLong(0)
    private val writerLimit = AtomicLong(0)
    private var writer: Thread? = null

    /** 每次迭代重置为新的已就绪代，并在需要时启动循环扩容 writer。 / Reset to a fresh ready generation per iteration and start the looping writer when required. */
    @Setup(Level.Iteration)
    fun startWriter() {
        cache = readyCache()
        writerSteps.set(0)
        writerRebuilds.set(0)
        writerLimit.set(coveredRange.toLong())
        if (pattern != "readyOnly") {
            running.set(true)
            writer = Thread({
                while (running.get()) {
                    expandPass()
                    if (!running.get()) break
                    // 固定协议：到界后重建一整代再继续，保证扩容贯穿整个计时窗。
                    // Fixed protocol: rebuild a whole generation at the ceiling so expansion spans the window.
                    cache = readyCache()
                    writerRebuilds.incrementAndGet()
                    writerLimit.set(coveredRange.toLong())
                }
            }, "benchmark-prime-writer").apply {
                isDaemon = true
                start()
            }
        }
    }

    /** 停止 writer 并打印进度，确认扩容在计时窗内持续且未退化。 / Stop the writer and print progress to confirm expansion stayed active and did not degrade. */
    @TearDown(Level.Iteration)
    fun stopWriter() {
        running.set(false)
        writer?.join(TimeUnit.SECONDS.toMillis(120))
        println("PRIME_LATENCY_ITERATION pattern=$pattern steps=${writerSteps.get()} rebuilds=${writerRebuilds.get()} finalLimit=${writerLimit.get()}")
        writer = null
    }

    /** @return 素性结果 / Primality result */
    @Benchmark
    fun coveredRead(): Boolean = cache.isPrime(UInt64(997))

    /** @return 不超过查询上界的素数数量 / Prime count within the query bound */
    @Benchmark
    fun coveredRangeRead(): Int = cache.getPrimes(UInt64(500)).size

    private fun readyCache(): PrimeCache = PrimeCache().apply { isPrime(UInt64(997)) }

    private fun expandPass() {
        when (pattern) {
            "consecutive" -> {
                for (value in coveredRange until expansionCeiling) {
                    if (!running.get()) return
                    cache.isPrime(UInt64(value))
                    writerSteps.incrementAndGet()
                    writerLimit.set(value.toLong())
                }
            }
            "fewLarge" -> {
                var limit = coveredRange
                while (limit < expansionCeiling) {
                    if (!running.get()) return
                    limit += 10_000
                    cache.getPrimes(UInt64(limit))
                    writerSteps.incrementAndGet()
                    writerLimit.set(limit.toLong())
                }
            }
            else -> {
                var limit = coveredRange
                while (limit < expansionCeiling) {
                    if (!running.get()) return
                    limit += 211
                    cache.getPrimes(UInt64(limit))
                    writerSteps.incrementAndGet()
                    writerLimit.set(limit.toLong())
                }
            }
        }
    }
}
