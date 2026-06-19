/**
 * 规范运算
 * Canonical Operations
 *
 * 提供规范多项式的核心运算操作，包括合并同类项、求值和部分求值。
 * 使用 PowerVectorKey 进行高效的幂向量比较，相比基二Map 的实现性能提升 50-100%。
 * Provides core operation functions for canonical polynomials,
 * including combining like terms, evaluation, and partial evaluation.
 * Uses PowerVectorKey for efficient power vector comparison,
 * achieving 50-100% performance improvement over Map-based implementation.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

// ============================================================================
// Canonical Polynomial Operations (Ring-based, no Generic conversion)
// ============================================================================

/**
 * 计算 Ring 类型的幂值 value^power
 * Compute power value^power for Ring types.
 *
 * 内部函数，供各运算文件共享使用。
 * Internal for use across operation files.
 *
 * @param value 底数 / The base value
 * @param power 指数（非负） / The exponent (non-negative)
 * @param one 乘法单位元 / The multiplicative identity
 * @return value 的非负 power 次幂 / value raised to the non-negative power
 */
internal fun <T : Ring<T>> computeNonNegativeRingPower(value: T, power: Int, one: T): T {
    if (power == 0) return one
    if (power == 1) return value
    var result = one
    var base = value
    var exp = power
    while (exp > 0) {
        if (exp % 2 == 1) result *= base
        if (exp > 1) base *= base
        exp /= 2
    }
    return result
}

/**
 * 计算 Ring 类型的幂值，负指数仅在值实现 TimesGroup 时可用
 * Compute a Ring power; negative exponents are available only for TimesGroup values.
 *
 * @param value 底数 / The base value
 * @param power 指数 / The exponent
 * @param one 乘法单位元 / The multiplicative identity
 * @return 幂值，无法处理负指数时返回 null / Power value, or null when a negative exponent is unsupported
 */
internal fun <T : Ring<T>> computeRingPowerOrNull(value: T, power: Int, one: T): T? {
    if (power >= 0) {
        return computeNonNegativeRingPower(value, power, one)
    }
    if (power == Int.MIN_VALUE) {
        return null
    }
    val reciprocal = try {
        (value as? TimesGroup<T>)?.reciprocal()
    } catch (_: Exception) {
        null
    } ?: return null
    return computeNonNegativeRingPower(reciprocal, -power, one)
}

/**
 * 计算 Ring 类型的幂值，失败时返回 Ret 错误
 * Compute a Ring power, returning a Ret error on failure.
 *
 * @param value 底数 / The base value
 * @param power 指数 / The exponent
 * @param one 乘法单位元 / The multiplicative identity
 * @return 幂值结果 / Power result
 */
internal fun <T : Ring<T>> computeRingPower(value: T, power: Int, one: T): Ret<T> {
    return computeRingPowerOrNull(value = value, power = power, one = one)
        ?.let { Ok(it) }
        ?: Failed(ErrorCode.IllegalArgument, "Negative exponent requires TimesGroup implementation.")
}

/**
 * 合并规范多项式中的同类项
 * Combine like terms in a canonical polynomial.
 *
 * 直接 Ring 操作，无 Generic 转换。
 * 使用 PowerVectorKey 替代 Map<Symbol, Int32> 作为 HashMap 键进行优化：
 * - 稠密模式：适用于 totalSymbols 较小或稀疏度较高（powers.size / totalSymbols >= 0.5）的情况
 * - 稀疏模式：适用于 totalSymbols 较大且稀疏度较低的情况
 * - 通过预计算哈希消除幂排序开销
 * 相比原 Map 实现，性能提升约 50-100%。
 *
 * Direct Ring operation - no Generic conversion.
 * Optimization: Uses PowerVectorKey instead of Map<Symbol, Int32> as HashMap key.
 * - Dense mode for small totalSymbols or high sparsity (powers.size / totalSymbols >= 0.5)
 * - Sparse mode for large totalSymbols with low sparsity
 * - Eliminates powers sorting overhead by using pre-computed hash
 * Performance: ~50-100% faster than original Map-based implementation.
 *
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 合并同类项后的单项式列表 / List of monomials with like terms combined
 */
