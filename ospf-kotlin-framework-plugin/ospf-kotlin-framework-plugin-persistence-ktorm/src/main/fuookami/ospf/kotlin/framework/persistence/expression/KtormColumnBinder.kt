/**
 * Ktorm 列绑定器
 * Ktorm column binder
 *
 * 提供基于 Ktorm 表的强类型列绑定能力。 / Provides strong-typed column binding based on Ktorm tables.
 *
 * 使用方式 / Usage:
 * ```kotlin
 * // 1. 通过 schema 生成 resolver
 * val resolver = UserSchema.ktormResolver(UsersTable)
 *
 * // 2. 将 resolver 交给仓储
 * class UserRepository(db: Database) : KtormRepository<User>(db, UsersTable, UserSchema.ktormResolver(UsersTable))
 *
 * // 3. 用 schema 的 predicate 构造强类型谓词
 * val where = UserSchema.predicate { (status eq "active") and (name like "%test%") }
 * val users = repository.find(where)
 * ```
*/
package fuookami.ospf.kotlin.framework.persistence.expression

import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table
import fuookami.ospf.kotlin.framework.persistence.expression.translator.KtormColumnResolver

/**
 * Ktorm 列绑定器
 * Ktorm column binder
 *
 * @param T Ktorm 表类型 / Ktorm table type
 * @property table Ktorm 表定义 / Ktorm table definition
 * @property columnMapping 属性名到后端列名的映射 / Property name to backend column name mapping
*/
class KtormColumnBinder<T : Table<*>>(
    val table: T,
    private val columnMapping: Map<String, String> = emptyMap()
) : ColumnBinder<ColumnDeclaring<*>> {

    /**
     * 解析属性路径为 Ktorm 列声明 / Resolve property path to Ktorm column declaring
     *
     * @param path 属性路径 / Property path
     * @return 对应的 Ktorm 列声明，未找到时返回 null / Corresponding Ktorm column declaring, or null if not found
    */
    override fun resolve(path: String): ColumnDeclaring<*>? {
        val backendName = columnMapping[path] ?: path
        return table.columns.find { it.name == backendName } as? ColumnDeclaring<*>
    }
}

/**
 * 将 KtormColumnBinder 转为 KtormColumnResolver
 * Convert KtormColumnBinder to KtormColumnResolver
 *
 * @return Ktorm 列解析器 / Ktorm column resolver
*/
fun KtormColumnBinder<*>.asKtormResolver(): KtormColumnResolver = KtormColumnResolver { path -> resolve(path) }

/**
 * 从 PredicateSchema + KtormTable 创建 KtormColumnResolver
 * Create KtormColumnResolver from PredicateSchema + KtormTable
 *
 * 使用 KSP 生成的 HasColumnMapping.columnMapping 作为属性名到列名的映射。
 * Uses KSP generated HasColumnMapping.columnMapping as property-to-column name mapping.
 *
 * @param table Ktorm 表定义 / Ktorm table definition
 * @return Ktorm 列解析器 / Ktorm column resolver
*/
fun HasColumnMapping.ktormResolver(table: Table<*>): KtormColumnResolver {
    val binder = KtormColumnBinder(table, columnMapping)
    return KtormColumnResolver { path -> binder.resolve(path) }
}

/**
 * 从 PredicateSchema + KtormTable + 显式映射创建 KtormColumnResolver
 * Create KtormColumnResolver from PredicateSchema + KtormTable + explicit mapping
 *
 * @param table Ktorm 表定义 / Ktorm table definition
 * @param columnMapping 属性名到后端列名的显式映射 / Explicit property name to backend column name mapping
 * @return Ktorm 列解析器 / Ktorm column resolver
*/
fun ktormResolver(table: Table<*>, columnMapping: Map<String, String>): KtormColumnResolver {
    val binder = KtormColumnBinder(table, columnMapping)
    return KtormColumnResolver { path -> binder.resolve(path) }
}

/**
 * Ktorm 多数据源列解析器构建器 / Ktorm multi-source column resolver builder
 *
 * 注册项必须由适配器代码显式提供，解析器不会根据用户输入猜测表名或列名。
 * Entries must be explicitly registered by adapter code; the resolver never guesses table or column names from user input.
 */
class ResolveColumnBuilder internal constructor() {
    private val qualified = linkedMapOf<String, ColumnDeclaring<*>>()
    private val short = linkedMapOf<String, MutableList<Pair<String, ColumnDeclaring<*>>>>()
    private val errors = mutableListOf<String>()

    /**
     * 注册单表字段 / Register a field without a source qualifier
     *
     * @param path 字段路径 / Field path
     * @param column Ktorm 列 / Ktorm column
     */
    fun map(path: String, column: ColumnDeclaring<*>) {
        register(null, path, column)
    }

    /**
     * 注册限定字段 / Register a qualified field
     *
     * @param source 数据源或别名 / Data source or alias
     * @param path 字段路径 / Field path
     * @param column Ktorm 列 / Ktorm column
     */
    fun map(source: String, path: String, column: ColumnDeclaring<*>) {
        register(source, path, column)
    }

    /**
     * 注册任意数据源的一组字段 / Register a group of fields for a data source
     *
     * @param source 数据源或别名 / Data source or alias
     * @param block 字段注册逻辑 / Field registration block
     */
    fun source(source: String, block: ResolveTableBuilder.() -> Unit) {
        ResolveTableBuilder(source, this).apply(block)
    }

