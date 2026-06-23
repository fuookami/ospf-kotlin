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

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret

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

/**
 * 将符号顺序列表编译为符号-索引映射
 * Compile an ordered symbol list into a symbol-to-index map
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @return 符号到索引的映射结果 / Result containing map from symbol to its index
 */
private fun compileOrderIndex(order: List<Symbol>): Ret<Map<Symbol, Int>> {
    if (order.toSet().size != order.size) {
        return Failed(ErrorCode.IllegalArgument, "Symbol order contains duplicated symbols.")
    }
    return Ok(order.withIndex().associate { it.value to it.index })
}

/**
 * 验证值列表大小与预期一致
 * Assert that the value list size matches the expected size
 *
 * @param values 值列表 / Value list
 * @param expectedSize 预期大小 / Expected size
 */
private fun requireValuesSize(
    values: List<*>,
    expectedSize: Int
) {
    require(values.size == expectedSize) {
        "Order and values size mismatch: order.size=$expectedSize, values.size=${values.size}."
    }
}

/**
 * 从索引映射中获取符号的位置，不存在时抛出异常
 * Get the index of a symbol from the index map, throwing if not found
 *
 * @param symbol 目标符号 / Target symbol
 * @param indexOfSymbol 符号-索引映射 / Symbol-to-index map
 * @return 符号在顺序中的索引 / Index of the symbol in the order
 */
private fun requireSymbolIndex(
    symbol: Symbol,
    indexOfSymbol: Map<Symbol, Int>
): Ret<Int> {
    return indexOfSymbol[symbol]
        ?.let { Ok(it) }
        ?: Failed(ErrorCode.DataNotFound, "Symbol ${symbol.name} not found in order.")
}

/**
 * 将线性多项式编译为求值函数
 * Compile a linear polynomial into an evaluation function.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param combineTerms 是否先合并同类项 / Whether to combine like terms first
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @return 接受值列表并返回求值结果的函数 / Function accepting a value list and returning the evaluation result
 */
