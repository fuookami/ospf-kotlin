/** Portable CP checkpoint v2. / 可移植 CP checkpoint v2。 */
package fuookami.ospf.kotlin.core.solver.constraint_programming

import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingSnapshotCodec
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalValue
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.SolveFingerprinting
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor

/**
 * Portable checkpoint envelope that contains no backend/native handle. / 不包含后端或 native 句柄的 checkpoint envelope。
 *
 * @property schemaVersion envelope schema 版本 / Envelope schema version
 * @property sourceFormat checkpoint 来源格式 / Source format of the checkpoint
 * @property migratedFromLegacy 是否由 legacy v1 迁移 / Whether this envelope was migrated from legacy v1
 * @property checkpointId checkpoint 标识 / Checkpoint identifier
 * @property identitySchemaVersion 身份 schema 版本 / Identity schema version
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property modelName 模型名称 / Model name
 * @property modelFingerprint 模型指纹 / Model fingerprint
 * @property configurationFingerprint 配置指纹 / Configuration fingerprint
 * @property solverFingerprint 求解器指纹 / Solver fingerprint
 * @property runId 运行标识 / Run identifier
 * @property attemptId 尝试标识 / Attempt identifier
 * @property parentCheckpointId 父 checkpoint 标识 / Parent checkpoint identifier
 * @property createdAtEpochMs 创建时间 / Creation time
 * @property snapshotJson snapshot JSON / Snapshot JSON
 * @property incumbent 已验证 incumbent / Validated incumbent
 * @property bestBound 历史 best bound / Historical best bound
 * @property gap 历史 gap / Historical gap
 * @property assumptions assumption 标识 / Assumption identifiers
 * @property conflicts 冲突证据 / Conflict evidence
 * @property benders Benders 状态 / Benders state
 * @property integritySha256 完整性摘要 / Integrity digest
 */
