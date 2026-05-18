@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.intermediate_symbol.SolverBoundaryCasts
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
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.AbstractTokenList
import fuookami.ospf.kotlin.core.token.LinearFlattenData


/**
 * V-generic base for function symbol registration lifecycle.
 *
 * Both [registerAuxiliaryTokens] and [registerConstraints] operate on V-typed
 * boundaries. Implementations use their [IntoValue]<V> converter to bridge
 * V-typed data to Flt64 when constructing constraints internally.
 *
 * At runtime, the token collection and mechanism model are always Flt64-based
 * (solver boundary), so call sites pass `AddableTokenCollection<fuookami.ospf.kotlin.math.algebra.number.Flt64>` and
 * `AbstractLinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>` which are subtypes of the V-typed interfaces.
 */
interface MathFunctionSymbolBase<V> where V : RealNumber<V>, V : NumberField<V> {
    fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try
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
        val slack = delegate as? SlackFunction<V>
        val slackRange = delegate as? SlackRangeFunction<V>
        val posV = slack?.posVar ?: slackRange?.posVar
        posV?.let { v ->
            LinearPolynomial(
                monomials = listOf(LinearMonomial(converter.one, v)),
                constant = converter.zero
            )
        }
    }

    val neg: LinearPolynomial<V>? by lazy {
        val slack = delegate as? SlackFunction<V>
        val slackRange = delegate as? SlackRangeFunction<V>
        val negV = slack?.negVar ?: slackRange?.negVar
        negV?.let { v ->
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
                d.negVar?.let { result = LinearPolynomial(result.monomials + LinearMonomial(unit, it), result.constant) }
                d.posVar?.let { result = LinearPolynomial(result.monomials + LinearMonomial(-unit, it), result.constant) }
                result
            }
            else -> null
        }
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
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRangeV()

    override fun flush(force: Boolean) {}
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val typedValues = values?.let { SolverBoundaryCasts.mapValuesToV(it, converter) }
        return if (typedValues.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(typedValues, tokenTable, converter, false)
        }
    }
    override fun toRawString(unfold: UInt64): String = name

    internal val flattenedMonomialsAsV: LinearFlattenData<V> get() {
        val poly = (delegate as? HasResultPolynomial<V>)?.resultPolynomial
            ?: LinearPolynomial(emptyList(), converter.zero)
        return LinearFlattenData(poly.monomials, poly.constant)
    }

    override val polynomial: LinearPolynomial<V>
        get() = (delegate as? HasResultPolynomial<V>)?.resultPolynomial
            ?: LinearPolynomial(emptyList(), converter.zero)

    override fun asMutable(): MutableLinearPolynomial<V> {
        val poly = (delegate as? HasResultPolynomial<V>)?.resultPolynomial
            ?: LinearPolynomial(emptyList(), converter.zero)
        return MutableLinearPolynomial(poly.monomials, poly.constant)
    }



    internal fun evaluate(tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone: Boolean): Flt64? {
        val v = delegate.evaluate(SolverBoundaryCasts.mapValuesToV(values, converter)) ?: return null
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
        return delegate.evaluate(SolverBoundaryCasts.mapValuesToV(values, converter))
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
