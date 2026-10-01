/**
 * Ktorm 更新翻译器 / Ktorm Update Translator
 *
 * 将 UpdateAssignments 翻译为 Ktorm UPDATE SET 子句。
 * Translates UpdateAssignments to Ktorm UPDATE SET clauses.
*/
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import java.util.concurrent.CancellationException
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 设置动态解析列的值 / Set value for a dynamically resolved column
 *
 * @param column 列声明表达式 / Column declaring expression
 * @param value 要设置的值 / Value to set
 */
@Suppress("UNCHECKED_CAST")
private fun UpdateStatementBuilder.setDynamicValue(column: Column<*>, value: Any?) {
    set(column as Column<Any>, value)
}

@Suppress("UNCHECKED_CAST")
private fun UpdateStatementBuilder.setDynamicExpression(column: Column<*>, expression: ColumnDeclaring<*>) {
    set(column as Column<Nothing>, expression as ColumnDeclaring<Nothing>)
}

/**
 * Ktorm 更新翻译器 / Ktorm update translator
 *
 * @property resolveColumn 列解析函数 / Column resolver function
 * @property table Ktorm 表定义 / Ktorm table definition
 */
class KtormUpdateTranslator(
    private val resolveColumn: KtormColumnResolver,
    private val table: Table<*>
) {

    private data class TranslatedAssignment(
        val column: Column<*>,
        val value: Any? = null,
        val expression: ColumnDeclaring<*>? = null
    )

    /**
     * 翻译并执行更新 / Translate and execute an update
     *
     * @param database Ktorm 数据库实例 / Ktorm database instance
     * @param whereCondition WHERE 条件 / WHERE condition
     * @param assignments 更新赋值列表 / Update assignment list
     * @return 受影响的行数结果 / Result containing the number of affected rows
     */
    fun executeUpdate(
        database: Database,
        whereCondition: ColumnDeclaring<Boolean>?,
        assignments: UpdateAssignments
    ): Ret<Int> {
        if (assignments.isEmpty()) return Ok(0)

        return try {
            val translatedAssignments = translateAssignments(assignments)
            translatedAssignments.propagateFailure<Int>()?.let { return it }
            val values = translatedAssignments.value
                ?: return assignmentFailure("<unknown>", "translation returned no value")

            Ok(database.update(table) {
                for (assignment in values) {
                    val expression = assignment.expression
                    if (expression == null) {
                        setDynamicValue(assignment.column, assignment.value)
                    } else {
                        setDynamicExpression(assignment.column, expression)
                    }
                }
                if (whereCondition != null) {
                    where { whereCondition }
                }
            })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("Ktorm update", error)
        }
    }

    private fun translateAssignments(assignments: UpdateAssignments): Ret<List<TranslatedAssignment>> {
        val translated = mutableListOf<TranslatedAssignment>()
        val scalarTranslator = KtormScalarTranslator(resolveColumn)
        for (item in assignments.items) {
            val declaring = resolveColumn(item.path)
                ?: return assignmentFailure(item.path, "column was not resolved")
            val column = declaring as? Column<*>
                ?: return assignmentFailure(item.path, "resolved target is not an updatable column")
            when (item) {
                is SetValue -> translated += TranslatedAssignment(column, item.value)
                is SetNull -> translated += TranslatedAssignment(column, null)
                is SetFromExpression -> {
                    val result = scalarTranslator.translate(item.expression)
                    result.propagateFailure<List<TranslatedAssignment>>()?.let { return it }
                    val expression = result.value as? ColumnDeclaring<*>
                        ?: return assignmentFailure(item.path, "expression could not be translated")
                    if (expression.sqlType.typeCode != column.sqlType.typeCode ||
                        expression.sqlType.typeName != column.sqlType.typeName
                    ) {
                        return assignmentFailure(item.path, "expression SQL type does not match target column")
                    }
                    translated += TranslatedAssignment(column, expression = expression)
                }
            }
        }
        return Ok(translated)
    }

    private fun <T> assignmentFailure(path: String, reason: String): Ret<T> {
        return Failed(
            ErrorCode.IllegalArgument,
            "更新赋值翻译失败：字段 $path：$reason / Update assignment translation failed: field $path: $reason"
        )
    }
}
