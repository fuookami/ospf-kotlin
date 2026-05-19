package fuookami.ospf.kotlin.core.intermediate_symbol.function

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

class FunctionSymbolNullStubGuardTest {
    private val root = File("src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function")
    private val forbiddenPatterns = listOf(
        Regex("""override\s+fun\s+prepare\([^)]*\)\s*:\s*V\?\s*=\s*null"""),
        Regex("""override\s+fun\s+evaluate\([^)]*\)\s*:\s*V\?\s*=\s*null"""),
        Regex("""internal\s+fun\s+prepareSolver\([^)]*\)\s*:\s*V\?\s*=\s*null"""),
        Regex("""internal\s+fun\s+evaluateSolver\([^)]*\)\s*:\s*V\?\s*=\s*null""")
    )

    @Test
    fun shouldNotIntroduceNullStubInFunctionEvaluationPaths() {
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
                    if (forbiddenPatterns.any { it.containsMatchIn(trimmed) }) {
                        violations += "$relativePath:${index + 1}: $trimmed"
                    }
                }
            }

        if (violations.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("Detected null-stub evaluation/prepare methods in function symbols:")
                    violations.forEach { appendLine(it) }
                }
            )
        }
    }
}