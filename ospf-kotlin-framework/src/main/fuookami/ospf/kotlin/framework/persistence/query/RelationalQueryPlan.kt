/**
 * 通用关系查询计划 / Generic relational query plan
 *
 * 该模型只表达关系查询语义，不包含数据库表名、SQL 字符串或业务领域类型。
 * The model expresses relational query semantics only and contains no database table names, SQL strings, or business types.
 */
package fuookami.ospf.kotlin.framework.persistence.query

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Collections
import java.util.IdentityHashMap
import java.lang.reflect.Modifier
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.OwnedSymbol
import fuookami.ospf.kotlin.math.symbol.StableSymbol
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.SymbolId
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.operation.NormalizeConfig
import fuookami.ospf.kotlin.math.symbol.expression.operation.normalize
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * 关系查询数据源 / Relational query source
 *
 * @property name 适配器注册的数据源名称 / Adapter-registered source name
 * @property alias 查询计划中使用的可选别名 / Optional alias used by the query plan
 */
data class QuerySource(
    val name: String,
    val alias: String? = null
)

/**
 * 关系查询列引用 / Relational query column reference
 *
 * @property source 数据源名称或别名 / Source name or alias
 * @property path 适配器注册的字段路径 / Adapter-registered field path
 */
data class ColumnRef(
    val source: String,
    val path: String
)

/** Join 类型 / Join type */
enum class JoinType {
    /** 内连接 / Inner join */
    Inner,

    /** 左外连接 / Left outer join */
    Left,

    /** 相关存在性半连接 / Correlated existence semi-join */
    Exists
}

/**
 * Join 基数 / Join cardinality
 *
 * @property isToMany 是否为目标侧多行关系 / Whether the target side may contain multiple rows
 */
enum class JoinCardinality {
    /** 一对一 / One-to-one */
    OneToOne,

    /** 多对一 / Many-to-one */
    ManyToOne,

    /** 一对多 / One-to-many */
    OneToMany,

    /** 多对多 / Many-to-many */
    ManyToMany;

    /** 是否为目标侧多行关系 / Whether the target side may contain multiple rows */
    val isToMany: Boolean get() = this == OneToMany || this == ManyToMany
}

/**
 * Join 规格 / Join specification
 *
 * @property type Join 类型 / Join type
 * @property source 待加入的数据源 / Source to join
 * @property condition Join 关联条件 / Join correlation condition
 * @property cardinality 声明的数据源基数 / Declared source cardinality
 */
data class JoinSpec(
    val type: JoinType,
    val source: QuerySource,
    val condition: BooleanExpression,
    val cardinality: JoinCardinality
)

/**
 * 投影字段 / Projection field
 *
 * @property column 要返回的列 / Column to return
 * @property alias 返回结果中的可选别名 / Optional alias in the returned result
 */
data class ProjectionSpec(
    val column: ColumnRef,
    val alias: String? = null
)

/** 排序方向 / Sort direction */
enum class SortDirection {
    /** 升序 / Ascending order */
    Ascending,

    /** 降序 / Descending order */
    Descending
}

/** NULL 排序位置 / NULL ordering */
enum class NullsOrder {
    /** 不指定 NULL 位置 / Do not specify NULL placement */
    Unspecified,

    /** NULL 排在前面 / NULL values first */
    First,

    /** NULL 排在后面 / NULL values last */
    Last
}

/**
 * 排序规格 / Order specification
 *
 * @property column 要排序的列 / Column to sort by
 * @property direction 排序方向 / Sort direction
 * @property nulls NULL 值排序位置 / NULL value placement
 */
data class OrderSpec(
    val column: ColumnRef,
    val direction: SortDirection = SortDirection.Ascending,
    val nulls: NullsOrder = NullsOrder.Unspecified
)

/**
 * 分页规格 / Page specification
 *
 * @property limit 单页最大记录数 / Maximum records per page
 * @property offset 跳过的记录数 / Number of records to skip
 */
data class PageSpec(
    val limit: Int,
    val offset: Int = 0
)

/**
 * 查询计划限制 / Query plan limits
 *
 * @property maxJoins 允许的最大 Join 数 / Maximum number of allowed joins
 * @property maxDepth 允许的最大数据源深度 / Maximum allowed source depth
 * @property maxProjections 允许的最大投影字段数 / Maximum number of allowed projections
 */
data class RelationalQueryLimits(
    val maxJoins: Int = 16,
    val maxDepth: Int = 16,
    val maxProjections: Int = 128
)

/**
 * 关系查询计划容器 / Relational query plan container
 *
 * [rootKey] 用于按根粒度生成去重计数；未提供根键时，适配器不得伪造根粒度计数。
 * [rootKey] is used for root-granularity distinct counts; adapters must not fake root counts when it is absent.
 *
 * 外层集合和框架已知的表达式容器及可变容器会在构造时递归复制；无法安全复制的自定义 payload 或循环容器会在
 * [validate] 中以结构化错误拒绝，而不是被替换为伪快照。canonical 只描述表达式形状，不包含标量字面量原值。
 * Outer collections, known expression containers, and known mutable containers are recursively copied at construction
 * time. Custom payloads or cyclic containers that cannot be copied safely are rejected by [validate] as structured
 * errors instead of being replaced with a fake snapshot. canonical describes expression shape and does not include
 * scalar literal values.
 *
 * @property root 根数据源 / Root source
 * @property joins 按顺序应用的 Join 规格 / Join specifications applied in order
 * @property predicate 根查询谓词 / Root query predicate
 * @property projections 返回投影 / Returned projections
 * @property distinct 是否对投影去重 / Whether to distinct the projection
 * @property groupBy 分组列 / Grouping columns
 * @property orderBy 排序规格 / Ordering specifications
 * @property page 分页规格 / Optional pagination specification
 * @property rootKey 根粒度计数使用的根键 / Root key used for root-granularity counts
 */
