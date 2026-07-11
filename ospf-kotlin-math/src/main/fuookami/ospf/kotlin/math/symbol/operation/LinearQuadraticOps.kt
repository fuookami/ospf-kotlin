/**
 * 线性二次运箌
 * Linear-Quadratic Operations
 *
 * 提供线性和二次多项式的核心运算操作。
 * 包括同类项合并、求值、有序求值和部分求值，基于 Ring 类型约束。
 * Provides core operation functions for linear and quadratic polynomials.
 * Includes combining like terms, evaluation, ordered evaluation,
 * and partial evaluation, based on Ring type constraints.
*/
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * Key for identifying quadratic terms during like-term combination.
 * 二次项合并时的项键。
 *
 * Normalizes symbol order so that (s1, s2) and (s2, s1) are treated as the same term.
 * 规范化符号顺序，使 (s1, s2) 和 (s2, s1) 被视为同一项。
 *
 * @property symbol1 the first symbol / 第一个符号
 * @property symbol2 the second symbol (null for linear terms in quadratic form) / 第二个符号（二次形式中的线性项为 null）
*/
internal class QuadraticTermKey private constructor(
    val symbol1: Symbol,
    val symbol2: Symbol?,
    private val hash: Int
) {

    /**
     * Companion object providing factory methods for QuadraticTermKey.
     * 提供 QuadraticTermKey 工厂方法的伴生对象。
    */
    companion object {

/**
 * Creates a normalized key for a quadratic term, ensuring consistent symbol ordering.
 * 为二次项创建规范化的键，确保符号顺序一致。
 *
 * @param symbol1 the first symbol / 第一个符号
 * @param symbol2 the second symbol (nullable) / 第二个符号（可为 null）
 * @param comparator the comparator for symbol ordering / 符号排序比较器
 * @return the normalized QuadraticTermKey / 规范化后的二次项键
*/
fun normalized(
            symbol1: Symbol,
            symbol2: Symbol?,
            comparator: Comparator<Symbol>
        ): QuadraticTermKey {
            if (symbol2 == null) {
                return QuadraticTermKey(symbol1, null, hashOf(symbol1, null))
            }
            return if (comparator.compare(symbol1, symbol2) <= 0) {
                QuadraticTermKey(symbol1, symbol2, hashOf(symbol1, symbol2))
            } else {
                QuadraticTermKey(symbol2, symbol1, hashOf(symbol2, symbol1))
            }
        }

        /**
         * Computes a combined hash value for two symbols.
         * 计算两个符号的组合哈希值。
         *
         * @param symbol1 the first symbol / 第一个符号
         * @param symbol2 the second symbol (nullable) / 第二个符号（可为 null）
         * @return the combined hash code / 组合哈希值
        */
        private fun hashOf(symbol1: Symbol, symbol2: Symbol?): Int {
            var result = symbol1.hashCode()
            result = 31 * result + (symbol2?.hashCode() ?: 0)
            return result
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is QuadraticTermKey) {
            return false
        }
        return symbol1 == other.symbol1 && symbol2 == other.symbol2
    }

    override fun hashCode(): Int = hash
}

/**
 * Combine like terms in a collection of linear monomials.
 * 合并线性单项式集合中的同类项。
 *
 * @param zero the zero value for the coefficient type / 系数类型的零值
 * @param isZero predicate to check if a value is zero / 判断值是否为零的谓词
 * @return the list of combined linear monomials / 合并后的线性单项式列表
*/
internal fun <T> Iterable<LinearMonomial<T>>.combineLinearMonomials(
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): List<LinearMonomial<T>> where T : Ring<T> {
    val coefficientOfSymbol = LinkedHashMap<Symbol, T>()
    for (monomial in this) {
        coefficientOfSymbol[monomial.symbol] =
            (coefficientOfSymbol[monomial.symbol] ?: zero) + monomial.coefficient
    }
    val combinedMonomials = ArrayList<LinearMonomial<T>>(coefficientOfSymbol.size)
    for ((symbol, coefficient) in coefficientOfSymbol) {
        if (!isZero(coefficient)) {
            combinedMonomials.add(LinearMonomial(coefficient = coefficient, symbol = symbol))
        }
    }
    return combinedMonomials
}

