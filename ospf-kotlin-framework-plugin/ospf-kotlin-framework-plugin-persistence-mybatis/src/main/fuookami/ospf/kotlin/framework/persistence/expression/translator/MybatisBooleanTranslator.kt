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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper

/**
 * 列名解析器
 * Column Name Resolver
 */
typealias MybatisColumnNameResolver = (String) -> String?

/**
 * MyBatis 布尔表达式翻译器
 * MyBatis Boolean Expression Translator
 *
 * 将 math.symbol.expression.BooleanExpression 翻译为 MyBatis-Plus 查询条件。
 * Translates math.symbol.expression.BooleanExpression to MyBatis-Plus query conditions.
 *
 * @param T 实体类型 / Entity type
 * @property resolveColumnName 列名解析函数 / Column name resolver function
 */
class MybatisBooleanTranslator<T : Any>(
    private val resolveColumnName: MybatisColumnNameResolver
) {
    /**
     * 翻译到 QueryWrapper
     * Translate to QueryWrapper
     */
    fun translate(wrapper: QueryWrapper<T>, expr: BooleanExpression): QueryWrapper<T> {
        return when (expr) {
            is BooleanConstant -> wrapper
            is Comparison<*> -> translateComparison(wrapper, expr)
            is InExpression<*> -> translateIn(wrapper, expr)
            is PatternMatch<*> -> translatePatternMatch(wrapper, expr)
            is NullCheck -> translateNullCheck(wrapper, expr)
            is AndExpression -> translateAnd(wrapper, expr)
            is OrExpression -> translateOr(wrapper, expr)
            is NotExpression -> translateNot(wrapper, expr)
            is BooleanCustom -> wrapper
        }
    }
    private fun translateComparison(wrapper: QueryWrapper<T>, expr: Comparison<*>): QueryWrapper<T> {
        val leftRef = expr.left as? ScalarReference<*>
        val leftConst = expr.left as? ScalarConstant<*>
        val rightRef = expr.right as? ScalarReference<*>
        val rightConst = expr.right as? ScalarConstant<*>

        // 左边是列引用，右边是常量
        // Left is column reference, right is constant
        if (leftRef != null && rightConst != null) {
            val column = resolveColumnName(leftRef.path.value) ?: return wrapper
            val value = rightConst.value ?: return wrapper

            return when (expr.operator) {
                ComparisonOperator.Eq -> wrapper.eq(column, value)
                ComparisonOperator.Ne -> wrapper.ne(column, value)
                ComparisonOperator.Lt -> wrapper.lt(column, value)
                ComparisonOperator.Le -> wrapper.le(column, value)
                ComparisonOperator.Gt -> wrapper.gt(column, value)
                ComparisonOperator.Ge -> wrapper.ge(column, value)
            }
        }

        // 左边是常量，右边是列引用（反转比较）
        // Left is constant, right is column reference (reverse comparison)
        if (leftConst != null && rightRef != null) {
            val column = resolveColumnName(rightRef.path.value) ?: return wrapper
            val value = leftConst.value ?: return wrapper

            return when (expr.operator) {
                ComparisonOperator.Eq -> wrapper.eq(column, value)
                ComparisonOperator.Ne -> wrapper.ne(column, value)
                ComparisonOperator.Lt -> wrapper.gt(column, value)  // 反转
                ComparisonOperator.Le -> wrapper.ge(column, value)  // 反转
                ComparisonOperator.Gt -> wrapper.lt(column, value)  // 反转
                ComparisonOperator.Ge -> wrapper.le(column, value)  // 反转
            }
        }

        return wrapper
    }
    private fun translateIn(wrapper: QueryWrapper<T>, expr: InExpression<*>): QueryWrapper<T> {
        val ref = expr.value as? ScalarReference<*> ?: return wrapper
        val column = resolveColumnName(ref.path.value) ?: return wrapper

        val values = expr.candidates.mapNotNull { (it as? ScalarConstant<*>)?.value }
        if (values.isEmpty()) return wrapper

        return if (expr.negated) {
            wrapper.notIn(column, values)
        } else {
            wrapper.`in`(column, values)
        }
    }
    private fun translatePatternMatch(wrapper: QueryWrapper<T>, expr: PatternMatch<*>): QueryWrapper<T> {
        val ref = expr.value as? ScalarReference<*> ?: return wrapper
        val column = resolveColumnName(ref.path.value) ?: return wrapper

        val patternValue = (expr.pattern as? ScalarConstant<*>)?.value?.toString() ?: return wrapper

        val sqlPattern = when (expr.mode) {
            PatternMatchMode.Exact -> patternValue
            PatternMatchMode.Prefix -> "$patternValue%"
            PatternMatchMode.Suffix -> "%$patternValue"
            PatternMatchMode.Contains -> "%$patternValue%"
            PatternMatchMode.Like -> patternValue
            PatternMatchMode.Regex -> return wrapper // MyBatis-Plus 不直接支持正则
        }

        return if (expr.negated) {
            wrapper.notLike(column, sqlPattern)
        } else {
            wrapper.like(column, sqlPattern)
        }
    }

    private fun translateNullCheck(wrapper: QueryWrapper<T>, expr: NullCheck): QueryWrapper<T> {
        val column = resolveColumnName(expr.path.value) ?: return wrapper

        return if (expr.isNull) {
            wrapper.isNull(column)
        } else {
            wrapper.isNotNull(column)
        }
    }

    private fun translateAnd(wrapper: QueryWrapper<T>, expr: AndExpression): QueryWrapper<T> {
        var result = wrapper
        for (operand in expr.operands) {
            result = translate(result, operand)
        }
        return result
    }

    private fun translateOr(wrapper: QueryWrapper<T>, expr: OrExpression): QueryWrapper<T> {
        if (expr.operands.isEmpty()) return wrapper

        return wrapper.and { innerWrapper ->
            var result = innerWrapper
            for ((index, operand) in expr.operands.withIndex()) {
                result = if (index == 0) {
                    translate(result, operand)
                } else {
                    result.or().let { translate(it, operand) }
                }
            }
            result
        }
    }

    private fun translateNot(wrapper: QueryWrapper<T>, expr: NotExpression): QueryWrapper<T> {
        return wrapper.not { innerWrapper ->
            translate(innerWrapper, expr.operand)
        }
    }
}