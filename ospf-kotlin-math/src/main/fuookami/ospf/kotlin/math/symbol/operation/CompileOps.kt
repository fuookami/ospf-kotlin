/**
 * 编译运算
 * Compile Operations
 *
 * 提供将多项式编译为高效求值函数的核心实现。
 * 支持编译求值函数和梯度函数，使用预计算的索引映射避免运行时查找开销。
 * Provides core implementation for compiling polynomials into efficient evaluation functions.
 * Supports compiling evaluation and gradient functions,
 * using pre-computed index mapping to avoid runtime lookup overhead.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

// ============================================================================
// Compiled Evaluation Operations (Ring-based, no Generic conversion)
// ============================================================================

private data class CompiledLinearMonomial<T>(
    val coefficient: T,
    val symbolIndex: Int
) where T : Ring<T>

private data class CompiledQuadraticMonomial<T>(
    val coefficient: T,
    val symbol1Index: Int,
    val symbol2Index: Int?
) where T : Ring<T>

private data class CompiledCanonicalMonomial<T>(
    val coefficient: T,
    val powers: List<Pair<Int, Int32>>  // (symbolIndex, exponent)
) where T : Ring<T>

private data class CompiledCanonicalGradientMonomial<T>(
    val coefficient: T,
    val factorCounts: List<Pair<Int, Int32>>
) where T : Ring<T>

private fun compileOrderIndex(order: List<Symbol>): Map<Symbol, Int> {
    require(order.toSet().size == order.size) {
        "Symbol order contains duplicated symbols."
    }
    return order.withIndex().associate { it.value to it.index }
}

private fun requireValuesSize(
    values: List<*>,
    expectedSize: Int
) {
    require(values.size == expectedSize) {
        "Order and values size mismatch: order.size=$expectedSize, values.size=${values.size}."
    }
}

private fun requireSymbolIndex(
    symbol: Symbol,
    indexOfSymbol: Map<Symbol, Int>
): Int {
    return indexOfSymbol[symbol]
        ?: throw IllegalArgumentException("Symbol ${symbol.name} not found in order.")
}

/**
 * Compile a linear polynomial into an evaluation function.
 */
fun <T> LinearPolynomial<T>.compileEvalLinear(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): (List<T>) -> T where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineLinearTerms(zero, isZero)
    } else {
        this
    }
    val expectedSize = order.size
    val monomials = source.monomials.map {
        CompiledLinearMonomial(
            coefficient = it.coefficient,
            symbolIndex = requireSymbolIndex(it.symbol, indexOfSymbol)
        )
    }
    val constant = source.constant
    return { values ->
        requireValuesSize(values, expectedSize)
        var result = constant
        for (monomial in monomials) {
            result += monomial.coefficient * values[monomial.symbolIndex]
        }
        result
    }
}

/**
 * Compile a quadratic polynomial into an evaluation function.
 */
fun <T> QuadraticPolynomial<T>.compileEvalQuadratic(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): (List<T>) -> T where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineQuadraticTerms(zero, isZero, symbolComparator)
    } else {
        this
    }
    val expectedSize = order.size
    val monomials = source.monomials.map {
        CompiledQuadraticMonomial(
            coefficient = it.coefficient,
            symbol1Index = requireSymbolIndex(it.symbol1, indexOfSymbol),
            symbol2Index = it.symbol2?.let { symbol -> requireSymbolIndex(symbol, indexOfSymbol) }
        )
    }
    val constant = source.constant
    return { values ->
        requireValuesSize(values, expectedSize)
        var result = constant
        for (monomial in monomials) {
            if (monomial.symbol2Index == null) {
                result += monomial.coefficient * values[monomial.symbol1Index]
            } else {
                result += monomial.coefficient * values[monomial.symbol1Index] * values[monomial.symbol2Index]
            }
        }
        result
    }
}

/**
 * Compile a canonical polynomial into an evaluation function.
 * Requires one (multiplicative identity) for power computation.
 */
fun <T> CanonicalPolynomial<T>.compileEvalCanonical(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null,
    one: T
): (List<T>) -> T where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
    } else {
        this
    }
    val expectedSize = order.size
    val monomials = source.monomials.map { monomial ->
        CompiledCanonicalMonomial(
            coefficient = monomial.coefficient,
            powers = monomial.powers.map { (symbol, exp) ->
                Pair(requireSymbolIndex(symbol, indexOfSymbol), exp)
            }
        )
    }
    val constant = source.constant
    return { values ->
        requireValuesSize(values, expectedSize)
        var result = constant
        for (monomial in monomials) {
            var monomialValue = monomial.coefficient
            for ((symbolIndex, power) in monomial.powers) {
                monomialValue *= computeRingPower(values[symbolIndex], power.toInt(), one)
            }
            result += monomialValue
        }
        result
    }
}

