/** Backend-neutral infeasibility analysis contracts. / 后端无关的不可行分析契约。 */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.iis

import kotlin.time.Clock
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModel
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.solver.AbstractQuadraticSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature
import fuookami.ospf.kotlin.core.solver.constraint_programming.constraintProgrammingSupport
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.EvidenceCompleteness
import fuookami.ospf.kotlin.core.solver.report.EvidenceExactness
import fuookami.ospf.kotlin.core.solver.report.EvidenceMinimality
import fuookami.ospf.kotlin.core.solver.report.EvidenceValidity
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidence
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidenceSource
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.SolveIssue
import fuookami.ospf.kotlin.core.solver.report.SolveIssueCategory
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.SolverCapabilities
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.VariableBoundRef
import fuookami.ospf.kotlin.core.solver.report.VariableDomainRef
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.diagnosticConstraintId
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/** 后端无关的不可行证据分析器。 / Backend-neutral infeasibility evidence analyzer. */
interface InfeasibilityAnalyzer<M> {
    /** 证据来源 / Evidence source */
    val source: InfeasibilityEvidenceSource

    /**
     * 分析一个已证明不可行的模型。 / Analyze a model proven infeasible.
     *
     * @param model 待分析模型 / Model to analyze
     * @return 结构化证据或错误 / Structured evidence or an error
     */
    suspend fun analyze(model: M): Ret<InfeasibilityEvidence>
}

/**
 * Materialized diagnostic result retained by compatibility facades. / 兼容 facade 保留的物化诊断结果。
 *
 * @property evidence 结构化不可行证据 / Structured infeasibility evidence
 * @property artifact 兼容 artifact / Compatibility artifact
 */
data class MaterializedInfeasibilityEvidence<A>(
    val evidence: InfeasibilityEvidence,
    val artifact: A?
)

/** Analyzer that can also return a legacy model artifact. / 同时能返回旧模型 artifact 的分析器。 */
interface MaterializingInfeasibilityAnalyzer<M, A> : InfeasibilityAnalyzer<M> {
    /**
     * 分析模型并保留兼容 artifact。 / Analyze a model and retain a compatibility artifact.
     *
     * @param model 待分析模型 / Model to analyze
     * @return 物化证据或错误 / Materialized evidence or an error
     */
    suspend fun analyzeMaterialized(model: M): Ret<MaterializedInfeasibilityEvidence<A>>
}

/**
 * 不可行分析器的 capability 声明。 / Capability declaration for an infeasibility analyzer.
 *
 * @property modelTypes 支持的模型类型 / Supported model types
 * @property exact 是否提供精确证据 / Whether exact evidence is provided
 * @property source 证据来源 / Evidence source
 */
data class InfeasibilityAnalyzerCapabilities(
    val modelTypes: Set<SolverModelType> = emptySet(),
    val exact: Boolean = false,
    val source: InfeasibilityEvidenceSource
)

/** 能够参与策略选择的分析器。 / Analyzer participating in capability-based selection. */
interface CapabilityAwareInfeasibilityAnalyzer<M> : InfeasibilityAnalyzer<M> {
    /** Analyzer capabilities used for strategy selection. / 用于策略选择的分析器能力。 */
    val capabilities: InfeasibilityAnalyzerCapabilities
}

/** Native IIS analyzer marker. / 原生 IIS 分析器标记接口。 */
interface NativeIISAnalyzer<M> : CapabilityAwareInfeasibilityAnalyzer<M> {
    override val source: InfeasibilityEvidenceSource
        get() = InfeasibilityEvidenceSource.NativeIIS
}

/** Farkas analyzer marker. / Farkas 分析器标记接口。 */
interface FarkasInfeasibilityAnalyzer<M> : CapabilityAwareInfeasibilityAnalyzer<M> {
    override val source: InfeasibilityEvidenceSource
        get() = InfeasibilityEvidenceSource.Farkas
}

