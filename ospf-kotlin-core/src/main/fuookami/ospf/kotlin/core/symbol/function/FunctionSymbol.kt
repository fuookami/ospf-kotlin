/**
 * 函数符号核心接口与适配器 / Function symbol core interfaces and adapters
 *
 * 定义数学函数符号的基础接口（[MathFunctionSymbol]、[MathFunctionSymbolBase]）及
 * 线性函数符号适配器 [LinearFunctionSymbolAdapter]，用于将函数符号集成到
 * 中间符号体系中。
 *
 * Defines base interfaces for math function symbols ([MathFunctionSymbol],
 * [MathFunctionSymbolBase]) and the [LinearFunctionSymbolAdapter] for integrating
 * function symbols into the intermediate symbol system.
 */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator

/**
 * 函数符号注册生命周期的 V 泛型基类。
 * V-generic base for function symbol registration lifecycle.
 *
 * [registerAuxiliaryTokens] 与 [registerConstraints] 都在 V 类型边界上工作。
 * 实现会通过 [IntoValue]<V> 转换器，在内部构造约束时完成 V 与 Flt64 之间的转换。
 *
 * Both [registerAuxiliaryTokens] and [registerConstraints] operate on V-typed
 * boundaries. Implementations use their [IntoValue]<V> converter to convert
 * between V-typed data and Flt64 when constructing constraints internally.
 *
 * 运行时 token 集合与机制模型仍位于 Flt64 求解器边界，因此调用点会传入
 * `AddableTokenCollection<fuookami.ospf.kotlin.math.algebra.number.Flt64>` 与
 * `AbstractLinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>`，
 * 它们也是 V 类型接口的子类型。
 *
 * At runtime, the token collection and mechanism model are always Flt64-based
 * (solver boundary), so call sites pass `AddableTokenCollection<fuookami.ospf.kotlin.math.algebra.number.Flt64>` and
 * `AbstractLinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>` which are subtypes of the V-typed interfaces.
 */
interface MathFunctionSymbolBase<V> where V : RealNumber<V>, V : NumberField<V> {
    fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try
    fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try
}

/**
 * Optional interface for [MathFunctionSymbol] implementations that expose
 * a result polynomial. [LinearFunctionSymbolAdapter] uses this to provide
 * a non-zero [polynomial] so that model.maximize(fn) and LinearPolynomial(fn)
 * produce the correct objective term.
 */
interface HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    val resultPolynomial: LinearPolynomial<V>
}

/**
 * Base interface for math-symbol-based function symbols.
 * Each function symbol creates helper variables and generates linear constraints.
 *
 * @param V the numeric type (must implement RealNumber and NumberField).
 */
interface MathFunctionSymbol<V> : MathFunctionSymbolBase<V> where V : RealNumber<V>, V : NumberField<V> {
    var name: String
    var displayName: String?

    /**
     * Helper variables created by this function (e.g. pos/neg slack variables).
     * Exposed so the framework can reference them in objectives.
     */
    val helperVariables: List<AbstractVariableItem<*, *>>

    /**
     * Evaluate this function symbol given resolved symbol values.
     */
    fun evaluate(values: Map<Symbol, V>): V?
}

/**
 * Internal non-generic base for quadratic function symbol registration.
 *
 * Mirrors [MathFunctionSymbolBase] but for quadratic mechanism models.
 * This is an internal solver-boundary interface.
 */
internal interface QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : NumberField<V> {
    fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try
    fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try
}

/**
 * Adapter that wraps a [MathFunctionSymbol]<V> to also implement
 * [LinearIntermediateSymbol]<V>. This allows function symbols to be stored in
 * `LinearIntermediateSymbols1/2` containers used throughout the framework.
 *
 * All [LinearIntermediateSymbol] members have sensible defaults since function
 * symbols generate constraints via [MathFunctionSymbol.register] rather than
 * through the traditional prepare/flatten pipeline.
 */
/**
 * 线性函数符号适配器 / Linear function symbol adapter
 *
 * 将 [MathFunctionSymbol] 包装为 [LinearIntermediateSymbol]，使函数符号可以
 * 存储在框架使用的 [LinearIntermediateSymbols1] 等容器中。
 *
 * Wraps a [MathFunctionSymbol] as a [LinearIntermediateSymbol], allowing function
 * symbols to be stored in framework containers such as [LinearIntermediateSymbols1].
 *
 * @property delegate 被包装的函数符号 / The wrapped function symbol
 * @property converter V 类型值转换器 / V-type value converter
 * @property pos 正松弛变量多项式 / Positive slack variable polynomial
 * @property neg 负松弛变量多项式 / Negative slack variable polynomial
 * @property polyX 带松弛调整的输入多项式 / Input polynomial with slack adjustments
 */
