/** 诊断激活协议（计划事项 J）。 / Diagnostic activation protocol (plan item J). */
package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember

/** 诊断激活协议 schema。 / Diagnostic activation protocol schema. */
const val ACTIVATION_PROTOCOL_SCHEMA_VERSION: String = "1.0"

/** 激活状态。 / Activation state. */
enum class ActivationState {
    /** 该原始证据参与派生模型。 / The original evidence participates in the derived model. */
    Activated,

    /** 该原始证据被移出派生模型。 / The original evidence is removed from the derived model. */
    Deactivated;

    /** 是否为激活。 / Whether this is an activated state. */
    val isActivated: Boolean
        get() = this == Activated
}

/** 一条原始证据的激活记录。 / Activation record for one original evidence source.
 *
 * @property source 原始证据来源 / Original evidence source
 * @property state 激活状态 / Activation state
 */
data class DiagnosticActivation(
    /** 原始证据来源。 / Original evidence source. */
    val source: DiagnosticSource,
    /** 激活状态。 / Activation state. */
    val state: ActivationState
) {
    /** 稳定身份。 / Stable identity. */
    val stableId: String
        get() = "${if (state.isActivated) "activated" else "deactivated"}:${source.stableId}"

    companion object {
        /** 激活一条原始证据。 / Activate one original evidence source.
         *
         * @param source 原始证据来源 / Original evidence source
         * @return 激活记录 / Activation record
         */
        fun activate(source: DiagnosticSource): DiagnosticActivation {
            return DiagnosticActivation(source, ActivationState.Activated)
        }

        /** 停用一条原始证据。 / Deactivate one original evidence source.
         *
         * @param source 原始证据来源 / Original evidence source
         * @return 停用记录 / Deactivation record
         */
        fun deactivate(source: DiagnosticSource): DiagnosticActivation {
            return DiagnosticActivation(source, ActivationState.Deactivated)
        }
    }
}

/**
 * 后端冲突提取层级——即计划事项 K 的优先顺序。
 *
 * 顺序固定为：原生 unsat core → assumption 提取 → 重复求解回退。能力必须来自后端声明，
 * 未声明的能力一律不得被当作可用。
 *
 * Backend conflict-extraction tier, i.e. plan item K's priority order. The order is fixed:
 * native unsat core, then assumption extraction, then repeated-solving fallback. Capability must
 * come from the backend declaration; an undeclared capability is never treated as available.
 */
enum class ConflictExtractionTier {
    /** 后端原生 unsat core。 / Native backend unsat core. */
    NativeUnsatCore,

    /** 基于 assumption 的框架提取。 / Framework extraction over assumptions. */
    AssumptionExtraction,

    /** 重复求解回退。 / Repeated-solving fallback. */
    RepeatedSolving,

    /** 没有任何可用路径。 / No usable path is declared. */
    Unavailable;

    /** 该层级是否避免重复求解。 / Whether this tier avoids repeated solving. */
    val isNative: Boolean
        get() = this == NativeUnsatCore

    /** 该层级是否可用。 / Whether the tier is usable. */
    val isAvailable: Boolean
        get() = this != Unavailable

    /** 稳定层级名称。 / Stable tier name. */
    val tierName: String
        get() = when (this) {
            NativeUnsatCore -> "native-unsat-core"
            AssumptionExtraction -> "assumption-extraction"
            RepeatedSolving -> "repeated-solving"
            Unavailable -> "unavailable"
        }

    companion object {
        /**
         * 依据能力矩阵选择层级。
         *
         * 这里刻意不使用 `CapabilitySupport.Conditional` 冒充原生能力：只有矩阵明确声明为
         * native（或显式 `Supported`）时才进入前两级。
         *
         * Select a tier from a capability matrix. A `Conditional` level never masquerades as a
         * native capability: only an explicitly native (or explicitly `Supported`) entry can
         * reach the first two tiers.
         *
         * @param matrix 分析能力矩阵 / Analysis capability matrix
         * @return 选择的冲突提取层级 / Selected conflict-extraction tier
         */
        fun select(matrix: CapabilityMatrix): ConflictExtractionTier {
            if (matrix.isNative(AnalysisCapability.NativeUnsatCore) ||
                matrix.support(AnalysisCapability.NativeUnsatCore) == CapabilitySupport.Supported
            ) {
                return NativeUnsatCore
            }
            if (matrix.isNative(AnalysisCapability.AssumptionSolving) ||
                matrix.support(AnalysisCapability.AssumptionSolving) == CapabilitySupport.Supported
            ) {
                return AssumptionExtraction
            }
            if (matrix.fallbackSatisfactionOnly &&
                matrix.support(AnalysisCapability.Conflict) != CapabilitySupport.Unsupported
            ) {
                return RepeatedSolving
            }
            return Unavailable
        }
    }
}

/**
 * 一次求解中激活的原始证据集合。
 *
 * 该集合是框架层的求解请求描述，独立于任何后端表示；后端只看到派生模型或 assumption，
 * 公开报告只看到这里的原始 [DiagnosticSource]。
 *
 * The set of original evidence active in one solve. It is a framework-level request description
 * independent of any backend representation: the backend only sees a derived model or
 * assumptions, while public reports only ever see the original [DiagnosticSource] values.
 *
 * @property activations 全部激活记录 / All activation records
 * @property size 记录数量 / Number of records
 * @property isEmpty 集合是否为空 / Whether the set has no records
 */
