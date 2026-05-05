/**
 * Ktorm 布尔表达式翻译器
 * Ktorm Boolean Expression Translator
 *
 * 将 BooleanExpression 翻译为 Ktorm ColumnDeclaring<Boolean>。
 * Translates BooleanExpression to Ktorm ColumnDeclaring<Boolean>.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import org.ktorm.dsl.*
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table

/**
 * 列名解析器
 * Column Name Resolver
 *
 * 将 PropertyPath 解析为 Ktorm Column。
 * Resolves PropertyPath to Ktorm Column.
 */
typealias KtormColumnResolver = (String) -> ColumnDeclaring<*>?

/**
 * Ktorm 布尔表达式翻译器
 * Ktorm Boolean Expression Translator
 *
 * 将 math.symbol.expression.BooleanExpression 翻译为 Ktorm 查询条件。
 * Translates math.symbol.expression.BooleanExpression to Ktorm query conditions.
 *
 * @property resolveColumn 列解析函数 / Column resolver function
 * @property patternMatchPolicy 模式匹配策略 / Pattern match policy
 */
class KtormBooleanTranslator(
    private val resolveColumn: KtormColumnResolver,
    private val patternMatchPolicy: PatternMatchPolicy = DefaultPatternMatchPolicy
) {
    /**
     * 翻译布尔表达式
     * Translate boolean expression
     */
    fun translate(expr: BooleanExpression): ColumnDeclaring<Boolean>? {
        return when (expr) {
            is BooleanConstant -> null
            is Comparison<*> -> translateComparison(expr)
            is InExpression<*> -> null
            is PatternMatch<*> -> translatePatternMatch(expr)
            is NullCheck -> translateNullCheck(expr)
            is AndExpression -> translateAnd(expr)
            is OrExpression -> translateOr(expr)
            is NotExpression -> translateNot(expr)
            is BooleanCustom -> null
        }
    }
    private fun translateComparison(expr: Comparison<*>): ColumnDeclaring<Boolean>? {
        val leftRef = expr.left as? ScalarReference<*>
        val leftConst = expr.left as? ScalarConstant<*>
        val rightRef = expr.right as? ScalarReference<*>
        val rightConst = expr.right as? ScalarConstant<*>

        // 左边是列引用，右边是常量
        // Left is column reference, right is constant
        if (leftRef != null && rightConst != null) {
            val column = resolveColumn(leftRef.path.value) ?: return null
            val value = rightConst.value ?: return null
            return when (expr.operator) {
                ComparisonOperator.Eq -> (column as ColumnDeclaring<Any>).eq(value)
                ComparisonOperator.Ne -> (column as ColumnDeclaring<Any>).neq(value)
                ComparisonOperator.Lt, ComparisonOperator.Le,
                ComparisonOperator.Gt, ComparisonOperator.Ge -> null
            }
        }

        // 左边是常量，右边是列引用（反转比较）
        // Left is constant, right is column reference (reverse comparison)
        if (leftConst != null && rightRef != null) {
            val column = resolveColumn(rightRef.path.value) ?: return null
            val value = leftConst.value ?: return null
            return when (expr.operator) {
                ComparisonOperator.Eq -> (column as ColumnDeclaring<Any>).eq(value)
                ComparisonOperator.Ne -> (column as ColumnDeclaring<Any>).neq(value)
                ComparisonOperator.Lt, ComparisonOperator.Le,
                ComparisonOperator.Gt, ComparisonOperator.Ge -> null
            }
        }

        return null
    }

    private fun translatePatternMatch(expr: PatternMatch<*>): ColumnDeclaring<Boolean>? {
        val ref = expr.value as? ScalarReference<*> ?: return null
        val column = resolveColumn(ref.path.value) ?: return null

        val patternValue = (expr.pattern as? ScalarConstant<*>)?.value?.toString() ?: return null

        val sqlPattern = when (expr.mode) {
            PatternMatchMode.Exact -> patternValue
            PatternMatchMode.Prefix -> "$patternValue%"
            PatternMatchMode.Suffix -> "%$patternValue"
            PatternMatchMode.Contains -> "%$patternValue%"
            PatternMatchMode.Like -> patternValue
            PatternMatchMode.Regex -> return patternMatchPolicy.translateRegex(column, patternValue)
        }

        val condition = patternMatchPolicy.translateLike(column, sqlPattern, caseSensitive = true)
        return if (expr.negated) condition.not() else condition
    }

    private fun translateNullCheck(expr: NullCheck): ColumnDeclaring<Boolean> {
        val column = resolveColumn(expr.path.value) ?: throw IllegalArgumentException(
            "Cannot resolve path: ${expr.path}"
        )
        return if (expr.isNull) column.isNull() else column.isNotNull()
    }

    private fun translateAnd(expr: AndExpression): ColumnDeclaring<Boolean>? {
        val conditions = expr.operands.mapNotNull { translate(it) }
        if (conditions.isEmpty()) return null
        return conditions.reduce { acc, cond -> acc.and(cond) }
    }

    private fun translateOr(expr: OrExpression): ColumnDeclaring<Boolean>? {
        val conditions = expr.operands.mapNotNull { translate(it) }
        if (conditions.isEmpty()) return null
        return conditions.reduce { acc, cond -> acc.or(cond) }
    }

    private fun translateNot(expr: NotExpression): ColumnDeclaring<Boolean>? {
        val condition = translate(expr.operand) ?: return null
        return condition.not()
    }
}