package fuookami.ospf.kotlin.benchmark

import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import java.io.File
import java.net.URLClassLoader

/**
 * Benchmark 运行入口（small 默认配置）
 * Benchmark runner entry (small default configuration)
 *
 * 用法：
 * Usage:
 * `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".* small 1 1"`
 */
fun main(args: Array<String>) {
    patchJavaClassPathForForkedJmh()

    val includeRegex = args.getOrNull(0) ?: ".*"
    val mode = args.getOrNull(1) ?: "small"
    val forks = args.getOrNull(2)?.toIntOrNull() ?: 1
    val warmupIterations = args.getOrNull(3)?.toIntOrNull() ?: 1
    val measurementIterations = args.getOrNull(4)?.toIntOrNull() ?: 1

    val options = OptionsBuilder()
        .include(includeRegex)
        .param("dataset", mode)
        .forks(forks)
        .warmupIterations(warmupIterations)
        .measurementIterations(measurementIterations)
        .build()

    Runner(options).run()
}

/**
 * 在 `exec:java` 场景下，JMH fork 进程读取的 `java.class.path` 可能不包含项目依赖。
 * Under `exec:java`, JMH forked VMs may see an incomplete `java.class.path`.
 */
private fun patchJavaClassPathForForkedJmh() {
    val classLoader = Thread.currentThread().contextClassLoader as? URLClassLoader ?: return
    val classpath = classLoader.urLs
        .mapNotNull { url ->
            runCatching { File(url.toURI()).absolutePath }.getOrNull()
        }
        .distinct()
        .joinToString(separator = File.pathSeparator)
    if (classpath.isNotBlank()) {
        System.setProperty("java.class.path", classpath)
    }
}
