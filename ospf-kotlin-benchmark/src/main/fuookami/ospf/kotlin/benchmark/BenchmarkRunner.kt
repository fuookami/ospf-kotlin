package fuookami.ospf.kotlin.benchmark

import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.openjdk.jmh.results.format.ResultFormatType
import java.io.File
import java.net.URLClassLoader

/**
 * Benchmark 运行入口（small 默认配置）
 * Benchmark runner entry (small default configuration)
 *
 * 用法：
 * Usage:
 * `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".* small 1 1"`
 *
 * 可选参数：
 * Optional arguments:
 * 1) include regex
 * 2) dataset (`small` / `medium` / `large`)
 * 3) forks
 * 4) warmup iterations
 * 5) measurement iterations
 * 6) result format (`json` / `csv` / ...)
 * 7) result file path
*/
fun main(args: Array<String>) {
    patchJavaClassPathForForkedJmh()

    val includeRegex = args.getOrNull(0) ?: ".*"
    val mode = args.getOrNull(1) ?: "small"
    val forks = args.getOrNull(2)?.toIntOrNull() ?: 1
    val warmupIterations = args.getOrNull(3)?.toIntOrNull() ?: 1
    val measurementIterations = args.getOrNull(4)?.toIntOrNull() ?: 1
    val resultFormat = args.getOrNull(5)?.uppercase() ?: "JSON"
    val resultFile = args.getOrNull(6) ?: "target/benchmark-results/${sanitizeForFileName(includeRegex)}-$mode.${
        resultFormat.lowercase()
    }"
    File(resultFile).parentFile?.mkdirs()

    val options = OptionsBuilder()
        .include(includeRegex)
        .param("dataset", mode)
        .forks(forks)
        .warmupIterations(warmupIterations)
        .measurementIterations(measurementIterations)
        .resultFormat(parseResultFormat(resultFormat))
        .result(resultFile)
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

/**
 * 解析结果格式，异常值回退到 JSON。
 * Parse result format and fallback to JSON on invalid input.
 *
 * @param format 结果格式的字符串表示 / string representation of the result format
 * @return 对应的 [ResultFormatType] 枚举值 / the corresponding [ResultFormatType] enum value
*/
private fun parseResultFormat(format: String): ResultFormatType {
    return runCatching {
        ResultFormatType.valueOf(format.uppercase())
    }.getOrElse {
        ResultFormatType.JSON
    }
}

/**
 * 过滤 regex 以生成稳定可读的文件名。
 * Sanitize regex text for stable and readable file names.
 *
 * @param raw 原始正则表达式字符串 / raw regex string
 * @return 经过清理的文件名安全字符串 / sanitized file-name-safe string
*/
private fun sanitizeForFileName(raw: String): String {
    val sanitized = raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return sanitized.trim('_').ifBlank { "all" }
}
