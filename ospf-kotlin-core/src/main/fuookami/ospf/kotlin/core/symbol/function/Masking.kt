@file:Suppress("unused")

/** 掩码函数符号 / Masking function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 掩码函数符号 / Masking function symbols
 *
 * 提供 [MaskingFunction]、[MaskingWithPolyMaskFunction]、[MaskingRangeFunction]，
 * 实现 y = x * mask 的线性化建模（mask 为二值变量）。
 *
 * Provides [MaskingFunction], [MaskingWithPolyMaskFunction], and [MaskingRangeFunction]
 * for linearized modeling of y = x * mask (where mask is binary).
 */

/**
 * 掩码函数：y = x * mask，其中 mask 为二值变量。
 * Masking function: y = x * mask where mask is binary.
 * 当 mask=1 时，y=x。当 mask=0 时，y=0。
 * When mask=1, y=x. When mask=0, y=0.
 *
 * @property input 输入线性多项式 / input linear polynomial
 * @property mask 二值掩码变量 / binary mask variable
 * @param bigM Big-M 界限（默认从输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from input range by default, falls back to 1e6)
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class MaskingFunction<V>(
    val input: LinearPolynomial<V>,
    val mask: AbstractVariableItem<*, *>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: input.defaultBigM(converter)

    val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_masking")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val maskValue = values[mask] ?: return converter.zero
        if (maskValue eq converter.zero) return converter.zero
        return input.evaluateWith(values)
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val one = converter.one
        val zero = converter.zero
        val inputPoly = input
        val inputBounds = inputPoly.finiteBounds(converter)
        val lower = inputBounds?.lower ?: -bigM
        val upper = inputBounds?.upper ?: bigM
        val resultIdx = LinearMonomial(one, resultVar)
        val negInputMonos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }

        val constraints = mutableListOf<LinearInequality<V>>()

        // y <= upper * mask / 上界：mask=0 时 y <= 0
        constraints += LinearInequality(
            LinearPolynomial(listOf(resultIdx, LinearMonomial(-upper, mask)), zero),
            LinearPolynomial(emptyList(), zero),
            Comparison.LE, "${name}_masking_zero_ub")

        // y >= lower * mask / 下界：mask=0 时 y >= 0
        constraints += LinearInequality(
            LinearPolynomial(listOf(resultIdx, LinearMonomial(-lower, mask)), zero),
            LinearPolynomial(emptyList(), zero),
            Comparison.GE, "${name}_masking_zero_lb")

        // y - x <= -lower*(1-mask) / mask=1 时 y <= x
        constraints += LinearInequality(
            LinearPolynomial(listOf(resultIdx) + negInputMonos + LinearMonomial(-lower, mask), -inputPoly.constant),
            LinearPolynomial(emptyList(), -lower),
            Comparison.LE, "${name}_masking_eq_ub")

        // y - x >= -upper*(1-mask) / mask=1 时 y >= x
        constraints += LinearInequality(
            LinearPolynomial(listOf(resultIdx) + negInputMonos + LinearMonomial(-upper, mask), -inputPoly.constant),
            LinearPolynomial(emptyList(), -upper),
            Comparison.GE, "${name}_masking_eq_lb")

        return addConstraints(model, constraints) ?: ok
    }
    companion object {
        /** 创建 [MaskingFunction] 实例。 / Create a [MaskingFunction] instance. */
        operator fun <V> invoke(
            input: LinearPolynomial<V>,
            mask: AbstractVariableItem<*, *>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): MaskingFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaskingFunction(input, mask, bigM, converter, name, displayName)

        /**
         * 通过掩码变量名创建 [MaskingFunction] 包装器。
         * Create a [MaskingFunction] wrapper by mask variable name.
         *
         * @param input 输入线性多项式 / input linear polynomial
         * @param maskVarName 掩码变量名称 / mask variable name
         * @param bigM Big-M 界限（默认从输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from input range by default, falls back to 1e6)
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 线性函数符号适配器 / linear function symbol adapter
         */
        fun <V> withMaskVarName(
            input: LinearPolynomial<V>,
            maskVarName: String,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> {
            val maskVar = BinVar("${name}_${maskVarName}")
            return LinearFunctionSymbolAdapter(
                MaskingFunction(input, maskVar, bigM, converter, name, displayName),
                converter = converter
            )
        }

        /**
         * 类型化符号工厂：将线性中间符号转换为线性多项式。
         * Typed symbol factory: converts a linear intermediate symbol to a linear polynomial.
         *
         * @param x 线性中间符号 / linear intermediate symbol
         * @param mask 二值掩码变量 / binary mask variable
         * @param bigM Big-M 界限（默认从输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from input range by default, falls back to 1e6)
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 线性函数符号适配器 / linear function symbol adapter
         */
        @JvmStatic
        @JvmName("fromLinearIntermediateSymbol")
        fun <V> fromLinearIntermediateSymbol(
            x: LinearIntermediateSymbol<V>,
            mask: AbstractVariableItem<*, *>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> = LinearFunctionSymbolAdapter(
            MaskingFunction(
                input = x.toLinearPolynomial(),
                mask = mask,
                bigM = bigM,
                converter = converter,
                name = name,
                displayName = displayName
            ),
            converter = converter
        )

        /**
         * 类型化多项式工厂：接受 input 与 mask 的线性多项式视图。
         * Typed polynomial factory: accepts linear-polynomial views for input and mask.
         *
         * @param x 输入的线性多项式视图 / linear polynomial view for input
         * @param mask 掩码的线性多项式视图 / linear polynomial view for mask
         * @param bigM Big-M 界限（默认从输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from input range by default, falls back to 1e6)
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 带多项式掩码的掩码函数 / masking function with polynomial mask
         */
        @JvmStatic
        @JvmName("fromLinearPolynomials")
        fun <V> fromLinearPolynomials(
            x: ToLinearPolynomial<V>,
            mask: ToLinearPolynomial<V>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): MaskingWithPolyMaskFunction<V> where V : RealNumber<V>, V : NumberField<V> = MaskingWithPolyMaskFunction(
            input = x.toLinearPolynomial(),
            maskPoly = mask.toLinearPolynomial(),
            bigM = bigM,
            converter = converter,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * 掩码函数变体：mask 为多项式而非单个变量。
 * Masking function variant where mask is a polynomial instead of a single variable.
 * y = x * mask，其中 mask 为多项式表达式。
 * y = x * mask where mask is a polynomial expression.
 * 创建内部变量 `m` 并约束 m = maskPoly，然后以 m 作为二值掩码应用标准 Big-M 掩码约束。
 * Creates an internal variable `m` with constraint m = maskPoly, then applies
 * standard Big-M masking constraints with m as the binary mask.
 *
 * @property input 输入线性多项式 / input linear polynomial
 * @property maskPoly 掩码多项式 / mask polynomial
 * @param bigM Big-M 界限（默认从输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from input range by default, falls back to 1e6)
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class MaskingWithPolyMaskFunction<V>(
    val input: LinearPolynomial<V>,
    val maskPoly: LinearPolynomial<V>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : LinearIntermediateSymbol<V>, MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: input.defaultBigM(converter)
    val maskVar: AbstractVariableItem<*, *> = BinVar("${name}_mask_var")
    val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_result")

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(maskVar, resultVar)

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = fuookami.ospf.kotlin.math.symbol.Linear
    override val cached: Boolean get() = false
    override val dependencies: Set<IntermediateSymbol<*>> get() = emptySet()
    override val discrete: Boolean get() = false
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRange()

    override fun flush(force: Boolean) {}

    /** 使用 Flt64 值预计算求解器结果。 / Pre-compute solver result with Flt64 values. */
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val typedValues = values?.let { SolverBoundaryCasts.mapValues(it, converter) }
        return if (typedValues.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(typedValues, tokenTable, converter, false)
        }
    }
    override fun toRawString(unfold: UInt64): String = name

    override val polynomial: LinearPolynomial<V> get() = LinearPolynomial(emptyList(), converter.zero)
    override fun asMutable(): MutableLinearPolynomial<V> = MutableLinearPolynomial(emptyList(), converter.zero)

    /** 使用 Flt64 token 列表求值（始终返回 null）。 / Evaluate with Flt64 token list (always returns null). */
    internal fun evaluate(tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null
    /** 使用 Flt64 结果列表求值（始终返回 null）。 / Evaluate with Flt64 results list (always returns null). */
    internal fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null
    /** 使用 Flt64 值映射求值。 / Evaluate with Flt64 value map. */
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<Flt64>?, zeroIfNone: Boolean): Flt64? {
        return delegate().evaluate(SolverBoundaryCasts.mapValues(values, converter))?.let { converter.fromValue(it) }
    }

    // V-typed evaluate overrides / V 类型求值重写
    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        return if (values.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(values, tokenTable, converter, false)
        }
    }
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val values = LinkedHashMap<Symbol, V>(tokenTable.tokensInSolver.size)
        for (token in tokenTable.tokensInSolver) {
            val tokenValue = token.result ?: if (zeroIfNone) converter.zero else return null
            values[token.variable] = tokenValue
        }
        return delegate().evaluate(values)
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val values = LinkedHashMap<Symbol, V>(tokenTable.tokensInSolver.size)
        for ((index, token) in tokenTable.tokensInSolver.withIndex()) {
            val value = if (index < results.size) {
                results[index]
            } else {
                if (!zeroIfNone) {
                    return null
                }
                converter.zero
            }
            values[token.variable] = value
        }
        return delegate().evaluate(values)
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return delegate().evaluate(values)
    }
    /** 使用 Flt64 结果列表进行求解器求值。 / Evaluate solver with Flt64 results list. */
    internal fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val typedResults = results.map { converter.intoValue(it) }
        return evaluate(typedResults, tokenTable, converter, zeroIfNone)
    }
    /** 使用 Flt64 值映射进行求解器求值。 / Evaluate solver with Flt64 value map. */
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val v = delegate().evaluate(SolverBoundaryCasts.mapValues(values, converter)) ?: return null
        return converter.intoValue(converter.fromValue(v))
    }

    /** 委托给自身作为 MathFunctionSymbol。 / Delegate to self as MathFunctionSymbol. */
    private fun delegate(): MathFunctionSymbol<V> = this

    override fun evaluate(values: Map<Symbol, V>): V? {
        val maskValue = maskPoly.evaluateWith(values) ?: return converter.zero
        if (maskValue eq converter.zero) return converter.zero
        return input.evaluateWith(values)
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val inputPoly = input
        val maskPolyF = maskPoly
        val inputBounds = inputPoly.finiteBounds(converter)
        val lower = inputBounds?.lower ?: -bigM
        val upper = inputBounds?.upper ?: bigM
        val resultIdx = LinearMonomial(converter.one, resultVar)
        val negInputMonos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }

        val constraints = mutableListOf<LinearInequality<V>>()

        // maskPoly = maskVar constraint / maskPoly = maskVar 约束
        val maskMonos = maskPolyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(converter.one, maskVar)
        constraints += LinearInequality(
            LinearPolynomial(maskMonos, -maskPolyF.constant),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_mask_eq")

        // result <= upper * maskVar / 上界：maskVar=0 时 result <= 0
        constraints += LinearInequality(
            LinearPolynomial(listOf(resultIdx, LinearMonomial(-upper, maskVar)), zero),
            LinearPolynomial(emptyList(), zero), Comparison.LE, "${name}_zero_ub")

        // result >= lower * maskVar / 下界：maskVar=0 时 result >= 0
        constraints += LinearInequality(
            LinearPolynomial(listOf(resultIdx, LinearMonomial(-lower, maskVar)), zero),
            LinearPolynomial(emptyList(), zero), Comparison.GE, "${name}_zero_lb")

        // result - input <= -lower*(1-maskVar) / maskVar=1 时 result <= input
        constraints += LinearInequality(
            LinearPolynomial(listOf(resultIdx) + negInputMonos + LinearMonomial(-lower, maskVar), -inputPoly.constant),
            LinearPolynomial(emptyList(), -lower), Comparison.LE, "${name}_ub")

        // result - input >= -upper*(1-maskVar) / maskVar=1 时 result >= input
        constraints += LinearInequality(
            LinearPolynomial(listOf(resultIdx) + negInputMonos + LinearMonomial(-upper, maskVar), -inputPoly.constant),
            LinearPolynomial(emptyList(), -upper), Comparison.GE, "${name}_lb")

        return addConstraints(model, constraints) ?: ok
    }}

