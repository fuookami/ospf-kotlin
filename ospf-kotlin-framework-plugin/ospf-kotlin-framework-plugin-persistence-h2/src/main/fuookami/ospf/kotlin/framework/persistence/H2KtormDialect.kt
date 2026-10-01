/**
 * H2 Ktorm SQL dialect / H2 Ktorm SQL 方言
 */
package fuookami.ospf.kotlin.framework.persistence

import org.ktorm.database.Database
import org.ktorm.database.SqlDialect
import org.ktorm.expression.*
import org.ktorm.schema.IntSqlType

/** H2 pagination formatter for Ktorm. / Ktorm 使用的 H2 分页格式化器。 */
internal object H2KtormDialect : SqlDialect {
    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return object : SqlFormatter(database, beautifySql, indentSize) {
            override fun writePagination(expr: QueryExpression) {
                newLine(Indentation.SAME)
                if (expr.limit != null) {
                    writeKeyword("limit ? ")
                    _parameters += ArgumentExpression(expr.limit, IntSqlType)
                }
                if (expr.offset != null) {
                    writeKeyword("offset ? ")
                    _parameters += ArgumentExpression(expr.offset, IntSqlType)
                }
            }
        }
    }
}
