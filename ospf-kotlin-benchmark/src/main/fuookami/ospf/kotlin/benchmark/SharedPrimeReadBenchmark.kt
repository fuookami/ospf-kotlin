package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.ordinary.PrimeCache

/** 多reader共享已建表的生产素性查询，不测后台预热。 / Production primality reads sharing a ready table, not background warmup. */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class SharedPrimeReadBenchmark {
    private lateinit var cache: PrimeCache
    private val query = UInt64(997)

    /** 建表在计时外，读线程只查询已覆盖范围。 / Build outside timing; readers only query the covered range. */
    @Setup
    fun setup() {
        cache = PrimeCache()
    }

    /** @return 已缓存的素性结果 / Cached primality result */
    @Benchmark
    fun readyRead(): Boolean = cache.isPrime(query)
}