/**
 * 掩码范围函数：y 在 [lower*mask, upper*mask] 范围内。
 * Masking range function: y in [lower*mask, upper*mask].
 * 当 mask=0 时，y=0。当 mask=1 时，y 在 [lower, upper] 范围内。
 * When mask=0, y=0. When mask=1, y in [lower, upper].
 *
 * @property mask 掩码线性多项式 / mask linear polynomial
 * @property lower 范围的下界 / lower bound of the range
 * @property upper 范围的上界 / upper bound of the range
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class MaskingRangeFunction<V>(
    val mask: LinearPolynomial<V>,
    val lower: V,
    val upper: V,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    init {
        require(lower ls upper || lower eq upper) {
            "MaskingRange lower bound must be <= upper bound"
        }
    }

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_mask_range")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val maskValue = mask.evaluateWith(values) ?: return converter.zero
        if (maskValue eq converter.zero) return converter.zero

        val yValue = values[resultVar] ?: return converter.zero
        val lowerCandidate = lower * maskValue
        val upperCandidate = upper * maskValue
        val lowerBound: V
        val upperBound: V
        if (lowerCandidate ls upperCandidate || lowerCandidate eq upperCandidate) {
            lowerBound = lowerCandidate
            upperBound = upperCandidate
        } else {
            lowerBound = upperCandidate
            upperBound = lowerCandidate
        }
        return when {
            yValue ls lowerBound -> lowerBound
            yValue gr upperBound -> upperBound
            else -> yValue
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val one = converter.one
        val zero = converter.zero
        val resultMon = LinearMonomial(one, resultVar)
        val maskPoly = mask
        val lowerValue = lower
        val upperValue = upper

        val constraints = mutableListOf<LinearInequality<V>>()

        // Upper: y - upper*mask <= 0 / 上界：y - upper*mask <= 0
        val upperMonos = listOf(resultMon) +
            maskPoly.monomials.map { LinearMonomial(it.coefficient * -upperValue, it.symbol) }
        val upperLhs = LinearPolynomial(upperMonos, maskPoly.constant * -upperValue)
        val upperRhs = LinearPolynomial(emptyList(), zero)
        constraints += LinearInequality(upperLhs, upperRhs, Comparison.LE, "${name}_masking_range_ub")

        // Lower: y - lower*mask >= 0 / 下界：y - lower*mask >= 0
        val lowerMonos = listOf(resultMon) +
            maskPoly.monomials.map { LinearMonomial(it.coefficient * -lowerValue, it.symbol) }
        val lowerLhs = LinearPolynomial(lowerMonos, maskPoly.constant * -lowerValue)
        val lowerRhs = LinearPolynomial(emptyList(), zero)
        constraints += LinearInequality(lowerLhs, lowerRhs, Comparison.GE, "${name}_masking_range_lb")

        return addConstraints(model, constraints) ?: ok
    }
    companion object {
        /** 创建 [MaskingRangeFunction] 实例。 / Create a [MaskingRangeFunction] instance. */
        operator fun <V> invoke(
            mask: LinearPolynomial<V>,
            lower: V,
            upper: V,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): MaskingRangeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaskingRangeFunction(mask, lower, upper, converter, name, displayName)
    }
}
