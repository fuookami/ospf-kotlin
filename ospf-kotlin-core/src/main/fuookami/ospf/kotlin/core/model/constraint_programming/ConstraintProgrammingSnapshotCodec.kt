/** Versioned CP snapshot transport codec. / 带版本的 CP snapshot 传输编解码器。 */
package fuookami.ospf.kotlin.core.model.constraint_programming

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * / 只序列化不可变 CP snapshot，不序列化求解器或 native 句柄。 / Serializes immutable CP snapshots without serializing solver/native handles. The receiver supplies stable-ID-to-variable bindings when decoding, which keeps
 * / 接收端解码时提供稳定 ID 到变量的绑定，从而不依赖发送端变量对象。 / transport independent of a particular OSPF variable instance.
 */
object ConstraintProgrammingSnapshotCodec {
    private const val CURRENT_SCHEMA = 1

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    /**
     * Encode a snapshot as versioned JSON. / 将 snapshot 编码为带版本 JSON。
     *
     * @param snapshot 不可变 CP snapshot / Immutable CP snapshot
     * @return 编码后的 JSON 或结构化错误 / Encoded JSON or a structured error
     */
    fun encode(snapshot: ConstraintProgrammingModelSnapshot): Ret<String> {
        if (!snapshot.validateIdentity()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 身份清单无效：存在重复 ID 或缺失 stable origin / Invalid CP snapshot identity manifest: duplicate ID or missing stable origin"
            )
        }
        if (!snapshot.validateObjectiveSemantics()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 只支持与模型方向一致的单一目标 / CP snapshots support at most one objective matching the model category"
            )
        }
        return try {
            ok(json.encodeToString(SnapshotPayload.serializer(), snapshot.toPayload()))
        } catch (error: Throwable) {
            Failed(
                ErrorCode.Other,
                "CP snapshot 序列化失败：${error.message} / CP snapshot serialization failed: ${error.message}"
            )
        }
    }

    /**
     * / 使用调用方提供的稳定变量绑定解码 JSON。 / Decode JSON with caller-provided stable variable bindings.
     *
     * @param encoded 带版本的 snapshot JSON / Versioned snapshot JSON
     * @param variables 稳定变量 ID 到 OSPF 变量的绑定 / Stable-ID-to-variable bindings
     * @return 解码后的 snapshot 或结构化错误 / Decoded snapshot or a structured error
     */
    fun decode(
        encoded: String,
        variables: Map<VariableId, AbstractVariableItem<*, *>>
    ): Ret<ConstraintProgrammingModelSnapshot> {
        return try {
            val payload = json.decodeFromString(SnapshotPayload.serializer(), encoded)
            if (payload.schema != CURRENT_SCHEMA) {
                return Failed(
                    ErrorCode.Other,
                    "不支持的 CP snapshot schema：${payload.schema} / Unsupported CP snapshot schema: ${payload.schema}"
                )
            }
            payload.validateIdentity()?.let { return Failed(ErrorCode.IllegalArgument, it) }
            payload.validateObjectiveSemantics()?.let { return Failed(ErrorCode.IllegalArgument, it) }
            payload.canonicalized().toSnapshot(variables)
        } catch (error: Throwable) {
            Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 反序列化失败：${error.message} / CP snapshot deserialization failed: ${error.message}"
            )
        }
    }

    /**
     * 仅校验 snapshot 传输结构，不要求变量绑定。
     * Validate snapshot transport structure without requiring variable bindings.
     *
     * @param encoded 带版本的 snapshot JSON / Versioned snapshot JSON
     * @return 校验结果 / Validation result
     */
    fun validate(encoded: String): Ret<Unit> {
        return try {
            val payload = json.decodeFromString(SnapshotPayload.serializer(), encoded)
            if (payload.schema != CURRENT_SCHEMA) {
                return Failed(
                    ErrorCode.Other,
                    "不支持的 CP snapshot schema：${payload.schema} / Unsupported CP snapshot schema: ${payload.schema}"
                )
            }
            payload.validateIdentity()?.let { return Failed(ErrorCode.IllegalArgument, it) }
            payload.validateObjectiveSemantics()?.let { return Failed(ErrorCode.IllegalArgument, it) }
            ok(Unit)
        } catch (error: Throwable) {
            Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 结构校验失败：${error.message} / CP snapshot validation failed: ${error.message}"
            )
        }
    }

    /**
     * Canonicalize a compatible snapshot JSON without requiring variable bindings. /
     * 在不要求变量绑定的情况下，将兼容 snapshot JSON 迁移为唯一规范文本。
     *
     * This is used by checkpoint migration so historical field order, scope aliases, and
     * provenance ordering do not make an otherwise equivalent snapshot unrecoverable. /
     * checkpoint 迁移使用该入口，避免历史字段顺序、scope 别名或 provenance 顺序导致等价
     * snapshot 无法恢复。
     *
     * @param encoded snapshot JSON / snapshot JSON
     * @return canonical snapshot JSON or a structured error / 规范 snapshot JSON 或结构化错误
     */
    fun canonicalize(encoded: String): Ret<String> {
        return try {
            val payload = json.decodeFromString(SnapshotPayload.serializer(), encoded)
            if (payload.schema != CURRENT_SCHEMA) {
                return Failed(
                    ErrorCode.Other,
                    "不支持的 CP snapshot schema：${payload.schema} / Unsupported CP snapshot schema: ${payload.schema}"
                )
            }
            payload.validateIdentity()?.let { return Failed(ErrorCode.IllegalArgument, it) }
            payload.validateObjectiveSemantics()?.let { return Failed(ErrorCode.IllegalArgument, it) }
            ok(json.encodeToString(SnapshotPayload.serializer(), payload.canonicalized()))
        } catch (error: Throwable) {
            Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 规范化失败：${error.message} / CP snapshot canonicalization failed: ${error.message}"
            )
        }
    }
}

