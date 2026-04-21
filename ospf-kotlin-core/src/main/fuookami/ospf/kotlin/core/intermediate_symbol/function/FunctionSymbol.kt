@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.ExpressionRange
import fuookami.ospf.kotlin.core.intermediate_model.*
import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenDataF64
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.intermediate_model.LegacyAbstractTokenTable
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AddableTokenCollectionF64
import fuookami.ospf.kotlin.core.variable.AbstractTokenListF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Base interface for math-symbol-based function symbols.
 * Each function symbol creates helper variables and generates linear constraints.
 *
 * @param T the numeric type (must implement Field). Currently only Flt64 is supported for register().
 */
interface MathFunctionSymbol<T : Field<T>> {
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
    fun evaluate(values: Map<Symbol, T>): T?

    /**
     * Register this function symbol with the model.
     * Note: Currently only supports T=Flt64 since the solver layer uses Flt64 constraints.
     *
     * Implementations should call [registerAuxiliaryTokens] as the first step
     * to add helper variables, then add constraints.
     */
    fun register(model: AbstractLinearMetaModelF64): Try

    /**
     * Register auxiliary tokens (helper variables) needed by this function symbol.
     *
     * This separates "register variables" from "register constraints" for clarity.
     * Called by [register] as the first step before adding constraints.
     * The default implementation registers [helperVariables] if non-empty.
     *
     * Override this when the function registers auxiliary variables that are not
     * captured by [helperVariables] (e.g. variables created lazily or dynamically).
     */
    fun registerAuxiliaryTokens(tokens: AddableTokenCollectionF64): Try {
        val vars = helperVariables
        return if (vars.isNotEmpty()) tokens.add(vars) else ok
    }
}

/**
 * Adapter that wraps a [MathFunctionSymbol]<Flt64> to also implement
 * [LinearIntermediateSymbol]. This allows function symbols to be stored in
 * `LinearIntermediateSymbols1/2` containers used throughout the framework.
 *
 * All [LinearIntermediateSymbol] members have sensible defaults since function
 * symbols generate constraints via [MathFunctionSymbol.register] rather than
 * through the traditional prepare/flatten pipeline.
 */
