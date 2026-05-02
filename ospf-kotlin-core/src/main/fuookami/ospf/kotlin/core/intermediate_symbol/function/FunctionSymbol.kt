@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenTableFlt64
import fuookami.ospf.kotlin.core.token.LinearFlattenDataFlt64
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.token.AbstractTokenListFlt64
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
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok


/**
 * Base interface for math-symbol-based function symbols.
 * Each function symbol creates helper variables and generates linear constraints.
 *
 * @param V the numeric type (must implement RealNumber and NumberField).
 */
interface MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
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

    /**
     * Register this function symbol with the model.
     *
     * Implementations should register helper variables via [model.add] as the first step,
     * then add constraints via [model.addConstraint].
     */
    fun register(model: AbstractLinearMetaModel<V>): Try
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
    val delegate: MathFunctionSymbol<V>
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
     * Expose positive slack variable as a LinearPolynomial<Flt64>, for framework compatibility.
     * Only meaningful when the delegate is a SlackFunction with withPositive=true.
     */
    val pos: LinearPolynomial<Flt64>? by lazy {
        val slack = delegate as? SlackFunction<Flt64> ?: return@lazy null
        slack.posVar?.let { v ->
            LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, v)),
                constant = Flt64.zero
            )
        }
    }

    /**
     * Expose negative slack variable as a LinearPolynomial<Flt64>, for framework compatibility.
     * Only meaningful when the delegate is a SlackFunction with withNegative=true.
     */
    val neg: LinearPolynomial<Flt64>? by lazy {
        val slack = delegate as? SlackFunction<Flt64> ?: return@lazy null
        slack.negVar?.let { v ->
            LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, v)),
                constant = Flt64.zero
            )
        }
    }

    /**
     * Expose the full slack expression (x + neg - pos) as a LinearPolynomial<Flt64>, for framework compatibility.
     * Only meaningful when the delegate is a SlackFunction.
     */
    val polyX: LinearPolynomial<Flt64>? by lazy {
        val slack = delegate as? SlackFunction<Flt64> ?: return@lazy null
        val xPoly = slack.x.asFlt64Poly()
        val coreMonomials = xPoly.monomials.mapNotNull { mono ->
            val sym = mono.symbol
            when (sym) {
                is fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *> ->
                    LinearMonomial(mono.coefficient, sym)
                is fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol<*> ->
                    LinearMonomial(mono.coefficient, sym)
                is fuookami.ospf.kotlin.math.symbol.Symbol -> {
                    // Math-level symbols can't be directly represented as core monomials
                    null
                }
                else -> null
            }
        }
        var result = LinearPolynomial(monomials = coreMonomials, constant = xPoly.constant)
        if (slack.withNegative && slack.negVar != null) {
            result = LinearPolynomial(
                monomials = result.monomials + LinearMonomial(Flt64.one, slack.negVar!!),
                constant = result.constant
            )
        }
        if (slack.withPositive && slack.posVar != null) {
            result = LinearPolynomial(
                monomials = result.monomials + LinearMonomial(-Flt64.one, slack.posVar!!),
                constant = result.constant
            )
        }
        result
    }

    override fun evaluate(values: Map<Symbol, V>): V? = delegate.evaluate(values)
    override fun register(model: AbstractLinearMetaModel<V>): Try = delegate.register(model)

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val cached: Boolean get() = false
    override val dependencies: Set<fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol<*>> get() = emptySet()
    override val discrete: Boolean get() = false
    override val range: ExpressionRange<Flt64> get() = ExpressionRange()

    override fun flush(force: Boolean) {}
    override fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? = null
    override fun toRawString(unfold: UInt64): String = name

    override val flattenedMonomials: LinearFlattenDataFlt64 get() = LinearFlattenDataFlt64(emptyList(), Flt64.zero)

    override val polynomial: LinearPolynomial<V>
        get() = LinearPolynomial(emptyList(), Flt64.zero as V)

    override fun asMutable(): MutableLinearPolynomial<V> {
        return MutableLinearPolynomial(emptyList(), Flt64.zero as V)
    }

    override fun toMathLinearInequality(): Flt64LinearInequality {
        return Flt64LinearInequality(LinearPolynomial(emptyList(), Flt64.zero), LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun toMathQuadraticInequality(): QuadraticInequality {
        return QuadraticInequality(fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.zero), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun evaluate(tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListFlt64?, zeroIfNone: Boolean): Flt64? {
        @Suppress("UNCHECKED_CAST")
        return delegate.evaluate(values as Map<Symbol, V>)?.asFlt64()
    }

    // V-typed evaluate overrides (P4-5) — delegate to Flt64-boundary evaluate + converter
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        @Suppress("UNCHECKED_CAST")
        val v = delegate.evaluate(values as Map<Symbol, V>) ?: return null
        return converter.intoValue(v.asFlt64())
    }
}

/**
 * Evaluate a math LinearPolynomial given a map of Symbol -> V values.
 * Returns null if any symbol in the polynomial is missing from the map.
 */
@Suppress("UNCHECKED_CAST")
fun <V> LinearPolynomial<V>.evaluate(values: Map<Symbol, V>): V? where V : RealNumber<V>, V : NumberField<V> {
    val monomialsWithValues = monomials.mapNotNull { mono ->
        val sv = values[mono.symbol] ?: return null
        mono.coefficient * sv
    }
    var sum: V? = null
    for (term in monomialsWithValues) {
        sum = if (sum == null) term else sum + term
    }
    return (sum ?: constant) as V
}

/** Internal helper: cast V to Flt64 for constraint generation. Only valid when V=Flt64. */
@Suppress("UNCHECKED_CAST")
internal fun <V> V.asFlt64(): Flt64 where V : RealNumber<V>, V : NumberField<V> = this as Flt64

/** Internal helper: get zero for type V. */
@Suppress("UNCHECKED_CAST")
internal fun <V> zeroOf(): V where V : RealNumber<V>, V : NumberField<V> = Flt64.zero as V

/** Internal helper: get one for type V. */
@Suppress("UNCHECKED_CAST")
internal fun <V> oneOf(): V where V : RealNumber<V>, V : NumberField<V> = Flt64.one as V

/** Internal helper: check if V is near zero. */
internal fun <V> V.isNearZero(tolerance: Double = NONZERO_TOLERANCE): Boolean where V : RealNumber<V>, V : NumberField<V> {
    val d = this.asFlt64().toDouble()
    return d <= tolerance && d >= -tolerance
}

/** Internal helper: check if V is nonzero. */
internal fun <V> V.isNonZero(tolerance: Double = NONZERO_TOLERANCE): Boolean where V : RealNumber<V>, V : NumberField<V> {
    val d = this.asFlt64().toDouble()
    return d > tolerance || d < -tolerance
}

/** Internal helper: convert LinearPolynomial<V> to LinearPolynomial<Flt64> for constraint generation. */
@Suppress("UNCHECKED_CAST")
internal fun <V> LinearPolynomial<V>.asFlt64Poly(): LinearPolynomial<Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient.asFlt64(), it.symbol) },
        constant.asFlt64()
    )
}
