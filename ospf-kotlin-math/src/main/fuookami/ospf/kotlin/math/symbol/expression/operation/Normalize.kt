/**
 * 表达式规范化
 * Expression Normalization
 *
 * 提供布尔表达式的规范化操作，包括扁平化、常量折叠、去重、双重否定消除、德摩根定律。
 * Provides boolean expression normalization operations, including flattening,
 * constant folding, deduplication, double negation elimination, and De Morgan's laws.
*/
package fuookami.ospf.kotlin.math.symbol.expression.operation

import fuookami.ospf.kotlin.math.symbol.identity
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent

/**
 * 规范化配罌
 * Normalization Configuration
 *
 * @property flatten 是否扁平化 And/Or / Whether to flatten And/Or
 * @property constantFolding 是否进行常量折叠 / Whether to perform constant folding
 * @property deduplicate 是否去重 / Whether to deduplicate
 * @property eliminateDoubleNegation 是否消除双重否定 / Whether to eliminate double negation
 * @property applyDeMorgan 是否应用德摩根定律 / Whether to apply De Morgan's laws
 * @property sortOperands 是否排序操作数（用于去重比较） / Whether to sort operands (for deduplication comparison)
*/
data class NormalizeConfig(

    /** 是否扁平匌And/Or / Whether to flatten And/Or */
    val flatten: Boolean = true,

    /** 是否进行常量折叠 / Whether to perform constant folding */
    val constantFolding: Boolean = true,

    /** 是否去重 / Whether to deduplicate */
    val deduplicate: Boolean = true,

    /** 是否消除双重否定 / Whether to eliminate double negation */
    val eliminateDoubleNegation: Boolean = true,

    /** 是否应用德摩根定後/ Whether to apply De Morgan's laws */
    val applyDeMorgan: Boolean = false,  // 默认关闭，可能改变语义结枌/ Off by default, may change semantic structure

    /** 是否排序操作数（用于去重比较， Whether to sort operands (for deduplication comparison) */
    val sortOperands: Boolean = false
)

/**
 * 规范化布尔表达式
 * Normalize boolean expression
 *
 * @param expr 要规范化的表达式 / Expression to normalize
 * @param config 规范化配罌/ Normalization configuration
 * @return 规范化后的表达式 / Normalized expression
*/
fun normalize(expr: BooleanExpression, config: NormalizeConfig = NormalizeConfig()): BooleanExpression {
    var result = expr

    // 1. 扁平匌And/Or / Flatten And/Or
    if (config.flatten) {
        result = flatten(result)
    }

    // 2. 消除双重否定 / Eliminate double negation
    if (config.eliminateDoubleNegation) {
        result = eliminateDoubleNegation(result)
    }

    // 3. 应用德摩根定後/ Apply De Morgan's laws
    if (config.applyDeMorgan) {
        result = applyDeMorgan(result)
    }

    // 4. 常量折叠 / Constant folding
    if (config.constantFolding) {
        result = constantFold(result)
    }

    // 5. 去重 / Deduplicate
    if (config.deduplicate) {
        result = deduplicate(result)
    }

    // 6. 排序操作敌/ Sort operands
    if (config.sortOperands) {
        result = sortOperands(result)
    }

    // 递归处理子表达式 / Recursively process sub-expressions
    result = normalizeChildren(result, config)

    // 7. 最终简化：单操作数皌And/Or 简化为该操作数 / Final simplification
    result = simplifySingleOperand(result)

    return result
}

/**
 * 简化单操作数的 And/Or 表达弌
 * Simplify single-operand And/Or expressions
 *
 * @param expr 要简化的表达式 / Expression to simplify
 * @return 简化后的表达式 / Simplified expression
*/
private fun simplifySingleOperand(expr: BooleanExpression): BooleanExpression {
    return when (expr) {
        is AndExpression -> {
            val operands = expr.operands.map { simplifySingleOperand(it) }
            when {
                operands.isEmpty() -> BooleanConstant(Trivalent.True)
                operands.size == 1 -> operands.first()
                else -> AndExpression(operands)
            }
        }
        is OrExpression -> {
            val operands = expr.operands.map { simplifySingleOperand(it) }
            when {
                operands.isEmpty() -> BooleanConstant(Trivalent.False)
                operands.size == 1 -> operands.first()
                else -> OrExpression(operands)
            }
        }
        is NotExpression -> NotExpression(simplifySingleOperand(expr.operand))
        else -> expr
    }
}

/**
 * 递归规范化子表达弌
 * Recursively normalize children
 *
 * @param expr 要递归处理的表达式 / Expression to recursively process
 * @param config 规范化配置 / Normalization configuration
 * @return 递归处理后的表达式 / Expression after recursive processing
*/
private fun normalizeChildren(expr: BooleanExpression, config: NormalizeConfig): BooleanExpression {
    return when (expr) {
        is AndExpression -> AndExpression(expr.operands.map { normalize(it, config) })
        is OrExpression -> OrExpression(expr.operands.map { normalize(it, config) })
        is NotExpression -> NotExpression(normalize(expr.operand, config))
        else -> expr
    }
}

