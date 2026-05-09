@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.AbstractTokenList
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality


/**
 * V-generic base for function symbol registration lifecycle.
 *
 * Both [registerAuxiliaryTokens] and [registerConstraints] operate on V-typed
 * boundaries. Implementations use their [IntoValue]<V> converter to bridge
 * V-typed data to Flt64 when constructing constraints internally.
 *
 * At runtime, the token collection and mechanism model are always Flt64-based
 * (solver boundary), so call sites pass `AddableTokenCollection<Flt64>` and
 * `AbstractLinearMechanismModel<Flt64>` which are subtypes of the V-typed interfaces.
 */
interface MathFunctionSymbolBase<V> where V : RealNumber<V>, V : NumberField<V> {
    fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try
    fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try
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
    fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try
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
        val slack = delegate as? SlackFunction<V> ?: return@lazy null
        slack.posVar?.let { v ->
            val unit = slack.x.constant / slack.x.constant
            LinearPolynomial(
                monomials = listOf(LinearMonomial(unit, v)),
                constant = slack.x.constant - slack.x.constant
            )
        }
    }

    /**
     * Expose negative slack variable as a LinearPolynomial<V>.
     * Only meaningful when the delegate is a SlackFunction with withNegative=true.
     */
    val neg: LinearPolynomial<V>? by lazy {
        val slack = delegate as? SlackFunction<V> ?: return@lazy null
        slack.negVar?.let { v ->
            val unit = slack.x.constant / slack.x.constant
            LinearPolynomial(
                monomials = listOf(LinearMonomial(unit, v)),
                constant = slack.x.constant - slack.x.constant
            )
        }
    }

    /**
     * Expose the full slack expression (x + neg - pos) as a LinearPolynomial<V>.
     * Only meaningful when the delegate is a SlackFunction.
     */
    val polyX: LinearPolynomial<V>? by lazy {
        val slack = delegate as? SlackFunction<V> ?: return@lazy null
        val unit = slack.x.constant / slack.x.constant
        var result = LinearPolynomial(slack.x.monomials.toMutableList(), slack.x.constant)
        if (slack.withNegative && slack.negVar != null) {
            result = LinearPolynomial(result.monomials + LinearMonomial(unit, slack.negVar!!), result.constant)
        }
        if (slack.withPositive && slack.posVar != null) {
            result = LinearPolynomial(result.monomials + LinearMonomial(-unit, slack.posVar!!), result.constant)
        }
        result
    }

    override fun evaluate(values: Map<Symbol, V>): V? = delegate.evaluate(values)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try = delegate.registerAuxiliaryTokens(tokens)

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try = delegate.registerConstraints(model)

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val cached: Boolean get() = false
    override val dependencies: Set<fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol<*>> get() = emptySet()
    override val discrete: Boolean get() = false
    override val range: ExpressionRange<Flt64> get() = ExpressionRange()

    override fun flush(force: Boolean) {}
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? = null
    override fun toRawString(unfold: UInt64): String = name

    internal val flattenedMonomials: LinearFlattenData<Flt64> get() = LinearFlattenData<Flt64>(emptyList(), Flt64.zero)

    override val polynomial: LinearPolynomial<V>
        get() = LinearPolynomial(emptyList(), converter.zero)

    override fun asMutable(): MutableLinearPolynomial<V> {
        return MutableLinearPolynomial(emptyList(), converter.zero)
    }

    internal fun toMathLinearInequality(): LinearInequality<Flt64> {
        return LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64.zero), LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    internal fun toMathQuadraticInequality(): QuadraticInequalityOf<Flt64> {
        return QuadraticInequalityOf<Flt64>(fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.zero), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    internal fun evaluate(tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<Flt64>?, zeroIfNone: Boolean): Flt64? {
        val v = delegate.evaluate(values as Map<Symbol, V>) ?: return null
        return converter.fromValue(v)
    }

    // V-typed evaluate overrides (P4-5) — delegate to Flt64-boundary evaluate + converter
    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? = null
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return delegate.evaluate(values)
    }
    internal fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return delegate.evaluate(values as Map<Symbol, V>)
    }
}

// ---- Converter-based helpers (safe, no unchecked casts) ----

/** Convert LinearPolynomial<V> to LinearPolynomial<Flt64> using the provided converter. */
internal fun <V> LinearPolynomial<V>.asFlt64Poly(converter: IntoValue<V>): LinearPolynomial<Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(converter.fromValue(it.coefficient), it.symbol) },
        converter.fromValue(constant)
    )
}

/** Convert QuadraticPolynomial<V> to QuadraticPolynomial<Flt64> using the provided converter. */
internal fun <V> QuadraticPolynomial<V>.asFlt64QuadraticPoly(converter: IntoValue<V>): QuadraticPolynomial<Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(converter.fromValue(it.coefficient), it.symbol1, it.symbol2) },
        converter.fromValue(constant)
    )
}

/** Convert QuadraticPolynomial<Flt64> to QuadraticPolynomial<V> using the provided converter. */
internal fun <V> QuadraticPolynomial<Flt64>.asVQuadraticPoly(converter: IntoValue<V>): QuadraticPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(converter.intoValue(it.coefficient), it.symbol1, it.symbol2) },
        converter.intoValue(constant)
    )
}