class RelationalQueryPlan(
    root: QuerySource,
    joins: List<JoinSpec> = emptyList(),
    predicate: BooleanExpression? = null,
    projections: List<ProjectionSpec> = emptyList(),
    val distinct: Boolean = false,
    groupBy: List<ColumnRef> = emptyList(),
    orderBy: List<OrderSpec> = emptyList(),
    val page: PageSpec? = null,
    rootKey: List<ColumnRef> = emptyList()
) {
    val root: QuerySource = root.normalized()
    val joins: List<JoinSpec> = immutableList(joins.map { it.normalized() })
    val predicate: BooleanExpression? = predicate?.let(::freezeBoolean)
    val projections: List<ProjectionSpec> = immutableList(projections.map { it.normalized() })
    val groupBy: List<ColumnRef> = immutableList(groupBy.map { it.normalized() })
    val orderBy: List<OrderSpec> = immutableList(orderBy.map { it.normalized() })
    val rootKey: List<ColumnRef> = immutableList(rootKey.map { it.normalized() })

    /**
     * 校验计划结构 / Validate plan structure
     *
     * @param limits 计划限制 / Plan limits
     * @return 校验结果 / Validation result
     */
    fun validate(limits: RelationalQueryLimits = RelationalQueryLimits()): Ret<Unit> {
        fun failure(message: String, field: String): Ret<Unit> {
            return Failed(
                ErrorCode.IllegalArgument,
                message,
                RelationalQueryValidationError(field, message)
            )
        }

        if (root.name.isBlank()) return failure("Root source name must not be blank", "root")
        if (root.alias != null && root.alias.isBlank()) {
            return failure("Root source alias must not be blank", "root.alias")
        }
        if (root.alias == root.name) {
            return failure("Root source alias must differ from its name", "root.alias")
        }
        if (joins.size > limits.maxJoins) return failure("Join count exceeds configured limit", "joins")
        if (joins.size + 1 > limits.maxDepth) return failure("Join depth exceeds configured limit", "joins")
        if (projections.size > limits.maxProjections) {
            return failure("Projection count exceeds configured limit", "projections")
        }
        if (page != null && (page.limit <= 0 || page.offset < 0)) {
            return failure("Page limit must be positive and offset must not be negative", "page")
        }
        if (rootKey.any { it.source.isBlank() || it.path.isBlank() }) {
            return failure("Root key contains an empty column reference", "rootKey")
        }
        if (projections.any { it.column.source.isBlank() || it.column.path.isBlank() }) {
            return failure("Projection contains an empty column reference", "projections")
        }
        if (groupBy.any { it.source.isBlank() || it.path.isBlank() }) {
            return failure("Group by contains an empty column reference", "groupBy")
        }
        if (orderBy.any { it.column.source.isBlank() || it.column.path.isBlank() }) {
            return failure("Order by contains an empty column reference", "orderBy")
        }

        val cyclicExpression = predicate?.let { findExpressionCycle(it, "predicate") }
            ?: joins.mapIndexed { index, join ->
                findExpressionCycle(join.condition, "joins[$index].condition")
            }.firstOrNull { it != null }
        if (cyclicExpression != null) {
            return failure(
                "Expression tree must not contain a cycle / 表达式树不能包含循环引用",
                cyclicExpression
            )
        }

        val unsupportedPayloads = mutableListOf<String>()
        predicate?.let { unsupportedCustomPayload(it, "predicate") }?.let(unsupportedPayloads::add)
        joins.forEachIndexed { index, join ->
            unsupportedCustomPayload(join.condition, "joins[$index].condition")
                ?.let(unsupportedPayloads::add)
        }
        val unsupportedPayload = unsupportedPayloads.firstOrNull()
        if (unsupportedPayload != null) {
            return failure(
                "Custom expression payload cannot be safely copied; use an immutable scalar or a known container / " +
                    "自定义表达式 payload 无法安全复制，请使用不可变标量或框架支持的容器",
                unsupportedPayload
            )
        }

        val names = linkedSetOf(root.name)
        val aliases = linkedSetOf<String>().apply { root.alias?.let(::add) }
        val boundSources = linkedSetOf<String>().apply {
            add(root.name)
            root.alias?.let(::add)
        }
        joins.forEachIndexed { index, join ->
            if (join.source.name.isBlank()) {
                return failure("Join source name must not be blank", "joins[$index].source")
            }
            if (join.source.name in aliases) {
                return failure("Join source name conflicts with an alias: ${join.source.name}", "joins[$index].source")
            }
            if (!names.add(join.source.name)) {
                return failure("Join source is duplicated: ${join.source.name}", "joins[$index].source")
            }
            join.source.alias?.let { alias ->
                if (alias.isBlank()) {
                    return failure("Join source alias must not be blank", "joins[$index].source.alias")
                }
                if (alias == join.source.name || alias in names || alias in aliases) {
                    return failure("Join source alias is ambiguous: $alias", "joins[$index].source.alias")
                }
                if (!aliases.add(alias)) {
                    return failure("Join source alias is duplicated: $alias", "joins[$index].source.alias")
                }
            }
            if (join.condition.collectReferences().isEmpty()) {
                return failure(
                    "Join condition must contain a qualified column relationship",
                    "joins[$index].condition"
                )
            }
            if (containsBooleanConstant(join.condition)) {
                return failure(
                    "Join condition must not contain a boolean constant",
                    "joins[$index].condition"
                )
            }
            val targetSources = linkedSetOf<String>().apply {
                add(join.source.name)
                join.source.alias?.let(::add)
            }
            val unknownQualifier = join.condition.collectReferences()
                .mapNotNull { sourceQualifier(it.value) }
                .firstOrNull { it !in boundSources && it !in targetSources }
            if (unknownQualifier != null) {
                return failure(
                    "Join condition references an unknown or not-yet-bound source: $unknownQualifier",
                    "joins[$index].condition"
                )
            }
            if (!containsExactColumnCorrelation(join.condition, targetSources, boundSources)) {
                return failure(
                    "Join condition must relate the joined source to an already bound source",
                    "joins[$index].condition"
                )
            }
            if (join.type == JoinType.Exists && !join.cardinality.isToMany) {
                return failure(
                    "Exists join requires a to-many cardinality declaration",
                    "joins[$index].cardinality"
                )
            }
            if (join.type != JoinType.Exists) {
                boundSources += targetSources
            }
        }
        return Ok(Unit)
    }

    /**
     * 返回稳定的计划表示 / Return a stable representation of this plan
     *
     * 表示保留列表顺序，因为 Join 顺序可能决定可见数据源；And/Or 子树按语义规范化后编码。
     * List order is preserved because join order can determine visible sources; And/Or subtrees are semantically normalized before encoding.
     *
     * @return 规范化计划字符串 / Canonical plan string
     */
    fun canonical(): String {
        return buildString {
            append("root=")
            appendSource(root, this)
            append("|joins=")
            append(joins.size)
            joins.forEach { join ->
                append(";")
                append(join.type)
                append(":")
                append(join.cardinality)
                append(":")
                appendSource(join.source, this)
                append(":")
                appendToken(normalizedKey(join.condition))
            }
            append("|predicate=")
            appendToken(predicate?.let(::normalizedKey) ?: "")
            append("|projections=")
            append(projections.size)
            projections.forEach {
                append(";")
                appendColumn(it.column, this)
                append(":")
                appendToken(it.alias.orEmpty())
            }
            append("|distinct=")
            append(distinct)
            append("|groupBy=")
            appendColumns(groupBy, this)
            append("|orderBy=")
            append(orderBy.size)
            orderBy.forEach {
                append(";")
                appendColumn(it.column, this)
                append(":")
                append(it.direction)
                append(":")
                append(it.nulls)
            }
            append("|page=")
            page?.let {
                append(it.limit)
                append(":")
                append(it.offset)
            }
            append("|rootKey=")
            appendColumns(rootKey, this)
        }
    }

    /**
     * 获取规范化计划的 SHA-256 / Get the SHA-256 of the canonical plan
     *
     * @return 规范化计划的十六进制 SHA-256 摘要 / Hexadecimal SHA-256 digest of the canonical plan
     */
    fun canonicalHash(): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical().toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    /** 复制计划并重新防御性复制所有集合 / Copy the plan with defensive copies of all collections */
    /**
     * 复制计划并重新规范化输入 / Copy the plan and normalize the supplied inputs again
     *
     * @param root 根数据源 / Root source
     * @param joins 按顺序应用的 Join 规格 / Join specifications applied in order
     * @param predicate 根查询谓词 / Root query predicate
     * @param projections 返回投影 / Returned projections
     * @param distinct 是否对投影去重 / Whether to distinct the projection
     * @param groupBy 分组列 / Grouping columns
     * @param orderBy 排序规格 / Ordering specifications
     * @param page 分页规格 / Optional pagination specification
     * @param rootKey 根粒度计数使用的根键 / Root key used for root-granularity counts
     * @return 重新构造并防御性复制的查询计划 / Reconstructed query plan with defensive copies
     */
    fun copy(
        root: QuerySource = this.root,
        joins: List<JoinSpec> = this.joins,
        predicate: BooleanExpression? = this.predicate,
        projections: List<ProjectionSpec> = this.projections,
        distinct: Boolean = this.distinct,
        groupBy: List<ColumnRef> = this.groupBy,
        orderBy: List<OrderSpec> = this.orderBy,
        page: PageSpec? = this.page,
        rootKey: List<ColumnRef> = this.rootKey
    ): RelationalQueryPlan {
        return RelationalQueryPlan(
            root = root,
            joins = joins,
            predicate = predicate,
            projections = projections,
            distinct = distinct,
            groupBy = groupBy,
            orderBy = orderBy,
            page = page,
            rootKey = rootKey
        )
    }

    /**
     * 按查询计划语义比较对象 / Compare objects by query plan semantics
     *
     * @param other 待比较对象 / Object to compare
     * @return 对象是否表示相同的查询计划 / Whether the object represents the same query plan
     */
    override fun equals(other: Any?): Boolean {
        return other is RelationalQueryPlan &&
            root == other.root &&
            joins == other.joins &&
            predicate == other.predicate &&
            projections == other.projections &&
            distinct == other.distinct &&
            groupBy == other.groupBy &&
            orderBy == other.orderBy &&
            page == other.page &&
            rootKey == other.rootKey
    }

    /**
     * 计算查询计划哈希值 / Calculate the query plan hash code
     *
     * @return 查询计划哈希值 / Query plan hash code
     */
    override fun hashCode(): Int {
        var result = root.hashCode()
        result = 31 * result + joins.hashCode()
        result = 31 * result + (predicate?.hashCode() ?: 0)
        result = 31 * result + projections.hashCode()
        result = 31 * result + distinct.hashCode()
        result = 31 * result + groupBy.hashCode()
        result = 31 * result + orderBy.hashCode()
        result = 31 * result + (page?.hashCode() ?: 0)
        result = 31 * result + rootKey.hashCode()
        return result
    }

    /**
     * 返回查询计划的规范化文本 / Return the canonical text of the query plan
     *
     * @return 规范化查询计划文本 / Canonical query plan text
     */
    override fun toString(): String = "RelationalQueryPlan(${canonical()})"
}