/**
 * 将 backend 原生诊断函数接入统一 SPI。 / Adapt a backend-native diagnostic function to the common SPI.
 *
 * @property capabilities 分析器能力 / Analyzer capabilities
 * @property delegate 原生分析函数 / Native analysis function
 */
class DelegatingInfeasibilityAnalyzer<M>(
    override val capabilities: InfeasibilityAnalyzerCapabilities,
    private val delegate: suspend (M) -> Ret<InfeasibilityEvidence>
) : CapabilityAwareInfeasibilityAnalyzer<M> {
    override val source: InfeasibilityEvidenceSource
        get() = capabilities.source

    override suspend fun analyze(model: M): Ret<InfeasibilityEvidence> {
        return delegate(model)
    }
}

/**
 * Native IIS delegate adapter. / 原生 IIS 委托适配器。
 *
 * @param modelTypes 支持的模型类型 / Supported model types
 * @property delegate 原生 IIS 分析函数 / Native IIS analysis function
 */
class NativeIISAnalyzerAdapter<M>(
    modelTypes: Set<SolverModelType> = emptySet(),
    private val delegate: suspend (M) -> Ret<InfeasibilityEvidence>
) : NativeIISAnalyzer<M> {
    override val capabilities: InfeasibilityAnalyzerCapabilities = InfeasibilityAnalyzerCapabilities(
        modelTypes = modelTypes,
        exact = true,
        source = InfeasibilityEvidenceSource.NativeIIS
    )

    override suspend fun analyze(model: M): Ret<InfeasibilityEvidence> {
        return delegate(model)
    }
}

/**
 * Farkas delegate adapter. / Farkas 委托适配器。
 *
 * @param modelTypes 支持的模型类型 / Supported model types
 * @property delegate Farkas 分析函数 / Farkas analysis function
 */
class FarkasAnalyzerAdapter<M>(
    modelTypes: Set<SolverModelType> = emptySet(),
    private val delegate: suspend (M) -> Ret<InfeasibilityEvidence>
) : FarkasInfeasibilityAnalyzer<M> {
    override val capabilities: InfeasibilityAnalyzerCapabilities = InfeasibilityAnalyzerCapabilities(
        modelTypes = modelTypes,
        exact = true,
        source = InfeasibilityEvidenceSource.Farkas
    )

    override suspend fun analyze(model: M): Ret<InfeasibilityEvidence> {
        return delegate(model)
    }
}

/**
 * 无可用精确/降级策略时返回结构化 unavailable 证据。 / / Return structured unavailable evidence when no exact or fallback strategy is available.
 *
 * @property issue unavailable 原因 / Unavailable reason
 */
class UnavailableInfeasibilityAnalyzer<M>(
    private val issue: SolveIssue = SolveIssue(
        code = "infeasibility-analysis-unavailable",
        category = SolveIssueCategory.Unsupported,
        message = "没有可用的不可行诊断路径 / No infeasibility diagnostic path is available"
    )
) : InfeasibilityAnalyzer<M> {
    override val source: InfeasibilityEvidenceSource = InfeasibilityEvidenceSource.None

    override suspend fun analyze(model: M): Ret<InfeasibilityEvidence> {
        return ok(
            InfeasibilityEvidence(
                source = source,
                exactness = EvidenceExactness.Unknown,
                completeness = EvidenceCompleteness.Unavailable,
                validity = EvidenceValidity.Unknown,
                minimality = EvidenceMinimality.NotChecked,
                unavailableReason = issue
            )
        )
    }
}

/** 诊断策略顺序。 / Ordered diagnostic strategy. */
enum class InfeasibilityDiagnosticStrategy {
    NativeIIS,
    Farkas,
    ConstraintConflict,
    Legacy
}

