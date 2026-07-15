package fuookami.ospf.kotlin.core.symbol.function

import java.io.File
import kotlin.test.*

class FunctionSymbolToDoubleConversionGuardTest {
    private val root = File("src/main/fuookami/ospf/kotlin/core/symbol/function")

    @Test
    fun shouldNotIntroduceToDoubleInFunctionSymbols() {
        if (!root.exists()) {
            fail("Scan root not found: ${root.path}")
        }

        val pattern = Regex("""\.toDouble\(\)""")
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
                    if (pattern.containsMatchIn(trimmed)) {
                        violations += "$relativePath:${index + 1}: $trimmed"
                    }
                }
            }

        if (violations.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("Detected .toDouble() usage in function symbols:")
                    violations.forEach { appendLine(it) }
                }
            )
        }
    }
}