private fun QuerySource.normalized(): QuerySource {
    return QuerySource(name.trim(), alias?.trim())
}

private fun ColumnRef.normalized(): ColumnRef {
    return ColumnRef(source.trim(), path.trim())
}

private fun ProjectionSpec.normalized(): ProjectionSpec {
    return ProjectionSpec(column.normalized(), alias?.trim())
}

private fun OrderSpec.normalized(): OrderSpec {
    return OrderSpec(column.normalized(), direction, nulls)
}

private fun JoinSpec.normalized(): JoinSpec {
    return JoinSpec(type, source.normalized(), freezeBoolean(condition), cardinality)
}

private fun <T> immutableList(values: List<T>): List<T> {
    return Collections.unmodifiableList(values.toList())
}

private fun StringBuilder.appendSource(source: QuerySource, target: StringBuilder) {
    target.appendToken(source.name)
    target.append(":")
    target.appendToken(source.alias.orEmpty())
}

private fun StringBuilder.appendColumn(column: ColumnRef, target: StringBuilder) {
    target.appendToken(column.source)
    target.append(":")
    target.appendToken(column.path)
}

private fun StringBuilder.appendColumns(columns: List<ColumnRef>, target: StringBuilder) {
    target.append(columns.size)
    columns.forEach {
        target.append(";")
        appendColumn(it, target)
    }
}

private fun StringBuilder.appendToken(value: String) {
    append(value.length)
    append(":")
    append(value)
}

private val canonicalNormalizeConfig = NormalizeConfig(
    deduplicate = false,
    sortOperands = false
)

@Suppress("UNCHECKED_CAST")
private fun normalizedKey(expression: BooleanExpression): String {
    if (findExpressionCycle(expression, "expression") != null) {
        return canonicalNode("ExpressionCycle")
    }
    return normalizeForCanonical(expression).canonicalQueryKey()
}

/**
 * Normalize every boolean node, including boolean expressions nested inside scalar expressions.
 * 规范化所有布尔节点，包括嵌套在标量表达式中的布尔表达式。
 */
@Suppress("UNCHECKED_CAST")
private fun normalizeForCanonical(expression: BooleanExpression): BooleanExpression {
    val prepared = when (expression) {
        is BooleanConstant, is NullCheck, is BooleanCustom -> expression
        is Comparison<*> -> Comparison(
            operator = expression.operator,
            left = normalizeScalarForCanonical(expression.left) as ScalarExpression<Any?>,
            right = normalizeScalarForCanonical(expression.right) as ScalarExpression<Any?>
        )
        is InExpression<*> -> InExpression(
            value = normalizeScalarForCanonical(expression.value) as ScalarExpression<Any?>,
            candidates = expression.candidates.map {
                normalizeScalarForCanonical(it) as ScalarExpression<Any?>
            },
            negated = expression.negated
        )
        is PatternMatch<*> -> PatternMatch(
            value = normalizeScalarForCanonical(expression.value) as ScalarExpression<Any?>,
            pattern = normalizeScalarForCanonical(expression.pattern) as ScalarExpression<Any?>,
            mode = expression.mode,
            negated = expression.negated
        )
        is AndExpression -> AndExpression(expression.operands.map(::normalizeForCanonical))
        is OrExpression -> OrExpression(expression.operands.map(::normalizeForCanonical))
        is NotExpression -> NotExpression(normalizeForCanonical(expression.operand))
    }
    return deduplicateForCanonical(normalize(prepared, canonicalNormalizeConfig))
}