/**
 * 按 solver capability 选择 native/Farkas/conflict/legacy 的后端无关编排器。 / / Backend-neutral orchestrator selecting native, Farkas, conflict, and legacy analyzers.
 *
 * @property analyzers 候选分析器 / Candidate analyzers
 * @property solverCapabilities 求解器能力 / Solver capabilities
 */
class InfeasibilityDiagnosticOrchestrator<M>(
    private val analyzers: List<InfeasibilityAnalyzer<M>>,
    private val solverCapabilities: SolverCapabilities? = null
) {
    /**
     * 按策略顺序返回可用分析器。 / Return analyzers available in strategy order.
     *
     * @return 按优先级排序的分析器 / Analyzers ordered by priority
     */
    fun availableAnalyzers(): List<InfeasibilityAnalyzer<M>> {
        val preferred = listOf(
            InfeasibilityEvidenceSource.NativeIIS,
            InfeasibilityEvidenceSource.Farkas,
            InfeasibilityEvidenceSource.ConstraintConflict,
            InfeasibilityEvidenceSource.ElasticFilter,
            InfeasibilityEvidenceSource.DeletionFilter
        )
        return analyzers
            .filter { analyzer ->
                val capability = analyzer as? CapabilityAwareInfeasibilityAnalyzer<M>
                capability == null || supports(capability.capabilities)
            }
            .sortedBy { analyzer -> preferred.indexOf(analyzer.source).takeIf { it >= 0 } ?: Int.MAX_VALUE }
    }

    /**
     * 执行首个成功的诊断策略。 / Execute the first successful diagnostic strategy.
     *
     * @param model 待分析模型 / Model to analyze
     * @return 结构化证据或错误 / Structured evidence or an error
     */
    suspend fun analyze(model: M): Ret<InfeasibilityEvidence> {
        val available = availableAnalyzers()
        return if (available.isEmpty()) {
            UnavailableInfeasibilityAnalyzer<M>().analyze(model)
        } else {
            InfeasibilityAnalyzerChain(available).analyze(model)
        }
    }

    /**
     * / 执行选中的分析器，并保留可选的兼容 artifact。 / Execute the selected analyzer while preserving an optional compatibility artifact.
     *
     * @param model 待分析模型 / Model to analyze
     * @return 物化证据或错误 / Materialized evidence or an error
     */
    suspend fun <A> analyzeMaterialized(model: M): Ret<MaterializedInfeasibilityEvidence<A>> {
        val available = availableAnalyzers()
        var last: Ret<MaterializedInfeasibilityEvidence<A>>? = null
        for (analyzer in available) {
            @Suppress("UNCHECKED_CAST")
            val materializer = analyzer as? MaterializingInfeasibilityAnalyzer<M, A>
            val result: Ret<MaterializedInfeasibilityEvidence<A>> = if (materializer != null) {
                materializer.analyzeMaterialized(model)
            } else {
                when (val evidence = analyzer.analyze(model)) {
                    is Ok -> ok(MaterializedInfeasibilityEvidence<A>(evidence.value, null))
                    is Failed -> Failed(evidence.error)
                    is Fatal -> Fatal(evidence.errors)
                }
            }
            when (result) {
                is Ok -> return result
                is Failed -> last = result
                is Fatal -> return result
            }
        }
        return last ?: UnavailableInfeasibilityAnalyzer<M>().analyze(model).map {
            MaterializedInfeasibilityEvidence(it, null)
        }
    }

    /**
     * 将诊断结果附加到已有报告；失败只进入 diagnostics，不改变原求解结论。 / Attach diagnostic results without changing the original solve conclusion on failure.
     *
     * @param report Existing solve report. / 已有求解报告。
     * @param model Model to analyze. / 待分析模型。
     * @return Report with diagnostic evidence or mapped errors. / 附加诊断证据或错误映射后的报告。
     */
    suspend fun <V> analyzeInto(
        report: SolveReport<V>,
        model: M
    ): SolveReport<V> {
        return when (val result = analyze(model)) {
            is Ok -> report.copy(
                diagnostics = report.diagnostics.copy(
                    infeasibilityEvidence = result.value
                )
            )
            is Failed -> report.copy(
                diagnostics = report.diagnostics.copy(
                    errors = report.diagnostics.errors + result.error.toIssue()
                )
            )
            is Fatal -> report.copy(
                diagnostics = report.diagnostics.copy(
                    errors = report.diagnostics.errors + result.errors.map { it.toIssue() }
                )
            )
        }
    }

    private fun supports(capability: InfeasibilityAnalyzerCapabilities): Boolean {
        val solver = solverCapabilities ?: return true
        if (capability.source == InfeasibilityEvidenceSource.NativeIIS && !solver.nativeIIS) {
            return false
        }
        if (capability.source == InfeasibilityEvidenceSource.Farkas && !solver.farkas) {
            return false
        }
        if (capability.source == InfeasibilityEvidenceSource.ConstraintConflict &&
            solver.constraintProgrammingSupport(ConstraintProgrammingFeature.ConflictCore) ==
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.Unsupported
        ) {
            return false
        }
        return capability.modelTypes.isEmpty() ||
            solver.modelTypes.isEmpty() ||
            capability.modelTypes.any { it in solver.modelTypes }
    }
}