/**
 * 扁平匌And/Or 表达弌
 * Flatten And/Or expressions
 *
 * 将嵌套的 And/Or 展开为单层。
 * Flattens nested And/Or into a single layer.
 *
 * 例如 / Example:
 * - And(A, And(B, C)) -> And(A, B, C)
 * - Or(A, Or(B, C)) -> Or(A, B, C)
 *
 * @param expr 要扁平化的表达式 / Expression to flatten
 * @return 扁平化后的表达式 / Flattened expression
*/
fun flatten(expr: BooleanExpression): BooleanExpression {
    return when (expr) {
        is AndExpression -> {
            val flattenedOperands = mutableListOf<BooleanExpression>()
            for (operand in expr.operands) {
                val flattened = flatten(operand)
                if (flattened is AndExpression) {
                    flattenedOperands.addAll(flattened.operands)
                } else {
                    flattenedOperands.add(flattened)
                }
            }
            if (flattenedOperands.size == 1) {
                flattenedOperands.first()
            } else {
                AndExpression(flattenedOperands)
            }
        }
        is OrExpression -> {
            val flattenedOperands = mutableListOf<BooleanExpression>()
            for (operand in expr.operands) {
                val flattened = flatten(operand)
                if (flattened is OrExpression) {
                    flattenedOperands.addAll(flattened.operands)
                } else {
                    flattenedOperands.add(flattened)
                }
            }
            if (flattenedOperands.size == 1) {
                flattenedOperands.first()
            } else {
                OrExpression(flattenedOperands)
            }
        }
        is NotExpression -> NotExpression(flatten(expr.operand))
        else -> expr
    }
}

/**
 * 常量折叠
 * Constant folding
 *
 * 简化包含布尔常量的表达式。
 * Simplifies expressions containing boolean constants.
 *
 * 规则 / Rules:
 * - A and true -> A
 * - A and false -> false
 * - A or true -> true
 * - A or false -> A
 * - not true -> false
 * - not false -> true
 *
 * @param expr 要进行常量折叠的表达式 / Expression to constant-fold
 * @return 常量折叠后的表达式 / Constant-folded expression
*/
fun constantFold(expr: BooleanExpression): BooleanExpression {
    return when (expr) {
        is BooleanConstant -> expr

        is AndExpression -> {
            val operands = expr.operands.map { constantFold(it) }

            // 如果朌false，结果是 false
            // If there's false, result is false
            if (operands.any { it is BooleanConstant && it.isFalse }) {
                return BooleanConstant(Trivalent.False)
            }

            // 过滤掌true
            // Filter out true
            val filtered = operands.filter { !(it is BooleanConstant && it.isTrue) }

            when {
                filtered.isEmpty() -> BooleanConstant(Trivalent.True)
                filtered.size == 1 -> filtered.first()
                else -> AndExpression(filtered)
            }
        }

        is OrExpression -> {
            val operands = expr.operands.map { constantFold(it) }

            // 如果朌true，结果是 true
            // If there's true, result is true
            if (operands.any { it is BooleanConstant && it.isTrue }) {
                return BooleanConstant(Trivalent.True)
            }

            // 过滤掌false
            // Filter out false
            val filtered = operands.filter { !(it is BooleanConstant && it.isFalse) }

            when {
                filtered.isEmpty() -> BooleanConstant(Trivalent.False)
                filtered.size == 1 -> filtered.first()
                else -> OrExpression(filtered)
            }
        }

        is NotExpression -> {
            val operand = constantFold(expr.operand)
            when (operand) {
                is BooleanConstant -> BooleanConstant(
                    when (operand.value) {
                        Trivalent.True -> Trivalent.False
                        Trivalent.False -> Trivalent.True
                        Trivalent.Unknown -> Trivalent.Unknown
                    }
                )
                else -> NotExpression(operand)
            }
        }

        else -> expr
    }
}

/**
 * 去重
 * Deduplicate
 *
 * 移除结构等价的重复操作数。
 * Removes structurally equivalent duplicate operands.
 *
 * 例如 / Example:
 * - And(A, A, B) -> And(A, B)
 * - Or(X, X, Y) -> Or(X, Y)
 *
 * @param expr 要去重的表达式 / Expression to deduplicate
 * @return 去重后的表达式 / Deduplicated expression
*/
fun deduplicate(expr: BooleanExpression): BooleanExpression {
    return when (expr) {
        is AndExpression -> {
            val uniqueOperands = expr.operands
                .map { deduplicate(it) }
                .distinctBy { it.structuralKey() }

            AndExpression(uniqueOperands)
        }

        is OrExpression -> {
            val uniqueOperands = expr.operands
                .map { deduplicate(it) }
                .distinctBy { it.structuralKey() }

            OrExpression(uniqueOperands)
        }

        is NotExpression -> NotExpression(deduplicate(expr.operand))

        else -> expr
    }
}

