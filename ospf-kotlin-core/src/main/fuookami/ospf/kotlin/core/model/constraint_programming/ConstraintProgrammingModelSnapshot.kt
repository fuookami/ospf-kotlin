/**
 * CP 模型的不可变 snapshot。 / Immutable snapshot of a CP model.
 */
package fuookami.ospf.kotlin.core.model.constraint_programming

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.VariableBoundRef
import fuookami.ospf.kotlin.core.solver.report.VariableDomainRef

/**
 * snapshot 中的标量变量定义。 / Scalar-variable definition in a snapshot.
 *
 * @property id 稳定变量 ID / Stable variable ID
 * @property name 展示名称 / Display name
 * @property typeName 变量类型名称 / Variable type name
 * @property domain 整数值域 / Integer domain
 * @property scope 身份作用域 / Identity scope
 * @property origin 稳定身份来源 / Stable identity origin
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 */
data class ConstraintProgrammingVariableSnapshot(
    val id: VariableId,
    val name: String,
    val typeName: String,
    val domain: IntegerDomain,
    val scope: String = "model-local",
    val origin: String? = null,
    val identityProvenance: List<ModelElementOrigin> = emptyList()
)

/**
 * snapshot 中的具名表达式。 / Named expression in a snapshot.
 *
 * @property name 表达式名称 / Expression name
 * @property expression 表达式 AST / Expression AST
 */
data class ConstraintProgrammingExpressionSnapshot(
    val name: String,
    val expression: ConstraintProgrammingExpression
)

/**
 * snapshot 中的约束定义。 / Constraint definition in a snapshot.
 *
 * @property id 稳定约束 ID / Stable constraint ID
 * @property name 展示名称 / Display name
 * @property groupName 约束组名称 / Constraint group name
 * @property constraint 约束 AST / Constraint AST
 * @property scope 身份作用域 / Identity scope
 * @property origin 稳定身份来源 / Stable identity origin
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 */
data class ConstraintProgrammingConstraintSnapshot(
    val id: ConstraintId,
    val name: String,
    val groupName: String?,
    val constraint: ConstraintProgrammingConstraint,
    val scope: String = "model-local",
    val origin: String? = null,
    val identityProvenance: List<ModelElementOrigin> = emptyList()
)

/**
 * snapshot 中的目标定义。 / Objective definition in a snapshot.
 *
 * @property id 稳定目标 ID / Stable objective ID
 * @property category 优化方向 / Optimization direction
 * @property name 展示名称 / Display name
 * @property expression 目标表达式 / Objective expression
 * @property scope 身份作用域 / Identity scope
 * @property origin 稳定身份来源 / Stable identity origin
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 */
data class ConstraintProgrammingObjectiveSnapshot(
    val id: ObjectiveId,
    val category: ObjectCategory,
    val name: String,
    val expression: ConstraintProgrammingExpression,
    val scope: String = "model-local",
    val origin: String? = null,
    val identityProvenance: List<ModelElementOrigin> = emptyList()
)

/**
 * 诊断模型中的稳定 activation 描述。该描述只引用原始模型成员，solver 辅助约束不得进入公共证据。 / Stable activation descriptor in a diagnostic model. / The descriptor references original model members only; solver auxiliaries must not leak into evidence.
 *
 * @property id activation 稳定 ID / Stable activation ID
 * @property member 对应的原始模型成员 / Referenced original model member
 */
data class ConstraintProgrammingActivationSnapshot(
    val id: String,
    val member: InfeasibilityMember
)

/**
 * CP 模型 snapshot；所有集合按注册顺序冻结。 / CP model snapshot with all collections frozen in registration order.
 * snapshot 不持有模型的可变注册表或求解器句柄，因此可以重复编译。 / The snapshot holds no mutable model registry or solver handle and can therefore be compiled repeatedly.
 *
 * @property name 模型名称 / Model name
 * @property objectCategory 模型优化方向 / Model optimization direction
 * @property variables 标量变量 / Scalar variables
 * @property intervals interval 变量 / Interval variables
 * @property expressions 具名表达式 / Named expressions
 * @property constraints 约束 / Constraints
 * @property objectives 目标 / Objectives
 * @property constraintGroups 约束组名称 / Constraint group names
 * @property identitySchemaVersion 身份 schema 版本 / Identity schema version
 * @property identityNamespace 身份命名空间 / Identity namespace
 */
