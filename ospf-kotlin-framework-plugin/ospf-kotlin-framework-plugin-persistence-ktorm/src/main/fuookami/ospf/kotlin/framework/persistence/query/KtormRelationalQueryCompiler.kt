/**
 * Ktorm 关系查询计划编译器 / Ktorm relational query plan compiler
 *
 * 将通用关系计划编译为 Ktorm 查询对象，不接受 SQL 字符串或用户提供的物理标识。
 * Compiles a generic relational plan into a Ktorm query without accepting SQL strings or user-provided physical identifiers.
 */
package fuookami.ospf.kotlin.framework.persistence.query

import java.util.Collections
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.dsl.QuerySource as KtormQuerySourceExpression
import org.ktorm.expression.OrderByExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.translator.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Ktorm 数据源注册 / Ktorm query source registration
 *
 * @property source 通用数据源描述 / Generic source descriptor
 * @property table 固定的 Ktorm 表 / Fixed Ktorm table
 * @property resolveColumnDetailed 数据源内字段解析器 / Field resolver within the source
 * @property defaultColumns 无显式投影时允许返回的注册字段路径 / Registered field paths allowed for implicit projections
 */
class KtormQuerySource(
    val source: QuerySource,
    val table: BaseTable<*>,
    val resolveColumnDetailed: DiagnosticPersistenceFieldResolver<ColumnDeclaring<*>>,
    defaultColumns: List<String> = emptyList()
) {
    val defaultColumns: List<String> = Collections.unmodifiableList(defaultColumns.map(String::trim))
}

/** Ktorm 关系计划编译结果 / Compiled Ktorm relational query */
data class KtormCompiledQuery(
    val query: Query,
    val plan: RelationalQueryPlan,
    val rootColumns: List<ColumnDeclaring<*>>,
    val audit: QueryAuditSummary
)

/** 可审计的查询摘要 / Auditable query summary */
data class QueryAuditSummary(
    val sqlTemplate: String,
    val parameterTypes: List<String>,
    val dialect: String
)

/** 查询执行错误分类 / Query execution error category */
enum class QueryExecutionErrorCategory {
    SqlGeneration,
    UnknownSource,
    UnknownColumn,
    InvalidJoin,
    ParameterBinding,
    UnsupportedDialect,
    Timeout,
    Database
}

/** 关系查询结构化失败 / Structured relational query failure */
data class RelationalQueryFailure(
    val category: QueryExecutionErrorCategory,
    val field: String? = null,
    val reason: String
)

/** 查询执行统计 / Query execution statistics */
data class QueryExecutionStats(
    val durationMillis: Long,
    val returnedRows: Long,
    val scannedRows: Long? = null,
    val scannedRowsExact: Boolean = false,
    val truncated: Boolean = false
)

/** 带统计信息的查询结果 / Query result with execution statistics */
data class QueryExecutionResult<T>(
    val value: T,
    val stats: QueryExecutionStats
)

/**
 * Ktorm 关系计划编译器 / Ktorm relational query plan compiler
 *
 * @property database Ktorm 数据库 / Ktorm database
 * @property sources 由适配器维护的数据源白名单 / Adapter-owned source allowlist
 * @property unsupportedPredicatePolicy 不支持谓词策略 / Unsupported predicate policy
 * @property targetConstantBinder 目标 SQL 类型感知的常量绑定器 / Target-SQL-type-aware constant binder
 */
