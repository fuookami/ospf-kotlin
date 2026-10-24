/**
 * 表达式规范化 / Expression Normalization
 *
 * 提供布尔表达式的规范化操作，包括扁平化、常量折叠、去重、双重否定消除、德摩根定律。 / Provides boolean expression normalization operations, including flattening,
 * constant folding, deduplication, double negation elimination, and De Morgan's laws.
*/
package fuookami.ospf.kotlin.math.symbol.expression.operation

import java.util.IdentityHashMap
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.identity
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent

/**
 * 规范化配罌 / Normalization Configuration
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
 * 规范化布尔表达式 / Normalize boolean expression
 *
 * @param expr 要规范化的表达式 / Expression to normalize
 * @param config 规范化配罌/ Normalization configuration
 * @return 规范化后的表达式 / Normalized expression
*/
fun normalize(expr: BooleanExpression, config: NormalizeConfig = NormalizeConfig()): BooleanExpression {
    return normalize(
        expr = expr,
        config = config,
        keys = if (config.deduplicate || config.sortOperands) normalizationKeys(expr) else null
    )
}

private fun normalize(
    expr: BooleanExpression,
    config: NormalizeConfig,
    keys: NormalizationKeys?
): BooleanExpression {
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
        result = deduplicate(result, keys)
    }

    // 6. 排序操作敌/ Sort operands
    if (config.sortOperands) {
        result = sortOperands(result, keys)
    }

    // 递归处理子表达式 / Recursively process sub-expressions
    result = normalizeChildren(expr = result, config = config, keys = keys)

    // 7. 最终简化：单操作数皌And/Or 简化为该操作数 / Final simplification
    result = simplifySingleOperand(result)

    return result
}

/**
 * 简化单操作数的 And/Or 表达弌 / Simplify single-operand And/Or expressions
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
 * 递归规范化子表达弌 / Recursively normalize children
 *
 * @param expr 要递归处理的表达式 / Expression to recursively process
 * @param config 规范化配置 / Normalization configuration
 * @return 递归处理后的表达式 / Expression after recursive processing
*/
private fun normalizeChildren(
    expr: BooleanExpression,
    config: NormalizeConfig,
    keys: NormalizationKeys?
): BooleanExpression {
    fun child(expr: BooleanExpression): BooleanExpression = normalize(
        expr = expr,
        config = config,
        keys = keys
    )
    return when (expr) {
        is AndExpression -> AndExpression(expr.operands.map { child(it) })
        is OrExpression -> OrExpression(expr.operands.map { child(it) })
        is NotExpression -> NotExpression(child(expr.operand))
        else -> expr
    }
}

/**
 * 扁平匌And/Or 表达弌 / Flatten And/Or expressions
 *
 * 将嵌套的 And/Or 展开为单层。 / Flattens nested And/Or into a single layer.
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
 * 常量折叠 / Constant folding
 *
 * 简化包含布尔常量的表达式。 / Simplifies expressions containing boolean constants.
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
 * 去重 / Deduplicate
 *
 * 移除结构等价的重复操作数。 / Removes structurally equivalent duplicate operands.
 *
 * 例如 / Example:
 * - And(A, A, B) -> And(A, B)
 * - Or(X, X, Y) -> Or(X, Y)
 *
 * @param expr 要去重的表达式 / Expression to deduplicate
 * @return 去重后的表达式 / Deduplicated expression
*/
fun deduplicate(expr: BooleanExpression): BooleanExpression {
    return deduplicate(expr, normalizationKeys(expr))
}

private fun deduplicate(expr: BooleanExpression, keys: NormalizationKeys?): BooleanExpression {
    return when (expr) {
        is AndExpression -> {
            val uniqueOperands = expr.operands
                .map { deduplicate(it, keys) }
                .distinctBy { keys?.boolean(it) ?: it.structuralKey() }

            AndExpression(uniqueOperands)
        }

        is OrExpression -> {
            val uniqueOperands = expr.operands
                .map { deduplicate(it, keys) }
                .distinctBy { keys?.boolean(it) ?: it.structuralKey() }

            OrExpression(uniqueOperands)
        }

        is NotExpression -> NotExpression(deduplicate(expr.operand, keys))

        else -> expr
    }
}