/** 诊断错误到结构化报告问题的映射。 / Map a diagnostic error to a structured report issue. */
private fun fuookami.ospf.kotlin.utils.error.Error<ErrorCode>.toIssue(): SolveIssue {
    return SolveIssue(
        code = code.toString(),
        category = SolveIssueCategory.Backend,
        message = message
    )
}

/** Stable variable identity and component members reconstructed from a legacy IIS artifact. / 从旧 IIS artifact 重建稳定变量身份和组件成员。 */
private data class LegacyEvidenceComponents(
    val variableBoundRefs: Set<VariableBoundRef>,
    val variableDomainRefs: Set<VariableDomainRef>,
    val members: Set<InfeasibilityMember>
)

/** Stable constraint identities reconstructed from original row origins. / 从原始行来源重建稳定约束身份。 */
private data class LegacyConstraintIds(
    val ids: Set<ConstraintId>,
    val unmappedRows: List<Int>
)

private fun <C : Any> legacyConstraintIds(
    originalOrigins: List<C?>,
    artifactOrigins: List<C?>,
    idForOriginal: (Int) -> ConstraintId,
    rowMatches: ((originalIndex: Int, artifactIndex: Int) -> Boolean)? = null
): LegacyConstraintIds {
    val ids = linkedSetOf<ConstraintId>()
    val unmappedRows = ArrayList<Int>()
    val mappedOriginalIndices = HashSet<Int>()
    artifactOrigins.forEachIndexed { row, artifactOrigin ->
        val originIndex = artifactOrigin?.let { origin ->
            originalOrigins.indexOfFirst { it === origin }
        } ?: -1
        val originalIndex = if (originIndex >= 0) {
            originIndex
        } else {
            rowMatches?.let { matches ->
                originalOrigins.indices.firstOrNull { candidate ->
                    candidate !in mappedOriginalIndices && matches(candidate, row)
                }
            } ?: -1
        }
        if (originalIndex >= 0) {
            mappedOriginalIndices += originalIndex
            ids += idForOriginal(originalIndex)
        } else {
            unmappedRows += row
        }
    }
    return LegacyConstraintIds(ids, unmappedRows)
}

