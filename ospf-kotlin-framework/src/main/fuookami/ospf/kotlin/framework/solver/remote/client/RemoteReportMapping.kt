package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.json.Json
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.solver.remote.domain.*

internal fun RemoteProblemStatus.toCoreStatus(): ProblemStatus {
    return when (this) {
        RemoteProblemStatus.FEASIBLE -> ProblemStatus.Feasible
        RemoteProblemStatus.INFEASIBLE -> ProblemStatus.Infeasible
        RemoteProblemStatus.UNBOUNDED -> ProblemStatus.Unbounded
        RemoteProblemStatus.INFEASIBLE_OR_UNBOUNDED -> ProblemStatus.InfeasibleOrUnbounded
        RemoteProblemStatus.UNKNOWN -> ProblemStatus.Unknown
    }
}

internal fun RemoteTerminationReason.toCoreReason(): TerminationReason {
    return when (this) {
        RemoteTerminationReason.COMPLETED -> TerminationReason.Completed
        RemoteTerminationReason.TIME_LIMIT -> TerminationReason.TimeLimit
        RemoteTerminationReason.NODE_LIMIT -> TerminationReason.NodeLimit
        RemoteTerminationReason.ITERATION_LIMIT -> TerminationReason.IterationLimit
        RemoteTerminationReason.SOLUTION_LIMIT -> TerminationReason.SolutionLimit
        RemoteTerminationReason.OBJECTIVE_LIMIT -> TerminationReason.ObjectiveLimit
        RemoteTerminationReason.CANCELLED -> TerminationReason.Cancelled
        RemoteTerminationReason.INTERRUPTED -> TerminationReason.Interrupted
        RemoteTerminationReason.NUMERICAL_FAILURE -> TerminationReason.NumericalFailure
        RemoteTerminationReason.BACKEND_FAILURE -> TerminationReason.BackendFailure
    }
}

internal fun RemoteSolutionPresence.toCorePresence(): SolutionPresence {
    return when (this) {
        RemoteSolutionPresence.NONE -> SolutionPresence.None
        RemoteSolutionPresence.INCUMBENT -> SolutionPresence.Incumbent
        RemoteSolutionPresence.OPTIMAL -> SolutionPresence.Optimal
    }
}

internal fun RemoteProofStatus.toCoreProof(): ProofStatus {
    return when (this) {
        RemoteProofStatus.NONE -> ProofStatus.None
        RemoteProofStatus.CLAIMED -> ProofStatus.Claimed
        RemoteProofStatus.VERIFIED -> ProofStatus.Verified
    }
}

internal fun String.asRemoteFingerprint(schemaVersion: String): AuditFingerprint {
    return AuditFingerprint(
        schemaVersion = schemaVersion,
        algorithm = "SHA-256",
        value = this
    )
}

internal fun Map<String, String>.remoteFlt64(key: String): Flt64? {
    return this[key]?.toDoubleOrNull()?.takeIf { it.isFinite() }?.let(::Flt64)
}

internal fun Map<String, String>.remoteULong(key: String): ULong? {
    return this[key]?.toULongOrNull()
}

private val SUPPORTED_LINEAR_QUADRATIC_SCHEMA_MAJORS = setOf(1, 2)

/**
 * Validate the backend-neutral linear/quadratic result envelope. /
 * 校验后端无关的线性/二次结果 envelope。
 *
 * Version 1 remains readable for legacy producers, while version 2 requires the complete
 * orthogonal state tuple and rejects future major versions. / 版本 1 保持旧生产端可读；版本 2
 * 要求完整的正交状态元组，并拒绝未来主版本。
 *
 * @return 协议校验结果 / Protocol validation result
 */
