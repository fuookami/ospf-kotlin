/**
 * Ktorm 列绑定器
 * Ktorm column binder
 *
 * 提供基于 Ktorm 表的强类型列绑定能力。
 * Provides strong-typed column binding based on Ktorm tables.
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
     * 解析属性路径为 Ktorm 列声明
     * Resolve property path to Ktorm column declaring
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
fun KtormColumnBinder<*>.asKtormResolver(): KtormColumnResolver = { path -> resolve(path) }

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
    return { path -> binder.resolve(path) }
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
    return { path -> binder.resolve(path) }
}