@Serializable
private data class SnapshotPayload(
    val schema: Int,
    val name: String,
    val objectCategory: String,
    val variables: List<VariablePayload>,
    val intervals: List<IntervalPayload>,
    val expressions: List<NamedExpressionPayload>,
    val constraints: List<ConstraintEntryPayload>,
    val objectives: List<ObjectivePayload>,
    val constraintGroups: List<String>,
    val identitySchemaVersion: String = "1.0",
    val identityNamespace: String = "model-local"
)

@Serializable
private data class VariablePayload(
    val id: String,
    val name: String,
    val typeName: String,
    val domain: DomainPayload,
    val scope: String = "model-local",
    val origin: String? = null,
    val identityProvenance: List<IdentityOriginPayload> = emptyList()
)

@Serializable
private data class IdentityOriginPayload(
    val kind: String,
    val key: String
)

@Serializable
private data class DomainPayload(
    val kind: String,
    val lower: Long? = null,
    val upper: Long? = null,
    val values: List<Long> = emptyList()
)

@Serializable
private data class NamedExpressionPayload(
    val name: String,
    val expression: ExpressionPayload
)

@Serializable
private data class ExpressionPayload(
    val kind: String,
    val value: Long? = null,
    val message: String? = null,
    val variableId: String? = null,
    val domain: DomainPayload? = null,
    val terms: List<TermPayload> = emptyList(),
    val constant: Long = 0L
)

@Serializable
private data class TermPayload(
    val variableId: String,
    val coefficient: Long
)

@Serializable
private data class LiteralPayload(
    val variableId: String? = null,
    val negated: Boolean = false,
    val constant: Boolean? = null
)

@Serializable
private data class IntervalPayload(
    val id: String,
    val start: ExpressionPayload,
    val size: ExpressionPayload,
    val end: ExpressionPayload,
    val presence: LiteralPayload? = null,
    val scope: String = "model-local",
    val origin: String? = null,
    val identityProvenance: List<IdentityOriginPayload> = emptyList()
)

@Serializable
private data class ConstraintEntryPayload(
    val id: String,
    val name: String,
    val groupName: String?,
    val constraint: ConstraintPayload,
    val scope: String = "model-local",
    val origin: String? = null,
    val identityProvenance: List<IdentityOriginPayload> = emptyList()
)

@Serializable
private data class ConstraintPayload(
    val kind: String,
    val expression: ExpressionPayload? = null,
    val comparison: String? = null,
    val rhs: Long? = null,
    val literals: List<LiteralPayload> = emptyList(),
    val enforcement: LiteralPayload? = null,
    val child: ConstraintPayload? = null,
    val reifiedDirection: String? = null,
    val reifiedLiteral: LiteralPayload? = null,
    val expressions: List<ExpressionPayload> = emptyList(),
    val index: ExpressionPayload? = null,
    val values: List<ValuePayload> = emptyList(),
    val target: ExpressionPayload? = null,
    val tuples: List<List<Long>> = emptyList(),
    val intervals: List<IntervalPayload> = emptyList(),
    val demands: List<ExpressionPayload> = emptyList(),
    val capacity: ExpressionPayload? = null,
    val successors: List<ExpressionPayload> = emptyList(),
    val initialState: Int? = null,
    val finalStates: List<Int> = emptyList(),
    val transitions: List<TransitionPayload> = emptyList(),
    val events: List<EventPayload> = emptyList(),
    val initialLevel: Long? = null,
    val minimumLevel: Long? = null,
    val maximumLevel: Long? = null
)

@Serializable
private data class ValuePayload(
    val integer: Long? = null,
    val expression: ExpressionPayload? = null
)

@Serializable
private data class TransitionPayload(
    val fromState: Int,
    val value: Long,
    val toState: Int
)

@Serializable
private data class EventPayload(
    val time: ExpressionPayload,
    val levelChange: ExpressionPayload
)

@Serializable
private data class ObjectivePayload(
    val id: String,
    val category: String,
    val name: String,
    val expression: ExpressionPayload,
    val scope: String = "model-local",
    val origin: String? = null,
    val identityProvenance: List<IdentityOriginPayload> = emptyList()
)

