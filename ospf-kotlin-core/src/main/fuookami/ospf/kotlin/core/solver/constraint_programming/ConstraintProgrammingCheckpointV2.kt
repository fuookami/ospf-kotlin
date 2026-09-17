/** Portable CP checkpoint envelope (schema 3.0). / 可移植 CP checkpoint envelope（schema 3.0）。 */
package fuookami.ospf.kotlin.core.solver.constraint_programming

import java.security.MessageDigest
import java.time.Instant
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
import fuookami.ospf.kotlin.core.solver.report.CancellationRecord
import fuookami.ospf.kotlin.core.solver.report.CancellationSource
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
 * @property cancellationChain 取消事实链，按发生顺序排列 / Cancellation-fact chain in occurrence order
 * @property provenance 求解器执行来源 / Solver execution provenance
 * @property integritySha256 完整性摘要 / Integrity digest
 *
 * 字段名、顺序与可空性由 `analysis-fixtures/checkpoint-wire-contract.tsv` 的 `[envelope-field]`
 * 段规定，两侧的声明顺序必须逐位一致：任何偏离都会使跨语言摘要失配。
 * Field names, order, and nullability are fixed by the `[envelope-field]` section of
 * `analysis-fixtures/checkpoint-wire-contract.tsv`, and both sides must declare them in exactly that
 * order: any drift breaks the cross-language digest.
 */
@Serializable
data class ConstraintProgrammingCheckpointEnvelope(
    val schemaVersion: String = "3.0",
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
    val cancellationChain: List<PortableCancellationRecord> = emptyList(),
    val provenance: PortableSolverProvenance? = null,
    val metadata: Map<String, String> = emptyMap(),
    val integritySha256: String = ""
)

/**
 * 取消事实的线格式。 / Wire shape of one cancellation fact.
 *
 * 字段名、顺序与可空性由契约 `[cancellation-record]` 段规定。线格式只承载**规范代码**字符串，
 * 因此任一侧都能无损往返另一侧的取值；无法识别的代码必须落到该侧的兜底变体并原样保留代码文本。
 * Field names, order, and nullability come from the contract's `[cancellation-record]` section. The
 * wire carries only the **canonical code** as a string, so either side round-trips the other side's
 * values losslessly: an unrecognized code must land in that side's catch-all variant and keep the
 * code text verbatim.
 *
 * @property origin 取消来源的规范代码 / Canonical cancellation-origin code
 * @property requestedAtEpochMs 取消请求时间戳（epoch 毫秒） / Cancellation request timestamp in epoch milliseconds
 * @property reason 取消原因 / Cancellation reason
 */
@Serializable
data class PortableCancellationRecord(
    val origin: String,
    val requestedAtEpochMs: Long,
    val reason: String? = null
)

/**
 * 求解器执行来源的线格式。 / Wire shape of the solver execution provenance.
 *
 * 字段名、顺序与可空性由契约 `[provenance-field]` 段规定；三个 string-map 必须按键升序输出，
 * 以保证摘要可跨语言复算。
 * Field names, order, and nullability come from the contract's `[provenance-field]` section; the
 * three string-maps must be emitted in ascending key order so the digest stays cross-language
 * recomputable.
 *
 * @property solverId 求解器标识 / Solver identifier
 * @property backendName 后端名称 / Backend name
 * @property backendVersion 后端版本 / Backend version
 * @property pluginVersion 插件版本 / Plugin version
 * @property requestedConfiguration 请求的配置参数 / Requested configuration parameters
 * @property effectiveConfiguration 实际生效的配置参数 / Effective configuration parameters
 * @property threadCount 线程数 / Thread count
 * @property randomSeed 随机种子 / Random seed
 * @property deterministic 是否确定性执行 / Whether execution is deterministic
 * @property environmentSummary 脱敏环境摘要 / Redacted environment summary
 */
@Serializable
data class PortableSolverProvenance(
    val solverId: String,
    val backendName: String,
    val backendVersion: String? = null,
    val pluginVersion: String? = null,
    val requestedConfiguration: Map<String, String> = emptyMap(),
    val effectiveConfiguration: Map<String, String> = emptyMap(),
    val threadCount: Int? = null,
    val randomSeed: Long? = null,
    val deterministic: Boolean? = null,
    val environmentSummary: Map<String, String> = emptyMap()
)

/**
 * 将取消事实投影为线格式，并保留原始来源代码文本。 /
 * Project a cancellation fact onto the wire shape, preserving the original origin code text.
 *
 * 当 [CancellationRecord.source] 为 [CancellationSource.Other] 时，收到的原始代码（例如 Rust 的
 * `backend`）会被写入 [CancellationRecord.wireOrigin]，因此再序列化回去仍然是同一个代码。
 * When [CancellationRecord.source] is [CancellationSource.Other], the code that was received (for
 * instance Rust's `backend`) is written into [CancellationRecord.wireOrigin], so re-serializing it
 * still yields the same code.
 *
 * @return 线格式取消记录 / Wire-format cancellation record
 */
