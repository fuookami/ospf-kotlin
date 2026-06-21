/**
 * Ktorm 布尔表达式翻译器
 * Ktorm Boolean Expression Translator
 *
 * 将 BooleanExpression 翻译为 Ktorm ColumnDeclaring<Boolean>。
 * Translates BooleanExpression to Ktorm ColumnDeclaring<Boolean>.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.ktorm.dsl.*
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.BinaryExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.InListExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.SqlType
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.*

/**
 * 列名解析器
 * Column Name Resolver
 *
 * 将 PropertyPath 解析为 Ktorm Column。
 * Resolves PropertyPath to Ktorm Column.
 */
typealias KtormColumnResolver = PersistenceFieldResolver<ColumnDeclaring<*>>

/**
 * Ktorm 布尔表达式翻译器
 * Ktorm Boolean Expression Translator
 *
 * 将 math.symbol.expression.BooleanExpression 翻译为 Ktorm 查询条件。
 * Translates math.symbol.expression.BooleanExpression to Ktorm query conditions.
 *
 * @property resolveColumn 列解析函数 / Column resolver function
 * @property patternMatchPolicy 模式匹配策略 / Pattern match policy
 * @property unsupportedPredicatePolicy 不支持谓词时的策略 / Policy for unsupported predicates
 */