private fun SnapshotPayload.validateIdentity(): String? {
    if (identitySchemaVersion.isBlank() || identityNamespace.isBlank()) {
        return "CP snapshot identity schema/namespace 不能为空 / CP snapshot identity schema and namespace must not be blank"
    }
    val elements = buildList {
        variables.forEach { add(Triple(it.id, it.scope, it.origin to it.identityProvenance)) }
        intervals.forEach { add(Triple(it.id, it.scope, it.origin to it.identityProvenance)) }
        constraints.forEach { add(Triple(it.id, it.scope, it.origin to it.identityProvenance)) }
        objectives.forEach { add(Triple(it.id, it.scope, it.origin to it.identityProvenance)) }
    }
    elements.forEach { (id, scope, metadata) ->
        if (id.isBlank()) {
            return "CP snapshot identity 包含空 ID / CP snapshot identity contains a blank ID"
        }
        validateConstraintProgrammingIdentity(
            id = id,
            scope = scope,
            origin = metadata.first,
            provenance = canonicalConstraintProgrammingIdentityProvenance(
                origin = metadata.first,
                provenance = metadata.second.toModelOrigins()
            ),
            identityNamespace = identityNamespace,
            identitySchemaVersion = identitySchemaVersion
        )?.let { return it }
    }
    if (elements.map { it.first }.distinct().size != elements.size) {
        return "CP snapshot 存在重复稳定 ID / CP snapshot contains duplicate element IDs"
    }
    return null
}

private fun SnapshotPayload.validateObjectiveSemantics(): String? {
    val category = enumValue<ObjectCategory>(objectCategory)
        ?: return "CP snapshot 目标方向无效 / CP snapshot objective category is invalid"
    if (objectives.size > 1) {
        return "CP snapshot 只支持单一目标 / CP snapshot supports at most one objective"
    }
    if (objectives.firstOrNull()?.category != null && objectives.first().category != category.name) {
        return "CP snapshot 目标方向与模型方向不一致 / CP snapshot objective category disagrees with model category"
    }
    return null
}

private fun ConstraintProgrammingModelSnapshot.toPayload(): SnapshotPayload {
    // Canonicalize transport order by stable identity. The mutable snapshot still preserves
    // registration order for in-process diagnostics, but fingerprints/checkpoints must not
    // change when an equivalent model is rebuilt in a different order.
    // 按稳定身份规范化传输顺序。可变 snapshot 仍保留进程内注册顺序，但等价模型重建时
    // 指纹与 checkpoint 不应因注册顺序变化而改变。
    return SnapshotPayload(
        schema = 1,
        name = name,
        objectCategory = objectCategory.name,
        variables = variables.sortedBy { it.id.value }.map {
            VariablePayload(
                id = it.id.value,
                name = it.name,
                typeName = it.typeName,
                domain = it.domain.toPayload(),
                scope = canonicalConstraintProgrammingIdentityScope(it.scope) ?: it.scope,
                origin = it.origin,
                identityProvenance = it.identityProvenance.toPayload(it.origin)
            )
        },
        intervals = intervals.sortedBy { it.id.value }.map { it.toPayload() },
        expressions = expressions.sortedBy { it.name }.map { NamedExpressionPayload(it.name, it.expression.toPayload()) },
        constraints = constraints.sortedBy { it.id.value }.map {
            ConstraintEntryPayload(
                id = it.id.value,
                name = it.name,
                groupName = it.groupName,
                constraint = it.constraint.toPayload(),
                scope = canonicalConstraintProgrammingIdentityScope(it.scope) ?: it.scope,
                origin = it.origin,
                identityProvenance = it.identityProvenance.toPayload(it.origin)
            )
        },
        objectives = objectives.sortedBy { it.id.value }.map {
            ObjectivePayload(
                id = it.id.value,
                category = it.category.name,
                name = it.name,
                expression = it.expression.toPayload(),
                scope = canonicalConstraintProgrammingIdentityScope(it.scope) ?: it.scope,
                origin = it.origin,
                identityProvenance = it.identityProvenance.toPayload(it.origin)
            )
        },
        constraintGroups = constraintGroups.sorted(),
        identitySchemaVersion = identitySchemaVersion,
        identityNamespace = identityNamespace
    ).canonicalized()
}

private fun SnapshotPayload.canonicalized(): SnapshotPayload {
    return copy(
        variables = variables
            .map { it.canonicalized() }
            .sortedBy { it.id },
        intervals = intervals
            .map { it.canonicalized() }
            .sortedBy { it.id },
        expressions = expressions
            .map { it.copy(expression = it.expression.canonicalized()) }
            .sortedBy { it.name },
        constraints = constraints
            .map { it.canonicalized() }
            .sortedBy { it.id },
        objectives = objectives
            .map { it.canonicalized() }
            .sortedBy { it.id },
        constraintGroups = constraintGroups.sorted(),
        identitySchemaVersion = identitySchemaVersion,
        identityNamespace = identityNamespace
    )
}

private fun VariablePayload.canonicalized(): VariablePayload {
    return copy(
        domain = domain.canonicalized(),
        scope = canonicalConstraintProgrammingIdentityScope(scope) ?: scope,
        identityProvenance = identityProvenance.canonicalized(origin)
    )
}

private fun IntervalPayload.canonicalized(): IntervalPayload {
    return copy(
        start = start.canonicalized(),
        size = size.canonicalized(),
        end = end.canonicalized(),
        presence = presence?.canonicalized(),
        scope = canonicalConstraintProgrammingIdentityScope(scope) ?: scope,
        identityProvenance = identityProvenance.canonicalized(origin)
    )
}

private fun ConstraintEntryPayload.canonicalized(): ConstraintEntryPayload {
    return copy(
        constraint = constraint.canonicalized(),
        scope = canonicalConstraintProgrammingIdentityScope(scope) ?: scope,
        identityProvenance = identityProvenance.canonicalized(origin)
    )
}

