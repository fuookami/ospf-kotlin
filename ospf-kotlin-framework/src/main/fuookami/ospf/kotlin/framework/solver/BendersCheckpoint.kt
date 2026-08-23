/** Portable Logic-Based Benders checkpoint SPI. / 可移植 Logic-Based Benders checkpoint SPI。 */
package fuookami.ospf.kotlin.framework.solver

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingSnapshotCodec
import fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingBendersState
import fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingConflict
import fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingCut
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingConflict
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.SolveFingerprinting

/**
 * Versioned serializer supplied by a domain for portable Benders cuts. /
 * 领域为可移植 Benders cut 提供的版本化序列化器。
 *
 * The SPI deliberately receives and returns primitive payloads only. Closures, solver handles,
 * model references, and backend objects must remain outside a checkpoint. /
 * 该 SPI 刻意只接收和返回基础类型 payload；闭包、求解器句柄、模型引用和后端对象不得进入 checkpoint。
 *
 * @property schemaVersion Domain-owned cut payload schema. / 领域拥有的 cut payload schema。
 */
interface BendersCutSerializationSpi {
    /** Cut payload schema owned by the domain. / 领域拥有的 cut payload schema。 */
    val schemaVersion: String

    /**
     * Serialize one accepted cut. / 序列化一个已接受的 cut。
     *
     * @param cut Accepted Benders cut. / 已接受的 Benders cut。
     * @return Portable primitive payload or a structured error. / 可移植基础 payload 或结构化错误。
     */
    fun encode(cut: BendersMasterCut): Ret<PortableConstraintProgrammingCut>

    /**
     * Deserialize and validate one cut. / 反序列化并校验一个 cut。
     *
     * @param cut Portable primitive payload. / 可移植基础 payload。
     * @return Domain cut or a structured error. / 领域 cut 或结构化错误。
     */
    fun decode(cut: PortableConstraintProgrammingCut): Ret<BendersMasterCut>
}

/**
 * In-memory Benders state used to seed a resumed engine. /
 * 用于启动恢复引擎的内存 Benders 状态。
 *
 * @property iteration Next iteration to execute. / 下一次要执行的迭代号。
 * @property cuts Restored and independently decoded cuts. / 已独立解码的 cut。
 * @property trace Historical trace retained for audit only. / 仅用于审计的历史轨迹。
 * @property assumptions Revalidated assumption IDs. / 已复验的 assumption 标识。
 * @property fixedBindings Revalidated CP fixed bindings. / 已复验的 CP 固定绑定。
 * @property conflicts Revalidated conflict evidence. / 已复验的冲突证据。
 * @property convergenceVerified Historical convergence marker; never trusted as a proof. /
 * 历史收敛标记，不作为证明信任。
 * @property masterFingerprint Fingerprint supplied by the domain master adapter. /
 * 由领域主问题适配器提供的主问题指纹。
 * @property subproblemModelFingerprint Fingerprint of the CP model used for the state. /
 * 该状态对应的 CP 子问题模型指纹。
 */
data class BendersResumeState(
    val iteration: Int,
    val cuts: List<BendersMasterCut>,
    val masterIncumbent: String? = null,
    val masterBestBound: String? = null,
    val trace: List<String> = emptyList(),
    val assumptions: List<String> = emptyList(),
    val fixedBindings: Map<String, Long> = emptyMap(),
    val conflicts: List<PortableConstraintProgrammingConflict> = emptyList(),
    val convergenceVerified: Boolean = false,
    val masterFingerprint: String? = null,
    val subproblemModelFingerprint: String? = null
)

/** Codec and restore gate for portable Benders state. / 可移植 Benders 状态编解码和恢复门禁。 */
object BendersCheckpointCodec {
    private const val DOCUMENT_SCHEMA = "1.0"
    private val documentJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    @Serializable
    private data class Document(
        val schemaVersion: String = DOCUMENT_SCHEMA,
        val state: PortableConstraintProgrammingBendersState,
        val integritySha256: String = ""
    )