/**
 * 消除双重否定
 * Eliminate double negation
 *
 * 规则 / Rule:
 * - not(not(x)) -> x
 *
 * @param expr 要消除双重否定的表达式 / Expression to eliminate double negation
 * @return 消除双重否定后的表达式 / Expression with double negation eliminated
*/
fun eliminateDoubleNegation(expr: BooleanExpression): BooleanExpression {
    return when (expr) {
        is NotExpression -> {
            val operand = eliminateDoubleNegation(expr.operand)
            when (operand) {
                is NotExpression -> operand.operand
                else -> NotExpression(operand)
            }
        }
        is AndExpression -> AndExpression(expr.operands.map { eliminateDoubleNegation(it) })
        is OrExpression -> OrExpression(expr.operands.map { eliminateDoubleNegation(it) })
        else -> expr
    }
}

/**
 * 应用德摩根定後
 * Apply De Morgan's laws
 *
 * 规则 / Rules:
 * - not(A and B) -> not(A) or not(B)
 * - not(A or B) -> not(A) and not(B)
 *
 * @param expr 要应用德摩根定律的表达式 / Expression to apply De Morgan's laws
 * @return 应用德摩根定律后的表达式 / Expression after applying De Morgan's laws
*/
fun applyDeMorgan(expr: BooleanExpression): BooleanExpression {
    return when (expr) {
        is NotExpression -> {
            val operand = applyDeMorgan(expr.operand)
            when (operand) {
                is AndExpression -> OrExpression(operand.operands.map { NotExpression(it) })
                is OrExpression -> AndExpression(operand.operands.map { NotExpression(it) })
                else -> NotExpression(operand)
            }
        }
        is AndExpression -> AndExpression(expr.operands.map { applyDeMorgan(it) })
        is OrExpression -> OrExpression(expr.operands.map { applyDeMorgan(it) })
        else -> expr
    }
}

/**
 * 排序操作敌
 * Sort operands
 *
 * 富And/Or 的操作数按结构键排序，便于比较。
 * Sort operands of And/Or by structural key for easier comparison.
 *
 * @param expr 要排序操作数的表达式 / Expression whose operands to sort
 * @return 操作数排序后的表达式 / Expression with sorted operands
*/
fun sortOperands(expr: BooleanExpression): BooleanExpression {
    return when (expr) {
        is AndExpression -> {
            val sorted = expr.operands.map { sortOperands(it) }.sortedBy { it.structuralKey() }
            AndExpression(sorted)
        }
        is OrExpression -> {
            val sorted = expr.operands.map { sortOperands(it) }.sortedBy { it.structuralKey() }
            OrExpression(sorted)
        }
        is NotExpression -> NotExpression(sortOperands(expr.operand))
        else -> expr
    }
}

// ========== 辅助函数 / Helper Functions ==========

/**
 * 获取表达式的结构键（用于去重和排序）
 * Get structural key of expression (for deduplication and sorting)
 *
 * @return 结构键字符串 / Structural key string
*/
fun BooleanExpression.structuralKey(): String {
    return when (this) {
        is BooleanConstant -> "Const:${when(value) { Trivalent.True -> "True"; Trivalent.False -> "False"; Trivalent.Unknown -> "Unknown" }}"
        is Comparison<*> -> "Cmp:$operator:${left.structuralKey()}:${right.structuralKey()}"
        is InExpression<*> -> "In:$negated:${value.structuralKey()}:${candidates.joinToString(",") { it.structuralKey() }}"
        is PatternMatch<*> -> "Match:$mode:$negated:${value.structuralKey()}:${pattern.structuralKey()}"
        is NullCheck -> "Null:$type:$path"
        is AndExpression -> "And:${operands.joinToString(",") { it.structuralKey() }}"
        is OrExpression -> "Or:${operands.joinToString(",") { it.structuralKey() }}"
        is NotExpression -> "Not:${operand.structuralKey()}"
        is BooleanCustom -> "Custom:$description"
    }
}

/**
 * 获取标量表达式的结构锌
 * Get structural key of scalar expression
 *
 * @return 结构键字符串 / Structural key string
*/
fun ScalarExpression<*>.structuralKey(): String {
    return when (this) {
        is ScalarConstant<*> -> "Const:$value"
        is ScalarReference<*> -> "Ref:$path"
        is ScalarSymbolReference<*> -> "SymRef:${symbol.identity()}"
        is ScalarUnary<*> -> "Unary:$operator:${operand.structuralKey()}"
        is ScalarBinary<*> -> "Bin:$operator:${left.structuralKey()}:${right.structuralKey()}"
        is ScalarFunction<*> -> "Func:$name:${arguments.joinToString(",") { it.structuralKey() }}"
        is ScalarConditional<*> -> "Cond:${condition.structuralKey()}:${thenBranch.structuralKey()}:${elseBranch.structuralKey()}"
        is ScalarBoolean<*> -> "Bool:${expr.structuralKey()}"
        is ScalarCustom<*> -> "Custom:$description"
    }
}
