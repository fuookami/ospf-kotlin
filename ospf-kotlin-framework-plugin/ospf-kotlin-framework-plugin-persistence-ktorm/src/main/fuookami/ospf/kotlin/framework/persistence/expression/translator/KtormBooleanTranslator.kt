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
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

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

    /**
     * 翻译常量布尔表达式
     * Translate constant boolean expression
     *
     * @param expr 常量布尔表达式 / Constant boolean expression
     * @return 恒真或恒假条件 / Always-true or always-false condition
     */
    private fun translateConstant(expr: BooleanConstant): Ret<ColumnDeclaring<Boolean>?> {
        return Ok(when (expr.value) {
            Trivalent.True -> alwaysTrue()
            Trivalent.False, Trivalent.Unknown -> alwaysFalse()
        })
    }

    /**
     * 翻译比较表达式为 Ktorm 二元比较
     * Translate comparison expression to Ktorm binary comparison
     *
     * @param expr 比较表达式 / Comparison expression
     * @return Ktorm 比较条件 / Ktorm comparison condition
     */
    private fun translateComparison(expr: Comparison<*>): Ret<ColumnDeclaring<Boolean>?> {
        val left = scalarTranslator.translate(expr.left).value
            ?: return unsupported("Unsupported left scalar expression: ${expr.left.typeName}", expr)
        val right = scalarTranslator.translate(expr.right).value
            ?: return unsupported("Unsupported right scalar expression: ${expr.right.typeName}", expr)
        return Ok(buildComparison(left, right, expr.operator))
    }

    /**
     * 翻译 IN 表达式为 Ktorm InListExpression
     * Translate IN expression to Ktorm InListExpression
     *
     * @param expr IN 表达式 / IN expression
     * @return Ktorm IN 列表条件 / Ktorm IN list condition
     */
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

    /**
     * 翻译模式匹配表达式为 LIKE 或正则条件
     * Translate pattern match expression to LIKE or regex condition
     *
     * @param expr 模式匹配表达式 / Pattern match expression
     * @return Ktorm 模式匹配条件 / Ktorm pattern match condition
     */
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

    /**
     * 翻译空值检查表达式为 IS NULL / IS NOT NULL
     * Translate null check expression to IS NULL / IS NOT NULL
     *
     * @param expr 空值检查表达式 / Null check expression
     * @return Ktorm 空值检查条件 / Ktorm null check condition
     */
    private fun translateNullCheck(expr: NullCheck): Ret<ColumnDeclaring<Boolean>?> {
        val column = resolveColumn(expr.path.value)
            ?: return unsupported("Unresolved null-check path: ${expr.path.value}", expr)
        return Ok(if (expr.isNull) column.isNull() else column.isNotNull())
    }

    /**
     * 翻译 AND 逻辑表达式为 Ktorm AND 组合条件
     * Translate AND logical expression to Ktorm AND combined condition
     *
     * @param expr AND 表达式 / AND expression
     * @return Ktorm AND 条件 / Ktorm AND condition
     */
    private fun translateAnd(expr: AndExpression): Ret<ColumnDeclaring<Boolean>?> {
        val conditions = expr.operands.map { translate(it).value ?: alwaysFalse() }
        return Ok(conditions.reduce { acc, cond -> acc.and(cond) })
    }

    /**
     * 翻译 OR 逻辑表达式为 Ktorm OR 组合条件
     * Translate OR logical expression to Ktorm OR combined condition
     *
     * @param expr OR 表达式 / OR expression
     * @return Ktorm OR 条件 / Ktorm OR condition
     */
    private fun translateOr(expr: OrExpression): Ret<ColumnDeclaring<Boolean>?> {
        val conditions = expr.operands.map { translate(it).value ?: alwaysFalse() }
        return Ok(conditions.reduce { acc, cond -> acc.or(cond) })
    }

    /**
     * 翻译 NOT 逻辑表达式为 Ktorm NOT 条件
     * Translate NOT logical expression to Ktorm NOT condition
     *
     * @param expr NOT 表达式 / NOT expression
     * @return Ktorm NOT 条件 / Ktorm NOT condition
     */
    private fun translateNot(expr: NotExpression): Ret<ColumnDeclaring<Boolean>?> {
        val condition = translate(expr.operand).value ?: return unsupported("Unsupported NOT operand", expr)
        return Ok(condition.not())
    }

    /**
     * 构建 Ktorm 二元比较表达式
     * Build Ktorm binary comparison expression
     *
     * @param left 左操作数标量表达式 / Left operand scalar expression
     * @param right 右操作数标量表达式 / Right operand scalar expression
     * @param operator 比较操作符 / Comparison operator
     * @return Ktorm 布尔比较表达式 / Ktorm boolean comparison expression
     */
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
