package fuookami.ospf.kotlin.core.symbol.function

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

class FunctionSymbolFlt64LeakageGuardTest {
    private val root = File("src/main/fuookami/ospf/kotlin/core/symbol/function")

    // 允许保留在兼容/边界层的 Flt64 约束构造点。
    // Allowed solver-boundary Flt64 constraint-construction points.
    private val allowlist = setOf(
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:195",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:207",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:211",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:217",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:221",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:319",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:325",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:332",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:339",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:347",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:368",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:375",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:378",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:383",
        "fuookami/ospf/kotlin/core/symbol/function/BigM.kt:386"
    )

    @Test
    fun shouldNotIntroduceNewFlt64ConstraintConstructionOutsideAllowlist() {
        if (!root.exists()) {
            fail("Scan root not found: ${root.path}")
        }

        val violations = mutableListOf<String>()
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relativePath = file.toRelativeString(File("src/main")).replace('\\', '/')
                file.readLines().forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/**")) {
                        return@forEachIndexed
                    }
                    val suspicious =
                        Regex("""LinearInequality<\s*(fuookami\.ospf\.kotlin\.math\.algebra\.number\.)?Flt64\s*>\(""").containsMatchIn(trimmed) ||
                            Regex("""mutableListOf<\s*LinearInequality<\s*(fuookami\.ospf\.kotlin\.math\.algebra\.number\.)?Flt64\s*>\s*>\(""").containsMatchIn(trimmed)
                    if (suspicious) {
                        val location = "$relativePath:${index + 1}"
                        if (location !in allowlist) {
                            violations += "$location: $trimmed"
                        }
                    }
                }
            }

        if (violations.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("Detected new Flt64 constraint-construction points outside allowlist:")
                    violations.forEach { appendLine(it) }
                }
            )
        }
    }
}
