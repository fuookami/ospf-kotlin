package fuookami.ospf.kotlin.core

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

class CoreToDoubleBridgeGuardTest {
    private val scanRoot = File("src/main")
    private val pathPrefix = "fuookami/ospf/kotlin/core/"

    private val allowedPath = "fuookami/ospf/kotlin/core/solver/value/SolveValueConversionContext.kt"
    private val allowedLine = "val converted = this.toDouble()"

    @Test
    fun shouldNotIntroduceToDoubleOutsideAllowlistInCore() {
        if (!scanRoot.exists()) {
            fail("Scan root not found: ${scanRoot.path}")
        }

        val pattern = Regex("""\.toDouble\(\)""")
        val violations = mutableListOf<String>()
        val allowedHits = mutableListOf<String>()

        scanRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relativePath = file.toRelativeString(scanRoot).replace('\\', '/')
                if (!relativePath.startsWith(pathPrefix)) {
                    return@forEach
                }
                file.readLines().forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/**")) {
                        return@forEachIndexed
                    }
                    if (pattern.containsMatchIn(trimmed)) {
                        val location = "$relativePath:${index + 1}"
                        val isAllowed = relativePath == allowedPath && trimmed == allowedLine
                        if (isAllowed) {
                            allowedHits += location
                        } else {
                            violations += "$location: $trimmed"
                        }
                    }
                }
            }

        if (violations.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("Detected new .toDouble() usage in core outside allowlist:")
                    violations.forEach { appendLine(it) }
                }
            )
        }

        if (allowedHits.size != 1) {
            fail("Expected exactly 1 allowed .toDouble() bridge in $allowedPath, but found ${allowedHits.size}.")
        }
    }
}