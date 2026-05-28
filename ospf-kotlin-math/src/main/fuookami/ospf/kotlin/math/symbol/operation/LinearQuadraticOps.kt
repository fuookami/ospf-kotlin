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

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*


internal class QuadraticTermKey private constructor(
    val symbol1: Symbol,
    val symbol2: Symbol?,
    private val hash: Int
) {
    companion object {
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

private fun buildOrderedSymbolIndex(order: List<Symbol>, valuesSize: Int): Map<Symbol, Int> {
    require(order.size == valuesSize) {
        "Order and values size mismatch: order.size=${order.size}, values.size=$valuesSize."
    }
    val indexOfSymbol = LinkedHashMap<Symbol, Int>(order.size)
    for ((index, symbol) in order.withIndex()) {
        require(indexOfSymbol.put(symbol, index) == null) {
            "Symbol order contains duplicated symbols."
        }
    }
    return indexOfSymbol
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
): T where T : Ring<T> {
    val indexOfSymbol = buildOrderedSymbolIndex(order, values.size)
    var value = constant
    for (monomial in monomials) {
        val index = indexOfSymbol[monomial.symbol]
            ?: throw IllegalArgumentException("Symbol ${monomial.symbol.name} not found in order.")
        value += monomial.coefficient * values[index]
    }
    return value
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
): T where T : Ring<T> {
    val indexOfSymbol = buildOrderedSymbolIndex(order, values.size)
    var value = constant
    for (monomial in monomials) {
        val i1 = indexOfSymbol[monomial.symbol1]
            ?: throw IllegalArgumentException("Symbol ${monomial.symbol1.name} not found in order.")
        var term = monomial.coefficient * values[i1]
        if (monomial.symbol2 != null) {
            val i2 = indexOfSymbol[monomial.symbol2]
                ?: throw IllegalArgumentException("Symbol ${monomial.symbol2.name} not found in order.")
            term *= values[i2]
        }
        value += term
    }
    return value
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
                // Both symbols have values, term becomes constant
                newConstant += monomial.coefficient * v1 * v2
            }
            v1 != null && monomial.symbol2 == null -> {
                // Only symbol1 has value and there's no symbol2 (linear term in quadratic form)
                newConstant += monomial.coefficient * v1
            }
            v1 != null -> {
                // Only symbol1 has value, symbol2 remains
                remainedMonomials.add(QuadraticMonomial(
                    coefficient = monomial.coefficient * v1,
                    symbol1 = monomial.symbol2!!,
                    symbol2 = null
                ))
            }
            v2 != null -> {
                // Only symbol2 has value, symbol1 remains
                remainedMonomials.add(QuadraticMonomial(
                    coefficient = monomial.coefficient * v2,
                    symbol1 = monomial.symbol1,
                    symbol2 = null
                ))
            }
            else -> {
                // Neither symbol has a value, keep monomial as-is
                remainedMonomials.add(monomial)
            }
        }
    }
    return QuadraticPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineQuadraticTerms(zero, isZero, symbolComparator)
}