fun CancellationRecord.toPortableCancellationRecord(): PortableCancellationRecord {
    return PortableCancellationRecord(
        origin = wireOrigin ?: source.toWireCode(),
        requestedAtEpochMs = requestedAt.toEpochMilli(),
        reason = reason
    )
}

/**
 * 将线格式取消记录解析回取消事实。 / Parse a wire-format cancellation record back into a cancellation fact.
 *
 * 无法识别的代码落到 [CancellationSource.Other]，并把代码文本原样保存在
 * [CancellationRecord.wireOrigin]，从而保证无损往返。
 * An unrecognized code lands in [CancellationSource.Other] and keeps its text verbatim in
 * [CancellationRecord.wireOrigin], which makes the round trip lossless.
 *
 * @return 取消事实 / Cancellation fact
 */
fun PortableCancellationRecord.toCancellationRecord(): CancellationRecord {
    val source = CancellationSource.fromWireCode(origin)
    return CancellationRecord(
        source = source,
        requestedAt = Instant.ofEpochMilli(requestedAtEpochMs),
        reason = reason,
        // 兜底变体必须原样保留收到的代码文本；专用变体的代码可由枚举复现，无需保留。
        // A catch-all variant must keep the received code text verbatim; a dedicated variant's code is
        // reproducible from the enum and needs no raw text.
        wireOrigin = origin.takeIf { source == CancellationSource.Other }
    )
}

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

