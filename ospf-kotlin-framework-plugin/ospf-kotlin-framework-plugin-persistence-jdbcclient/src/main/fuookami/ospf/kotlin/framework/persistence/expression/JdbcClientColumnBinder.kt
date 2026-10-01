/**
 * JdbcClient 列绑定器 / JdbcClient column binder
 *
 * 将表达式属性路径映射到物理列名；没有显式映射时回退到驼峰转蛇形。
 * Maps expression property paths to physical column names and falls back to camelCase-to-snake_case conversion.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.utils.meta_programming.NameTransfer
import fuookami.ospf.kotlin.utils.meta_programming.NamingSystem

/** JdbcClient 列名解析器 / JdbcClient column name resolver */
typealias JdbcClientColumnNameResolver = PersistenceFieldResolver<String>

/**
 * 列绑定器 / Column binder
 *
 * @property columnMapping 属性路径到物理列名的映射 / Property path to physical column mapping
 * @property namingTransfer 未映射路径的转换规则 / Conversion rule for unmapped paths
 */
class JdbcClientColumnBinder(
    private val columnMapping: Map<String, String> = emptyMap(),
    private val namingTransfer: NameTransfer = NameTransfer(NamingSystem.CamelCase, NamingSystem.SnakeCase)
) : ColumnBinder<String> {
    /**
     * 解析属性路径 / Resolve a property path
     *
     * @param path 表达式属性路径 / Expression property path
     * @return 物理列名；未映射时按命名规则转换 / Physical column name, converted by the naming rule when unmapped
     */
    override fun resolve(path: String): String? {
        return columnMapping[path] ?: namingTransfer(path)
    }
}

/**
 * 将绑定器转为 JdbcClient 列解析器 / Convert a binder to a JdbcClient column resolver
 *
 * @return JdbcClient 列解析器 / JdbcClient column resolver
 */
fun JdbcClientColumnBinder.asJdbcClientResolver(): JdbcClientColumnNameResolver =
    JdbcClientColumnNameResolver { path -> resolve(path) }

/**
 * 从生成的列映射创建 JdbcClient 列解析器 / Create a JdbcClient resolver from a generated column mapping
 *
 * @return 列解析器 / Column resolver
 */
fun HasColumnMapping.jdbcClientResolver(): JdbcClientColumnNameResolver =
    JdbcClientColumnBinder(columnMapping).asJdbcClientResolver()

/**
 * 从显式映射创建 JdbcClient 列解析器 / Create a JdbcClient resolver from an explicit mapping
 *
 * @param columnMapping 属性路径到物理列名的映射 / Property path to physical column mapping
 * @return 列解析器 / Column resolver
 */
fun jdbcClientResolver(columnMapping: Map<String, String>): JdbcClientColumnNameResolver =
    JdbcClientColumnBinder(columnMapping).asJdbcClientResolver()
