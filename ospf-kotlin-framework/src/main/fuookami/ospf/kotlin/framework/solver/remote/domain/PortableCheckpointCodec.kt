package fuookami.ospf.kotlin.framework.solver.remote.domain

import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** v2 checkpoint codec kept wire-compatible with the dispatcher protocol module. */
object PortableCheckpointCodec {
    private const val CURRENT_SCHEMA = "2.0"
    private val v2MarkerFields = setOf(
        "schemaVersion", "sourceFormat", "migratedFromLegacy", "checkpointId",
        "identitySchemaVersion", "identityNamespace", "modelFingerprint",
        "configurationFingerprint", "solverFingerprint", "runId", "attemptId",
        "parentCheckpointId", "createdAtEpochMs", "incumbent", "bestBound", "gap",
        "assumptions", "conflicts", "benders", "integritySha256"
    )
    private val legacyV1Fields = setOf("schema", "modelName", "solverId", "snapshotJson")
    private val codecJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    @Serializable
    private data class LegacyBendersState(
        val iteration: Long,
        val masterIncumbent: String? = null,
        val masterBestBound: String? = null,
        val cuts: List<PortableConstraintProgrammingCut> = emptyList(),
        val trace: List<String> = emptyList(),
        val assumptions: List<String> = emptyList(),
        val fixedBindings: Map<String, Long> = emptyMap(),
        val conflicts: List<PortableConstraintProgrammingConflict> = emptyList(),
        val convergenceVerified: Boolean = false,
        val subproblemModelFingerprint: String? = null
    )

    @Serializable
    private data class LegacyEnvelope(
        val schemaVersion: String = CURRENT_SCHEMA,
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
        val benders: LegacyBendersState? = null,
        val integritySha256: String = ""
    )

    fun encode(envelope: PortableCheckpointEnvelope): String {
        val migrated = envelope.sourceFormat == "legacy-v1" || envelope.migratedFromLegacy
        val normalized = envelope.copy(
            schemaVersion = CURRENT_SCHEMA,
            sourceFormat = "v2",
            migratedFromLegacy = migrated,
            checkpointId = if (migrated) "legacy-v1-migrated" else envelope.checkpointId,
            integritySha256 = digest(
                envelope.copy(
                    schemaVersion = CURRENT_SCHEMA,
                    sourceFormat = "v2",
                    migratedFromLegacy = migrated,
                    checkpointId = if (migrated) "legacy-v1-migrated" else envelope.checkpointId,
                    integritySha256 = ""
                )
            )
        )
        return codecJson.encodeToString(PortableCheckpointEnvelope.serializer(), normalized)
    }

    /** Decode only a verified v2 envelope. Legacy migration is deliberately separate. */
    fun decodeOrNull(encoded: String): PortableCheckpointEnvelope? {
        return runCatching {
            val envelope = codecJson.decodeFromString(PortableCheckpointEnvelope.serializer(), encoded)
            if (envelope.schemaVersion != CURRENT_SCHEMA || envelope.sourceFormat != "v2") {
                return@runCatching null
            }
            if (!envelope.migratedFromLegacy && envelope.checkpointId == "legacy-v1") {
                return@runCatching null
            }
            if (envelope.migratedFromLegacy && envelope.checkpointId != "legacy-v1-migrated") {
                return@runCatching null
            }
            if (envelope.integritySha256.isBlank()) {
                return@runCatching null
            }
            if (envelope.integritySha256 != digest(envelope.copy(integritySha256 = ""))) {
                return@runCatching decodeLegacyV2OrNull(encoded)
            }
            if (sha256(envelope.snapshotJson) != envelope.modelFingerprint) {
                return@runCatching null
            }
            envelope
        }.getOrNull()
    }