/** Encoder and verifier for the portable CP checkpoint envelope. / 可移植 CP checkpoint envelope 的编码与复验器。 */
object ConstraintProgrammingCheckpointCodec {
    private const val CURRENT_SCHEMA = "3.0"
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
        "cancellationChain",
        "provenance",
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
     * Captures a schema 3.0 envelope from a snapshot and optional incumbent.
     * 从 snapshot 和可选 incumbent 捕获 schema 3.0 envelope。
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
     * @param cancellationChain 取消事实链，按发生顺序 / Cancellation-fact chain in occurrence order
     * @param provenance 求解器执行来源 / Solver execution provenance
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
        benders: PortableConstraintProgrammingBendersState? = null,
        cancellationChain: List<CancellationRecord> = emptyList(),
        provenance: PortableSolverProvenance? = null,
        metadata: Map<String, String> = emptyMap()
    ): Ret<ConstraintProgrammingCheckpointEnvelope> {
        // 捕获侧即拒绝自引用与空白父链：恢复侧校验只在显式启用时生效，若放任此类 envelope
        // 落盘，损坏数据会先被持久化，直到恢复时才暴露。
        //
        // Reject a self-referencing or blank parent at capture time: restore-side validation runs
        // only when explicitly enabled, so persisting such an envelope would first write corrupt
        // data and only surface the problem later, on recovery.
        parentLinkFailure(
            checkpointId = checkpointId,
            parentCheckpointId = parentCheckpointId,
            validateParentLink = true,
            expectedParentCheckpointId = null
        )?.let { return Failed(ErrorCode.IllegalArgument, it) }
        val encoded = ConstraintProgrammingSnapshotCodec.encode(snapshot)
        if (encoded.failed) {
            return propagate(encoded)
        }
        val portableIncumbent = incumbent?.toPortable(snapshot)
        if (portableIncumbent != null && portableIncumbent.failed) {
            return propagate(portableIncumbent)
        }
        validateBendersState(benders)?.let { return Failed(ErrorCode.IllegalArgument, it) }
        // 取消链的顺序与 provenance 的形状同样在捕获侧即校验，理由与父链一致：链上的顺序在恢复侧是
        // 无条件校验的，若捕获侧放过一条乱序链，损坏的审计轨迹会先落盘才被发现。
        //
        // The chain's ordering and the provenance's shape are validated at capture time for the same
        // reason as the parent link: the ordering is validated unconditionally on restore, so letting a
        // misordered chain through at capture time would persist a corrupt audit trail first.
        validateCancellationChain(cancellationChain.map { it.toPortableCancellationRecord() })
            ?.let { return Failed(ErrorCode.IllegalArgument, it) }
        validateProvenance(provenance)?.let { return Failed(ErrorCode.IllegalArgument, it) }
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
            benders = benders,
            cancellationChain = cancellationChain.map { it.toPortableCancellationRecord() },
            provenance = provenance,
            metadata = metadata
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
     * Decodes and verifies a schema 3.0 envelope.
     * 解码并复验 schema 3.0 envelope。
     *
     * @param encoded JSON 文本 / JSON text
     * @return envelope 或结构化错误 / Envelope or structured error
     */
    fun decode(encoded: String): Ret<ConstraintProgrammingCheckpointEnvelope> {
        return try {
            // 解码即按键升序规范化 string-map：契约 [canonicalization] 第 3 条把"string-map 升序"作为
            // 摘要可复算的前提，因此摘要在规范化后的文本上复算，解码结果也始终是规范形态。Rust 侧
            // 反序列化到 BTreeMap 后天然如此，两侧由此对齐：乱序 map 的文档在两侧都会被拒绝。
            //
            // Decoding canonicalizes the string-maps into ascending key order: contract
            // [canonicalization] rule 3 makes ascending maps a precondition for digest recomputation, so
            // the digest is recomputed over the canonical text and the decoded envelope is always
            // canonical. Rust behaves this way for free by deserializing into BTreeMaps, which aligns
            // the two sides: a document with unsorted maps is rejected on both.
            val envelope = canonicalizeMapKeys(
                json.decodeFromString(ConstraintProgrammingCheckpointEnvelope.serializer(), encoded)
            )
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
            // 解码即校验父链自身合法性（自引用 / 空白）。调用方若已知期望父标识，应改用
            // `restore(..., expectedParentCheckpointId = ...)` 做完整比对。
            //
            // Decoding validates the parent link's own legality (self-reference, blank). A caller
            // that knows the expected parent should use `restore(..., expectedParentCheckpointId = ...)`
            // for the full comparison.
            parentLinkFailure(
                checkpointId = envelope.checkpointId,
                parentCheckpointId = envelope.parentCheckpointId,
                validateParentLink = true,
                expectedParentCheckpointId = null
            )?.let { return Failed(ErrorCode.IllegalArgument, it) }
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
     * Decodes schema 3.0, the published schema 2.0 envelope, or the legacy v1 snapshot-only format.
     * 解码 schema 3.0、已发布的 schema 2.0 envelope 或 legacy v1 仅 snapshot 格式。
     *
     * @param encoded checkpoint JSON / Checkpoint JSON
     * @return verified schema 3.0-compatible envelope or structured error / 已验证的 schema 3.0 兼容 envelope 或结构化错误
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
        decodeLegacyV2(encoded)?.let { legacy ->
            // legacy v2 形状同样携带父标识，必须与当前形状接受同样的父链合法性检查。
            // The legacy v2 shape also carries a parent identifier and must accept the same
            // parent-link legality check as the current shape.
            parentLinkFailure(
                checkpointId = legacy.checkpointId,
                parentCheckpointId = legacy.parentCheckpointId,
                validateParentLink = true,
                expectedParentCheckpointId = null
            )?.let { return Failed(ErrorCode.IllegalArgument, it) }
            return ok(legacy)
        }
        // A document carrying any v2 marker is never eligible for legacy fallback. /
        // 含有任一 v2 标记的文档绝不能降级为 legacy。
        if (root.keys.any { it in V2_MARKER_FIELDS }) {
            // 原样附上当前形状解码的真实失败原因：只回一句"不得降级"会把"父链自引用"这类可定位
            // 的问题替换成一句泛化诊断，调用方无法据此判断该怎么修。降级禁令本身保持不变。
            //
            // Append the current shape's actual failure reason: answering only "must not downgrade"
            // would replace a locatable problem such as a self-referencing parent link with a generic
            // diagnostic the caller cannot act on. The downgrade prohibition itself is unchanged.
            val reason = when (current) {
                is Failed -> current.error.message
                is Fatal -> current.errors.firstOrNull()?.message
                else -> null
            }
            return Failed(
                ErrorCode.IllegalArgument,
                "损坏的 v2 checkpoint 不得降级为 legacy / A malformed v2 checkpoint must not downgrade to legacy" +
                    if (reason.isNullOrBlank()) "" else "：$reason"
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
     * **本摘要是完整性校验，不是真实性/防篡改证明。** / **This digest is an integrity check, not
     * an authenticity or tamper-proof guarantee.**
     *
     * 它使用无密钥的 SHA-256，且本函数是公开的，任何调用方都能在改动字段后重新计算摘要。
     * 因此它只能发现**意外损坏**（截断、写入错误、传输损坏、手工编辑失误），无法阻止**有意
     * 伪造**：攻击者或误用者可以改掉 `parentCheckpointId`、`runId`、`attemptId`、`bestBound`
     * 等任一字段后调用本函数重算摘要，从而通过 `decode` / `restore` 的摘要校验。
     *
     * 需要真实性保证的场景必须另外引入带密钥的 MAC（HMAC）或数字签名，并把密钥放在调用方
     * 不可及的位置；仅靠本摘要无法达成。父链/身份一致性校验（见 `parentLinkFailure`、
     * `restore` 的 `expectedRunId`/`expectedAttemptId`）用于让**调用方明确声明**期望值，
     * 而不是替代签名。
     *
     * It uses unkeyed SHA-256, and this function is public, so any caller can recompute the digest
     * after changing fields. It therefore detects only **accidental corruption** (truncation, write
     * errors, transport damage, mistaken manual edits) and cannot prevent **deliberate forgery**: an
     * attacker or misuser can alter `parentCheckpointId`, `runId`, `attemptId`, `bestBound`, or any
     * other field and call this function to recompute the digest, passing the digest checks in
     * `decode` and `restore`.
     *
     * Authenticity requires a keyed MAC (HMAC) or digital signature with the key held out of the
     * caller's reach; this digest alone cannot provide it. Parent/identity consistency validation
     * (see `parentLinkFailure` and `restore`'s `expectedRunId`/`expectedAttemptId`) lets the
     * **caller declare** expectations; it does not replace a signature.
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
     * @param validateParentLink 是否启用父链一致性校验；默认关闭以保持既有调用行为不变 / Whether to enable parent-chain consistency validation; disabled by default so existing call behavior is unchanged
     * @param expectedParentCheckpointId 调用方声明的期望父 checkpoint 标识；非 null 时同样启用父链一致性校验 / Expected parent checkpoint identifier declared by the caller; a non-null value also enables parent-chain consistency validation
     * @param expectedRunId 调用方声明的期望运行标识；非 null 时校验 envelope 的 runId 一致 / Expected run identifier declared by the caller; a non-null value verifies the envelope's runId
     * @param expectedAttemptId 调用方声明的期望尝试标识；非 null 时校验 envelope 的 attemptId 一致 / Expected attempt identifier declared by the caller; a non-null value verifies the envelope's attemptId
     * @param expectedCancellationChain 调用方声明的期望取消链；非 null 时要求 envelope 的链以它为前缀 / Expected cancellation chain declared by the caller; a non-null value requires the envelope's chain to carry it as a prefix
     * @param expectedProvenance 调用方声明的期望 provenance；非 null 时要求与 envelope 的 provenance 整体相同 / Expected provenance declared by the caller; a non-null value requires the envelope's provenance to match it exactly
     * @return 恢复结果 / Restore result
     */
    fun restore(
        envelope: ConstraintProgrammingCheckpointEnvelope,
        snapshot: ConstraintProgrammingModelSnapshot,
        expectedConfigurationFingerprint: String?,
        expectedSolverFingerprint: String?,
        allowLegacyConfigurationFingerprint: Boolean = false,
        allowLegacySolverFingerprint: Boolean = false,
        validateParentLink: Boolean = false,
        expectedParentCheckpointId: String? = null,
        expectedRunId: String? = null,
        expectedAttemptId: String? = null,
        expectedCancellationChain: List<PortableCancellationRecord>? = null,
        expectedProvenance: PortableSolverProvenance? = null
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
        // Parent-chain consistency validation is opt-in: callers that pass neither the flag nor an
        // expected parent keep the historical carry-without-validation behavior, while recovery entry
        // points that know their source checkpoint can reject forged, self-referencing and blank links.
        // 父链一致性校验为显式启用：既不传开关也不传期望父标识的调用方保持历史“只携带不校验”行为，
        // 而明确知道来源 checkpoint 的恢复入口可以拒绝伪造、自引用和空白父链。
        val parentLinkValidation = validateParentLink || expectedParentCheckpointId != null
        parentLinkFailure(
            checkpointId = envelope.checkpointId,
            parentCheckpointId = envelope.parentCheckpointId,
            validateParentLink = parentLinkValidation,
            expectedParentCheckpointId = expectedParentCheckpointId
        )?.let { return Failed(ErrorCode.IllegalArgument, it) }
        // 运行/尝试身份必须一次性比对：Rust `validate_resume_from` 要求 runId 与 attemptId 同时
        // 匹配，否则"用另一个运行的 checkpoint 恢复当前运行"这类串号无法被发现。
        // 空白期望值同样拒绝，避免调用方传入的空串被当成"匹配成功"。
        //
        // Run/attempt identity is compared in one pass: Rust's `validate_resume_from` requires both
        // runId and attemptId to match, otherwise resuming a run from another run's checkpoint goes
        // undetected. Blank expectations are rejected too, so an empty string is never treated as a
        // successful match.
        // 调用方声明的期望 provenance 自身必须合法：Rust `validate_resume_internal` 在比对之前先拒绝
        // 身份为空的期望 provenance，否则一个空身份的期望值只会以"不一致"这种误导性理由失败。
        //
        // The caller-declared expected provenance must itself be legal: Rust's
        // `validate_resume_internal` rejects a blank-identity expected provenance before comparing, so a
        // blank-identity expectation does not fail with the misleading reason "does not match".
        if (expectedProvenance != null) {
            if (expectedProvenance.solverId.isBlank() || expectedProvenance.backendName.isBlank()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "恢复请求的 provenance 身份不能为空白 / Resume request provenance identity cannot be blank"
                )
            }
        }
        if (expectedRunId != null) {
            if (expectedRunId.isBlank()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "期望的 checkpoint 运行标识不能为空白 / Expected checkpoint run identifier must not be blank"
                )
            }
            if (envelope.runId != expectedRunId) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "checkpoint 运行标识与恢复请求不一致 / Checkpoint run identity does not match the resume request"
                )
            }
        }
        if (expectedAttemptId != null) {
            if (expectedAttemptId.isBlank()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "期望的 checkpoint 尝试标识不能为空白 / Expected checkpoint attempt identifier must not be blank"
                )
            }
            if (envelope.attemptId != expectedAttemptId) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "checkpoint 尝试标识与恢复请求不一致 / Checkpoint attempt identity does not match the resume request"
                )
            }
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
        // provenance 与取消链的期望值比对同样为显式启用，位置与 Rust `validate_resume_internal` 一致：
        // 排在指纹比对之后。provenance 要求整体相同；取消链只要求以期望链为**前缀**，因为恢复请求
        // 通常只声明自己已知的那一段因果历史，envelope 上更晚发生的取消是合法的。
        //
        // The expected provenance and cancellation chain are compared only when declared, in the same
        // position as Rust's `validate_resume_internal`: after the fingerprint comparisons. Provenance is
        // compared in full; the chain only has to carry the expected chain as a **prefix**, because a
        // resume request usually declares just the causal history it knows about, and cancellations that
        // happened later in the envelope are legitimate.
        if (expectedProvenance != null && envelope.provenance != expectedProvenance) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint provenance 与恢复请求不一致 / Checkpoint provenance does not match the resume request"
            )
        }
        if (expectedCancellationChain != null &&
            (expectedCancellationChain.size > envelope.cancellationChain.size ||
                envelope.cancellationChain.take(expectedCancellationChain.size) != expectedCancellationChain)
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "checkpoint 取消链未保留期望前缀 / " +
                    "Checkpoint cancellation chain does not preserve the expected prefix"
            )
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
        validateCancellationChain(normalizedEnvelope.cancellationChain)?.let { return Failed(ErrorCode.IllegalArgument, it) }
        validateProvenance(normalizedEnvelope.provenance)?.let { return Failed(ErrorCode.IllegalArgument, it) }
        validateHistoricalBounds(
            incumbent = normalizedEnvelope.incumbent,
            bestBound = normalizedEnvelope.bestBound,
            gap = normalizedEnvelope.gap
        )?.let { return Failed(ErrorCode.IllegalArgument, it) }
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

    /**
     * 校验父链并返回失败消息，或在可接受时返回 null。
     * Validate the parent link, returning a failure message or null when acceptable.
     *
     * 捕获、解码与恢复三条路径共用本函数，确保"某一入口能拒绝的问题，另一些入口不会放行"。
     * 语义与 Rust `SolveCheckpoint::validate`（自引用、空白父）与 `validate_resume_from`
     * （期望父标识必须完全匹配，含 envelope 无父链的情形）对齐。
     *
     * Capture, decode, and restore share this function so a problem rejected at one entry point
     * is never let through at another. Semantics align with Rust's `SolveCheckpoint::validate`
     * (self-reference, blank parent) and `validate_resume_from` (the expected parent must match
     * exactly, including when the envelope carries no parent at all).
     *
     * @param checkpointId envelope 自身标识 / The envelope's own identifier
     * @param parentCheckpointId envelope 携带的父标识 / The parent identifier carried by the envelope
     * @param validateParentLink 是否校验父链自身合法性 / Whether to validate the parent link itself
     * @param expectedParentCheckpointId 调用方声明的期望父标识 / Expected parent identifier declared by the caller
     * @return 失败消息或 null / A failure message, or null
     */
    private fun parentLinkFailure(
        checkpointId: String,
        parentCheckpointId: String?,
        validateParentLink: Boolean,
        expectedParentCheckpointId: String?
    ): String? {
        if (validateParentLink && parentCheckpointId != null) {
            if (parentCheckpointId.isBlank()) {
                return "checkpoint 父标识不能为空白 / Checkpoint parent identifier must not be blank"
            }
            if (parentCheckpointId == checkpointId) {
                return "checkpoint 父标识不能等于自身标识 / " +
                        "Checkpoint parent identifier must not equal its own identifier"
            }
        }
        if (expectedParentCheckpointId != null) {
            if (expectedParentCheckpointId.isBlank()) {
                return "期望的 checkpoint 父标识不能为空白 / " +
                        "Expected checkpoint parent identifier must not be blank"
            }
            if (parentCheckpointId != expectedParentCheckpointId) {
                return "checkpoint 父标识与恢复请求不一致 / " +
                        "Checkpoint parent identifier does not match the resume request"
            }
        }
        return null
    }

    /**
     * 校验取消链的线格式合法性。 / Validate the cancellation chain's wire-shape legality.
     *
     * 校验线格式承载的全部取值：来源代码不能为空白（否则回读时无法映射回任何变体），时间戳不能为负
     * （`requested_at_epoch_ms` 在 Rust 侧是无符号数），且时间戳必须**单调不减**——取消链表示因果
     * 顺序，一条乱序的链意味着审计轨迹被篡改或拼接错误。未知代码是合法状态，由兜底变体承载，因此
     * 不在这里拒绝。
     *
     * 语义与 Rust `SolveCheckpoint::validate` 对齐：那里的顺序校验同样是无条件的，而不是仅在恢复
     * 请求显式声明期望链时才执行。
     *
     * Validates every value the wire carries: the origin code must not be blank (otherwise it cannot
     * map back to any variant), the timestamp must not be negative (`requested_at_epoch_ms` is unsigned
     * on the Rust side), and the timestamps must be **non-decreasing** — a cancellation chain expresses
     * causal order, so an out-of-order chain means the audit trail was tampered with or mis-spliced. An
     * unknown code is legitimate and lands in the catch-all variant, so it is not rejected here.
     *
     * Semantics align with Rust's `SolveCheckpoint::validate`, where the ordering check is likewise
     * unconditional rather than running only when a resume request declares an expected chain.
     *
     * @param chain 取消事实链 / Cancellation-fact chain
     * @return 失败消息或 null / A failure message, or null
     */
    private fun validateCancellationChain(chain: List<PortableCancellationRecord>): String? {
        var previousTimestamp = 0L
        chain.forEach { record ->
            if (record.origin.isBlank()) {
                return "checkpoint 取消来源代码不能为空白 / Checkpoint cancellation origin code must not be blank"
            }
            if (record.requestedAtEpochMs < 0L) {
                return "checkpoint 取消时间戳不能为负 / Checkpoint cancellation timestamp must not be negative"
            }
            // 单调不减：与 Rust `previous_timestamp` 从 0 起步并逐项比较的实现一致。
            // Non-decreasing: matches Rust, where `previous_timestamp` starts at 0 and is compared per
            // entry.
            if (record.requestedAtEpochMs < previousTimestamp) {
                return "checkpoint 取消链顺序无效 / Checkpoint cancellation chain is not ordered"
            }
            previousTimestamp = record.requestedAtEpochMs
        }
        return null
    }

    /**
     * 校验 provenance 的线格式合法性。 / Validate the provenance's wire-shape legality.
     *
     * @param provenance 求解器执行来源 / Solver execution provenance
     * @return 失败消息或 null / A failure message, or null
     */
    private fun validateProvenance(provenance: PortableSolverProvenance?): String? {
        if (provenance == null) {
            return null
        }
        if (provenance.solverId.isBlank() || provenance.backendName.isBlank()) {
            return "checkpoint provenance 缺少求解器或后端标识 / " +
                "Checkpoint provenance is missing the solver or backend identifier"
        }
        if (provenance.threadCount != null && provenance.threadCount < 0) {
            return "checkpoint provenance 线程数不能为负 / Checkpoint provenance thread count must not be negative"
        }
        return null
    }

    /**
     * 校验历史 incumbent / best bound / gap 的数值语义。
     * Validate the numeric semantics of the historical incumbent, best bound, and gap.
     *
     * 语义逐条对齐 Rust `SolveCheckpoint::validate`（`ospf-rust-core/src/solver/checkpoint.rs`）：
     *
     * 1. 非有限值一律拒绝（`NaN` / `Infinity` / 非数值文本）；
     * 2. `gap` 不能为负；
     * 3. `gap` 必须同时有 incumbent 目标值与 best bound —— 相对 gap 的定义就是"当前解与界之间的
     *    相对距离"，缺少任一端时它不是"无信息"，而是**无定义**；
     * 4. 三者齐备时必须互相自洽：`gap ≈ |objective - bound| / max(|objective|, 1)`。
     *
     * 第 3 条是相对此前 Kotlin 行为的**收紧**：Kotlin 过去允许只带 bound 的 gap。仅有 bound 的
     * "gap" 无法解释，放任它会掩盖上游把 gap 写错位置这类缺陷，因此与 Rust 对齐。
     *
     * Semantics align item by item with Rust's `SolveCheckpoint::validate`
     * (`ospf-rust-core/src/solver/checkpoint.rs`):
     *
     * 1. non-finite values (`NaN` / `Infinity` / non-numeric text) are always rejected;
     * 2. the `gap` must not be negative;
     * 3. the `gap` requires both an incumbent objective and a best bound — a relative gap is by
     *    definition the relative distance between the current solution and the bound, so when either
     *    end is missing it is not "uninformative" but **undefined**;
     * 4. when all three are present they must be mutually consistent:
     *    `gap ≈ |objective - bound| / max(|objective|, 1)`.
     *
     * Rule 3 is a **tightening** relative to Kotlin's previous behavior, which allowed a gap with
     * only a bound. A "gap" with no incumbent cannot be interpreted, and allowing it masks upstream
     * defects such as writing the gap into the wrong field, so it now matches Rust.
     *
     * @param incumbent 历史 incumbent / Historical incumbent
     * @param bestBound 历史最佳界 / Historical best bound
     * @param gap 历史最优间隙 / Historical optimality gap
     * @return 失败消息或 null / A failure message, or null
     */
    private fun validateHistoricalBounds(
        incumbent: PortableConstraintProgrammingIncumbent?,
        bestBound: String?,
        gap: String?
    ): String? {
        // 非数值文本同样按"非有限"处理：Rust 侧这些字段是 f64，无法表示 `NaN` 以外的任意文本，
        // 因此不可解析的值必须被拒绝，而不是被悄悄当成"无信息"。
        // Non-numeric text counts as non-finite too: these fields are f64 on the Rust side, so an
        // unparseable value must be rejected rather than silently treated as "no information".
        fun parse(value: String?, name: String): Any? {
            if (value == null) {
                return null
            }
            val parsed = value.toBigDecimalOrNull()
                ?: return "checkpoint $name 不是有效数值 / Checkpoint $name is not a valid number"
            return parsed
        }

        val boundValue = when (val parsed = parse(bestBound, "best bound")) {
            is String -> return parsed
            else -> parsed as java.math.BigDecimal?
        }
        val gapValue = when (val parsed = parse(gap, "relative gap")) {
            is String -> return parsed
            else -> parsed as java.math.BigDecimal?
        }
        val objectiveValue = when (val parsed = parse(incumbent?.objective, "incumbent objective")) {
            is String -> return parsed
            else -> parsed as java.math.BigDecimal?
        }

        if (gapValue != null && gapValue.signum() < 0) {
            return "checkpoint 相对 gap 不能为负 / Checkpoint relative gap cannot be negative"
        }
        if (gapValue != null && (objectiveValue == null || boundValue == null)) {
            return "checkpoint 相对 gap 需要同时提供 incumbent 目标值与最佳界 / " +
                "Checkpoint relative gap requires both an incumbent objective and a best bound"
        }
        if (objectiveValue != null && boundValue != null && gapValue != null) {
            val context = java.math.MathContext(34, java.math.RoundingMode.HALF_UP)
            val scale = objectiveValue.abs().max(java.math.BigDecimal.ONE)
            val expected = objectiveValue.subtract(boundValue).abs().divide(scale, context)
            val tolerance = java.math.BigDecimal("1E-9").multiply(
                expected.abs().max(gapValue.abs()).max(java.math.BigDecimal.ONE)
            )
            if (expected.subtract(gapValue).abs() > tolerance) {
                return "checkpoint 相对 gap 与 incumbent 及最佳界不一致 / " +
                    "Checkpoint relative gap does not match the incumbent and bound"
            }
        }
        return null
    }

    /**
     * 校验恢复出的子 checkpoint 是否正确指向其源 checkpoint。
     * Validate that a resumed child checkpoint correctly points at its source checkpoint.
     *
     * 语义逐条对齐 Rust `SolveCheckpoint::validate_resumed_child`
     * （`ospf-rust-core/src/solver/checkpoint.rs`），仅把"父 attempt"换成 Kotlin envelope 的
     * "父 checkpoint"链接：Rust 的父子关系记录在 `parent_attempt_id`，而 Kotlin 的 envelope 用
     * `parentCheckpointId` 记录同一关系。
     *
     * 1. 子与源必须属于**同一运行**（`runId` 相同）——否则是串号恢复；
     * 2. 子的 `parentCheckpointId` 必须**恰好等于源的 `checkpointId`**；
     * 3. 模型 / 配置 / 求解器指纹与 provenance 必须与源**完全一致**——恢复不得悄悄换掉身份；
     * 4. 子的取消链必须以源的取消链为**前缀**——恢复不得抹掉既有的取消历史。
     *
     * Semantics align item by item with Rust's `SolveCheckpoint::validate_resumed_child`
     * (`ospf-rust-core/src/solver/checkpoint.rs`), substituting Kotlin's parent **checkpoint** link
     * for Rust's parent **attempt**: Rust records the relationship in `parent_attempt_id`, while the
     * Kotlin envelope records the same relationship in `parentCheckpointId`.
     *
     * 1. child and source must belong to the **same run** (`runId` equal), otherwise it is a
     *    cross-run resume;
     * 2. the child's `parentCheckpointId` must be **exactly** the source's `checkpointId`;
     * 3. model / configuration / solver fingerprints and provenance must match the source exactly —
     *    a resume must not silently swap identity;
     * 4. the child's cancellation chain must carry the source's chain as a **prefix** — a resume must
     *    not erase prior cancellation history.
     *
     * @param parent 源 checkpoint / Source checkpoint
     * @param child 恢复出的子 checkpoint / Resumed child checkpoint
     * @return 失败消息或 null / A failure message, or null
     */
    fun validateResumedChild(
        parent: ConstraintProgrammingCheckpointEnvelope,
        child: ConstraintProgrammingCheckpointEnvelope
    ): String? {
        if (child.runId != parent.runId) {
            return "恢复出的子 checkpoint 不属于源运行 / " +
                "Resumed child checkpoint does not belong to the source run"
        }
        if (child.parentCheckpointId != parent.checkpointId) {
            return "恢复出的子 checkpoint 的父链接与源 checkpoint 不一致 / " +
                "Resumed child checkpoint parent does not match the source checkpoint"
        }
        if (child.modelFingerprint != parent.modelFingerprint ||
            child.configurationFingerprint != parent.configurationFingerprint ||
            child.solverFingerprint != parent.solverFingerprint ||
            child.provenance != parent.provenance
        ) {
            return "恢复出的子 checkpoint 未保持源身份 / " +
                "Resumed child checkpoint does not preserve source identity"
        }
        val sourceChain = parent.cancellationChain
        if (child.cancellationChain.size < sourceChain.size ||
            child.cancellationChain.take(sourceChain.size) != sourceChain
        ) {
            return "恢复出的子 checkpoint 丢失了源的取消历史 / " +
                "Resumed child checkpoint lost source cancellation history"
        }
        return null
    }

    /**
     * 保留父链接与取消链，派生下一次 attempt 的 checkpoint。
     * Derive the next attempt's checkpoint while preserving the parent link and cancellation chain.
     *
     * 对齐 Rust `SolveCheckpoint::fork_for_resume`：新 envelope 继承源的全部内容，只把
     * `parentCheckpointId` 指向源、替换自身标识与尝试标识、并更新时间戳，随后**强制**通过
     * [validateResumedChild]，因此不可能派生出父子关系不成立的 checkpoint。
     *
     * Mirrors Rust's `SolveCheckpoint::fork_for_resume`: the new envelope inherits everything from the
     * source, points `parentCheckpointId` at it, replaces its own identity and attempt identifier, and
     * refreshes the timestamp, then **must** pass [validateResumedChild] — so a checkpoint with an
     * inconsistent parent relationship cannot be produced.
     *
     * @param parent 源 checkpoint / Source checkpoint
     * @param checkpointId 子 checkpoint 标识 / Child checkpoint identifier
     * @param createdAtEpochMs 子创建时间 / Child creation time
     * @param attemptId 子尝试标识；null 时沿用源的尝试标识 / Child attempt identifier; the source's is reused when null
     * @return 子 envelope 或结构化错误 / Child envelope or structured error
     */
    fun forkForResume(
        parent: ConstraintProgrammingCheckpointEnvelope,
        checkpointId: String,
        createdAtEpochMs: Long,
        attemptId: String? = null
    ): Ret<ConstraintProgrammingCheckpointEnvelope> {
        if (checkpointId.isBlank()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "fork 出的 checkpoint 标识不能为空白 / A forked checkpoint identifier must not be blank"
            )
        }
        if (checkpointId == parent.checkpointId) {
            return Failed(
                ErrorCode.IllegalArgument,
                "fork 出的 checkpoint 标识不能与源相同 / A forked checkpoint identifier must differ from its source"
            )
        }
        val child = withIntegrity(
            parent.copy(
                checkpointId = checkpointId,
                attemptId = attemptId ?: parent.attemptId,
                parentCheckpointId = parent.checkpointId,
                createdAtEpochMs = createdAtEpochMs
            )
        )
        validateResumedChild(parent, child)?.let { return Failed(ErrorCode.IllegalArgument, it) }
        return ok(child)
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
        // 摘要必须建立在规范化后的文本上：契约要求所有 string-map 按键升序输出，而 Kotlin 的
        // `Map` 保留插入顺序，调用方传入的乱序 Map 若直接进入摘要就会与 Rust 侧的 BTreeMap 失配。
        //
        // The digest must be computed over the canonical text: the contract requires every
        // string-map in ascending key order, while Kotlin's `Map` preserves insertion order, so an
        // unsorted map supplied by a caller would otherwise disagree with Rust's BTreeMap.
        val normalized = canonicalizeMapKeys(envelope).copy(schemaVersion = CURRENT_SCHEMA)
        return normalized.copy(
            integritySha256 = digest(normalized.copy(integritySha256 = ""))
        )
    }

    /**
     * 按键升序规范化 envelope 内所有 string-map。 / Canonicalize every string-map inside the envelope into ascending key order.
     *
     * 列表顺序（含 `cancellationChain` 的取消链顺序）是契约语义的一部分，因此只排序 Map，不排序列表。
     * List order (including the cancellation-chain order) is part of the contract semantics, so only
     * maps are sorted, never lists.
     *
     * @param envelope checkpoint envelope / checkpoint envelope
     * @return 所有 string-map 已按键升序排列的 envelope / Envelope whose string-maps are all in ascending key order
     */
    private fun canonicalizeMapKeys(
        envelope: ConstraintProgrammingCheckpointEnvelope
    ): ConstraintProgrammingCheckpointEnvelope {
        return envelope.copy(
            incumbent = envelope.incumbent?.let { incumbent ->
                incumbent.copy(
                    valuesById = incumbent.valuesById.toSortedMap(),
                    intervalsById = incumbent.intervalsById.toSortedMap()
                )
            },
            conflicts = envelope.conflicts.map { it.canonicalizeMapKeys() },
            benders = envelope.benders?.let { state ->
                state.copy(
                    fixedBindings = state.fixedBindings.toSortedMap(),
                    cuts = state.cuts.map { it.canonicalizeMapKeys() },
                    conflicts = state.conflicts.map { it.canonicalizeMapKeys() }
                )
            },
            provenance = envelope.provenance?.let { provenance ->
                provenance.copy(
                    requestedConfiguration = provenance.requestedConfiguration.toSortedMap(),
                    effectiveConfiguration = provenance.effectiveConfiguration.toSortedMap(),
                    environmentSummary = provenance.environmentSummary.toSortedMap()
                )
            },
            metadata = envelope.metadata.toSortedMap()
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

private fun PortableConstraintProgrammingConflict.canonicalizeMapKeys(): PortableConstraintProgrammingConflict {
    return copy(provenance = provenance.toSortedMap())
}

private fun PortableConstraintProgrammingCut.canonicalizeMapKeys(): PortableConstraintProgrammingCut {
    return copy(
        provenance = provenance.toSortedMap(),
        payload = payload.toSortedMap()
    )
}

private fun <T> propagate(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "CP checkpoint 结果状态无效 / Invalid CP checkpoint result state")
    }
}