private fun ObjectivePayload.canonicalized(): ObjectivePayload {
    return copy(
        expression = expression.canonicalized(),
        scope = canonicalConstraintProgrammingIdentityScope(scope) ?: scope,
        identityProvenance = identityProvenance.canonicalized(origin)
    )
}

private fun DomainPayload.canonicalized(): DomainPayload {
    return when (kind) {
        "values" -> copy(values = values.sorted())
        else -> this
    }
}

private fun ExpressionPayload.canonicalized(): ExpressionPayload {
    return copy(
        domain = domain?.canonicalized(),
        terms = terms.sortedWith(compareBy({ it.variableId }, { it.coefficient }))
    )
}

private fun LiteralPayload.canonicalized(): LiteralPayload = this

private fun ConstraintPayload.canonicalized(): ConstraintPayload {
    val canonicalExpressions = expressions.map { it.canonicalized() }
    val canonicalIntervals = intervals.map { it.canonicalized() }
    val canonicalDemands = demands.map { it.canonicalized() }
    val intervalDemandPairs = if (canonicalIntervals.size == canonicalDemands.size) {
        canonicalIntervals.zip(canonicalDemands)
            .sortedWith(compareBy({ it.first.canonicalKey() }, { it.second.canonicalKey() }))
    } else {
        emptyList()
    }
    val orderedExpressions = if (kind == "all-different") {
        canonicalExpressions.sortedBy { it.canonicalKey() }
    } else {
        canonicalExpressions
    }
    val orderedLiterals = when (kind) {
        "bool-and", "bool-or", "bool-xor" -> literals.map { it.canonicalized() }.sortedWith(
            compareBy({ it.variableId ?: "" }, { it.constant ?: false }, { it.negated })
        )
        else -> literals.map { it.canonicalized() }
    }
    val orderedTuples = tuples.sortedWith(Comparator { left, right -> compareIntLists(left, right) })
    val orderedIntervals = when {
        kind == "no-overlap" -> canonicalIntervals.sortedBy { it.canonicalKey() }
        kind == "cumulative" && intervalDemandPairs.isNotEmpty() -> intervalDemandPairs.map { it.first }
        else -> canonicalIntervals
    }
    val orderedDemands = if (kind == "cumulative" && intervalDemandPairs.isNotEmpty()) {
        intervalDemandPairs.map { it.second }
    } else {
        canonicalDemands
    }
    val orderedEvents = if (kind == "reservoir") {
        events.map { it.canonicalized() }.sortedBy { it.canonicalKey() }
    } else {
        events.map { it.canonicalized() }
    }
    return copy(
        expression = expression?.canonicalized(),
        literals = orderedLiterals,
        child = child?.canonicalized(),
        reifiedLiteral = reifiedLiteral?.canonicalized(),
        expressions = orderedExpressions,
        index = index?.canonicalized(),
        values = values.map { it.canonicalized() },
        tuples = orderedTuples,
        intervals = orderedIntervals,
        demands = orderedDemands,
        capacity = capacity?.canonicalized(),
        successors = successors.map { it.canonicalized() },
        finalStates = finalStates.sorted(),
        transitions = transitions.sortedWith(compareBy({ it.fromState }, { it.value }, { it.toState })),
        events = orderedEvents
    )
}

private fun ValuePayload.canonicalized(): ValuePayload {
    return copy(expression = expression?.canonicalized())
}

private fun EventPayload.canonicalized(): EventPayload {
    return copy(
        time = time.canonicalized(),
        levelChange = levelChange.canonicalized()
    )
}

private fun EventPayload.canonicalKey(): String = canonicalized().toString()

private fun List<IdentityOriginPayload>.canonicalized(origin: String?): List<IdentityOriginPayload> {
    return canonicalConstraintProgrammingIdentityProvenance(
        origin = origin,
        provenance = toModelOrigins()
    ).map { IdentityOriginPayload(it.kind, it.key) }
}

private fun ExpressionPayload.canonicalKey(): String = canonicalized().toString()

private fun IntervalPayload.canonicalKey(): String = canonicalized().toString()

private fun compareIntLists(left: List<Long>, right: List<Long>): Int {
    for (index in 0 until minOf(left.size, right.size)) {
        val comparison = left[index].compareTo(right[index])
        if (comparison != 0) {
            return comparison
        }
    }
    return left.size.compareTo(right.size)
}

private fun IntegerDomain.toPayload(): DomainPayload {
    return when (this) {
        is IntegerDomain.Interval -> DomainPayload("interval", lowerBound.toLong(), upperBound.toLong())
        is IntegerDomain.Values -> DomainPayload("values", values = values.map { it.toLong() })
    }
}

private fun IntervalVariable.toPayload(): IntervalPayload {
    return IntervalPayload(
        id = id.value,
        start = start.toPayload(),
        size = size.toPayload(),
        end = end.toPayload(),
        presence = presence?.toPayload(),
        scope = canonicalConstraintProgrammingIdentityScope(scope) ?: scope,
        origin = origin,
        identityProvenance = identityProvenance.toPayload(origin)
    )
}