private fun legacyIisIssue(
    unmappedRows: List<Int>,
    unmappedVariables: List<Int> = emptyList(),
    quadratic: Boolean = false
): SolveIssue {
    val details = linkedMapOf<String, String>()
    if (unmappedRows.isNotEmpty()) {
        details["unmappedArtifactRows"] = unmappedRows.joinToString(",")
    }
    if (unmappedVariables.isNotEmpty()) {
        details["unmappedArtifactVariables"] = unmappedVariables.joinToString(",")
    }
    return SolveIssue(
        code = "legacy-iis-heuristic",
        category = SolveIssueCategory.Unsupported,
        message = if (quadratic) {
            "Legacy quadratic IIS filtering is not independently verified / " +
                "旧二次 IIS 过滤结果未经过独立复验"
        } else {
            "Legacy IIS filtering is not an independently verified IIS / " +
                "旧 IIS 过滤结果未经过独立 IIS 复验"
        },
        details = details
    )
}

private fun legacyVariableId(variable: Variable, originalIndex: Int? = null): VariableId {
    val origin = variable.origin
    return VariableId(
        origin?.let { "${it.identifier}:${it.index}" }
            ?: originalIndex?.let { "model-local-variable:$it" }
            ?: "model-local-artifact-variable:${variable.index}"
    )
}

private data class LegacyVariableIds(
    val idsByArtifactIndex: Map<Int, VariableId>,
    val unmappedVariables: List<Int>
)

private fun legacyVariableIds(
    originalVariables: List<Variable>,
    artifactVariables: List<Variable>
): LegacyVariableIds {
    val ids = linkedMapOf<Int, VariableId>()
    val mappedOriginalIndices = HashSet<Int>()
    val unmappedVariables = ArrayList<Int>()
    artifactVariables.forEach { artifactVariable ->
        val originIndex = artifactVariable.origin?.let { origin ->
            originalVariables.indexOfFirst { it.origin === origin }
        } ?: -1
        val sameNameAndTypeAmount = originalVariables.count {
            it.name == artifactVariable.name && it.type::class == artifactVariable.type::class
        }
        val directIndex = if (
            originIndex < 0 &&
            sameNameAndTypeAmount == 1 &&
            artifactVariable.index in originalVariables.indices &&
            artifactVariable.index !in mappedOriginalIndices
        ) {
            artifactVariable.index
        } else {
            -1
        }
        val originalIndex = when {
            originIndex >= 0 -> originIndex
            directIndex >= 0 && originalVariables[directIndex].name == artifactVariable.name -> directIndex
            else -> originalVariables.indices.firstOrNull { candidate ->
                candidate !in mappedOriginalIndices &&
                    originalVariables[candidate].name == artifactVariable.name &&
                    originalVariables[candidate].type::class == artifactVariable.type::class
            } ?: -1
        }
        if (originalIndex >= 0) {
            mappedOriginalIndices += originalIndex
            ids[artifactVariable.index] = legacyVariableId(
                variable = originalVariables[originalIndex],
                originalIndex = originalIndex
            )
        } else {
            unmappedVariables += artifactVariable.index
            ids[artifactVariable.index] = legacyVariableId(artifactVariable)
        }
    }
    return LegacyVariableIds(ids, unmappedVariables)
}

private fun legacyEvidenceComponents(
    variables: List<Variable>,
    constraintIds: Set<ConstraintId>,
    variableIds: Map<Int, VariableId>
): LegacyEvidenceComponents {
    val bounds = variables.flatMapTo(linkedSetOf()) { variable ->
        val id = variableIds[variable.index] ?: legacyVariableId(variable)
        buildList {
            if (variable.lowerBound != Flt64.negativeInfinity) {
                add(VariableBoundRef(id, BoundSide.Lower))
            }
            if (variable.upperBound != Flt64.infinity) {
                add(VariableBoundRef(id, BoundSide.Upper))
            }
        }
    }
    val domains = variables
        .filter { it.type.isIntegerType }
        .mapTo(linkedSetOf()) {
            VariableDomainRef(variableIds[it.index] ?: legacyVariableId(it))
        }
    val members = linkedSetOf<InfeasibilityMember>().apply {
        constraintIds.forEach { add(InfeasibilityMember.Constraint(it)) }
        bounds.forEach { add(InfeasibilityMember.VariableBound(it)) }
        domains.forEach { add(InfeasibilityMember.VariableDomain(it)) }
    }
    return LegacyEvidenceComponents(bounds, domains, members)
}

