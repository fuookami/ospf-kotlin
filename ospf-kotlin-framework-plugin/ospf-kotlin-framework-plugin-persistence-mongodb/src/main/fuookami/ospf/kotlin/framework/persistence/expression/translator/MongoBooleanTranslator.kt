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
    /**
     * 标量表达式翻译器实例
     * Scalar expression translator instance
     */
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

    /**
     * 翻译布尔常量表达式
     * Translate boolean constant expression
     *
     * @param expr 布尔常量表达式 / Boolean constant expression
     * @return Bson 查询条件 / Bson query condition
     */
    private fun translateConstant(expr: BooleanConstant): Ret<Bson?> {
        val result: Bson? = when (expr.value) {
            Trivalent.True -> Filters.empty()
            Trivalent.False, Trivalent.Unknown -> alwaysFalse()
        }
        return Ok(result)
    }
    /**
     * 翻译比较表达式
     * Translate comparison expression
     *
     * @param expr 比较表达式 / Comparison expression
     * @return Bson 查询条件 / Bson query condition
     */
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
    /**
     * 翻译 IN 表达式
     * Translate IN expression
     *
     * @param expr IN 表达式 / IN expression
     * @return Bson 查询条件 / Bson query condition
     */
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
    /**
     * 翻译模式匹配表达式
     * Translate pattern match expression
     *
     * @param expr 模式匹配表达式 / Pattern match expression
     * @return Bson 查询条件 / Bson query condition
     */
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

    /**
     * 翻译空值检查表达式
     * Translate null check expression
     *
     * @param expr 空值检查表达式 / Null check expression
     * @return Bson 查询条件 / Bson query condition
     */
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

    /**
     * 翻译逻辑与表达式
     * Translate logical AND expression
     *
     * @param expr 逻辑与表达式 / Logical AND expression
     * @return Bson 查询条件 / Bson query condition
     */
    private fun translateAnd(expr: AndExpression): Ret<Bson?> {
        val conditions = expr.operands.map { translate(it).value ?: alwaysFalse() }
        return Ok(Filters.and(conditions))
    }

    /**
     * 翻译逻辑或表达式
     * Translate logical OR expression
     *
     * @param expr 逻辑或表达式 / Logical OR expression
     * @return Bson 查询条件 / Bson query condition
     */
    private fun translateOr(expr: OrExpression): Ret<Bson?> {
        val conditions = expr.operands.map { translate(it).value ?: alwaysFalse() }
        return Ok(Filters.or(conditions))
    }

    /**
     * 翻译逻辑非表达式
     * Translate logical NOT expression
     *
     * @param expr 逻辑非表达式 / Logical NOT expression
     * @return Bson 查询条件 / Bson query condition
     */
    private fun translateNot(expr: NotExpression): Ret<Bson?> {
        val condition = translate(expr.operand).value ?: return unsupported("Unsupported NOT operand", expr)
        return Ok(Filters.not(condition))
    }

    /**
     * 将比较运算符映射为 MongoDB 表达式运算符
     * Map comparison operator to MongoDB expression operator
     *
     * @param operator 比较运算符 / Comparison operator
     * @return MongoDB 表达式运算符字符串 / MongoDB expression operator string
     */
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

    /**
     * 根据策略处理不支持的表达式
     * Handle unsupported expression based on policy
     *
     * @param reason 不支持的原因 / Reason for being unsupported
     * @param expression 不支持的表达式 / Unsupported expression
     * @return 处理结果 / Handling result
     */
    private fun unsupported(reason: String, expression: BooleanExpression): Ret<Bson> {
        return when (unsupportedPredicatePolicy) {
            UnsupportedPredicatePolicy.FailFast -> {
                val detail = UnsupportedPredicateDetail.failFast(
                    expressionType = expression.typeName,
                    reason = reason,
                    backendName = "MongoDB"
                )
                Failed(detail.toError())
            }
            UnsupportedPredicatePolicy.AlwaysFalse -> Ok(alwaysFalse())
            UnsupportedPredicatePolicy.ClientFilter -> {
                val detail = UnsupportedPredicateDetail.clientFilter(
                    expressionType = expression.typeName,
                    reason = reason,
                    backendName = "MongoDB"
                )
                Failed(detail.toError())
            }
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