private fun ConstraintProgrammingExpression.toPayload(): ExpressionPayload {
    return when (this) {
        is ConstraintProgrammingExpression.Constant -> ExpressionPayload("constant", value = value.toLong())
        is ConstraintProgrammingExpression.Invalid -> ExpressionPayload("invalid", message = message)
        is ConstraintProgrammingExpression.Variable -> ExpressionPayload(
            kind = "variable",
            variableId = variableId.value,
            domain = domain.toPayload()
        )
        is ConstraintProgrammingExpression.Linear -> ExpressionPayload(
            kind = "linear",
            terms = terms.map { TermPayload(it.variableId.value, it.coefficient.toLong()) },
            constant = constant.toLong()
        )
    }
}

private fun BooleanLiteral.toPayload(): LiteralPayload {
    return LiteralPayload(variableId?.value, negated, constant)
}

private fun ConstraintProgrammingConstraint.toPayload(): ConstraintPayload {
    return when (this) {
        is ConstraintProgrammingConstraint.IntegerComparison -> ConstraintPayload(
            kind = "integer-comparison",
            expression = expression.toPayload(),
            comparison = comparison.name,
            rhs = rhs.toLong()
        )
        is ConstraintProgrammingConstraint.BoolAnd -> ConstraintPayload("bool-and", literals = literals.map { it.toPayload() })
        is ConstraintProgrammingConstraint.BoolOr -> ConstraintPayload("bool-or", literals = literals.map { it.toPayload() })
        is ConstraintProgrammingConstraint.BoolXor -> ConstraintPayload("bool-xor", literals = literals.map { it.toPayload() })
        is ConstraintProgrammingConstraint.Literal -> ConstraintPayload("literal", literals = listOf(literal.toPayload()))
        is ConstraintProgrammingConstraint.Implication -> ConstraintPayload(
            kind = "implication",
            enforcement = enforcement.toPayload(),
            child = constraint.toPayload()
        )
        is ConstraintProgrammingConstraint.Reified -> ConstraintPayload(
            kind = "reified",
            reifiedLiteral = literal.toPayload(),
            child = constraint.toPayload(),
            reifiedDirection = direction.name
        )
        is ConstraintProgrammingConstraint.AllDifferent -> ConstraintPayload("all-different", expressions = expressions.map { it.toPayload() })
        is ConstraintProgrammingConstraint.Element -> ConstraintPayload(
            kind = "element",
            index = index.toPayload(),
            values = values.map {
                when (it) {
                    is ConstraintProgrammingExpression -> ValuePayload(expression = it.toPayload())
                    is Int64 -> ValuePayload(integer = it.toLong())
                    is Long -> ValuePayload(integer = it)
                    is Int -> ValuePayload(integer = it.toLong())
                    else -> ValuePayload()
                }
            },
            target = target.toPayload()
        )
        is ConstraintProgrammingConstraint.AllowedAssignments -> ConstraintPayload(
            "allowed-assignments",
            expressions = expressions.map { it.toPayload() },
            tuples = tuples.map { tuple -> tuple.map { it.toLong() } }
        )
        is ConstraintProgrammingConstraint.ForbiddenAssignments -> ConstraintPayload(
            "forbidden-assignments",
            expressions = expressions.map { it.toPayload() },
            tuples = tuples.map { tuple -> tuple.map { it.toLong() } }
        )
        is ConstraintProgrammingConstraint.Circuit -> ConstraintPayload("circuit", successors = successors.map { it.toPayload() })
        is ConstraintProgrammingConstraint.Automaton -> ConstraintPayload(
            kind = "automaton",
            expressions = expressions.map { it.toPayload() },
            initialState = initialState,
            finalStates = finalStates.toList(),
            transitions = transitions.map { TransitionPayload(it.fromState, it.value.toLong(), it.toState) }
        )
        is ConstraintProgrammingConstraint.Reservoir -> ConstraintPayload(
            kind = "reservoir",
            events = events.map { EventPayload(it.time.toPayload(), it.levelChange.toPayload()) },
            initialLevel = initialLevel.toLong(),
            minimumLevel = minimumLevel.toLong(),
            maximumLevel = maximumLevel.toLong()
        )
        is NoOverlap -> ConstraintPayload("no-overlap", intervals = intervals.map { it.toPayload() })
        is Cumulative -> ConstraintPayload(
            kind = "cumulative",
            intervals = intervals.map { it.toPayload() },
            demands = demands.map { it.toPayload() },
            capacity = capacity.toPayload()
        )
    }
}