class KtormRelationalQueryCompiler(
    private val database: Database,
    private val sources: Map<String, KtormQuerySource>,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast,
    private val targetConstantBinder: KtormTargetConstantBinder? = null
) {

    private data class BoundSource(
        val definition: KtormQuerySource,
        val source: QuerySource,
        val table: BaseTable<*>,
        val columns: List<ColumnDeclaring<*>>
    )

    private data class BuildResult(
        val from: KtormQuerySourceExpression,
        val root: BoundSource,
        val allSources: Map<String, BoundSource>,
        val extraPredicates: List<ColumnDeclaring<Boolean>>
    )

    /**
     * 编译关系查询计划 / Compile a relational query plan
     *
     * @param plan 通用关系查询计划 / Generic relational query plan
     * @return 编译结果或结构化错误 / Compiled query or structured error
     */
    fun compile(plan: RelationalQueryPlan): Ret<KtormCompiledQuery> {
        val validation = plan.validate()
        if (validation.failed) {
            return validationFailure(validation)
        }

        val parameterTypes = mutableListOf<String>()
        val builtResult = buildSources(plan, parameterTypes::add)
        if (builtResult.failed) {
            @Suppress("UNCHECKED_CAST")
            val failed = builtResult as Failed<BuildResult, ErrorCode, Error<ErrorCode>>
            return Failed(failed.error)
        }
        val built = builtResult.value ?: return failure(
            category = QueryExecutionErrorCategory.SqlGeneration,
            field = "sources",
            reason = "Relational query sources were not built"
        )
        val translatedPredicate = plan.predicate?.let { expression ->
            val translated = translatePredicate(
                expression = expression,
                bound = built.allSources,
                parameterTypeSink = parameterTypes::add,
                fallbackCategory = QueryExecutionErrorCategory.SqlGeneration,
                field = "predicate"
            )
            if (translated.failed) {
                @Suppress("UNCHECKED_CAST")
                val failed = translated as Failed<ColumnDeclaring<Boolean>?, ErrorCode, Error<ErrorCode>>
                return Failed(failed.error)
            }
            translated.value ?: return failure(
                category = QueryExecutionErrorCategory.SqlGeneration,
                field = "predicate",
                reason = "Predicate could not be translated"
            )
        }
        val predicates = listOfNotNull(translatedPredicate) + built.extraPredicates
        val predicate = predicates.reduceOrNull { left, right -> left.and(right) }

        val projectionsResult = resolveProjections(plan, built)
        if (projectionsResult.failed) {
            @Suppress("UNCHECKED_CAST")
            val failed = projectionsResult as Failed<List<ColumnDeclaring<*>>, ErrorCode, Error<ErrorCode>>
            return Failed(failed.error)
        }
        val projections = projectionsResult.value ?: return failure(
            category = QueryExecutionErrorCategory.SqlGeneration,
            field = "projections",
            reason = "Projection columns were not resolved"
        )
        if (projections.isEmpty() && built.root.columns.isEmpty()) {
            return failure(
                category = QueryExecutionErrorCategory.SqlGeneration,
                field = "root.defaultColumns",
                reason = "A default projection must contain at least one explicitly registered column"
            )
        }
        var query = if (plan.distinct) {
            if (projections.isEmpty()) built.from.selectDistinct(*built.root.columns.toTypedArray())
            else built.from.selectDistinct(*projections.toTypedArray())
        } else {
            if (projections.isEmpty()) built.from.select(*built.root.columns.toTypedArray())
            else built.from.select(*projections.toTypedArray())
        }
        if (predicate != null) query = query.where(predicate)

        val groupByResult = resolveColumns(plan.groupBy, built.allSources, "groupBy")
        if (groupByResult.failed) {
            @Suppress("UNCHECKED_CAST")
            val failed = groupByResult as Failed<List<ColumnDeclaring<*>>, ErrorCode, Error<ErrorCode>>
            return Failed(failed.error)
        }
        val groupBy = groupByResult.value ?: return failure(
            category = QueryExecutionErrorCategory.SqlGeneration,
            field = "groupBy",
            reason = "Group-by columns were not resolved"
        )
        if (groupBy.isNotEmpty()) query = query.groupBy(*groupBy.toTypedArray())

        val ordersResult = resolveOrders(plan.orderBy, built.allSources)
        if (ordersResult.failed) {
            @Suppress("UNCHECKED_CAST")
            val failed = ordersResult as Failed<List<OrderByExpression>, ErrorCode, Error<ErrorCode>>
            return Failed(failed.error)
        }
        val orders = ordersResult.value ?: return failure(
            category = QueryExecutionErrorCategory.SqlGeneration,
            field = "orderBy",
            reason = "Order-by columns were not resolved"
        )
        if (orders.isNotEmpty()) query = query.orderBy(*orders.toTypedArray())

        plan.page?.let {
            query = query.limit(it.limit)
            if (it.offset > 0) query = query.offset(it.offset)
        }
        val sqlTemplate = try {
            query.sql
        } catch (error: Exception) {
            return failure(
                category = classifyCompilationFailure(error),
                field = "query",
                reason = error.message ?: "SQL generation failed"
            )
        }
        return Ok(
            KtormCompiledQuery(
                query = query,
                plan = plan,
                rootColumns = built.root.columns,
                audit = QueryAuditSummary(
                    sqlTemplate = sqlTemplate,
                    parameterTypes = parameterTypes.toList(),
                    dialect = database.dialect::class.qualifiedName ?: database.dialect::class.simpleName.orEmpty()
                )
            )
        )
    }

    /**
     * 编译根粒度计数查询 / Compile a root-granularity count query
     *
     * 仅接受单列根键；复合根键必须由上层适配器显式实现，避免伪造计数语义。
     * Only a single root key is accepted; adapters must explicitly implement composite keys.
     *
     * @param plan 通用关系查询计划 / Generic relational query plan
     * @return 计数查询或结构化错误 / Count query or structured error
     */
    fun compileCount(plan: RelationalQueryPlan): Ret<Query> {
        val validation = plan.validate()
        if (validation.failed) {
            return validationFailure(validation)
        }
        if (plan.rootKey.size != 1) {
            return failure(
                category = QueryExecutionErrorCategory.SqlGeneration,
                field = "rootKey",
                reason = "Root-granularity count requires exactly one root key column"
            )
        }
        val parameterTypes = mutableListOf<String>()
        val builtResult = buildSources(plan, parameterTypes::add)
        if (builtResult.failed) {
            @Suppress("UNCHECKED_CAST")
            val failed = builtResult as Failed<BuildResult, ErrorCode, Error<ErrorCode>>
            return Failed(failed.error)
        }
        val built = builtResult.value ?: return failure(
            category = QueryExecutionErrorCategory.SqlGeneration,
            field = "sources",
            reason = "Relational query sources were not built"
        )
        val rootKeyRef = plan.rootKey.single()
        if (!matchesSource(rootKeyRef.source, built.root.source)) {
            return failure(
                category = QueryExecutionErrorCategory.SqlGeneration,
                field = "rootKey",
                reason = "Root key must belong to the root query source"
            )
        }
        val predicate = plan.predicate?.let {
            val translated = translatePredicate(
                expression = it,
                bound = built.allSources,
                parameterTypeSink = parameterTypes::add,
                fallbackCategory = QueryExecutionErrorCategory.SqlGeneration,
                field = "predicate"
            )
            if (translated.failed) {
                @Suppress("UNCHECKED_CAST")
                val failed = translated as Failed<ColumnDeclaring<Boolean>?, ErrorCode, Error<ErrorCode>>
                return Failed(failed.error)
            }
            val translatedValue = translated.value ?: return failure<Query>(
                category = QueryExecutionErrorCategory.SqlGeneration,
                field = "predicate",
                reason = "Predicate could not be translated"
            )
            translatedValue
        }
        val rootKeyResult = resolveColumnResult(plan.rootKey.single(), built.allSources, "rootKey")
        if (rootKeyResult.failed) {
            @Suppress("UNCHECKED_CAST")
            val failed = rootKeyResult as Failed<ColumnDeclaring<*>, ErrorCode, Error<ErrorCode>>
            return Failed(failed.error)
        }
        val rootKey = rootKeyResult.value ?: return failure(
            category = QueryExecutionErrorCategory.SqlGeneration,
            field = "rootKey",
            reason = "Root key column was not resolved"
        )
        val predicates = listOfNotNull(predicate) + built.extraPredicates
        val combinedPredicate = predicates.reduceOrNull { left, right -> left.and(right) }
        var query = built.from.select(countDistinct(rootKey))
        if (combinedPredicate != null) query = query.where(combinedPredicate)
        return try {
            query.sql
            Ok(query)
        } catch (error: Exception) {
            failure(
                category = classifyCompilationFailure(error),
                field = "query",
                reason = error.message ?: "SQL generation failed"
            )
        }
    }

    private fun buildSources(
        plan: RelationalQueryPlan,
        parameterTypeSink: (String) -> Unit
    ): Ret<BuildResult> {
        val rootDefinition = sources[plan.root.name] ?: return failure(
            category = QueryExecutionErrorCategory.UnknownSource,
            field = "root",
            reason = "Unknown root query source: ${plan.root.name}"
        )
        val rootResult = bind(rootDefinition, plan.root, "root.defaultColumns")
        if (rootResult.failed) {
            @Suppress("UNCHECKED_CAST")
            return rootResult as Failed<BuildResult, ErrorCode, Error<ErrorCode>>
        }
        val root = rootResult.value ?: return failure(
            category = QueryExecutionErrorCategory.SqlGeneration,
            field = "root.defaultColumns",
            reason = "Root source default columns were not resolved"
        )
        val bound = linkedMapOf<String, BoundSource>(plan.root.name to root)
        val extraPredicates = mutableListOf<ColumnDeclaring<Boolean>>()
        var from = database.from(root.table)
        for ((index, join) in plan.joins.withIndex()) {
            val definition = sources[join.source.name] ?: return failure(
                category = QueryExecutionErrorCategory.UnknownSource,
                field = "joins[$index].source",
                reason = "Unknown join query source: ${join.source.name}"
            )
            val targetResult = bind(definition, join.source, "joins[$index].source.defaultColumns")
            if (targetResult.failed) {
                @Suppress("UNCHECKED_CAST")
                return targetResult as Failed<BuildResult, ErrorCode, Error<ErrorCode>>
            }
            val target = targetResult.value ?: return failure(
                category = QueryExecutionErrorCategory.SqlGeneration,
                field = "joins[$index].source.defaultColumns",
                reason = "Join source default columns were not resolved"
            )
            if (join.type == JoinType.Exists && target.columns.isEmpty()) {
                return failure(
                    category = QueryExecutionErrorCategory.InvalidJoin,
                    field = "joins[$index].source.defaultColumns",
                    reason = "Exists join requires at least one explicitly registered target column"
                )
            }
            val conditionBound = LinkedHashMap(bound)
            conditionBound[join.source.name] = target
            val joinValidationFailure = validateJoinCondition(
                expression = join.condition,
                target = join.source,
                bound = bound,
                field = "joins[$index].condition"
            )
            if (joinValidationFailure != null) return joinValidationFailure
            val condition = translatePredicate(
                expression = join.condition,
                bound = conditionBound,
                parameterTypeSink = parameterTypeSink,
                fallbackCategory = QueryExecutionErrorCategory.InvalidJoin,
                field = "joins[$index].condition"
            )
            if (condition.failed) {
                @Suppress("UNCHECKED_CAST")
                val failed = condition as Failed<ColumnDeclaring<Boolean>?, ErrorCode, Error<ErrorCode>>
                return Failed(failed.error)
            }
            val conditionValue = condition.value ?: return failure(
                category = QueryExecutionErrorCategory.InvalidJoin,
                field = "joins[$index].condition",
                reason = "Join condition could not be translated"
            )
            when (join.type) {
                JoinType.Inner -> {
                    from = from.innerJoin(target.table, conditionValue)
                    bound[join.source.name] = target
                }
                JoinType.Left -> {
                    from = from.leftJoin(target.table, conditionValue)
                    bound[join.source.name] = target
                }
                JoinType.Exists -> {
                    if (!join.cardinality.isToMany) {
                        return failure(
                            category = QueryExecutionErrorCategory.InvalidJoin,
                            field = "joins[$index].cardinality",
                            reason = "Exists join requires a to-many cardinality declaration"
                        )
                    }
                    val existsQuery = target.tableQuerySource().select(*target.columns.toTypedArray())
                        .where(conditionValue)
                    val existsCondition = exists(existsQuery)
                    extraPredicates += existsCondition
                }
            }
        }
        return Ok(BuildResult(from, root, bound, extraPredicates))
    }

    private fun validateJoinCondition(
        expression: BooleanExpression,
        target: QuerySource,
        bound: Map<String, BoundSource>,
        field: String
    ): Failed<BuildResult, ErrorCode, Error<ErrorCode>>? {
        if (containsBooleanConstant(expression)) {
            return failure(
                category = QueryExecutionErrorCategory.InvalidJoin,
                field = field,
                reason = "Join condition must not contain a boolean constant"
            )
        }
        val targetSources = linkedSetOf<String>().apply {
            add(target.name)
            target.alias?.let(::add)
        }
        val boundSources = linkedSetOf<String>().apply {
            bound.values.forEach {
                add(it.source.name)
                it.source.alias?.let(::add)
            }
        }
        if (!containsExactColumnCorrelation(expression, targetSources, boundSources)) {
            return failure(
                category = QueryExecutionErrorCategory.InvalidJoin,
                field = field,
                reason = "Join condition must relate the joined source to an already bound source"
            )
        }
        return null
    }

    private fun containsBooleanConstant(expression: BooleanExpression): Boolean {
        return when (expression) {
            is BooleanConstant -> true
            is AndExpression -> expression.operands.any(::containsBooleanConstant)
            is OrExpression -> expression.operands.any(::containsBooleanConstant)
            is NotExpression -> containsBooleanConstant(expression.operand)
            else -> false
        }
    }

    private fun bind(
        definition: KtormQuerySource,
        source: QuerySource,
        field: String
    ): Ret<BoundSource> {
        val table = source.alias?.let { definition.table.aliased(it) } ?: definition.table
        val columns = mutableListOf<ColumnDeclaring<*>>()
        definition.defaultColumns.forEachIndexed { index, path ->
            when (val result = definition.resolveColumnDetailed.resolveDetailed(path)) {
                is PersistenceFieldResolution.Resolved -> {
                    val column = remapColumn(table, result.value) ?: return failure(
                        category = QueryExecutionErrorCategory.SqlGeneration,
                        field = "$field[$index]",
                        reason = "Registered default column is not present in the bound table: $path"
                    )
                    columns += column
                }
                is PersistenceFieldResolution.Missing -> return failure(
                    category = QueryExecutionErrorCategory.UnknownColumn,
                    field = "$field[$index]",
                    reason = "Registered default column is missing from the field resolver: ${result.path}"
                )
                is PersistenceFieldResolution.Ambiguous -> return failure(
                    category = QueryExecutionErrorCategory.UnknownColumn,
                    field = "$field[$index]",
                    reason = "Ambiguous registered default column: ${result.candidates.joinToString(", ")}"
                )
                is PersistenceFieldResolution.InvalidConfiguration -> return failure(
                    category = QueryExecutionErrorCategory.SqlGeneration,
                    field = "$field[$index]",
                    reason = result.reason
                )
            }
        }
        return Ok(BoundSource(definition, source, table, columns))
    }

    private fun BoundSource.tableQuerySource(): KtormQuerySourceExpression {
        return database.from(table)
    }

    private fun resolver(bound: Map<String, BoundSource>): KtormColumnResolver {
        return KtormColumnResolver { path ->
            (resolveDetailed(path, bound) as? PersistenceFieldResolution.Resolved)?.value
        }
    }

    private fun resolveColumnResult(
        ref: ColumnRef,
        bound: Map<String, BoundSource>,
        field: String
    ): Ret<ColumnDeclaring<*>> {
        return when (val result = resolveDetailed("${ref.source}.${ref.path}", bound)) {
            is PersistenceFieldResolution.Resolved -> Ok(result.value)
            is PersistenceFieldResolution.Missing -> failure(
                category = QueryExecutionErrorCategory.UnknownColumn,
                field = field,
                reason = "Unknown query column: ${result.path}"
            )
            is PersistenceFieldResolution.Ambiguous -> failure(
                category = QueryExecutionErrorCategory.UnknownColumn,
                field = field,
                reason = "Ambiguous query column ${result.path}: ${result.candidates.joinToString(", ")}"
            )
            is PersistenceFieldResolution.InvalidConfiguration -> failure(
                category = QueryExecutionErrorCategory.SqlGeneration,
                field = field,
                reason = result.reason
            )
        }
    }

    private fun resolveColumns(
        refs: List<ColumnRef>,
        bound: Map<String, BoundSource>,
        field: String
    ): Ret<List<ColumnDeclaring<*>>> {
        val result = mutableListOf<ColumnDeclaring<*>>()
        refs.forEachIndexed { index, ref ->
            val column = resolveColumnResult(ref, bound, "$field[$index]")
            if (column.failed) {
                @Suppress("UNCHECKED_CAST")
                return column as Failed<List<ColumnDeclaring<*>>, ErrorCode, Error<ErrorCode>>
            }
            result += column.value ?: return failure(
                category = QueryExecutionErrorCategory.SqlGeneration,
                field = "$field[$index]",
                reason = "Query column was not resolved"
            )
        }
        return Ok(result)
    }

    private fun resolveDetailed(
        path: String,
        bound: Map<String, BoundSource>
    ): PersistenceFieldResolution<ColumnDeclaring<*>> {
        val normalizedPath = path.trim()
        val segments = normalizedPath.split(".", limit = 2)
        val sourceName = if (segments.size == 2) segments[0] else null
        val fieldPath = if (segments.size == 2) segments[1] else normalizedPath
        val candidates = if (sourceName != null) {
            bound.values.filter { it.source.name == sourceName || it.source.alias == sourceName }
        } else {
            bound.values
        }
        if (sourceName != null && candidates.isEmpty()) {
            return PersistenceFieldResolution.Missing(normalizedPath)
        }
        val sourceResolutions = candidates.map { source ->
            source to resolveSourceColumnDetailed(source, fieldPath)
        }
        val invalid = sourceResolutions.firstOrNull { (_, result) ->
            result is PersistenceFieldResolution.InvalidConfiguration
        }?.second
        if (invalid is PersistenceFieldResolution.InvalidConfiguration) {
            return invalid
        }
        val ambiguous = sourceResolutions.flatMap { (source, result) ->
            when (result) {
                is PersistenceFieldResolution.Ambiguous -> result.candidates.map {
                    if (it.contains('.')) it else "${source.source.name}.$it"
                }
                else -> emptyList()
            }
        }
        if (ambiguous.isNotEmpty()) {
            return PersistenceFieldResolution.Ambiguous(normalizedPath, ambiguous)
        }
        val resolved = sourceResolutions.mapNotNull { (source, result) ->
            val original = (result as? PersistenceFieldResolution.Resolved)?.value ?: return@mapNotNull null
            remapColumn(source.table, original)
        }
        return when {
            resolved.isEmpty() -> PersistenceFieldResolution.Missing(normalizedPath)
            resolved.size > 1 -> PersistenceFieldResolution.Ambiguous(
                normalizedPath,
                candidates.map { "${it.source.name}.$fieldPath" }
            )
            else -> PersistenceFieldResolution.Resolved(resolved.single())
        }
    }

    private fun remapColumn(
        table: BaseTable<*>,
        original: ColumnDeclaring<*>
    ): ColumnDeclaring<*>? {
        val column = original as? Column<*>
        return if (column == null) {
            original
        } else {
            table.columns.find { it.name == column.name } as? ColumnDeclaring<*>
        }
    }

    private fun resolveSourceColumnDetailed(
        source: BoundSource,
        path: String
    ): PersistenceFieldResolution<ColumnDeclaring<*>> {
        return when (val result = source.definition.resolveColumnDetailed.resolveDetailed(path)) {
            is PersistenceFieldResolution.Resolved -> {
                val column = result.value as? ColumnDeclaring<*>
                if (column == null) {
                    PersistenceFieldResolution.InvalidConfiguration(
                        "Resolved field is not a Ktorm column: ${source.source.name}.$path"
                    )
                } else {
                    PersistenceFieldResolution.Resolved(column)
                }
            }
            is PersistenceFieldResolution.Missing -> result
            is PersistenceFieldResolution.Ambiguous -> result
            is PersistenceFieldResolution.InvalidConfiguration -> result
        }
    }

    private fun resolveProjections(
        plan: RelationalQueryPlan,
        built: BuildResult
    ): Ret<List<ColumnDeclaring<*>>> {
        val result = mutableListOf<ColumnDeclaring<*>>()
        plan.projections.forEachIndexed { index, projection ->
            val column = resolveColumnResult(projection.column, built.allSources, "projections[$index]")
            if (column.failed) {
                @Suppress("UNCHECKED_CAST")
                return column as Failed<List<ColumnDeclaring<*>>, ErrorCode, Error<ErrorCode>>
            }
            val resolved = column.value ?: return failure(
                category = QueryExecutionErrorCategory.SqlGeneration,
                field = "projections[$index]",
                reason = "Projection column was not resolved"
            )
            result += projection.alias?.let { resolved.aliased(it) } ?: resolved
        }
        return Ok(result)
    }

    private fun resolveOrders(
        specs: List<OrderSpec>,
        bound: Map<String, BoundSource>
    ): Ret<List<OrderByExpression>> {
        val result = mutableListOf<OrderByExpression>()
        specs.forEachIndexed { index, spec ->
            val columnResult = resolveColumnResult(spec.column, bound, "orderBy[$index]")
            if (columnResult.failed) {
                @Suppress("UNCHECKED_CAST")
                return columnResult as Failed<List<OrderByExpression>, ErrorCode, Error<ErrorCode>>
            }
            val column = columnResult.value ?: return failure(
                category = QueryExecutionErrorCategory.SqlGeneration,
                field = "orderBy[$index]",
                reason = "Order-by column was not resolved"
            )
            if (spec.nulls != NullsOrder.Unspecified) {
                result += when (spec.nulls) {
                    NullsOrder.First -> column.isNull().desc()
                    NullsOrder.Last -> column.isNull().asc()
                    NullsOrder.Unspecified -> column.isNull().asc()
                }
            }
            result += when (spec.direction) {
                SortDirection.Ascending -> column.asc()
                SortDirection.Descending -> column.desc()
            }
        }
        return Ok(result)
    }

    private fun translatePredicate(
        expression: BooleanExpression,
        bound: Map<String, BoundSource>,
        parameterTypeSink: (String) -> Unit,
        fallbackCategory: QueryExecutionErrorCategory,
        field: String
    ): Ret<ColumnDeclaring<Boolean>?> {
        val referenceFailure = validatePredicateReferences(expression, bound, field)
        if (referenceFailure != null) {
            return referenceFailure
        }
        val translated = KtormBooleanTranslator(
            resolveColumn = resolver(bound),
            unsupportedPredicatePolicy = unsupportedPredicatePolicy,
            parameterTypeSink = { parameterTypeSink(it.typeName) },
            resolveColumnDetailed = { path -> resolveDetailed(path, bound) },
            targetConstantBinder = targetConstantBinder
        ).translate(expression)
        if (translated.failed) {
            @Suppress("UNCHECKED_CAST")
            val failed = translated as Failed<ColumnDeclaring<Boolean>?, ErrorCode, Error<ErrorCode>>
            val detail = failed.error.value as? UnsupportedPredicateDetail
            val reason = detail?.reason ?: failed.error.message
            val category = when {
                isParameterBindingFailure(reason) -> QueryExecutionErrorCategory.ParameterBinding
                else -> fallbackCategory
            }
            return failure(category, field, reason)
        }
        return Ok(translated.value)
    }

    private fun validatePredicateReferences(
        expression: BooleanExpression,
        bound: Map<String, BoundSource>,
        field: String
    ): Failed<ColumnDeclaring<Boolean>?, ErrorCode, Error<ErrorCode>>? {
        expression.collectReferences().forEach { reference ->
            when (val result = resolveDetailed(reference.value, bound)) {
                is PersistenceFieldResolution.Resolved -> Unit
                is PersistenceFieldResolution.Missing -> return failure(
                    category = QueryExecutionErrorCategory.UnknownColumn,
                    field = field,
                    reason = "Unknown query column: ${result.path}"
                )
                is PersistenceFieldResolution.Ambiguous -> return failure(
                    category = QueryExecutionErrorCategory.UnknownColumn,
                    field = field,
                    reason = "Ambiguous query column ${result.path}: ${result.candidates.joinToString(", ")}"
                )
                is PersistenceFieldResolution.InvalidConfiguration -> return failure(
                    category = QueryExecutionErrorCategory.SqlGeneration,
                    field = field,
                    reason = result.reason
                )
            }
        }
        return null
    }

    private fun <T> failure(
        category: QueryExecutionErrorCategory,
        field: String?,
        reason: String
    ): Failed<T, ErrorCode, Error<ErrorCode>> {
        return Failed(
            ErrorCode.IllegalArgument,
            reason,
            RelationalQueryFailure(category, field, reason)
        )
    }

    private fun <T> validationFailure(
        validation: Ret<Unit>
    ): Failed<T, ErrorCode, Error<ErrorCode>> {
        @Suppress("UNCHECKED_CAST")
        val failed = validation as Failed<Unit, ErrorCode, Error<ErrorCode>>
        val detail = failed.error.value as? RelationalQueryValidationError
        val field = detail?.field
        val category = if (field?.startsWith("joins") == true) {
            QueryExecutionErrorCategory.InvalidJoin
        } else {
            QueryExecutionErrorCategory.SqlGeneration
        }
        return failure(
            category = category,
            field = field,
            reason = detail?.reason ?: failed.error.message
        )
    }

    private fun matchesSource(qualifier: String, source: QuerySource): Boolean {
        return qualifier == source.name || qualifier == source.alias
    }

    private fun isParameterBindingFailure(reason: String): Boolean {
        val normalized = reason.lowercase()
        return normalized.contains("no sql binding") ||
            normalized.contains("unsupported scalar constant") ||
            normalized.contains("incompatible sql types") ||
            normalized.contains("requires a translated")
    }

    private fun classifyCompilationFailure(error: Exception): QueryExecutionErrorCategory {
        val message = error.message.orEmpty().lowercase()
        return if (error is UnsupportedOperationException ||
            message.contains("unsupported") ||
            message.contains("dialect")
        ) {
            QueryExecutionErrorCategory.UnsupportedDialect
        } else {
            QueryExecutionErrorCategory.SqlGeneration
        }
    }
}

