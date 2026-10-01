/**
 * MySQL JdbcClient SQL 方言 / MySQL JdbcClient SQL dialect
*/
package fuookami.ospf.kotlin.framework.persistence

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.SortDirection
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientDialect
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientSqlFragment

/** MySQL JdbcClient 方言 / MySQL JdbcClient dialect */
object MySQLJdbcClientDialect : JdbcClientDialect {
    override val name: String = "mysql"

    override fun quoteIdentifier(identifier: String): Ret<String> {
        if (identifier.isBlank() || identifier.any { Character.isISOControl(it) }) {
            return Failed(ErrorCode.IllegalArgument, "MySQL 标识符无效 / MySQL identifier is invalid")
        }
        return ok("`${identifier.replace("`", "``")}`")
    }

    override fun pagination(limit: Int?, offset: Int?): Ret<JdbcClientSqlFragment> {
        if (limit != null && limit < 0 || offset != null && offset < 0) {
            return Failed(ErrorCode.IllegalArgument, "分页参数不能为负数 / Pagination values must not be negative")
        }
        if (offset != null && limit == null) {
            return Failed(ErrorCode.IllegalArgument, "MySQL offset 分页必须指定 limit / MySQL offset pagination requires a limit")
        }
        if (limit == null) {
            return ok(JdbcClientSqlFragment(""))
        }
        val offsetSql = if (offset == null) "" else " OFFSET ?"
        val parameters = if (offset == null) listOf(limit) else listOf(limit, offset)
        return ok(JdbcClientSqlFragment("LIMIT ?$offsetSql", parameters))
    }

    override fun orderNulls(
        expressionSql: String,
        direction: SortDirection,
        nulls: NullsOrder?
    ): Ret<JdbcClientSqlFragment> {
        if (expressionSql.isBlank()) {
            return Failed(ErrorCode.IllegalArgument, "排序表达式不能为空 / Sort expression must not be blank")
        }
        val directionSql = when (direction) {
            SortDirection.Asc -> "ASC"
            SortDirection.Desc -> "DESC"
        }
        val nullsSql = when (nulls) {
            NullsOrder.NullsFirst -> "CASE WHEN ($expressionSql) IS NULL THEN 0 ELSE 1 END ASC, "
            NullsOrder.NullsLast -> "CASE WHEN ($expressionSql) IS NULL THEN 1 ELSE 0 END ASC, "
            null -> ""
        }
        return ok(JdbcClientSqlFragment("$nullsSql$expressionSql $directionSql"))
    }
}