/**
 * 消除双重否定 / Eliminate double negation
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
 * 应用德摩根定後 / Apply De Morgan's laws
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
 * 排序操作敌 / Sort operands
 *
 * 富And/Or 的操作数按结构键排序，便于比较。 / Sort operands of And/Or by structural key for easier comparison.
 *
 * @param expr 要排序操作数的表达式 / Expression whose operands to sort
 * @return 操作数排序后的表达式 / Expression with sorted operands
*/
fun sortOperands(expr: BooleanExpression): BooleanExpression {
    return sortOperands(expr, normalizationKeys(expr))
}

private fun sortOperands(expr: BooleanExpression, keys: NormalizationKeys?): BooleanExpression {
    return when (expr) {
        is AndExpression -> {
            val sorted = expr.operands.map { sortOperands(it, keys) }.sortedBy { keys?.boolean(it) ?: it.structuralKey() }
            AndExpression(sorted)
        }
        is OrExpression -> {
            val sorted = expr.operands.map { sortOperands(it, keys) }.sortedBy { keys?.boolean(it) ?: it.structuralKey() }
            OrExpression(sorted)
        }
        is NotExpression -> NotExpression(sortOperands(expr.operand, keys))
        else -> expr
    }
}

// ========== 辅助函数 / Helper Functions ==========