/**
 * 执行 Ktorm 查询并返回统计信息 / Execute a Ktorm query with statistics
 *
 * 数据库无法可靠提供扫描行数时保持 null，不伪造精确统计。
 * Keeps scanned rows null when the database cannot provide a reliable value.
 *
 * @param mapper 行映射逻辑 / Row mapping function
 * @return 查询结果或结构化数据库错误 / Query result or structured database error
 */
fun <T> KtormCompiledQuery.execute(
    maxReturnedRows: Int? = null,
    mapper: (QueryRowSet) -> T?
): Ret<QueryExecutionResult<List<T>>> {
    if (maxReturnedRows != null && maxReturnedRows <= 0) {
        return Failed(
            ErrorCode.IllegalArgument,
            "maxReturnedRows must be positive",
            RelationalQueryFailure(
                category = QueryExecutionErrorCategory.SqlGeneration,
                field = "maxReturnedRows",
                reason = "maxReturnedRows must be positive"
            )
        )
    }
    val started = System.nanoTime()
    return try {
        val rows = mutableListOf<T>()
        var truncated = false
        val iterator = query.iterator()
        while (iterator.hasNext()) {
            if (maxReturnedRows != null && rows.size >= maxReturnedRows) {
                truncated = iterator.hasNext()
                break
            }
            val value = mapper(iterator.next()) ?: continue
            rows += value
        }
        val duration = (System.nanoTime() - started) / 1_000_000L
        Ok(
            QueryExecutionResult(
                value = rows.toList(),
                stats = QueryExecutionStats(
                    durationMillis = duration,
                    returnedRows = rows.size.toLong(),
                    truncated = truncated
                )
            )
        )
    } catch (error: Exception) {
        val category = classifyExecutionFailure(error)
        Failed(
            ErrorCode.ApplicationFailed,
            "Ktorm query execution failed: ${error.message ?: error::class.simpleName}",
            RelationalQueryFailure(category, reason = error.message ?: "Database error")
        )
    }
}

private fun classifyExecutionFailure(error: Exception): QueryExecutionErrorCategory {
    val message = error.message.orEmpty().lowercase()
    val sqlState = (error as? java.sql.SQLException)?.sqlState.orEmpty()
    return when {
        error is java.sql.SQLTimeoutException ||
            error::class.simpleName.orEmpty().contains("timeout", ignoreCase = true) ||
            sqlState == "HYT00" || sqlState == "HYT01" || sqlState == "57014" -> {
            QueryExecutionErrorCategory.Timeout
        }
        error is UnsupportedOperationException ||
            message.contains("unsupported") ||
            message.contains("dialect") -> {
            QueryExecutionErrorCategory.UnsupportedDialect
        }
        else -> QueryExecutionErrorCategory.Database
    }
}