@Suppress("UNCHECKED_CAST")
private fun normalizeScalarForCanonical(expression: ScalarExpression<*>): ScalarExpression<*> {
    return when (expression) {
        is ScalarConstant<*>, is ScalarReference<*>, is ScalarSymbolReference<*>, is ScalarCustom<*> -> expression
        is ScalarUnary<*> -> ScalarUnary(
            operator = expression.operator,
            operand = normalizeScalarForCanonical(expression.operand) as ScalarExpression<Any?>
        )
        is ScalarBinary<*> -> ScalarBinary(
            operator = expression.operator,
            left = normalizeScalarForCanonical(expression.left) as ScalarExpression<Any?>,
            right = normalizeScalarForCanonical(expression.right) as ScalarExpression<Any?>
        )
        is ScalarFunction<*> -> ScalarFunction(
            name = expression.name,
            arguments = expression.arguments.map {
                normalizeScalarForCanonical(it) as ScalarExpression<Any?>
            }
        )
        is ScalarConditional<*> -> ScalarConditional(
            condition = normalizeForCanonical(expression.condition),
            thenBranch = normalizeScalarForCanonical(expression.thenBranch) as ScalarExpression<Any?>,
            elseBranch = normalizeScalarForCanonical(expression.elseBranch) as ScalarExpression<Any?>
        )
        is ScalarBoolean<*> -> ScalarBoolean<Any?>(normalizeForCanonical(expression.expr))
    }
}

private fun deduplicateForCanonical(expression: BooleanExpression): BooleanExpression {
    return when (expression) {
        is AndExpression -> {
            val operands = expression.operands.map(::deduplicateForCanonical)
                .distinctBy { operand -> operand.normalizationKey() }
            when (operands.size) {
                0 -> BooleanConstant(fuookami.ospf.kotlin.math.Trivalent.True)
                1 -> operands.first()
                else -> AndExpression(operands)
            }
        }
        is OrExpression -> {
            val operands = expression.operands.map(::deduplicateForCanonical)
                .distinctBy { operand -> operand.normalizationKey() }
            when (operands.size) {
                0 -> BooleanConstant(fuookami.ospf.kotlin.math.Trivalent.False)
                1 -> operands.first()
                else -> OrExpression(operands)
            }
        }
        is NotExpression -> NotExpression(deduplicateForCanonical(expression.operand))
        else -> expression
    }
}

private fun BooleanExpression.normalizationKey(): String = when (this) {
    is BooleanConstant -> canonicalNode("BooleanConstant", trivalentKey(value))
    is Comparison<*> -> canonicalNode(
        "Comparison",
        operator.name,
        left.normalizationKey(),
        right.normalizationKey()
    )
    is InExpression<*> -> canonicalNode(
        "In",
        negated.toString(),
        value.normalizationKey(),
        candidates.map { it.normalizationKey() }.sorted().joinToString(";")
    )
    is PatternMatch<*> -> canonicalNode(
        "PatternMatch",
        mode.name,
        negated.toString(),
        value.normalizationKey(),
        pattern.normalizationKey()
    )
    is NullCheck -> canonicalNode("NullCheck", type.name, path.value)
    is AndExpression -> canonicalNode("And", operands.map { it.normalizationKey() }.sorted())
    is OrExpression -> canonicalNode("Or", operands.map { it.normalizationKey() }.sorted())
    is NotExpression -> canonicalNode("Not", operand.normalizationKey())
    is BooleanCustom -> canonicalNode("BooleanCustom", literalKey(value), descriptionKey(description))
}

private fun ScalarExpression<*>.normalizationKey(): String = when (this) {
    is ScalarConstant<*> -> canonicalNode("ScalarConstant", literalKey(value))
    is ScalarReference<*> -> canonicalNode("ScalarReference", path.value)
    is ScalarSymbolReference<*> -> canonicalNode("ScalarSymbolReference", symbolIdentityKey(symbol))
    is ScalarUnary<*> -> canonicalNode("ScalarUnary", operator.name, operand.normalizationKey())
    is ScalarBinary<*> -> canonicalNode(
        "ScalarBinary",
        operator.name,
        left.normalizationKey(),
        right.normalizationKey()
    )
    is ScalarFunction<*> -> canonicalNode(
        "ScalarFunction",
        name,
        arguments.joinToString(";") { it.normalizationKey() }
    )
    is ScalarConditional<*> -> canonicalNode(
        "ScalarConditional",
        condition.normalizationKey(),
        thenBranch.normalizationKey(),
        elseBranch.normalizationKey()
    )
    is ScalarBoolean<*> -> canonicalNode("ScalarBoolean", expr.normalizationKey())
    is ScalarCustom<*> -> canonicalNode("ScalarCustom", literalKey(value), descriptionKey(description))
}

/**
 * Build a stable key for a normalized expression without delegating custom
 * payload identity to Any.toString() or a mutable collection.
 * 构造规范化表达式的稳定键，不依赖 Any.toString() 或可变集合表达自定义 payload 身份。
 */
private fun BooleanExpression.canonicalQueryKey(): String = when (this) {
    is BooleanConstant -> canonicalNode("BooleanConstant", trivalentKey(value))
    is Comparison<*> -> canonicalNode(
        "Comparison",
        operator.name,
        left.canonicalQueryKey(),
        right.canonicalQueryKey()
    )
    is InExpression<*> -> canonicalNode(
        "In",
        negated.toString(),
        value.canonicalQueryKey(),
        canonicalNode("Candidates", candidates.map { it.canonicalQueryKey() }.sorted())
    )
    is PatternMatch<*> -> canonicalNode(
        "PatternMatch",
        mode.name,
        negated.toString(),
        value.canonicalQueryKey(),
        pattern.canonicalQueryKey()
    )
    is NullCheck -> canonicalNode("NullCheck", type.name, path.value)
    is AndExpression -> canonicalNode("And", operands.map(BooleanExpression::canonicalQueryKey).sorted())
    is OrExpression -> canonicalNode("Or", operands.map(BooleanExpression::canonicalQueryKey).sorted())
    is NotExpression -> canonicalNode("Not", operand.canonicalQueryKey())
    is BooleanCustom -> canonicalNode("BooleanCustom", customPayloadShape(value), descriptionKey(description))
}

private fun ScalarExpression<*>.canonicalQueryKey(): String = when (this) {
    is ScalarConstant<*> -> canonicalNode("ScalarConstant", customPayloadShape(value))
    is ScalarReference<*> -> canonicalNode("ScalarReference", path.value)
    is ScalarSymbolReference<*> -> canonicalNode("ScalarSymbolReference", symbolIdentityKey(symbol))
    is ScalarUnary<*> -> canonicalNode("ScalarUnary", operator.name, operand.canonicalQueryKey())
    is ScalarBinary<*> -> canonicalNode(
        "ScalarBinary",
        operator.name,
        left.canonicalQueryKey(),
        right.canonicalQueryKey()
    )
    is ScalarFunction<*> -> canonicalNode(
        "ScalarFunction",
        name,
        canonicalNode("Arguments", arguments.map { it.canonicalQueryKey() })
    )
    is ScalarConditional<*> -> canonicalNode(
        "ScalarConditional",
        condition.canonicalQueryKey(),
        thenBranch.canonicalQueryKey(),
        elseBranch.canonicalQueryKey()
    )
    is ScalarBoolean<*> -> canonicalNode("ScalarBoolean", expr.canonicalQueryKey())
    is ScalarCustom<*> -> canonicalNode("ScalarCustom", customPayloadShape(value), descriptionKey(description))
}