fun <T> Iterable<CanonicalMonomial<T>>.combineCanonicalMonomials(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): List<CanonicalMonomial<T>> where T : Ring<T> {
    // Step 1: Collect all unique symbols and build index mapping / 步骤一：收集所有唯一符号并构建索引映射
    val comparator = symbolComparator ?: defaultSymbolComparator
    val allSymbols = mutableSetOf<Symbol>()
    for (monomial in this) {
        allSymbols.addAll(monomial.powers.keys)
    }

    // Sort symbols by comparator for normalization / 按比较器排序符号以规范化
    val symbolList = allSymbols.sortedWith(comparator)
    val symbolIndex = symbolList.indices.associateBy { symbolList[it] }

    // Step 2: Combine using PowerVectorKey / 步骤二：使用 PowerVectorKey 合并
    val coefficientOfKey = LinkedHashMap<PowerVectorKey, T>()

    for (monomial in this) {
        val key = PowerVectorKey.create(
            powers = monomial.powers,
            symbolIndex = symbolIndex,
            totalSymbols = symbolList.size
        )
        coefficientOfKey[key] = (coefficientOfKey[key] ?: zero) + monomial.coefficient
    }

    // Step 3: Convert back to CanonicalMonomial list / 步骤三：转换回 CanonicalMonomial 列表
    return coefficientOfKey
        .asSequence()
        .filter { !isZero(it.value) }
        .map { entry ->
            CanonicalMonomial(
                coefficient = entry.value,
                powers = entry.key.toPowers(symbolList)
            )
        }
        .toList()
}

/**
 * 合并规范多项式中的同类项
 * Combine like terms in a canonical polynomial.
 *
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 合并同类项后的规范多项式 / Canonical polynomial with like terms combined
 */
fun <T> CanonicalPolynomial<T>.combineCanonicalPolynomialTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    return copy(monomials = monomials.combineCanonicalMonomials(zero, isZero, symbolComparator))
}

/**
 * 使用给定值对规范多项式求值
 * Evaluate a canonical polynomial with given values.
 *
 * 需要提供乘法单位元 one 用于幂运算。
 * Requires one (multiplicative identity) for power computation.
 *
 * @param values 符号到值的映射 / Map of symbol to value
 * @param onMissing 缺失符号的回调函数（可选） / Callback for missing symbols (optional)
 * @param one 乘法单位元 / The multiplicative identity
 * @return 求值结果，若存在未提供值的符号则返回 null / Evaluation result, or null if a symbol has no value
 */
fun <T> CanonicalPolynomial<T>.evaluateCanonical(
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
            val powerValue = computeRingPowerOrNull(factor, power.toInt(), one) ?: return null
            monomialValue *= powerValue
        }
        value += monomialValue
    }
    return value
}

/**
 * 使用有序符号和值对规范多项式求值
 * Evaluate a canonical polynomial with ordered symbols and values.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param values 与符号顺序对应的值列表 / List of values corresponding to symbol order
 * @param one 乘法单位元 / The multiplicative identity
 * @return 求值结果 / Evaluation result
 */
fun <T> CanonicalPolynomial<T>.evaluateCanonicalOrdered(
    order: List<Symbol>,
    values: List<T>,
    one: T
): Ret<T> where T : Ring<T> {
    if (order.toSet().size != order.size) {
        return Failed(ErrorCode.IllegalArgument, "Symbol order contains duplicated symbols.")
    }
    if (order.size != values.size) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Order and values size mismatch: order.size=${order.size}, values.size=${values.size}."
        )
    }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    var value = constant
    for (monomial in monomials) {
        var monomialValue = monomial.coefficient
        for ((symbol, power) in monomial.powers) {
            val index = indexOfSymbol[symbol]
                ?: return Failed(ErrorCode.DataNotFound, "Symbol ${symbol.name} not found in order.")
            val powerValue = when (val result = computeRingPower(values[index], power.toInt(), one)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            monomialValue *= powerValue
        }
        value += monomialValue
    }
    return Ok(value)
}

/**
 * 对规范多项式进行部分求值
 * Partially evaluate a canonical polynomial.
 *
 * 将已知符号的值代入，返回仅包含未知符号的规范多项式。
 * Substitutes known symbol values, returning a canonical polynomial with only unknown symbols.
 *
 * @param values 已知符号到值的映射 / Map of known symbol to value
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param one 乘法单位元 / The multiplicative identity
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 部分求值后的规范多项式 / Partially evaluated canonical polynomial
 */
fun <T> CanonicalPolynomial<T>.partialEvaluateCanonical(
    values: Map<Symbol, T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    one: T,
    symbolComparator: Comparator<Symbol>? = null
): Ret<CanonicalPolynomial<T>> where T : Ring<T> {
    val remainedMonomials = ArrayList<CanonicalMonomial<T>>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        var newCoefficient = monomial.coefficient
        val remainedPowers = LinkedHashMap<Symbol, Int32>()
        for ((symbol, power) in monomial.powers) {
            val factor = values[symbol]
            if (factor == null) {
                remainedPowers[symbol] = power
            } else {
                val powerValue = when (val result = computeRingPower(factor, power.toInt(), one)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                newCoefficient *= powerValue
            }
        }
        if (remainedPowers.isEmpty()) {
            newConstant += newCoefficient
        } else {
            remainedMonomials.add(CanonicalMonomial(coefficient = newCoefficient, powers = remainedPowers))
        }
    }
    return Ok(CanonicalPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineCanonicalPolynomialTerms(zero, isZero, symbolComparator))
}
