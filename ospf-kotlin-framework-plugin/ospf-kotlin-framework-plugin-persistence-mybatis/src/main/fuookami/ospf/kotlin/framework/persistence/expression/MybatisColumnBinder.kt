/**
 * MyBatis 列绑定器
 * MyBatis column binder
 *
 * 提供基于 MyBatis 的强类型列绑定能力。
 * Provides strong-typed column binding based on MyBatis.
 *
 * 使用方式 / Usage:
 * ```kotlin
 * // 1. 通过 schema 生成 resolver
 * val resolver = UserSchema.mybatisResolver()
 *
 * // 2. 将 resolver 交给仓储
 * class UserRepository(mapper: UserMapper) : MybatisRepository<User, UserMapper>(mapper, UserSchema.mybatisResolver())
 *
 * // 3. 用 schema 的 predicate 构造强类型谓词
 * val where = UserSchema.predicate { (status eq "active") and (name like "%test%") }
 * val users = repository.find(where)
 * ```
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.utils.meta_programming.NameTransfer
import fuookami.ospf.kotlin.utils.meta_programming.NamingSystem

/**
 * MyBatis 列名解析器函数类型
 * MyBatis column name resolver function type
 */
typealias MybatisColumnNameResolver = PersistenceFieldResolver<String>

/**
 * MyBatis 列绑定器
 * MyBatis column binder
 *
 * @property columnMapping 属性名到后端列名的映射 / Property name to backend column name mapping
 * @property namingTransfer 命名转换器，无映射时用于回退 / Naming transfer for fallback when no mapping found
 */
class MybatisColumnBinder(
    private val columnMapping: Map<String, String> = emptyMap(),
    private val namingTransfer: NameTransfer = NameTransfer(NamingSystem.CamelCase, NamingSystem.SnakeCase)
) : ColumnBinder<String> {
    override fun resolve(path: String): String? {
        return columnMapping[path] ?: namingTransfer(path)
    }
}

/**
 * 将 MybatisColumnBinder 转为 MybatisColumnNameResolver
 * Convert MybatisColumnBinder to MybatisColumnNameResolver
 *
 * @return MyBatis 列名解析器 / MyBatis column name resolver
 */
fun MybatisColumnBinder.asMybatisResolver(): MybatisColumnNameResolver = { path -> resolve(path) }

/**
 * 从 HasColumnMapping 创建 MybatisColumnNameResolver
 * Create MybatisColumnNameResolver from HasColumnMapping
 *
 * 使用 KSP 生成的 HasColumnMapping.columnMapping 作为属性名到列名的映射。
 * 无映射时回退到 camelCase -> snake_case 转换。
 * Uses KSP generated HasColumnMapping.columnMapping as property-to-column name mapping.
 * Falls back to camelCase -> snake_case conversion when no mapping found.
 *
 * @return MyBatis 列名解析器 / MyBatis column name resolver
 */
fun HasColumnMapping.mybatisResolver(): MybatisColumnNameResolver {
    val binder = MybatisColumnBinder(columnMapping)
    return { path -> binder.resolve(path) }
}

/**
 * 从显式映射创建 MybatisColumnNameResolver
 * Create MybatisColumnNameResolver from explicit mapping
 *
 * @param columnMapping 属性名到后端列名的显式映射 / Explicit property name to backend column name mapping
 * @return MyBatis 列名解析器 / MyBatis column name resolver
 */
fun mybatisResolver(columnMapping: Map<String, String>): MybatisColumnNameResolver {
    val binder = MybatisColumnBinder(columnMapping)
    return { path -> binder.resolve(path) }
}
