/**
 * CP 模型注册器与生命周期。 / CP model registry and lifecycle.
 */
package fuookami.ospf.kotlin.core.model.constraint_programming

import java.util.IdentityHashMap
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.MetaConstraintGroup
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 独立的 CP 模型，不继承线性/二次 MetaModel。 / Independent CP model, intentionally not a Linear/Quadratic MetaModel.
 * 模型中的注册表保持插入顺序，snapshot 会在校验后复制所有集合。 / Registries preserve insertion order, and snapshot copies every collection after validation.
 *
 * @property name 模型名称 / Model name
 * @property objectCategory 模型优化方向 / Model optimization direction
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property identitySchemaVersion 身份 schema 版本 / Identity schema version
 */
class ConstraintProgrammingModel(
    val name: String = "constraint-programming-model",
    val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    val identityNamespace: String = "model-local",
    val identitySchemaVersion: String = "1.0"
) : ConstraintGroupRegistry, AutoCloseable {
    private data class VariableEntry(
        val variable: AbstractVariableItem<*, *>,
        val domain: IntegerDomain,
        val scope: String,
        val origin: String?,
        val identityProvenance: List<ModelElementOrigin>
    )

    private data class ConstraintEntry(
        val id: ConstraintId,
        val name: String,
        val groupName: String?,
        val constraint: ConstraintProgrammingConstraint,
        val scope: String,
        val origin: String?,
        val identityProvenance: List<ModelElementOrigin>
    )

    private data class ObjectiveEntry(
        val id: ObjectiveId,
        val category: ObjectCategory,
        val name: String,
        val expression: ConstraintProgrammingExpression,
        val scope: String,
        val origin: String?,
        val identityProvenance: List<ModelElementOrigin>
    )

    private val variables = LinkedHashMap<VariableId, VariableEntry>()
    private val variableIdsByIdentity = IdentityHashMap<AbstractVariableItem<*, *>, VariableId>()
    private val intervals = LinkedHashMap<IntervalId, IntervalVariable>()
    private val expressions = LinkedHashMap<String, ConstraintProgrammingExpression>()
    private val constraints = LinkedHashMap<ConstraintId, ConstraintEntry>()
    private val objectives = LinkedHashMap<ObjectiveId, ObjectiveEntry>()
    private val groups = LinkedHashMap<String, MetaConstraintGroup>()
    private var currentGroupName: String? = null
    private var closed = false

    /** 模型是否已关闭。 / Whether the model is closed. */
    val isClosed: Boolean
        get() = closed

    /** 已注册标量变量数量。 / Number of registered scalar variables. */
    val variableCount: Int
        get() = variables.size

    /** 已注册约束数量。 / Number of registered constraints. */
    val constraintCount: Int
        get() = constraints.size

    /**
     * 注册现有 OSPF 整数变量。 / Register an existing OSPF integer variable.
     *
     * @param variable OSPF 标量变量 / OSPF scalar variable
     * @param domain CP 值域；为空时按变量类型推导 / CP domain, inferred from variable type when null
     * @param scope 身份作用域 / Identity scope
     * @param origin 稳定身份来源 / Stable identity origin
     * @param identityProvenance 完整身份来源集合 / Complete identity provenance
     * @return 稳定变量 ID或错误 / Stable variable ID or an error
     */
    fun registerVariable(
        variable: AbstractVariableItem<*, *>,
        domain: IntegerDomain? = null,
        scope: String = "model-local",
        origin: String? = null,
        identityProvenance: List<ModelElementOrigin> = emptyList()
    ): Ret<VariableId> {
        if (closed) {
            return closedFailure()
        }
        val id = variableIdOf(variable)
        val identity = canonicalizeIdentityMetadata(
            id = id.value,
            scope = scope,
            origin = origin,
            identityProvenance = identityProvenance,
            identityNamespace = identityNamespace,
            identitySchemaVersion = identitySchemaVersion
        )
        if (identity.failed) {
            return propagateModelFailure(identity)
        }
        val metadata = identity.value!!
        val reference = ConstraintProgrammingExpression.variable(variable, domain)
        if (reference.failed) {
            return propagateModelFailure(reference)
        }
        if (variables.containsKey(id)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP 变量 ID 重复：$id / Duplicate CP variable ID: $id"
            )
        }
        val existingBinding = variableIdsByIdentity[variable]
        if (existingBinding != null && existingBinding != id) {
            return Failed(
                ErrorCode.IllegalArgument,
                "同一 OSPF 变量不能绑定多个 CP ID：$existingBinding 与 $id / " +
                    "The same OSPF variable cannot be bound to multiple CP IDs: $existingBinding and $id"
            )
        }
        variables[id] = VariableEntry(
            variable,
            reference.value!!.domain,
            metadata.scope,
            origin,
            metadata.provenance
        )
        variableIdsByIdentity[variable] = id
        return ok(id)
    }

    /**
     * 使用显式稳定 ID 注册变量；模型 snapshot 会将引用表达式绑定到该 ID。 / Register a variable with an explicit stable ID; model snapshots bind references to this ID.
     *
     * @param id 稳定变量 ID / Stable variable ID
     * @param variable OSPF 标量变量 / OSPF scalar variable
     * @param domain CP 值域 / CP domain
     * @param scope 身份作用域 / Identity scope
     * @param origin 稳定身份来源 / Stable identity origin
     * @param identityProvenance 完整身份来源集合 / Complete identity provenance
     * @return 稳定变量 ID 或结构化错误 / Stable variable ID or a structured error
     */
    fun registerVariable(
        id: VariableId,
        variable: AbstractVariableItem<*, *>,
        domain: IntegerDomain,
        scope: String = "model-local",
        origin: String? = null,
        identityProvenance: List<ModelElementOrigin> = emptyList()
    ): Ret<VariableId> {
        if (closed) {
            return closedFailure()
        }
        val identity = canonicalizeIdentityMetadata(
            id = id.value,
            scope = scope,
            origin = origin,
            identityProvenance = identityProvenance,
            identityNamespace = identityNamespace,
            identitySchemaVersion = identitySchemaVersion
        )
        if (identity.failed) {
            return propagateModelFailure(identity)
        }
        val metadata = identity.value!!
        if (id.value.isBlank()) {
            return Failed(ErrorCode.IllegalArgument, "CP 变量 ID 不能为空 / CP variable ID must not be blank")
        }
        val reference = ConstraintProgrammingExpression.variable(variable, domain)
        if (reference.failed) {
            return propagateModelFailure(reference)
        }
        if (variables.containsKey(id)) {
            return Failed(ErrorCode.IllegalArgument, "CP 变量 ID 重复：$id / Duplicate CP variable ID: $id")
        }
        val existingBinding = variableIdsByIdentity[variable]
        if (existingBinding != null && existingBinding != id) {
            return Failed(
                ErrorCode.IllegalArgument,
                "同一 OSPF 变量不能绑定多个 CP ID：$existingBinding 与 $id / " +
                    "The same OSPF variable cannot be bound to multiple CP IDs: $existingBinding and $id"
            )
        }
        variables[id] = VariableEntry(
            variable,
            reference.value!!.domain,
            metadata.scope,
            origin,
            metadata.provenance
        )
        variableIdsByIdentity[variable] = id
        return ok(id)
    }

    /**
     * 注册 interval 结构变量。 / Register an interval structural variable.
     *
     * @param interval interval 定义 / Interval definition
     * @return 稳定 interval ID 或结构化错误 / Stable interval ID or a structured error
     */
    fun registerInterval(interval: IntervalVariable): Ret<IntervalId> {
        if (closed) {
            return closedFailure()
        }
        val identity = canonicalizeIdentityMetadata(
            id = interval.id.value,
            scope = interval.scope,
            origin = interval.origin,
            identityProvenance = interval.identityProvenance,
            identityNamespace = identityNamespace,
            identitySchemaVersion = identitySchemaVersion
        )
        if (identity.failed) {
            return propagateModelFailure(identity)
        }
        val metadata = identity.value!!
        if (interval.id.value.isBlank()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "interval ID 不能为空 / Interval ID must not be blank"
            )
        }
        if (intervals.containsKey(interval.id)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "interval ID 重复：${interval.id} / Duplicate interval ID: ${interval.id}"
            )
        }
        intervals[interval.id] = interval.copy(
            scope = metadata.scope,
            identityProvenance = metadata.provenance
        )
        return ok(interval.id)
    }

    /**
     * 注册具名表达式。 / Register a named expression.
     *
     * @param name 表达式名称 / Expression name
     * @param expression 表达式 AST / Expression AST
     * @return 注册名称或结构化错误 / Registered name or a structured error
     */
    fun registerExpression(
        name: String,
        expression: ConstraintProgrammingExpression
    ): Ret<String> {
        if (closed) {
            return closedFailure()
        }
        if (name.isBlank()) {
            return Failed(ErrorCode.IllegalArgument, "CP 表达式名称不能为空 / CP expression name must not be blank")
        }
        if (expressions.containsKey(name)) {
            return Failed(ErrorCode.IllegalArgument, "CP 表达式名称重复：$name / Duplicate CP expression name: $name")
        }
        expressions[name] = expression
        return ok(name)
    }

    /**
     * 注册约束。 / Register a constraint.
     *
     * @param constraint 约束 AST / Constraint AST
     * @param id 可选稳定约束 ID / Optional stable constraint ID
     * @param name 展示名称 / Display name
     * @param group 约束组 / Constraint group
     * @param scope 身份作用域 / Identity scope
     * @param origin 稳定身份来源 / Stable identity origin
     * @param identityProvenance 完整身份来源集合 / Complete identity provenance
     * @return 稳定约束 ID 或结构化错误 / Stable constraint ID or a structured error
     */
    fun addConstraint(
        constraint: ConstraintProgrammingConstraint,
        id: ConstraintId? = null,
        name: String = "",
        group: MetaConstraintGroup? = null,
        scope: String = "model-local",
        origin: String? = null,
        identityProvenance: List<ModelElementOrigin> = emptyList()
    ): Ret<ConstraintId> {
        if (closed) {
            return closedFailure()
        }
        val resolvedId = id ?: ConstraintId("constraint-${constraints.size}")
        val identity = canonicalizeIdentityMetadata(
            id = resolvedId.value,
            scope = scope,
            origin = origin,
            identityProvenance = identityProvenance,
            identityNamespace = identityNamespace,
            identitySchemaVersion = identitySchemaVersion
        )
        if (identity.failed) {
            return propagateModelFailure(identity)
        }
        val metadata = identity.value!!
        if (resolvedId.value.isBlank()) {
            return Failed(ErrorCode.IllegalArgument, "CP 约束 ID 不能为空 / CP constraint ID must not be blank")
        }
        if (constraints.containsKey(resolvedId)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP 约束 ID 重复：$resolvedId / Duplicate CP constraint ID: $resolvedId"
            )
        }
        group?.let { registerConstraintGroup(it) }
        constraints[resolvedId] = ConstraintEntry(
            id = resolvedId,
            name = name.ifBlank { resolvedId.value },
            groupName = group?.name ?: currentGroupName,
            constraint = constraint,
            scope = metadata.scope,
            origin = origin,
            identityProvenance = metadata.provenance
        )
        return ok(resolvedId)
    }

    /**
     * 使用字符串 ID 注册约束。 / Register a constraint with a string ID.
     *
     * @param constraint 约束 AST / Constraint AST
     * @param id 稳定约束 ID 字符串 / Stable constraint ID string
     * @param name 展示名称 / Display name
     * @param group 约束组 / Constraint group
     * @param scope 身份作用域 / Identity scope
     * @param origin 稳定身份来源 / Stable identity origin
     * @param identityProvenance 完整身份来源集合 / Complete identity provenance
     * @return 稳定约束 ID 或结构化错误 / Stable constraint ID or a structured error
     */
    fun addConstraint(
        constraint: ConstraintProgrammingConstraint,
        id: String,
        name: String = "",
        group: MetaConstraintGroup? = null,
        scope: String = "model-local",
        origin: String? = null,
        identityProvenance: List<ModelElementOrigin> = emptyList()
    ): Ret<ConstraintId> {
        return addConstraint(
            constraint = constraint,
            id = ConstraintId(id),
            name = name,
            group = group,
            scope = scope,
            origin = origin,
            identityProvenance = identityProvenance
        )
    }

    /**
     * 注册目标。 / Register an objective.
     *
     * @param category 优化方向 / Optimization direction
     * @param expression 目标表达式 / Objective expression
     * @param id 可选稳定目标 ID / Optional stable objective ID
     * @param name 展示名称 / Display name
     * @param scope 身份作用域 / Identity scope
     * @param origin 稳定身份来源 / Stable identity origin
     * @param identityProvenance 完整身份来源集合 / Complete identity provenance
     * @return 稳定目标 ID 或结构化错误 / Stable objective ID or a structured error
     */
    fun addObjective(
        category: ObjectCategory,
        expression: ConstraintProgrammingExpression,
        id: ObjectiveId? = null,
        name: String = "",
        scope: String = "model-local",
        origin: String? = null,
        identityProvenance: List<ModelElementOrigin> = emptyList()
    ): Ret<ObjectiveId> {
        if (closed) {
            return closedFailure()
        }
        val resolvedId = id ?: ObjectiveId("objective-${objectives.size}")
        val identity = canonicalizeIdentityMetadata(
            id = resolvedId.value,
            scope = scope,
            origin = origin,
            identityProvenance = identityProvenance,
            identityNamespace = identityNamespace,
            identitySchemaVersion = identitySchemaVersion
        )
        if (identity.failed) {
            return propagateModelFailure(identity)
        }
        val metadata = identity.value!!
        if (resolvedId.value.isBlank()) {
            return Failed(ErrorCode.IllegalArgument, "CP 目标 ID 不能为空 / CP objective ID must not be blank")
        }
        if (objectives.containsKey(resolvedId)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP 目标 ID 重复：$resolvedId / Duplicate CP objective ID: $resolvedId"
            )
        }
        objectives[resolvedId] = ObjectiveEntry(
            id = resolvedId,
            category = category,
            name = name.ifBlank { resolvedId.value },
            expression = expression,
            scope = metadata.scope,
            origin = origin,
            identityProvenance = metadata.provenance
        )
        return ok(resolvedId)
    }

    /**
     * 注册最小化目标。 / Register a minimization objective.
     *
     * @param expression 目标表达式 / Objective expression
     * @param id 可选稳定目标 ID / Optional stable objective ID
     * @param name 展示名称 / Display name
     * @param scope 身份作用域 / Identity scope
     * @param origin 稳定身份来源 / Stable identity origin
     * @param identityProvenance 完整身份来源集合 / Complete identity provenance
     * @return 稳定目标 ID 或结构化错误 / Stable objective ID or a structured error
     */
    fun minimize(
        expression: ConstraintProgrammingExpression,
        id: ObjectiveId? = null,
        name: String = "",
        scope: String = "model-local",
        origin: String? = null,
        identityProvenance: List<ModelElementOrigin> = emptyList()
    ): Ret<ObjectiveId> {
        return addObjective(
            category = ObjectCategory.Minimum,
            expression = expression,
            id = id,
            name = name,
            scope = scope,
            origin = origin,
            identityProvenance = identityProvenance
        )
    }

    /**
     * 注册最大化目标。 / Register a maximization objective.
     *
     * @param expression 目标表达式 / Objective expression
     * @param id 可选稳定目标 ID / Optional stable objective ID
     * @param name 展示名称 / Display name
     * @param scope 身份作用域 / Identity scope
     * @param origin 稳定身份来源 / Stable identity origin
     * @param identityProvenance 完整身份来源集合 / Complete identity provenance
     * @return 稳定目标 ID 或结构化错误 / Stable objective ID or a structured error
     */
    fun maximize(
        expression: ConstraintProgrammingExpression,
        id: ObjectiveId? = null,
        name: String = "",
        scope: String = "model-local",
        origin: String? = null,
        identityProvenance: List<ModelElementOrigin> = emptyList()
    ): Ret<ObjectiveId> {
        return addObjective(
            category = ObjectCategory.Maximum,
            expression = expression,
            id = id,
            name = name,
            scope = scope,
            origin = origin,
            identityProvenance = identityProvenance
        )
    }

    /** 注册约束组；Pipeline 注册入口调用此方法。 / Register a group for Pipeline integration. */
    override fun registerConstraintGroup(group: MetaConstraintGroup) {
        if (!closed && group.name.isNotBlank()) {
            groups.putIfAbsent(group.name, group)
            currentGroupName = group.name
        }
    }

    /**
     * 获取约束组中的约束。 / Get constraints belonging to a group.
     *
     * @param group 约束组 / Constraint group
     * @return 该组中的约束列表 / Constraints in the group
     */
    fun constraintsOfGroup(group: MetaConstraintGroup): List<ConstraintProgrammingConstraint> {
        return constraints.values
            .filter { it.groupName == group.name }
            .map { it.constraint }
    }

    /**
     * 校验当前模型的所有引用。 / Validate all references in the current model.
     *
     * @return 成功或第一个结构化错误 / Success or the first structured error
     */
    fun validate(): Try {
        if (closed) {
            return closedFailure()
        }
        if (objectives.size > 1) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP 模型只支持单一目标：${objectives.size} / CP models support exactly one objective at most: ${objectives.size}"
            )
        }
        objectives.values.firstOrNull()?.let { objective ->
            if (objective.category != objectCategory) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "CP 目标方向与模型方向不一致：${objective.category} != $objectCategory / " +
                        "CP objective category disagrees with the model category: ${objective.category} != $objectCategory"
                )
            }
        }
        val knownVariables = variables.keys
        val bindings = variableIdsByIdentity
        for ((expressionName, expression) in expressions) {
            val missing = expression.bindStableVariableIds(bindings).variables.firstOrNull { it !in knownVariables }
            if (missing != null) {
                return missingReference("表达式 $expressionName", missing)
            }
        }
        for ((intervalId, interval) in intervals) {
            val missing = interval.bindStableVariableIds(bindings).variables.firstOrNull { it !in knownVariables }
            if (missing != null) {
                return missingReference("interval $intervalId", missing)
            }
        }
        for ((constraintId, constraint) in constraints) {
            val missing = constraint.constraint.bindStableVariableIds(bindings).variables.firstOrNull { it !in knownVariables }
            if (missing != null) {
                return missingReference("约束 $constraintId", missing)
            }
        }
        for ((objectiveId, objective) in objectives) {
            val missing = objective.expression.bindStableVariableIds(bindings).variables.firstOrNull { it !in knownVariables }
            if (missing != null) {
                return missingReference("目标 $objectiveId", missing)
            }
        }
        return ok
    }

    /**
     * 生成可重复编译的不可变 snapshot。 / Build an immutable, repeatably compilable snapshot.
     *
     * @return 不可变 snapshot 或结构化错误 / Immutable snapshot or a structured error
     */
    fun snapshot(): Ret<ConstraintProgrammingModelSnapshot> {
        val valid = validate()
        if (valid.failed) {
            return propagateModelFailure(valid)
        }
        val snapshot = ConstraintProgrammingModelSnapshot(
                name = name,
                objectCategory = objectCategory,
                variables = variables.map { (id, entry) ->
                    ConstraintProgrammingVariableSnapshot(
                        id = id,
                        name = entry.variable.name,
                        typeName = entry.variable.type.name,
                        domain = copyDomain(entry.domain),
                        scope = entry.scope,
                        origin = entry.origin,
                        identityProvenance = entry.identityProvenance
                    )
                },
                intervals = intervals.values.map { it.bindStableVariableIds(variableIdsByIdentity) },
                expressions = expressions.map { (expressionName, expression) ->
                    ConstraintProgrammingExpressionSnapshot(
                        expressionName,
                        expression.bindStableVariableIds(variableIdsByIdentity)
                    )
                },
                constraints = constraints.values.map {
                    ConstraintProgrammingConstraintSnapshot(
                        id = it.id,
                        name = it.name,
                        groupName = it.groupName,
                        constraint = it.constraint.bindStableVariableIds(variableIdsByIdentity),
                        scope = it.scope,
                        origin = it.origin,
                        identityProvenance = it.identityProvenance
                    )
                },
            objectives = objectives.values.map {
                    ConstraintProgrammingObjectiveSnapshot(
                        id = it.id,
                        category = it.category,
                        name = it.name,
                        expression = it.expression.bindStableVariableIds(variableIdsByIdentity),
                        scope = it.scope,
                        origin = it.origin,
                        identityProvenance = it.identityProvenance
                    )
                },
                constraintGroups = groups.keys.toList(),
                identitySchemaVersion = identitySchemaVersion,
                identityNamespace = identityNamespace
            )
        if (!snapshot.validateIdentity()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 身份清单无效 / CP snapshot identity manifest is invalid"
            )
        }
        return ok(snapshot)
    }

    /** 关闭并释放注册表。 / Close and release the registries. */
    override fun close() {
        closed = true
        variables.clear()
        intervals.clear()
        variableIdsByIdentity.clear()
        expressions.clear()
        constraints.clear()
        objectives.clear()
        groups.clear()
        currentGroupName = null
    }
}