    /**
     * State shape used by published schema 1.0 documents before masterFingerprint was added.
     * masterFingerprint 加入前已发布的 schema 1.0 文档状态形状。
     */
    @Serializable
    private data class LegacyState(
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

    /** Published schema 1.0 document without the later master fingerprint field.
     * 不含后续 master fingerprint 字段的已发布 schema 1.0 文档。
     */
    @Serializable
    private data class LegacyDocument(
        val schemaVersion: String = DOCUMENT_SCHEMA,
        val state: LegacyState,
        val integritySha256: String = ""
    )

    @Serializable
    private data class TraceDocument(
        val schemaVersion: String = "1.0",
        val iteration: Int,
        val masterObjective: String? = null,
        val masterBestBound: String? = null,
        val masterStatus: String? = null,
        val subproblemStatus: String,
        val subproblemObjective: String? = null,
        val cutCount: Int,
        val conflictCoreSize: Int,
        val elapsedNanos: Long,
        val proofMode: String,
        val convergenceGap: String? = null
    )

    /**
     * Encode portable Benders state as a self-contained, integrity-protected document. /
     * 将可移植 Benders 状态编码为自包含且带完整性保护的文档。
     *
     * @param state Primitive-only state captured by [capture] / [capture] 捕获的基础类型状态
     * @return JSON document or a structured error / JSON 文档或结构化错误
     */
    fun encode(state: PortableConstraintProgrammingBendersState): Ret<String> {
        val validation = validatePortableState(state)
        when (validation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return try {
            ok(documentJson.encodeToString(Document.serializer(), withDigest(Document(state = state))))
        } catch (error: Throwable) {
            Failed(
                ErrorCode.Other,
                "Benders checkpoint 编码失败：${error.message} / Benders checkpoint encoding failed: ${error.message}"
            )
        }
    }

    /**
     * Decode and verify a portable Benders document. /
     * 解码并校验可移植 Benders 文档。
     *
     * @param encoded JSON document / JSON 文档
     * @return primitive-only state or a structured error / 基础类型状态或结构化错误
     */
    fun decode(encoded: String): Ret<PortableConstraintProgrammingBendersState> {
        return try {
            val document = documentJson.decodeFromString(Document.serializer(), encoded)
            if (document.schemaVersion != DOCUMENT_SCHEMA) {
                return Failed(
                    ErrorCode.Other,
                    "不支持的 Benders checkpoint schema：${document.schemaVersion} / Unsupported Benders checkpoint schema: ${document.schemaVersion}"
                )
            }
            var legacyDigest = false
            val state = if (document.integritySha256.isNotBlank() &&
                document.integritySha256 == digest(document.copy(integritySha256 = ""))
            ) {
                document.state
            } else {
                legacyDigest = true
                decodeLegacyDocument(encoded) ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders checkpoint 完整性摘要不匹配 / Benders checkpoint integrity digest mismatch"
                )
            }
            val validation = validatePortableState(
                state = state,
                allowLegacyUnboundMasterFingerprint = legacyDigest
            )
            when (validation) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            ok(state)
        } catch (error: Throwable) {
            Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint 解码失败：${error.message} / Benders checkpoint decoding failed: ${error.message}"
            )
        }
    }

    /**
     * Capture and encode a report in one persistence operation. / 一次性捕获并编码报告。
     *
     * @param report Benders report to capture. / 要捕获的 Benders 报告。
     * @param serializer Domain cut serializer. / 领域 cut 序列化器。
     * @param subproblemSnapshot Optional CP snapshot used to bind the state identity. / 可选 CP snapshot，用于绑定状态身份。
     * @param masterFingerprint Optional domain master fingerprint. / 可选的领域主问题指纹。
     * @return Integrity-protected checkpoint document or a structured error. / 带完整性保护的 checkpoint 文档或结构化错误。
     */
    fun captureEncoded(
        report: LogicBasedBendersReport,
        serializer: BendersCutSerializationSpi,
        subproblemSnapshot: ConstraintProgrammingModelSnapshot? = null,
        masterFingerprint: String? = null
    ): Ret<String> {
        return when (val captured = capture(report, serializer, subproblemSnapshot, masterFingerprint)) {
            is Ok -> encode(captured.value)
            is Failed -> Failed(captured.error)
            is Fatal -> Fatal(captured.errors)
        }
    }

