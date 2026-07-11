@file:Suppress("unused")
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Flt64 求值快捷函数
 * Flt64 Evaluation Convenience Functions
 *
 * 提供 Flt64 单项式和多项式的求值、有序求值、部分求值和区间极值计算。
 * 封装通用求值运算，支持 ValueProvider 和 Map 两种值来源。
 * Provides Flt64 monomial and polynomial evaluation, ordered evaluation,
 * partial evaluation, and interval extremum computation.
 * Wraps generic evaluation operations with ValueProvider and Map value sources.
*/

/** 构造符号缺失值的错误结果 / Construct an error result for a missing symbol value
 * @param symbol 缺失值的符号 / The symbol with missing value
 * @return 包含错误码和信息的 Failed 结果 / Failed result containing error code and message
*/
private fun missingValueFailed(symbol: Symbol): Ret<Flt64> {
    return Failed(ErrorCode.DataNotFound, "Missing value for symbol: ${symbol.name}")
}

/**
 * 根据值提供者和缺失值策略解析符号的值
 * Resolve a symbol's value from the provider according to the missing-value policy
 *
 * @param symbol 目标符号 / Target symbol
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 解析到的值，或在 ReturnNull 策略下返回 null / Resolved value, or null under ReturnNull policy
*/
private fun resolveValue(
    symbol: Symbol,
    provider: ValueProvider,
    policy: MissingValuePolicy
): Flt64? {
    val value = provider[symbol]
    return if (value != null) {
        value
    } else {
        when (policy) {
            MissingValuePolicy.AsZero -> Flt64.zero
            MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> null
        }
    }
}

/**
 * 根据值提供者和缺失值策略解析符号的值，返回 Ret 包装结果
 * Resolve a symbol's value from the provider, returning a Ret-wrapped result
 *
 * @param symbol 目标符号 / Target symbol
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 包装了值的 Ok 或错误的 Failed / Ok wrapping the value, or Failed on error
*/
private fun resolveValueRet(
    symbol: Symbol,
    provider: ValueProvider,
    policy: MissingValuePolicy
): Ret<Flt64> {
    val value = provider[symbol]
    return if (value != null) {
        Ok(value)
    } else {
        when (policy) {
            MissingValuePolicy.AsZero -> Ok(Flt64.zero)
            MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> missingValueFailed(symbol)
        }
    }
}

/**
 * 使用值提供者求值 Flt64 线性单项式
 * Evaluate a Flt64 linear monomial using a value provider
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果，若缺少值则返回 null / Evaluation result, or null if value is missing
*/
fun LinearMonomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    val symbolValue = resolveValue(symbol, provider, policy)
    return if (symbolValue != null) {
        coefficient * symbolValue
    } else {
        null
    }
}

/**
 * 使用 Map 求值 Flt64 线性单项式
 * Evaluate a Flt64 linear monomial using a Map
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果，若缺少值则返回 null / Evaluation result, or null if value is missing
*/
fun LinearMonomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

/**
 * 使用值提供者求值 Flt64 线性单项式（Ret 安全版本）
 * Evaluate a Flt64 linear monomial using a value provider (Ret-safe version)
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun LinearMonomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return when (val result = resolveValueRet(symbol, provider, policy)) {
        is Ok -> Ok(coefficient * result.value)
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 使用 Map 求值 Flt64 线性单项式（Ret 安全版本）
 * Evaluate a Flt64 linear monomial using a Map (Ret-safe version)
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun LinearMonomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

/**
 * 使用有序值求值 Flt64 线性单项式
 * Evaluate a Flt64 linear monomial using ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 求值结果 / Evaluation result
*/
fun LinearMonomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Ret<Flt64> {
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
    val index = indexOfSymbol[symbol]
        ?: return Failed(ErrorCode.DataNotFound, "Symbol ${symbol.name} not found in order.")
    return Ok(coefficient * values[index])
}

/**
 * 部分求值 Flt64 线性单项式
 * Partially evaluate a Flt64 linear monomial
 *
 * @param provider 值提供者 / Value provider
 * @return 部分求值后的线性多项式 / Partially evaluated linear polynomial
*/
fun LinearMonomial<Flt64>.partialEvaluate(provider: ValueProvider): LinearPolynomial<Flt64> {
    val symbolValue = provider[symbol]
    return if (symbolValue != null) {
        LinearPolynomial(
            monomials = emptyList(),
            constant = coefficient * symbolValue
        )
    } else {
        LinearPolynomial(
            monomials = listOf(this),
            constant = Flt64.zero
        )
    }
}