/**
 * Combine like terms in a collection of quadratic monomials.
 * 合并二次单项式集合中的同类项。
 *
 * @param zero the zero value for the coefficient type / 系数类型的零值
 * @param isZero predicate to check if a value is zero / 判断值是否为零的谓词
 * @param symbolComparator comparator for symbol ordering (optional) / 符号排序比较器（可选）
 * @return the list of combined quadratic monomials / 合并后的二次单项式列表
*/
internal fun <T> Iterable<QuadraticMonomial<T>>.combineQuadraticMonomials(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): List<QuadraticMonomial<T>> where T : Ring<T> {
    val comparator = symbolComparator ?: defaultSymbolComparator
    val coefficientOfKey = LinkedHashMap<QuadraticTermKey, T>()
    for (monomial in this) {
        val key = QuadraticTermKey.normalized(monomial.symbol1, monomial.symbol2, comparator)
        coefficientOfKey[key] = (coefficientOfKey[key] ?: zero) + monomial.coefficient
    }
    val combinedMonomials = ArrayList<QuadraticMonomial<T>>(coefficientOfKey.size)
    for ((key, coefficient) in coefficientOfKey) {
        if (!isZero(coefficient)) {
            combinedMonomials.add(
                QuadraticMonomial(
                    coefficient = coefficient,
                    symbol1 = key.symbol1,
                    symbol2 = key.symbol2
                )
            )
        }
    }
    return combinedMonomials
}

/**
 * 构建有序符号到索引的映射
 * Build a mapping from ordered symbols to indices
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param valuesSize 值列表大小，需与 order 大小一致 / Size of values list, must match order size
 * @return 符号到索引的映射 / Map from symbol to index
 * @throws IllegalArgumentException 若大小不匹配或存在重复符号 / If sizes mismatch or duplicate symbols exist
*/
private fun buildOrderedSymbolIndex(order: List<Symbol>, valuesSize: Int): Ret<Map<Symbol, Int>> {
    if (order.size != valuesSize) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Order and values size mismatch: order.size=${order.size}, values.size=$valuesSize."
        )
    }
    val indexOfSymbol = LinkedHashMap<Symbol, Int>(order.size)
    for ((index, symbol) in order.withIndex()) {
        if (indexOfSymbol.put(symbol, index) != null) {
            return Failed(ErrorCode.IllegalArgument, "Symbol order contains duplicated symbols.")
        }
    }
    return Ok(indexOfSymbol)
}

// ============================================================================
// Linear Polynomial Operations (Ring-based, no Generic conversion)
// ============================================================================

/**
 * 合并线性多项式中的同类项
 * Combine like terms in a linear polynomial.
 *
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @return 合并同类项后的线性多项式 / Linear polynomial with like terms combined
*/
fun <T> LinearPolynomial<T>.combineLinearTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    val newMonomials = monomials.combineLinearMonomials(zero, isZero)
    return LinearPolynomial(monomials = newMonomials, constant = constant)
}

/**
 * 使用给定值对线性多项式求值
 * Evaluate a linear polynomial with given values.
 *
 * @param values 符号到值的映射 / Map of symbol to value
 * @param onMissing 缺失符号的回调函数（可选） / Callback for missing symbols (optional)
 * @return 求值结果，若存在未提供值的符号则返回 null / Evaluation result, or null if a symbol has no value
*/
fun <T> LinearPolynomial<T>.evaluateLinear(
    values: Map<Symbol, T>,
    onMissing: ((Symbol) -> T?)? = null
): T? where T : Ring<T> {
    var value = constant
    for (monomial in monomials) {
        val symbolValue = values[monomial.symbol]
            ?: onMissing?.invoke(monomial.symbol)
            ?: return null
        value += monomial.coefficient * symbolValue
    }
    return value
}

/**
 * 使用有序符号和值对线性多项式求值
 * Evaluate a linear polynomial with ordered symbols and values.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param values 与符号顺序对应的值列表 / List of values corresponding to symbol order
 * @return 求值结果 / Evaluation result
*/
fun <T> LinearPolynomial<T>.evaluateLinearOrdered(
    order: List<Symbol>,
    values: List<T>
): Ret<T> where T : Ring<T> {
    val indexOfSymbol = when (val result = buildOrderedSymbolIndex(order, values.size)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    var value = constant
    for (monomial in monomials) {
        val index = indexOfSymbol[monomial.symbol]
            ?: return Failed(ErrorCode.DataNotFound, "Symbol ${monomial.symbol.name} not found in order.")
        value += monomial.coefficient * values[index]
    }
    return Ok(value)
}

/**
 * 对线性多项式进行部分求值
 * Partially evaluate a linear polynomial.
 *
 * 将已知符号的值代入，返回仅包含未知符号的线性多项式。
 * Substitutes known symbol values, returning a linear polynomial with only unknown symbols.
 *
 * @param values 已知符号到值的映射 / Map of known symbol to value
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @return 部分求值后的线性多项式 / Partially evaluated linear polynomial
*/
fun <T> LinearPolynomial<T>.partialEvaluateLinear(
    values: Map<Symbol, T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    val remainedMonomials = ArrayList<LinearMonomial<T>>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        val symbolValue = values[monomial.symbol]
        if (symbolValue == null) {
            remainedMonomials.add(monomial)
        } else {
            newConstant += monomial.coefficient * symbolValue
        }
    }
    return LinearPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineLinearTerms(zero, isZero)
}

// ============================================================================
// Quadratic Polynomial Operations (Ring-based, no Generic conversion)
// ============================================================================