    /**
     * Decode, structurally validate, and restore a persisted Benders state. / 解码、结构校验并恢复持久化状态。
     *
     * @param encoded Integrity-protected checkpoint document. / 带完整性保护的 checkpoint 文档。
     * @param serializer Domain cut serializer. / 领域 cut 序列化器。
     * @param proofMode Active engine proof mode. / 引擎当前证明模式。
     * @param expectedSubproblemSnapshot Current CP snapshot used to verify identity. / 用于校验身份的当前 CP snapshot。
     * @return Restored Benders state or a structured error. / 恢复后的 Benders 状态或结构化错误。
     */
    fun restoreEncoded(
        encoded: String,
        serializer: BendersCutSerializationSpi,
        proofMode: BendersProofMode = BendersProofMode.Exact,
        expectedSubproblemSnapshot: ConstraintProgrammingModelSnapshot? = null
    ): Ret<BendersResumeState> {
        return when (val decoded = decode(encoded)) {
            is Ok -> restore(decoded.value, serializer, proofMode, expectedSubproblemSnapshot)
            is Failed -> Failed(decoded.error)
            is Fatal -> Fatal(decoded.errors)
        }
    }

    /**
     * Capture report cuts into the portable checkpoint representation. /
     * 将报告中的 cut 捕获为可移植 checkpoint 表示。
     *
     * @param report Benders report to capture. / 要捕获的 Benders 报告。
     * @param serializer Domain cut serializer. / 领域 cut 序列化器。
     * @param subproblemSnapshot Optional CP snapshot used to bind the state identity. / 可选的 CP snapshot，用于绑定状态身份。
     * @param masterFingerprint Optional domain master fingerprint. / 可选的领域主问题指纹。
     * @return Portable Benders state or a structured error. / 可移植 Benders 状态或结构化错误。
     */
    fun capture(
        report: LogicBasedBendersReport,
        serializer: BendersCutSerializationSpi,
        subproblemSnapshot: ConstraintProgrammingModelSnapshot? = null,
        masterFingerprint: String? = null
    ): Ret<PortableConstraintProgrammingBendersState> {
        if (serializer.schemaVersion.isBlank()) {
            return Failed(ErrorCode.IllegalArgument, "Benders cut schema 不能为空 / Benders cut schema must not be blank")
        }
        if ((report.masterOutput != null || report.assignment != null) && masterFingerprint.isNullOrBlank()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders 主问题状态必须提供模型指纹 / Benders master state requires a model fingerprint"
            )
        }
        val modelFingerprint = when (val fingerprint = subproblemSnapshot?.let(::computeBendersSubproblemModelFingerprint)) {
            null -> null
            is Ok -> fingerprint.value
            is Failed -> return Failed(fingerprint.error)
            is Fatal -> return Fatal(fingerprint.errors)
        }
        val encoded = ArrayList<PortableConstraintProgrammingCut>()
        val ids = HashSet<String>()
        for (cut in report.cuts) {
            val payload = serializer.encode(cut)
            when (payload) {
                is Ok -> {
                    val value = payload.value
                    if (value.id.isBlank() || value.schemaVersion != serializer.schemaVersion) {
                        return Failed(
                            ErrorCode.IllegalArgument,
                            "Benders checkpoint cut 身份或 schema 无效 / Benders checkpoint cut identity or schema is invalid"
                        )
                    }
                    if (!ids.add(value.id)) {
                        return Failed(
                            ErrorCode.IllegalArgument,
                            "Benders checkpoint 存在重复 cut ID / Benders checkpoint contains duplicate cut IDs"
                        )
                    }
                    encoded += value
                }

                is Failed -> return Failed(payload.error)
                is Fatal -> return Fatal(payload.errors)
            }
        }
        val state = PortableConstraintProgrammingBendersState(
                iteration = report.iterations.lastOrNull()?.iteration?.toLong()?.plus(1L) ?: 0L,
                masterIncumbent = report.masterOutput?.solution?.objective?.toString(),
                masterBestBound = report.masterOutput?.bestBound?.toString(),
                cuts = encoded,
                trace = report.iterations.map(::encodeTrace),
                assumptions = report.assignment?.assumptions
                    ?.mapNotNull { it.variableId?.value }
                    ?.distinct()
                    ?: emptyList(),
                fixedBindings = report.assignment?.fixedValues
                    ?.mapKeys { it.key.value }
                    ?.mapValues { it.value.toLong() }
                    ?: emptyMap(),
                conflicts = (report.subproblemResult as? InfeasibleSubproblemResult)
                    ?.conflict
                    ?.let(::portableConflict)
                    ?.let(::listOf)
                    ?: emptyList(),
                convergenceVerified = report.proof.status == ProofStatus.Verified,
                masterFingerprint = masterFingerprint,
                subproblemModelFingerprint = modelFingerprint
            )
        validatePortableState(state).let { validation ->
            when (validation) {
                is Ok -> return ok(state)
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
        }
    }

