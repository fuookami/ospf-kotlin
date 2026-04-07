/**
 * MongoDB 布尔表达式翻译器
 * MongoDB Boolean Expression Translator
 *
 * 将 BooleanExpression 翻译为 MongoDB Bson 查询条件。
 * Translates BooleanExpression to MongoDB Bson query conditions.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import org.bson.conversions.Bson
import com.mongodb.client.model.Filters

/**
 * 字段名解析器
 * Field Name Resolver
 */
typealias MongoFieldNameResolver = (String) -> String?

/**
 * MongoDB 布尔表达式翻译器
 * MongoDB Boolean Expression Translator
 *
 * 将 math.symbol.expression.BooleanExpression 翻译为 MongoDB 查询条件。
 * Translates math.symbol.expression.BooleanExpression to MongoDB query conditions.
 *
 * @property resolveFieldName 字段名解析函数 / Field name resolver function
 */
class MongoBooleanTranslator(
    private val resolveFieldName: MongoFieldNameResolver
) {
    /**
     * 翻译布尔表达式为 Bson
     * Translate boolean expression to Bson
     */
    fun translate(expr: BooleanExpression): Bson? {
        return when (expr) {
            is BooleanConstant -> translateConstant(expr)
            is Comparison<*> -> translateComparison(expr)
            is InExpression<*> -> translateIn(expr)
            is PatternMatch<*> -> translatePatternMatch(expr)
            is NullCheck -> translateNullCheck(expr)
            is AndExpression -> translateAnd(expr)
            is OrExpression -> translateOr(expr)
            is NotExpression -> translateNot(expr)
            is BooleanCustom -> null
        }
    }

    private fun translateConstant(expr: BooleanConstant): Bson? {
        return when (expr.value) {
            Trivalent.True -> Filters.expr(true)
            Trivalent.False -> Filters.expr(false)
            Trivalent.Unknown -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun translateComparison(expr: Comparison<*>): Bson? {
        val leftRef = expr.left as? ScalarReference<*>
        val leftConst = expr.left as? ScalarConstant<*>
        val rightRef = expr.right as? ScalarReference<*>
        val rightConst = expr.right as? ScalarConstant<*>

        // 左边是列引用，右边是常量
        // Left is column reference, right is constant
        if (leftRef != null && rightConst != null) {
            val field = resolveFieldName(leftRef.path.value) ?: return null
            val value = rightConst.value ?: return null

            return when (expr.operator) {
                ComparisonOperator.Eq -> Filters.eq(field, value)
                ComparisonOperator.Ne -> Filters.ne(field, value)
                ComparisonOperator.Lt -> Filters.lt(field, value)
                ComparisonOperator.Le -> Filters.lte(field, value)
                ComparisonOperator.Gt -> Filters.gt(field, value)
                ComparisonOperator.Ge -> Filters.gte(field, value)
            }
        }

        // 左边是常量，右边是列引用（反转比较）
        // Left is constant, right is column reference (reverse comparison)
        if (leftConst != null && rightRef != null) {
            val field = resolveFieldName(rightRef.path.value) ?: return null
            val value = leftConst.value ?: return null

            return when (expr.operator) {
                ComparisonOperator.Eq -> Filters.eq(field, value)
                ComparisonOperator.Ne -> Filters.ne(field, value)
                ComparisonOperator.Lt -> Filters.gt(field, value)  // 反转
                ComparisonOperator.Le -> Filters.gte(field, value)  // 反转
                ComparisonOperator.Gt -> Filters.lt(field, value)  // 反转
                ComparisonOperator.Ge -> Filters.lte(field, value)  // 反转
            }
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun translateIn(expr: InExpression<*>): Bson? {
        val ref = expr.value as? ScalarReference<*> ?: return null
        val field = resolveFieldName(ref.path.value) ?: return null

        val values = expr.candidates.mapNotNull { (it as? ScalarConstant<*>)?.value }
        if (values.isEmpty()) return null

        return if (expr.negated) {
            Filters.nin(field, values)
        } else {
            Filters.`in`(field, values)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun translatePatternMatch(expr: PatternMatch<*>): Bson? {
        val ref = expr.value as? ScalarReference<*> ?: return null
        val field = resolveFieldName(ref.path.value) ?: return null

        val patternValue = (expr.pattern as? ScalarConstant<*>)?.value?.toString() ?: return null

        val regexPattern = when (expr.mode) {
            PatternMatchMode.Exact -> "^${escapeRegex(patternValue)}$"
            PatternMatchMode.Prefix -> "^${escapeRegex(patternValue)}"
            PatternMatchMode.Suffix -> "${escapeRegex(patternValue)}$"
            PatternMatchMode.Contains -> escapeRegex(patternValue)
            PatternMatchMode.Like -> translateLikeToRegex(patternValue)
            PatternMatchMode.Regex -> patternValue
        }

        return if (expr.negated) {
            Filters.not(Filters.regex(field, regexPattern))
        } else {
            Filters.regex(field, regexPattern)
        }
    }

    private fun translateNullCheck(expr: NullCheck): Bson? {
        val field = resolveFieldName(expr.path.value) ?: return null

        return if (expr.isNull) {
            Filters.exists(field, false)
        } else {
            Filters.exists(field, true)
        }
    }

    private fun translateAnd(expr: AndExpression): Bson? {
        val conditions = expr.operands.mapNotNull { translate(it) }
        if (conditions.isEmpty()) return null
        return Filters.and(conditions)
    }

    private fun translateOr(expr: OrExpression): Bson? {
        val conditions = expr.operands.mapNotNull { translate(it) }
        if (conditions.isEmpty()) return null
        return Filters.or(conditions)
    }

    private fun translateNot(expr: NotExpression): Bson? {
        val condition = translate(expr.operand) ?: return null
        return Filters.not(condition)
    }

    /**
     * 转义正则特殊字符
     * Escape regex special characters
     */
    private fun escapeRegex(pattern: String): String {
        return pattern.replace(Regex("[\\[\\]{}()*+?.\\\\^$|]")) { "\\${it.value}" }
    }

    /**
     * 将 SQL LIKE 模式转换为正则表达式
     * Convert SQL LIKE pattern to regex
     */
    private fun translateLikeToRegex(pattern: String): String {
        val escaped = escapeRegex(pattern)
        return escaped
            .replace("%", ".*")
            .replace("_", ".")
    }
}