private fun SnapshotPayload.toSnapshot(
    variables: Map<VariableId, AbstractVariableItem<*, *>>
): Ret<ConstraintProgrammingModelSnapshot> {
    val variableSnapshots = ArrayList<ConstraintProgrammingVariableSnapshot>()
    for (variable in this.variables) {
        val domain = variable.domain.toDomain()
        if (domain.failed) return propagate(domain)
        variableSnapshots += ConstraintProgrammingVariableSnapshot(
            id = VariableId(variable.id),
            name = variable.name,
            typeName = variable.typeName,
            domain = domain.value!!,
            scope = canonicalConstraintProgrammingIdentityScope(variable.scope) ?: variable.scope,
            origin = variable.origin,
            identityProvenance = canonicalConstraintProgrammingIdentityProvenance(
                origin = variable.origin,
                provenance = variable.identityProvenance.toModelOrigins()
            )
        )
    }
    val intervalsResult = this.intervals.mapResult { it.toInterval(variables) }
    if (intervalsResult.failed) return propagate(intervalsResult)
    val expressionsResult = this.expressions.mapResult {
        decodeExpression(it.expression, variables).map { expression ->
            ConstraintProgrammingExpressionSnapshot(it.name, expression)
        }
    }
    if (expressionsResult.failed) return propagate(expressionsResult)
    val constraintsResult = this.constraints.mapResult {
        decodeConstraint(it.constraint, variables).map { constraint ->
            ConstraintProgrammingConstraintSnapshot(
                id = ConstraintId(it.id),
                name = it.name,
                groupName = it.groupName,
                constraint = constraint,
                scope = canonicalConstraintProgrammingIdentityScope(it.scope) ?: it.scope,
                origin = it.origin,
                identityProvenance = canonicalConstraintProgrammingIdentityProvenance(
                    origin = it.origin,
                    provenance = it.identityProvenance.toModelOrigins()
                )
            )
        }
    }
    if (constraintsResult.failed) return propagate(constraintsResult)
    val objectivesResult = this.objectives.mapResult {
        val category = enumValue<ObjectCategory>(it.category)
            ?: return@mapResult Failed(ErrorCode.IllegalArgument, "未知目标类别 / Unknown objective category")
        decodeExpression(it.expression, variables).map { expression ->
            ConstraintProgrammingObjectiveSnapshot(
                id = ObjectiveId(it.id),
                category = category,
                name = it.name,
                expression = expression,
                scope = canonicalConstraintProgrammingIdentityScope(it.scope) ?: it.scope,
                origin = it.origin,
                identityProvenance = canonicalConstraintProgrammingIdentityProvenance(
                    origin = it.origin,
                    provenance = it.identityProvenance.toModelOrigins()
                )
            )
        }
    }
    if (objectivesResult.failed) return propagate(objectivesResult)
    val category = enumValue<ObjectCategory>(objectCategory)
        ?: return Failed(ErrorCode.IllegalArgument, "未知模型目标类别 / Unknown model objective category")
    return ok(
        ConstraintProgrammingModelSnapshot(
            name = name,
            objectCategory = category,
            variables = variableSnapshots,
            intervals = intervalsResult.value!!,
            expressions = expressionsResult.value!!,
            constraints = constraintsResult.value!!,
            objectives = objectivesResult.value!!,
            constraintGroups = constraintGroups.toList(),
            identitySchemaVersion = identitySchemaVersion,
            identityNamespace = identityNamespace
        )
    )
}

private fun DomainPayload.toDomain(): Ret<IntegerDomain> {
    return when (kind) {
        "interval" -> if (lower == null || upper == null) {
            Failed(ErrorCode.IllegalArgument, "CP interval domain 缺少边界 / CP interval domain is missing bounds")
        } else {
            IntegerDomain.interval(lower, upper)
        }
        "values" -> IntegerDomain.values(values)
        else -> Failed(ErrorCode.IllegalArgument, "未知 CP domain 类型：$kind / Unknown CP domain kind: $kind")
    }
}

private fun IntervalPayload.toInterval(
    variables: Map<VariableId, AbstractVariableItem<*, *>>
): Ret<IntervalVariable> {
    val start = decodeExpression(start, variables)
    if (start.failed) return propagate(start)
    val size = decodeExpression(size, variables)
    if (size.failed) return propagate(size)
    val end = decodeExpression(end, variables)
    if (end.failed) return propagate(end)
    val presence = presence?.let { decodeLiteral(it, variables) }
    if (presence != null && presence.failed) return propagate(presence)
    return ok(
        IntervalVariable(
            id = IntervalId(id),
            start = start.value!!,
            size = size.value!!,
            end = end.value!!,
            presence = presence?.value,
            scope = canonicalConstraintProgrammingIdentityScope(scope) ?: scope,
            origin = origin,
            identityProvenance = canonicalConstraintProgrammingIdentityProvenance(
                origin = origin,
                provenance = identityProvenance.toModelOrigins()
            )
        )
    )
}

private fun decodeExpression(
    payload: ExpressionPayload,
    variables: Map<VariableId, AbstractVariableItem<*, *>>
): Ret<ConstraintProgrammingExpression> {
    return when (payload.kind) {
        "constant" -> payload.value?.let { ok(ConstraintProgrammingExpression.Constant(Int64(it))) }
            ?: Failed(ErrorCode.IllegalArgument, "CP constant 缺少值 / CP constant is missing a value")
        "invalid" -> ok(ConstraintProgrammingExpression.Invalid(payload.message.orEmpty()))
        "variable" -> {
            val id = payload.variableId?.let(::VariableId)
                ?: return Failed(ErrorCode.IllegalArgument, "CP variable expression 缺少 ID / CP variable expression is missing an ID")
            val variable = variables[id]
                ?: return Failed(ErrorCode.DataNotFound, "缺少 CP 变量绑定：$id / Missing CP variable binding: $id")
            val domain = payload.domain?.toDomain() ?: IntegerDomain.variableDefault(variable)
            if (domain.failed) return propagate(domain)
            ok(ConstraintProgrammingExpression.Variable(variable, domain.value!!, id))
        }
        "linear" -> {
            val terms = ArrayList<ConstraintProgrammingExpression.Term>()
            for (term in payload.terms) {
                val id = VariableId(term.variableId)
                val variable = variables[id]
                    ?: return Failed(ErrorCode.DataNotFound, "缺少 CP 变量绑定：$id / Missing CP variable binding: $id")
                terms += ConstraintProgrammingExpression.Term(variable, Int64(term.coefficient), id)
            }
            ok(ConstraintProgrammingExpression.Linear(terms, Int64(payload.constant)))
        }
        else -> Failed(ErrorCode.IllegalArgument, "未知 CP expression 类型：${payload.kind} / Unknown CP expression kind: ${payload.kind}")
    }
}