private fun variableIdOf(variable: AbstractVariableItem<*, *>): VariableId {
    return VariableId("${variable.identifier}:${variable.index}")
}

private fun copyDomain(domain: IntegerDomain): IntegerDomain {
    return when (domain) {
        is IntegerDomain.Interval -> IntegerDomain.Interval(domain.lowerBound, domain.upperBound)
        is IntegerDomain.Values -> IntegerDomain.Values(domain.values.toList())
    }
}

private fun missingReference(owner: String, id: VariableId): Try {
    return Failed(
        ErrorCode.IllegalArgument,
        "$owner 引用了未注册变量：$id / $owner references an unregistered variable: $id"
    )
}

private fun <T> closedFailure(): Ret<T> {
    return Failed(
        ErrorCode.ApplicationStopped,
        "CP 模型已关闭 / CP model is closed"
    )
}

private fun <T> propagateModelFailure(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "CP 模型结果状态无效 / Invalid CP model result state")
    }
}

private data class CanonicalIdentityMetadata(
    val scope: String,
    val provenance: List<ModelElementOrigin>
)

private fun canonicalizeIdentityMetadata(
    id: String,
    scope: String,
    origin: String?,
    identityProvenance: List<ModelElementOrigin>,
    identityNamespace: String,
    identitySchemaVersion: String
): Ret<CanonicalIdentityMetadata> {
    val canonicalScope = canonicalConstraintProgrammingIdentityScope(scope)
        ?: return Failed(
            ErrorCode.IllegalArgument,
            "CP 身份 scope 无效：$scope / Invalid CP identity scope: $scope"
        )
    val canonicalProvenance = canonicalConstraintProgrammingIdentityProvenance(
        origin = origin,
        provenance = identityProvenance
    )
    val message = validateConstraintProgrammingIdentity(
        id = id,
        scope = canonicalScope,
        origin = origin,
        provenance = canonicalProvenance,
        identityNamespace = identityNamespace,
        identitySchemaVersion = identitySchemaVersion
    )
    return if (message == null) {
        ok(CanonicalIdentityMetadata(canonicalScope, canonicalProvenance))
    } else {
        Failed(ErrorCode.IllegalArgument, message)
    }
}
