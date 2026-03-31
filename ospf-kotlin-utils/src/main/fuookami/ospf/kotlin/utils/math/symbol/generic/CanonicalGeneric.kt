package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial

/**
 * 泛型标准单项式 / Generic canonical monomial
 *
 * 形式：`c * S1^n1 * S2^n2 * ...`
 * Form: `c * S1^n1 * S2^n2 * ...`
 *
 * @param T 系数类型，需要实现 Ring
 * @param powers 符号到指数的映射，支持负指数
 */
data class GenericCanonicalMonomial<T>(
    val coefficient: T,
    val powers: Map<Symbol, Int> = emptyMap()
) where T : Ring<T> {
    /**
     * 获取总次数（所有指数之和）
     * Get the total degree (sum of all exponents)
     * Note: For negative exponents, this may not be meaningful
     */
    val degree: Int
        get() = powers.values.sum()

    /**
     * 是否为常数项
     * Check if this is a constant term
     */
    val isConstant: Boolean
        get() = powers.isEmpty()

    /**
     * 获取符号数量
     * Get the number of symbols
     */
    val symbolCount: Int
        get() = powers.size

    companion object {
        /**
         * 创建常数项
         * Create a constant term
         */
        fun <T : Ring<T>> constant(coefficient: T): GenericCanonicalMonomial<T> {
            return GenericCanonicalMonomial(coefficient, emptyMap())
        }

        /**
         * 从 factors 列表创建（每个符号指数为1）
         * Create from factors list (each symbol has exponent 1)
         */
        fun <T : Ring<T>> fromFactors(coefficient: T, factors: List<Symbol>): GenericCanonicalMonomial<T> {
            val powers = factors.groupBy { it }.mapValues { it.value.size }
            return GenericCanonicalMonomial(coefficient, powers)
        }
    }
}

data class GenericCanonicalPolynomial<T>(
    val monomials: List<GenericCanonicalMonomial<T>> = emptyList(),
    val constant: T
) where T : Ring<T>

fun <T> Iterable<GenericCanonicalMonomial<T>>.combineCanonicalTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): List<GenericCanonicalMonomial<T>> where T : Ring<T> {
    val comparator = symbolComparator ?: defaultSymbolComparator
    val coefficientOfPowers = LinkedHashMap<Map<Symbol, Int>, T>()
    for (monomial in this) {
        // 对 powers 的 key 进行排序，确保相同符号组合可以合并
        val normalizedPowers = monomial.powers.entries
            .sortedWith(compareBy { comparator.compare(it.key, it.key) })
            .associate { it.key to it.value }
        coefficientOfPowers[normalizedPowers] =
            (coefficientOfPowers[normalizedPowers] ?: zero) + monomial.coefficient
    }
    return coefficientOfPowers
        .asSequence()
        .filter { !isZero(it.value) }
        .map { GenericCanonicalMonomial(coefficient = it.value, powers = it.key) }
        .toList()
}

fun <T> GenericCanonicalPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): GenericCanonicalPolynomial<T> where T : Ring<T> {
    return copy(monomials = monomials.combineCanonicalTerms(zero, isZero, symbolComparator))
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

fun <T> GenericCanonicalPolynomial<T>.evaluate(
    values: Map<Symbol, T>,
    onMissing: ((Symbol) -> T?)? = null,
    one: T
): T? where T : Ring<T> {
    var value = constant
    for (monomial in monomials) {
        var monomialValue = monomial.coefficient
        for ((symbol, power) in monomial.powers) {
            val factor = values[symbol]
                ?: onMissing?.invoke(symbol)
                ?: return null
            monomialValue *= computePower(factor, power, one)
        }
        value += monomialValue
    }
    return value
}

fun <T> GenericCanonicalPolynomial<T>.evaluateOrdered(
    order: List<Symbol>,
    values: List<T>,
    one: T
): T where T : Ring<T> {
    require(order.toSet().size == order.size) {
        "Symbol order contains duplicated symbols."
    }
    require(order.size == values.size) {
        "Order and values size mismatch: order.size=${order.size}, values.size=${values.size}."
    }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    var value = constant
    for (monomial in monomials) {
        var monomialValue = monomial.coefficient
        for ((symbol, power) in monomial.powers) {
            val index = indexOfSymbol[symbol]
                ?: throw IllegalArgumentException("Symbol ${symbol.name} not found in order.")
            monomialValue *= computePower(values[index], power, one)
        }
        value += monomialValue
    }
    return value
}

fun <T> GenericCanonicalPolynomial<T>.partialEvaluate(
    values: Map<Symbol, T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    one: T,
    symbolComparator: Comparator<Symbol>? = null
): GenericCanonicalPolynomial<T> where T : Ring<T> {
    val remainedMonomials = ArrayList<GenericCanonicalMonomial<T>>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        var newCoefficient = monomial.coefficient
        val remainedPowers = LinkedHashMap<Symbol, Int>()
        for ((symbol, power) in monomial.powers) {
            val factor = values[symbol]
            if (factor == null) {
                remainedPowers[symbol] = power
            } else {
                newCoefficient *= computePower(factor, power, one)
            }
        }
        if (remainedPowers.isEmpty()) {
            newConstant += newCoefficient
        } else {
            remainedMonomials.add(
                GenericCanonicalMonomial(
                    coefficient = newCoefficient,
                    powers = remainedPowers
                )
            )
        }
    }
    return GenericCanonicalPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineTerms(zero, isZero, symbolComparator)
}

// ============================================================================
// 转换函数 / Conversion functions
// ============================================================================

fun CanonicalMonomial<Flt64, Int32>.toGenericCanonicalMonomial(): GenericCanonicalMonomial<Flt64> {
    return GenericCanonicalMonomial(
        coefficient = coefficient,
        powers = powers.mapValues { it.value.toInt() }
    )
}

fun GenericCanonicalMonomial<Flt64>.toCanonicalMonomial(): CanonicalMonomial<Flt64, Int32> {
    return CanonicalMonomial<Flt64, Int32>(
        coefficient = coefficient,
        powers = powers.mapValues { Int32(it.value) }
    )
}

fun CanonicalPolynomial<Flt64, Int32>.toGenericCanonicalPolynomial(): GenericCanonicalPolynomial<Flt64> {
    return GenericCanonicalPolynomial(
        monomials = monomials.map { it.toGenericCanonicalMonomial() },
        constant = constant
    )
}

fun GenericCanonicalPolynomial<Flt64>.toCanonicalPolynomial(): CanonicalPolynomial<Flt64, Int32> {
    return CanonicalPolynomial<Flt64, Int32>(
        monomials = monomials.map { it.toCanonicalMonomial() },
        constant = constant
    )
}