/**
 * 使用 Map 部分求值 Flt64 线性单项式
 * Partially evaluate a Flt64 linear monomial using a Map
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @return 部分求值后的线性多项式 / Partially evaluated linear polynomial
*/
fun LinearMonomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): LinearPolynomial<Flt64> {
    return partialEvaluate(MapValueProvider(values))
}

/**
 * 使用值提供者求值 Flt64 二次单项式
 * Evaluate a Flt64 quadratic monomial using a value provider
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果，若缺少值则返回 null / Evaluation result, or null if value is missing
*/
fun QuadraticMonomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    val v1 = resolveValue(symbol1, provider, policy)
    if (v1 == null) return null

    if (symbol2 == null) {
        return coefficient * v1
    }

    val v2 = resolveValue(symbol2, provider, policy)
    return if (v2 != null) {
        coefficient * v1 * v2
    } else {
        null
    }
}

/**
 * 使用 Map 求值 Flt64 二次单项式
 * Evaluate a Flt64 quadratic monomial using a Map
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果，若缺少值则返回 null / Evaluation result, or null if value is missing
*/
fun QuadraticMonomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

/**
 * 使用值提供者求值 Flt64 二次单项式（Ret 安全版本）
 * Evaluate a Flt64 quadratic monomial using a value provider (Ret-safe version)
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun QuadraticMonomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    val v1Result = resolveValueRet(symbol1, provider, policy)
    when (v1Result) {
        is Failed -> return Failed(v1Result.error)
        is Fatal -> return Fatal(v1Result.errors)
        is Ok -> {}
    }

    val v1 = v1Result.value!!

    if (symbol2 == null) {
        return Ok(coefficient * v1)
    }

    val v2Result = resolveValueRet(symbol2, provider, policy)
    return when (v2Result) {
        is Ok -> Ok(coefficient * v1 * v2Result.value)
        is Failed -> Failed(v2Result.error)
        is Fatal -> Fatal(v2Result.errors)
    }
}

/**
 * 使用 Map 求值 Flt64 二次单项式（Ret 安全版本）
 * Evaluate a Flt64 quadratic monomial using a Map (Ret-safe version)
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun QuadraticMonomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

/**
 * 使用有序值求值 Flt64 二次单项式
 * Evaluate a Flt64 quadratic monomial using ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 求值结果 / Evaluation result
*/
fun QuadraticMonomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Ret<Flt64> {
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
    val i1 = indexOfSymbol[symbol1]
        ?: return Failed(ErrorCode.DataNotFound, "Symbol ${symbol1.name} not found in order.")
    var result = coefficient * values[i1]
    if (symbol2 != null) {
        val i2 = indexOfSymbol[symbol2]
            ?: return Failed(ErrorCode.DataNotFound, "Symbol ${symbol2.name} not found in order.")
        result *= values[i2]
    }
    return Ok(result)
}

/**
 * 部分求值 Flt64 二次单项式
 * Partially evaluate a Flt64 quadratic monomial
 *
 * @param provider 值提供者 / Value provider
 * @return 部分求值后的二次多项式 / Partially evaluated quadratic polynomial
*/
fun QuadraticMonomial<Flt64>.partialEvaluate(provider: ValueProvider): QuadraticPolynomial<Flt64> {
    val v1 = provider[symbol1]
    val v2 = if (symbol2 != null) provider[symbol2] else null

    return when {
        v1 != null && v2 != null -> {
            QuadraticPolynomial(
                monomials = emptyList(),
                constant = coefficient * v1 * v2
            )
        }
        v1 != null && symbol2 == null -> {
            QuadraticPolynomial(
                monomials = emptyList(),
                constant = coefficient * v1
            )
        }
        v1 != null -> {
            QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(
                    coefficient = coefficient * v1,
                    symbol1 = symbol2!!,
                    symbol2 = null
                )),
                constant = Flt64.zero
            )
        }
        v2 != null -> {
            QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(
                    coefficient = coefficient * v2,
                    symbol1 = symbol1,
                    symbol2 = null
                )),
                constant = Flt64.zero
            )
        }
        else -> {
            QuadraticPolynomial(
                monomials = listOf(this),
                constant = Flt64.zero
            )
        }
    }
}

/**
 * 使用 Map 部分求值 Flt64 二次单项式
 * Partially evaluate a Flt64 quadratic monomial using a Map
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @return 部分求值后的二次多项式 / Partially evaluated quadratic polynomial
*/
fun QuadraticMonomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): QuadraticPolynomial<Flt64> {
    return partialEvaluate(MapValueProvider(values))
}