    /**
     * 注册主表字段 / Register fields for the main table
     *
     * 默认使用 `main` 作为限定名；调用方也可以提供实际查询别名。
     * The default qualifier is `main`; callers can provide the actual query alias.
     *
     * @param source 数据源或别名 / Data source or alias
     * @param block 字段注册逻辑 / Field registration block
     */
    fun mainTable(source: String = "main", block: ResolveTableBuilder.() -> Unit) {
        source(source, block)
    }

    /**
     * 注册版本表字段 / Register fields for the version table
     *
     * 默认使用 `version` 作为限定名；调用方也可以提供实际查询别名。
     * The default qualifier is `version`; callers can provide the actual query alias.
     *
     * @param source 数据源或别名 / Data source or alias
     * @param block 字段注册逻辑 / Field registration block
     */
    fun versionTable(source: String = "version", block: ResolveTableBuilder.() -> Unit) {
        source(source, block)
    }

    private fun register(source: String?, path: String, column: ColumnDeclaring<*>) {
        if (path.isBlank()) {
            errors += "Column path must not be blank / 列路径不能为空"
            return
        }
        val normalizedPath = path.trim()
        if (source == null) {
            short.getOrPut(normalizedPath) { mutableListOf() }.add("" to column)
        } else {
            val normalizedSource = source.trim()
            if (normalizedSource.isBlank()) {
                errors += "Column source must not be blank / 数据源不能为空"
                return
            }
            val key = "$normalizedSource.$normalizedPath"
            if (key in qualified) {
                errors += "Duplicate column mapping: $key / 列映射重复"
                return
            }
            qualified[key] = column
            short.getOrPut(normalizedPath) { mutableListOf() }.add(normalizedSource to column)
        }
    }

    internal fun build(): KtormColumnResolver {
        if (errors.isNotEmpty()) {
            return KtormColumnResolver { null }
        }
        val qualifiedCopy = qualified.toMap()
        val shortCopy = short.mapValues { (_, values) -> values.toList() }
        return KtormColumnResolver { path ->
            val normalizedPath = path.trim()
            qualifiedCopy[normalizedPath]
                ?: shortCopy[normalizedPath]
                    ?.singleOrNull()
                    ?.second
        }
    }

    internal fun buildDiagnostic(): DiagnosticPersistenceFieldResolver<ColumnDeclaring<*>> {
        val configurationError = errors.joinToString("; ")
        val qualifiedCopy = qualified.toMap()
        val shortCopy = short.mapValues { (_, values) -> values.toList() }
        return object : DiagnosticPersistenceFieldResolver<ColumnDeclaring<*>> {
            override fun resolveDetailed(path: String): PersistenceFieldResolution<ColumnDeclaring<*>> {
                if (configurationError.isNotBlank()) {
                    return PersistenceFieldResolution.InvalidConfiguration(configurationError)
                }
                val normalizedPath = path.trim()
                qualifiedCopy[normalizedPath]?.let { return PersistenceFieldResolution.Resolved(it) }
                val candidates = shortCopy[normalizedPath].orEmpty()
                return when {
                    candidates.isEmpty() -> PersistenceFieldResolution.Missing(path)
                    candidates.size > 1 -> PersistenceFieldResolution.Ambiguous(
                        path = path,
                        candidates = candidates.map { (source, _) ->
                            if (source.isBlank()) path else "$source.$normalizedPath"
                        }
                    )
                    else -> PersistenceFieldResolution.Resolved(candidates.single().second)
                }
            }
        }
    }
}

/**
 * 版本化表字段注册器 / Versioned table field registrar
 *
 * 该对象只允许向外层构建器注册字段，不持有表名或 SQL 字符串。
 * This registrar only adds fields to the enclosing builder and does not hold table names or SQL strings.
 *
 * @param source 数据源或别名 / Data source or alias
 * @param owner 外层列解析器构建器 / Enclosing column resolver builder
 */
class ResolveTableBuilder internal constructor(
    private val source: String,
    private val owner: ResolveColumnBuilder
) {
    /**
     * 注册字段 / Register a field
     *
     * @param path 字段路径 / Field path
     * @param column Ktorm 列 / Ktorm column
     */
    fun map(path: String, column: ColumnDeclaring<*>) {
        owner.map(source, path, column)
    }
}

/**
 * 构造 Ktorm 列解析器 / Build a Ktorm column resolver
 *
 * @param block 字段注册逻辑 / Field registration block
 * @return Ktorm 列解析器 / Ktorm column resolver
 */
fun resolveColumn(block: ResolveColumnBuilder.() -> Unit): KtormColumnResolver {
    return ResolveColumnBuilder().apply(block).build()
}

/**
 * 构造带诊断的 Ktorm 列解析器 / Build a diagnostic Ktorm column resolver
 *
 * @param block 字段注册逻辑 / Field registration block
 * @return 带诊断的解析器 / Diagnostic resolver
 */
fun resolveColumnWithDiagnostics(
    block: ResolveColumnBuilder.() -> Unit
): DiagnosticPersistenceFieldResolver<ColumnDeclaring<*>> {
    return ResolveColumnBuilder().apply(block).buildDiagnostic()
}
