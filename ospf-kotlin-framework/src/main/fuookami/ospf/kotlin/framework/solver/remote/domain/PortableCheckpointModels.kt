package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlinx.serialization.Serializable

/** Portable remote checkpoint v2 envelope shared with the dispatcher. */
@Serializable
data class PortableCheckpointEnvelope(
    val schemaVersion: String = "2.0",
    val sourceFormat: String = "v2",
    val migratedFromLegacy: Boolean = false,
    val checkpointId: String,
    val identitySchemaVersion: String = "1.0",
    val identityNamespace: String = "model-local",
    val modelName: String = "remote-cp",
    val modelFingerprint: String,
    val configurationFingerprint: String? = null,
    val solverFingerprint: String? = null,
    val runId: String? = null,
    val attemptId: String? = null,
    val parentCheckpointId: String? = null,
    val createdAtEpochMs: Long,
    val snapshotJson: String,
    val incumbent: PortableConstraintProgrammingIncumbent? = null,
    val bestBound: String? = null,
    val gap: String? = null,
    val assumptions: List<String> = emptyList(),
    val conflicts: List<PortableConstraintProgrammingConflict> = emptyList(),
    val benders: PortableConstraintProgrammingBendersState? = null,
    val integritySha256: String = ""
)

@Serializable
data class PortableConstraintProgrammingIncumbent(
    val valuesById: Map<String, Long> = emptyMap(),
    val intervalsById: Map<String, PortableConstraintProgrammingIntervalValue> = emptyMap(),
    val objective: String? = null
)

@Serializable
data class PortableConstraintProgrammingIntervalValue(
    val start: Long,
    val size: Long,
    val end: Long,
    val present: Boolean = true
)

@Serializable
data class PortableConstraintProgrammingConflict(
    val validity: String,
    val minimality: String,
    val memberIds: List<String> = emptyList(),
    val assumptionIds: List<String> = emptyList(),
    val provenance: Map<String, String> = emptyMap()
)

@Serializable
data class PortableConstraintProgrammingBendersState(
    val iteration: Long,
    val masterIncumbent: String? = null,
    val masterBestBound: String? = null,
    val cuts: List<PortableConstraintProgrammingCut> = emptyList(),
    val trace: List<String> = emptyList(),
    val assumptions: List<String> = emptyList(),
    val fixedBindings: Map<String, Long> = emptyMap(),
    val conflicts: List<PortableConstraintProgrammingConflict> = emptyList(),
    val convergenceVerified: Boolean = false,
    val masterFingerprint: String? = null,
    val subproblemModelFingerprint: String? = null
)

@Serializable
data class PortableConstraintProgrammingCut(
    val id: String,
    val schemaVersion: String,
    val validity: String,
    val provenance: Map<String, String> = emptyMap(),
    val payload: Map<String, String> = emptyMap()
)