private fun decodeLiteral(
    payload: LiteralPayload,
    variables: Map<VariableId, AbstractVariableItem<*, *>>
): Ret<BooleanLiteral> {
    payload.constant?.let { return ok(BooleanLiteral(it)) }
    val id = payload.variableId?.let(::VariableId)
        ?: return Failed(ErrorCode.IllegalArgument, "CP literal 缺少变量 ID / CP literal is missing a variable ID")
    val variable = variables[id]
        ?: return Failed(ErrorCode.DataNotFound, "缺少 CP 布尔变量绑定：$id / Missing CP Boolean variable binding: $id")
    val binary = variable as? fuookami.ospf.kotlin.core.variable.BinVariable
        ?: return Failed(ErrorCode.IllegalArgument, "CP literal 绑定变量不是二值变量 / CP literal binding is not binary")
    return ok(BooleanLiteral(binary, payload.negated, id))
}

private fun decodeConstraint(
    payload: ConstraintPayload,
    variables: Map<VariableId, AbstractVariableItem<*, *>>
): Ret<ConstraintProgrammingConstraint> {
    fun expression(value: ExpressionPayload?): Ret<ConstraintProgrammingExpression> {
        return value?.let { decodeExpression(it, variables) }
            ?: Failed(ErrorCode.IllegalArgument, "CP constraint 缺少表达式 / CP constraint is missing an expression")
    }
    fun literals(values: List<LiteralPayload>): Ret<List<BooleanLiteral>> {
        val result = ArrayList<BooleanLiteral>()
        for (value in values) {
            val literal = decodeLiteral(value, variables)
            if (literal.failed) return propagate(literal)
            result += literal.value!!
        }
        return ok(result)
    }
    return when (payload.kind) {
        "integer-comparison" -> {
            val expression = expression(payload.expression)
            if (expression.failed) return propagate(expression)
            val comparison = payload.comparison?.let { enumValue<ConstraintProgrammingComparison>(it) }
                ?: return Failed(ErrorCode.IllegalArgument, "未知 CP comparison / Unknown CP comparison")
            val rhs = payload.rhs ?: return Failed(ErrorCode.IllegalArgument, "CP comparison 缺少 rhs / CP comparison is missing rhs")
            ok(ConstraintProgrammingConstraint.IntegerComparison(expression.value!!, comparison, Int64(rhs)))
        }
        "bool-and", "bool-or", "bool-xor" -> {
            val literals = literals(payload.literals)
            if (literals.failed) return propagate(literals)
            when (payload.kind) {
                "bool-and" -> ok(ConstraintProgrammingConstraint.BoolAnd(literals.value!!))
                "bool-or" -> ok(ConstraintProgrammingConstraint.BoolOr(literals.value!!))
                else -> ok(ConstraintProgrammingConstraint.BoolXor(literals.value!!))
            }
        }
        "literal" -> {
            val literal = payload.literals.singleOrNull()?.let { decodeLiteral(it, variables) }
                ?: return Failed(ErrorCode.IllegalArgument, "CP literal 数量无效 / Invalid CP literal arity")
            literal.map { ConstraintProgrammingConstraint.Literal(it) }
        }
        "implication" -> {
            val enforcement = payload.enforcement?.let { decodeLiteral(it, variables) }
                ?: return Failed(ErrorCode.IllegalArgument, "CP implication 缺少 enforcement / CP implication is missing enforcement")
            val child = payload.child?.let { decodeConstraint(it, variables) }
                ?: return Failed(ErrorCode.IllegalArgument, "CP implication 缺少 child / CP implication is missing child")
            if (enforcement.failed) return propagate(enforcement)
            if (child.failed) return propagate(child)
            ok(ConstraintProgrammingConstraint.Implication(enforcement.value!!, child.value!!))
        }
        "reified" -> {
            val literal = payload.reifiedLiteral?.let { decodeLiteral(it, variables) }
                ?: return Failed(ErrorCode.IllegalArgument, "CP reified 缺少 literal / CP reified is missing literal")
            val child = payload.child?.let { decodeConstraint(it, variables) }
                ?: return Failed(ErrorCode.IllegalArgument, "CP reified 缺少 child / CP reified is missing child")
            val direction = payload.reifiedDirection?.let { enumValue<ReificationDirection>(it) }
                ?: return Failed(ErrorCode.IllegalArgument, "CP reified 缺少方向 / CP reified is missing direction")
            if (literal.failed) return propagate(literal)
            if (child.failed) return propagate(child)
            ok(ConstraintProgrammingConstraint.Reified(literal.value!!, child.value!!, direction))
        }
        "all-different" -> {
            val expressions = payload.expressions.mapResult { decodeExpression(it, variables) }
            if (expressions.failed) return propagate(expressions)
            ok(ConstraintProgrammingConstraint.AllDifferent(expressions.value!!))
        }
        "element" -> {
            val index = expression(payload.index)
            val target = expression(payload.target)
            if (index.failed) return propagate(index)
            if (target.failed) return propagate(target)
            val values = ArrayList<Any>()
            for (value in payload.values) {
                if (value.expression != null) {
                    val decoded = decodeExpression(value.expression, variables)
                    if (decoded.failed) return propagate(decoded)
                    values += decoded.value!!
                } else if (value.integer != null) {
                    values += Int64(value.integer)
                } else {
                    return Failed(ErrorCode.IllegalArgument, "Element 值为空 / Element value is empty")
                }
            }
            ok(ConstraintProgrammingConstraint.Element(index.value!!, values, target.value!!))
        }
        "allowed-assignments", "forbidden-assignments" -> {
            val expressions = payload.expressions.mapResult { decodeExpression(it, variables) }
            if (expressions.failed) return propagate(expressions)
            val tuples = payload.tuples.map { tuple -> tuple.map { Int64(it) } }
            if (payload.kind == "allowed-assignments") {
                ok(ConstraintProgrammingConstraint.AllowedAssignments(expressions.value!!, tuples))
            } else {
                ok(ConstraintProgrammingConstraint.ForbiddenAssignments(expressions.value!!, tuples))
            }
        }
        "circuit" -> {
            val successors = payload.successors.mapResult { decodeExpression(it, variables) }
            if (successors.failed) return propagate(successors)
            ok(ConstraintProgrammingConstraint.Circuit(successors.value!!))
        }
        "automaton" -> {
            val expressions = payload.expressions.mapResult { decodeExpression(it, variables) }
            if (expressions.failed) return propagate(expressions)
            val automaton = ConstraintProgrammingConstraint.automaton(
                expressions = expressions.value!!,
                initialState = payload.initialState ?: 0,
                finalStates = payload.finalStates.toSet(),
                transitions = payload.transitions.map {
                    ConstraintProgrammingConstraint.AutomatonTransition(
                        it.fromState,
                        Int64(it.value),
                        it.toState
                    )
                }
            )
            if (automaton.failed) return propagate(automaton)
            ok(automaton.value!!)
        }
        "reservoir" -> {
            val events = ArrayList<ConstraintProgrammingConstraint.Reservoir.Event>()
            for (event in payload.events) {
                val time = decodeExpression(event.time, variables)
                val change = decodeExpression(event.levelChange, variables)
                if (time.failed) return propagate(time)
                if (change.failed) return propagate(change)
                events += ConstraintProgrammingConstraint.Reservoir.Event(time.value!!, change.value!!)
            }
            val initial = payload.initialLevel ?: return Failed(ErrorCode.IllegalArgument, "Reservoir 缺少初始液位 / Reservoir is missing initial level")
            val minimum = payload.minimumLevel ?: return Failed(ErrorCode.IllegalArgument, "Reservoir 缺少最小液位 / Reservoir is missing minimum level")
            val maximum = payload.maximumLevel ?: return Failed(ErrorCode.IllegalArgument, "Reservoir 缺少最大液位 / Reservoir is missing maximum level")
            ok(ConstraintProgrammingConstraint.Reservoir(events, Int64(initial), Int64(minimum), Int64(maximum)))
        }
        "no-overlap", "cumulative" -> {
            val intervals = payload.intervals.mapResult { it.toInterval(variables) }
            if (intervals.failed) return propagate(intervals)
            if (payload.kind == "no-overlap") {
                ok(NoOverlap(intervals.value!!))
            } else {
                val demands = payload.demands.mapResult { decodeExpression(it, variables) }
                val capacity = expression(payload.capacity)
                if (demands.failed) return propagate(demands)
                if (capacity.failed) return propagate(capacity)
                ok(Cumulative(intervals.value!!, demands.value!!, capacity.value!!))
            }
        }
        else -> Failed(ErrorCode.IllegalArgument, "未知 CP constraint 类型：${payload.kind} / Unknown CP constraint kind: ${payload.kind}")
    }
}