fun <T> LinearPolynomial<T>.compileEvalLinear(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): Ret<(List<T>) -> T> where T : Ring<T> {
    val indexOfSymbol = when (val result = compileOrderIndex(order)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val source = if (combineTerms) {
        combineLinearTerms(zero, isZero)
    } else {
        this
    }
    val expectedSize = order.size
    val monomials = ArrayList<CompiledLinearMonomial<T>>(source.monomials.size)
    for (monomial in source.monomials) {
        val symbolIndex = when (val result = requireSymbolIndex(monomial.symbol, indexOfSymbol)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        monomials.add(
            CompiledLinearMonomial(
                coefficient = monomial.coefficient,
                symbolIndex = symbolIndex
            )
        )
    }
    val constant = source.constant
    return Ok { values ->
        requireValuesSize(values, expectedSize)
        var result = constant
        for (monomial in monomials) {
            result += monomial.coefficient * values[monomial.symbolIndex]
        }
        result
    }
}

/**
 * 将二次多项式编译为求值函数
 * Compile a quadratic polynomial into an evaluation function.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param combineTerms 是否先合并同类项 / Whether to combine like terms first
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 接受值列表并返回求值结果的函数 / Function accepting a value list and returning the evaluation result
 */
fun <T> QuadraticPolynomial<T>.compileEvalQuadratic(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): Ret<(List<T>) -> T> where T : Ring<T> {
    val indexOfSymbol = when (val result = compileOrderIndex(order)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val source = if (combineTerms) {
        combineQuadraticTerms(zero, isZero, symbolComparator)
    } else {
        this
    }
    val expectedSize = order.size
    val monomials = ArrayList<CompiledQuadraticMonomial<T>>(source.monomials.size)
    for (monomial in source.monomials) {
        val symbol1Index = when (val result = requireSymbolIndex(monomial.symbol1, indexOfSymbol)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val symbol2Index = if (monomial.symbol2 != null) {
            when (val result = requireSymbolIndex(monomial.symbol2, indexOfSymbol)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        } else {
            null
        }
        monomials.add(
            CompiledQuadraticMonomial(
                coefficient = monomial.coefficient,
                symbol1Index = symbol1Index,
                symbol2Index = symbol2Index
            )
        )
    }
    val constant = source.constant
    return Ok { values ->
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
 * 将规范多项式编译为求值函数
 * Compile a canonical polynomial into an evaluation function.
 *
 * 需要提供乘法单位元 one 用于幂运算。
 * Requires one (multiplicative identity) for power computation.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param combineTerms 是否先合并同类项 / Whether to combine like terms first
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @param one 乘法单位元 / The multiplicative identity
 * @return 接受值列表并返回求值结果的函数 / Function accepting a value list and returning the evaluation result
 */
fun <T> CanonicalPolynomial<T>.compileEvalCanonical(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null,
    one: T
): Ret<(List<T>) -> T> where T : Ring<T> {
    val indexOfSymbol = when (val result = compileOrderIndex(order)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val source = if (combineTerms) {
        combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
    } else {
        this
    }
    val expectedSize = order.size
    val monomials = ArrayList<CompiledCanonicalMonomial<T>>(source.monomials.size)
    for (monomial in source.monomials) {
        val powers = ArrayList<Pair<Int, Int32>>(monomial.powers.size)
        for ((symbol, exp) in monomial.powers) {
            if (exp.toInt() < 0) {
                return Failed(ErrorCode.IllegalArgument, "Negative exponent is not supported by compiled canonical evaluation.")
            }
            val symbolIndex = when (val result = requireSymbolIndex(symbol, indexOfSymbol)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            powers.add(Pair(symbolIndex, exp))
        }
        monomials.add(
            CompiledCanonicalMonomial(
                coefficient = monomial.coefficient,
                powers = powers
            )
        )
    }
    val constant = source.constant
    return Ok { values ->
        requireValuesSize(values, expectedSize)
        var result = constant
        for (monomial in monomials) {
            var monomialValue = monomial.coefficient
            for ((symbolIndex, power) in monomial.powers) {
                monomialValue *= computeNonNegativeRingPower(values[symbolIndex], power.toInt(), one)
            }
            result += monomialValue
        }
        result
    }
}

/**
 * 将规范多项式编译为求值函数（自动推断乘法单位元）
 * Compile a canonical polynomial into an evaluation function (infer one).
 *
 * 需要 Arithmetic<T> 约束以访问 constants.one。
 * Requires Arithmetic<T> to access constants.one.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param combineTerms 是否先合并同类项 / Whether to combine like terms first
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 接受值列表并返回求值结果的函数 / Function accepting a value list and returning the evaluation result
 */
fun <T> CanonicalPolynomial<T>.compileEvalCanonical(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): Ret<(List<T>) -> T> where T : Ring<T>, T : Arithmetic<T> {
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

/**
 * 通过重复加法将值缩放指定整数倍
 * Scale a value by a non-negative integer via repeated addition
 *
 * @param value 要缩放的值 / Value to scale
 * @param amount 缩放倍数（非负） / Scale amount (non-negative)
 * @param zero 类型零值 / Zero value for the type
 * @return 缩放后的值 / Scaled result
 */
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
 * 将线性多项式编译为梯度函数
 * Compile a linear polynomial's gradient.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param combineTerms 是否先合并同类项 / Whether to combine like terms first
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @return 接受值列表并返回梯度向量的函数 / Function accepting a value list and returning the gradient vector
 */
fun <T> LinearPolynomial<T>.compileGradientLinear(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): Ret<(List<T>) -> List<T>> where T : Ring<T> {
    val indexOfSymbol = when (val result = compileOrderIndex(order)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val source = if (combineTerms) {
        combineLinearTerms(zero, isZero)
    } else {
        this
    }
    val expectedSize = order.size
    val gradient = MutableList(expectedSize) { zero }
    for (monomial in source.monomials) {
        val symbolIndex = when (val result = requireSymbolIndex(monomial.symbol, indexOfSymbol)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        gradient[symbolIndex] += monomial.coefficient
    }
    val compiledGradient = gradient.toList()
    return Ok { values ->
        requireValuesSize(values, expectedSize)
        compiledGradient
    }
}

/**
 * 将二次多项式编译为梯度函数
 * Compile a quadratic polynomial's gradient.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param combineTerms 是否先合并同类项 / Whether to combine like terms first
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 接受值列表并返回梯度向量的函数 / Function accepting a value list and returning the gradient vector
 */
fun <T> QuadraticPolynomial<T>.compileGradientQuadratic(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): Ret<(List<T>) -> List<T>> where T : Ring<T> {
    val indexOfSymbol = when (val result = compileOrderIndex(order)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val source = if (combineTerms) {
        combineQuadraticTerms(zero, isZero, symbolComparator)
    } else {
        this
    }
    val expectedSize = order.size
    val baseGradient = MutableList(expectedSize) { zero }
    val quadraticMonomials = ArrayList<CompiledQuadraticMonomial<T>>(source.monomials.size)
    for (monomial in source.monomials) {
        val symbol1Index = when (val result = requireSymbolIndex(monomial.symbol1, indexOfSymbol)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val symbol2Index = if (monomial.symbol2 != null) {
            when (val result = requireSymbolIndex(monomial.symbol2, indexOfSymbol)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        } else {
            null
        }
        val compiled = CompiledQuadraticMonomial(
            coefficient = monomial.coefficient,
            symbol1Index = symbol1Index,
            symbol2Index = symbol2Index
        )
        if (compiled.symbol2Index == null) {
            baseGradient[compiled.symbol1Index] += compiled.coefficient
        } else {
            quadraticMonomials.add(compiled)
        }
    }
    return Ok { values ->
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
 * 将规范多项式编译为梯度函数
 * Compile a canonical polynomial's gradient.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param combineTerms 是否先合并同类项 / Whether to combine like terms first
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 接受值列表并返回梯度向量的函数 / Function accepting a value list and returning the gradient vector
 */
fun <T> CanonicalPolynomial<T>.compileGradientCanonical(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): Ret<(List<T>) -> List<T>> where T : Ring<T> {
    val indexOfSymbol = when (val result = compileOrderIndex(order)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val source = if (combineTerms) {
        combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
    } else {
        this
    }
    val expectedSize = order.size
    val monomials = ArrayList<CompiledCanonicalGradientMonomial<T>>(source.monomials.size)
    for (monomial in source.monomials) {
        val factorCounts = ArrayList<Pair<Int, Int32>>(monomial.powers.size)
        for ((symbol, power) in monomial.powers) {
            if (power.toInt() < 0) {
                return Failed(ErrorCode.IllegalArgument, "Negative exponent is not supported by compiled canonical gradient.")
            }
            val symbolIndex = when (val result = requireSymbolIndex(symbol, indexOfSymbol)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            factorCounts.add(Pair(symbolIndex, power))
        }
        monomials.add(
            CompiledCanonicalGradientMonomial(
                coefficient = monomial.coefficient,
                factorCounts = factorCounts
            )
        )
    }
    return Ok { values ->
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
