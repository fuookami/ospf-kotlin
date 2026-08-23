package fuookami.ospf.kotlin.core.solver.constraint_programming

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingSnapshotCodec
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointEnvelope
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointRestore
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSession
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.SolveFingerprinting
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.IntVar

/**
 * Checkpoint serialization and rebuild-boundary regression tests.
 * checkpoint 序列化与重建边界回归测试。
 */
class ConstraintProgrammingCheckpointProcessTest {
    /**
     * Verify that a serialized incumbent can be restored and reused after a backend rebuild.
     * 验证序列化 incumbent 可在后端重建后恢复并继续使用。
     */
    @Test
    fun serializedCheckpointRestoresEquivalentIncumbentAcrossBackendRebuild() = runBlocking {
        val model = model()
        try {
            val solver = FakeConstraintProgrammingSolver()
            val direct = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(solver.solve(model)).value
            )
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = snapshot,
                    descriptor = solver.descriptor,
                    checkpointId = "process-boundary",
                    createdAtEpochMs = 1L,
                    incumbent = direct.solution,
                    configurationFingerprint = "fixture-configuration",
                    runId = "run-1",
                    attemptId = "slice-1"
                )
            ).value
            val encoded = assertIs<Ok<String, *, *>>(
                ConstraintProgrammingCheckpointCodec.encode(captured)
            ).value

            val checkpointFile = Files.createTempFile("ospf-cp-checkpoint-", ".json")
            try {
                Files.writeString(checkpointFile, encoded)
                val javaExecutable = Path.of(
                    System.getProperty("java.home"),
                    "bin",
                    if (System.getProperty("os.name").contains("win", ignoreCase = true)) "java.exe" else "java"
                )
                val process = ProcessBuilder(
                    javaExecutable.toString(),
                    "-cp",
                    System.getProperty("java.class.path"),
                    ConstraintProgrammingCheckpointProcessProbe::class.java.name,
                    checkpointFile.toString()
                ).redirectErrorStream(true).start()
                val processOutput = process.inputStream.bufferedReader().use { it.readText() }
                assertEquals(0, process.waitFor(), processOutput)
            } finally {
                Files.deleteIfExists(checkpointFile)
            }

            val decoded = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(encoded)
            ).value
            val restored = assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                ConstraintProgrammingCheckpointCodec.restore(
                    envelope = decoded,
                    snapshot = snapshot,
                    allowLegacyConfigurationFingerprint = true,
                    expectedConfigurationFingerprint = decoded.configurationFingerprint,
                    expectedSolverFingerprint = decoded.solverFingerprint
                )
            ).value
            assertEquals(direct.solution, restored.incumbent)

            val session = assertIs<Ok<ConstraintProgrammingSession, *, *>>(solver.createSession(model)).value
            try {
                val resumed = assertIs<ConstraintProgrammingFeasibleOutput>(
                    assertIs<Ok<*, *, *>>(
                        session.solve(hints = restored.incumbent)
                    ).value
                )
                assertEquals(direct.solution, resumed.solution)
                assertEquals(direct.exactObjective, resumed.exactObjective)
            } finally {
                session.close()
            }
        } finally {
            model.close()
        }
    }

    /**
     * Verify scope aliases and provenance permutations share a canonical checkpoint fingerprint.
     * 验证 scope 别名和 provenance 排列变化仍共享规范 checkpoint 指纹。
     */
    @Test
    fun checkpointFingerprintCanonicalizesIdentityAliasesAndProvenanceOrder() {
        val provenance = listOf(
            ModelElementOrigin("source", "canonical"),
            ModelElementOrigin("domain", "canonical")
        )
        val first = canonicalIdentityModel("STABLE", provenance)
        val second = canonicalIdentityModel("stable", provenance.reversed())
        try {
            val solver = FakeConstraintProgrammingSolver()
            val firstSnapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(
                first.snapshot()
            ).value
            val secondSnapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(
                second.snapshot()
            ).value
            val firstCheckpoint = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = firstSnapshot,
                    descriptor = solver.descriptor,
                    checkpointId = "canonical-first",
                    createdAtEpochMs = 1L
                )
            ).value
            val secondCheckpoint = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = secondSnapshot,
                    descriptor = solver.descriptor,
                    checkpointId = "canonical-second",
                    createdAtEpochMs = 1L
                )
            ).value

            assertEquals(firstCheckpoint.modelFingerprint, secondCheckpoint.modelFingerprint)
            assertEquals(firstCheckpoint.snapshotJson, secondCheckpoint.snapshotJson)
        } finally {
            first.close()
            second.close()
        }
    }

    /**
     * Verify a historical snapshot with omitted provenance and legacy scope spelling can be restored.
     * 验证省略 provenance 且使用历史 scope 拼写的 snapshot 仍可恢复。
     */
    @Test
    fun historicalSnapshotJsonIsCanonicalizedBeforeRestore() {
        val model = stableIdentityModel("historical-snapshot-model").first
        try {
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val currentSnapshotJson = ConstraintProgrammingSnapshotCodec.encode(snapshot).value!!
            val root = Json.parseToJsonElement(currentSnapshotJson).jsonObject.toMutableMap()

            fun historicalElement(element: kotlinx.serialization.json.JsonElement): JsonObject {
                val elementRoot = element.jsonObject.toMutableMap()
                elementRoot.remove("identityProvenance")
                elementRoot["scope"] = JsonPrimitive("STABLE")
                return JsonObject(elementRoot)
            }

            root["variables"] = JsonArray(root.getValue("variables").jsonArray.map(::historicalElement))
            root["constraints"] = JsonArray(root.getValue("constraints").jsonArray.map(::historicalElement))
            root["objectives"] = JsonArray(root.getValue("objectives").jsonArray.map(::historicalElement))
            val historicalSnapshotJson = Json.encodeToString(JsonObject(root))
            val legacyCheckpoint = Json.encodeToString(
                buildJsonObject {
                    put("schema", 1)
                    put("modelName", snapshot.name)
                    put("solverId", "historical-solver")
                    put("snapshotJson", historicalSnapshotJson)
                }
            )

            val decoded = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(legacyCheckpoint)
            ).value
            assertEquals(currentSnapshotJson, decoded.snapshotJson)

            val restored = assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                ConstraintProgrammingCheckpointCodec.restore(
                    envelope = decoded,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = null,
                    expectedSolverFingerprint = null
                )
            ).value
            assertEquals(decoded.modelFingerprint, restored.envelope.modelFingerprint)
        } finally {
            model.close()
        }
    }

    /**
     * Verify direct restore migrates a historical v2 envelope before comparing its snapshot. /
     * 验证直接 restore 会先迁移历史 v2 envelope，再比较 snapshot。
     */
    @Test
    fun directRestoreMigratesHistoricalV2SnapshotBeforeFingerprintComparison() {
        val model = stableIdentityModel("historical-v2-restore").first
        try {
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val currentSnapshotJson = ConstraintProgrammingSnapshotCodec.encode(snapshot).value!!
            val root = Json.parseToJsonElement(currentSnapshotJson).jsonObject.toMutableMap()

            fun historicalElement(element: kotlinx.serialization.json.JsonElement): JsonObject {
                val elementRoot = element.jsonObject.toMutableMap()
                elementRoot.remove("identityProvenance")
                elementRoot["scope"] = JsonPrimitive("STABLE")
                return JsonObject(elementRoot)
            }

            root["variables"] = JsonArray(root.getValue("variables").jsonArray.map(::historicalElement))
            root["constraints"] = JsonArray(root.getValue("constraints").jsonArray.map(::historicalElement))
            root["objectives"] = JsonArray(root.getValue("objectives").jsonArray.map(::historicalElement))
            val historicalSnapshotJson = Json.encodeToString(JsonObject(root))
            val captured = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = snapshot,
                    descriptor = FakeConstraintProgrammingSolver().descriptor,
                    checkpointId = "historical-v2",
                    createdAtEpochMs = 1L
                )
            ).value
            val historicalEnvelope = ConstraintProgrammingCheckpointCodec.withIntegrity(
                captured.copy(
                    modelFingerprint = SolveFingerprinting.sha256(historicalSnapshotJson).value,
                    snapshotJson = historicalSnapshotJson,
                    integritySha256 = ""
                )
            )

            val restored = ConstraintProgrammingCheckpointCodec.restore(
                envelope = historicalEnvelope,
                snapshot = snapshot,
                expectedConfigurationFingerprint = null,
                expectedSolverFingerprint = null,
                allowLegacyConfigurationFingerprint = true,
                allowLegacySolverFingerprint = true
            )

            assertTrue(restored.ok)
            assertEquals(currentSnapshotJson, restored.value!!.envelope.snapshotJson)
            assertEquals(
                SolveFingerprinting.sha256(currentSnapshotJson).value,
                restored.value!!.envelope.modelFingerprint
            )
        } finally {
            model.close()
        }
    }

    /**
     * Verify that an interrupted solve without an incumbent does not create one during restore.
     * 验证无 incumbent 的中断求解在恢复时不会凭空生成解。
     */
    @Test
    fun interruptedSolveCheckpointDoesNotInventAnIncumbent() = runBlocking {
        val model = model()
        try {
            val solver = FakeConstraintProgrammingSolver()
            val interrupted = assertIs<ConstraintProgrammingUnknownOutput>(
                assertIs<Ok<*, *, *>>(
                    solver.solve(
                        model,
                        ConstraintProgrammingSolveOptions(
                            timeLimit = kotlin.time.Duration.ZERO
                        )
                    )
                ).value
            )
            assertEquals(TerminationReason.TimeLimit, interrupted.terminationReason)

            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = snapshot,
                    descriptor = solver.descriptor,
                    checkpointId = "interrupted-without-incumbent",
                    createdAtEpochMs = 2L
                )
            ).value
            val encoded = assertIs<Ok<String, *, *>>(
                ConstraintProgrammingCheckpointCodec.encode(captured)
            ).value
            val decoded = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(encoded)
            ).value
            val restored = assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                ConstraintProgrammingCheckpointCodec.restore(
                    envelope = decoded,
                    snapshot = snapshot,
                    allowLegacyConfigurationFingerprint = true,
                    expectedConfigurationFingerprint = decoded.configurationFingerprint,
                    expectedSolverFingerprint = decoded.solverFingerprint
                )
            ).value
            assertNull(restored.incumbent)
        } finally {
            model.close()
        }
    }

    /**
     * Verify that a checkpoint is rejected when restored against a different model snapshot.
     * 验证 checkpoint 使用不同模型 snapshot 恢复时会被拒绝。
     */
    @Test
    fun incompatibleSnapshotIsRejectedAfterSerializationBoundary() = runBlocking {
        val original = model()
        val rebuilt = model(name = "different-model")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val originalSnapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(original.snapshot()).value
            val captured = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = originalSnapshot,
                    descriptor = solver.descriptor,
                    checkpointId = "incompatible-model",
                    createdAtEpochMs = 3L
                )
            ).value
            val encoded = assertIs<Ok<String, *, *>>(
                ConstraintProgrammingCheckpointCodec.encode(captured)
            ).value
            val decoded = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(encoded)
            ).value
            val rebuiltSnapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(rebuilt.snapshot()).value
            assertIs<Failed<*, *, *>>(
                ConstraintProgrammingCheckpointCodec.restore(
                    decoded,
                    rebuiltSnapshot,
                    expectedConfigurationFingerprint = null,
                    expectedSolverFingerprint = null
                )
            )
            Unit
        } finally {
            original.close()
            rebuilt.close()
        }
    }

    /**
     * Reject a directly constructed legacy envelope with a non-legacy source identifier.
     * 拒绝直接构造但来源标识不是 legacy 的 envelope。
     */
    @Test
    fun legacySourceRequiresTheCanonicalLegacyCheckpointId() {
        val model = model("legacy-source-id-model")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = snapshot,
                    descriptor = solver.descriptor,
                    checkpointId = "legacy-source-id",
                    createdAtEpochMs = 4L
                )
            ).value
            val legacy = ConstraintProgrammingCheckpointCodec.withIntegrity(
                captured.copy(sourceFormat = "legacy-v1")
            )
            assertIs<Failed<*, *, *>>(
                ConstraintProgrammingCheckpointCodec.restore(
                    envelope = legacy,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = null,
                    expectedSolverFingerprint = null,
                    allowLegacyConfigurationFingerprint = true,
                    allowLegacySolverFingerprint = true
                )
            )
        } finally {
            model.close()
        }
    }

    /**
     * Verify that legacy v1 decoding migrates to a readable ordinary v2 document.
     * 验证 legacy v1 解码后迁移出的普通 v2 文档仍可再次读取。
     */
    @Test
    fun legacyV1CanBeEncodedAndDecodedAgainAsV2() {
        val model = model("legacy-round-trip-model")
        try {
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val snapshotJson = ConstraintProgrammingSnapshotCodec.encode(snapshot).value!!
            val legacy = Json.encodeToString(
                buildJsonObject {
                    put("schema", 1)
                    put("modelName", snapshot.name)
                    put("solverId", "legacy-solver")
                    put("snapshotJson", snapshotJson)
                }
            )
            val migrated = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(legacy)
            ).value
            val encoded = assertIs<Ok<String, *, *>>(
                ConstraintProgrammingCheckpointCodec.encode(migrated)
            ).value
            val roundTripped = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(encoded)
            ).value

            assertEquals("v2", roundTripped.sourceFormat)
            assertTrue(roundTripped.migratedFromLegacy)
            assertEquals("legacy-v1-migrated", roundTripped.checkpointId)
            assertEquals(snapshotJson, roundTripped.snapshotJson)
        } finally {
            model.close()
        }
    }

    /**
     * Verify that a malformed v2 document carrying a legacy schema marker is not downgraded.
     * 验证带 legacy schema 标记的损坏 v2 文档不会被降级接受。
     */
    @Test
    fun malformedV2WithLegacySchemaMarkerIsRejected() {
        val model = model("malformed-v2-model")
        try {
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = snapshot,
                    descriptor = FakeConstraintProgrammingSolver().descriptor,
                    checkpointId = "malformed-v2",
                    createdAtEpochMs = 5L,
                    configurationFingerprint = "configuration",
                    runId = "run",
                    attemptId = "attempt"
                )
            ).value
            val encoded = assertIs<Ok<String, *, *>>(
                ConstraintProgrammingCheckpointCodec.encode(captured)
            ).value
            val root = Json.parseToJsonElement(encoded).jsonObject.toMutableMap()
            root["schema"] = kotlinx.serialization.json.JsonPrimitive(1)
            val malformed = Json.encodeToString(JsonObject(root))
            assertTrue(ConstraintProgrammingCheckpointCodec.decodeCompatible(malformed).failed)
        } finally {
            model.close()
        }
    }

    /**
     * Verify that a published V2 Benders envelope without masterFingerprint remains readable.
     * 验证缺少 masterFingerprint 的已发布 V2 Benders envelope 仍可读取。
     */
    @Test
    fun publishedV2BendersEnvelopeWithoutMasterFingerprintRemainsReadable() {
        val model = model("legacy-v2-benders-model")
        try {
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = snapshot,
                    descriptor = FakeConstraintProgrammingSolver().descriptor,
                    checkpointId = "legacy-v2-benders",
                    createdAtEpochMs = 7L,
                    configurationFingerprint = "configuration",
                    runId = "run",
                    attemptId = "slice",
                    benders = PortableConstraintProgrammingBendersState(
                        iteration = 2L,
                        masterFingerprint = "master",
                        cuts = listOf(
                            PortableConstraintProgrammingCut(
                                id = "cut-1",
                                schemaVersion = "domain-1",
                                validity = "Global"
                            )
                        )
                    )
                )
            ).value
            val encoded = assertIs<Ok<String, *, *>>(
                ConstraintProgrammingCheckpointCodec.encode(captured)
            ).value
            val root = Json.parseToJsonElement(encoded).jsonObject.toMutableMap()
            root["benders"] = JsonObject(root.getValue("benders").jsonObject.toMutableMap().apply {
                remove("masterFingerprint")
            })
            root["integritySha256"] = JsonPrimitive("")
            val unsigned = JsonObject(root)
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(unsigned.toString().toByteArray(Charsets.UTF_8))
                .joinToString(separator = "") { byte -> "%02x".format(byte) }
            root["integritySha256"] = JsonPrimitive(digest)
            val legacy = Json.encodeToString(JsonObject(root))

            val decoded = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(legacy)
            ).value
            assertEquals(null, decoded.benders?.masterFingerprint)
            assertEquals("cut-1", decoded.benders?.cuts?.single()?.id)
            val restoredResult = ConstraintProgrammingCheckpointCodec.restore(
                envelope = decoded,
                snapshot = snapshot,
                expectedConfigurationFingerprint = "configuration",
                expectedSolverFingerprint = decoded.solverFingerprint
            )
            assertTrue(
                restoredResult is Ok,
                (restoredResult as? Failed)?.error?.message ?: restoredResult.toString()
            )
            val restored = (restoredResult as Ok).value
            assertEquals(null, restored.envelope.benders?.masterFingerprint)

            val masterRoot = root.toMutableMap()
            masterRoot["benders"] = JsonObject(root.getValue("benders").jsonObject.toMutableMap().apply {
                put("masterIncumbent", JsonPrimitive("1"))
            })
            masterRoot["integritySha256"] = JsonPrimitive("")
            val masterUnsigned = JsonObject(masterRoot)
            val masterDigest = MessageDigest.getInstance("SHA-256")
                .digest(masterUnsigned.toString().toByteArray(Charsets.UTF_8))
                .joinToString(separator = "") { byte -> "%02x".format(byte) }
            masterRoot["integritySha256"] = JsonPrimitive(masterDigest)
            val masterDecoded = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(Json.encodeToString(JsonObject(masterRoot)))
            ).value
            assertIs<Failed<*, *, *>>(
                ConstraintProgrammingCheckpointCodec.restore(
                    envelope = masterDecoded,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = "configuration",
                    expectedSolverFingerprint = masterDecoded.solverFingerprint
                )
            )
        } finally {
            model.close()
        }
    }

    /**
     * Verify that stable identity metadata survives a checkpoint capture/encode/decode/restore round trip.
     * 验证稳定身份元数据在 checkpoint capture/encode/decode/restore 往返后保持不变。
     */
    @Test
    fun checkpointRoundTripPreservesStableIdentity() {
        val (model, variable) = stableIdentityModel("checkpoint-identity-model")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = snapshot,
                    descriptor = solver.descriptor,
                    checkpointId = "identity-round-trip",
                    createdAtEpochMs = 8L
                )
            ).value
            val encoded = assertIs<Ok<String, *, *>>(
                ConstraintProgrammingCheckpointCodec.encode(captured)
            ).value
            val decoded = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(encoded)
            ).value

            assertEquals(snapshot.identitySchemaVersion, decoded.identitySchemaVersion)
            assertEquals(snapshot.identityNamespace, decoded.identityNamespace)
            assertEquals("2.0", snapshot.identitySchemaVersion)
            assertEquals("cp-identity", snapshot.identityNamespace)
            assertEquals("identity-round-trip", decoded.checkpointId)

            val restored = assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                ConstraintProgrammingCheckpointCodec.restore(
                    envelope = decoded,
                    snapshot = snapshot,
                    allowLegacyConfigurationFingerprint = true,
                    expectedConfigurationFingerprint = decoded.configurationFingerprint,
                    expectedSolverFingerprint = decoded.solverFingerprint
                )
            ).value

            val decodedSnapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(
                ConstraintProgrammingSnapshotCodec.decode(
                    decoded.snapshotJson,
                    mapOf(snapshot.variables.single().id to variable)
                )
            ).value
            assertEquals(identityManifest(snapshot), identityManifest(decodedSnapshot))
            assertNull(restored.incumbent)
        } finally {
            model.close()
        }
    }

    private fun model(name: String = "checkpoint-process-model"): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel(name, ObjectCategory.Minimum)
        val value = IntVar("checkpoint-value")
        model.registerVariable(value, IntegerDomain.interval(0, 3).value!!)
        val expression = ConstraintProgrammingExpression.Variable(value)
        model.addConstraint(
            ConstraintProgrammingConstraint.greaterOrEqual(expression, Int64.one).value!!,
            id = "checkpoint-lower-bound"
        )
        model.minimize(expression)
        return model
    }

    private fun canonicalIdentityModel(
        scope: String,
        provenance: List<ModelElementOrigin>
    ): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel(
            name = "canonical-checkpoint-model",
            objectCategory = ObjectCategory.Minimum,
            identityNamespace = "cp-canonical",
            identitySchemaVersion = "1.0"
        )
        val value = IntVar("canonical-checkpoint-value")
        model.registerVariable(
            id = VariableId("variable:canonical-checkpoint-value"),
            variable = value,
            domain = IntegerDomain.interval(0, 3).value!!,
            scope = scope,
            origin = "legacy/canonical",
            identityProvenance = provenance
        )
        val expression = ConstraintProgrammingExpression.Variable(value)
        model.addConstraint(
            constraint = ConstraintProgrammingConstraint.greaterOrEqual(expression, Int64.one).value!!,
            id = ConstraintId("constraint:canonical-checkpoint-lower-bound"),
            scope = scope,
            origin = "legacy/canonical",
            identityProvenance = provenance
        )
        model.minimize(
            expression = expression,
            id = ObjectiveId("objective:canonical-checkpoint"),
            scope = scope,
            origin = "legacy/canonical",
            identityProvenance = provenance
        )
        return model
    }

    private fun stableIdentityModel(name: String): Pair<ConstraintProgrammingModel, IntVar> {
        val model = ConstraintProgrammingModel(
            name = name,
            objectCategory = ObjectCategory.Minimum,
            identityNamespace = "cp-identity",
            identitySchemaVersion = "2.0"
        )
        val value = IntVar("checkpoint-identity-value")
        model.registerVariable(
            value,
            IntegerDomain.interval(0, 3).value!!,
            scope = "stable",
            origin = "fixture/business-x"
        )
        val expression = ConstraintProgrammingExpression.Variable(value)
        model.addConstraint(
            ConstraintProgrammingConstraint.greaterOrEqual(expression, Int64.one).value!!,
            id = "checkpoint-identity-lower-bound",
            scope = "stable",
            origin = "fixture/non-negative"
        )
        model.minimize(expression, id = ObjectiveId("checkpoint-identity-objective"), scope = "stable", origin = "fixture/objective")
        return model to value
    }

    private fun identityManifest(snapshot: ConstraintProgrammingModelSnapshot): List<String> {
        val elements = ArrayList<String>()
        snapshot.variables.forEach {
            elements.add("variable|${it.id}|${it.scope}|${it.origin}|${it.identityProvenance}")
        }
        snapshot.intervals.forEach {
            elements.add("interval|${it.id}|${it.scope}|${it.origin}|${it.identityProvenance}")
        }
        snapshot.constraints.forEach {
            elements.add("constraint|${it.id}|${it.scope}|${it.origin}|${it.identityProvenance}")
        }
        snapshot.objectives.forEach {
            elements.add("objective|${it.id}|${it.scope}|${it.origin}|${it.identityProvenance}")
        }
        return elements.sorted()
    }
}

/**
 * Independent JVM probe for checkpoint decoding.
 * 用于 checkpoint 解码的独立 JVM 探针。
 */
object ConstraintProgrammingCheckpointProcessProbe {
    /**
     * Decode a checkpoint file in a separate JVM and validate its incumbent presence.
     * 在独立 JVM 中解码 checkpoint 文件并校验 incumbent 存在。
     *
     * @param args single checkpoint file path / 单个 checkpoint 文件路径
     */
    @JvmStatic
    fun main(args: Array<String>) {
        check(args.size == 1) { "Expected one checkpoint file path" }
        val encoded = Files.readString(Path.of(args[0]))
        val decoded = ConstraintProgrammingCheckpointCodec.decodeCompatible(encoded)
        check(decoded.ok && decoded.value?.incumbent != null) {
            "Checkpoint decode failed in independent JVM: $decoded"
        }
    }
}
