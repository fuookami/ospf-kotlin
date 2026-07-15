/**
 * Bpp3D 迁移边界测试。
 * Bpp3D migration boundary test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.io.path.*
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.*

class Bpp3dMigrationBoundaryTest {
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

    @Test
    fun migrationQuantityNamesShouldNotReturn() {
        val root = bpp3dRoot()
        val forbiddenTokens = listOf(
            "Leg" + "acy" + "Quantity",
            "Infra" + "Number",
            "Infra" + "Quantity",
            "infra" + "Scalar"
        )
        val violations = Files.walk(root).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .filter { isMainKotlin(it) }
                .flatMap { path ->
                    Files
                        .readAllLines(path)
                        .mapIndexedNotNull { index, line ->
                            if (forbiddenTokens.any { token -> line.contains(token) }) {
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
                prefix = "Migration quantity names returned:${System.lineSeparator()}"
            )
        )
    }
}