    /**
     * Restore a portable Benders state after schema, identity, and proof checks. /
     * 在 schema、身份和证明校验后恢复可移植 Benders 状态。
     *
     * @param state Portable state from a checkpoint. / checkpoint 中的可移植状态。
     * @param serializer Domain cut serializer. / 领域 cut 序列化器。
     * @param proofMode Active engine proof mode. / 引擎当前证明模式。
     * @param expectedSubproblemSnapshot Current CP snapshot used to verify the state identity. / 当前 CP snapshot，用于校验状态身份。
     * @return In-memory resume state or a structured error. / 内存恢复状态或结构化错误。
     */
    fun restore(
        state: PortableConstraintProgrammingBendersState,
        serializer: BendersCutSerializationSpi,
        proofMode: BendersProofMode = BendersProofMode.Exact,
        expectedSubproblemSnapshot: ConstraintProgrammingModelSnapshot? = null
    ): Ret<BendersResumeState> {
        if (serializer.schemaVersion.isBlank()) {
            return Failed(ErrorCode.IllegalArgument, "Benders cut schema 不能为空 / Benders cut schema must not be blank")
        }
        if (state.iteration < 0L || state.iteration > Int.MAX_VALUE.toLong()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint iteration 超出有效范围 / Benders checkpoint iteration is outside the valid range"
            )
        }
        if (state.assumptions.any(String::isBlank) || state.fixedBindings.keys.any(String::isBlank)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint binding 标识不能为空 / Benders checkpoint binding identifiers must not be blank"
            )
        }
        if (state.masterFingerprint?.isBlank() == true || state.subproblemModelFingerprint?.isBlank() == true) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint 模型指纹不能为空 / Benders checkpoint model fingerprints must not be blank"
            )
        }
        if (hasMasterState(state) && state.masterFingerprint.isNullOrBlank()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders 主问题状态缺少模型指纹 / Benders master state is missing a model fingerprint"
            )
        }
        val expectedFingerprint = when (val fingerprint = expectedSubproblemSnapshot?.let(::computeBendersSubproblemModelFingerprint)) {
            null -> null
            is Ok -> fingerprint.value
            is Failed -> return Failed(fingerprint.error)
            is Fatal -> return Fatal(fingerprint.errors)
        }
        if (state.subproblemModelFingerprint != null &&
            expectedFingerprint != null &&
            state.subproblemModelFingerprint != expectedFingerprint
        ) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "Benders checkpoint 子问题模型指纹不匹配 / Benders checkpoint subproblem model fingerprint mismatch"
            )
        }
        if (state.conflicts.any { conflict ->
                conflict.validity !in setOf("Verified", "Heuristic", "Unknown") ||
                    conflict.minimality !in setOf("Irreducible", "Partial", "NotChecked") ||
                    conflict.memberIds.any(String::isBlank) ||
                    conflict.assumptionIds.any(String::isBlank)
            }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint conflict 证据无效 / Benders checkpoint conflict evidence is invalid"
            )
        }
        val ids = HashSet<String>()
        val cuts = ArrayList<BendersMasterCut>()
        for (portable in state.cuts) {
            if (portable.id.isBlank() || portable.schemaVersion != serializer.schemaVersion || !ids.add(portable.id)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders checkpoint cut schema 或 ID 无效 / Benders checkpoint cut schema or ID is invalid"
                )
            }
            val decoded = serializer.decode(portable)
            when (decoded) {
                is Ok -> {
                    val cut = decoded.value
                    if (proofMode == BendersProofMode.Exact &&
                        (cut.validity != BendersCutValidity.Global || cut.proofStatus != ProofStatus.Verified)
                    ) {
                        return Failed(
                            ErrorCode.IllegalArgument,
                            "Exact 模式拒绝未验证的恢复 cut / Exact mode rejects an unverified restored cut"
                        )
                    }
                    cuts += cut
                }

                is Failed -> return Failed(decoded.error)
                is Fatal -> return Fatal(decoded.errors)
            }
        }
        if (state.iteration > Int.MAX_VALUE.toLong()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint iteration 超出范围 / Benders checkpoint iteration exceeds Int range"
            )
        }
        val decodedTraces = state.trace.map { encoded ->
            when (val decoded = decodeTrace(encoded)) {
                is Ok -> decoded.value
                is Failed -> return Failed(decoded.error)
                is Fatal -> return Fatal(decoded.errors)
            }
        }
        var previousIteration = -1
        for (trace in decodedTraces) {
            if (trace.iteration <= previousIteration || trace.iteration >= state.iteration) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders checkpoint trace 顺序或迭代范围无效 / Benders checkpoint trace order or iteration range is invalid"
                )
            }
            if (trace.proofMode != proofMode) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders checkpoint trace proof mode 与当前引擎不一致 / Benders checkpoint trace proof mode disagrees with the active engine"
                )
            }
            previousIteration = trace.iteration
        }
        val traces = state.trace
        val iteration = state.iteration.toInt()
        return ok(
            BendersResumeState(
                iteration = iteration,
                cuts = cuts,
                masterIncumbent = state.masterIncumbent,
                masterBestBound = state.masterBestBound,
                trace = traces,
                assumptions = state.assumptions.distinct(),
                fixedBindings = state.fixedBindings.toMap(),
                conflicts = state.conflicts.toList(),
                convergenceVerified = state.convergenceVerified,
                masterFingerprint = state.masterFingerprint,
                subproblemModelFingerprint = state.subproblemModelFingerprint
            )
        )
    }

    private fun encodeTrace(trace: BendersIterationTrace): String {
        return "v1:" + documentJson.encodeToString(
            TraceDocument.serializer(),
            TraceDocument(
                iteration = trace.iteration,
                masterObjective = trace.masterObjective?.toString(),
                masterBestBound = trace.masterBestBound?.toString(),
                masterStatus = trace.masterStatus?.name,
                subproblemStatus = trace.subproblemStatus.name,
                subproblemObjective = trace.subproblemObjective?.toString(),
                cutCount = trace.cutCount,
                conflictCoreSize = trace.conflictCoreSize,
                elapsedNanos = trace.elapsed.inWholeNanoseconds,
                proofMode = trace.proofMode.name,
                convergenceGap = trace.convergenceGap?.toString()
            )
        )
    }

    internal fun decodeTrace(encoded: String): Ret<BendersIterationTrace> {
        if (!encoded.startsWith("v1:")) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint trace 缺少版本 / Benders checkpoint trace version is missing"
            )
        }
        return try {
            val document = documentJson.decodeFromString<TraceDocument>(encoded.removePrefix("v1:"))
            if (document.schemaVersion != "1.0" || document.iteration < 0 ||
                document.cutCount < 0 || document.conflictCoreSize < 0 || document.elapsedNanos < 0
            ) {
                return Failed(ErrorCode.IllegalArgument, "Benders checkpoint trace 字段无效 / Benders checkpoint trace fields are invalid")
            }
            val subproblemStatus = runCatching { BendersSubproblemStatus.valueOf(document.subproblemStatus) }.getOrNull()
                ?: return Failed(ErrorCode.IllegalArgument, "Benders checkpoint trace 子问题状态无效 / Benders checkpoint trace subproblem status is invalid")
            val proofMode = runCatching { BendersProofMode.valueOf(document.proofMode) }.getOrNull()
                ?: return Failed(ErrorCode.IllegalArgument, "Benders checkpoint trace proof mode 无效 / Benders checkpoint trace proof mode is invalid")
            val masterStatus = document.masterStatus?.let {
                runCatching { fuookami.ospf.kotlin.core.solver.output.SolverStatus.valueOf(it) }.getOrNull()
                    ?: return Failed(
                        ErrorCode.IllegalArgument,
                        "Benders checkpoint trace 主问题状态无效 / Benders checkpoint trace master status is invalid"
                    )
            }
            val masterObjective = when (val parsed = parseTraceNumber(document.masterObjective, "masterObjective")) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }
            val masterBestBound = when (val parsed = parseTraceNumber(document.masterBestBound, "masterBestBound")) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }
            val subproblemObjective = when (val parsed = parseTraceNumber(document.subproblemObjective, "subproblemObjective")) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }
            val convergenceGap = when (val parsed = parseTraceNumber(document.convergenceGap, "convergenceGap")) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }
            ok(
                BendersIterationTrace(
                    iteration = document.iteration,
                    masterObjective = masterObjective,
                    masterBestBound = masterBestBound,
                    masterStatus = masterStatus,
                    subproblemStatus = subproblemStatus,
                    subproblemObjective = subproblemObjective,
                    cutCount = document.cutCount,
                    conflictCoreSize = document.conflictCoreSize,
                    elapsed = document.elapsedNanos.toDuration(DurationUnit.NANOSECONDS),
                    proofMode = proofMode,
                    convergenceGap = convergenceGap
                )
            )
        } catch (error: Throwable) {
            Failed(ErrorCode.IllegalArgument, "Benders checkpoint trace 解码失败：${error.message} / Benders checkpoint trace decoding failed: ${error.message}")
        }
    }

    private fun validatePortableState(
        state: PortableConstraintProgrammingBendersState,
        allowLegacyUnboundMasterFingerprint: Boolean = false
    ): Try {
        if (state.iteration < 0L || state.cuts.any { it.id.isBlank() || it.schemaVersion.isBlank() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint state 或 cut ID/schema 无效 / Benders checkpoint state or cut ID/schema is invalid"
            )
        }
        if (state.masterFingerprint?.isBlank() == true || state.subproblemModelFingerprint?.isBlank() == true) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint 模型指纹不能为空 / Benders checkpoint model fingerprints must not be blank"
            )
        }
        if (!allowLegacyUnboundMasterFingerprint && hasMasterState(state) && state.masterFingerprint.isNullOrBlank()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders 主问题状态缺少模型指纹 / Benders master state is missing a model fingerprint"
            )
        }
        if (listOf(state.masterIncumbent, state.masterBestBound).any { value ->
                value != null && (value.toDoubleOrNull()?.isFinite() != true)
            }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint master bound/ incumbent 无效 / Benders checkpoint master bound or incumbent is invalid"
            )
        }
        if (state.cuts.map { it.id }.distinct().size != state.cuts.size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint 存在重复 cut ID / Benders checkpoint contains duplicate cut IDs"
            )
        }
        if (state.cuts.any { cut ->
                cut.provenance.keys.any(String::isBlank) || cut.payload.keys.any(String::isBlank)
            }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint cut payload 键不能为空 / Benders checkpoint cut payload keys must not be blank"
            )
        }
        if (state.assumptions.any(String::isBlank) || state.fixedBindings.keys.any(String::isBlank)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint binding 标识不能为空 / Benders checkpoint binding identifiers must not be blank"
            )
        }
        return ok
    }

    private fun parseTraceNumber(value: String?, field: String): Ret<Flt64?> {
        if (value == null) {
            return ok(null)
        }
        val parsed = value.toDoubleOrNull()
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint trace 数值无效：$field / Benders checkpoint trace number is invalid: $field"
            )
        if (!parsed.isFinite()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint trace 数值必须有限：$field / Benders checkpoint trace number must be finite: $field"
            )
        }
        return ok(Flt64(parsed))
    }

    private fun withDigest(document: Document): Document {
        return document.copy(integritySha256 = digest(document.copy(integritySha256 = "")))
    }

    private fun digest(document: Document): String {
        val canonical = documentJson.encodeToString(Document.serializer(), document)
        return SolveFingerprinting.sha256(canonical, DOCUMENT_SCHEMA).value
    }

    private fun digest(document: LegacyDocument): String {
        val canonical = documentJson.encodeToString(LegacyDocument.serializer(), document)
        return SolveFingerprinting.sha256(canonical, DOCUMENT_SCHEMA).value
    }

    private fun decodeLegacyDocument(encoded: String): PortableConstraintProgrammingBendersState? {
        return runCatching {
            val legacy = documentJson.decodeFromString(LegacyDocument.serializer(), encoded)
            if (legacy.schemaVersion != DOCUMENT_SCHEMA || legacy.integritySha256.isBlank() ||
                legacy.integritySha256 != digest(legacy.copy(integritySha256 = ""))
            ) {
                return@runCatching null
            }
            PortableConstraintProgrammingBendersState(
                iteration = legacy.state.iteration,
                masterIncumbent = legacy.state.masterIncumbent,
                masterBestBound = legacy.state.masterBestBound,
                cuts = legacy.state.cuts,
                trace = legacy.state.trace,
                assumptions = legacy.state.assumptions,
                fixedBindings = legacy.state.fixedBindings,
                conflicts = legacy.state.conflicts,
                convergenceVerified = legacy.state.convergenceVerified,
                masterFingerprint = null,
                subproblemModelFingerprint = legacy.state.subproblemModelFingerprint
            )
        }.getOrNull()
    }

    private fun hasMasterState(state: PortableConstraintProgrammingBendersState): Boolean {
        return state.masterIncumbent != null || state.masterBestBound != null ||
            state.assumptions.isNotEmpty() || state.fixedBindings.isNotEmpty() ||
            state.conflicts.isNotEmpty() || state.convergenceVerified
    }
}

