/** Stable sources allowed in public diagnostic evidence. / 公共诊断证据允许的稳定来源。 */
package fuookami.ospf.kotlin.core.analysis

import java.util.Locale
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.VariableBoundRef
import fuookami.ospf.kotlin.core.solver.report.VariableDomainRef
import fuookami.ospf.kotlin.core.solver.report.VariableId

/**
 * A diagnostic source at the original-model evidence boundary.
 *
 * Solver rows, columns, generated variables, and backend handles are
 * intentionally absent from this type. `stableId` is suitable for cache keys
 * and cross-backend reports.
 */
sealed interface DiagnosticSource {
    /** Stable serialized identity. / 稳定序列化身份。 */
    val stableId: String

    /** Stable source kind. / 稳定来源类型。 */
    val kind: String

    /** Original CP constraint. / 原始 CP 约束。 */
    data class Constraint(val id: ConstraintId) : DiagnosticSource {
        init {
            require(id.value.isNotBlank()) { "Constraint ID must not be blank" }
        }

        override val kind: String
            get() = "constraint"

        override val stableId: String
            get() = "constraint:${id.value}"
    }

    /** Lower bound of an original variable. / 原始变量下界。 */
    data class VariableLowerBound(val variableId: VariableId) : DiagnosticSource {
        init {
            require(variableId.value.isNotBlank()) { "Variable ID must not be blank" }
        }

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

    /** Upper bound of an original variable. / 原始变量上界。 */
    data class VariableUpperBound(val variableId: VariableId) : DiagnosticSource {
        init {
            require(variableId.value.isNotBlank()) { "Variable ID must not be blank" }
        }

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

    /** Generic variable-bound source for code that does not know the side statically. */
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

    /** Sparse integer-domain source. / 稀疏整数值域来源。 */
    data class SparseDomain(val variableId: VariableId) : DiagnosticSource {
        init {
            require(variableId.value.isNotBlank()) { "Variable ID must not be blank" }
        }

        constructor(ref: VariableDomainRef) : this(ref.variableId)

        val ref: VariableDomainRef
            get() = VariableDomainRef(variableId)

        override val kind: String
            get() = "sparse-domain"

        override val stableId: String
            get() = "variable:${variableId.value}:domain"
    }

    /** Objective target used by target-feasibility analysis. / 目标可行性分析的目标条件。 */
    data class ObjectiveTarget(
        val target: fuookami.ospf.kotlin.core.analysis.ObjectiveTarget
    ) : DiagnosticSource {
        override val kind: String
            get() = "objective-target"

        override val stableId: String
            get() = target.stableId
    }

    /** Project a source to the existing incompatibility-evidence member when one exists. */
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