    /** Decode an explicitly recognized legacy v1 snapshot and return a migrated envelope. */
    fun decodeCompatibleOrNull(encoded: String): PortableCheckpointEnvelope? {
        decodeOrNull(encoded)?.let { return it }
        return runCatching {
            val root = codecJson.parseToJsonElement(encoded).jsonObject
            if (root.keys.any { it in v2MarkerFields } || root.keys.any { it !in legacyV1Fields }) {
                return@runCatching null
            }
            if (root["schema"]?.jsonPrimitive?.intOrNull != 1) {
                return@runCatching null
            }
            val snapshotJson = root["snapshotJson"]?.jsonPrimitive?.content
                ?: return@runCatching null
            val snapshotRoot = codecJson.parseToJsonElement(snapshotJson).jsonObject
            PortableCheckpointEnvelope(
                checkpointId = "legacy-v1",
                sourceFormat = "legacy-v1",
                migratedFromLegacy = false,
                identitySchemaVersion = snapshotRoot["identitySchemaVersion"]?.jsonPrimitive?.content ?: "1.0",
                identityNamespace = snapshotRoot["identityNamespace"]?.jsonPrimitive?.content ?: "model-local",
                modelName = root["modelName"]?.jsonPrimitive?.content
                    ?: snapshotRoot["name"]?.jsonPrimitive?.content ?: "legacy",
                modelFingerprint = sha256(snapshotJson),
                createdAtEpochMs = 0L,
                snapshotJson = snapshotJson
            )
        }.getOrNull()
    }

    private fun digest(envelope: PortableCheckpointEnvelope): String =
        sha256(codecJson.encodeToString(PortableCheckpointEnvelope.serializer(), envelope))

    private fun digest(envelope: LegacyEnvelope): String =
        sha256(codecJson.encodeToString(LegacyEnvelope.serializer(), envelope))

    private fun decodeLegacyV2OrNull(encoded: String): PortableCheckpointEnvelope? {
        return runCatching {
            val legacy = codecJson.decodeFromString(LegacyEnvelope.serializer(), encoded)
            if (legacy.schemaVersion != CURRENT_SCHEMA || legacy.sourceFormat != "v2" ||
                (!legacy.migratedFromLegacy && legacy.checkpointId == "legacy-v1") ||
                (legacy.migratedFromLegacy && legacy.checkpointId != "legacy-v1-migrated") ||
                legacy.integritySha256.isBlank() ||
                legacy.integritySha256 != digest(legacy.copy(integritySha256 = "")) ||
                sha256(legacy.snapshotJson) != legacy.modelFingerprint
            ) {
                return@runCatching null
            }
            PortableCheckpointEnvelope(
                schemaVersion = legacy.schemaVersion,
                sourceFormat = legacy.sourceFormat,
                migratedFromLegacy = legacy.migratedFromLegacy,
                checkpointId = legacy.checkpointId,
                identitySchemaVersion = legacy.identitySchemaVersion,
                identityNamespace = legacy.identityNamespace,
                modelName = legacy.modelName,
                modelFingerprint = legacy.modelFingerprint,
                configurationFingerprint = legacy.configurationFingerprint,
                solverFingerprint = legacy.solverFingerprint,
                runId = legacy.runId,
                attemptId = legacy.attemptId,
                parentCheckpointId = legacy.parentCheckpointId,
                createdAtEpochMs = legacy.createdAtEpochMs,
                snapshotJson = legacy.snapshotJson,
                incumbent = legacy.incumbent,
                bestBound = legacy.bestBound,
                gap = legacy.gap,
                assumptions = legacy.assumptions,
                conflicts = legacy.conflicts,
                benders = legacy.benders?.let { state ->
                    PortableConstraintProgrammingBendersState(
                        iteration = state.iteration,
                        masterIncumbent = state.masterIncumbent,
                        masterBestBound = state.masterBestBound,
                        cuts = state.cuts,
                        trace = state.trace,
                        assumptions = state.assumptions,
                        fixedBindings = state.fixedBindings,
                        conflicts = state.conflicts,
                        convergenceVerified = state.convergenceVerified,
                        subproblemModelFingerprint = state.subproblemModelFingerprint
                    )
                },
                integritySha256 = legacy.integritySha256
            )
        }.getOrNull()
    }

    fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