/**
 * 使用值提供者求值 Flt64 规范单项式
 * Evaluate a Flt64 canonical monomial using a value provider
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果，若缺少值则返回 null / Evaluation result, or null if value is missing
*/
fun CanonicalMonomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    var result = coefficient
    for ((symbol, power) in powers) {
        val symbolValue = resolveValue(symbol, provider, policy)
        if (symbolValue == null) return null
        result *= computeRingPowerOrNull(symbolValue, power.toInt(), Flt64.one) ?: return null
    }
    return result
}

/**
 * 使用 Map 求值 Flt64 规范单项式
 * Evaluate a Flt64 canonical monomial using a Map
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果，若缺少值则返回 null / Evaluation result, or null if value is missing
*/
fun CanonicalMonomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

/**
 * 使用值提供者求值 Flt64 规范单项式（Ret 安全版本）
 * Evaluate a Flt64 canonical monomial using a value provider (Ret-safe version)
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun CanonicalMonomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var result = coefficient
    for ((symbol, power) in powers) {
        val symbolValueResult = resolveValueRet(symbol, provider, policy)
        when (symbolValueResult) {
            is Failed -> return Failed(symbolValueResult.error)
            is Fatal -> return Fatal(symbolValueResult.errors)
            is Ok -> {
                val powerValue = when (val powerResult = computeRingPower(symbolValueResult.value, power.toInt(), Flt64.one)) {
                    is Ok -> powerResult.value
                    is Failed -> return Failed(powerResult.error)
                    is Fatal -> return Fatal(powerResult.errors)
                }
                result *= powerValue
            }
        }
    }
    return Ok(result)
}

/**
 * 使用 Map 求值 Flt64 规范单项式（Ret 安全版本）
 * Evaluate a Flt64 canonical monomial using a Map (Ret-safe version)
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun CanonicalMonomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

/**
 * 使用有序值求值 Flt64 规范单项式
 * Evaluate a Flt64 canonical monomial using ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 求值结果 / Evaluation result
*/
fun CanonicalMonomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Ret<Flt64> {
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
    var result = coefficient
    for ((symbol, power) in powers) {
        val index = indexOfSymbol[symbol]
            ?: return Failed(ErrorCode.DataNotFound, "Symbol ${symbol.name} not found in order.")
        val powerValue = when (val powerResult = computeRingPower(values[index], power.toInt(), Flt64.one)) {
            is Ok -> powerResult.value
            is Failed -> return Failed(powerResult.error)
            is Fatal -> return Fatal(powerResult.errors)
        }
        result *= powerValue
    }
    return Ok(result)
}

/**
 * 部分求值 Flt64 规范单项式
 * Partially evaluate a Flt64 canonical monomial
 *
 * @param provider 值提供者 / Value provider
 * @return 部分求值后的规范单项式 / Partially evaluated canonical monomial
*/
fun CanonicalMonomial<Flt64>.partialEvaluate(provider: ValueProvider): Ret<CanonicalMonomial<Flt64>> {
    var newCoefficient = coefficient
    val remainedPowers = LinkedHashMap<Symbol, Int32>()
    for ((symbol, power) in powers) {
        val symbolValue = provider[symbol]
        if (symbolValue != null) {
            val powerValue = when (val powerResult = computeRingPower(symbolValue, power.toInt(), Flt64.one)) {
                is Ok -> powerResult.value
                is Failed -> return Failed(powerResult.error)
                is Fatal -> return Fatal(powerResult.errors)
            }
            newCoefficient *= powerValue
        } else {
            remainedPowers[symbol] = power
        }
    }
    return Ok(CanonicalMonomial(
        coefficient = newCoefficient,
        powers = remainedPowers
    ))
}

/**
 * 使用 Map 部分求值 Flt64 规范单项式
 * Partially evaluate a Flt64 canonical monomial using a Map
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @return 部分求值后的规范单项式 / Partially evaluated canonical monomial
*/
fun CanonicalMonomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): Ret<CanonicalMonomial<Flt64>> {
    return partialEvaluate(MapValueProvider(values))
}

/**
 * 使用值提供者求值 Flt64 线性多项式
 * Evaluate a Flt64 linear polynomial using a value provider
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果，若缺少值则返回 null / Evaluation result, or null if value is missing
*/
fun LinearPolynomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluateLinear(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(symbol, provider, policy)
        }
    )
}