/**
 * Compile a canonical polynomial into an evaluation function (infer one).
 * Requires Arithmetic<T> to access constants.one.
 */
fun <T> CanonicalPolynomial<T>.compileEvalCanonical(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): (List<T>) -> T where T : Ring<T>, T : Arithmetic<T> {
    return compileEvalCanonical(
        order = order,
        combineTerms = combineTerms,
        zero = zero,
        isZero = isZero,
        symbolComparator = symbolComparator,
        one = constant.constants.one
    )
}

// ============================================================================
// Compiled Gradient Operations (Ring-based, no Generic conversion)
// ============================================================================

private fun <T> scaleByInt(
    value: T,
    amount: Int,
    zero: T
): T where T : Ring<T> {
    require(amount >= 0) {
        "Scale amount must be non-negative, but got $amount."
    }
    var result = zero
    repeat(amount) { result += value }
    return result
}

/**
 * Compile a linear polynomial's gradient.
 */
fun <T> LinearPolynomial<T>.compileGradientLinear(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): (List<T>) -> List<T> where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineLinearTerms(zero, isZero)
    } else {
        this
    }
    val expectedSize = order.size
    val gradient = MutableList(expectedSize) { zero }
    for (monomial in source.monomials) {
        val symbolIndex = requireSymbolIndex(monomial.symbol, indexOfSymbol)
        gradient[symbolIndex] += monomial.coefficient
    }
    val compiledGradient = gradient.toList()
    return { values ->
        requireValuesSize(values, expectedSize)
        compiledGradient
    }
}

/**
 * Compile a quadratic polynomial's gradient.
 */
fun <T> QuadraticPolynomial<T>.compileGradientQuadratic(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): (List<T>) -> List<T> where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineQuadraticTerms(zero, isZero, symbolComparator)
    } else {
        this
    }
    val expectedSize = order.size
    val baseGradient = MutableList(expectedSize) { zero }
    val quadraticMonomials = ArrayList<CompiledQuadraticMonomial<T>>(source.monomials.size)
    for (monomial in source.monomials) {
        val compiled = CompiledQuadraticMonomial(
            coefficient = monomial.coefficient,
            symbol1Index = requireSymbolIndex(monomial.symbol1, indexOfSymbol),
            symbol2Index = monomial.symbol2?.let { symbol -> requireSymbolIndex(symbol, indexOfSymbol) }
        )
        if (compiled.symbol2Index == null) {
            baseGradient[compiled.symbol1Index] += compiled.coefficient
        } else {
            quadraticMonomials.add(compiled)
        }
    }
    return { values ->
        requireValuesSize(values, expectedSize)
        val gradient = baseGradient.toMutableList()
        for (monomial in quadraticMonomials) {
            val symbol2Index = monomial.symbol2Index!!
            if (monomial.symbol1Index == symbol2Index) {
                gradient[monomial.symbol1Index] +=
                    (monomial.coefficient + monomial.coefficient) * values[monomial.symbol1Index]
            } else {
                gradient[monomial.symbol1Index] += monomial.coefficient * values[symbol2Index]
                gradient[symbol2Index] += monomial.coefficient * values[monomial.symbol1Index]
            }
        }
        gradient
    }
}

/**
 * Compile a canonical polynomial's gradient.
 */
fun <T> CanonicalPolynomial<T>.compileGradientCanonical(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): (List<T>) -> List<T> where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
    } else {
        this
    }
    val expectedSize = order.size
    val monomials = source.monomials.map { monomial ->
        CompiledCanonicalGradientMonomial(
            coefficient = monomial.coefficient,
            factorCounts = monomial.powers.entries.map {
                Pair(requireSymbolIndex(it.key, indexOfSymbol), it.value)
            }
        )
    }
    return { values ->
        requireValuesSize(values, expectedSize)
        val gradient = MutableList(expectedSize) { zero }
        for (monomial in monomials) {
            for ((targetIndex, amount) in monomial.factorCounts) {
                var derivative = scaleByInt(monomial.coefficient, amount.toInt(), zero)
                for ((index, power) in monomial.factorCounts) {
                    val repeat = if (index == targetIndex) {
                        power.toInt() - 1
                    } else {
                        power.toInt()
                    }
                    repeat(repeat) {
                        derivative *= values[index]
                    }
                }
                gradient[targetIndex] += derivative
            }
        }
        gradient
    }
}
