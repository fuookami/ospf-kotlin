/**
 * 关系查询编译器契约 / Relational query compiler contract
 *
 * framework 只定义计划到后端编译结果的边界，不依赖具体数据库驱动。
 * The framework defines only the plan-to-backend boundary and does not depend on a database driver.
 */
package fuookami.ospf.kotlin.framework.persistence.query

import java.util.Collections
import java.util.LinkedHashMap
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * 关系数据库方言 / Relational database dialect
 *
 * 具体后端负责把方言映射到数据库驱动或 SQL 生成器。
 * Concrete backends map the dialect to a database driver or SQL generator.
 */
enum class RelationalQueryDialect {
    /** MySQL 方言 / MySQL dialect */
    MySQL,

    /** PostgreSQL 方言 / PostgreSQL dialect */
    PostgreSQL,

    /** SQLite 方言 / SQLite dialect */
    SQLite,

    /** Oracle 方言 / Oracle dialect */
    Oracle
}

/**
 * 关系查询计划编译器 / Relational query plan compiler
 *
 * 编译器实现负责校验计划、解析适配器白名单并生成后端查询对象。所有失败都通过 [Ret] 返回，
 * 具体数据库驱动、连接和行映射不属于 framework 契约。
 * Compiler implementations validate plans, resolve adapter allowlists, and produce backend query objects.
 * All failures are returned through [Ret]; database drivers, connections, and row mapping are outside the
 * framework contract.
 *
 * @param T 后端编译结果类型 / Backend-specific compiled query type
 * @property dialect 编译器使用的数据库方言 / Database dialect used by the compiler
 */
interface RelationalQueryCompiler<T> {
    val dialect: RelationalQueryDialect

    /**
     * 编译关系查询计划 / Compile a relational query plan
     *
     * @param plan 待编译的关系查询计划 / Relational query plan to compile
     * @return 编译结果或结构化错误 / Compiled result or structured failure
     */
    fun compile(plan: RelationalQueryPlan): Ret<T>
}

/**
 * 关系查询来源映射 / Relational query source mapping
 *
 * [name] 是计划中的逻辑来源名，[table] 和 [columns] 是适配器维护的物理白名单。
 * [name] is the logical source name in a plan; [table] and [columns] are adapter-owned physical allowlists.
 *
 * @property name 逻辑来源名 / Logical source name
 * @property table 物理表名 / Physical table name
 * @property columns 逻辑字段路径到物理列名的映射 / Mapping from logical field paths to physical columns
 * @property defaultColumns 无显式投影时使用的字段 / Columns used when no projection is supplied
 */
data class RelationalQuerySource(
    val name: String,
    val table: String,
    val columns: Map<String, String> = emptyMap(),
    val defaultColumns: List<String> = columns.keys.toList()
) {
    /**
     * 通过查询来源创建映射 / Create a mapping from a query source
     *
     * @param source 查询来源 / Query source
     * @param table 物理表名 / Physical table name
     * @param columns 字段映射 / Field mapping
     * @param defaultColumns 默认投影字段 / Default projection fields
     */
    constructor(
        source: QuerySource,
        table: String,
        columns: Map<String, String> = emptyMap(),
        defaultColumns: List<String> = columns.keys.toList()
    ) : this(source.name, table, columns, defaultColumns)
}

/**
 * 关系查询来源注册表 / Relational query source registry
 *
 * 注册表构造后不可变，因此同一个编译器可以安全地并发使用。
 * The registry is immutable after construction, so one compiler can be used concurrently.
 *
 * @property sources 来源映射 / Source mappings
 */
class RelationalQuerySourceRegistry(
    definitions: Iterable<RelationalQuerySource>
) {
    val sources: List<RelationalQuerySource> = Collections.unmodifiableList(
        definitions.map { definition ->
            definition.copy(
                columns = Collections.unmodifiableMap(LinkedHashMap(definition.columns)),
                defaultColumns = Collections.unmodifiableList(definition.defaultColumns.toList())
            )
        }.toList()
    )

    /**
     * 通过名称映射创建注册表 / Create a registry from a name-keyed map
     *
     * @param definitions 名称到来源映射 / Name-to-source mappings
     */
    constructor(definitions: Map<String, RelationalQuerySource>) : this(
        definitions.map { (name, definition) ->
            if (definition.name == name) definition else definition.copy(name = name)
        }
    )

    /**
     * 通过来源名查找映射 / Find a mapping by source name
     *
     * @param name 来源名 / Source name
     * @return 映射；不存在时为 null / Mapping, or null when absent
     */
    operator fun get(name: String): RelationalQuerySource? {
        return sources.firstOrNull { it.name == name }
    }

    companion object {
        /** 空注册表 / Empty registry */
        val empty: RelationalQuerySourceRegistry = RelationalQuerySourceRegistry(emptyList())
    }
}