class KtormBooleanTranslator(
    private val resolveColumn: KtormColumnResolver,
    private val patternMatchPolicy: PatternMatchPolicy = DefaultPatternMatchPolicy,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) {
    private val scalarTranslator = KtormScalarTranslator(resolveColumn, unsupportedPredicatePolicy)

    /**
     * 翻译布尔表达式为 Ktorm 条件
     * Translate boolean expression to Ktorm condition
     *
     * @param expr 布尔表达式 / Boolean expression
     * @return Ktorm 条件表达式，不支持时返回 null / Ktorm condition expression, or null if unsupported
     */
    fun translate(expr: BooleanExpression): Ret<ColumnDeclaring<Boolean>?> {
        return when (expr) {
            is BooleanConstant -> translateConstant(expr)
            is Comparison<*> -> translateComparison(expr)
            is InExpression<*> -> translateIn(expr)
            is PatternMatch<*> -> translatePatternMatch(expr)
            is NullCheck -> translateNullCheck(expr)
            is AndExpression -> translateAnd(expr)
            is OrExpression -> translateOr(expr)
            is NotExpression -> translateNot(expr)
            is BooleanCustom -> unsupported("BooleanCustom is not supported", expr)
        }
    }

    private fun translateConstant(expr: BooleanConstant): Ret<ColumnDeclaring<Boolean>?> {
        return Ok(when (expr.value) {
            Trivalent.True -> alwaysTrue()
            Trivalent.False, Trivalent.Unknown -> alwaysFalse()
        })
    }

    private fun translateComparison(expr: Comparison<*>): Ret<ColumnDeclaring<Boolean>?> {
        val left = scalarTranslator.translate(expr.left).value
            ?: return unsupported("Unsupported left scalar expression: ${expr.left.typeName}", expr)
        val right = scalarTranslator.translate(expr.right).value
            ?: return unsupported("Unsupported right scalar expression: ${expr.right.typeName}", expr)
        return Ok(buildComparison(left, right, expr.operator))
    }

    private fun translateIn(expr: InExpression<*>): Ret<ColumnDeclaring<Boolean>?> {
        val ref = expr.value as? ScalarReference<*>
            ?: return unsupported("IN value must be a column reference", expr)
        val column = resolveColumn(ref.path.value)
            ?: return unsupported("Unresolved IN path: ${ref.path.value}", expr)
        val values = expr.candidates.mapNotNull { (it as? ScalarConstant<*>)?.value }
        if (values.size != expr.candidates.size || values.isEmpty()) {
            return unsupported("IN candidates must be non-empty scalar constants", expr)
        }

        @Suppress("UNCHECKED_CAST")
        val sqlType = column.sqlType as SqlType<Any>
        val inValues = values.map { value ->
            ArgumentExpression(value as Any, sqlType) as ScalarExpression<*>
        }
        return Ok(InListExpression(
            left = column.asExpression(),
            values = inValues,
            notInList = expr.negated
        ))
    }

    private fun translatePatternMatch(expr: PatternMatch<*>): Ret<ColumnDeclaring<Boolean>?> {
        val ref = expr.value as? ScalarReference<*>
            ?: return unsupported("Pattern value must be a column reference", expr)
        val column = resolveColumn(ref.path.value)
            ?: return unsupported("Unresolved pattern path: ${ref.path.value}", expr)

        val patternValue = (expr.pattern as? ScalarConstant<*>)?.value?.toString()
            ?: return unsupported("Pattern must be a scalar constant", expr)

        val sqlPattern = when (expr.mode) {
            PatternMatchMode.Exact -> patternValue
            PatternMatchMode.Prefix -> "$patternValue%"
            PatternMatchMode.Suffix -> "%$patternValue"
            PatternMatchMode.Contains -> "%$patternValue%"
            PatternMatchMode.Like -> patternValue
            PatternMatchMode.Regex -> {
                val result = patternMatchPolicy.translateRegex(column, patternValue)
                    ?: return unsupported("Regex pattern is not supported by current Ktorm policy", expr)
                return Ok(result)
            }
        }

        val condition = patternMatchPolicy.translateLike(column, sqlPattern, caseSensitive = true)
        return Ok(if (expr.negated) condition.not() else condition)
    }

    private fun translateNullCheck(expr: NullCheck): Ret<ColumnDeclaring<Boolean>?> {
        val column = resolveColumn(expr.path.value)
            ?: return unsupported("Unresolved null-check path: ${expr.path.value}", expr)
        return Ok(if (expr.isNull) column.isNull() else column.isNotNull())
    }

    private fun translateAnd(expr: AndExpression): Ret<ColumnDeclaring<Boolean>?> {
        val conditions = expr.operands.map { translate(it).value ?: alwaysFalse() }
        return Ok(conditions.reduce { acc, cond -> acc.and(cond) })
    }

    private fun translateOr(expr: OrExpression): Ret<ColumnDeclaring<Boolean>?> {
        val conditions = expr.operands.map { translate(it).value ?: alwaysFalse() }
        return Ok(conditions.reduce { acc, cond -> acc.or(cond) })
    }

    private fun translateNot(expr: NotExpression): Ret<ColumnDeclaring<Boolean>?> {
        val condition = translate(expr.operand).value ?: return unsupported("Unsupported NOT operand", expr)
        return Ok(condition.not())
    }

    private fun buildComparison(
        left: ScalarExpression<*>,
        right: ScalarExpression<*>,
        operator: ComparisonOperator
    ): ColumnDeclaring<Boolean> {
        return BinaryExpression(
            type = when (operator) {
                ComparisonOperator.Eq -> BinaryExpressionType.EQUAL
                ComparisonOperator.Ne -> BinaryExpressionType.NOT_EQUAL
                ComparisonOperator.Lt -> BinaryExpressionType.LESS_THAN
                ComparisonOperator.Le -> BinaryExpressionType.LESS_THAN_OR_EQUAL
                ComparisonOperator.Gt -> BinaryExpressionType.GREATER_THAN
                ComparisonOperator.Ge -> BinaryExpressionType.GREATER_THAN_OR_EQUAL
            },
            left = left,
            right = right,
            sqlType = BooleanSqlType
        )
    }

    private fun unsupported(reason: String, expression: BooleanExpression): Ret<ColumnDeclaring<Boolean>?> {
        return when (unsupportedPredicatePolicy) {
            UnsupportedPredicatePolicy.FailFast -> {
                val detail = UnsupportedPredicateDetail.failFast(
                    expressionType = expression.typeName,
                    reason = reason,
                    backendName = "Ktorm"
                )
                Failed(detail.toError())
            }
            UnsupportedPredicatePolicy.AlwaysFalse -> Ok(alwaysFalse())
            UnsupportedPredicatePolicy.ClientFilter -> {
                val detail = UnsupportedPredicateDetail.clientFilter(
                    expressionType = expression.typeName,
                    reason = reason,
                    backendName = "Ktorm"
                )
                Failed(detail.toError())
            }
        }
    }

    private fun alwaysFalse(): ColumnDeclaring<Boolean> {
        return BinaryExpression(
            type = BinaryExpressionType.EQUAL,
            left = ArgumentExpression(1, IntSqlType),
            right = ArgumentExpression(0, IntSqlType),
            sqlType = BooleanSqlType
        )
    }

    private fun alwaysTrue(): ColumnDeclaring<Boolean> {
        return BinaryExpression(
            type = BinaryExpressionType.EQUAL,
            left = ArgumentExpression(1, IntSqlType),
            right = ArgumentExpression(1, IntSqlType),
            sqlType = BooleanSqlType
        )
    }
}