internal fun SolveResult.validateLinearQuadraticResult(): Try {
    val major = schemaVersion.substringBefore('.').toIntOrNull()
        ?: return Failed(ErrorCode.ORSolutionInvalid, "远程结果 schema 无效 / Invalid remote result schema")
    if (major !in SUPPORTED_LINEAR_QUADRATIC_SCHEMA_MAJORS) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "不支持的远程结果 schema 主版本：$schemaVersion / Unsupported remote result schema major: $schemaVersion"
        )
    }
    val strict = major >= 2
    if (strict && schemaVersion != "2.0") {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "不支持的远程结果 schema：$schemaVersion / Unsupported remote result schema: $schemaVersion"
        )
    }
    if (feasible && problemStatus != RemoteProblemStatus.FEASIBLE) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "远程结果问题状态与可行性不一致 / Remote result problem status disagrees with feasibility"
        )
    }
    if (!feasible && problemStatus == RemoteProblemStatus.FEASIBLE) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "不可行远程结果不得标记为 FEASIBLE / Infeasible remote result must not be FEASIBLE"
        )
    }
    if (optimal && (!feasible || (strict && proofStatus == RemoteProofStatus.NONE))) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "最优远程结果必须可行且带证明状态 / Optimal remote result must be feasible and carry proof status"
        )
    }
    val expectedPresence = when {
        optimal -> RemoteSolutionPresence.OPTIMAL
        feasible -> RemoteSolutionPresence.INCUMBENT
        else -> RemoteSolutionPresence.NONE
    }
    if (solutionPresence != expectedPresence) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "远程结果解存在性与可行性/最优性不一致 / Remote result solution presence disagrees with feasibility or optimality"
        )
    }
    if (!feasible && (objectiveValue != null || objectiveValueInt64 != null)) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "无解远程结果不得携带目标值 / A remote result without an incumbent must not carry an objective"
        )
    }
    if (strict && feasible && objectiveValue == null) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "严格可行远程结果缺少目标值 / Strict feasible remote result is missing an objective"
        )
    }
    if (objectiveValue != null && objectiveValueInt64 != null) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "远程结果不得同时携带浮点和 Int64 目标 / Remote result must not carry both floating and Int64 objectives"
        )
    }
    if (objectiveValueInt64 != null) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "线性/二次远程结果不得使用 Int64 目标字段 / Linear/quadratic results must not use the Int64 objective field"
        )
    }
    if (strict) {
        if (fingerprints.keys != fingerprintSchemas.keys || fingerprintSchemas.values.any(String::isBlank)) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "严格远程结果的指纹 schema 不完整 / Strict remote result fingerprint schemas are incomplete"
            )
        }
        if ((resultRef == null) != (artifactDigest == null)) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 resultRef 与摘要必须成对出现 / Remote resultRef and digest must be provided together"
            )
        }
        if (runId.isNullOrBlank() || attemptId.isNullOrBlank()) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "严格远程结果缺少 run/attempt 标识 / Strict remote result is missing run/attempt identifiers"
            )
        }
    }
    return ok
}

/**
 * Parse indexed remote issue fields without silently accepting malformed entries. /
 * 解析带索引的远程问题字段，不静默接受畸形条目。
 *
 * @param prefix 字段前缀 / Issue field prefix
 * @return 结构化问题列表或协议错误 / Structured issues or a protocol error
 */
