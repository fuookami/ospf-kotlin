package fuookami.ospf.kotlin.utils.math.benchmark

import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import java.util.concurrent.TimeUnit

object BenchmarkLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val includePattern = if (args.isNotEmpty()) {
            args[0]
        } else {
            ".*MathOrdinaryBenchmark.*"
        }
        val options = OptionsBuilder()
            .include(includePattern)
            .warmupIterations(2)
            .measurementIterations(3)
            .forks(1)
            .timeUnit(TimeUnit.MILLISECONDS)
            .build()
        Runner(options).run()
    }
}

