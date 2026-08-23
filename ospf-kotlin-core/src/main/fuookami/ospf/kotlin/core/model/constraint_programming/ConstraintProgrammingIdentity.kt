/** CP identity metadata validation helpers. / CP 身份元数据校验辅助函数。 */
package fuookami.ospf.kotlin.core.model.constraint_programming

import java.util.Locale
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 将表达式中的源变量绑定到模型注册的稳定 ID。 / Bind source variables in an expression to model-registered stable IDs.
 *
 * @param bindings 源变量到稳定 ID 的绑定 / Source-variable to stable-ID bindings
 * @return 绑定后的表达式 / Expression with stable IDs bound
 */
internal fun ConstraintProgrammingExpression.bindStableVariableIds(
    bindings: Map<AbstractVariableItem<*, *>, VariableId>
): ConstraintProgrammingExpression {
    return when (this) {
        is ConstraintProgrammingExpression.Constant -> this
        is ConstraintProgrammingExpression.Invalid -> this
        is ConstraintProgrammingExpression.Variable -> copy(id = bindings[variable] ?: id)
        is ConstraintProgrammingExpression.Linear -> copy(
            terms = terms.map { term ->
                term.copy(id = bindings[term.variable] ?: term.id)
            }
        )
    }
}

/**
 * 将布尔文字绑定到模型注册的稳定 ID。 / Bind a Boolean literal to a model-registered stable ID.
 *
 * @param bindings 源变量到稳定 ID 的绑定 / Source-variable to stable-ID bindings
 * @return 绑定后的文字 / Literal with a stable ID bound
 */
internal fun BooleanLiteral.bindStableVariableIds(
    bindings: Map<AbstractVariableItem<*, *>, VariableId>
): BooleanLiteral {
    return when (this) {
        is BooleanLiteral.Constant -> this
        is BooleanLiteral.Variable -> copy(id = bindings[variable] ?: id)
    }
}

/**
 * 将 interval 的所有表达式和 presence literal 绑定到稳定 ID。 / Bind all interval expressions and presence literal to stable IDs.
 *
 * @param bindings 源变量到稳定 ID 的绑定 / Source-variable to stable-ID bindings
 * @return 绑定后的 interval / Interval with stable IDs bound
 */
internal fun IntervalVariable.bindStableVariableIds(
    bindings: Map<AbstractVariableItem<*, *>, VariableId>
): IntervalVariable {
    return copy(
        start = start.bindStableVariableIds(bindings),
        size = size.bindStableVariableIds(bindings),
        end = end.bindStableVariableIds(bindings),
        presence = presence?.bindStableVariableIds(bindings)
    )
}

/**
 * 递归绑定约束树中的所有源变量。 / Recursively bind all source variables in a constraint tree.
 *
 * @param bindings 源变量到稳定 ID 的绑定 / Source-variable to stable-ID bindings
 * @return 绑定后的约束 / Constraint with stable IDs bound
 */
