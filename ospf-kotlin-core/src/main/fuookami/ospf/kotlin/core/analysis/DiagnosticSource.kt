/** Stable sources allowed in public diagnostic evidence. / 公共诊断证据允许的稳定来源。 */
package fuookami.ospf.kotlin.core.analysis

import java.util.Locale
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.VariableBoundRef
import fuookami.ospf.kotlin.core.solver.report.VariableDomainRef
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember

/**
 * 原始模型证据边界上的诊断来源。 / A diagnostic source at the original-model evidence boundary.
 *
 * 此类型刻意不包含 solver 行、列、生成变量或后端句柄；`stableId` 适合用作缓存键和跨后端报告身份。
 * / Solver rows, columns, generated variables, and backend handles are intentionally absent from
 * this type. `stableId` is suitable for cache keys and cross-backend reports.
 *
 * @property stableId 稳定序列化身份 / Stable serialized identity
 * @property kind 稳定来源类型 / Stable source kind
 */
sealed interface DiagnosticSource {
    /** Stable serialized identity. / 稳定序列化身份。 */
    val stableId: String

    /** Stable source kind. / 稳定来源类型。 */
    val kind: String

    /** 原始 CP 约束。 / Original CP constraint.
     *
     * @property id 约束 ID / Constraint ID
     */
    data class Constraint(val id: ConstraintId) : DiagnosticSource {
        init {
            require(id.value.isNotBlank()) { "Constraint ID must not be blank" }
        }

        override val kind: String
            get() = "constraint"

        override val stableId: String
            get() = "constraint:${id.value}"
    }

    /** 原始变量下界。 / Lower bound of an original variable.
     *
     * @property variableId 变量 ID / Variable ID
     */
    data class VariableLowerBound(val variableId: VariableId) : DiagnosticSource {
        init {
            require(variableId.value.isNotBlank()) { "Variable ID must not be blank" }
        }

        /** 从下界引用构造来源。 / Construct a source from a lower-bound reference.
         *
         * @param ref 下界引用 / Lower-bound reference
         */
        constructor(ref: VariableBoundRef) : this(ref.variableId) {
            require(ref.side == BoundSide.Lower) { "A lower-bound source requires BoundSide.Lower" }
        }

        val ref: VariableBoundRef
            get() = VariableBoundRef(variableId, BoundSide.Lower)

        override val kind: String
            get() = "variable-lower-bound"

        override val stableId: String
            get() = "variable:${variableId.value}:lower"
    }

    /** 原始变量上界。 / Upper bound of an original variable.
     *
     * @property variableId 变量 ID / Variable ID
     */
    data class VariableUpperBound(val variableId: VariableId) : DiagnosticSource {
        init {
            require(variableId.value.isNotBlank()) { "Variable ID must not be blank" }
        }

        /** 从上界引用构造来源。 / Construct a source from an upper-bound reference.
         *
         * @param ref 上界引用 / Upper-bound reference
         */
        constructor(ref: VariableBoundRef) : this(ref.variableId) {
            require(ref.side == BoundSide.Upper) { "An upper-bound source requires BoundSide.Upper" }
        }

        val ref: VariableBoundRef
            get() = VariableBoundRef(variableId, BoundSide.Upper)

        override val kind: String
            get() = "variable-upper-bound"

        override val stableId: String
            get() = "variable:${variableId.value}:upper"
    }

    /** 不预先知道边界方向的通用变量边界来源。 / Generic variable-bound source for code that does not know the side statically.
     *
     * @property ref 变量边界引用 / Variable-bound reference
     */
    data class VariableBound(val ref: VariableBoundRef) : DiagnosticSource {
        init {
            require(ref.variableId.value.isNotBlank()) { "Variable ID must not be blank" }
        }

        val variableId: VariableId
            get() = ref.variableId

        override val kind: String
            get() = "variable-bound"

        override val stableId: String
            get() = "variable:${ref.variableId.value}:${ref.side.name.lowercase(Locale.ROOT)}"
    }

    /** 稀疏整数值域来源。 / Sparse integer-domain source.
     *
     * @property variableId 变量 ID / Variable ID
     */
    data class SparseDomain(val variableId: VariableId) : DiagnosticSource {
        init {
            require(variableId.value.isNotBlank()) { "Variable ID must not be blank" }
        }

        /** 从值域引用构造来源。 / Construct a source from a domain reference.
         *
         * @param ref 值域引用 / Domain reference
         */
        constructor(ref: VariableDomainRef) : this(ref.variableId)

        val ref: VariableDomainRef
            get() = VariableDomainRef(variableId)

        override val kind: String
            get() = "sparse-domain"

        override val stableId: String
            get() = "variable:${variableId.value}:domain"
    }

    /** 目标可行性分析使用的目标条件。 / Objective target used by target-feasibility analysis.
     *
     * @property target 目标条件 / Objective target
     */
    data class ObjectiveTarget(
        val target: fuookami.ospf.kotlin.core.analysis.ObjectiveTarget
    ) : DiagnosticSource {
        override val kind: String
            get() = "objective-target"

        override val stableId: String
            get() = target.stableId
    }

    /** 将来源投影为已有的不相容证据成员（若存在）。 / Project a source to the existing incompatibility-evidence member when one exists.
     *
     * @return 对应的不相容证据成员，不适用时为 null / Matching incompatibility-evidence member, or null when not applicable
     */
    fun asInfeasibilityMember(): InfeasibilityMember? {
        return when (this) {
            is Constraint -> InfeasibilityMember.Constraint(id)
            is VariableBound -> InfeasibilityMember.VariableBound(ref)
            is VariableLowerBound -> InfeasibilityMember.VariableBound(ref)
            is VariableUpperBound -> InfeasibilityMember.VariableBound(ref)
            is SparseDomain -> InfeasibilityMember.VariableDomain(ref)
            is ObjectiveTarget -> null
        }
    }
}
