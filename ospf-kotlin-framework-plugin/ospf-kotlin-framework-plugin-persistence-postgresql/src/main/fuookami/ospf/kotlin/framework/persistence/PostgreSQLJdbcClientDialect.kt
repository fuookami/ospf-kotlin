/**
 * PostgreSQL JdbcClient SQL 方言 / PostgreSQL JdbcClient SQL dialect
*/
package fuookami.ospf.kotlin.framework.persistence

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.SortDirection
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientDialect
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientSqlFragment

/** PostgreSQL JdbcClient 方言 / PostgreSQL JdbcClient dialect */
object PostgreSQLJdbcClientDialect : JdbcClientDialect {
    override val name: String = "postgresql"

    override fun quoteIdentifier(identifier: String): Ret<String> {
        if (identifier.isBlank() || identifier.any { Character.isISOControl(it) }) {
            return Failed(ErrorCode.IllegalArgument, "PostgreSQL 标识符无效 / PostgreSQL identifier is invalid")
        }
        return ok("\"${identifier.replace("\"", "\"\"")}\"")
    }

    override fun pagination(limit: Int?, offset: Int?): Ret<JdbcClientSqlFragment> {
        if (limit != null && limit < 0 || offset != null && offset < 0) {
            return Failed(ErrorCode.IllegalArgument, "分页参数不能为负数 / Pagination values must not be negative")
        }
        val clauses = mutableListOf<String>()
        val parameters = mutableListOf<Any?>()
        if (limit != null) {
            clauses += "LIMIT ?"
            parameters += limit
        }
        if (offset != null) {
            clauses += "OFFSET ?"
            parameters += offset
        }
        return ok(JdbcClientSqlFragment(clauses.joinToString(" "), parameters))
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
            NullsOrder.NullsFirst -> " NULLS FIRST"
            NullsOrder.NullsLast -> " NULLS LAST"
            null -> ""
        }
        return ok(JdbcClientSqlFragment("$expressionSql $directionSql$nullsSql"))
    }
}