data class ConstraintProgrammingModelSnapshot(
    val name: String,
    val objectCategory: ObjectCategory,
    val variables: List<ConstraintProgrammingVariableSnapshot>,
    val intervals: List<IntervalVariable>,
    val expressions: List<ConstraintProgrammingExpressionSnapshot>,
    val constraints: List<ConstraintProgrammingConstraintSnapshot>,
    val objectives: List<ConstraintProgrammingObjectiveSnapshot>,
    val constraintGroups: List<String>,
    val identitySchemaVersion: String = "1.0",
    val identityNamespace: String = "model-local"
) {
    /**
     * 按 ID 查找标量变量。 / Find a scalar variable by ID.
     *
     * @param id 稳定变量 ID / Stable variable ID
     * @return 匹配的变量定义，找不到时为 null / Matching variable definition, or null when absent
     */
    fun variable(id: VariableId): ConstraintProgrammingVariableSnapshot? {
        return variables.firstOrNull { it.id == id }
    }

    /**
     * 按 ID 查找约束。 / Find a constraint by ID.
     *
     * @param id 稳定约束 ID / Stable constraint ID
     * @return 匹配的约束定义，找不到时为 null / Matching constraint definition, or null when absent
     */
    fun constraint(id: ConstraintId): ConstraintProgrammingConstraintSnapshot? {
        return constraints.firstOrNull { it.id == id }
    }

    /**
     * 按 ID 查找目标。 / Find an objective by ID.
     *
     * @param id 稳定目标 ID / Stable objective ID
     * @return 匹配的目标定义，找不到时为 null / Matching objective definition, or null when absent
     */
    fun objective(id: ObjectiveId): ConstraintProgrammingObjectiveSnapshot? {
        return objectives.firstOrNull { it.id == id }
    }

    /**
     * 生成约束、上下界和稀疏值域 activation 的稳定清单。 / Build a stable list of activations for constraints, bounds, and sparse domains.
     *
     * @return 稳定 activation 清单 / Stable activation descriptors
     */
    fun diagnosticActivations(): List<ConstraintProgrammingActivationSnapshot> {
        val result = ArrayList<ConstraintProgrammingActivationSnapshot>()
        constraints.forEach { constraint ->
            result += ConstraintProgrammingActivationSnapshot(
                id = "constraint:${constraint.id.value}",
                member = InfeasibilityMember.Constraint(constraint.id)
            )
        }
        variables.forEach { variable ->
            result += ConstraintProgrammingActivationSnapshot(
                id = "variable:${variable.id.value}:lower",
                member = InfeasibilityMember.VariableBound(
                    VariableBoundRef(variable.id, BoundSide.Lower)
                )
            )
            result += ConstraintProgrammingActivationSnapshot(
                id = "variable:${variable.id.value}:upper",
                member = InfeasibilityMember.VariableBound(
                    VariableBoundRef(variable.id, BoundSide.Upper)
                )
            )
            if (variable.domain is IntegerDomain.Values && variable.domain != IntegerDomain.boolean) {
                result += ConstraintProgrammingActivationSnapshot(
                    id = "variable:${variable.id.value}:domain",
                    member = InfeasibilityMember.VariableDomain(VariableDomainRef(variable.id))
                )
            }
        }
        return result
    }

    /**
     * 校验身份清单，拒绝重复 ID 和不完整的 stable origin。
     * Validate identity metadata and reject duplicate IDs or incomplete stable origins.
     *
     * @return 身份清单是否有效 / Whether the identity manifest is valid
     */
    fun validateIdentity(): Boolean {
        if (identitySchemaVersion.isBlank() || identityNamespace.isBlank()) {
            return false
        }
        val elements = buildList {
            variables.forEach {
                add(Triple(it.id.value, it.scope, it.origin to it.identityProvenance))
            }
            intervals.forEach {
                add(Triple(it.id.value, it.scope, it.origin to it.identityProvenance))
            }
            constraints.forEach {
                add(Triple(it.id.value, it.scope, it.origin to it.identityProvenance))
            }
            objectives.forEach {
                add(Triple(it.id.value, it.scope, it.origin to it.identityProvenance))
            }
        }
        if (elements.any { (id, scope, metadata) ->
                id.isBlank() || validateConstraintProgrammingIdentity(
                    id = id,
                    scope = scope,
                    origin = metadata.first,
                    provenance = canonicalConstraintProgrammingIdentityProvenance(
                        origin = metadata.first,
                        provenance = metadata.second
                    ),
                    identityNamespace = identityNamespace,
                    identitySchemaVersion = identitySchemaVersion
                ) != null
            }) {
            return false
        }
        return elements.map { it.first }.distinct().size == elements.size
    }

    /**
     * 校验 snapshot 的目标语义。 / Validate snapshot objective semantics.
     *
     * CP 编译器只支持单一目标，且目标方向必须与模型方向一致。 /
     * The CP compilers support one objective only, and its direction must match the model.
     *
     * @return 语义是否有效 / Whether objective semantics are valid
     */
    fun validateObjectiveSemantics(): Boolean {
        val objective = objectives.firstOrNull()
        return objectives.size <= 1 && (objective == null || objective.category == objectCategory)
    }
}
