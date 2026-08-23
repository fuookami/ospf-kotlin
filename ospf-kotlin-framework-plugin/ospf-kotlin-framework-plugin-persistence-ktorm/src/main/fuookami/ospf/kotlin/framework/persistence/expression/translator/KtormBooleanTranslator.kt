/**
 * Ktorm 布尔表达式翻译器 / Ktorm Boolean Expression Translator
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
import org.ktorm.schema.VarcharSqlType
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 列名解析器 / Column Name Resolver
 *
 * 将 PropertyPath 解析为 Ktorm Column。
 * Resolves PropertyPath to Ktorm Column.
*/
typealias KtormColumnResolver = PersistenceFieldResolver<ColumnDeclaring<*>>

/**
 * Ktorm 布尔表达式翻译器 / Ktorm Boolean Expression Translator
 *
 * 将 math.symbol.expression.BooleanExpression 翻译为 Ktorm 查询条件。
 * Translates math.symbol.expression.BooleanExpression to Ktorm query conditions.
 *
 * @property resolveColumn 列解析函数 / Column resolver function
 * @property patternMatchPolicy 模式匹配策略 / Pattern match policy
 * @property unsupportedPredicatePolicy 不支持谓词时的策略 / Policy for unsupported predicates
 * @property targetConstantBinder 目标 SQL 类型感知的常量绑定器 / Target-SQL-type-aware constant binder
*/
class KtormBooleanTranslator(
    private val resolveColumn: KtormColumnResolver,
    private val patternMatchPolicy: PatternMatchPolicy = DefaultPatternMatchPolicy,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse,
    private val parameterTypeSink: ((SqlType<*>) -> Unit)? = null,
    private val resolveColumnDetailed: ((String) -> PersistenceFieldResolution<ColumnDeclaring<*>>)? = null,
    private val targetConstantBinder: KtormTargetConstantBinder? = null
) {
    private val scalarTranslator = KtormScalarTranslator(
        resolveColumn = resolveColumn,
        unsupportedPredicatePolicy = unsupportedPredicatePolicy,
        parameterTypeSink = parameterTypeSink,
        resolveColumnDetailed = resolveColumnDetailed,
        targetConstantBinder = targetConstantBinder
    )

    /**
     * 翻译布尔表达式为 Ktorm 条件 / Translate boolean expression to Ktorm condition
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
     * 翻译常量布尔表达式 / Translate constant boolean expression
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
     * 翻译比较表达式为 Ktorm 二元比较 / Translate comparison expression to Ktorm binary comparison
     *
     * @param expr 比较表达式 / Comparison expression
     * @return Ktorm 比较条件 / Ktorm comparison condition
    */
    private fun translateComparison(expr: Comparison<*>): Ret<ColumnDeclaring<Boolean>?> {
        val leftConstant = expr.left as? ScalarConstant<*>
        val rightConstant = expr.right as? ScalarConstant<*>
        if (leftConstant != null && rightConstant != null) {
            val leftResult = scalarTranslator.translateConstant(leftConstant.value)
            if (leftResult.failed) return propagateScalarFailure(leftResult)
            val left = leftResult.value
                ?: return unsupported("Unsupported left scalar constant: ${leftConstant.typeName}", expr)
            val rightResult = scalarTranslator.translateConstant(rightConstant.value)
            if (rightResult.failed) return propagateScalarFailure(rightResult)
            val right = rightResult.value
                ?: return unsupported("Unsupported right scalar constant: ${rightConstant.typeName}", expr)
            if (!compatibleSqlTypes(left.sqlType, right.sqlType)) {
                return unsupported("Constant comparison operands have incompatible SQL types", expr)
            }
            return Ok(buildComparison(left, right, expr.operator))
        }
        val translatedLeft = if (leftConstant == null) {
            val translated = scalarTranslator.translate(expr.left)
            if (translated.failed) return propagateScalarFailure(translated)
            translated.value
                ?: return unsupported("Unsupported left scalar expression: ${expr.left.typeName}", expr)
        } else {
            null
        }
        val translatedRight = if (rightConstant == null) {
            val translated = scalarTranslator.translate(expr.right)
            if (translated.failed) return propagateScalarFailure(translated)
            translated.value
                ?: return unsupported("Unsupported right scalar expression: ${expr.right.typeName}", expr)
        } else {
            null
        }
        val left = if (leftConstant != null) {
            val targetType = translatedRight?.sqlType
                ?: return unsupported("A constant comparison operand requires a translated right operand", expr)
            val translated = scalarTranslator.translateConstant(leftConstant.value, targetType)
            if (translated.failed) return propagateScalarFailure(translated)
            translated.value
                ?: return unsupported("Unsupported left scalar constant: ${leftConstant.typeName}", expr)
        } else {
            translatedLeft ?: return unsupported("Missing translated left operand", expr)
        }
        val right = if (rightConstant != null) {
            val targetType = translatedLeft?.sqlType
                ?: return unsupported("A constant comparison operand requires a translated left operand", expr)
            val translated = scalarTranslator.translateConstant(rightConstant.value, targetType)
            if (translated.failed) return propagateScalarFailure(translated)
            translated.value
                ?: return unsupported("Unsupported right scalar constant: ${rightConstant.typeName}", expr)
        } else {
            translatedRight ?: return unsupported("Missing translated right operand", expr)
        }
        if (leftConstant == null && rightConstant == null && !compatibleSqlTypes(left.sqlType, right.sqlType)) {
            return unsupported("Comparison operands have incompatible SQL types", expr)
        }
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
            ?: return unsupported(resolveFailure(ref.path.value), expr)
        val values = expr.candidates.mapNotNull { (it as? ScalarConstant<*>)?.value }
        if (values.size != expr.candidates.size || values.isEmpty()) {
            return unsupported("IN candidates must be non-empty scalar constants", expr)
        }

        val inValues = values.map { value ->
            val translated = scalarTranslator.translateConstant(value, column.sqlType)
            if (translated.failed) return propagateScalarFailure(translated)
            translated.value ?: return unsupported("Unsupported IN scalar constant", expr)
        }
        return Ok(InListExpression(
            left = column.asExpression(),
            values = inValues,
            notInList = expr.negated
        ))
    }

    /**
     * 翻译模式匹配表达式为 LIKE 或正则条件 / Translate pattern match expression to LIKE or regex condition
     *
     * @param expr 模式匹配表达式 / Pattern match expression
     * @return Ktorm 模式匹配条件 / Ktorm pattern match condition
    */
    private fun translatePatternMatch(expr: PatternMatch<*>): Ret<ColumnDeclaring<Boolean>?> {
        val ref = expr.value as? ScalarReference<*>
            ?: return unsupported("Pattern value must be a column reference", expr)
        val column = resolveColumn(ref.path.value)
            ?: return unsupported(resolveFailure(ref.path.value), expr)

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

        parameterTypeSink?.invoke(VarcharSqlType)
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
            ?: return unsupported(resolveFailure(expr.path.value), expr)
        return Ok(if (expr.isNull) column.isNull() else column.isNotNull())
    }

    /**
     * 翻译 AND 逻辑表达式为 Ktorm AND 组合条件 / Translate AND logical expression to Ktorm AND combined condition
     *
     * @param expr AND 表达式 / AND expression
     * @return Ktorm AND 条件 / Ktorm AND condition
    */
    private fun translateAnd(expr: AndExpression): Ret<ColumnDeclaring<Boolean>?> {
        val conditions = mutableListOf<ColumnDeclaring<Boolean>>()
        expr.operands.forEach { operand ->
            val translated = translate(operand)
            if (translated.failed) return propagateBooleanFailure(translated)
            conditions += translated.value ?: return unsupported("Unsupported AND operand", expr)
        }
        return Ok(conditions.reduceOrNull { acc, condition -> acc.and(condition) } ?: alwaysTrue())
    }

    /**
     * 翻译 OR 逻辑表达式为 Ktorm OR 组合条件 / Translate OR logical expression to Ktorm OR combined condition
     *
     * @param expr OR 表达式 / OR expression
     * @return Ktorm OR 条件 / Ktorm OR condition
    */
    private fun translateOr(expr: OrExpression): Ret<ColumnDeclaring<Boolean>?> {
        val conditions = mutableListOf<ColumnDeclaring<Boolean>>()
        expr.operands.forEach { operand ->
            val translated = translate(operand)
            if (translated.failed) return propagateBooleanFailure(translated)
            conditions += translated.value ?: return unsupported("Unsupported OR operand", expr)
        }
        return Ok(conditions.reduceOrNull { acc, condition -> acc.or(condition) } ?: alwaysFalse())
    }

    /**
     * 翻译 NOT 逻辑表达式为 Ktorm NOT 条件
     * Translate NOT logical expression to Ktorm NOT condition
     *
     * @param expr NOT 表达式 / NOT expression
     * @return Ktorm NOT 条件 / Ktorm NOT condition
    */
    private fun translateNot(expr: NotExpression): Ret<ColumnDeclaring<Boolean>?> {
        val translated = translate(expr.operand)
        if (translated.failed) return propagateBooleanFailure(translated)
        val condition = translated.value ?: return unsupported("Unsupported NOT operand", expr)
        return Ok(condition.not())
    }

    /**
     * 构建 Ktorm 二元比较表达式 / Build Ktorm binary comparison expression
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

/**
 * unsupported.
 * unsupported。
 * @param reason 不支持该表达式的原因 / Reason why the expression is unsupported
 * @param expression 不支持的布尔表达式 / The unsupported boolean expression
 * @return 根据不支持谓词策略返回的结果 / Result based on unsupported predicate policy
*/
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

    private fun resolveFailure(path: String): String {
        return when (val result = resolveColumnDetailed?.invoke(path)) {
            is PersistenceFieldResolution.Ambiguous -> {
                "Ambiguous path $path: ${result.candidates.joinToString(", ")}"
            }
            is PersistenceFieldResolution.Missing -> "Unresolved path: $path"
            is PersistenceFieldResolution.InvalidConfiguration -> result.reason
            is PersistenceFieldResolution.Resolved, null -> "Unresolved path: $path"
        }
    }

    private fun compatibleSqlTypes(left: SqlType<*>, right: SqlType<*>): Boolean {
        return left.typeCode == right.typeCode && left.typeName == right.typeName
    }

    private fun propagateScalarFailure(
        result: Ret<ScalarExpression<*>?>
    ): Ret<ColumnDeclaring<Boolean>?> {
        @Suppress("UNCHECKED_CAST")
        val failed = result as Failed<ScalarExpression<*>?, ErrorCode, Error<ErrorCode>>
        return Failed(failed.error)
    }

    private fun propagateBooleanFailure(
        result: Ret<ColumnDeclaring<Boolean>?>
    ): Ret<ColumnDeclaring<Boolean>?> {
        @Suppress("UNCHECKED_CAST")
        val failed = result as Failed<ColumnDeclaring<Boolean>?, ErrorCode, Error<ErrorCode>>
        return Failed(failed.error)
    }

/**
 * alwaysFalse.
 * alwaysFalse。
 * @return 恒为假的 Ktorm 表达式 / A Ktorm expression that always evaluates to false
*/
    private fun alwaysFalse(): ColumnDeclaring<Boolean> {
        return BinaryExpression(
            type = BinaryExpressionType.EQUAL,
            left = ArgumentExpression(1, IntSqlType),
            right = ArgumentExpression(0, IntSqlType),
            sqlType = BooleanSqlType
        )
    }

/**
 * alwaysTrue.
 * alwaysTrue。
 * @return 恒为真的 Ktorm 表达式 / A Ktorm expression that always evaluates to true
*/
    private fun alwaysTrue(): ColumnDeclaring<Boolean> {
        return BinaryExpression(
            type = BinaryExpressionType.EQUAL,
            left = ArgumentExpression(1, IntSqlType),
            right = ArgumentExpression(1, IntSqlType),
            sqlType = BooleanSqlType
        )
    }
}
