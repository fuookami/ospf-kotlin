package fuookami.ospf.kotlin.core.solver.scip

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ScipBindingCapabilityAssessmentTest {
    @Test
    fun currentBindingReportsRebuildFallbackForUnavailableIncrementalApis() {
        val assessment = ScipBindingCapabilityAssessmentProvider.current()

        assertEquals("jscip-1.0.0", assessment.bindingVersion)
        assertFalse(assessment.incrementalBoundUpdates)
        assertFalse(assessment.probing)
        assertFalse(assessment.conflictGraph)
        assertEquals("model-rebuild", assessment.fallback)
    }

    @Test
    fun runtimeFingerprintIsIndependentOfNativeLibraryPath() {
        val first = Files.createTempFile("ospf-jscip", ".bin")
        val second = Files.createTempFile("ospf-jscip", ".bin")
        val previous = System.getProperty("ospf.scip.library")
        try {
            val bytes = byteArrayOf(1, 3, 5, 7, 11)
            Files.write(first, bytes)
            Files.write(second, bytes)
            System.setProperty("ospf.scip.library", first.toString())
            val firstFingerprint = scipRuntimeFingerprint()
            System.setProperty("ospf.scip.library", second.toString())
            val secondFingerprint = scipRuntimeFingerprint()
            assertEquals(firstFingerprint, secondFingerprint)
            assertFalse(firstFingerprint.contains(first.toString()))
            assertFalse(firstFingerprint.contains(second.toString()))
        } finally {
            if (previous == null) {
                System.clearProperty("ospf.scip.library")
            } else {
                System.setProperty("ospf.scip.library", previous)
            }
            Files.deleteIfExists(first)
            Files.deleteIfExists(second)
        }
    }

    @Test
    fun runtimeFingerprintIsIndependentOfExplicitOrSystemLibraryLoading() {
        val explicit = Files.createTempFile("ospf-jscip-explicit", ".bin")
        val systemDirectory = Files.createTempDirectory("ospf-jscip-system")
        val system = systemDirectory.resolve(System.mapLibraryName("jscip"))
        val previousExplicit = System.getProperty("ospf.scip.library")
        val previousLibraryPath = System.getProperty("java.library.path")
        try {
            val bytes = byteArrayOf(13, 21, 34, 55, 89)
            Files.write(explicit, bytes)
            Files.write(system, bytes)

            System.setProperty("ospf.scip.library", explicit.toString())
            val explicitFingerprint = scipRuntimeFingerprint()
            System.clearProperty("ospf.scip.library")
            System.setProperty("java.library.path", systemDirectory.toString())
            val systemFingerprint = scipRuntimeFingerprint()

            assertEquals(explicitFingerprint, systemFingerprint)
            assertTrue(explicitFingerprint.startsWith("scip-runtime-2:"))
            assertFalse(explicitFingerprint.contains(explicit.toString()))
            assertFalse(explicitFingerprint.contains(systemDirectory.toString()))
        } finally {
            if (previousExplicit == null) {
                System.clearProperty("ospf.scip.library")
            } else {
                System.setProperty("ospf.scip.library", previousExplicit)
            }
            if (previousLibraryPath == null) {
                System.clearProperty("java.library.path")
            } else {
                System.setProperty("java.library.path", previousLibraryPath)
            }
            Files.deleteIfExists(explicit)
            Files.deleteIfExists(system)
            Files.deleteIfExists(systemDirectory)
        }
    }

    @Test
    fun runtimeFingerprintIncludesNonAdjacentScipDependencyContent() {
        val primary = Files.createTempFile("ospf-jscip-primary", ".bin")
        val dependencyDirectory = Files.createTempDirectory("ospf-jscip-dependency")
        val dependencyName = when {
            System.getProperty("os.name").contains("win", ignoreCase = true) -> "libscip.dll"
            System.getProperty("os.name").contains("mac", ignoreCase = true) -> "libscip.dylib"
            else -> "libscip.so"
        }
        val dependency = dependencyDirectory.resolve(dependencyName)
        val previousExplicit = System.getProperty("ospf.scip.library")
        val previousLibraryPath = System.getProperty("java.library.path")
        try {
            Files.write(primary, byteArrayOf(1, 2, 3))
            Files.write(dependency, byteArrayOf(4, 5, 6))
            System.setProperty("ospf.scip.library", primary.toString())
            System.setProperty("java.library.path", dependencyDirectory.toString())
            val first = scipRuntimeFingerprint()

            Files.write(dependency, byteArrayOf(7, 8, 9))
            val second = scipRuntimeFingerprint()
            assertNotEquals(first, second)
            assertFalse(first.contains(dependencyDirectory.toString()))
            assertFalse(second.contains(dependencyDirectory.toString()))
        } finally {
            restoreProperty("ospf.scip.library", previousExplicit)
            restoreProperty("java.library.path", previousLibraryPath)
            Files.deleteIfExists(dependency)
            Files.deleteIfExists(dependencyDirectory)
            Files.deleteIfExists(primary)
        }
    }

    @Test
    fun legacyFingerprintsRemainExplicitlyVersioned() {
        val candidates = scipRuntimeFingerprintLegacyV1Candidates()
        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.all { it.startsWith("scip-runtime-1:") })
        assertTrue(candidates.none { it == scipRuntimeFingerprint() })
    }

    @Test
    fun legacyFingerprintsReplayBothHistoricalCanonicalizations() {
        val library = Files.createTempFile("ospf-jscip-legacy", ".bin")
        val systemDirectory = Files.createTempDirectory("ospf-jscip-legacy-system")
        val systemLibrary = systemDirectory.resolve(System.mapLibraryName("jscip"))
        val previousLibrary = System.getProperty("ospf.scip.library")
        val previousLibraryPath = System.getProperty("java.library.path")
        val previousNative = System.getProperty("ospf.scip.native.version")
        val previousScipVersion = System.getProperty("scip.version")
        val previousPlugin = System.getProperty("ospf.scip.plugin.version")
        try {
            val bytes = byteArrayOf(2, 3, 5, 7, 11, 13)
            Files.write(library, bytes)
            Files.write(systemLibrary, bytes)
            System.setProperty("ospf.scip.library", library.toString())
            System.setProperty("java.library.path", systemDirectory.toString())
            System.setProperty("ospf.scip.native.version", "path-native")
            System.setProperty("scip.version", "content-native")
            System.setProperty("ospf.scip.plugin.version", "stable-plugin")

            val pathPlugins = historicalPathPluginVersions()
            val digest = sha256(bytes)
            val expectedWithExplicitNative = pathPlugins.mapTo(linkedSetOf()) { pluginVersion ->
                historicalFingerprint(
                    nativeVersion = "path-native",
                    pluginVersion = pluginVersion,
                    libraryIdentity = historicalPathLibraryIdentity(library)
                )
            } + setOf(
                historicalFingerprint(
                    nativeVersion = "path-native",
                    pluginVersion = "stable-plugin",
                    libraryIdentity = "explicit:sha256:$digest"
                ),
                historicalFingerprint(
                    nativeVersion = "path-native",
                    pluginVersion = "stable-plugin",
                    libraryIdentity = "system:sha256:$digest"
                )
            )
            assertEquals(expectedWithExplicitNative, scipRuntimeFingerprintLegacyV1Candidates())

            System.clearProperty("ospf.scip.native.version")
            val environmentNative = System.getenv("SCIP_VERSION")
                ?.takeUnless { it.isBlank() }
                ?: "unknown"
            val expectedWithScipVersion = pathPlugins.mapTo(linkedSetOf()) { pluginVersion ->
                historicalFingerprint(
                    nativeVersion = environmentNative,
                    pluginVersion = pluginVersion,
                    libraryIdentity = historicalPathLibraryIdentity(library)
                )
            } + setOf(
                historicalFingerprint(
                    nativeVersion = "content-native",
                    pluginVersion = "stable-plugin",
                    libraryIdentity = "explicit:sha256:$digest"
                ),
                historicalFingerprint(
                    nativeVersion = "content-native",
                    pluginVersion = "stable-plugin",
                    libraryIdentity = "system:sha256:$digest"
                )
            )
            assertEquals(expectedWithScipVersion, scipRuntimeFingerprintLegacyV1Candidates())
            assertNotEquals(expectedWithExplicitNative, expectedWithScipVersion)
        } finally {
            restoreProperty("ospf.scip.library", previousLibrary)
            restoreProperty("java.library.path", previousLibraryPath)
            restoreProperty("ospf.scip.native.version", previousNative)
            restoreProperty("scip.version", previousScipVersion)
            restoreProperty("ospf.scip.plugin.version", previousPlugin)
            Files.deleteIfExists(library)
            Files.deleteIfExists(systemLibrary)
            Files.deleteIfExists(systemDirectory)
        }
    }

    private fun historicalPathPluginVersions(): Set<String> {
        val packageVersion = ScipConstraintProgrammingSolver::class.java.`package`?.implementationVersion
            ?.takeUnless { it.isBlank() }
        val codeSourceVersion = ScipConstraintProgrammingSolver::class.java.protectionDomain?.codeSource?.location
            ?.toExternalForm()
            ?.takeUnless { it.isBlank() }
        return buildSet {
            packageVersion?.let(::add)
            codeSourceVersion?.let(::add)
            if (isEmpty()) {
                add("unknown")
            }
        }
    }

    private fun historicalPathLibraryIdentity(library: Path): String {
        val attributes = Files.readAttributes(
            library,
            java.nio.file.attribute.BasicFileAttributes::class.java
        )
        return "explicit:$library:size=${attributes.size()}:modified=${attributes.lastModifiedTime().toMillis()}"
    }

    private fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun historicalFingerprint(
        nativeVersion: String,
        pluginVersion: String,
        libraryIdentity: String
    ): String {
        val canonical = listOf(
            "solverId=scip-cp",
            "backend=SCIP",
            "nativeVersion=$nativeVersion",
            "bindingVersion=jscip-1.0.0",
            "pluginVersion=$pluginVersion",
            "nativeLibraryIdentity=$libraryIdentity"
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        return "scip-runtime-1:$digest"
    }

    private fun restoreProperty(name: String, value: String?) {
        if (value == null) {
            System.clearProperty(name)
        } else {
            System.setProperty(name, value)
        }
    }
}