internal fun ConstraintProgrammingConstraint.bindStableVariableIds(
    bindings: Map<AbstractVariableItem<*, *>, VariableId>
): ConstraintProgrammingConstraint {
    return when (this) {
        is ConstraintProgrammingConstraint.IntegerComparison -> copy(
            expression = expression.bindStableVariableIds(bindings)
        )
        is ConstraintProgrammingConstraint.BoolAnd -> copy(
            literals = literals.map { it.bindStableVariableIds(bindings) }
        )
        is ConstraintProgrammingConstraint.BoolOr -> copy(
            literals = literals.map { it.bindStableVariableIds(bindings) }
        )
        is ConstraintProgrammingConstraint.BoolXor -> copy(
            literals = literals.map { it.bindStableVariableIds(bindings) }
        )
        is ConstraintProgrammingConstraint.Literal -> copy(
            literal = literal.bindStableVariableIds(bindings)
        )
        is ConstraintProgrammingConstraint.Implication -> copy(
            enforcement = enforcement.bindStableVariableIds(bindings),
            constraint = constraint.bindStableVariableIds(bindings)
        )
        is ConstraintProgrammingConstraint.Reified -> copy(
            literal = literal.bindStableVariableIds(bindings),
            constraint = constraint.bindStableVariableIds(bindings)
        )
        is ConstraintProgrammingConstraint.AllDifferent -> copy(
            expressions = expressions.map { it.bindStableVariableIds(bindings) }
        )
        is ConstraintProgrammingConstraint.Element -> copy(
            index = index.bindStableVariableIds(bindings),
            values = values.map { value ->
                if (value is ConstraintProgrammingExpression) {
                    value.bindStableVariableIds(bindings)
                } else {
                    value
                }
            },
            target = target.bindStableVariableIds(bindings)
        )
        is ConstraintProgrammingConstraint.AllowedAssignments -> copy(
            expressions = expressions.map { it.bindStableVariableIds(bindings) }
        )
        is ConstraintProgrammingConstraint.ForbiddenAssignments -> copy(
            expressions = expressions.map { it.bindStableVariableIds(bindings) }
        )
        is ConstraintProgrammingConstraint.Circuit -> copy(
            successors = successors.map { it.bindStableVariableIds(bindings) }
        )
        is ConstraintProgrammingConstraint.Automaton -> copy(
            expressions = expressions.map { it.bindStableVariableIds(bindings) }
        )
        is ConstraintProgrammingConstraint.Reservoir -> copy(
            events = events.map { event ->
                ConstraintProgrammingConstraint.Reservoir.Event(
                    time = event.time.bindStableVariableIds(bindings),
                    levelChange = event.levelChange.bindStableVariableIds(bindings)
                )
            }
        )
        is NoOverlap -> copy(intervals = intervals.map { it.bindStableVariableIds(bindings) })
        is Cumulative -> copy(
            intervals = intervals.map { it.bindStableVariableIds(bindings) },
            demands = demands.map { it.bindStableVariableIds(bindings) },
            capacity = capacity.bindStableVariableIds(bindings)
        )
    }
}

/** Canonical CP identity scopes accepted at the model boundary. / CP 模型边界接受的规范身份作用域。 */
internal enum class ConstraintProgrammingIdentityScope {
    Stable,
    ModelLocal
}

/** Canonical compatibility provenance kind for the legacy string origin field. / 旧字符串 origin 字段的规范兼容 provenance kind。 */
internal const val CONSTRAINT_PROGRAMMING_LEGACY_ORIGIN_KIND = "legacy-origin"

internal val ConstraintProgrammingIdentityScope.canonicalName: String
    get() = when (this) {
        ConstraintProgrammingIdentityScope.Stable -> "stable"
        ConstraintProgrammingIdentityScope.ModelLocal -> "model-local"
    }

/** Return the single transport spelling for a CP identity scope. / 返回 CP identity scope 的唯一传输拼写。 */
internal fun canonicalConstraintProgrammingIdentityScope(scope: String): String? {
    return parseConstraintProgrammingIdentityScope(scope)?.canonicalName
}

/**
 * Canonicalize CP provenance as a sorted set while preserving explicit provenance precedence. /
 * 将 CP provenance 规范化为排序集合，并优先保留显式 provenance。
 *
 * The legacy origin is always represented as a structured compatibility entry. This makes the
 * relationship between the scalar compatibility field and the complete source set explicit;
 * a conflicting explicit `legacy-origin` entry is rejected by validation. / 旧 origin 始终表示为
 * 结构化兼容来源，使标量兼容字段与完整来源集合之间的关系明确；冲突的显式
 * `legacy-origin` 条目会由校验拒绝。
 */
internal fun canonicalConstraintProgrammingIdentityProvenance(
    origin: String?,
    provenance: List<ModelElementOrigin>
): List<ModelElementOrigin> {
    val compatibilityOrigin = origin?.let {
        ModelElementOrigin(CONSTRAINT_PROGRAMMING_LEGACY_ORIGIN_KIND, it)
    }
    return (provenance + listOfNotNull(compatibilityOrigin))
        .distinct()
        .sortedWith(compareBy({ it.kind }, { it.key }))
}

