package fuookami.ospf.kotlin.core.solver.report

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import fuookami.ospf.kotlin.core.solver.config.GurobiSolverConfig
import fuookami.ospf.kotlin.core.solver.config.SCIPSolverConfig
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/** Tests for typed, redacted solver metadata. */
class SolverReportMetadataTest {
    @Test
    fun solverFingerprintCoversAllDeclaredCapabilitiesAndCanonicalizesFeatureMapOrder() {
        val capabilities = SolverCapabilities(
            modelTypes = setOf(SolverModelType.LP, SolverModelType.CP),
            nativeIIS = true,
            dual = true,
            farkas = true,
            warmStart = true,
            solutionPool = true,
            callback = true,
            interrupt = true,
            checkpoint = true,
            resume = true,
            constraintProgrammingFeatures = linkedMapOf(
                ConstraintProgrammingFeature.Table to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.BooleanLogic to ConstraintProgrammingSupportLevel.Native
            )
        )
        val descriptor = SolverDescriptor(
            solverId = "capability-fixture",
            backendName = "fixture",
            capabilities = capabilities
        )
        val reordered = descriptor.copy(
            capabilities = capabilities.copy(
                constraintProgrammingFeatures = linkedMapOf(
                    ConstraintProgrammingFeature.BooleanLogic to ConstraintProgrammingSupportLevel.Native,
                    ConstraintProgrammingFeature.Table to ConstraintProgrammingSupportLevel.ExactLowering
                )
            )
        )
        val changed = descriptor.copy(
            capabilities = capabilities.copy(resume = false)
        )

        assertEquals("solver-2", solverFingerprint(descriptor).schemaVersion)
        assertEquals(solverFingerprint(descriptor), solverFingerprint(reordered))
        assertNotEquals(solverFingerprint(descriptor), solverFingerprint(changed))
    }

    @Test
    fun backendConfigurationSnapshotIsTypedRedactedSerializableAndFingerprintable() {
        val config = SolverConfig(
            time = 12.seconds,
            threadNum = UInt64(4),
            gap = Flt64(0.01),
            backendConfiguration = GurobiSolverConfig(
                server = "compute.example",
                password = "secret-value",
                connectionTime = 3.seconds
            )
        )

        val snapshot = config.configurationSnapshot()
        val encoded = Json.encodeToString(BackendConfigurationSnapshot.serializer(), snapshot)

        assertEquals("gurobi", snapshot.type)
        assertTrue(snapshot.parameters.any { it.name == "backend.password" && it.sensitive })
        assertTrue(encoded.contains("<redacted>"))
        assertFalse(encoded.contains("secret-value"))
        assertEquals(snapshot.fingerprint(), config.configurationSnapshot().fingerprint())
    }

    @Test
    fun typedParameterOrderDoesNotChangeFingerprintButValuesDo() {
        val first = BackendConfigurationSnapshot(
            type = "fixture",
            parameters = listOf(
                BackendParameter("seed", BackendParameterValue.Integer(7)),
                BackendParameter("deterministic", BackendParameterValue.BooleanValue(true))
            )
        )
        val reordered = first.copy(parameters = first.parameters.reversed())
        val changed = first.copy(
            parameters = listOf(
                BackendParameter("seed", BackendParameterValue.Integer(8)),
                BackendParameter("deterministic", BackendParameterValue.BooleanValue(true))
            )
        )

        assertEquals(first.fingerprint(), reordered.fingerprint())
        assertNotEquals(first.fingerprint(), changed.fingerprint())
    }

    @Test
    fun solverProvenanceCarriesEffectiveTypedConfigurationAndRuntimeIdentity() {
        val config = SolverConfig(
            threadNum = UInt64(6),
            backendConfiguration = SCIPSolverConfig(
                randomSeed = 17L,
                deterministic = true,
                nativeParameters = mapOf(
                    "limits.time" to BackendParameterValue.Decimal("30.0")
                )
            )
        )
        val descriptor = SolverDescriptor(
            solverId = "scip-fixture",
            backendName = "SCIP",
            backendVersion = "9.0.0",
            pluginVersion = "1.1.0",
            capabilities = SolverCapabilities(modelTypes = setOf(SolverModelType.LP))
        )

        val provenance = solverProvenance(descriptor, config)

        assertEquals("scip-fixture", provenance.descriptor.solverId)
        assertEquals("9.0.0", provenance.nativeVersion)
        assertEquals(6, provenance.threadCount)
        assertEquals(17L, provenance.randomSeed)
        assertEquals(true, provenance.deterministic)
        assertEquals("17", provenance.effectiveParameters["backend.randomSeed"])
        assertEquals("scip", provenance.configuration?.type)
        assertTrue(provenance.configuration?.parameters?.any { it.name == "backend.limits.time" } == true)
    }
}
