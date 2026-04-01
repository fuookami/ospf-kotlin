package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.Int32
import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial

/**
 * ���ͱ�׼����ʽ / Generic canonical monomial
 *
 * ��ʽ��`c * S1^n1 * S2^n2 * ...`
 * Form: `c * S1^n1 * S2^n2 * ...`
 *
 * @param T ϵ�����ͣ���Ҫʵ�� Ring
 * @param powers ���ŵ�ָ����ӳ�䣬֧�ָ�ָ��
 */
data class GenericCanonicalMonomial<T>(
    val coefficient: T,
    val powers: Map<Symbol, Int> = emptyMap()
) where T : Ring<T> {
    constructor(
        coefficient: T,
        factors: List<Symbol>
    ) : this(
        coefficient = coefficient,
        powers = factors.groupingBy { it }.eachCount()
    )

    val factors: List<Symbol>
        get() = powers.entries.flatMap { (symbol, exp) -> List(exp) { symbol } }

    /**
     * ��ȡ�ܴ���������ָ��֮�ͣ�
     * Get the total degree (sum of all exponents)
     * Note: For negative exponents, this may not be meaningful
     */
    val degree: Int
        get() = powers.values.sum()

    /**
     * �Ƿ�Ϊ������
     * Check if this is a constant term
     */
    val isConstant: Boolean
        get() = powers.isEmpty()

    /**
     * ��ȡ��������
     * Get the number of symbols
     */
    val symbolCount: Int
        get() = powers.size

    companion object {
        /**
         * ����������
         * Create a constant term
         */
        fun <T : Ring<T>> constant(coefficient: T): GenericCanonicalMonomial<T> {
            return GenericCanonicalMonomial(coefficient, emptyMap())
        }

        /**
         * �� factors �б������ÿ������ָ��Ϊ1��
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
        // �� powers �� key ��������ȷ����ͬ������Ͽ��Ժϲ�
        val normalizedPowers = monomial.powers.entries
            .sortedWith { lhs, rhs -> comparator.compare(lhs.key, rhs.key) }
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
 * �����ݴ� value^power
 * Compute power value^power
 */
private fun <T : Ring<T>> computePower(value: T, power: Int, one: T): T {
    if (power == 0) return one
    if (power == 1) return value
    
    // ���ڸ�ָ������Ҫ T ʵ�� TimesGroup���� reciprocal��
    // ��������ֻ������ָ��
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

fun <T> GenericCanonicalPolynomial<T>.evaluate(
    values: Map<Symbol, T>,
    onMissing: ((Symbol) -> T?)? = null
): T? where T : Ring<T> {
    return evaluate(
        values = values,
        onMissing = onMissing,
        one = inferOneOrThrow(constant, values.values.firstOrNull())
    )
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

fun <T> GenericCanonicalPolynomial<T>.evaluateOrdered(
    order: List<Symbol>,
    values: List<T>
): T where T : Ring<T> {
    return evaluateOrdered(
        order = order,
        values = values,
        one = inferOneOrThrow(constant, values.firstOrNull())
    )
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

fun <T> GenericCanonicalPolynomial<T>.partialEvaluate(
    values: Map<Symbol, T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): GenericCanonicalPolynomial<T> where T : Ring<T> {
    return partialEvaluate(
        values = values,
        zero = zero,
        isZero = isZero,
        one = inferOneOrThrow(zero, constant, values.values.firstOrNull()),
        symbolComparator = symbolComparator
    )
}

// ============================================================================
// ת������ / Conversion functions
// ============================================================================

fun <T> CanonicalMonomial<T>.toGenericCanonicalMonomial(): GenericCanonicalMonomial<T>
        where T : NumberField<T> {
    return GenericCanonicalMonomial(
        coefficient = coefficient,
        powers = powers.mapValues { it.value.toInt() }
    )
}

fun <T> CanonicalPolynomial<T>.toGenericCanonicalPolynomial(): GenericCanonicalPolynomial<T>
        where T : NumberField<T> {
    return GenericCanonicalPolynomial(
        monomials = monomials.map { it.toGenericCanonicalMonomial() },
        constant = constant
    )
}

fun GenericCanonicalMonomial<Flt64>.toCanonicalMonomial(): CanonicalMonomial<Flt64> {
    return CanonicalMonomial<Flt64>(
        coefficient = coefficient,
        powers = powers.mapValues { Int32(it.value) }
    )
}

fun GenericCanonicalPolynomial<Flt64>.toCanonicalPolynomial(): CanonicalPolynomial<Flt64> {
    return CanonicalPolynomial<Flt64>(
        monomials = monomials.map { it.toCanonicalMonomial() },
        constant = constant
    )
}