/**
 * 已编译的关系查询 / Compiled relational query
 *
 * SQL 中的标量值全部由 [parameters] 绑定，因而 [sql] 不包含业务字面量。
 * Scalar values in the SQL are all bound through [parameters], so [sql] contains no business literals.
 *
 * @property sql 参数化 SQL / Parameterized SQL
 * @property parameters SQL 参数 / SQL parameters
 * @property canonicalHash 查询计划规范化哈希 / Canonical query-plan hash
 */
data class CompiledRelationalQuery(
    val sql: String,
    val parameters: List<Any?>,
    val canonicalHash: String
) {
    /** 计划哈希别名 / Plan-hash alias */
    val planHash: String get() = canonicalHash
}

/**
 * 关系查询编译错误类别 / Relational query compilation error category
 */
enum class RelationalQueryCompilationErrorCategory {
    /** 来源不存在 / Unknown source */
    UnknownSource,

    /** 字段不存在或有歧义 / Unknown or ambiguous column */
    UnknownColumn,

    /** 标识符不安全 / Unsafe identifier */
    InvalidIdentifier,

    /** Join 结构非法 / Invalid join */
    InvalidJoin,

    /** 表达式不受支持 / Unsupported expression */
    UnsupportedExpression,

    /** 分页或计划结构非法 / Invalid plan structure */
    InvalidPlan,

    /** 方言不支持计划语义 / Dialect does not support the plan semantics */
    UnsupportedDialect
}

/**
 * 关系查询结构化编译错误 / Structured relational query compilation error
 *
 * @property category 错误类别 / Error category
 * @property field 发生错误的计划字段 / Plan field containing the error
 * @property reason 错误原因 / Error reason
 */
data class RelationalQueryCompilationError(
    val category: RelationalQueryCompilationErrorCategory,
    val field: String,
    val reason: String
)

/**
 * 纯 Kotlin 参数化 SQL 编译器 / Pure Kotlin parameterized SQL compiler
 *
 * 编译器不连接数据库，也不依赖 Ktorm 或 JDBC。未提供来源注册表时，来源名和字段路径
 * 作为逻辑名直接映射到同名物理对象；提供注册表后仅允许白名单中的来源和字段。
 * The compiler opens no database connection and depends on neither Ktorm nor JDBC. Without a source registry,
 * source names and field paths map to same-named physical objects; with a registry, only allowlisted objects are accepted.
 *
 * @property dialect SQL 方言 / SQL dialect
 * @property sourceRegistry 来源白名单 / Source allowlist
 */
