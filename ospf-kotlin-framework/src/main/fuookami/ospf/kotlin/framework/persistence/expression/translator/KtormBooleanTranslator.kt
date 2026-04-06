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
import fuookami.ospf.kotlin.math.Trivalent
import org.ktorm.dsl.*
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.expression.ScalarExpression

/**
 * Ktorm 布尔表达式翻译器
 * Ktorm Boolean Expression Translator
 *
 * 将 math.symbol.expression.BooleanExpression 翻译为 Ktorm 查询条件。
 * Translates math.symbol.expression.BooleanExpression to Ktorm query conditions.
 *
 * @property meta 实体元数据 / Entity metadata
 * @property patternMatchPolicy 模式匹配策略 / Pattern match policy
 */
class KtormBooleanTranslator(
    private val meta: EntityMeta<*>,
    private val patternMatchPolicy: PatternMatchPolicy = DefaultPatternMatchPolicy
) {
    /**
     * 翻译布尔表达式
     * Translate boolean expression
     *
     * @param expr 布尔表达式 / Boolean expression
     * @return Ktorm 条件表达式，无法翻译时返回 null / Ktorm condition, null if cannot translate
     */
    fun translate(expr: BooleanExpression): ColumnDeclaring<Boolean>? {
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

    private fun translateConstant(expr: BooleanConstant): ColumnDeclaring<Boolean>? {
        return when (expr.value) {
            Trivalent.True -> trueLit()
            Trivalent.False -> falseLit()
            Trivalent.Unknown -> null
        }
    }

    private fun translateComparison(expr: Comparison<*>): ColumnDeclaring<Boolean>? {
        val left = translateScalar(expr.left) ?: return null
        val right = translateScalar(expr.right) ?: return null

        return when (expr.operator) {
            ComparisonOperator.Eq -> left.eq(right)
            ComparisonOperator.Ne -> left.neq(right)
            ComparisonOperator.Lt -> left.lt(right)
            ComparisonOperator.Le -> left.lte(right)
            ComparisonOperator.Gt -> left.gt(right)
            ComparisonOperator.Ge -> left.gte(right)
        }
    }

    private fun translateIn(expr: InExpression<*>): ColumnDeclaring<Boolean>? {
        val value = translateScalar(expr.value) ?: return null

        // 检查是否所有候选值都是常量
        // Check if all candidates are constants
        val constantValues = expr.candidates.mapNotNull { (it as? ScalarConstant<*>)?.value }
        if (constantValues.size != expr.candidates.size) {
            return null  // 暂不支持非常量候选值 / Non-constant candidates not supported yet
        }

        @Suppress("UNCHECKED_CAST")
        val condition = (value as ColumnDeclaring<Any>).`in`(constantValues)
        return if (expr.negated) condition.not() else condition
    }

    private fun translatePatternMatch(expr: PatternMatch<*>): ColumnDeclaring<Boolean>? {
        val column = translateScalar(expr.value) ?: return null
        val patternValue = (expr.pattern as? ScalarConstant<*>)?.value?.toString() ?: return null

        // 将通用模式转换为 SQL LIKE 模式
        // Convert generic pattern to SQL LIKE pattern
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
        val column = meta.resolveColumn(expr.path) ?: throw IllegalArgumentException(
            "Cannot resolve path: ${expr.path}"
        )

        return if (expr.isNull) {
            column.isNull()
        } else {
            column.isNotNull()
        }
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

    private fun translateScalar(expr: ScalarExpression<*>): ColumnDeclaring<*>? {
        return when (expr) {
            is ScalarConstant<*> -> {
                // 常量需要包装为参数绑定
                // Wrap constant as parameter binding
                wrapConstant(expr.value)
            }
            is ScalarReference<*> -> {
                meta.resolveColumn(expr.path) ?: throw IllegalArgumentException(
                    "Cannot resolve path: ${expr.path}"
                )
            }
            is ScalarUnary<*> -> translateUnary(expr)
            is ScalarBinary<*> -> translateBinary(expr)
            is ScalarFunction<*> -> null  // 暂不支持 / Not supported yet
            is ScalarCustom<*> -> null
        }
    }

    private fun wrapConstant(value: Any?): ColumnDeclaring<*> {
        return when (value) {
            is String -> bindArgument(StringSqlType, value)
            is Int -> bindArgument(IntSqlType, value)
            is Long -> bindArgument(LongSqlType, value)
            is Double -> bindArgument(DoubleSqlType, value)
            is Boolean -> bindArgument(BooleanSqlType, value)
            null -> throw IllegalArgumentException("Cannot bind null constant without type")
            else -> bindArgument(StringSqlType, value.toString())
        }
    }

    private fun translateUnary(expr: ScalarUnary<*>): ColumnDeclaring<*>? {
        val operand = translateScalar(expr.operand) ?: return null

        return when (expr.operator) {
            UnaryOperator.Negate -> {
                // Ktorm 不直接支持一元负号，使用表达式
                // Ktorm doesn't directly support unary minus, use expression
                null  // 暂不支持 / Not supported yet
            }
            UnaryOperator.Positive -> operand
            UnaryOperator.Abs -> {
                // Ktorm 不直接支持 ABS，需要使用函数调用
                // Ktorm doesn't directly support ABS, need function call
                null  // 暂不支持 / Not supported yet
            }
        }
    }

    private fun translateBinary(expr: ScalarBinary<*>): ColumnDeclaring<*>? {
        val left = translateScalar(expr.left) ?: return null
        val right = translateScalar(expr.right) ?: return null

        return when (expr.operator) {
            BinaryOperator.Add -> null  // 暂不支持 / Not supported yet
            BinaryOperator.Subtract -> null
            BinaryOperator.Multiply -> null
            BinaryOperator.Divide -> null
            BinaryOperator.Modulo -> null
            BinaryOperator.Power -> null
        }
    }
}

// ========== 便捷扩展函数 / Convenience Extension Functions ==========

/**
 * 将 BooleanExpression 翻译为 Ktorm WHERE 条件
 * Translate BooleanExpression to Ktorm WHERE condition
 */
fun BooleanExpression.toKtormWhere(
    meta: EntityMeta<*>,
    policy: PatternMatchPolicy = DefaultPatternMatchPolicy
): ColumnDeclaring<Boolean>? {
    return KtormBooleanTranslator(meta, policy).translate(this)
}

/**
 * 应用 BooleanExpression 到 QuerySource 作为 WHERE 条件
 * Apply BooleanExpression to QuerySource as WHERE condition
 */
fun QuerySource.where(
    expr: BooleanExpression,
    meta: EntityMeta<*>,
    policy: PatternMatchPolicy = DefaultPatternMatchPolicy
): QuerySource {
    val condition = expr.toKtormWhere(meta, policy) ?: return this
    return this.where(condition)
}