internal fun Map<String, String>.toRemoteIssues(prefix: String): Ret<List<SolveIssue>> {
    val pattern = Regex("^${Regex.escape(prefix)}\\.(\\d+)\\.code$")
    val indexes = keys.mapNotNull { pattern.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
        .distinct()
        .sorted()
    val issues = ArrayList<SolveIssue>(indexes.size)
    for (index in indexes) {
        val base = "$prefix.$index"
        val code = this["$base.code"]
            ?: return Failed(ErrorCode.ORSolutionInvalid, "Remote issue code is missing: $base")
        val categoryName = this["$base.category"]
            ?: return Failed(ErrorCode.ORSolutionInvalid, "Remote issue category is missing: $base")
        val category = runCatching { SolveIssueCategory.valueOf(categoryName) }.getOrNull()
            ?: return Failed(ErrorCode.ORSolutionInvalid, "Remote issue category is invalid: $categoryName")
        val message = this["$base.message"]
            ?: return Failed(ErrorCode.ORSolutionInvalid, "Remote issue message is missing: $base")
        val details = entries
            .filter { (key, _) -> key.startsWith("$base.details.") }
            .associate { (key, value) -> key.removePrefix("$base.details.") to value }
        issues += SolveIssue(
            code = code,
            category = category,
            message = message,
            details = details
        )
    }
    val fieldPattern = Regex(
        "^${Regex.escape(prefix)}\\.\\d+\\.(code|category|message|details\\..+)$"
    )
    if (keys.any { key -> key.startsWith("$prefix.") && !fieldPattern.matches(key) }) {
        return Failed(ErrorCode.ORSolutionInvalid, "Remote issue fields are malformed")
    }
    return Ok(issues)
}

internal fun Map<String, String>.toRemoteDiagnostics(): Ret<SolveDiagnostics<Flt64>> {
    val allowed = setOf("infeasibility.", "warning.", "error.")
    if (keys.any { key -> allowed.none { key.startsWith(it) } }) {
        return Failed(ErrorCode.ORSolutionInvalid, "Remote diagnostic field is unknown")
    }
    return when (val warnings = toRemoteIssues("warning")) {
        is Ok -> when (val errors = toRemoteIssues("error")) {
            is Ok -> when (val evidence = toRemoteEvidence()) {
                is Ok -> Ok(
                    SolveDiagnostics(
                        infeasibilityEvidence = evidence.value,
                        warnings = warnings.value,
                        errors = errors.value
                    )
                )
                is Failed -> Failed(evidence.error)
                is Fatal -> Fatal(evidence.errors)
            }
            is Failed -> Failed(errors.error)
            is Fatal -> Fatal(errors.errors)
        }
        is Failed -> Failed(warnings.error)
        is Fatal -> Fatal(warnings.errors)
    }
}

/**
 * 校验线性/二次 raw result 与 resultRef artifact 的字段一致性。
 * Validate field agreement between a linear/quadratic raw result and its resultRef artifact.
 *
 * v1 artifact 允许缺少 v2 正交字段；v2 artifact 必须完整提供并逐字段匹配，避免
 * 一个来源单方面提升证明或覆盖终止状态。 / A v1 artifact may omit v2 orthogonal fields;
 * a v2 artifact must provide and match them field by field so neither source can unilaterally
 * strengthen proof or replace the termination state.
 *
 * @param serialized resultRef artifact / resultRef artifact
 * @return 校验结果 / Validation result
 */
internal fun SolveResult.validateLinearQuadraticArtifact(serialized: SerializedSolution): Try {
    val strict = serialized.schemaVersion.substringBefore('.').toIntOrNull()?.let { it >= 2 } == true
    if (serialized.schemaVersion != schemaVersion) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "远程 artifact schema 与 raw result 不一致 / Remote artifact schema disagrees with raw result"
        )
    }
    if (strict && ((resultRef == null) != (artifactDigest == null))) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "严格远程 resultRef 与摘要必须成对出现 / Strict remote resultRef and digest must be provided together"
        )
    }
    if (serialized.feasible != feasible || serialized.optimal != optimal) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "远程 artifact 可行性或最优性与 raw result 不一致 / Remote artifact feasibility or optimality disagrees with raw result"
        )
    }
    if (serialized.objectiveValue != objectiveValue || serialized.gap != gap) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "远程 artifact 目标或间隙与 raw result 不一致 / Remote artifact objective or gap disagrees with raw result"
        )
    }
    if (serialized.objectiveValueInt64 != null) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "线性/二次 artifact 不得携带 Int64 目标字段 / Linear/quadratic artifacts must not carry an Int64 objective"
        )
    }
    if (!serialized.feasible && serialized.objectiveValue != null) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "无解远程 artifact 不得携带目标值 / A remote artifact without a solution must not carry an objective"
        )
    }
    if (strict && serialized.feasible && serialized.objectiveValue == null) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "严格可行远程 artifact 缺少目标值 / Strict feasible remote artifact is missing an objective"
        )
    }
    if ((strict && (serialized.problemStatus != problemStatus ||
            serialized.solutionPresence != solutionPresence ||
            serialized.proofStatus != proofStatus ||
            serialized.terminationReason != terminationReason)) ||
        (!strict && ((serialized.problemStatus != null && serialized.problemStatus != problemStatus) ||
            (serialized.solutionPresence != null && serialized.solutionPresence != solutionPresence) ||
            (serialized.proofStatus != null && serialized.proofStatus != proofStatus) ||
            (serialized.terminationReason != null && serialized.terminationReason != terminationReason)))
    ) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "远程 artifact 正交状态与 raw result 不一致 / Remote artifact orthogonal state disagrees with raw result"
        )
    }
    if (serialized.provenance != provenance ||
        serialized.fingerprints != fingerprints ||
        serialized.fingerprintSchemas != fingerprintSchemas ||
        serialized.statistics != statistics ||
        serialized.diagnostics != diagnostics
    ) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "远程 artifact 审计字段与 raw result 不一致 / Remote artifact audit fields disagree with raw result"
        )
    }
    if ((strict && (serialized.runId != runId || serialized.attemptId != attemptId)) ||
        (!strict && ((serialized.runId != null && serialized.runId != runId) ||
            (serialized.attemptId != null && serialized.attemptId != attemptId)))
    ) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "远程 artifact run/attempt 与 raw result 不一致 / Remote artifact run/attempt disagrees with raw result"
        )
    }
    if (serialized.artifactDigest != artifactDigest) {
        return Failed(
            ErrorCode.ORSolutionInvalid,
            "远程 artifact 摘要与 raw result 不一致 / Remote artifact digest disagrees with raw result"
        )
    }
    return ok
}