private fun canonicalNode(tag: String, vararg parts: String): String {
    return canonicalNode(tag, parts.toList())
}

private fun canonicalNode(tag: String, parts: List<String>): String = buildString {
    appendToken(tag)
    append(parts.size)
    parts.forEach(::appendToken)
}

private fun trivalentKey(value: fuookami.ospf.kotlin.math.Trivalent): String {
    return when (value) {
        fuookami.ospf.kotlin.math.Trivalent.True -> "True"
        fuookami.ospf.kotlin.math.Trivalent.False -> "False"
        fuookami.ospf.kotlin.math.Trivalent.Unknown -> "Unknown"
    }
}

private fun symbolIdentityKey(symbol: Symbol): String {
    val stableId = (symbol as? StableSymbol)?.stableSymbolId?.value
    return if (stableId != null) {
        canonicalNode("stable", stableId)
    } else {
        canonicalNode("identity", System.identityHashCode(symbol).toString())
    }
}

private data class UnsupportedCustomPayload(val typeName: String)

private data class FrozenArray(
    val kind: String,
    val elements: List<Any?>
)

/**
 * Build a shape-only payload key; payload values are deliberately excluded.
 * 构造只包含形状的 payload 键；明确排除 payload 字面量值。
 */
private fun customPayloadShape(value: Any?): String = when {
    value == null -> canonicalNode("null")
    value is UnsupportedCustomPayload -> canonicalNode("unsupported", value.typeName)
    value is FrozenArray -> canonicalNode(value.kind, value.elements.map(::customPayloadShape))
    value is String -> canonicalNode("string")
    value is Char -> canonicalNode("char")
    value is Boolean -> canonicalNode("boolean")
    value is Byte || value is Short || value is Int || value is Long ||
        value is Float || value is Double || value is java.math.BigInteger || value is java.math.BigDecimal ||
        value is UInt64 || value is FltX -> {
        canonicalNode("number", typeName(value))
    }
    value is Enum<*> && isKnownImmutableEnumType(value::class.java) -> {
        canonicalNode("enum", typeName(value))
    }
    isKnownImmutableScalar(value) && value is java.time.temporal.Temporal -> {
        canonicalNode("temporal", typeName(value))
    }
    isKnownImmutableScalar(value) && value is java.time.temporal.TemporalAmount -> {
        canonicalNode("temporalAmount", typeName(value))
    }
    value is Map<*, *> -> {
        val entries = value.entries.map {
            canonicalNode("entry", customPayloadShape(it.key), customPayloadShape(it.value))
        }.sorted()
        canonicalNode("map", entries)
    }
    value is Set<*> -> canonicalNode("set", value.map(::customPayloadShape).sorted())
    value is Collection<*> -> canonicalNode("collection", value.map(::customPayloadShape))
    value is Array<*> -> canonicalNode("array", value.map(::customPayloadShape))
    value is BooleanArray -> canonicalNode("booleanArray", value.size.toString())
    value is ByteArray -> canonicalNode("byteArray", value.size.toString())
    value is CharArray -> canonicalNode("charArray", value.size.toString())
    value is DoubleArray -> canonicalNode("doubleArray", value.size.toString())
    value is FloatArray -> canonicalNode("floatArray", value.size.toString())
    value is IntArray -> canonicalNode("intArray", value.size.toString())
    value is LongArray -> canonicalNode("longArray", value.size.toString())
    value is ShortArray -> canonicalNode("shortArray", value.size.toString())
    else -> canonicalNode("opaque", typeName(value))
}

private fun literalKey(value: Any?): String {
    return literalKey(value, IdentityHashMap())
}

private fun literalKey(value: Any?, active: IdentityHashMap<Any, Boolean>): String {
    return when {
        value == null -> canonicalNode("null")
        value is UnsupportedCustomPayload -> canonicalNode("unsupported", value.typeName)
        value is FrozenArray -> canonicalNode(value.kind, value.elements.map { literalKey(it, active) })
        value is String -> canonicalNode("string", value)
        value is Char -> canonicalNode("char", value.toString())
        value is Boolean -> canonicalNode("boolean", value.toString())
        value is Byte || value is Short || value is Int || value is Long ||
            value is Float || value is Double || value is java.math.BigInteger || value is java.math.BigDecimal ||
            value is UInt64 || value is FltX -> {
            canonicalNode("number", typeName(value), value.toString())
        }
        value is Enum<*> && isKnownImmutableEnumType(value::class.java) -> {
            canonicalNode("enum", typeName(value), value.name)
        }
        isKnownImmutableScalar(value) && value is java.time.temporal.Temporal -> {
            canonicalNode("temporal", typeName(value), value.toString())
        }
        isKnownImmutableScalar(value) && value is java.time.temporal.TemporalAmount -> {
            canonicalNode("temporalAmount", typeName(value), value.toString())
        }
        value is Map<*, *> -> withActiveValue(value, active) {
            canonicalNode(
                "map",
                value.entries.map {
                    canonicalNode("entry", literalKey(it.key, active), literalKey(it.value, active))
                }.sorted()
            )
        }
        value is Set<*> -> withActiveValue(value, active) {
            canonicalNode("set", value.map { literalKey(it, active) }.sorted())
        }
        value is Collection<*> -> withActiveValue(value, active) {
            canonicalNode("collection", value.map { literalKey(it, active) })
        }
        value is Array<*> -> withActiveValue(value, active) {
            canonicalNode("array", value.map { literalKey(it, active) })
        }
        value is BooleanArray -> canonicalNode("booleanArray", value.joinToString(","))
        value is ByteArray -> canonicalNode("byteArray", value.joinToString(","))
        value is CharArray -> canonicalNode("charArray", value.concatToString())
        value is DoubleArray -> canonicalNode("doubleArray", value.joinToString(","))
        value is FloatArray -> canonicalNode("floatArray", value.joinToString(","))
        value is IntArray -> canonicalNode("intArray", value.joinToString(","))
        value is LongArray -> canonicalNode("longArray", value.joinToString(","))
        value is ShortArray -> canonicalNode("shortArray", value.joinToString(","))
        else -> canonicalNode("opaque", typeName(value), System.identityHashCode(value).toString())
    }
}

private fun withActiveValue(
    value: Any,
    active: IdentityHashMap<Any, Boolean>,
    copy: () -> String
): String {
    if (active.put(value, true) != null) {
        return canonicalNode("cycle", typeName(value))
    }
    return try {
        copy()
    } finally {
        active.remove(value)
    }
}

private fun descriptionKey(description: String?): String {
    return description?.let { canonicalNode("description", it) } ?: canonicalNode("description")
}