private fun <T : Enum<T>> enumValue(clazz: Class<T>, value: String): T? {
    return clazz.enumConstants.firstOrNull { it.name == value }
}

private inline fun <reified T : Enum<T>> enumValue(value: String): T? {
    return enumValue(T::class.java, value)
}

private fun List<ModelElementOrigin>.toPayload(
    origin: String? = null
): List<IdentityOriginPayload> {
    return canonicalConstraintProgrammingIdentityProvenance(
        origin = origin,
        provenance = this
    ).map { IdentityOriginPayload(it.kind, it.key) }
}

private fun List<IdentityOriginPayload>.toModelOrigins(): List<ModelElementOrigin> {
    return map { ModelElementOrigin(it.kind, it.key) }
}

private fun <T, U> Iterable<T>.mapResult(transform: (T) -> Ret<U>): Ret<List<U>> {
    val result = ArrayList<U>()
    for (item in this) {
        val mapped = transform(item)
        if (mapped.failed) return propagate(mapped)
        result += mapped.value!!
    }
    return ok(result)
}

private fun <T> propagate(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "CP snapshot 结果状态无效 / Invalid CP snapshot result state")
    }
}

private fun IntegerDomain.Companion.variableDefault(
    variable: AbstractVariableItem<*, *>
): Ret<IntegerDomain> {
    return ConstraintProgrammingExpression.variable(variable).map { it.domain }
}
