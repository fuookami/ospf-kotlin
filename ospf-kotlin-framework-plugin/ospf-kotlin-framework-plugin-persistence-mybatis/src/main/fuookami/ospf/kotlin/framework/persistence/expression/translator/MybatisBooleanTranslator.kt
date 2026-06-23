/**
 * MyBatis 布尔表达式翻译器
 * MyBatis Boolean Expression Translator
 *
 * 将 BooleanExpression 翻译为 MyBatis-Plus Wrapper 条件。
 * Translates BooleanExpression to MyBatis-Plus Wrapper conditions.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper

/**
 * 列名解析器
 * Column Name Resolver
 */
typealias MybatisColumnNameResolver = PersistenceFieldResolver<String>

/**
 * MyBatis 布尔表达式翻译器
 * MyBatis Boolean Expression Translator
 *
 * 将 math.symbol.expression.BooleanExpression 翻译为 MyBatis-Plus 查询条件。
 * Translates math.symbol.expression.BooleanExpression to MyBatis-Plus query conditions.
 *
 * @param T 实体类型 / Entity type
 * @property resolveColumnName 列名解析函数 / Column name resolver function
 * @property unsupportedPredicatePolicy 不支持谓词时的策略 / Policy for unsupported predicates
 */
class MybatisBooleanTranslator<T : Any>(
    private val resolveColumnName: MybatisColumnNameResolver,
    private val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) {
    private val scalarTranslator = MybatisScalarTranslator(resolveColumnName, unsupportedPredicatePolicy)

    /**
     * 翻译布尔表达式到 QueryWrapper
     * Translate boolean expression to QueryWrapper
     *
     * @param wrapper MyBatis-Plus 查询 Wrapper / MyBatis-Plus query wrapper
     * @param expr 布尔表达式 / Boolean expression
     * @return 应用条件后的 QueryWrapper / QueryWrapper with condition applied
     */
    fun translate(wrapper: QueryWrapper<T>, expr: BooleanExpression): Ret<QueryWrapper<T>> {
        return translateInternal(wrapper, expr)
    }

    /**
     * 翻译布尔表达式到 UpdateWrapper
     * Translate boolean expression to UpdateWrapper
     *
     * @param wrapper MyBatis-Plus 更新 Wrapper / MyBatis-Plus update wrapper
     * @param expr 布尔表达式 / Boolean expression
     * @return 应用条件后的 UpdateWrapper / UpdateWrapper with condition applied
     */
    fun translate(wrapper: UpdateWrapper<T>, expr: BooleanExpression): Ret<UpdateWrapper<T>> {
        return translateInternal(wrapper, expr)
    }

    /**
     * 内部翻译分发，根据表达式类型路由到具体翻译方法
     * Internal translation dispatch, routes to specific translation methods by expression type
     *
     * @param wrapper MyBatis-Plus 条件 Wrapper / MyBatis-Plus condition wrapper
     * @param expr 布尔表达式 / Boolean expression
     * @return 应用条件后的 Wrapper / Wrapper with condition applied
     */
    private fun <W : AbstractWrapper<T, String, W>> translateInternal(wrapper: W, expr: BooleanExpression): Ret<W> {
        return when (expr) {
            is BooleanConstant -> translateConstant(wrapper, expr)
            is Comparison<*> -> translateComparison(wrapper, expr)
            is InExpression<*> -> translateIn(wrapper, expr)
            is PatternMatch<*> -> translatePatternMatch(wrapper, expr)
            is NullCheck -> translateNullCheck(wrapper, expr)
            is AndExpression -> translateAnd(wrapper, expr)
            is OrExpression -> translateOr(wrapper, expr)
            is NotExpression -> translateNot(wrapper, expr)
            is BooleanCustom -> unsupported(wrapper, "BooleanCustom is not supported", expr)
        }
    }

    /**
     * 翻译常量布尔表达式
     * Translate constant boolean expression
     *
     * @param wrapper MyBatis-Plus 条件 Wrapper / MyBatis-Plus condition wrapper
     * @param expr 常量布尔表达式 / Constant boolean expression
     * @return 应用常量条件后的 Wrapper / Wrapper with constant condition applied
     */
    private fun <W : AbstractWrapper<T, String, W>> translateConstant(wrapper: W, expr: BooleanConstant): Ret<W> {
        return Ok(when (expr.value) {
            Trivalent.True -> wrapper
            Trivalent.False, Trivalent.Unknown -> wrapper.apply("1 = 0")
        })
    }

    /** 构建 AlwaysFalse 条件 / Build AlwaysFalse condition */
    private fun <W : AbstractWrapper<T, String, W>> alwaysFalse(wrapper: W): W {
        return wrapper.apply("1 = 0")
    }

    /** 构建 Unsupported 条件 / Build Unsupported condition */
    private fun <W : AbstractWrapper<T, String, W>> unsupported(
        wrapper: W,
        reason: String,
        expression: BooleanExpression
    ): Ret<W> {
        return when (unsupportedPredicatePolicy) {
            UnsupportedPredicatePolicy.FailFast -> {
                val detail = UnsupportedPredicateDetail.failFast(
                    expressionType = expression.typeName,
                    reason = reason,
                    backendName = "MyBatis"
                )
                Failed(detail.toError())
            }
            UnsupportedPredicatePolicy.AlwaysFalse -> Ok(alwaysFalse(wrapper))
            UnsupportedPredicatePolicy.ClientFilter -> {
                val detail = UnsupportedPredicateDetail.clientFilter(
                    expressionType = expression.typeName,
                    reason = reason,
                    backendName = "MyBatis"
                )
                Failed(detail.toError())
            }
        }
    }

    /**
     * 翻译比较表达式为 MyBatis-Plus 比较条件
     * Translate comparison expression to MyBatis-Plus comparison condition
     *
     * @param wrapper MyBatis-Plus 条件 Wrapper / MyBatis-Plus condition wrapper
     * @param expr 比较表达式 / Comparison expression
     * @return 应用比较条件后的 Wrapper / Wrapper with comparison condition applied
     */
    private fun <W : AbstractWrapper<T, String, W>> translateComparison(wrapper: W, expr: Comparison<*>): Ret<W> {
        val leftRef = expr.left as? ScalarReference<*>
        val leftConst = expr.left as? ScalarConstant<*>
        val rightRef = expr.right as? ScalarReference<*>
        val rightConst = expr.right as? ScalarConstant<*>

        // 左边是列引用，右边是常量
        // Left is column reference, right is constant
        if (leftRef != null && rightConst != null) {
            val column = resolveColumnName(leftRef.path.value)
                ?: return unsupported(wrapper, "Unresolved comparison path: ${leftRef.path.value}", expr)
            val value = rightConst.value
                ?: return unsupported(wrapper, "Null comparison constant is not supported", expr)
            val jdbcValue = MybatisValueConverter.convert(value)

            return Ok(when (expr.operator) {
                ComparisonOperator.Eq -> wrapper.eq(column, jdbcValue)
                ComparisonOperator.Ne -> wrapper.ne(column, jdbcValue)
                ComparisonOperator.Lt -> wrapper.lt(column, jdbcValue)
                ComparisonOperator.Le -> wrapper.le(column, jdbcValue)
                ComparisonOperator.Gt -> wrapper.gt(column, jdbcValue)
                ComparisonOperator.Ge -> wrapper.ge(column, jdbcValue)
            })
        }

        // 左边是常量，右边是列引用（反转比较）
        // Left is constant, right is column reference (reverse comparison)
        if (leftConst != null && rightRef != null) {
            val column = resolveColumnName(rightRef.path.value)
                ?: return unsupported(wrapper, "Unresolved comparison path: ${rightRef.path.value}", expr)
            val value = leftConst.value
                ?: return unsupported(wrapper, "Null comparison constant is not supported", expr)
            val jdbcValue = MybatisValueConverter.convert(value)

            return Ok(when (expr.operator) {
                ComparisonOperator.Eq -> wrapper.eq(column, jdbcValue)
                ComparisonOperator.Ne -> wrapper.ne(column, jdbcValue)
                ComparisonOperator.Lt -> wrapper.gt(column, jdbcValue)  // 反转
                ComparisonOperator.Le -> wrapper.ge(column, jdbcValue)  // 反转
                ComparisonOperator.Gt -> wrapper.lt(column, jdbcValue)  // 反转
                ComparisonOperator.Ge -> wrapper.le(column, jdbcValue)  // 反转
            })
        }

        val left = scalarTranslator.translate(expr.left).value
            ?: return unsupported(wrapper, "Unsupported left scalar expression: ${expr.left.typeName}", expr)
        val right = scalarTranslator.translate(expr.right).value?.shifted(left.params.size)
            ?: return unsupported(wrapper, "Unsupported right scalar expression: ${expr.right.typeName}", expr)
        val sql = "${left.sql} ${comparisonSql(expr.operator)} ${right.sql}"
        val params = left.params + right.params
        return Ok(wrapper.apply(sql, *params.toTypedArray()))
    }

    /**
     * 翻译 IN 表达式为 MyBatis-Plus in/notIn 条件
     * Translate IN expression to MyBatis-Plus in/notIn condition
     *
     * @param wrapper MyBatis-Plus 条件 Wrapper / MyBatis-Plus condition wrapper
     * @param expr IN 表达式 / IN expression
     * @return 应用 IN 条件后的 Wrapper / Wrapper with IN condition applied
     */
    private fun <W : AbstractWrapper<T, String, W>> translateIn(wrapper: W, expr: InExpression<*>): Ret<W> {
        val ref = expr.value as? ScalarReference<*>
            ?: return unsupported(wrapper, "IN value must be a column reference", expr)
        val column = resolveColumnName(ref.path.value)
            ?: return unsupported(wrapper, "Unresolved IN path: ${ref.path.value}", expr)

        val values = expr.candidates.mapNotNull { (it as? ScalarConstant<*>)?.value }
        if (values.size != expr.candidates.size || values.isEmpty()) {
            return unsupported(wrapper, "IN candidates must be non-empty scalar constants", expr)
        }
        val jdbcValues = values.map { MybatisValueConverter.convert(it) }

        return Ok(if (expr.negated) {
            wrapper.notIn(column, jdbcValues)
        } else {
            wrapper.`in`(column, jdbcValues)
        })
    }

    /**
     * 翻译模式匹配表达式为 MyBatis-Plus like/notLike 条件
     * Translate pattern match expression to MyBatis-Plus like/notLike condition
     *
     * @param wrapper MyBatis-Plus 条件 Wrapper / MyBatis-Plus condition wrapper
     * @param expr 模式匹配表达式 / Pattern match expression
     * @return 应用模式匹配条件后的 Wrapper / Wrapper with pattern match condition applied
     */
    private fun <W : AbstractWrapper<T, String, W>> translatePatternMatch(wrapper: W, expr: PatternMatch<*>): Ret<W> {
        val ref = expr.value as? ScalarReference<*>
            ?: return unsupported(wrapper, "Pattern value must be a column reference", expr)
        val column = resolveColumnName(ref.path.value)
            ?: return unsupported(wrapper, "Unresolved pattern path: ${ref.path.value}", expr)

        val patternValue = (expr.pattern as? ScalarConstant<*>)?.value?.toString()
            ?: return unsupported(wrapper, "Pattern must be a scalar constant", expr)

        val sqlPattern = when (expr.mode) {
            PatternMatchMode.Exact -> patternValue
            PatternMatchMode.Prefix -> "$patternValue%"
            PatternMatchMode.Suffix -> "%$patternValue"
            PatternMatchMode.Contains -> "%$patternValue%"
            PatternMatchMode.Like -> patternValue
            PatternMatchMode.Regex -> return unsupported(wrapper, "Regex pattern is not supported", expr)
        }

        return Ok(if (expr.negated) {
            wrapper.notLike(column, sqlPattern)
        } else {
            wrapper.like(column, sqlPattern)
        })
    }

    /**
     * 翻译空值检查表达式为 MyBatis-Plus isNull/isNotNull 条件
     * Translate null check expression to MyBatis-Plus isNull/isNotNull condition
     *
     * @param wrapper MyBatis-Plus 条件 Wrapper / MyBatis-Plus condition wrapper
     * @param expr 空值检查表达式 / Null check expression
     * @return 应用空值检查条件后的 Wrapper / Wrapper with null check condition applied
     */
    private fun <W : AbstractWrapper<T, String, W>> translateNullCheck(wrapper: W, expr: NullCheck): Ret<W> {
        val column = resolveColumnName(expr.path.value)
            ?: return unsupported(wrapper, "Unresolved null-check path: ${expr.path.value}", expr)

        return Ok(if (expr.isNull) {
            wrapper.isNull(column)
        } else {
            wrapper.isNotNull(column)
        })
    }

    /**
     * 翻译 AND 逻辑表达式，依次应用各操作数条件
     * Translate AND logical expression, applies each operand condition sequentially
     *
     * @param wrapper MyBatis-Plus 条件 Wrapper / MyBatis-Plus condition wrapper
     * @param expr AND 表达式 / AND expression
     * @return 应用 AND 条件后的 Wrapper / Wrapper with AND condition applied
     */
    private fun <W : AbstractWrapper<T, String, W>> translateAnd(wrapper: W, expr: AndExpression): Ret<W> {
        var result = wrapper
        for (operand in expr.operands) {
            result = translateInternal(result, operand).value ?: return Ok(result)
        }
        return Ok(result)
    }

    /**
     * 翻译 OR 逻辑表达式，使用嵌套 and 块实现 OR 组合
     * Translate OR logical expression, uses nested and block to implement OR combination
     *
     * @param wrapper MyBatis-Plus 条件 Wrapper / MyBatis-Plus condition wrapper
     * @param expr OR 表达式 / OR expression
     * @return 应用 OR 条件后的 Wrapper / Wrapper with OR condition applied
     */
    private fun <W : AbstractWrapper<T, String, W>> translateOr(wrapper: W, expr: OrExpression): Ret<W> {
        if (expr.operands.isEmpty()) return Ok(wrapper)

        return Ok(wrapper.and { innerWrapper ->
            var result = innerWrapper
            for ((index, operand) in expr.operands.withIndex()) {
                result = if (index == 0) {
                    translateInternal(result, operand).value ?: result
                } else {
                    result.or().let { translateInternal(it, operand).value ?: it }
                }
            }
            result
        })
    }

    /**
     * 翻译 NOT 逻辑表达式为 MyBatis-Plus not 块条件
     * Translate NOT logical expression to MyBatis-Plus not block condition
     *
     * @param wrapper MyBatis-Plus 条件 Wrapper / MyBatis-Plus condition wrapper
     * @param expr NOT 表达式 / NOT expression
     * @return 应用 NOT 条件后的 Wrapper / Wrapper with NOT condition applied
     */
    private fun <W : AbstractWrapper<T, String, W>> translateNot(wrapper: W, expr: NotExpression): Ret<W> {
        return Ok(wrapper.not { innerWrapper ->
            translateInternal(innerWrapper, expr.operand).value ?: innerWrapper
        })
    }

    private fun comparisonSql(operator: ComparisonOperator): String {
        return when (operator) {
            ComparisonOperator.Eq -> "="
            ComparisonOperator.Ne -> "<>"
            ComparisonOperator.Lt -> "<"
            ComparisonOperator.Le -> "<="
            ComparisonOperator.Gt -> ">"
            ComparisonOperator.Ge -> ">="
        }
    }
}
