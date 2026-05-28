package fuookami.ospf.kotlin.framework.bpp3d.application.service

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class Bpp3dGenericBoundaryTest {
    private fun bpp3dRoot(): Path {
        val cwd = Path.of("").toAbsolutePath()
        val candidates = generateSequence(cwd) { it.parent }.flatMap { path ->
            sequenceOf(path, path.resolve("ospf-kotlin-framework-bpp3d"))
        }
        return candidates.first { candidate ->
            candidate.isDirectory() && candidate.resolve("bpp3d-application").isDirectory()
        }
    }

    private fun isMainKotlin(path: Path): Boolean {
        val normalized = path.pathString.replace('\\', '/')
        return normalized.contains("/src/main/") && path.name.endsWith(".kt")
    }

    private fun isLegacyQuantityAllowed(path: Path): Boolean {
        val normalized = path.pathString.replace('\\', '/')
        return normalized.contains("/api/compat/")
                || normalized.contains("/model/compat/")
                || normalized.contains("/LegacyScalars.kt")
                || normalized.contains("/legacy/")
                || normalized.contains("/compat/")
    }

    @Test
    fun legacyQuantityShouldStayInsideCompatibilityBoundary() {
        val root = bpp3dRoot()
        val violations = Files.walk(root).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .filter { isMainKotlin(it) }
                .filter { !isLegacyQuantityAllowed(it) }
                .flatMap { path ->
                    Files
                        .readAllLines(path)
                        .mapIndexedNotNull { index, line ->
                            if (line.contains("LegacyQuantity")) {
                                "${root.relativize(path)}:${index + 1}:$line"
                            } else {
                                null
                            }
                        }
                        .stream()
                }
                .toList()
        }

        assertTrue(
            actual = violations.isEmpty(),
            message = violations.joinToString(
                separator = System.lineSeparator(),
                prefix = "LegacyQuantity leaked outside compatibility boundary:${System.lineSeparator()}"
            )
        )
    }
}