private fun Map<String, String>.toRemoteEvidence(): Ret<InfeasibilityEvidence?> {
    if (keys.none { it.startsWith("infeasibility.") }) {
        return Ok(null)
    }
    fun required(name: String): String {
        return this["infeasibility.$name"] ?: ""
    }
    val source = runCatching { InfeasibilityEvidenceSource.valueOf(required("source")) }.getOrNull()
        ?: return Failed(ErrorCode.ORSolutionInvalid, "Remote infeasibility source is invalid")
    val exactness = runCatching { EvidenceExactness.valueOf(required("exactness")) }.getOrNull()
        ?: return Failed(ErrorCode.ORSolutionInvalid, "Remote infeasibility exactness is invalid")
    val completeness = runCatching { EvidenceCompleteness.valueOf(required("completeness")) }.getOrNull()
        ?: return Failed(ErrorCode.ORSolutionInvalid, "Remote infeasibility completeness is invalid")
    val validity = runCatching { EvidenceValidity.valueOf(required("validity")) }.getOrNull()
        ?: return Failed(ErrorCode.ORSolutionInvalid, "Remote infeasibility validity is invalid")
    val minimality = runCatching { EvidenceMinimality.valueOf(required("minimality")) }.getOrNull()
        ?: return Failed(ErrorCode.ORSolutionInvalid, "Remote infeasibility minimality is invalid")

    fun list(name: String): Ret<List<String>> {
        val encoded = this["infeasibility.$name"] ?: return Ok(emptyList())
        return try {
            Ok(Json.decodeFromString<List<String>>(encoded))
        } catch (_: Exception) {
            Failed(ErrorCode.ORSolutionInvalid, "Remote infeasibility list is malformed: $name")
        }
    }

    return when (val constraintIds = list("constraintIds")) {
        is Ok -> when (val bounds = list("variableBoundRefs")) {
            is Ok -> when (val domains = list("variableDomainRefs")) {
                is Ok -> when (val members = list("members")) {
                    is Ok -> when (val assumptions = list("assumptionIds")) {
                        is Ok -> {
                            val parsedBounds = bounds.value.map { value ->
                                val separator = value.lastIndexOf(':')
                                if (separator <= 0) {
                                    return Failed(ErrorCode.ORSolutionInvalid, "Remote variable bound reference is malformed")
                                }
                                val side = runCatching {
                                    BoundSide.valueOf(value.substring(separator + 1))
                                }.getOrNull() ?: return Failed(
                                    ErrorCode.ORSolutionInvalid,
                                    "Remote variable bound side is invalid"
                                )
                                VariableBoundRef(
                                    variableId = VariableId(value.substring(0, separator)),
                                    side = side
                                )
                            }.toSet()
                            val parsedDomains = domains.value.map { VariableDomainRef(VariableId(it)) }.toSet()
                            val parsedMembers = members.value.map { value ->
                                when {
                                    value.startsWith("constraint:") -> InfeasibilityMember.Constraint(
                                        ConstraintId(value.removePrefix("constraint:"))
                                    )
                                    value.startsWith("domain:") -> InfeasibilityMember.VariableDomain(
                                        VariableDomainRef(VariableId(value.removePrefix("domain:")))
                                    )
                                    value.startsWith("bound:") -> {
                                        val encoded = value.removePrefix("bound:")
                                        val separator = encoded.lastIndexOf(':')
                                        if (separator <= 0) {
                                            return Failed(ErrorCode.ORSolutionInvalid, "Remote infeasibility member is malformed")
                                        }
                                        val side = runCatching {
                                            BoundSide.valueOf(encoded.substring(separator + 1))
                                        }.getOrNull() ?: return Failed(
                                            ErrorCode.ORSolutionInvalid,
                                            "Remote infeasibility member bound side is invalid"
                                        )
                                        InfeasibilityMember.VariableBound(
                                            VariableBoundRef(VariableId(encoded.substring(0, separator)), side)
                                        )
                                    }
                                    else -> return Failed(
                                        ErrorCode.ORSolutionInvalid,
                                        "Remote infeasibility member type is unknown"
                                    )
                                }
                            }.toSet()
                            val unavailable = this["infeasibility.unavailable.code"]?.let {
                                val category = runCatching {
                                    SolveIssueCategory.valueOf(required("unavailable.category"))
                                }.getOrNull() ?: return Failed(
                                    ErrorCode.ORSolutionInvalid,
                                    "Remote unavailable diagnostic category is invalid"
                                )
                                SolveIssue(
                                    code = it,
                                    category = category,
                                    message = required("unavailable.message")
                                        .takeUnless(String::isBlank)
                                        ?: return Failed(
                                            ErrorCode.ORSolutionInvalid,
                                            "Remote unavailable diagnostic message is missing"
                                        )
                                )
                            }
                            val termination = this["infeasibility.terminationReason"]?.let {
                                runCatching { TerminationReason.valueOf(it) }.getOrNull()
                                    ?: return Failed(ErrorCode.ORSolutionInvalid, "Remote diagnostic termination is invalid")
                            }
                            Ok(
                                InfeasibilityEvidence(
                                    source = source,
                                    exactness = exactness,
                                    completeness = completeness,
                                    constraintIds = constraintIds.value.map(::ConstraintId).toSet(),
                                    variableBoundIds = parsedBounds.map { it.variableId }.toSet(),
                                    elapsed = this["infeasibility.elapsedMs"]?.toLongOrNull()?.milliseconds,
                                    unavailableReason = unavailable,
                                    validity = validity,
                                    minimality = minimality,
                                    variableBoundRefs = parsedBounds,
                                    variableDomainRefs = parsedDomains,
                                    members = parsedMembers,
                                    reference = this["infeasibility.reference"],
                                    assumptionIds = assumptions.value.map(::VariableId).toSet(),
                                    verificationChecks = this["infeasibility.verificationChecks"]
                                        ?.toULongOrNull()
                                        ?.let(::UInt64),
                                    terminationReason = termination
                                )
                            )
                        }
                        is Failed -> Failed(assumptions.error)
                        is Fatal -> Fatal(assumptions.errors)
                    }
                    is Failed -> Failed(members.error)
                    is Fatal -> Fatal(members.errors)
                }
                is Failed -> Failed(domains.error)
                is Fatal -> Fatal(domains.errors)
            }
            is Failed -> Failed(bounds.error)
            is Fatal -> Fatal(bounds.errors)
        }
        is Failed -> Failed(constraintIds.error)
        is Fatal -> Fatal(constraintIds.errors)
    }
}