private fun typeName(value: Any): String {
    return value::class.qualifiedName ?: value::class.java.name
}

private fun findExpressionCycle(expression: BooleanExpression, field: String): String? {
    return findExpressionCycle(expression, field, IdentityHashMap())
}

private fun findExpressionCycle(
    expression: BooleanExpression,
    field: String,
    active: IdentityHashMap<Any, Boolean>
): String? {
    if (active.put(expression, true) != null) return field
    return try {
        when (expression) {
            is BooleanConstant, is NullCheck, is BooleanCustom -> null
            is Comparison<*> -> {
                findScalarExpressionCycle(expression.left, "$field.left", active)
                    ?: findScalarExpressionCycle(expression.right, "$field.right", active)
            }
            is InExpression<*> -> {
                findScalarExpressionCycle(expression.value, "$field.value", active)
                    ?: expression.candidates.mapIndexed { index, candidate ->
                        findScalarExpressionCycle(candidate, "$field.candidates[$index]", active)
                    }.firstOrNull { it != null }
            }
            is PatternMatch<*> -> {
                findScalarExpressionCycle(expression.value, "$field.value", active)
                    ?: findScalarExpressionCycle(expression.pattern, "$field.pattern", active)
            }
            is AndExpression -> expression.operands.mapIndexed { index, operand ->
                findExpressionCycle(operand, "$field.operands[$index]", active)
            }.firstOrNull { it != null }
            is OrExpression -> expression.operands.mapIndexed { index, operand ->
                findExpressionCycle(operand, "$field.operands[$index]", active)
            }.firstOrNull { it != null }
            is NotExpression -> findExpressionCycle(expression.operand, "$field.operand", active)
        }
    } finally {
        active.remove(expression)
    }
}

private fun findScalarExpressionCycle(
    expression: ScalarExpression<*>,
    field: String,
    active: IdentityHashMap<Any, Boolean>
): String? {
    if (active.put(expression, true) != null) return field
    return try {
        when (expression) {
            is ScalarConstant<*>, is ScalarReference<*>, is ScalarSymbolReference<*>, is ScalarCustom<*> -> null
            is ScalarUnary<*> -> findScalarExpressionCycle(expression.operand, "$field.operand", active)
            is ScalarBinary<*> -> {
                findScalarExpressionCycle(expression.left, "$field.left", active)
                    ?: findScalarExpressionCycle(expression.right, "$field.right", active)
            }
            is ScalarFunction<*> -> expression.arguments.mapIndexed { index, argument ->
                findScalarExpressionCycle(argument, "$field.arguments[$index]", active)
            }.firstOrNull { it != null }
            is ScalarConditional<*> -> {
                findExpressionCycle(expression.condition, "$field.condition", active)
                    ?: findScalarExpressionCycle(expression.thenBranch, "$field.thenBranch", active)
                    ?: findScalarExpressionCycle(expression.elseBranch, "$field.elseBranch", active)
            }
            is ScalarBoolean<*> -> findExpressionCycle(expression.expr, "$field.expr", active)
        }
    } finally {
        active.remove(expression)
    }
}

private fun sourceQualifier(path: String): String? {
    val separator = path.indexOf('.')
    return if (separator > 0 && separator < path.lastIndex) {
        path.substring(0, separator)
    } else {
        null
    }
}

private fun containsBooleanConstant(expression: BooleanExpression): Boolean {
    return when (expression) {
        is BooleanConstant -> true
        is Comparison<*> -> {
            containsBooleanConstant(expression.left) || containsBooleanConstant(expression.right)
        }
        is InExpression<*> -> {
            containsBooleanConstant(expression.value) ||
                expression.candidates.any(::containsBooleanConstant)
        }
        is PatternMatch<*> -> {
            containsBooleanConstant(expression.value) || containsBooleanConstant(expression.pattern)
        }
        is NullCheck -> false
        is AndExpression -> expression.operands.any(::containsBooleanConstant)
        is OrExpression -> expression.operands.any(::containsBooleanConstant)
        is NotExpression -> containsBooleanConstant(expression.operand)
        is BooleanCustom -> false
    }
}

private fun containsBooleanConstant(expression: ScalarExpression<*>): Boolean {
    return when (expression) {
        is ScalarConstant<*>, is ScalarReference<*>, is ScalarSymbolReference<*>, is ScalarCustom<*> -> false
        is ScalarUnary<*> -> containsBooleanConstant(expression.operand)
        is ScalarBinary<*> -> {
            containsBooleanConstant(expression.left) || containsBooleanConstant(expression.right)
        }
        is ScalarFunction<*> -> expression.arguments.any(::containsBooleanConstant)
        is ScalarConditional<*> -> {
            containsBooleanConstant(expression.condition) ||
                containsBooleanConstant(expression.thenBranch) ||
                containsBooleanConstant(expression.elseBranch)
        }
        is ScalarBoolean<*> -> true
    }
}

/**
 * 判断表达式是否包含精确的列对列关联 / Check whether an expression contains an exact column-to-column correlation
 *
 * 比较两端必须直接是限定的标量列引用，且一端属于目标数据源、另一端属于已绑定数据源。
 * Both comparison operands must be qualified scalar column references, with one side belonging to the target
 * source and the other side belonging to an already bound source.
 *
 * AND 允许额外过滤条件；OR 要求每个分支都保留关联条件；NOT 递归检查其操作数。
 * AND allows additional filters; OR requires every branch to retain a correlation; NOT checks its operand recursively.
 *
 * @param expression 待检查的布尔表达式 / Boolean expression to inspect
 * @param targetSources 当前目标数据源的名称和别名 / Names and aliases of the current target source
 * @param boundSources 已绑定数据源的名称和别名 / Names and aliases of already bound sources
 * @return 是否包含满足要求的列对列关联 / Whether the expression contains a permitted correlation
 */
fun containsExactColumnCorrelation(
    expression: BooleanExpression,
    targetSources: Set<String>,
    boundSources: Set<String>
): Boolean {
    return when (expression) {
        is Comparison<*> -> {
            val left = expression.left as? ScalarReference<*>
            val right = expression.right as? ScalarReference<*>
            if (left == null || right == null) {
                false
            } else {
                val leftQualifier = sourceQualifier(left.path.value)
                val rightQualifier = sourceQualifier(right.path.value)
                leftQualifier != null && rightQualifier != null &&
                    ((leftQualifier in targetSources && rightQualifier in boundSources) ||
                        (rightQualifier in targetSources && leftQualifier in boundSources))
            }
        }
        is AndExpression -> expression.operands.any {
            containsExactColumnCorrelation(it, targetSources, boundSources)
        }
        is OrExpression -> expression.operands.isNotEmpty() && expression.operands.all {
            containsExactColumnCorrelation(it, targetSources, boundSources)
        }
        is NotExpression -> containsExactColumnCorrelation(expression.operand, targetSources, boundSources)
        is BooleanConstant, is InExpression<*>, is PatternMatch<*>, is NullCheck, is BooleanCustom -> false
    }
}

