/**
 * Ktorm 更新翻译器
 * Ktorm Update Translator
 *
 * 将 UpdateAssignment 翻译为 Ktorm UPDATE SET 子句。
 * Translates UpdateAssignment to Ktorm UPDATE SET clause.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table

/**
 * Ktorm 更新翻译器
 * Ktorm Update Translator
 *
 * 将 UpdateAssignments 翻译为 Ktorm 更新构建器调用。
 * Translates UpdateAssignments to Ktorm update builder calls.
 *
 * @property meta 实体元数据 / Entity metadata
 * @property table Ktorm 表定义 / Ktorm table definition
 */
class KtormUpdateTranslator(
    private val meta: EntityMeta<*>,
    private val table: Table<*>
) {
    /**
     * 执行更新语句
     * Execute update statement
     */
    fun executeUpdate(
        database: Database,
        whereCondition: ColumnDeclaring<Boolean>?,
        assignments: UpdateAssignments
    ): Int {
        if (assignments.isEmpty()) return 0

        return database.update(table) {
            for (item in assignments.items) {
                when (item) {
                    is SetValue -> {
                        val column = resolveColumn(item.path)
                        set(column, item.value)
                    }
                    is SetNull -> {
                        val column = resolveColumn(item.path)
                        set(column, null)
                    }
                    is SetFromExpression -> {
                        val column = resolveColumn(item.path)
                        val exprValue = (item.expression as? ScalarConstant<*>)?.value
                        if (exprValue != null) {
                            set(column, exprValue)
                        } else {
                            throw IllegalArgumentException("Cannot translate expression for path: ${item.path}")
                        }
                    }
                }
            }
            if (whereCondition != null) {
                where { whereCondition }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveColumn(path: String): Column<Any> {
        val column = meta.resolveColumn(path) ?: throw IllegalArgumentException("Cannot resolve path: $path")
        return column as Column<Any>
    }
}