/** Match a legacy linear row without relying on display names. / 不依赖展示名称匹配旧线性行。 */
private fun sameLinearConstraint(
    original: LinearConstraintBatch,
    originalIndex: Int,
    artifact: LinearConstraintBatch,
    artifactIndex: Int
): Boolean {
    if (original.signs[originalIndex] != artifact.signs[artifactIndex] ||
        original.rhs[originalIndex] != artifact.rhs[artifactIndex]
    ) {
        return false
    }
    val originalRow = original.lhs[originalIndex]
    val artifactRow = artifact.lhs[artifactIndex]
    return originalRow.size == artifactRow.size && originalRow.zip(artifactRow).all { (left, right) ->
        left.colIndex == right.colIndex && left.coefficient == right.coefficient
    }
}

/** Match a legacy quadratic row without relying on display names. / 不依赖展示名称匹配旧二次行。 */
private fun sameQuadraticConstraint(
    original: QuadraticConstraintBatch,
    originalIndex: Int,
    artifact: QuadraticConstraintBatch,
    artifactIndex: Int
): Boolean {
    if (original.signs[originalIndex] != artifact.signs[artifactIndex] ||
        original.rhs[originalIndex] != artifact.rhs[artifactIndex]
    ) {
        return false
    }
    val originalRow = original.lhs[originalIndex]
    val artifactRow = artifact.lhs[artifactIndex]
    return originalRow.size == artifactRow.size && originalRow.zip(artifactRow).all { (left, right) ->
        left.colIndex1 == right.colIndex1 &&
            left.colIndex2 == right.colIndex2 &&
            left.coefficient == right.coefficient
    }
}

/**
 * / 基于求解器已验证 conflict 输出的 CP 不可行分析器。 / CP conflict analyzer backed by a solver's verified conflict output.
 *
 * @property solver CP 求解器 / CP solver
 * @property options CP 求解选项 / CP solve options
 */
class ConstraintProgrammingConflictAnalyzer(
    private val solver: ConstraintProgrammingSolver,
    private val options: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions()
) : CapabilityAwareInfeasibilityAnalyzer<ConstraintProgrammingModel> {
    override val source: InfeasibilityEvidenceSource = InfeasibilityEvidenceSource.ConstraintConflict
    override val capabilities: InfeasibilityAnalyzerCapabilities = InfeasibilityAnalyzerCapabilities(
        modelTypes = setOf(SolverModelType.CP, SolverModelType.MIP),
        exact = true,
        source = source
    )

    override suspend fun analyze(model: ConstraintProgrammingModel): Ret<InfeasibilityEvidence> {
        val result = solver.solve(
            model,
            options.copy(
                collectConflict = true,
                shrinkConflict = options.shrinkConflict
            )
        )
        if (result.failed) {
            return propagate(result)
        }
        val output = (result as Ok).value
        if (output !is ConstraintProgrammingInfeasibleOutput) {
            return Failed(
                ErrorCode.Other,
                "CP 模型未返回不可行输出 / CP model did not return an infeasible output"
            )
        }
        return output.report?.diagnostics?.infeasibilityEvidence?.let(::ok)
            ?: Failed(
                ErrorCode.Other,
                "CP 输出缺少结构化 conflict 证据 / CP output has no structured conflict evidence"
            )
    }
}

/**
 * / 保留旧弹性/删除过滤算法并明确标记为启发式证据的适配器。 / Legacy elastic/deletion IIS adapter with an explicit heuristic evidence grade.
 *
 * @property solver 线性求解器 / Linear solver
 * @property config IIS 配置 / IIS configuration
 */