@Suppress("UNCHECKED_CAST")
private fun freezeBoolean(expression: BooleanExpression): BooleanExpression {
    return freezeBoolean(expression, IdentityHashMap())
}

@Suppress("UNCHECKED_CAST")
private fun freezeBoolean(
    expression: BooleanExpression,
    active: IdentityHashMap<Any, Boolean>
): BooleanExpression {
    if (active.put(expression, true) != null) return expression
    return try {
        when (expression) {
            is BooleanConstant, is NullCheck -> expression
            is BooleanCustom -> BooleanCustom(
                value = freezeCustomValue(expression.value)!!,
                description = expression.description
            )
            is Comparison<*> -> Comparison(
                operator = expression.operator,
                left = freezeScalar(expression.left, active) as ScalarExpression<Any?>,
                right = freezeScalar(expression.right, active) as ScalarExpression<Any?>
            )
            is InExpression<*> -> InExpression(
                value = freezeScalar(expression.value, active) as ScalarExpression<Any?>,
                candidates = immutableList(
                    expression.candidates.map { freezeScalar(it, active) as ScalarExpression<Any?> }
                ),
                negated = expression.negated
            )
            is PatternMatch<*> -> PatternMatch(
                value = freezeScalar(expression.value, active) as ScalarExpression<Any?>,
                pattern = freezeScalar(expression.pattern, active) as ScalarExpression<Any?>,
                mode = expression.mode,
                negated = expression.negated
            )
            is AndExpression -> AndExpression(
                immutableList(expression.operands.map { freezeBoolean(it, active) })
            )
            is OrExpression -> OrExpression(
                immutableList(expression.operands.map { freezeBoolean(it, active) })
            )
            is NotExpression -> NotExpression(freezeBoolean(expression.operand, active))
        }
    } finally {
        active.remove(expression)
    }
}

@Suppress("UNCHECKED_CAST")
private fun freezeScalar(expression: ScalarExpression<*>): ScalarExpression<*> {
    return freezeScalar(expression, IdentityHashMap())
}

@Suppress("UNCHECKED_CAST")
private fun freezeScalar(
    expression: ScalarExpression<*>,
    active: IdentityHashMap<Any, Boolean>
): ScalarExpression<*> {
    if (active.put(expression, true) != null) return expression
    return try {
        when (expression) {
            is ScalarConstant<*> -> ScalarConstant(freezeCustomValue(expression.value))
            is ScalarReference<*> -> expression
            is ScalarSymbolReference<*> -> ScalarSymbolReference<Any?>(freezeSymbol(expression.symbol))
            is ScalarCustom<*> -> ScalarCustom<Any?>(
                value = freezeCustomValue(expression.value)!!,
                description = expression.description
            )
            is ScalarUnary<*> -> ScalarUnary(
                operator = expression.operator,
                operand = freezeScalar(expression.operand, active) as ScalarExpression<Any?>
            )
            is ScalarBinary<*> -> ScalarBinary(
                operator = expression.operator,
                left = freezeScalar(expression.left, active) as ScalarExpression<Any?>,
                right = freezeScalar(expression.right, active) as ScalarExpression<Any?>
            )
            is ScalarFunction<*> -> ScalarFunction(
                name = expression.name,
                arguments = immutableList(
                    expression.arguments.map { freezeScalar(it, active) as ScalarExpression<Any?> }
                )
            )
            is ScalarConditional<*> -> ScalarConditional(
                condition = freezeBoolean(expression.condition, active),
                thenBranch = freezeScalar(expression.thenBranch, active) as ScalarExpression<Any?>,
                elseBranch = freezeScalar(expression.elseBranch, active) as ScalarExpression<Any?>
            )
            is ScalarBoolean<*> -> ScalarBoolean<Any?>(freezeBoolean(expression.expr, active))
        }
    } finally {
        active.remove(expression)
    }
}

private fun freezeSymbol(symbol: Symbol): Symbol {
    val stableId = (symbol as? StableSymbol)?.stableSymbolId?.value
        ?: "${symbol.name}#${System.identityHashCode(symbol)}"
    return OwnedSymbol(
        id = SymbolId(stableId),
        name = symbol.name,
        displayName = symbol.displayName
    )
}

/**
 * A custom expression is never translated by the relational compiler, but it
 * still belongs to the immutable query-plan boundary. Snapshot known mutable
 * containers; opaque objects are retained and rejected by validation.
 * 自定义表达式不会由关系编译器翻译，但仍属于不可变计划边界；快照已知可变容器，
 * 不透明对象保留原值并由校验结构化拒绝。
 */
private fun freezeCustomValue(value: Any?): Any? {
    return freezeCustomValue(value, IdentityHashMap())
}

private fun freezeCustomValue(value: Any?, active: IdentityHashMap<Any, Boolean>): Any? {
    return when {
        value == null || value is FrozenArray || isKnownImmutableScalar(value) -> value
        value is Map<*, *> -> {
            val map = value
            withActiveContainer(map, active) {
                Collections.unmodifiableMap(
                    map.entries.associate { (key, item) ->
                        freezeCustomValue(key, active) to freezeCustomValue(item, active)
                    }
                )
            }
        }
        value is Set<*> -> {
            val set = value
            withActiveContainer(set, active) {
                Collections.unmodifiableSet(set.map { freezeCustomValue(it, active) }.toSet())
            }
        }
        value is Collection<*> -> {
            val collection = value
            withActiveContainer(collection, active) {
                Collections.unmodifiableList(collection.map { freezeCustomValue(it, active) })
            }
        }
        value is Array<*> -> {
            val array = value
            withActiveContainer(array, active) {
                FrozenArray("array", immutableList(array.map { freezeCustomValue(it, active) }))
            }
        }
        value is BooleanArray -> withActiveContainer(value, active) {
            FrozenArray("booleanArray", immutableList(value.toList()))
        }
        value is ByteArray -> withActiveContainer(value, active) {
            FrozenArray("byteArray", immutableList(value.toList()))
        }
        value is CharArray -> withActiveContainer(value, active) {
            FrozenArray("charArray", immutableList(value.toList()))
        }
        value is DoubleArray -> withActiveContainer(value, active) {
            FrozenArray("doubleArray", immutableList(value.toList()))
        }
        value is FloatArray -> withActiveContainer(value, active) {
            FrozenArray("floatArray", immutableList(value.toList()))
        }
        value is IntArray -> withActiveContainer(value, active) {
            FrozenArray("intArray", immutableList(value.toList()))
        }
        value is LongArray -> withActiveContainer(value, active) {
            FrozenArray("longArray", immutableList(value.toList()))
        }
        value is ShortArray -> withActiveContainer(value, active) {
            FrozenArray("shortArray", immutableList(value.toList()))
        }
        else -> value
    }
}