class LinearFunctionSymbolAdapter(
    private val delegate: MathFunctionSymbol<Flt64>
) : LinearIntermediateSymbol<Flt64>, MathFunctionSymbol<Flt64> {
    override var name: String
        get() = delegate.name
        set(value) { delegate.name = value }

    override var displayName: String?
        get() = delegate.displayName
        set(value) { delegate.displayName = value }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = delegate.helperVariables

    /**
     * Expose positive slack variable as a MathLinearPolynomial<Flt64>, for framework compatibility.
     * Only meaningful when the delegate is a SlackFunction with withPositive=true.
     */
    val pos: MathLinearPolynomial<Flt64>? by lazy {
        val slack = delegate as? SlackFunction<Flt64> ?: return@lazy null
        slack.posVar?.let { v ->
            MathLinearPolynomial(
                monomials = listOf(MathLinearMonomial(Flt64.one, v)),
                constant = Flt64.zero
            )
        }
    }

    /**
     * Expose negative slack variable as a MathLinearPolynomial<Flt64>, for framework compatibility.
     * Only meaningful when the delegate is a SlackFunction with withNegative=true.
     */
    val neg: MathLinearPolynomial<Flt64>? by lazy {
        val slack = delegate as? SlackFunction<Flt64> ?: return@lazy null
        slack.negVar?.let { v ->
            MathLinearPolynomial(
                monomials = listOf(MathLinearMonomial(Flt64.one, v)),
                constant = Flt64.zero
            )
        }
    }

    /**
     * Expose the full slack expression (x + neg - pos) as a MathLinearPolynomial<Flt64>, for framework compatibility.
     * Only meaningful when the delegate is a SlackFunction.
     */
    val polyX: MathLinearPolynomial<Flt64>? by lazy {
        val slack = delegate as? SlackFunction<Flt64> ?: return@lazy null
        val xPoly = slack.x.asFlt64Poly()
        val coreMonomials = xPoly.monomials.mapNotNull { mono ->
            val sym = mono.symbol
            when (sym) {
                is fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *> ->
                    MathLinearMonomial(mono.coefficient, sym)
                is fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol<*> ->
                    MathLinearMonomial(mono.coefficient, sym)
                is fuookami.ospf.kotlin.math.symbol.Symbol -> {
                    // Math-level symbols can't be directly represented as core monomials
                    null
                }
                else -> null
            }
        }
        var result = MathLinearPolynomial(monomials = coreMonomials, constant = xPoly.constant)
        if (slack.withNegative && slack.negVar != null) {
            result = MathLinearPolynomial(
                monomials = result.monomials + MathLinearMonomial(Flt64.one, slack.negVar!!),
                constant = result.constant
            )
        }
        if (slack.withPositive && slack.posVar != null) {
            result = MathLinearPolynomial(
                monomials = result.monomials + MathLinearMonomial(-Flt64.one, slack.posVar!!),
                constant = result.constant
            )
        }
        result
    }

    override fun evaluate(values: Map<Symbol, Flt64>): Flt64? = delegate.evaluate(values)
    override fun register(model: AbstractLinearMetaModelF64): Try = delegate.register(model)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.variable.AddableTokenCollectionF64): Try {
        return delegate.registerAuxiliaryTokens(tokens)
    }

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val cached: Boolean get() = false
    override val dependencies: Set<fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol<*>> get() = emptySet()
    override val discrete: Boolean get() = false
    override val range: ExpressionRange<Flt64> get() = ExpressionRange()

    override fun flush(force: Boolean) {}
    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: LegacyAbstractTokenTable): Flt64? = null
    override fun toRawString(unfold: UInt64): String = name

    override val flattenedMonomials: LinearFlattenDataF64 get() = LinearFlattenDataF64(emptyList(), Flt64.zero)

    override fun toMathLinearInequality(): MathLinearInequality {
        return MathLinearInequality(MathLinearPolynomial(emptyList(), Flt64.zero), MathLinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun toMathQuadraticInequality(): MathQuadraticInequality {
        return MathQuadraticInequality(fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.zero), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun evaluate(tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListF64?, zeroIfNone: Boolean): Flt64? = delegate.evaluate(values)
}

/**
 * Evaluate a math LinearPolynomial given a map of Symbol -> T values.
 * Returns null if any symbol in the polynomial is missing from the map.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Field<T>> MathLinearPolynomial<T>.evaluate(values: Map<Symbol, T>): T? {
    val monomialsWithValues = monomials.mapNotNull { mono ->
        val sv = values[mono.symbol] ?: return null
        mono.coefficient * sv
    }
    var sum: T? = null
    for (term in monomialsWithValues) {
        sum = if (sum == null) term else sum + term
    }
    return (sum ?: constant) as T
}

/** Internal helper: cast T to Flt64 for constraint generation. Only valid when T=Flt64. */
@Suppress("UNCHECKED_CAST")
internal fun <T : Field<T>> T.asFlt64(): Flt64 = this as Flt64

/** Internal helper: get zero for type T. */
internal fun <T : Field<T>> zeroOf(): T = Flt64.zero as T

/** Internal helper: get one for type T. */
internal fun <T : Field<T>> oneOf(): T = Flt64.one as T

/** Internal helper: check if T is near zero. */
internal fun <T : Field<T>> T.isNearZero(tolerance: Double = NONZERO_TOLERANCE): Boolean {
    val d = this.asFlt64().toDouble()
    return d <= tolerance && d >= -tolerance
}

/** Internal helper: check if T is nonzero. */
internal fun <T : Field<T>> T.isNonZero(tolerance: Double = NONZERO_TOLERANCE): Boolean {
    val d = this.asFlt64().toDouble()
    return d > tolerance || d < -tolerance
}

/** Internal helper: convert MathLinearPolynomial<T> to MathLinearPolynomial<Flt64> for constraint generation. */
@Suppress("UNCHECKED_CAST")
internal fun <T : Field<T>> MathLinearPolynomial<T>.asFlt64Poly(): MathLinearPolynomial<Flt64> {
    return MathLinearPolynomial(
        monomials.map { MathLinearMonomial(it.coefficient.asFlt64(), it.symbol) },
        constant.asFlt64()
    )
}

// ========== Flt64 type aliases for backward compatibility ==========

typealias Flt64MathFunctionSymbol = MathFunctionSymbol<Flt64>

typealias Flt64AndFunction = AndFunction<Flt64>
typealias Flt64OrFunction = OrFunction<Flt64>
typealias Flt64NotFunction = NotFunction<Flt64>
typealias Flt64XorFunction = XorFunction<Flt64>
typealias Flt64IfFunction = IfFunction<Flt64>
typealias Flt64BinaryzationFunction = LinearFunctionSymbolAdapter
typealias Flt64MaskingFunction = LinearFunctionSymbolAdapter
typealias Flt64MaskingRangeFunction = LinearFunctionSymbolAdapter
typealias Flt64MaxFunction = MaxFunction<Flt64>
typealias Flt64MinFunction = MinFunction<Flt64>
typealias Flt64MinMaxFunction = MinMaxFunction<Flt64>
typealias Flt64MaxMinFunction = MaxMinFunction<Flt64>
typealias Flt64SlackFunction = LinearFunctionSymbolAdapter

// Newly migrated functions (2026-04-13)
typealias Flt64AbsFunction = AbsFunction<Flt64>
typealias Flt64SemiFunction = SemiFunction<Flt64>
typealias Flt64FloorFunction = FloorFunction<Flt64>
typealias Flt64CeilingFunction = CeilingFunction<Flt64>
typealias Flt64RoundingFunction = RoundingFunction<Flt64>
typealias Flt64ModFunction = ModFunction<Flt64>
typealias Flt64UnivariateLinearPiecewiseFunction = UnivariateLinearPiecewiseFunction<Flt64>
typealias Flt64SigmoidFunction = SigmoidFunction<Flt64>
typealias Flt64IfInFunction = IfInFunction<Flt64>
typealias Flt64IfThenFunction = IfThenFunction<Flt64>
typealias Flt64SameAsFunction = SameAsFunction<Flt64>
typealias Flt64OneOfFunction = OneOfFunction<Flt64>

// Newly migrated functions (2026-04-13 batch 2)
typealias Flt64FirstFunction = FirstFunction<Flt64>
typealias Flt64SatisfiedAmountFunction = SatisfiedAmountFunction<Flt64>
typealias Flt64BivariateLinearPiecewiseFunction = BivariateLinearPiecewiseFunction<Flt64>

// Phase 0 remaining functions (2026-04-14)
typealias Flt64BalanceTernaryzationFunction = BalanceTernaryzationFunction<Flt64>
typealias Flt64InStepRangeFunction = InStepRangeFunction<Flt64>
typealias Flt64SlackRangeFunction = SlackRangeFunction<Flt64>
typealias Flt64InequalityFunction = InequalityFunction<Flt64>
typealias Flt64SatisfiedAmountInequalityFunction = SatisfiedAmountInequalityFunction<Flt64>
typealias Flt64SinFunction = SinFunction<Flt64>
typealias Flt64CosFunction = CosFunction<Flt64>
typealias Flt64AnyFunction = AnyFunction<Flt64>
typealias Flt64AllFunction = AllFunction<Flt64>
typealias Flt64AtLeastInequalityFunction = AtLeastInequalityFunction<Flt64>
typealias Flt64NotAllFunction = NotAllFunction<Flt64>
typealias Flt64NumerableFunction = NumerableFunction<Flt64>
