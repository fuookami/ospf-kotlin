/**
 * JdbcClient 排序翻译器 / JdbcClient order-by translator
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.*

/**
 * 将排序描述翻译为 SQL / Translate sort descriptions to SQL
 *
 * @property resolveColumnName 属性路径解析器 / Property path resolver
 * @property dialect SQL 方言 / SQL dialect
 */
class JdbcClientOrderByTranslator(
    private val resolveColumnName: JdbcClientColumnNameResolver,
    private val dialect: JdbcClientDialect
) {
    /**
     * 翻译排序 / Translate a sort description
     *
     * @param sortBy 排序描述 / Sort description
     * @return ORDER BY 子句，空排序返回 null / ORDER BY clause or null for an empty sort
     */
    fun translate(sortBy: SortBy): Ret<JdbcClientSqlFragment?> {
        if (sortBy.isEmpty()) return Ok(null)
        val fragments = mutableListOf<JdbcClientSqlFragment>()
        for (item in sortBy.items) {
            val column = resolveColumnName(item.path)
                ?: return Failed(ErrorCode.IllegalArgument, "无法解析排序字段：${item.path} / Cannot resolve sort field: ${item.path}")
            val quoted = when (val result = quoteIdentifierPath(column, dialect)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            when (val result = dialect.orderNulls(quoted, item.direction, item.nulls)) {
                is Ok -> fragments.add(result.value)
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return Ok(
            JdbcClientSqlFragment(
                sql = fragments.joinToString(", ") { it.sql },
                parameters = fragments.flatMap { it.parameters }
            )
        )
    }
}