/**
 * Convert a report into portable Benders state using a domain SPI. /
 * 使用领域 SPI 将报告转换为可移植 Benders 状态。
 *
 * @param serializer Domain cut serializer. / 领域 cut 序列化器。
 * @param subproblemSnapshot Optional CP snapshot used to bind the state identity. / 可选的 CP snapshot，用于绑定状态身份。
 * @param masterFingerprint Optional domain master fingerprint. / 可选的领域主问题指纹。
 * @return Portable Benders state or a structured error. / 可移植 Benders 状态或结构化错误。
 */
fun LogicBasedBendersReport.toPortableBendersState(
    serializer: BendersCutSerializationSpi,
    subproblemSnapshot: ConstraintProgrammingModelSnapshot? = null,
    masterFingerprint: String? = null
): Ret<PortableConstraintProgrammingBendersState> {
    return BendersCheckpointCodec.capture(this, serializer, subproblemSnapshot, masterFingerprint)
}

/**
 * Computes the canonical fingerprint for a CP subproblem snapshot. /
 * 计算 CP 子问题 snapshot 的规范指纹。
 *
 * @param snapshot CP subproblem snapshot. / CP 子问题 snapshot。
 * @return Snapshot fingerprint or a structured encoding error. / snapshot 指纹或结构化编码错误。
 */
