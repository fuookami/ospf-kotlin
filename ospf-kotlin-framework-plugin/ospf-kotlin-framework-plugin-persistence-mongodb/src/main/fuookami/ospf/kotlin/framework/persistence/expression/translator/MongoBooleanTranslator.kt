/**
 * MongoDB 布尔表达式翻译器
 * MongoDB Boolean Expression Translator
 *
 * 将 BooleanExpression 翻译为 MongoDB Bson 查询条件。
 * Translates BooleanExpression to MongoDB Bson query conditions.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.conversions.Bson
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.*

/**
 * 字段名解析器
 * Field Name Resolver
 */
typealias MongoFieldNameResolver = PersistenceFieldResolver<String>

/**
 * MongoDB 布尔表达式翻译器
 * MongoDB Boolean Expression Translator
 *
 * 将 math.symbol.expression.BooleanExpression 翻译为 MongoDB 查询条件。
 * Translates math.symbol.expression.BooleanExpression to MongoDB query conditions.
 *
 * @property resolveFieldName 字段名解析函数 / Field name resolver function
 * @property unsupportedPredicatePolicy 不支持谓词时的策略 / Policy for unsupported predicates
 */
class MongoBooleanTranslator(
    private val resolveFieldName: MongoFieldNameResolver,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) {
    private val scalarTranslator = MongoScalarTranslator(resolveFieldName, unsupportedPredicatePolicy)

    /**
     * 翻译布尔表达式为 Bson
     * Translate boolean expression to Bson
     *
     * @param expr 布尔表达式 / Boolean expression
     * @return Bson 查询条件，不支持时返回 null / Bson query condition, or null if unsupported
     */
    fun translate(expr: BooleanExpression): Ret<Bson?> {
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

    private fun translateConstant(expr: BooleanConstant): Ret<Bson?> {
        val result: Bson? = when (expr.value) {
            Trivalent.True -> Filters.empty()
            Trivalent.False, Trivalent.Unknown -> alwaysFalse()
        }
        return Ok(result)
    }
    private fun translateComparison(expr: Comparison<*>): Ret<Bson?> {
        val leftRef = expr.left as? ScalarReference<*>
        val leftConst = expr.left as? ScalarConstant<*>
        val rightRef = expr.right as? ScalarReference<*>
        val rightConst = expr.right as? ScalarConstant<*>

        // 左边是列引用，右边是常量
        // Left is column reference, right is constant
        if (leftRef != null && rightConst != null) {
            val field = resolveFieldName(leftRef.path.value)
                ?: return unsupported("Unresolved comparison path: ${leftRef.path.value}", expr)
            val value = rightConst.value
                ?: return unsupported("Null comparison constant is not supported", expr)

            return Ok(when (expr.operator) {
                ComparisonOperator.Eq -> Filters.eq(field, value)
                ComparisonOperator.Ne -> Filters.ne(field, value)
                ComparisonOperator.Lt -> Filters.lt(field, value)
                ComparisonOperator.Le -> Filters.lte(field, value)
                ComparisonOperator.Gt -> Filters.gt(field, value)
                ComparisonOperator.Ge -> Filters.gte(field, value)
            })
        }

        // 左边是常量，右边是列引用（反转比较）
        // Left is constant, right is column reference (reverse comparison)
        if (leftConst != null && rightRef != null) {
            val field = resolveFieldName(rightRef.path.value)
                ?: return unsupported("Unresolved comparison path: ${rightRef.path.value}", expr)
            val value = leftConst.value
                ?: return unsupported("Null comparison constant is not supported", expr)

            return Ok(when (expr.operator) {
                ComparisonOperator.Eq -> Filters.eq(field, value)
                ComparisonOperator.Ne -> Filters.ne(field, value)
                ComparisonOperator.Lt -> Filters.gt(field, value)  // 反转
                ComparisonOperator.Le -> Filters.gte(field, value)  // 反转
                ComparisonOperator.Gt -> Filters.lt(field, value)  // 反转
                ComparisonOperator.Ge -> Filters.lte(field, value)  // 反转
            })
        }

        val left = scalarTranslator.translate(expr.left).value
            ?: return unsupported("Unsupported left scalar expression: ${expr.left.typeName}", expr)
        val right = scalarTranslator.translate(expr.right).value
            ?: return unsupported("Unsupported right scalar expression: ${expr.right.typeName}", expr)
        return Ok(Document("\$expr", Document(exprOperator(expr.operator), listOf(left, right))))
    }
    private fun translateIn(expr: InExpression<*>): Ret<Bson?> {
        val ref = expr.value as? ScalarReference<*>
            ?: return unsupported("IN value must be a field reference", expr)
        val field = resolveFieldName(ref.path.value)
            ?: return unsupported("Unresolved IN path: ${ref.path.value}", expr)

        val values = expr.candidates.mapNotNull { (it as? ScalarConstant<*>)?.value }
        if (values.size != expr.candidates.size || values.isEmpty()) {
            return unsupported("IN candidates must be non-empty scalar constants", expr)
        }

        return Ok(if (expr.negated) {
            Filters.nin(field, values)
        } else {
            Filters.`in`(field, values)
        })
    }
    private fun translatePatternMatch(expr: PatternMatch<*>): Ret<Bson?> {
        val ref = expr.value as? ScalarReference<*>
            ?: return unsupported("Pattern value must be a field reference", expr)
        val field = resolveFieldName(ref.path.value)
            ?: return unsupported("Unresolved pattern path: ${ref.path.value}", expr)

        val patternValue = (expr.pattern as? ScalarConstant<*>)?.value?.toString()
            ?: return unsupported("Pattern must be a scalar constant", expr)

        val regexPattern = when (expr.mode) {
            PatternMatchMode.Exact -> "^${escapeRegex(patternValue)}$"
            PatternMatchMode.Prefix -> "^${escapeRegex(patternValue)}"
            PatternMatchMode.Suffix -> "${escapeRegex(patternValue)}$"
            PatternMatchMode.Contains -> escapeRegex(patternValue)
            PatternMatchMode.Like -> translateLikeToRegex(patternValue)
            PatternMatchMode.Regex -> patternValue
        }

        return Ok(if (expr.negated) {
            Filters.not(Filters.regex(field, regexPattern))
        } else {
            Filters.regex(field, regexPattern)
        })
    }

    private fun translateNullCheck(expr: NullCheck): Ret<Bson?> {
        val field = resolveFieldName(expr.path.value)
            ?: return unsupported("Unresolved null-check path: ${expr.path.value}", expr)

        return Ok(if (expr.isNull) {
            Filters.or(
                Filters.eq(field, null),
                Filters.exists(field, false)
            )
        } else {
            Filters.and(
                Filters.exists(field, true),
                Filters.ne(field, null)
            )
        })
    }

    private fun translateAnd(expr: AndExpression): Ret<Bson?> {
        val conditions = expr.operands.map { translate(it).value ?: alwaysFalse() }
        return Ok(Filters.and(conditions))
    }

    private fun translateOr(expr: OrExpression): Ret<Bson?> {
        val conditions = expr.operands.map { translate(it).value ?: alwaysFalse() }
        return Ok(Filters.or(conditions))
    }

    private fun translateNot(expr: NotExpression): Ret<Bson?> {
        val condition = translate(expr.operand).value ?: return unsupported("Unsupported NOT operand", expr)
        return Ok(Filters.not(condition))
    }

    private fun exprOperator(operator: ComparisonOperator): String {
        return when (operator) {
            ComparisonOperator.Eq -> "\$eq"
            ComparisonOperator.Ne -> "\$ne"
            ComparisonOperator.Lt -> "\$lt"
            ComparisonOperator.Le -> "\$lte"
            ComparisonOperator.Gt -> "\$gt"
            ComparisonOperator.Ge -> "\$gte"
        }
    }

    private fun unsupported(reason: String, expression: BooleanExpression): Ret<Bson> {
        return when (unsupportedPredicatePolicy) {
            UnsupportedPredicatePolicy.FailFast -> Failed(
                ErrorCode.IllegalArgument,
                "Unsupported predicate ${expression.typeName}: $reason"
            )
            UnsupportedPredicatePolicy.AlwaysFalse -> Ok(alwaysFalse())
            UnsupportedPredicatePolicy.ClientFilter -> Failed(
                ErrorCode.IllegalArgument,
                "ClientFilter is not implemented for unsupported predicate ${expression.typeName}: $reason"
            )
        }
    }

    /**
     * 生成恒假条件，避免 unsupported 表达式被误解释为“无条件”。
     * Build an always-false filter to avoid unsupported expressions becoming "no filter".
     */
    private fun alwaysFalse(): Bson {
        return Filters.exists("_id", false)
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