/**
 * 获取表达式的结构键（用于去重和排序） / Get structural key of expression (for deduplication and sorting)
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
 * 获取标量表达式的结构锌 / Get structural key of scalar expression
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

private fun normalizationKeys(expression: BooleanExpression): NormalizationKeys? {
    fun compositeScalar(expression: ScalarExpression<*>): Boolean {
        return when (expression) {
            is ScalarConstant<*>, is ScalarReference<*>, is ScalarSymbolReference<*>, is ScalarCustom<*> -> false
            else -> true
        }
    }
    fun compositeOperand(expression: BooleanExpression): Boolean {
        return when (expression) {
            is Comparison<*> -> compositeScalar(expression.left) || compositeScalar(expression.right)
            is InExpression<*> -> compositeScalar(expression.value) || expression.candidates.any { compositeScalar(it) }
            is PatternMatch<*> -> compositeScalar(expression.value) || compositeScalar(expression.pattern)
            is AndExpression, is OrExpression, is NotExpression -> true
            else -> false
        }
    }
    val reusable = when (expression) {
        is AndExpression -> expression.operands.any { compositeOperand(it) }
        is OrExpression -> expression.operands.any { compositeOperand(it) }
        else -> compositeOperand(expression)
    }
    if (reusable) return NormalizationKeys(expression)
    val operands = when (expression) {
        is AndExpression -> expression.operands
        is OrExpression -> expression.operands
        else -> return null
    }
    if (operands.size < 8) return null
    fun stableAtom(expression: ScalarExpression<*>): Boolean {
        return when (expression) {
            is ScalarReference<*>, is ScalarCustom<*> -> true
            is ScalarConstant<*> -> when (expression.value) {
                null, is String, is Boolean, is Char, is Byte, is Short, is Int, is Long, is Float, is Double,
                is Int8, is Int16, is Int32, is Int64, is IntX, is UInt8, is UInt16, is UInt32, is UInt64, is UIntX,
                is Flt32, is Flt64, is FltX, is Trivalent -> true
                else -> false
            }
            else -> false
        }
    }
    fun stableOperand(expression: BooleanExpression): Boolean {
        return when (expression) {
            is Comparison<*> -> stableAtom(expression.left) && stableAtom(expression.right)
            is InExpression<*> -> stableAtom(expression.value) && expression.candidates.all { stableAtom(it) }
            is PatternMatch<*> -> stableAtom(expression.value) && stableAtom(expression.pattern)
            is BooleanConstant, is NullCheck, is BooleanCustom -> true
            else -> false
        }
    }
    val stable = operands.all { stableOperand(it) }
    // 浅层原子操作数只复用布尔键，不建立安全扫描表或缓存廉价的标量叶键。
    // Shallow atomic operands reuse only Boolean keys, without a safety map or cheap scalar-leaf keys.
    return if (stable) NormalizationKeys(expression = expression, atomic = true) else null
}

private class NormalizationKeys(expression: BooleanExpression, private val atomic: Boolean = false) {
    private val keys = IdentityHashMap<Any, String>()
    private val safeNodes = if (atomic) null else IdentityHashMap<Any, Boolean>()
    private val enabled = atomic || canMemoize(expression)
    private var cachedCharacters = 0

    private inline fun cachedKey(expression: Any, build: () -> String): String {
        keys[expression]?.let { return it }
        val key = build()
        // 单次调用的缓存有界，避免深树的所有祖先键同时驻留。
        // Bound per-call retention so deep trees do not retain every large ancestor key.
        if (keys.size < 4096 && key.length <= 1_048_576 - cachedCharacters) {
            keys[expression] = key
            cachedCharacters += key.length
        }
        return key
    }

    private fun canMemoize(expression: Any): Boolean {
        val safeNodes = safeNodes ?: return false
        safeNodes[expression]?.let { return it }
        safeNodes[expression] = false
        val safe = when (expression) {
            is BooleanConstant, is NullCheck, is BooleanCustom, is ScalarReference<*>, is ScalarCustom<*> -> true
            is Comparison<*> -> canMemoize(expression.left) && canMemoize(expression.right)
            is InExpression<*> -> canMemoize(expression.value) && expression.candidates.all { canMemoize(it) }
            is PatternMatch<*> -> canMemoize(expression.value) && canMemoize(expression.pattern)
            is AndExpression -> expression.operands.all { canMemoize(it) }
            is OrExpression -> expression.operands.all { canMemoize(it) }
            is NotExpression -> canMemoize(expression.operand)
            is ScalarConstant<*> -> when (expression.value) {
                null, is String, is Boolean, is Char, is Byte, is Short, is Int, is Long, is Float, is Double,
                is Int8, is Int16, is Int32, is Int64, is IntX, is UInt8, is UInt16, is UInt32, is UInt64, is UIntX,
                is Flt32, is Flt64, is FltX, is Trivalent -> true
                else -> false
            }
            is ScalarUnary<*> -> canMemoize(expression.operand)
            is ScalarBinary<*> -> canMemoize(expression.left) && canMemoize(expression.right)
            is ScalarFunction<*> -> expression.arguments.all { canMemoize(it) }
            is ScalarConditional<*> -> canMemoize(expression.condition) &&
                canMemoize(expression.thenBranch) && canMemoize(expression.elseBranch)
            is ScalarBoolean<*> -> canMemoize(expression.expr)
            else -> false
        }
        safeNodes[expression] = safe
        return safe
    }

    fun boolean(expression: BooleanExpression): String {
        // 未知常量的toString与自定义符号身份可能有状态，整个调用保留原键读取路径。
        // Unknown constant formatting and custom symbol identity may be stateful; retain original reads for the call.
        if (!enabled) return expression.structuralKey()
        if (atomic) return cachedKey(expression) { expression.structuralKey() }
        return cachedKey(expression) {
            when (expression) {
                is BooleanConstant -> "Const:${when (expression.value) { Trivalent.True -> "True"; Trivalent.False -> "False"; Trivalent.Unknown -> "Unknown" }}"
                is Comparison<*> -> "Cmp:${expression.operator}:${scalar(expression.left)}:${scalar(expression.right)}"
                is InExpression<*> -> "In:${expression.negated}:${scalar(expression.value)}:${expression.candidates.joinToString(",") { scalar(it) }}"
                is PatternMatch<*> -> "Match:${expression.mode}:${expression.negated}:${scalar(expression.value)}:${scalar(expression.pattern)}"
                is NullCheck -> "Null:${expression.type}:${expression.path}"
                is AndExpression -> "And:${expression.operands.joinToString(",") { boolean(it) }}"
                is OrExpression -> "Or:${expression.operands.joinToString(",") { boolean(it) }}"
                is NotExpression -> "Not:${boolean(expression.operand)}"
                is BooleanCustom -> "Custom:${expression.description}"
            }
        }
    }

    private fun scalar(expression: ScalarExpression<*>): String {
        return cachedKey(expression) {
            when (expression) {
                is ScalarConstant<*> -> "Const:${expression.value}"
                is ScalarReference<*> -> "Ref:${expression.path}"
                is ScalarSymbolReference<*> -> "SymRef:${expression.symbol.identity()}"
                is ScalarUnary<*> -> "Unary:${expression.operator}:${scalar(expression.operand)}"
                is ScalarBinary<*> -> "Bin:${expression.operator}:${scalar(expression.left)}:${scalar(expression.right)}"
                is ScalarFunction<*> -> "Func:${expression.name}:${expression.arguments.joinToString(",") { scalar(it) }}"
                is ScalarConditional<*> -> "Cond:${boolean(expression.condition)}:${scalar(expression.thenBranch)}:${scalar(expression.elseBranch)}"
                is ScalarBoolean<*> -> "Bool:${boolean(expression.expr)}"
                is ScalarCustom<*> -> "Custom:${expression.description}"
            }
        }
    }
}