/**
 * 使用值提供者求值 Flt64 线性多项式（Ret 安全版本）
 * Evaluate a Flt64 linear polynomial using a value provider (Ret-safe version)
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun LinearPolynomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var failure: Ret<Flt64>? = null
    val value = evaluateLinear(
        values = emptyMap(),
        onMissing = { symbol ->
            when (val result = resolveValueRet(symbol, provider, policy)) {
                is Ok -> result.value
                is Failed -> {
                    if (failure == null) {
                        failure = result
                    }
                    null
                }
                is Fatal -> {
                    failure = result
                    null
                }
            }
        }
    )
    return when {
        value != null -> Ok(value)
        failure != null -> failure!!
        else -> Failed(ErrorCode.DataNotFound, "Missing value for one or more symbols.")
    }
}

/**
 * 使用 Map 求值 Flt64 线性多项式（Ret 安全版本）
 * Evaluate a Flt64 linear polynomial using a Map (Ret-safe version)
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun LinearPolynomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

/**
 * 使用有序值求值 Flt64 线性多项式
 * Evaluate a Flt64 linear polynomial using ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 求值结果 / Evaluation result
*/
fun LinearPolynomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Ret<Flt64> {
    return evaluateLinearOrdered(order, values)
}

/**
 * 部分求值 Flt64 线性多项式
 * Partially evaluate a Flt64 linear polynomial
 *
 * @param provider 值提供者 / Value provider
 * @return 部分求值后的线性多项式 / Partially evaluated linear polynomial
*/
fun LinearPolynomial<Flt64>.partialEvaluate(provider: ValueProvider): LinearPolynomial<Flt64> {
    val values = LinkedHashMap<Symbol, Flt64>()
    for (monomial in monomials) {
        provider[monomial.symbol]?.let { values[monomial.symbol] = it }
    }
    return partialEvaluateLinear(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 使用 Map 部分求值 Flt64 线性多项式
 * Partially evaluate a Flt64 linear polynomial using a Map
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @return 部分求值后的线性多项式 / Partially evaluated linear polynomial
*/
fun LinearPolynomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): LinearPolynomial<Flt64> {
    return partialEvaluateLinear(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 计算 Flt64 线性多项式在区间上的极值范围
 * Compute the extremum range of a Flt64 linear polynomial over intervals
 *
 * @param intervals 符号到值区间的映射 / Symbol-to-value-range mapping
 * @return 极值范围，若缺少区间则返回 null / Extremum range, or null if interval is missing
*/
fun LinearPolynomial<Flt64>.evaluateIntervalExtremum(
    intervals: Map<Symbol, ValueRange<Flt64>>
): ValueRange<Flt64>? {
    var lowerBound = constant
    var upperBound = constant
    for (monomial in monomials) {
        val interval = intervals[monomial.symbol] ?: return null
        val lb = interval.lowerBound.value.unwrapOrNull() ?: return null
        val ub = interval.upperBound.value.unwrapOrNull() ?: return null
        when {
            monomial.coefficient > Flt64.zero -> {
                lowerBound += monomial.coefficient * lb
                upperBound += monomial.coefficient * ub
            }

            monomial.coefficient < Flt64.zero -> {
                lowerBound += monomial.coefficient * ub
                upperBound += monomial.coefficient * lb
            }

            else -> {}
        }
    }
    return when (
        val range = ValueRange(
            lb = lowerBound,
            ub = upperBound,
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = Flt64
        )
    ) {
        is Ok -> range.value
        is Failed -> null
        is Fatal -> null
    }
}

/**
 * 使用值提供者求值 Flt64 二次多项式
 * Evaluate a Flt64 quadratic polynomial using a value provider
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果，若缺少值则返回 null / Evaluation result, or null if value is missing
*/
fun QuadraticPolynomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluateQuadratic(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(symbol, provider, policy)
        }
    )
}

/**
 * 使用值提供者求值 Flt64 二次多项式（Ret 安全版本）
 * Evaluate a Flt64 quadratic polynomial using a value provider (Ret-safe version)
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun QuadraticPolynomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var failure: Ret<Flt64>? = null
    val value = evaluateQuadratic(
        values = emptyMap(),
        onMissing = { symbol ->
            when (val result = resolveValueRet(symbol, provider, policy)) {
                is Ok -> result.value
                is Failed -> {
                    if (failure == null) {
                        failure = result
                    }
                    null
                }
                is Fatal -> {
                    failure = result
                    null
                }
            }
        }
    )
    return when {
        value != null -> Ok(value)
        failure != null -> failure!!
        else -> Failed(ErrorCode.DataNotFound, "Missing value for one or more symbols.")
    }
}

/**
 * 使用 Map 求值 Flt64 二次多项式（Ret 安全版本）
 * Evaluate a Flt64 quadratic polynomial using a Map (Ret-safe version)
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun QuadraticPolynomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

/**
 * 使用有序值求值 Flt64 二次多项式
 * Evaluate a Flt64 quadratic polynomial using ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 求值结果 / Evaluation result
*/
fun QuadraticPolynomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Ret<Flt64> {
    return evaluateQuadraticOrdered(order, values)
}

/**
 * 部分求值 Flt64 二次多项式
 * Partially evaluate a Flt64 quadratic polynomial
 *
 * @param provider 值提供者 / Value provider
 * @return 部分求值后的二次多项式 / Partially evaluated quadratic polynomial
*/
fun QuadraticPolynomial<Flt64>.partialEvaluate(provider: ValueProvider): QuadraticPolynomial<Flt64> {
    val values = LinkedHashMap<Symbol, Flt64>()
    for (monomial in monomials) {
        provider[monomial.symbol1]?.let { values[monomial.symbol1] = it }
        monomial.symbol2?.let { symbol ->
            provider[symbol]?.let { values[symbol] = it }
        }
    }
    return partialEvaluateQuadratic(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 使用 Map 部分求值 Flt64 二次多项式
 * Partially evaluate a Flt64 quadratic polynomial using a Map
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @return 部分求值后的二次多项式 / Partially evaluated quadratic polynomial
*/
fun QuadraticPolynomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): QuadraticPolynomial<Flt64> {
    return partialEvaluateQuadratic(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 使用值提供者求值 Flt64 规范多项式
 * Evaluate a Flt64 canonical polynomial using a value provider
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果，若缺少值则返回 null / Evaluation result, or null if value is missing
*/
fun CanonicalPolynomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluateCanonical(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(symbol, provider, policy)
        },
        one = Flt64.one
    )
}

/**
 * 使用值提供者求值 Flt64 规范多项式（Ret 安全版本）
 * Evaluate a Flt64 canonical polynomial using a value provider (Ret-safe version)
 *
 * @param provider 值提供者 / Value provider
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun CanonicalPolynomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var failure: Ret<Flt64>? = null
    val value = evaluateCanonical(
        values = emptyMap(),
        onMissing = { symbol ->
            when (val result = resolveValueRet(symbol, provider, policy)) {
                is Ok -> result.value
                is Failed -> {
                    if (failure == null) {
                        failure = result
                    }
                    null
                }
                is Fatal -> {
                    failure = result
                    null
                }
            }
        },
        one = Flt64.one
    )
    return when {
        value != null -> Ok(value)
        failure != null -> failure!!
        else -> Failed(ErrorCode.DataNotFound, "Missing value for one or more symbols.")
    }
}

/**
 * 使用 Map 求值 Flt64 规范多项式（Ret 安全版本）
 * Evaluate a Flt64 canonical polynomial using a Map (Ret-safe version)
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @param policy 缺失值策略 / Missing value policy
 * @return 求值结果 / Evaluation result
*/
fun CanonicalPolynomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

/**
 * 使用有序值求值 Flt64 规范多项式
 * Evaluate a Flt64 canonical polynomial using ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 求值结果 / Evaluation result
*/
fun CanonicalPolynomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Ret<Flt64> {
    return evaluateCanonicalOrdered(order, values, Flt64.one)
}

/**
 * 部分求值 Flt64 规范多项式
 * Partially evaluate a Flt64 canonical polynomial
 *
 * @param provider 值提供者 / Value provider
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 部分求值后的规范多项式 / Partially evaluated canonical polynomial
*/
fun CanonicalPolynomial<Flt64>.partialEvaluate(
    provider: ValueProvider,
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<CanonicalPolynomial<Flt64>> {
    val values = LinkedHashMap<Symbol, Flt64>()
    for (monomial in monomials) {
        for (symbol in monomial.powers.keys) {
            provider[symbol]?.let { values[symbol] = it }
        }
    }
    return partialEvaluateCanonical(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        one = Flt64.one,
        symbolComparator = symbolComparator
    )
}

/**
 * 使用 Map 部分求值 Flt64 规范多项式
 * Partially evaluate a Flt64 canonical polynomial using a Map
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 部分求值后的规范多项式 / Partially evaluated canonical polynomial
*/
fun CanonicalPolynomial<Flt64>.partialEvaluate(
    values: Map<Symbol, Flt64>,
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<CanonicalPolynomial<Flt64>> {
    return partialEvaluateCanonical(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        one = Flt64.one,
        symbolComparator = symbolComparator
    )
}