internal fun SolveResult.toRemoteSolveReport(
    output: SolveReport<Flt64>?,
    modelTypes: Set<SolverModelType>
): Ret<SolveReport<Flt64>> {
    return when (val diagnostics = diagnostics.toRemoteDiagnostics()) {
        is Failed -> Failed(diagnostics.error)
        is Fatal -> Fatal(diagnostics.errors)
        is Ok -> {
            val fingerprint = { key: String ->
                fingerprints[key]?.let { value ->
                    value.asRemoteFingerprint(fingerprintSchemas[key] ?: schemaVersion)
                }
            }
            Ok(
                SolveReport(
                    schemaVersion = schemaVersion,
                    runId = runId?.let(::SolveRunId),
                    problemStatus = problemStatus.toCoreStatus(),
                    terminationReason = terminationReason.toCoreReason(),
                    solutionPresence = solutionPresence.toCorePresence(),
                    solution = output?.let {
                        SolveSolution(values = it.values, objective = it.solution?.objective)
                    },
                    proof = SolveProof(
                        status = proofStatus.toCoreProof(),
                        kind = provenance["proof.kind"]
                    ),
                    statistics = SolveStatistics(
                        solveTime = elapsed,
                        iterations = statistics.remoteULong("iterations"),
                        nodes = statistics.remoteULong("nodes"),
                        bestBound = output?.bestBound ?: statistics.remoteFlt64("bestBound"),
                        gap = gap ?: statistics.remoteFlt64("gap")
                    ),
                    diagnostics = diagnostics.value,
                    provenance = SolverProvenance(
                        descriptor = SolverDescriptor(
                            solverId = provenance["solverId"] ?: "remote",
                            backendName = provenance["backend"] ?: "remote",
                            backendVersion = provenance["backendVersion"],
                            pluginVersion = provenance["pluginVersion"],
                            capabilities = SolverCapabilities(modelTypes = modelTypes)
                        ),
                        nativeVersion = provenance["nativeVersion"],
                        effectiveParameters = provenance
                            .filterKeys { it.startsWith("parameter.") }
                            .mapKeys { it.key.removePrefix("parameter.") },
                        threadCount = provenance["threads"]?.toIntOrNull(),
                        randomSeed = provenance["randomSeed"]?.toLongOrNull(),
                        deterministic = provenance["deterministic"]?.toBooleanStrictOrNull(),
                        environmentSummary = provenance
                            .filterKeys { it.startsWith("environment.") }
                            .mapKeys { it.key.removePrefix("environment.") }
                    ),
                    fingerprints = SolveFingerprints(
                        model = fingerprint("model"),
                        configuration = fingerprint("configuration"),
                        solver = fingerprint("solver")
                    )
                )
            )
        }
    }
}
