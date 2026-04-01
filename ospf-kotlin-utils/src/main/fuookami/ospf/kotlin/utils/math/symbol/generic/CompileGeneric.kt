package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol

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
    val powers: List<Pair<Int, Int>>  // (symbolIndex, exponent)
) where T : Ring<T>

private data class CompiledCanonicalGradientMonomial<T>(
    val coefficient: T,
    val factorCounts: List<Pair<Int, Int>>
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

fun <T> GenericLinearPolynomial<T>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): (List<T>) -> T where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineTerms(zero = zero, isZero = isZero)
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

fun <T> GenericQuadraticPolynomial<T>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): (List<T>) -> T where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineTerms(
            zero = zero,
            isZero = isZero,
            symbolComparator = symbolComparator
        )
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
 * 计算幂次 value^power
 * Compute power value^power
 */
private fun <T : Ring<T>> computePower(value: T, power: Int, one: T): T {
    if (power == 0) return one
    if (power == 1) return value
    
    // 对于负指数，需要 T 实现 TimesGroup（有 reciprocal）
    // 这里我们只处理正指数
    if (power < 0) {
        throw IllegalArgumentException("Negative exponent requires TimesGroup implementation.")
    }
    
    var result = one
    var base = value
    var exp = power
    
    while (exp > 0) {
        if (exp % 2 == 1) {
            result = result * base
        }
        if (exp > 1) {
            base = base * base
        }
        exp /= 2
    }
    return result
}

fun <T> GenericCanonicalPolynomial<T>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null,
    one: T
): (List<T>) -> T where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineTerms(
            zero = zero,
            isZero = isZero,
            symbolComparator = symbolComparator
        )
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
                monomialValue *= computePower(values[symbolIndex], power, one)
            }
            result += monomialValue
        }
        result
    }
}

fun <T> GenericCanonicalPolynomial<T>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): (List<T>) -> T where T : Ring<T> {
    return compileEval(
        order = order,
        combineTerms = combineTerms,
        zero = zero,
        isZero = isZero,
        symbolComparator = symbolComparator,
        one = inferOneOrThrow(zero, constant)
    )
}

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

fun <T> GenericLinearPolynomial<T>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): (List<T>) -> List<T> where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineTerms(zero = zero, isZero = isZero)
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

fun <T> GenericQuadraticPolynomial<T>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): (List<T>) -> List<T> where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineTerms(
            zero = zero,
            isZero = isZero,
            symbolComparator = symbolComparator
        )
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

fun <T> GenericCanonicalPolynomial<T>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): (List<T>) -> List<T> where T : Ring<T> {
    val indexOfSymbol = compileOrderIndex(order)
    val source = if (combineTerms) {
        combineTerms(
            zero = zero,
            isZero = isZero,
            symbolComparator = symbolComparator
        )
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
                var derivative = scaleByInt(monomial.coefficient, amount, zero)
                for ((index, power) in monomial.factorCounts) {
                    val repeat = if (index == targetIndex) {
                        power - 1
                    } else {
                        power
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
@Suppress("UNCHECKED_CAST")
private fun <T : Ring<T>> inferOneOrThrow(vararg candidates: Any?): T {
    for (candidate in candidates) {
        if (candidate != null) {
            val arithmetic = candidate as? Arithmetic<T>
            if (arithmetic != null) {
                return arithmetic.constants.one
            }
        }
    }
    throw IllegalArgumentException("Cannot infer multiplicative identity, please pass parameter one explicitly.")
}