internal fun computeBendersSubproblemModelFingerprint(
    snapshot: ConstraintProgrammingModelSnapshot
): Ret<String> {
    val encoded = ConstraintProgrammingSnapshotCodec.encode(snapshot)
    return when (encoded) {
        is Ok -> ok(SolveFingerprinting.sha256(encoded.value!!).value)
        is Failed -> Failed(encoded.error)
        is Fatal -> Fatal(encoded.errors)
    }
}

private fun portableConflict(conflict: ConstraintProgrammingConflict): PortableConstraintProgrammingConflict {
    return PortableConstraintProgrammingConflict(
        validity = conflict.validity.name,
        minimality = conflict.minimality.name,
        memberIds = conflict.members.map(::portableMember).sorted(),
        assumptionIds = conflict.assumptions.mapNotNull { it.variableId?.value }.distinct().sorted(),
        provenance = buildMap {
            conflict.message?.let { put("message", it) }
            conflict.verificationChecks?.let { put("verificationChecks", it.toString()) }
            conflict.terminationReason?.let { put("terminationReason", it.name) }
        }
    )
}

private fun portableMember(member: InfeasibilityMember): String {
    return when (member) {
        is InfeasibilityMember.Constraint -> "constraint:${member.id.value}"
        is InfeasibilityMember.VariableBound -> "bound:${member.ref.variableId.value}:${member.ref.side.name}"
        is InfeasibilityMember.VariableDomain -> "domain:${member.ref.variableId.value}"
    }
}