class DiagnosticActivationSet private constructor(
    private val entries: MutableList<DiagnosticActivation>
) {
    /** 全部记录（只读）。 / Every record (read-only). */
    val activations: List<DiagnosticActivation>
        get() = entries.toList()

    /** 记录数量。 / Number of records. */
    val size: Int
        get() = entries.size

    /** 集合是否为空。 / Whether the set has no records. */
    val isEmpty: Boolean
        get() = entries.isEmpty()

    /** 记录一条激活状态（后者覆盖前者）。 / Record an activation state, overriding any earlier one.
     *
     * @param activation 要记录的激活状态 / Activation state to record
     */
    fun put(activation: DiagnosticActivation) {
        entries.removeAll { it.source == activation.source }
        entries += activation
    }

    /** 激活一条来源。 / Activate a source.
     *
     * @param source 原始证据来源 / Original evidence source
     */
    fun activate(source: DiagnosticSource) {
        put(DiagnosticActivation.activate(source))
    }

    /** 停用一条来源。 / Deactivate a source.
     *
     * @param source 原始证据来源 / Original evidence source
     */
    fun deactivate(source: DiagnosticSource) {
        put(DiagnosticActivation.deactivate(source))
    }

    /**
     * 某个来源是否激活。
     *
     * 未出现的来源按未激活处理：调用方必须先显式声明其证据全集。
     *
     * Whether a source is active. An absent source is inactive: callers must declare their
     * evidence universe explicitly.
     *
     * @param source 要检查的原始证据来源 / Original evidence source to check
     * @return 来源是否激活 / Whether the source is active
     */
    fun isActive(source: DiagnosticSource): Boolean {
        return entries.any { it.source == source && it.state.isActivated }
    }

    /** 全部激活来源。 / Every active source.
     *
     * @return 激活来源列表 / Active source list
     */
    fun activeSources(): List<DiagnosticSource> {
        return entries.filter { it.state.isActivated }.map { it.source }
    }

    /** 全部激活的原始约束 ID。 / Every active original constraint ID.
     *
     * @return 激活的原始约束 ID 集合 / Active original constraint IDs
     */
    fun activeConstraintIds(): Set<ConstraintId> {
        return entries.filter { it.state.isActivated }
            .mapNotNull { (it.source as? DiagnosticSource.Constraint)?.id }
            .toSet()
    }

    /** 全部停用来源。 / Every deactivated source.
     *
     * @return 停用来源列表 / Deactivated source list
     */
    fun deactivatedSources(): List<DiagnosticSource> {
        return entries.filter { !it.state.isActivated }.map { it.source }
    }

    /**
     * 把后端返回的不可行证据成员回映为原始证据。
     *
     * 回映以**本激活集合**为唯一来源：先枚举激活的原始证据，再检查后端证据是否指向其中之一。
     * 后端返回的任何未知成员都会被丢弃，因此 solver 生成的辅助元素不可能出现在结果里。
     *
     * Remap backend infeasibility evidence back to original evidence. Remapping is driven
     * exclusively by this activation set, so any unrecognised backend member is dropped and a
     * solver-generated auxiliary element can never appear in the result.
     *
     * @param members 后端返回的不可行证据成员 / Backend infeasibility evidence members
     * @return 回映后的原始证据列表 / Remapped original evidence list
     */
    fun remapBackendEvidence(members: Collection<InfeasibilityMember>): List<DiagnosticSource> {
        val wanted = members.toSet()
        return activeSources().filter { source ->
            source.asInfeasibilityMember()?.let { it in wanted } == true
        }
    }

    /**
     * 校验激活集合不变量。
     *
     * 公开证据边界在此强制：任何来源都必须能回映为原始模型身份，且不得重复。
     *
     * Validate activation-set invariants. The public evidence boundary is enforced here: every
     * source must map back to an original-model identity and must not repeat.
     *
     * @return 结构化校验结果 / Structured validation result
     */
    fun validate(): Try {
        val seen = HashSet<String>()
        for (entry in entries) {
            if (!seen.add(entry.source.stableId)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "激活集合包含重复来源：${entry.source.stableId} / Activation set contains a duplicate source: ${entry.source.stableId}"
                )
            }
        }
        return ok
    }

    companion object {
        /** 空集合。 / An empty set.
         *
         * @return 空的激活集合 / Empty activation set
         */
        fun empty(): DiagnosticActivationSet {
            return DiagnosticActivationSet(ArrayList())
        }

        /**
         * 从快照构造：所有原始约束与固定背景证据均激活。
         *
         * 变量边界按两侧分别登记；稀疏值域额外登记为 [DiagnosticSource.SparseDomain]。
         *
         * Build from a snapshot: every original constraint and fixed-background source is active.
         * Variable bounds are registered per side, and a sparse domain is additionally
         * registered as [DiagnosticSource.SparseDomain].
         *
         * @param snapshot CP 模型 snapshot / CP model snapshot
         * @return 包含原始证据激活记录的集合 / Set containing original-evidence activation records
         */
        fun fromSnapshot(snapshot: ConstraintProgrammingModelSnapshot): DiagnosticActivationSet {
            val set = empty()
            for (constraint in snapshot.constraints) {
                set.activate(DiagnosticSource.Constraint(constraint.id))
            }
            for (variable in snapshot.variables) {
                val id = VariableId(variable.id.value)
                set.activate(DiagnosticSource.VariableLowerBound(id))
                set.activate(DiagnosticSource.VariableUpperBound(id))
                if (variable.domain is IntegerDomain.Values) {
                    set.activate(DiagnosticSource.SparseDomain(id))
                }
            }
            return set
        }
    }
}
