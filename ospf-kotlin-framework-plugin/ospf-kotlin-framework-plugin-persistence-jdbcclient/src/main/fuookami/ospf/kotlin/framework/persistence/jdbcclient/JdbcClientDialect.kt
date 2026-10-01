/**
 * JdbcClient SQL 方言扩展点 / JdbcClient SQL dialect extension point
 *
 * 方言只负责标识符引用及数据库特有的排序、分页片段。 / A dialect handles identifier quoting and database-specific order and pagination fragments.
 */
package fuookami.ospf.kotlin.framework.persistence.jdbcclient

import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.SortDirection

/**
 * JdbcClient 参数化 SQL 片段 / JdbcClient parameterized SQL fragment
 *
 * @property sql SQL 文本 / SQL text
 * @property parameters 按占位符出现顺序排列的绑定值 / Bound values in placeholder order
 */
data class JdbcClientSqlFragment(
    val sql: String,
    val parameters: List<Any?> = emptyList()
)

/**
 * 数据库方言契约 / Database dialect contract
 *
 * @property name 稳定的方言名称 / Stable dialect name
 */
interface JdbcClientDialect {
    val name: String

    /**
     * 安全引用一个标识符段 / Safely quote a single identifier segment
     *
     * @param identifier 未引用的标识符段 / Unquoted identifier segment
     * @return 引用后的标识符，失败时返回结构化错误 / Quoted identifier or a structured error
     */
    fun quoteIdentifier(identifier: String): Ret<String>

    /**
     * 生成分页子句 / Build a pagination clause
     *
     * @param limit 最大返回行数 / Maximum number of returned rows
     * @param offset 跳过的行数 / Number of rows to skip
     * @return 分页 SQL 及其有序绑定参数 / Pagination SQL and ordered bind parameters
     */
    fun pagination(limit: Int?, offset: Int?): Ret<JdbcClientSqlFragment>

    /**
     * 生成一个完整的排序项，并处理空值排序 / Build one complete order item and its NULL ordering
     *
     * @param expressionSql 已引用的列 SQL / Quoted column SQL
     * @param direction 排序方向 / Sort direction
     * @param nulls 空值排序要求；为 null 时使用数据库默认值 / Requested NULL order; null uses the database default
     * @return 排序 SQL 片段 / Order SQL fragment
     */
    fun orderNulls(
        expressionSql: String,
        direction: SortDirection,
        nulls: NullsOrder?
    ): Ret<JdbcClientSqlFragment>
}

/**
 * JdbcClient 和对应方言 / JdbcClient paired with its SQL dialect
 *
 * @property client Spring JdbcClient 实例 / Spring JdbcClient instance
 * @property dialect SQL 方言 / SQL dialect
 */
data class JdbcClientBackend(
    val client: org.springframework.jdbc.core.simple.JdbcClient,
    val dialect: JdbcClientDialect
)