class LinearFunctionSymbolAdapter<V>(
    val delegate: MathFunctionSymbol<V>,
    private val converter: IntoValue<V>
) : LinearIntermediateSymbol<V>, MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    override var name: String
        get() = delegate.name
        set(value) { delegate.name = value }

    override var displayName: String?
        get() = delegate.displayName
        set(value) { delegate.displayName = value }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = delegate.helperVariables

    /**
     * Expose positive slack variable as a LinearPolynomial<V>.
     * Only meaningful when the delegate is a SlackFunction with withPositive=true.
     */
    val pos: LinearPolynomial<V>? by lazy {
        val slack = delegate as? SlackFunction<V>
        val slackRange = delegate as? SlackRangeFunction<V>
        val posVar = slack?.posVar ?: slackRange?.posVar
        posVar?.let { v ->
            LinearPolynomial(
                monomials = listOf(LinearMonomial(converter.one, v)),
                constant = converter.zero
            )
        }
    }

    val neg: LinearPolynomial<V>? by lazy {
        val slack = delegate as? SlackFunction<V>
        val slackRange = delegate as? SlackRangeFunction<V>
        val negVar = slack?.negVar ?: slackRange?.negVar
        negVar?.let { v ->
            LinearPolynomial(
                monomials = listOf(LinearMonomial(converter.one, v)),
                constant = converter.zero
            )
        }
    }

    val polyX: LinearPolynomial<V>? by lazy {
        when (val d = delegate) {
            is SlackFunction<V> -> {
                val unit = converter.one
                var result = LinearPolynomial(d.x.monomials.toMutableList(), d.x.constant)
                if (d.withNegative && d.negVar != null) {
                    result = LinearPolynomial(result.monomials + LinearMonomial(unit, d.negVar!!), result.constant)
                }
                if (d.withPositive && d.posVar != null) {
                    result = LinearPolynomial(result.monomials + LinearMonomial(-unit, d.posVar!!), result.constant)
                }
                result
            }
            is SlackRangeFunction<V> -> {
                val unit = converter.one
                var result = LinearPolynomial(d.x.monomials.toMutableList(), d.x.constant)
                result = LinearPolynomial(result.monomials + LinearMonomial(unit, d.negVar), result.constant)
                result = LinearPolynomial(result.monomials + LinearMonomial(-unit, d.posVar), result.constant)
                result
            }
            else -> null
        }
    }

    override fun evaluate(values: Map<Symbol, V>): V? = delegate.evaluate(values)

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try = delegate.registerAuxiliaryTokens(tokens)

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try = delegate.registerConstraints(model)

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = fuookami.ospf.kotlin.math.symbol.Linear
    override val cached: Boolean get() = false
    override val dependencies: Set<IntermediateSymbol<*>> get() = emptySet()
    override val discrete: Boolean get() = false
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRange()

    override fun flush(force: Boolean) {}
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val typedValues = values?.let { SolverBoundaryCasts.mapValues(it, converter) }
        return if (typedValues.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(typedValues, tokenTable, converter, false)
        }
    }
    override fun toRawString(unfold: UInt64): String = name

    @Suppress("UNCHECKED_CAST")
    private fun resultPolynomialOrZero(): LinearPolynomial<V> {
        // 安全不变量：本模块内 HasResultPolynomial 的实现与 delegate 使用相同的 V 类型参数。
        // Safety invariant: HasResultPolynomial implementations in this module use the same V type parameter as delegate.
        return (delegate as? HasResultPolynomial<*>)?.resultPolynomial as? LinearPolynomial<V>
            ?: LinearPolynomial(emptyList(), converter.zero)
    }

    internal val flattenedMonomials: LinearFlattenData<V> get() {
        val poly = resultPolynomialOrZero()
        return LinearFlattenData(poly.monomials, poly.constant)
    }

    override val polynomial: LinearPolynomial<V>
        get() = resultPolynomialOrZero()

    override fun asMutable(): MutableLinearPolynomial<V> {
        val poly = resultPolynomialOrZero()
        return MutableLinearPolynomial(poly.monomials, poly.constant)
    }

    internal fun evaluate(tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone: Boolean): Flt64? {
        val v = delegate.evaluate(SolverBoundaryCasts.mapValues(values, converter)) ?: return null
        return converter.fromValue(v)
    }

    // V-typed evaluate overrides (P4-5) - delegate to Flt64-boundary evaluate + converter
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
        return delegate.evaluate(values)
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
        return delegate.evaluate(values)
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return delegate.evaluate(values)
    }
    internal fun evaluateSolver(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val typedResults = results.map { converter.intoValue(it) }
        return evaluate(typedResults, tokenTable, converter, zeroIfNone)
    }
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return delegate.evaluate(SolverBoundaryCasts.mapValues(values, converter))
    }
}

// ---- Converter-based helpers (safe, no unchecked casts) ----

/** Convert LinearPolynomial<V> to LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> using the provided converter. */
internal fun <V> LinearPolynomial<V>.asFlt64Poly(converter: IntoValue<V>): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(converter.fromValue(it.coefficient), it.symbol) },
        converter.fromValue(constant)
    )
}

/** Convert QuadraticPolynomial<V> to QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> using the provided converter. */
internal fun <V> QuadraticPolynomial<V>.asFlt64QuadraticPoly(converter: IntoValue<V>): QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(converter.fromValue(it.coefficient), it.symbol1, it.symbol2) },
        converter.fromValue(constant)
    )
}

/** Convert QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> to QuadraticPolynomial<V> using the provided converter. */
internal fun <V> QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.asVQuadraticPoly(converter: IntoValue<V>): QuadraticPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(converter.intoValue(it.coefficient), it.symbol1, it.symbol2) },
        converter.intoValue(constant)
    )
}
