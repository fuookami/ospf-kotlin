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
            ".*Math.*Benchmark.*"
        }
        val forks = if (args.size >= 2) {
            args[1].toIntOrNull() ?: 0
        } else {
            0
        }
        val options = OptionsBuilder()
            .include(includePattern)
            .warmupIterations(2)
            .measurementIterations(3)
            .forks(forks)
            .timeUnit(TimeUnit.MILLISECONDS)
            .build()
        Runner(options).run()
    }
}