class SqlRelationalQueryCompiler(
    override val dialect: RelationalQueryDialect,
    private val sourceRegistry: RelationalQuerySourceRegistry = RelationalQuerySourceRegistry.empty
) : RelationalQueryCompiler<CompiledRelationalQuery> {

    private data class BoundSource(
        val definition: RelationalQuerySource,
        val source: QuerySource,
        val qualifier: String
    )

    private class SqlState {
        val parameters = mutableListOf<Any?>()
    }

    /**
     * 使用来源列表创建编译器 / Create a compiler from source mappings
     *
     * @param dialect SQL 方言 / SQL dialect
     * @param sources 来源映射 / Source mappings
     */
    constructor(
        dialect: RelationalQueryDialect,
        sources: Iterable<RelationalQuerySource>
    ) : this(dialect, RelationalQuerySourceRegistry(sources))

    /**
     * 使用来源映射创建编译器 / Create a compiler from a source map
     *
     * @param dialect SQL 方言 / SQL dialect
     * @param sources 来源映射 / Source mappings
     */
    constructor(
        dialect: RelationalQueryDialect,
        sources: Map<String, RelationalQuerySource>
    ) : this(
        dialect,
        RelationalQuerySourceRegistry(
            sources.map { (name, definition) ->
                if (definition.name == name) definition else definition.copy(name = name)
            }
        )
    )

    /**
     * 编译查询计划 / Compile a query plan
     *
     * @param plan 待编译计划 / Query plan to compile
     * @return 编译查询或结构化错误 / Compiled query or structured failure
     */
    override fun compile(plan: RelationalQueryPlan): Ret<CompiledRelationalQuery> {
        val validation = plan.validate()
        if (validation.failed) {
            val detail = (validation as? Failed<*, *, *>)?.error?.value as? RelationalQueryValidationError
            return failure(
                category = RelationalQueryCompilationErrorCategory.InvalidPlan,
                field = detail?.field ?: "plan",
                reason = detail?.reason ?: "Query plan validation failed / 查询计划校验失败"
            )
        }
        val joinParameters = mutableListOf<Any?>()
        val existsParameters = mutableListOf<Any?>()
        val state = SqlState()
        val bound = linkedMapOf<String, BoundSource>()
        val rootDefinition = definition(plan.root.name)
            ?: return failure(
                category = RelationalQueryCompilationErrorCategory.UnknownSource,
                field = "root",
                reason = "Unknown root query source: ${plan.root.name} / 未知根查询来源：${plan.root.name}"
            )
        val root = bind(rootDefinition, plan.root, "root")
        if (root.failed) return propagate(root)
        val rootBound = root.value ?: return failure(
            category = RelationalQueryCompilationErrorCategory.InvalidPlan,
            field = "root",
            reason = "Root source could not be bound / 根来源无法绑定"
        )
        addBound(bound, rootBound)

        val joinSql = mutableListOf<String>()
        val existsSql = mutableListOf<String>()
        for ((index, join) in plan.joins.withIndex()) {
            val definition = definition(join.source.name)
                ?: return failure(
                    category = RelationalQueryCompilationErrorCategory.UnknownSource,
                    field = "joins[$index].source",
                    reason = "Unknown join query source: ${join.source.name} / 未知 Join 来源：${join.source.name}"
                )
            val targetResult = bind(definition, join.source, "joins[$index].source")
            if (targetResult.failed) return propagate(targetResult)
            val target = targetResult.value ?: return failure(
                category = RelationalQueryCompilationErrorCategory.InvalidJoin,
                field = "joins[$index].source",
                reason = "Join source could not be bound / Join 来源无法绑定"
            )
            val conditionBound = LinkedHashMap(bound)
            addBound(conditionBound, target)
            val conditionState = SqlState()
            val conditionResult = compileBoolean(
                expression = join.condition,
                bound = conditionBound,
                state = conditionState,
                field = "joins[$index].condition"
            )
            if (conditionResult.failed) return propagate(conditionResult)
            val condition = conditionResult.value ?: return failure(
                category = RelationalQueryCompilationErrorCategory.InvalidJoin,
                field = "joins[$index].condition",
                reason = "Join condition could not be compiled / Join 条件无法编译"
            )
            val table = quoteIdentifier(definition.table, "joins[$index].source.table")
            if (table.failed) return propagate(table)
            val tableSql = table.value ?: return failure(
                category = RelationalQueryCompilationErrorCategory.InvalidIdentifier,
                field = "joins[$index].source.table",
                reason = "Join table name is empty / Join 表名为空"
            )
            val sourceSql = "$tableSql AS ${quoteIdentifierOrFallback(target.qualifier)}"
            when (join.type) {
                JoinType.Inner -> {
                    joinSql += "INNER JOIN $sourceSql ON $condition"
                    joinParameters.addAll(conditionState.parameters)
                    addBound(bound, target)
                }
                JoinType.Left -> {
                    joinSql += "LEFT JOIN $sourceSql ON $condition"
                    joinParameters.addAll(conditionState.parameters)
                    addBound(bound, target)
                }
                JoinType.Exists -> {
                    existsSql += "EXISTS (SELECT 1 FROM $sourceSql WHERE $condition)"
                    existsParameters.addAll(conditionState.parameters)
                }
            }
        }

        val predicateState = SqlState()
        val predicate = plan.predicate?.let {
            val result = compileBoolean(it, bound, predicateState, "predicate")
            if (result.failed) return propagate(result)
            result.value
        }

        val projectionRefs = if (plan.projections.isEmpty()) {
            if (rootBound.definition.defaultColumns.isEmpty()) {
                return failure(
                    category = RelationalQueryCompilationErrorCategory.InvalidPlan,
                    field = "projections",
                    reason = "An explicit projection or registered default column is required / 必须提供显式投影或注册默认字段"
                )
            }
            rootBound.definition.defaultColumns.map { ProjectionSpec(ColumnRef(plan.root.name, it)) }
        } else {
            plan.projections
        }
        val projectionSql = mutableListOf<String>()
        for ((index, projection) in projectionRefs.withIndex()) {
            val result = compileColumn(projection.column, bound, state, "projections[$index]")
            if (result.failed) return propagate(result)
            val column = result.value ?: return failure(
                category = RelationalQueryCompilationErrorCategory.UnknownColumn,
                field = "projections[$index]",
                reason = "Projection column could not be resolved / 投影字段无法解析"
            )
            val alias = projection.alias?.let {
                val aliasResult = quoteIdentifier(it, "projections[$index].alias")
                if (aliasResult.failed) return propagate(aliasResult)
                aliasResult.value
            }
            projectionSql += if (alias == null) column else "$column AS $alias"
        }

        val table = quoteIdentifier(rootBound.definition.table, "root.table")
        if (table.failed) return propagate(table)
        val tableSql = table.value ?: return failure(
            category = RelationalQueryCompilationErrorCategory.InvalidIdentifier,
            field = "root.table",
            reason = "Root table name is empty / 根表名为空"
        )
        val groupSql = mutableListOf<String>()
        for ((index, column) in plan.groupBy.withIndex()) {
            val result = compileColumn(column, bound, state, "groupBy[$index]")
            if (result.failed) return propagate(result)
            groupSql += result.value ?: return failure(
                category = RelationalQueryCompilationErrorCategory.UnknownColumn,
                field = "groupBy[$index]",
                reason = "Group-by column could not be resolved / 分组字段无法解析"
            )
        }
        val orderSql = mutableListOf<String>()
        for ((index, order) in plan.orderBy.withIndex()) {
            val result = compileColumn(order.column, bound, state, "orderBy[$index]")
            if (result.failed) return propagate(result)
            val column = result.value ?: return failure(
                category = RelationalQueryCompilationErrorCategory.UnknownColumn,
                field = "orderBy[$index]",
                reason = "Order-by column could not be resolved / 排序字段无法解析"
            )
            orderSql += orderSql(column, order)
        }
        val pagination = plan.page?.let(::paginationSql)
        if (pagination?.failed == true) return propagate(pagination)
        val sql = buildString {
            append("SELECT ")
            if (plan.distinct) append("DISTINCT ")
            append(projectionSql.joinToString(", "))
            append(" FROM ")
            append(tableSql)
            append(" AS ")
            append(quoteIdentifierOrFallback(rootBound.qualifier))
            if (joinSql.isNotEmpty()) {
                append(" ")
                append(joinSql.joinToString(" "))
            }
            val whereParts = listOfNotNull(predicate) + existsSql
            if (whereParts.isNotEmpty()) {
                append(" WHERE ")
                append(whereParts.joinToString(" AND ") { "($it)" })
            }
            if (groupSql.isNotEmpty()) {
                append(" GROUP BY ")
                append(groupSql.joinToString(", "))
            }
            if (orderSql.isNotEmpty()) {
                append(" ORDER BY ")
                append(orderSql.joinToString(", "))
            }
            pagination?.value?.let { value -> append(value) }
        }
        return Ok(
            CompiledRelationalQuery(
                sql = sql,
                parameters = Collections.unmodifiableList(
                    (joinParameters + predicateState.parameters + existsParameters).toList()
                ),
                canonicalHash = plan.hash()
            )
        )
    }

    private fun paginationSql(page: PageSpec): Ret<String> {
        val limit = page.limit
        if (limit == null) {
            return when (dialect) {
                RelationalQueryDialect.MySQL -> failure(
                    category = RelationalQueryCompilationErrorCategory.UnsupportedDialect,
                    field = "page.limit",
                    reason = "MySQL requires a finite LIMIT for offset-only pagination / " +
                        "MySQL 的仅偏移分页必须提供有限 LIMIT"
                )
                RelationalQueryDialect.PostgreSQL -> Ok(" OFFSET ${page.offset}")
                RelationalQueryDialect.SQLite -> Ok(" LIMIT -1 OFFSET ${page.offset}")
                RelationalQueryDialect.Oracle -> Ok(" OFFSET ${page.offset} ROWS")
            }
        }
        return Ok(
            when (dialect) {
                RelationalQueryDialect.MySQL -> " LIMIT ${page.offset}, $limit"
                RelationalQueryDialect.PostgreSQL -> " LIMIT $limit OFFSET ${page.offset}"
                RelationalQueryDialect.SQLite -> " LIMIT $limit OFFSET ${page.offset}"
                RelationalQueryDialect.Oracle -> " OFFSET ${page.offset} ROWS FETCH NEXT $limit ROWS ONLY"
            }
        )
    }

    private fun definition(name: String): RelationalQuerySource? {
        return sourceRegistry[name] ?: if (sourceRegistry.sources.isEmpty()) {
            RelationalQuerySource(name = name, table = name)
        } else {
            null
        }
    }

    private fun bind(
        definition: RelationalQuerySource,
        source: QuerySource,
        field: String
    ): Ret<BoundSource> {
        val table = quoteIdentifier(definition.table, "$field.table")
        if (table.failed) return propagate(table)
        val qualifier = source.alias ?: source.name
        val qualifierResult = quoteIdentifier(qualifier, "$field.alias")
        if (qualifierResult.failed) return propagate(qualifierResult)
        return Ok(BoundSource(definition, source, qualifier))
    }

    private fun addBound(bound: MutableMap<String, BoundSource>, source: BoundSource) {
        bound[source.source.name] = source
        source.source.alias?.let { bound[it] = source }
    }

    private fun compileColumn(
        ref: ColumnRef,
        bound: Map<String, BoundSource>,
        state: SqlState,
        field: String
    ): Ret<String> {
        val source = bound[ref.source]
            ?: return failure(
                category = RelationalQueryCompilationErrorCategory.UnknownSource,
                field = field,
                reason = "Unknown column source: ${ref.source} / 未知字段来源：${ref.source}"
            )
        return column(source, ref.path, state, field)
    }

    private fun column(
        source: BoundSource,
        path: String,
        state: SqlState,
        field: String
    ): Ret<String> {
        val logicalPath = path.trim()
        val physicalPath = if (source.definition.columns.isEmpty()) {
            logicalPath
        } else {
            source.definition.columns[logicalPath]
                ?: return failure(
                    category = RelationalQueryCompilationErrorCategory.UnknownColumn,
                    field = field,
                    reason = "Unknown query column: ${source.source.name}.$logicalPath / 未知查询字段：${source.source.name}.$logicalPath"
                )
        }
        val quoted = quoteIdentifier(physicalPath, field)
        if (quoted.failed) return propagate(quoted)
        val value = quoted.value ?: return failure(
            category = RelationalQueryCompilationErrorCategory.InvalidIdentifier,
            field = field,
            reason = "Column name is empty / 字段名为空"
        )
        return Ok("${quoteIdentifierOrFallback(source.qualifier)}.$value")
    }

    private fun compileBoolean(
        expression: BooleanExpression,
        bound: Map<String, BoundSource>,
        state: SqlState,
        field: String
    ): Ret<String> {
        return when (expression) {
            is BooleanConstant -> Ok(booleanSql(expression.value))
            is Comparison<*> -> {
                val leftConstant = expression.left as? ScalarConstant<*>
                val rightConstant = expression.right as? ScalarConstant<*>
                if (rightConstant != null &&
                    rightConstant.value == null &&
                    expression.operator in setOf(ComparisonOperator.Eq, ComparisonOperator.Ne)
                ) {
                    val left = compileScalar(expression.left, bound, state, "$field.left")
                    if (left.failed) return propagate(left)
                    Ok("${left.value} ${if (expression.operator == ComparisonOperator.Eq) "IS" else "IS NOT"} NULL")
                } else if (leftConstant != null &&
                    leftConstant.value == null &&
                    expression.operator in setOf(ComparisonOperator.Eq, ComparisonOperator.Ne)
                ) {
                    val right = compileScalar(expression.right, bound, state, "$field.right")
                    if (right.failed) return propagate(right)
                    Ok("${right.value} ${if (expression.operator == ComparisonOperator.Eq) "IS" else "IS NOT"} NULL")
                } else {
                    val left = compileScalar(expression.left, bound, state, "$field.left")
                    if (left.failed) return propagate(left)
                    val right = compileScalar(expression.right, bound, state, "$field.right")
                    if (right.failed) return propagate(right)
                    val operator = when (expression.operator) {
                        ComparisonOperator.Eq -> "="
                        ComparisonOperator.Ne -> "<>"
                        ComparisonOperator.Lt -> "<"
                        ComparisonOperator.Le -> "<="
                        ComparisonOperator.Gt -> ">"
                        ComparisonOperator.Ge -> ">="
                    }
                    Ok("${left.value} $operator ${right.value}")
                }
            }
            is InExpression<*> -> {
                if (expression.candidates.isEmpty()) {
                    Ok(booleanSql(if (expression.negated) Trivalent.True else Trivalent.False))
                } else {
                    val value = compileScalar(expression.value, bound, state, "$field.value")
                    if (value.failed) return propagate(value)
                    val candidates = mutableListOf<String>()
                    for ((index, candidate) in expression.candidates.withIndex()) {
                        val result = compileScalar(candidate, bound, state, "$field.candidates[$index]")
                        if (result.failed) return propagate(result)
                        candidates += result.value ?: return failure(
                            category = RelationalQueryCompilationErrorCategory.UnsupportedExpression,
                            field = "$field.candidates[$index]",
                            reason = "IN candidate could not be compiled / IN 候选值无法编译"
                        )
                    }
                    Ok("${value.value} ${if (expression.negated) "NOT IN" else "IN"} (${candidates.joinToString(", ")})")
                }
            }
            is PatternMatch<*> -> compilePattern(expression, bound, state, field)
            is NullCheck -> {
                val result = compileReference(expression.path.value, bound, state, field)
                if (result.failed) return propagate(result)
                Ok("${result.value} ${if (expression.type == NullCheckType.IsNull) "IS NULL" else "IS NOT NULL"}")
            }
            is AndExpression -> compileLogical(expression.operands, "AND", bound, state, field)
            is OrExpression -> compileLogical(expression.operands, "OR", bound, state, field)
            is NotExpression -> {
                val operand = compileBoolean(expression.operand, bound, state, "$field.operand")
                if (operand.failed) return propagate(operand)
                Ok("NOT (${operand.value})")
            }
            is BooleanCustom -> failure(
                category = RelationalQueryCompilationErrorCategory.UnsupportedExpression,
                field = field,
                reason = "Custom boolean expressions are not supported / 不支持自定义布尔表达式"
            )
        }
    }

    private fun compileLogical(
        operands: List<BooleanExpression>,
        operator: String,
        bound: Map<String, BoundSource>,
        state: SqlState,
        field: String
    ): Ret<String> {
        val values = mutableListOf<String>()
        for ((index, operand) in operands.withIndex()) {
            val result = compileBoolean(operand, bound, state, "$field.operands[$index]")
            if (result.failed) return propagate(result)
            values += result.value ?: return failure(
                category = RelationalQueryCompilationErrorCategory.UnsupportedExpression,
                field = "$field.operands[$index]",
                reason = "Logical operand could not be compiled / 逻辑操作数无法编译"
            )
        }
        return Ok(values.joinToString(" $operator ") { "($it)" })
    }

    private fun compilePattern(
        expression: PatternMatch<*>,
        bound: Map<String, BoundSource>,
        state: SqlState,
        field: String
    ): Ret<String> {
        val value = compileScalar(expression.value, bound, state, "$field.value")
        if (value.failed) return propagate(value)
        if (expression.mode == PatternMatchMode.Regex) {
            val pattern = compileScalar(expression.pattern, bound, state, "$field.pattern")
            if (pattern.failed) return propagate(pattern)
            return when (dialect) {
                RelationalQueryDialect.Oracle -> Ok(
                    "${if (expression.negated) "NOT " else ""}REGEXP_LIKE(${value.value}, ${pattern.value})"
                )
                RelationalQueryDialect.MySQL -> Ok("${value.value} ${if (expression.negated) "NOT " else ""}REGEXP ${pattern.value}")
                RelationalQueryDialect.PostgreSQL -> Ok("${value.value} ${if (expression.negated) "!~" else "~"} ${pattern.value}")
                RelationalQueryDialect.SQLite -> failure(
                    category = RelationalQueryCompilationErrorCategory.UnsupportedExpression,
                    field = field,
                    reason = "SQLite regex requires an application-registered function / SQLite 正则需要应用注册函数"
                )
            }
        }
        val patternConstant = expression.pattern as? ScalarConstant<*>
        val patternText = patternConstant?.value as? String
        val pattern = if (patternText != null && expression.mode != PatternMatchMode.Exact && expression.mode != PatternMatchMode.Like) {
            val transformed = when (expression.mode) {
                PatternMatchMode.Prefix -> "$patternText%"
                PatternMatchMode.Suffix -> "%$patternText"
                PatternMatchMode.Contains -> "%$patternText%"
                else -> patternText
            }
            state.parameters += transformed
            "?"
        } else {
            val result = compileScalar(expression.pattern, bound, state, "$field.pattern")
            if (result.failed) return propagate(result)
            result.value
        }
        val operator = if (expression.mode == PatternMatchMode.Exact) {
            if (expression.negated) "<>" else "="
        } else {
            if (expression.negated) "NOT LIKE" else "LIKE"
        }
        return Ok("${value.value} $operator $pattern")
    }

    private fun compileScalar(
        expression: ScalarExpression<*>,
        bound: Map<String, BoundSource>,
        state: SqlState,
        field: String
    ): Ret<String> {
        return when (expression) {
            is ScalarConstant<*> -> {
                state.parameters += expression.value
                Ok("?")
            }
            is ScalarReference<*> -> compileReference(expression.path.value, bound, state, field)
            is ScalarSymbolReference<*> -> compileReference(expression.symbol.name, bound, state, field)
            is ScalarCustom<*> -> failure(
                category = RelationalQueryCompilationErrorCategory.UnsupportedExpression,
                field = field,
                reason = "Custom scalar expressions are not supported / 不支持自定义标量表达式"
            )
            is ScalarUnary<*> -> {
                val operand = compileScalar(expression.operand, bound, state, "$field.operand")
                if (operand.failed) return propagate(operand)
                when (expression.operator) {
                    UnaryOperator.Negate -> Ok("(-${operand.value})")
                    UnaryOperator.Positive -> Ok("(+${operand.value})")
                    UnaryOperator.Abs -> Ok("ABS(${operand.value})")
                }
            }
            is ScalarBinary<*> -> {
                val left = compileScalar(expression.left, bound, state, "$field.left")
                if (left.failed) return propagate(left)
                val right = compileScalar(expression.right, bound, state, "$field.right")
                if (right.failed) return propagate(right)
                when (expression.operator) {
                    BinaryOperator.Add -> Ok("(${left.value} + ${right.value})")
                    BinaryOperator.Subtract -> Ok("(${left.value} - ${right.value})")
                    BinaryOperator.Multiply -> Ok("(${left.value} * ${right.value})")
                    BinaryOperator.Divide -> Ok("(${left.value} / ${right.value})")
                    BinaryOperator.Modulo -> if (dialect == RelationalQueryDialect.Oracle) {
                        Ok("MOD(${left.value}, ${right.value})")
                    } else {
                        Ok("(${left.value} % ${right.value})")
                    }
                    BinaryOperator.Power -> Ok("POWER(${left.value}, ${right.value})")
                }
            }
            is ScalarFunction<*> -> {
                val name = expression.name.lowercase()
                val supported = name in setOf("abs", "lower", "upper", "trim", "length", "coalesce")
                if (!supported) {
                    return failure(
                        category = RelationalQueryCompilationErrorCategory.UnsupportedExpression,
                        field = field,
                        reason = "Unsupported scalar function: ${expression.name} / 不支持的标量函数：${expression.name}"
                    )
                }
                val arguments = mutableListOf<String>()
                for ((index, argument) in expression.arguments.withIndex()) {
                    val result = compileScalar(argument, bound, state, "$field.arguments[$index]")
                    if (result.failed) return propagate(result)
                    arguments += result.value ?: return failure(
                        category = RelationalQueryCompilationErrorCategory.UnsupportedExpression,
                        field = "$field.arguments[$index]",
                        reason = "Function argument could not be compiled / 函数参数无法编译"
                    )
                }
                val validArity = when (name) {
                    "coalesce" -> arguments.isNotEmpty()
                    else -> arguments.size == 1
                }
                if (!validArity) {
                    return failure(
                        category = RelationalQueryCompilationErrorCategory.UnsupportedExpression,
                        field = field,
                        reason = "Invalid argument count for function ${expression.name} / 函数 ${expression.name} 的参数数量非法"
                    )
                }
                Ok("${name.uppercase()}(${arguments.joinToString(", ")})")
            }
            is ScalarConditional<*> -> {
                val condition = compileBoolean(expression.condition, bound, state, "$field.condition")
                if (condition.failed) return propagate(condition)
                val thenBranch = compileScalar(expression.thenBranch, bound, state, "$field.thenBranch")
                if (thenBranch.failed) return propagate(thenBranch)
                val elseBranch = compileScalar(expression.elseBranch, bound, state, "$field.elseBranch")
                if (elseBranch.failed) return propagate(elseBranch)
                Ok("CASE WHEN ${condition.value} THEN ${thenBranch.value} ELSE ${elseBranch.value} END")
            }
            is ScalarBoolean<*> -> {
                val result = compileBoolean(expression.expr, bound, state, "$field.expr")
                if (result.failed) return propagate(result)
                if (dialect == RelationalQueryDialect.Oracle) {
                    Ok("CASE WHEN ${result.value} THEN 1 ELSE 0 END")
                } else {
                    Ok("(${result.value})")
                }
            }
        }
    }

    private fun compileReference(
        path: String,
        bound: Map<String, BoundSource>,
        state: SqlState,
        field: String
    ): Ret<String> {
        val normalized = path.trim()
        val parts = normalized.split('.', limit = 2)
        val source = if (parts.size == 2 && bound.containsKey(parts[0])) {
            bound[parts[0]] to parts[1]
        } else {
            val candidates = bound.values.distinct()
            if (candidates.size != 1) {
                return failure(
                    category = RelationalQueryCompilationErrorCategory.UnknownColumn,
                    field = field,
                    reason = "Unqualified column is ambiguous: $normalized / 未限定字段有歧义：$normalized"
                )
            }
            candidates.single() to normalized
        }
        return column(source.first ?: return failure(
            category = RelationalQueryCompilationErrorCategory.UnknownSource,
            field = field,
            reason = "Unknown reference source / 未知引用来源"
        ), source.second, state, field)
    }

    private fun orderSql(column: String, order: OrderSpec): String {
        val direction = if (order.direction == SortDirection.Ascending) "ASC" else "DESC"
        return if (order.nulls == NullsOrder.Unspecified) {
            "$column $direction"
        } else if (dialect == RelationalQueryDialect.MySQL || dialect == RelationalQueryDialect.SQLite) {
            val nullRank = if (order.nulls == NullsOrder.First) "0" else "1"
            val nonNullRank = if (order.nulls == NullsOrder.First) "1" else "0"
            "CASE WHEN $column IS NULL THEN $nullRank ELSE $nonNullRank END ASC, $column $direction"
        } else {
            "$column $direction NULLS ${if (order.nulls == NullsOrder.First) "FIRST" else "LAST"}"
        }
    }

    private fun booleanSql(value: Trivalent): String {
        return when (dialect) {
            RelationalQueryDialect.Oracle -> when (value) {
                Trivalent.True -> "1 = 1"
                Trivalent.False -> "1 = 0"
                Trivalent.Unknown -> "NULL = NULL"
            }
            RelationalQueryDialect.MySQL,
            RelationalQueryDialect.PostgreSQL,
            RelationalQueryDialect.SQLite -> when (value) {
                Trivalent.True -> "TRUE"
                Trivalent.False -> "FALSE"
                Trivalent.Unknown -> "NULL"
            }
        }
    }

    private fun quoteIdentifier(identifier: String, field: String): Ret<String> {
        val value = identifier.trim()
        if (value.isEmpty() || value.any { it == '\u0000' }) {
            return failure(
                category = RelationalQueryCompilationErrorCategory.InvalidIdentifier,
                field = field,
                reason = "Identifier must not be blank or contain NUL / 标识符不能为空或包含 NUL"
            )
        }
        val parts = value.split('.')
        if (parts.any { it.isBlank() }) {
            return failure(
                category = RelationalQueryCompilationErrorCategory.InvalidIdentifier,
                field = field,
                reason = "Identifier contains an empty segment / 标识符包含空分段"
            )
        }
        val quote = if (dialect == RelationalQueryDialect.MySQL) '`' else '"'
        return Ok(parts.joinToString(".") { part ->
            val escaped = part.replace(quote.toString(), "$quote$quote")
            "$quote$escaped$quote"
        })
    }

    private fun quoteIdentifierOrFallback(identifier: String): String {
        val quote = if (dialect == RelationalQueryDialect.MySQL) '`' else '"'
        val escaped = identifier.replace(quote.toString(), "$quote$quote")
        return "$quote$escaped$quote"
    }

    private fun <T> failure(
        category: RelationalQueryCompilationErrorCategory,
        field: String,
        reason: String
    ): Failed<T, ErrorCode, Error<ErrorCode>> {
        return Failed(ErrorCode.IllegalArgument, reason, RelationalQueryCompilationError(category, field, reason))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> propagate(result: Ret<*>): Ret<T> {
        return when (result) {
            is Ok<*, *, *> -> failure(
                category = RelationalQueryCompilationErrorCategory.InvalidPlan,
                field = "compiler",
                reason = "Unexpected successful compiler result / 编译器出现意外成功结果"
            )
            is Failed<*, *, *> -> Failed<T, ErrorCode, Error<ErrorCode>>(result.error as Error<ErrorCode>)
            is Fatal<*, *, *> -> Fatal<T, ErrorCode, Error<ErrorCode>>(
                result.errors as List<Error<ErrorCode>>
            )
        }
    }
}

/** 兼容性别名 / Compatibility alias */
typealias PureRelationalQueryCompiler = SqlRelationalQueryCompiler