@Serializable
data class ConstraintProgrammingCheckpointEnvelope(
    val schemaVersion: String = "2.0",
    val sourceFormat: String = "v2",
    val migratedFromLegacy: Boolean = false,
    val checkpointId: String,
    val identitySchemaVersion: String,
    val identityNamespace: String,
    val modelName: String,
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

/**
 * Portable incumbent representation. / 可移植 incumbent 表示。
 *
 * @property valuesById 稳定变量值 / Stable variable values
 * @property intervalsById 稳定 interval 值 / Stable interval values
 * @property objective 目标值 / Objective value
 */
@Serializable
data class PortableConstraintProgrammingIncumbent(
    val valuesById: Map<String, Long> = emptyMap(),
    val intervalsById: Map<String, PortableConstraintProgrammingIntervalValue> = emptyMap(),
    val objective: String? = null
)

/**
 * Portable interval value. / 可移植 interval 值。
 *
 * @property start 开始值 / Start value
 * @property size 长度值 / Size value
 * @property end 结束值 / End value
 * @property present 存在标志 / Presence flag
 */
@Serializable
data class PortableConstraintProgrammingIntervalValue(
    val start: Long,
    val size: Long,
    val end: Long,
    val present: Boolean = true
)

/**
 * Portable conflict evidence. / 可移植冲突证据。
 *
 * @property validity 有效性 / Evidence validity
 * @property minimality 最小性 / Evidence minimality
 * @property memberIds 原始成员标识 / Original member identifiers
 * @property assumptionIds 参与冲突的 assumption 变量标识 / Assumption variable identifiers participating in the conflict
 * @property provenance 来源摘要 / Provenance summary
 */
@Serializable
data class PortableConstraintProgrammingConflict(
    /** Evidence validity classification. / 证据有效性分类。 */
    val validity: String,
    /** Evidence minimality classification. / 证据最小性分类。 */
    val minimality: String,
    /** Stable member IDs participating in the conflict. / 参与冲突的稳定成员标识。 */
    val memberIds: List<String> = emptyList(),
    /** Stable assumption variable IDs participating in the conflict. / 参与冲突的稳定 assumption 变量标识。 */
    val assumptionIds: List<String> = emptyList(),
    /** Sanitized provider evidence metadata. / 脱敏后的 provider 证据元数据。 */
    val provenance: Map<String, String> = emptyMap()
)

/**
 * Portable Benders state. / 可移植 Benders 状态。
 *
 * @property iteration 当前迭代 / Current iteration
 * @property masterIncumbent master incumbent / Master incumbent
 * @property masterBestBound master best bound / Master best bound
 * @property cuts 去重 cut 记录 / Deduplicated cut records
 * @property trace 迭代轨迹 / Iteration trace
 * @property assumptions 已验证的 assumption 标识 / Revalidated assumption identifiers
 * @property fixedBindings 已验证的 CP 固定绑定 / Revalidated CP fixed bindings
 * @property conflicts 已验证的冲突证据 / Revalidated conflict evidence
 * @property convergenceVerified exact 收敛证明 / Exact convergence proof
 * @property masterFingerprint 主问题模型指纹 / Master model fingerprint
 * @property subproblemModelFingerprint Benders 子问题模型指纹 / Benders subproblem model fingerprint
 */
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

/**
 * Versioned cut payload that excludes closures and native handles. / 排除闭包和 native 句柄的版本化 cut payload。
 *
 * @property id 稳定 cut 标识 / Stable cut identifier
 * @property schemaVersion cut payload schema 版本 / Cut payload schema version
 * @property validity 有效性 / Validity state
 * @property provenance 稳定来源 / Stable provenance
 * @property payload 基础类型 payload / Primitive payload
 */
@Serializable
data class PortableConstraintProgrammingCut(
    val id: String,
    val schemaVersion: String,
    val validity: String,
    val provenance: Map<String, String> = emptyMap(),
    val payload: Map<String, String> = emptyMap()
)

/**
 * Restored and verified checkpoint data. / 已恢复并复验的 checkpoint 数据。
 *
 * @property envelope 解码后的 envelope / Decoded envelope
 * @property incumbent 复验后的 incumbent / Revalidated incumbent
 * @property assumptions 已复验的 assumption 标识 / Revalidated assumption identifiers
 * @property conflicts 已复验的冲突证据 / Revalidated conflict evidence
 * @property bestBound 历史最佳界 / Historical best bound
 * @property gap 历史最优间隙 / Historical optimality gap
 */
data class ConstraintProgrammingCheckpointRestore(
    val envelope: ConstraintProgrammingCheckpointEnvelope,
    val incumbent: ConstraintProgrammingSolution?,
    val assumptions: List<String> = envelope.assumptions,
    val conflicts: List<PortableConstraintProgrammingConflict> = envelope.conflicts,
    val bestBound: String? = envelope.bestBound,
    val gap: String? = envelope.gap
)

/** Encoder and verifier for portable CP checkpoint v2. / portable CP checkpoint v2 的编码与复验器。 */
object ConstraintProgrammingCheckpointCodec {
    private const val CURRENT_SCHEMA = "2.0"
    private val V2_MARKER_FIELDS = setOf(
        "schemaVersion",
        "sourceFormat",
        "migratedFromLegacy",
        "checkpointId",
        "identitySchemaVersion",
        "identityNamespace",
        "modelFingerprint",
        "configurationFingerprint",
        "solverFingerprint",
        "runId",
        "attemptId",
        "parentCheckpointId",
        "createdAtEpochMs",
        "incumbent",
        "bestBound",
        "gap",
        "assumptions",
        "conflicts",
        "benders",
        "integritySha256"
    )
    private val LEGACY_V1_FIELDS = setOf(
        "schema",
        "modelName",
        "solverId",
        "snapshotJson"
    )
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    /**
     * Benders state shape used before masterFingerprint was added to V2.
     * masterFingerprint 加入 V2 前使用的 Benders 状态形状。
     */
    @Serializable
    private data class LegacyV2BendersState(
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

    /**
     * V2 envelope serializer retaining the published pre-masterFingerprint shape.
     * 保留 masterFingerprint 加入前已发布 V2 envelope 形状的序列化器。
     */
    @Serializable
    private data class LegacyV2Envelope(
        val schemaVersion: String = "2.0",
        val sourceFormat: String = "v2",
        val migratedFromLegacy: Boolean = false,
        val checkpointId: String,
        val identitySchemaVersion: String,
        val identityNamespace: String,
        val modelName: String,
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
        val benders: LegacyV2BendersState? = null,
        val integritySha256: String = ""
    )

    /**
     * Captures a v2 envelope from a snapshot and optional incumbent.
     * 从 snapshot 和可选 incumbent 捕获 v2 envelope。
     *
     * @param snapshot CP 模型 snapshot / CP model snapshot
     * @param descriptor 求解器描述符 / Solver descriptor
     * @param checkpointId checkpoint 标识 / Checkpoint identifier
     * @param createdAtEpochMs 创建时间 / Creation time
     * @param incumbent 可选 incumbent / Optional incumbent
     * @param configurationFingerprint 配置指纹 / Configuration fingerprint
     * @param runId 运行标识 / Run identifier
     * @param attemptId 尝试标识 / Attempt identifier
     * @param parentCheckpointId 父 checkpoint 标识 / Parent checkpoint identifier
     * @param bestBound 历史最佳界 / Historical best bound
     * @param gap 历史最优间隙 / Historical optimality gap
     * @param assumptions 已激活的 assumption 变量 ID / Activated assumption variable IDs
     * @param conflicts 冲突证据 / Conflict evidence
     * @param benders 可移植 Benders 状态 / Portable Benders state
     * @return envelope 或结构化错误 / Envelope or structured error
     */
    fun capture(
        snapshot: ConstraintProgrammingModelSnapshot,
        descriptor: SolverDescriptor,
        checkpointId: String,
        createdAtEpochMs: Long,
        incumbent: ConstraintProgrammingSolution? = null,
        configurationFingerprint: String? = null,
        runId: String? = null,
        attemptId: String? = null,
        parentCheckpointId: String? = null,
        bestBound: String? = null,
        gap: String? = null,
        assumptions: List<String> = emptyList(),
        conflicts: List<PortableConstraintProgrammingConflict> = emptyList(),
        benders: PortableConstraintProgrammingBendersState? = null
    ): Ret<ConstraintProgrammingCheckpointEnvelope> {
        val encoded = ConstraintProgrammingSnapshotCodec.encode(snapshot)
        if (encoded.failed) {
            return propagate(encoded)
        }
        val portableIncumbent = incumbent?.toPortable(snapshot)
        if (portableIncumbent != null && portableIncumbent.failed) {
            return propagate(portableIncumbent)
        }
        validateBendersState(benders)?.let { return Failed(ErrorCode.IllegalArgument, it) }
        val envelope = ConstraintProgrammingCheckpointEnvelope(
            checkpointId = checkpointId,
            identitySchemaVersion = snapshot.identitySchemaVersion,
            identityNamespace = snapshot.identityNamespace,
            modelName = snapshot.name,
            modelFingerprint = SolveFingerprinting.sha256(encoded.value!!).value,
            configurationFingerprint = configurationFingerprint,
            solverFingerprint = SolveFingerprinting.sha256(
                "${descriptor.solverId}|${descriptor.backendName}|${descriptor.backendVersion ?: ""}"
            ).value,
            runId = runId,
            attemptId = attemptId,
            parentCheckpointId = parentCheckpointId,
            createdAtEpochMs = createdAtEpochMs,
            snapshotJson = encoded.value!!,
            incumbent = portableIncumbent?.value,
            bestBound = bestBound,
            gap = gap,
            assumptions = assumptions,
            conflicts = conflicts,
            benders = benders
        )
        val normalized = withDigest(envelope)
        return when (val restored = restore(
            envelope = normalized,
            snapshot = snapshot,
            expectedConfigurationFingerprint = configurationFingerprint,
            expectedSolverFingerprint = normalized.solverFingerprint,
            allowLegacyConfigurationFingerprint = configurationFingerprint.isNullOrBlank()
        )) {
            is Ok -> ok(normalized)
            is Failed -> Failed(restored.error)
            is Fatal -> Fatal(restored.errors)
        }
    }

    /**
     * Encodes an envelope and computes its integrity digest.
     * 编码 envelope 并计算完整性摘要。
     *
     * @param envelope checkpoint envelope / checkpoint envelope
     * @return JSON 文本 / JSON text
     */
    fun encode(envelope: ConstraintProgrammingCheckpointEnvelope): Ret<String> {
        return try {
            val canonical = canonicalizeSnapshotEnvelope(envelope)
            if (canonical.failed) {
                return propagate(canonical)
            }
            val canonicalEnvelope = canonical.value!!
            // Encoding always emits the current v2 source marker. Legacy envelopes are read-only
            // migration values and must become ordinary v2 documents when persisted again. /
            // 编码始终输出当前 v2 来源标记；legacy envelope 仅作为迁移输入，再次持久化时必须成为普通 v2 文档。
            val migratedFromLegacy = canonicalEnvelope.sourceFormat == "legacy-v1" || canonicalEnvelope.migratedFromLegacy
            val normalized = withDigest(
                canonicalEnvelope.copy(
                    sourceFormat = "v2",
                    migratedFromLegacy = migratedFromLegacy,
                    checkpointId = if (migratedFromLegacy) "legacy-v1-migrated" else canonicalEnvelope.checkpointId
                )
            )
            ok(json.encodeToString(ConstraintProgrammingCheckpointEnvelope.serializer(), normalized))
        } catch (error: Throwable) {
            Failed(ErrorCode.Other, "CP checkpoint 编码失败：${error.message} / CP checkpoint encoding failed: ${error.message}")
        }
    }

    /**
     * Decodes and verifies a v2 envelope.
     * 解码并复验 v2 envelope。
     *
     * @param encoded JSON 文本 / JSON text
     * @return envelope 或结构化错误 / Envelope or structured error
     */
    fun decode(encoded: String): Ret<ConstraintProgrammingCheckpointEnvelope> {
        return try {
            val envelope = json.decodeFromString(ConstraintProgrammingCheckpointEnvelope.serializer(), encoded)
            if (envelope.schemaVersion != CURRENT_SCHEMA) {
                return Failed(ErrorCode.Other, "不支持的 checkpoint schema：${envelope.schemaVersion} / Unsupported checkpoint schema: ${envelope.schemaVersion}")
            }
            if (envelope.sourceFormat != "v2") {
                return Failed(ErrorCode.IllegalArgument, "checkpoint 来源格式无效 / Checkpoint source format is invalid")
            }
            if (!envelope.migratedFromLegacy && envelope.checkpointId == "legacy-v1") {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "v2 checkpoint 不能伪装为 legacy-v1 / A v2 checkpoint must not masquerade as legacy-v1"
                )
            }
            if (envelope.migratedFromLegacy && envelope.checkpointId != "legacy-v1-migrated") {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "legacy 迁移 checkpoint 标识无效 / Migrated legacy checkpoint identifier is invalid"
                )
            }
            if (envelope.integritySha256 != digest(envelope.copy(integritySha256 = ""))) {
                return Failed(ErrorCode.IllegalArgument, "checkpoint 完整性摘要不匹配 / Checkpoint integrity digest mismatch")
            }
            if (SolveFingerprinting.sha256(envelope.snapshotJson).value != envelope.modelFingerprint) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "checkpoint snapshot 与模型指纹不一致 / Checkpoint snapshot disagrees with model fingerprint"
                )
            }
            ConstraintProgrammingSnapshotCodec.validate(envelope.snapshotJson).map { envelope }
        } catch (error: Throwable) {
            Failed(ErrorCode.IllegalArgument, "CP checkpoint 解码失败：${error.message} / CP checkpoint decoding failed: ${error.message}")
        }
    }

    /**
     * Decodes v2 or the legacy v1 snapshot-only checkpoint format.
     * 解码 v2 或 legacy v1 仅 snapshot checkpoint 格式。
     *
     * @param encoded checkpoint JSON / Checkpoint JSON
     * @return verified v2-compatible envelope or structured error / 已验证的 v2 兼容 envelope 或结构化错误
     */
    fun decodeCompatible(encoded: String): Ret<ConstraintProgrammingCheckpointEnvelope> {
        val root = try {
            json.parseToJsonElement(encoded).jsonObject
        } catch (error: Throwable) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint JSON 无效：${error.message} / Checkpoint JSON is invalid: ${error.message}"
            )
        }
        val current = decode(encoded)
        if (!current.failed) {
            return canonicalizeSnapshotEnvelope(current.value!!)
        }
        decodeLegacyV2(encoded)?.let { return ok(it) }
        // A document carrying any v2 marker is never eligible for legacy fallback. /
        // 含有任一 v2 标记的文档绝不能降级为 legacy。
        if (root.keys.any { it in V2_MARKER_FIELDS }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "损坏的 v2 checkpoint 不得降级为 legacy / A malformed v2 checkpoint must not downgrade to legacy"
            )
        }
        if (root.keys.any { it !in LEGACY_V1_FIELDS }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "legacy checkpoint 包含未知字段 / Legacy checkpoint contains unknown fields"
            )
        }
        return try {
            if (root["schema"]?.jsonPrimitive?.intOrNull != 1) {
                return Failed(ErrorCode.IllegalArgument, "不支持的 checkpoint 格式 / Unsupported checkpoint format")
            }
            val snapshotJson = root["snapshotJson"]?.jsonPrimitive?.content
                ?: return Failed(ErrorCode.IllegalArgument, "legacy checkpoint 缺少 snapshotJson / Legacy checkpoint is missing snapshotJson")
            val canonical = ConstraintProgrammingSnapshotCodec.canonicalize(snapshotJson)
            if (canonical.failed) {
                return Failed(ErrorCode.IllegalArgument, "legacy checkpoint snapshot 无效 / Legacy checkpoint snapshot is invalid")
            }
            val canonicalSnapshotJson = canonical.value!!
            val snapshotRoot = json.parseToJsonElement(canonicalSnapshotJson).jsonObject
            ok(
                withDigest(
                    ConstraintProgrammingCheckpointEnvelope(
                        checkpointId = "legacy-v1",
                        sourceFormat = "legacy-v1",
                        migratedFromLegacy = false,
                        identitySchemaVersion = snapshotRoot["identitySchemaVersion"]?.jsonPrimitive?.content ?: "1.0",
                        identityNamespace = snapshotRoot["identityNamespace"]?.jsonPrimitive?.content ?: "model-local",
                        modelName = root["modelName"]?.jsonPrimitive?.content
                            ?: snapshotRoot["name"]?.jsonPrimitive?.content
                            ?: "legacy",
                        modelFingerprint = SolveFingerprinting.sha256(canonicalSnapshotJson).value,
                        // Legacy v1 did not define the canonical solver fingerprint. / Legacy v1 没有定义规范化求解器指纹。
                        solverFingerprint = null,
                        createdAtEpochMs = 0L,
                        snapshotJson = canonicalSnapshotJson
                    )
                )
            )
        } catch (error: Throwable) {
            Failed(ErrorCode.IllegalArgument, "legacy checkpoint 解码失败：${error.message} / Legacy checkpoint decoding failed: ${error.message}")
        }
    }

    private fun canonicalizeSnapshotEnvelope(
        envelope: ConstraintProgrammingCheckpointEnvelope
    ): Ret<ConstraintProgrammingCheckpointEnvelope> {
        val canonical = ConstraintProgrammingSnapshotCodec.canonicalize(envelope.snapshotJson)
        if (canonical.failed) {
            return propagate(canonical)
        }
        val snapshotJson = canonical.value!!
        return try {
            val snapshotRoot = json.parseToJsonElement(snapshotJson).jsonObject
            val normalized = envelope.copy(
                identitySchemaVersion = snapshotRoot["identitySchemaVersion"]?.jsonPrimitive?.content
                    ?: envelope.identitySchemaVersion,
                identityNamespace = snapshotRoot["identityNamespace"]?.jsonPrimitive?.content
                    ?: envelope.identityNamespace,
                modelName = snapshotRoot["name"]?.jsonPrimitive?.content ?: envelope.modelName,
                modelFingerprint = SolveFingerprinting.sha256(snapshotJson).value,
                snapshotJson = snapshotJson
            )
            if (normalized == envelope) {
                ok(envelope)
            } else {
                ok(withDigest(normalized))
            }
        } catch (error: Throwable) {
            Failed(
                ErrorCode.IllegalArgument,
                "checkpoint snapshot 迁移失败：${error.message} / Checkpoint snapshot migration failed: ${error.message}"
            )
        }
    }

    private fun decodeLegacyV2(encoded: String): ConstraintProgrammingCheckpointEnvelope? {
        return runCatching {
            val legacy = json.decodeFromString(LegacyV2Envelope.serializer(), encoded)
            val canonical = ConstraintProgrammingSnapshotCodec.canonicalize(legacy.snapshotJson)
            if (canonical.failed) {
                return@runCatching null
            }
            val canonicalSnapshotJson = canonical.value!!
            val snapshotRoot = json.parseToJsonElement(canonicalSnapshotJson).jsonObject
            if (legacy.schemaVersion != "2.0" || legacy.sourceFormat != "v2" ||
                (!legacy.migratedFromLegacy && legacy.checkpointId == "legacy-v1") ||
                (legacy.migratedFromLegacy && legacy.checkpointId != "legacy-v1-migrated") ||
                SolveFingerprinting.sha256(legacy.snapshotJson).value != legacy.modelFingerprint ||
                legacy.integritySha256.isBlank() ||
                legacy.integritySha256 != digest(legacy.copy(integritySha256 = ""))
            ) {
                return@runCatching null
            }
            withDigest(ConstraintProgrammingCheckpointEnvelope(
                schemaVersion = legacy.schemaVersion,
                sourceFormat = legacy.sourceFormat,
                migratedFromLegacy = legacy.migratedFromLegacy,
                checkpointId = legacy.checkpointId,
                identitySchemaVersion = snapshotRoot["identitySchemaVersion"]?.jsonPrimitive?.content
                    ?: legacy.identitySchemaVersion,
                identityNamespace = snapshotRoot["identityNamespace"]?.jsonPrimitive?.content
                    ?: legacy.identityNamespace,
                modelName = snapshotRoot["name"]?.jsonPrimitive?.content ?: legacy.modelName,
                modelFingerprint = SolveFingerprinting.sha256(canonicalSnapshotJson).value,
                configurationFingerprint = legacy.configurationFingerprint,
                solverFingerprint = legacy.solverFingerprint,
                runId = legacy.runId,
                attemptId = legacy.attemptId,
                parentCheckpointId = legacy.parentCheckpointId,
                createdAtEpochMs = legacy.createdAtEpochMs,
                snapshotJson = canonicalSnapshotJson,
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
                        masterFingerprint = null,
                        subproblemModelFingerprint = state.subproblemModelFingerprint
                    )
                },
                integritySha256 = legacy.integritySha256
            ))
        }.getOrNull()
    }

    /**
     * Normalizes an envelope and refreshes its integrity digest.
     * 规范化 envelope 并刷新完整性摘要。
     *
     * @param envelope checkpoint envelope / checkpoint envelope
     * @return 带有效摘要的 envelope / envelope with a valid integrity digest
     */
    fun withIntegrity(envelope: ConstraintProgrammingCheckpointEnvelope): ConstraintProgrammingCheckpointEnvelope {
        return withDigest(envelope)
    }

    /**
     * Revalidates an incumbent against the supplied snapshot.
     * 根据给定 snapshot 重新校验 incumbent。
     *
     * @param envelope checkpoint envelope / checkpoint envelope
     * @param snapshot 当前模型 snapshot / Current model snapshot
     * @param expectedConfigurationFingerprint 当前生效配置指纹 / Fingerprint of the effective active configuration
     * @param expectedSolverFingerprint 当前求解器指纹 / Fingerprint of the active solver runtime
     * @param allowLegacyConfigurationFingerprint 是否允许调用方已独立复验的旧配置指纹 / Whether to allow a legacy configuration fingerprint independently validated by the caller
     * @param allowLegacySolverFingerprint 是否允许调用方已独立复验的旧求解器指纹 / Whether to allow a legacy solver fingerprint independently validated by the caller
     * @return 恢复结果 / Restore result
     */
    fun restore(
        envelope: ConstraintProgrammingCheckpointEnvelope,
        snapshot: ConstraintProgrammingModelSnapshot,
        expectedConfigurationFingerprint: String?,
        expectedSolverFingerprint: String?,
        allowLegacyConfigurationFingerprint: Boolean = false,
        allowLegacySolverFingerprint: Boolean = false
    ): Ret<ConstraintProgrammingCheckpointRestore> {
        if (envelope.sourceFormat !in setOf("v2", "legacy-v1")) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint 来源格式未知 / Checkpoint source format is unknown"
            )
        }
        if (envelope.sourceFormat == "v2" && envelope.schemaVersion != CURRENT_SCHEMA) {
            return Failed(
                ErrorCode.Other,
                "不支持的 checkpoint schema：${envelope.schemaVersion} / Unsupported checkpoint schema: ${envelope.schemaVersion}"
            )
        }
        if (envelope.sourceFormat == "legacy-v1" && envelope.checkpointId != "legacy-v1") {
            return Failed(
                ErrorCode.IllegalArgument,
                "legacy checkpoint 来源标识无效 / Legacy checkpoint source identifier is invalid"
            )
        }
        if (envelope.sourceFormat == "v2" && envelope.checkpointId == "legacy-v1") {
            return Failed(
                ErrorCode.IllegalArgument,
                "v2 checkpoint 不能伪装为 legacy-v1 / A v2 checkpoint must not masquerade as legacy-v1"
            )
        }
        val migratedLegacyV2 = envelope.sourceFormat == "v2" && envelope.migratedFromLegacy
        if (migratedLegacyV2 && envelope.checkpointId != "legacy-v1-migrated") {
            return Failed(
                ErrorCode.IllegalArgument,
                "legacy 迁移 checkpoint 标识无效 / Migrated legacy checkpoint identifier is invalid"
            )
        }
        if (envelope.sourceFormat == "v2") {
            if (!migratedLegacyV2 &&
                ((!allowLegacyConfigurationFingerprint && expectedConfigurationFingerprint.isNullOrBlank()) ||
                    (!allowLegacySolverFingerprint && expectedSolverFingerprint.isNullOrBlank()))
            ) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "恢复 v2 checkpoint 必须提供当前配置和求解器指纹 / Current configuration and solver fingerprints are required to restore a V2 checkpoint"
                )
            }
            if ((!migratedLegacyV2 && envelope.solverFingerprint.isNullOrBlank()) ||
                (!migratedLegacyV2 && !allowLegacySolverFingerprint && expectedSolverFingerprint.isNullOrBlank()) ||
                (!allowLegacySolverFingerprint && !migratedLegacyV2 && envelope.solverFingerprint != expectedSolverFingerprint)
            ) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "checkpoint 求解器指纹缺失或不匹配 / Checkpoint solver fingerprint is missing or mismatched"
                )
            }
            if ((!migratedLegacyV2 && !allowLegacyConfigurationFingerprint && expectedConfigurationFingerprint.isNullOrBlank()) ||
                (!allowLegacyConfigurationFingerprint && !migratedLegacyV2 &&
                    envelope.configurationFingerprint != expectedConfigurationFingerprint)
            ) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "checkpoint 配置指纹缺失或不匹配 / Checkpoint configuration fingerprint is missing or mismatched"
                )
            }
            if (!migratedLegacyV2 && !allowLegacyConfigurationFingerprint &&
                (envelope.bestBound != null || envelope.gap != null) &&
                expectedConfigurationFingerprint.isNullOrBlank()
            ) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "恢复历史界限前必须提供当前配置指纹 / Current configuration fingerprint is required before restoring historical bounds"
                )
            }
        }
        if (envelope.integritySha256.isBlank() ||
            envelope.integritySha256 != digest(envelope.copy(integritySha256 = ""))
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint 完整性摘要无效 / Checkpoint integrity digest is invalid"
            )
        }
        if (SolveFingerprinting.sha256(envelope.snapshotJson).value != envelope.modelFingerprint) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint snapshot 与原始模型指纹不一致 / Checkpoint snapshot disagrees with its raw model fingerprint"
            )
        }
        val canonicalEnvelope = canonicalizeSnapshotEnvelope(envelope)
        if (canonicalEnvelope.failed) return propagate(canonicalEnvelope)
        val normalizedEnvelope = canonicalEnvelope.value!!
        val encoded = ConstraintProgrammingSnapshotCodec.encode(snapshot)
        if (encoded.failed) return propagate(encoded)
        val fingerprint = SolveFingerprinting.sha256(encoded.value!!).value
        if (fingerprint != normalizedEnvelope.modelFingerprint ||
            snapshot.identitySchemaVersion != normalizedEnvelope.identitySchemaVersion ||
            snapshot.identityNamespace != normalizedEnvelope.identityNamespace
        ) {
            return Failed(ErrorCode.IllegalArgument, "checkpoint 与模型身份或指纹不匹配 / Checkpoint model identity or fingerprint mismatch")
        }
        if (normalizedEnvelope.snapshotJson != encoded.value!!) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint 内嵌 snapshot 与当前模型不一致 / Checkpoint embedded snapshot disagrees with the current model"
            )
        }
        val variableIds = snapshot.variables.mapTo(linkedSetOf()) { it.id.value }
        val constraintIds = snapshot.constraints.mapTo(linkedSetOf()) { it.id.value }
        if (normalizedEnvelope.assumptions.any { it !in variableIds }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint assumption 引用了未知变量 / Checkpoint assumption references an unknown variable"
            )
        }
        for (conflict in normalizedEnvelope.conflicts) {
            if (conflict.validity !in setOf("Verified", "Heuristic", "Unknown") ||
                conflict.minimality !in setOf("Irreducible", "Partial", "NotChecked")
            ) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "checkpoint conflict 证据枚举无效 / Checkpoint conflict evidence enum is invalid"
                )
            }
            if (conflict.validity == "Verified" && conflict.memberIds.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "已验证 checkpoint conflict 不能没有成员 / A verified checkpoint conflict must contain members"
                )
            }
            if (conflict.assumptionIds.any { it !in variableIds }) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "checkpoint conflict 引用了未知 assumption / Checkpoint conflict references an unknown assumption"
                )
            }
            for (member in conflict.memberIds) {
                when {
                    member.startsWith("constraint:") -> {
                        if (member.removePrefix("constraint:") !in constraintIds) {
                            return Failed(
                                ErrorCode.IllegalArgument,
                                "checkpoint conflict 引用了未知约束 / Checkpoint conflict references an unknown constraint"
                            )
                        }
                    }
                    member.startsWith("domain:") -> {
                        if (member.removePrefix("domain:") !in variableIds) {
                            return Failed(
                                ErrorCode.IllegalArgument,
                                "checkpoint conflict 引用了未知变量域 / Checkpoint conflict references an unknown variable domain"
                            )
                        }
                    }
                    member.startsWith("bound:") -> {
                        val encodedMember = member.removePrefix("bound:")
                        val separator = encodedMember.lastIndexOf(':')
                        val variableId = if (separator > 0) encodedMember.substring(0, separator) else ""
                        val side = if (separator > 0) encodedMember.substring(separator + 1) else ""
                        if (variableId !in variableIds || side !in setOf("Lower", "Upper")) {
                            return Failed(
                                ErrorCode.IllegalArgument,
                                "checkpoint conflict 边界成员无效 / Checkpoint conflict bound member is invalid"
                            )
                        }
                    }
                    else -> {
                        return Failed(
                            ErrorCode.IllegalArgument,
                            "checkpoint conflict 成员类型未知 / Checkpoint conflict member type is unknown"
                        )
                    }
                }
            }
        }
        validateBendersState(normalizedEnvelope.benders)?.let { return Failed(ErrorCode.IllegalArgument, it) }
        val incumbent = normalizedEnvelope.incumbent?.toSolution(snapshot)
        if (incumbent != null && incumbent.failed) {
            return propagate(incumbent)
        }
        val restoreBestBound = normalizedEnvelope.bestBound.takeUnless { allowLegacyConfigurationFingerprint || migratedLegacyV2 }
        val restoreGap = normalizedEnvelope.gap.takeUnless { allowLegacyConfigurationFingerprint || migratedLegacyV2 }
        return ok(
            ConstraintProgrammingCheckpointRestore(
                envelope = normalizedEnvelope,
                incumbent = incumbent?.value,
                bestBound = restoreBestBound,
                gap = restoreGap
            )
        )
    }

    private fun validateBendersState(state: PortableConstraintProgrammingBendersState?): String? {
        if (state == null) {
            return null
        }
        if (state.iteration < 0L) {
            return "Benders checkpoint iteration 不能为负 / Benders checkpoint iteration must not be negative"
        }
        val hasMasterState = state.masterIncumbent != null ||
            state.masterBestBound != null ||
            state.assumptions.isNotEmpty() ||
            state.fixedBindings.isNotEmpty() ||
            state.conflicts.isNotEmpty() ||
            state.convergenceVerified
        if (hasMasterState && state.masterFingerprint.isNullOrBlank()) {
            return "Benders 主问题状态缺少模型指纹 / Benders master state is missing a model fingerprint"
        }
        if (state.masterFingerprint?.isBlank() == true || state.subproblemModelFingerprint?.isBlank() == true) {
            return "Benders checkpoint 模型指纹不能为空 / Benders checkpoint model fingerprints must not be blank"
        }
        return null
    }

    private fun withDigest(envelope: ConstraintProgrammingCheckpointEnvelope): ConstraintProgrammingCheckpointEnvelope {
        return envelope.copy(
            schemaVersion = CURRENT_SCHEMA,
            sourceFormat = envelope.sourceFormat,
            integritySha256 = digest(envelope.copy(schemaVersion = CURRENT_SCHEMA, integritySha256 = ""))
        )
    }

    private fun digest(envelope: ConstraintProgrammingCheckpointEnvelope): String {
        val content = json.encodeToString(ConstraintProgrammingCheckpointEnvelope.serializer(), envelope)
        return MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun digest(envelope: LegacyV2Envelope): String {
        val content = json.encodeToString(LegacyV2Envelope.serializer(), envelope)
        return MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun ConstraintProgrammingSolution.toPortable(
        snapshot: ConstraintProgrammingModelSnapshot
    ): Ret<PortableConstraintProgrammingIncumbent> {
        val expectedIds = snapshot.variables.map { it.id }.toSet()
        if (values.keys != expectedIds) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint incumbent 变量集合不匹配 / Checkpoint incumbent variable set mismatch"
            )
        }
        if (values.any { (id, value) -> snapshot.variable(id)?.domain?.contains(value) != true }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint incumbent 超出变量值域 / Checkpoint incumbent is outside a variable domain"
            )
        }
        for (constraint in snapshot.constraints) {
            val satisfied = constraint.constraint.isSatisfied(values)
            if (satisfied.failed) return propagate(satisfied)
            if (satisfied.value != true) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "checkpoint incumbent 违反约束：${constraint.id.value} / " +
                        "Checkpoint incumbent violates constraint: ${constraint.id.value}"
                )
            }
        }
        val evaluatedIntervals = linkedMapOf<fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId, IntervalValue>()
        for (interval in snapshot.intervals) {
            val evaluated = interval.evaluate(values)
            if (evaluated.failed) return propagate(evaluated)
            val value = evaluated.value!!
            val supplied = intervals[interval.id]
            if (supplied != null && supplied != value) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "checkpoint incumbent interval 与 snapshot 求值不一致 / " +
                        "Checkpoint incumbent interval disagrees with snapshot evaluation"
                )
            }
            evaluatedIntervals[interval.id] = value
        }
        if (intervals.keys.any { it !in evaluatedIntervals.keys }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint incumbent 包含未知 interval / Checkpoint incumbent contains an unknown interval"
            )
        }
        val objective = snapshot.objectives.firstOrNull()?.expression?.let { expression ->
            val evaluated = expression.evaluate(values)
            if (evaluated.failed) return propagate(evaluated)
            evaluated.value!!.toString()
        }
        return ok(PortableConstraintProgrammingIncumbent(
            valuesById = values.mapKeys { it.key.value }.mapValues { it.value.toLong() },
            intervalsById = evaluatedIntervals.mapKeys { it.key.value }.mapValues {
                PortableConstraintProgrammingIntervalValue(
                    start = it.value.start.toLong(),
                    size = it.value.size.toLong(),
                    end = it.value.end.toLong(),
                    present = it.value.present
                )
            },
            objective = objective
        ))
    }

    private fun PortableConstraintProgrammingIncumbent.toSolution(
        snapshot: ConstraintProgrammingModelSnapshot
    ): Ret<ConstraintProgrammingSolution> {
        val expectedIds = snapshot.variables.map { it.id.value }.toSet()
        if (valuesById.keys != expectedIds) {
            return Failed(ErrorCode.IllegalArgument, "checkpoint incumbent 变量集合不匹配 / Checkpoint incumbent variable set mismatch")
        }
        val values = valuesById.mapKeys { fuookami.ospf.kotlin.core.solver.report.VariableId(it.key) }
            .mapValues { Int64(it.value) }
        if (values.any { (id, value) -> snapshot.variable(id)?.domain?.contains(value) != true }) {
            return Failed(ErrorCode.IllegalArgument, "checkpoint incumbent 超出变量值域 / Checkpoint incumbent is outside a variable domain")
        }
        for (constraint in snapshot.constraints) {
            val satisfied = constraint.constraint.isSatisfied(values)
            if (satisfied.failed || satisfied.value != true) {
                return Failed(ErrorCode.IllegalArgument, "checkpoint incumbent 违反约束：${constraint.id.value} / Checkpoint incumbent violates constraint: ${constraint.id.value}")
            }
        }
        val objective = snapshot.objectives.firstOrNull()?.expression
        if (objective != null) {
            val evaluated = objective.evaluate(values)
            if (evaluated.failed) {
                return propagate(evaluated)
            }
            if (this.objective != null && this.objective != evaluated.value!!.toString()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "checkpoint incumbent 目标值不一致 / Checkpoint incumbent objective is inconsistent"
                )
            }
        } else if (this.objective != null) {
            return Failed(
                ErrorCode.IllegalArgument,
                "无目标模型不应包含 checkpoint 目标值 / A model without objectives must not contain a checkpoint objective"
            )
        }
        val intervals = linkedMapOf<fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId, IntervalValue>()
        val expectedIntervalIds = snapshot.intervals.map { it.id.value }.toSet()
        if (intervalsById.keys != expectedIntervalIds) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint incumbent interval 集合不匹配 / Checkpoint incumbent interval set mismatch"
            )
        }
        for (interval in snapshot.intervals) {
            val evaluated = interval.evaluate(values)
            if (evaluated.failed) return propagate(evaluated)
            val evaluatedValue = evaluated.value!!
            val serialized = intervalsById[interval.id.value]!!
            if (serialized.start != evaluatedValue.start.toLong() ||
                serialized.size != evaluatedValue.size.toLong() ||
                serialized.end != evaluatedValue.end.toLong() ||
                serialized.present != evaluatedValue.present
            ) {
                return Failed(ErrorCode.IllegalArgument, "checkpoint interval 与 snapshot 求值不一致 / Checkpoint interval disagrees with snapshot evaluation")
            }
            intervals[interval.id] = evaluatedValue
        }
        return ok(ConstraintProgrammingSolution(values = values, intervals = intervals))
    }
}

private fun <T> propagate(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "CP checkpoint 结果状态无效 / Invalid CP checkpoint result state")
    }
}