/**
 * Parse a CP scope while accepting the historical lower-case and hyphenated spellings. /
 * 解析 CP scope，同时兼容历史小写和连字符写法。
 *
 * @param scope 原始作用域 / Raw scope
 * @return 规范作用域，未知值返回 null / Canonical scope, or null for an unknown value
 */
internal fun parseConstraintProgrammingIdentityScope(
    scope: String
): ConstraintProgrammingIdentityScope? {
    return when (scope.trim().uppercase(Locale.ROOT).replace('-', '_')) {
        "STABLE" -> ConstraintProgrammingIdentityScope.Stable
        "MODEL_LOCAL" -> ConstraintProgrammingIdentityScope.ModelLocal
        else -> null
    }
}

/**
 * Validate one CP element identity against the model root metadata. /
 * 根据模型根元数据校验单个 CP 元素身份。
 *
 * @param scope 元素作用域 / Element scope
 * @param origin 兼容入口的主来源 / Primary origin from the compatibility entry point
 * @param provenance 结构化完整来源集合 / Structured complete provenance
 * @param identityNamespace 模型身份命名空间 / Model identity namespace
 * @param identitySchemaVersion 模型身份 schema / Model identity schema
 * @return 双语错误消息；有效时返回 null / Bilingual error message, or null when valid
 */
internal fun validateConstraintProgrammingIdentity(
    id: String? = null,
    scope: String,
    origin: String?,
    provenance: List<ModelElementOrigin>,
    identityNamespace: String,
    identitySchemaVersion: String
): String? {
    val parsedScope = parseConstraintProgrammingIdentityScope(scope)
        ?: return "CP 身份 scope 无效：$scope / Invalid CP identity scope: $scope"
    val explicitLegacyOrigins = provenance.filter {
        it.kind == CONSTRAINT_PROGRAMMING_LEGACY_ORIGIN_KIND
    }
    if (explicitLegacyOrigins.any { origin == null || it.key != origin }) {
        return "CP legacy origin 与 provenance 不一致 / CP legacy origin disagrees with provenance"
    }
    val canonicalProvenance = canonicalConstraintProgrammingIdentityProvenance(origin, provenance)
    val compatibilityOrigin = origin?.let {
        ModelElementOrigin(CONSTRAINT_PROGRAMMING_LEGACY_ORIGIN_KIND, it)
    }
    if (compatibilityOrigin != null && compatibilityOrigin !in canonicalProvenance) {
        return "CP origin 未包含在规范 provenance 中 / CP origin is missing from canonical provenance"
    }
    if (id?.isBlank() == true) {
        return "CP 身份 ID 不能为空 / CP identity ID must not be blank"
    }
    if (canonicalProvenance.any { it.kind.isBlank() || it.key.isBlank() }) {
        return "CP 身份 provenance 不能为空 / CP identity provenance entries must not be blank"
    }
    return when (parsedScope) {
        ConstraintProgrammingIdentityScope.Stable -> {
            when {
                id.isNullOrBlank() ->
                    "稳定 CP 身份必须有 ID / Stable CP identities require an ID"
                id.startsWith("model-local-") ->
                    "稳定 CP 身份不能使用 model-local ID / Stable CP identities must not use model-local IDs"
                id.startsWith("artifact:") ->
                    "CP 源身份不能使用保留的 artifact 前缀 / CP source identities must not use the reserved artifact prefix"
                identityNamespace.isBlank() || identitySchemaVersion.isBlank() ->
                    "稳定 CP 身份必须有 namespace/schema / Stable CP identities require namespace and schema"
                origin.isNullOrBlank() && canonicalProvenance.isEmpty() ->
                    "稳定 CP 身份必须提供 origin 或 provenance / Stable CP identities require an origin or provenance"
                else -> null
            }
        }

        ConstraintProgrammingIdentityScope.ModelLocal -> {
            when {
                id?.startsWith("artifact:") == true ->
                    "model-local CP 身份不能使用保留的 artifact 前缀 / Model-local CP identities must not use the reserved artifact prefix"
                origin != null || canonicalProvenance.isNotEmpty() ->
                    "model-local CP 身份不能携带稳定来源 / Model-local CP identities must not carry stable provenance"
                else -> null
            }
        }
    }
}
