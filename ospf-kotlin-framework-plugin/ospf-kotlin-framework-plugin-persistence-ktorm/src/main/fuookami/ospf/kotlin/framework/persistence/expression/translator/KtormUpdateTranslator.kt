/**
 * Ktorm 更新翻译器
 * Ktorm Update Translator
 *
 * 将 UpdateAssignment 翻译为 Ktorm UPDATE SET 子句。
 * Translates UpdateAssignment to Ktorm UPDATE SET clause.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.*

/** 设置动态解析列的值 / Set value for a dynamically resolved column */
@Suppress("UNCHECKED_CAST")
private fun UpdateStatementBuilder.setDynamicValue(column: ColumnDeclaring<*>, value: Any?) {
    set(column as Column<Any>, value)
}

/**
 * Ktorm 更新翻译器
 * Ktorm Update Translator
 *
 * 将 UpdateAssignments 翻译为 Ktorm 更新构建器调用。
 * Translates UpdateAssignments to Ktorm update builder calls.
 *
 * @property resolveColumn 列解析函数 / Column resolver function
 * @property table Ktorm 表定义 / Ktorm table definition
 */
class KtormUpdateTranslator(
    private val resolveColumn: KtormColumnResolver,
    private val table: Table<*>
) {
    /**
     * 执行更新语句
     * Execute update statement
     *
     * @param database Ktorm 数据库实例 / Ktorm database instance
     * @param whereCondition WHERE 条件 / WHERE condition
     * @param assignments 更新赋值列表 / Update assignment list
     * @return 受影响的行数 / Number of affected rows
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
                        if (column != null) {
                            setDynamicValue(column, item.value)
                        }
                    }
                    is SetNull -> {
                        val column = resolveColumn(item.path)
                        if (column != null) {
                            setDynamicValue(column, null)
                        }
                    }
                    is SetFromExpression -> {
                        val column = resolveColumn(item.path)
                        if (column != null) {
                            val exprValue = (item.expression as? ScalarConstant<*>)?.value
                            if (exprValue != null) {
                                setDynamicValue(column, exprValue)
                            }
                        }
                    }
                }
            }
            if (whereCondition != null) {
                where { whereCondition }
            }
        }
    }
}
