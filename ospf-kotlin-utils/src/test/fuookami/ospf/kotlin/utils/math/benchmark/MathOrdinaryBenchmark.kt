package fuookami.ospf.kotlin.utils.math.benchmark

import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.math.ordinary.factorize
import fuookami.ospf.kotlin.utils.math.ordinary.gcd
import fuookami.ospf.kotlin.utils.math.ordinary.lcm
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

@State(Scope.Thread)
open class MathOrdinaryBenchmark {
    private val sampleA = UInt64(123_456)
    private val sampleB = UInt64(789_012)
    private val sampleC = UInt64(456_789)

    @Benchmark
    fun gcdBaseline(): UInt64 {
        return gcd(sampleA, sampleB, sampleC)
    }

    @Benchmark
    fun lcmBaseline(): UInt64 {
        return lcm(sampleA, sampleB, sampleC)
    }

    @Benchmark
    fun factorizeBaseline(): List<Pair<UInt64, Int>> {
        return factorize(sampleC)
    }
}