private fun withActiveContainer(
    value: Any,
    active: IdentityHashMap<Any, Boolean>,
    copy: () -> Any?
): Any? {
    if (active.put(value, true) != null) {
        return UnsupportedCustomPayload(value::class.qualifiedName ?: value::class.java.name)
    }
    return try {
        copy()
    } finally {
        active.remove(value)
    }
}

private fun isKnownImmutableScalar(value: Any): Boolean {
    val typeName = value::class.java.name
    return value is String ||
        value is Char ||
        value is Boolean ||
        value is Byte ||
        value is Short ||
        value is Int ||
        value is Long ||
        value is Float ||
        value is Double ||
        value is java.math.BigInteger ||
        value is java.math.BigDecimal ||
        value is UInt64 ||
        value is FltX ||
        (value is Enum<*> && isKnownImmutableEnumType(value::class.java)) ||
            isKnownImmutableValueClass(value::class.java) ||
        isStatelessSingleton(value::class.java) ||
        ((value is java.time.temporal.Temporal || value is java.time.temporal.TemporalAmount) &&
            typeName.startsWith("java.time."))
}

private fun isKnownImmutableValueClass(type: Class<*>): Boolean {
    if (!type.isAnnotationPresent(kotlin.jvm.JvmInline::class.java)) return false
    val instanceFields = type.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }
    return instanceFields.size == 1 && isKnownImmutableType(instanceFields.single().type)
}

private fun isKnownImmutableType(type: Class<*>): Boolean {
    return type.isPrimitive ||
        type == String::class.java ||
        type == Char::class.javaObjectType ||
        type == Boolean::class.javaObjectType ||
        type == Byte::class.javaObjectType ||
        type == Short::class.javaObjectType ||
        type == Int::class.javaObjectType ||
        type == Long::class.javaObjectType ||
        type == Float::class.javaObjectType ||
        type == Double::class.javaObjectType ||
        type == java.math.BigInteger::class.java ||
        type == java.math.BigDecimal::class.java ||
        type == UInt64::class.java ||
        type == FltX::class.java ||
        isKnownImmutableEnumType(type) ||
        ((java.time.temporal.Temporal::class.java.isAssignableFrom(type) ||
            java.time.temporal.TemporalAmount::class.java.isAssignableFrom(type)) &&
            type.name.startsWith("java.time."))
}

private fun isKnownImmutableEnumType(type: Class<*>): Boolean {
    return type.isEnum && type.declaredFields.none { !Modifier.isStatic(it.modifiers) }
}

private fun isStatelessSingleton(type: Class<*>): Boolean {
    return type.declaredFields.any { Modifier.isStatic(it.modifiers) && it.name == "INSTANCE" } &&
        type.declaredFields.none { !Modifier.isStatic(it.modifiers) }
}

private fun unsupportedCustomPayload(expression: BooleanExpression, field: String): String? {
    return when (expression) {
        is BooleanConstant,
        is NullCheck -> null
        is BooleanCustom -> unsupportedCustomPayload(expression.value, "$field.value")
        is Comparison<*> -> {
            unsupportedCustomPayload(expression.left, "$field.left")
                ?: unsupportedCustomPayload(expression.right, "$field.right")
        }
        is InExpression<*> -> {
            unsupportedCustomPayload(expression.value, "$field.value")
                ?: expression.candidates.mapIndexed { index, candidate ->
                    unsupportedCustomPayload(candidate, "$field.candidates[$index]")
                }.firstOrNull { it != null }
        }
        is PatternMatch<*> -> {
            unsupportedCustomPayload(expression.value, "$field.value")
                ?: unsupportedCustomPayload(expression.pattern, "$field.pattern")
        }
        is AndExpression -> expression.operands.mapIndexed { index, operand ->
            unsupportedCustomPayload(operand, "$field.operands[$index]")
        }.firstOrNull { it != null }
        is OrExpression -> expression.operands.mapIndexed { index, operand ->
            unsupportedCustomPayload(operand, "$field.operands[$index]")
        }.firstOrNull { it != null }
        is NotExpression -> unsupportedCustomPayload(expression.operand, "$field.operand")
    }
}

private fun unsupportedCustomPayload(expression: ScalarExpression<*>, field: String): String? {
    return when (expression) {
        is ScalarConstant<*> -> unsupportedCustomPayload(expression.value, "$field.value")
        is ScalarReference<*>,
        is ScalarSymbolReference<*> -> null
        is ScalarCustom<*> -> unsupportedCustomPayload(expression.value, "$field.value")
        is ScalarUnary<*> -> unsupportedCustomPayload(expression.operand, "$field.operand")
        is ScalarBinary<*> -> {
            unsupportedCustomPayload(expression.left, "$field.left")
                ?: unsupportedCustomPayload(expression.right, "$field.right")
        }
        is ScalarFunction<*> -> expression.arguments.mapIndexed { index, argument ->
            unsupportedCustomPayload(argument, "$field.arguments[$index]")
        }.firstOrNull { it != null }
        is ScalarConditional<*> -> {
            unsupportedCustomPayload(expression.condition, "$field.condition")
                ?: unsupportedCustomPayload(expression.thenBranch, "$field.thenBranch")
                ?: unsupportedCustomPayload(expression.elseBranch, "$field.elseBranch")
        }
        is ScalarBoolean<*> -> unsupportedCustomPayload(expression.expr, "$field.expr")
    }
}

private fun unsupportedCustomPayload(value: Any?, field: String): String? {
    return when {
        value is UnsupportedCustomPayload -> field
        value is FrozenArray -> value.elements.mapIndexed { index, item ->
            unsupportedCustomPayload(item, "$field[$index]")
        }.firstOrNull { it != null }
        value == null || isKnownImmutableScalar(value) -> null
        value is Map<*, *> -> value.entries.mapIndexed { index, entry ->
            unsupportedCustomPayload(entry.key, "$field.keys[$index]")
                ?: unsupportedCustomPayload(entry.value, "$field.values[$index]")
        }.firstOrNull { it != null }
        value is Set<*> -> value.toList().mapIndexed { index, item ->
            unsupportedCustomPayload(item, "$field[$index]")
        }.firstOrNull { it != null }
        value is Collection<*> -> value.mapIndexed { index, item ->
            unsupportedCustomPayload(item, "$field[$index]")
        }.firstOrNull { it != null }
        value is Array<*> -> value.mapIndexed { index, item ->
            unsupportedCustomPayload(item, "$field[$index]")
        }.firstOrNull { it != null }
        value is BooleanArray ||
            value is ByteArray ||
            value is CharArray ||
            value is DoubleArray ||
            value is FloatArray ||
            value is IntArray ||
            value is LongArray ||
            value is ShortArray -> null
        else -> field
    }
}

/**
 * 关系查询计划校验错误 / Relational query plan validation error
 *
 * @property field 发生错误的计划字段 / Plan field containing the error
 * @property reason 校验失败原因 / Reason for validation failure
 */
data class RelationalQueryValidationError(
    val field: String,
    val reason: String
)