class LegacyElasticInfeasibilityAnalyzer(
    private val solver: AbstractLinearSolver,
    private val config: IISConfig = IISConfig()
) : CapabilityAwareInfeasibilityAnalyzer<LinearTriadModelView>,
    MaterializingInfeasibilityAnalyzer<LinearTriadModelView, fuookami.ospf.kotlin.core.solver.iis.LinearIISModel> {
    override val source: InfeasibilityEvidenceSource = InfeasibilityEvidenceSource.ElasticFilter
    override val capabilities: InfeasibilityAnalyzerCapabilities = InfeasibilityAnalyzerCapabilities(
        modelTypes = setOf(SolverModelType.LP, SolverModelType.MIP),
        exact = false,
        source = source
    )

    override suspend fun analyze(model: LinearTriadModelView): Ret<InfeasibilityEvidence> {
        return analyzeMaterialized(model).map { it.evidence }
    }

    override suspend fun analyzeMaterialized(
        model: LinearTriadModelView
    ): Ret<MaterializedInfeasibilityEvidence<LinearIISModel>> {
        val started = Clock.System.now()
        return try {
            val result = computeLegacyIIS(model, solver, config)
            if (result.failed) {
                return propagate(result)
            }
            val iis = (result as Ok).value
            val constraintMapping = legacyConstraintIds(
                originalOrigins = model.constraints.origins,
                artifactOrigins = iis.constraints.origins,
                idForOriginal = model::diagnosticConstraintId,
                rowMatches = { originalIndex, artifactIndex ->
                    sameLinearConstraint(model.constraints, originalIndex, iis.constraints, artifactIndex)
                }
            )
            val constraintIds = constraintMapping.ids
            val variableMapping = legacyVariableIds(model.variables, iis.variables)
            val components = legacyEvidenceComponents(iis.variables, constraintIds, variableMapping.idsByArtifactIndex)
            val source = if (iis.relaxedFeasible) {
                InfeasibilityEvidenceSource.ElasticFilter
            } else {
                InfeasibilityEvidenceSource.DeletionFilter
            }
            ok(
                MaterializedInfeasibilityEvidence(
                    evidence = InfeasibilityEvidence(
                        source = source,
                        exactness = EvidenceExactness.Heuristic,
                        completeness = if (iis.relaxedFeasible) {
                            EvidenceCompleteness.Complete
                        } else {
                            EvidenceCompleteness.Partial
                        },
                        constraintIds = constraintIds,
                        variableBoundIds = components.variableBoundRefs.mapTo(linkedSetOf()) { it.variableId },
                        elapsed = Clock.System.now() - started,
                        validity = EvidenceValidity.Heuristic,
                        minimality = EvidenceMinimality.NotChecked,
                        variableBoundRefs = components.variableBoundRefs,
                        variableDomainRefs = components.variableDomainRefs,
                        members = components.members,
                        unavailableReason = legacyIisIssue(
                            unmappedRows = constraintMapping.unmappedRows,
                            unmappedVariables = variableMapping.unmappedVariables
                        )
                    ),
                    artifact = iis
                )
            )
        } catch (error: Throwable) {
            Failed(
                ErrorCode.OREngineSolvingException,
                "Legacy IIS 分析失败：${error.message ?: error::class.simpleName} / " +
                    "Legacy IIS analysis failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }
}

/**
 * Legacy quadratic IIS adapter with an explicit heuristic evidence grade. / 带明确启发式等级的旧二次 IIS 适配器。
 *
 * @property solver 二次求解器 / Quadratic solver
 * @property config IIS 配置 / IIS configuration
 */
class LegacyElasticQuadraticInfeasibilityAnalyzer(
    private val solver: AbstractQuadraticSolver,
    private val config: IISConfig = IISConfig()
) : CapabilityAwareInfeasibilityAnalyzer<QuadraticTetradModelView>,
    MaterializingInfeasibilityAnalyzer<QuadraticTetradModelView, QuadraticTetradModel> {
    override val source: InfeasibilityEvidenceSource = InfeasibilityEvidenceSource.ElasticFilter
    override val capabilities: InfeasibilityAnalyzerCapabilities = InfeasibilityAnalyzerCapabilities(
        modelTypes = setOf(SolverModelType.QP, SolverModelType.QCP),
        exact = false,
        source = source
    )

    override suspend fun analyze(model: QuadraticTetradModelView): Ret<InfeasibilityEvidence> {
        return analyzeMaterialized(model).map { it.evidence }
    }

    override suspend fun analyzeMaterialized(
        model: QuadraticTetradModelView
    ): Ret<MaterializedInfeasibilityEvidence<QuadraticTetradModel>> {
        val started = Clock.System.now()
        return try {
            val result = computeLegacyIIS(model, solver, config)
            if (result.failed) return propagate(result)
            val legacyResult = result.value!!
            val iis = legacyResult.model
            val constraintMapping = legacyConstraintIds(
                originalOrigins = model.constraints.origins,
                artifactOrigins = iis.constraints.origins,
                idForOriginal = model::diagnosticConstraintId,
                rowMatches = { originalIndex, artifactIndex ->
                    sameQuadraticConstraint(model.constraints, originalIndex, iis.constraints, artifactIndex)
                }
            )
            val constraintIds = constraintMapping.ids
            val variableMapping = legacyVariableIds(model.variables, iis.variables)
            val components = legacyEvidenceComponents(iis.variables, constraintIds, variableMapping.idsByArtifactIndex)
            val evidence = InfeasibilityEvidence(
                source = legacyResult.source,
                exactness = EvidenceExactness.Heuristic,
                completeness = EvidenceCompleteness.Partial,
                constraintIds = constraintIds,
                variableBoundIds = components.variableBoundRefs.mapTo(linkedSetOf()) { it.variableId },
                elapsed = Clock.System.now() - started,
                validity = EvidenceValidity.Heuristic,
                minimality = EvidenceMinimality.NotChecked,
                variableBoundRefs = components.variableBoundRefs,
                variableDomainRefs = components.variableDomainRefs,
                members = components.members,
                unavailableReason = legacyIisIssue(
                    unmappedRows = constraintMapping.unmappedRows,
                    unmappedVariables = variableMapping.unmappedVariables,
                    quadratic = true
                )
            )
            ok(MaterializedInfeasibilityEvidence(evidence = evidence, artifact = iis))
        } catch (error: Throwable) {
            Failed(
                ErrorCode.OREngineSolvingException,
                "Legacy quadratic IIS 分析失败：${error.message ?: error::class.simpleName} / " +
                    "Legacy quadratic IIS analysis failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }
}

/**
 * 按顺序尝试的不可行分析器降级链。 / Ordered analyzer fallback chain.
 *
 * @property analyzers 按顺序排列的分析器 / Ordered analyzers
 */
class InfeasibilityAnalyzerChain<M>(
    private val analyzers: List<InfeasibilityAnalyzer<M>>
) {
    /**
     * 按声明顺序尝试分析器。 / Try analyzers in declaration order.
     *
     * @param model 待分析模型 / Model to analyze
     * @return 首个成功证据或最后一个错误 / First evidence or the last error
     */
    suspend fun analyze(model: M): Ret<InfeasibilityEvidence> {
        var last: Ret<InfeasibilityEvidence>? = null
        for (analyzer in analyzers) {
            val result = analyzer.analyze(model)
            when (result) {
                is Ok -> return result
                is Failed -> last = result
                is Fatal -> return Fatal(result.errors)
            }
        }
        return last ?: Failed(
            ErrorCode.Other,
            "没有可用的不可行分析器 / No infeasibility analyzer is available"
        )
    }
}

private fun <T> propagate(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "不可行分析结果状态无效 / Invalid infeasibility analyzer result state")
    }
}