/**
 * 合并二次多项式中的同类项
 * Combine like terms in a quadratic polynomial.
 *
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 合并同类项后的二次多项式 / Quadratic polynomial with like terms combined
*/
fun <T> QuadraticPolynomial<T>.combineQuadraticTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T> where T : Ring<T> {
    val newMonomials = monomials.combineQuadraticMonomials(
        zero = zero,
        isZero = isZero,
        symbolComparator = symbolComparator
    )
    return QuadraticPolynomial(monomials = newMonomials, constant = constant)
}

/**
 * 使用给定值对二次多项式求值
 * Evaluate a quadratic polynomial with given values.
 *
 * @param values 符号到值的映射 / Map of symbol to value
 * @param onMissing 缺失符号的回调函数（可选） / Callback for missing symbols (optional)
 * @return 求值结果，若存在未提供值的符号则返回 null / Evaluation result, or null if a symbol has no value
*/
fun <T> QuadraticPolynomial<T>.evaluateQuadratic(
    values: Map<Symbol, T>,
    onMissing: ((Symbol) -> T?)? = null
): T? where T : Ring<T> {
    var value = constant
    for (monomial in monomials) {
        val v1 = values[monomial.symbol1] ?: onMissing?.invoke(monomial.symbol1) ?: return null
        var term = monomial.coefficient * v1
        if (monomial.symbol2 != null) {
            val v2 = values[monomial.symbol2] ?: onMissing?.invoke(monomial.symbol2) ?: return null
            term *= v2
        }
        value += term
    }
    return value
}

/**
 * 使用有序符号和值对二次多项式求值
 * Evaluate a quadratic polynomial with ordered symbols and values.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param values 与符号顺序对应的值列表 / List of values corresponding to symbol order
 * @return 求值结果 / Evaluation result
*/
fun <T> QuadraticPolynomial<T>.evaluateQuadraticOrdered(
    order: List<Symbol>,
    values: List<T>
): Ret<T> where T : Ring<T> {
    val indexOfSymbol = when (val result = buildOrderedSymbolIndex(order, values.size)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    var value = constant
    for (monomial in monomials) {
        val i1 = indexOfSymbol[monomial.symbol1]
            ?: return Failed(ErrorCode.DataNotFound, "Symbol ${monomial.symbol1.name} not found in order.")
        var term = monomial.coefficient * values[i1]
        if (monomial.symbol2 != null) {
            val i2 = indexOfSymbol[monomial.symbol2]
                ?: return Failed(ErrorCode.DataNotFound, "Symbol ${monomial.symbol2.name} not found in order.")
            term *= values[i2]
        }
        value += term
    }
    return Ok(value)
}

/**
 * 对二次多项式进行部分求值
 * Partially evaluate a quadratic polynomial.
 *
 * 将已知符号的值代入，返回仅包含未知符号的二次多项式。
 * Substitutes known symbol values, returning a quadratic polynomial with only unknown symbols.
 *
 * @param values 已知符号到值的映射 / Map of known symbol to value
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 部分求值后的二次多项式 / Partially evaluated quadratic polynomial
*/
fun <T> QuadraticPolynomial<T>.partialEvaluateQuadratic(
    values: Map<Symbol, T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T> where T : Ring<T> {
    val remainedMonomials = ArrayList<QuadraticMonomial<T>>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        val v1 = values[monomial.symbol1]
        val v2 = if (monomial.symbol2 != null) values[monomial.symbol2] else null

        when {
            v1 != null && v2 != null -> {
                // Both symbols have values, term becomes constant / 两个符号都有值，项变为常数
                newConstant += monomial.coefficient * v1 * v2
            }
            v1 != null && monomial.symbol2 == null -> {
                // Only symbol1 has value and there's no symbol2 (linear term in quadratic form) / 仅 symbol1 有值且无 symbol2（二次形式中的线性项）
                newConstant += monomial.coefficient * v1
            }
            v1 != null -> {
                // Only symbol1 has value, symbol2 remains / 仅 symbol1 有值，symbol2 保留
                remainedMonomials.add(QuadraticMonomial(
                    coefficient = monomial.coefficient * v1,
                    symbol1 = monomial.symbol2!!,
                    symbol2 = null
                ))
            }
            v2 != null -> {
                // Only symbol2 has value, symbol1 remains / 仅 symbol2 有值，symbol1 保留
                remainedMonomials.add(QuadraticMonomial(
                    coefficient = monomial.coefficient * v2,
                    symbol1 = monomial.symbol1,
                    symbol2 = null
                ))
            }
            else -> {
                // Neither symbol has a value, keep monomial as-is / 两个符号都无值，保持单项式不变
                remainedMonomials.add(monomial)
            }
        }
    }
    return QuadraticPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineQuadraticTerms(zero, isZero, symbolComparator)
}
