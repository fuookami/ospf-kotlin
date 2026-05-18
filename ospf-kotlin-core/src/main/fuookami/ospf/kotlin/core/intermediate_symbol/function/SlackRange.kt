@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.UIntVar
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.core.variable.VariableTypeKind
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel

private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * Slack range function: bounds x within [lb, ub] using slack variables.
 *
 * Original semantics: polyX = x + neg - pos, with constraints polyX leq ub / geq lb.
 * neg = lower slack (x below lb), pos = upper slack (x above ub).
 *
 * @param x the expression to bound
 * @param lb lower bound polynomial
 * @param ub upper bound polynomial
 * @param type variable type kind (UInteger or UContinuous)
 * @param constraint whether to add polyX leq ub / geq lb constraints
 * @param converter value type converter
 */
class SlackRangeFunction<V>(
    val x: LinearPolynomial<V>,
    val lb: LinearPolynomial<V>,
    val ub: LinearPolynomial<V>,
    val type: VariableTypeKind = UContinuous,
    val constraint: Boolean = true,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    val negVar: AbstractVariableItem<*, *> by lazy {
        if (type.isIntegerType) UIntVar("${name}_neg") else URealVar("${name}_neg")
    }
    val posVar: AbstractVariableItem<*, *> by lazy {
        if (type.isIntegerType) UIntVar("${name}_pos") else URealVar("${name}_pos")
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(negVar, posVar)

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, posVar)), converter.zero)

    val neg: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, negVar)), converter.zero)
    }
    val pos: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, posVar)), converter.zero)
    }

    val polyX: LinearPolynomial<V> by lazy {
        val unit = converter.one
        LinearPolynomial(
            x.monomials + LinearMonomial(unit, negVar) + LinearMonomial(-unit, posVar),
            x.constant
        )
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val lbValue = lb.evaluateWith(values) ?: return null
        val ubValue = ub.evaluateWith(values) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
        } else {
            converter.zero
        }
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        if (constraint) {
            val constraints = mutableListOf<LinearInequality<V>>()
            constraints += LinearInequality(polyX, ub, Comparison.LE, "${name}_ub")
            constraints += LinearInequality(polyX, lb, Comparison.GE, "${name}_lb")
            return addConstraints(model, constraints) ?: ok
        }
        return ok
    }

    companion object {
        /** V-generic factory with lb/ub polynomials. */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            lb: LinearPolynomial<V>,
            ub: LinearPolynomial<V>,
            type: VariableTypeKind = UContinuous,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): SlackRangeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SlackRangeFunction(x, lb, ub, type, constraint, converter, name, displayName)

        /** Flt64-specific factory with lb/ub polynomials. */
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            lb: LinearPolynomial<Flt64>,
            ub: LinearPolynomial<Flt64>,
            type: VariableTypeKind = UContinuous,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): SlackRangeFunction<Flt64> = SlackRangeFunction(
            x, lb, ub, type, constraint, flt64Converter, name, displayName
        )

        /** Flt64-specific factory with LinearIntermediateSymbol and lb/ub polynomials. */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
            lb: LinearPolynomial<Flt64>,
            ub: LinearPolynomial<Flt64>,
            type: VariableTypeKind = UContinuous,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(x.toLinearPolynomial(), lb, ub, type, constraint, flt64Converter, name, displayName),
            converter = flt64Converter
        )

        /** Flt64-specific factory with LinearIntermediateSymbol and scalar lb/ub. */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
            lb: Flt64,
            ub: Flt64,
            type: VariableTypeKind = UContinuous,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(
                x = x.toLinearPolynomial(),
                lb = LinearPolynomial(emptyList(), lb),
                ub = LinearPolynomial(emptyList(), ub),
                type = type,
                constraint = constraint,
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        )

        /** Flt64-specific factory with AbstractVariableItem and scalar lb/ub. */
        @JvmStatic
        operator fun invoke(
            x: AbstractVariableItem<*, *>,
            lb: Flt64,
            ub: Flt64,
            type: VariableTypeKind = UContinuous,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(
                x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
                lb = LinearPolynomial(emptyList(), lb),
                ub = LinearPolynomial(emptyList(), ub),
                type = type,
                constraint = constraint,
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        )

        /** Flt64-specific factory with LinearIntermediateSymbol and UInt64 lb/ub. */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
            lb: UInt64,
            ub: UInt64,
            type: VariableTypeKind = UContinuous,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = invoke(
            x = x,
            lb = lb.toFlt64(),
            ub = ub.toFlt64(),
            type = type,
            constraint = constraint,
            name = name,
            displayName = displayName
        )

        /** Flt64-specific factory with AbstractVariableItem and UInt64 lb/ub. */
        @JvmStatic
        operator fun invoke(
            x: AbstractVariableItem<*, *>,
            lb: UInt64,
            ub: UInt64,
            type: VariableTypeKind = UContinuous,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = invoke(
            x = x,
            lb = lb.toFlt64(),
            ub = ub.toFlt64(),
            type = type,
            constraint = constraint,
            name = name,
            displayName = displayName
        )

        /** Deprecated: single-threshold factory. Use lb/ub instead. */
        @Deprecated("Use lb/ub overload instead. This threshold-based overload will be removed in a future version.", level = DeprecationLevel.WARNING)
        @JvmStatic
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            threshold: Flt64,
            type: VariableTypeKind = UContinuous,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(
                x = x,
                lb = LinearPolynomial(emptyList(), -threshold),
                ub = LinearPolynomial(emptyList(), threshold),
                type = type,
                constraint = true,
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        )

        /** Deprecated: single-threshold factory with LinearIntermediateSymbol. */
        @Deprecated("Use lb/ub overload instead. This threshold-based overload will be removed in a future version.", level = DeprecationLevel.WARNING)
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
            threshold: Flt64,
            type: VariableTypeKind = UContinuous,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = invoke(
            x = x.toLinearPolynomial(),
            threshold = threshold,
            type = type,
            name = name,
            displayName = displayName
        )

        /** Deprecated: single-threshold factory with UInt64 threshold. */
        @Deprecated("Use lb/ub overload instead. This threshold-based overload will be removed in a future version.", level = DeprecationLevel.WARNING)
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
            threshold: UInt64,
            type: VariableTypeKind = UContinuous,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = invoke(
            x = x.toLinearPolynomial(),
            threshold = threshold.toFlt64(),
            type = type,
            name = name,
            displayName = displayName
        )

        /** Deprecated: single-threshold factory with AbstractVariableItem. */
        @Deprecated("Use lb/ub overload instead. This threshold-based overload will be removed in a future version.", level = DeprecationLevel.WARNING)
        @JvmStatic
        operator fun invoke(
            x: AbstractVariableItem<*, *>,
            threshold: Flt64,
            type: VariableTypeKind = UContinuous,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = invoke(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            threshold = threshold,
            type = type,
            name = name,
            displayName = displayName
        )

        /** Deprecated: single-threshold factory with UInt64 threshold and AbstractVariableItem. */
        @Deprecated("Use lb/ub overload instead. This threshold-based overload will be removed in a future version.", level = DeprecationLevel.WARNING)
        @JvmStatic
        operator fun invoke(
            x: AbstractVariableItem<*, *>,
            threshold: UInt64,
            type: VariableTypeKind = UContinuous,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = invoke(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            threshold = threshold.toFlt64(),
            type = type,
            name = name,
            displayName = displayName
        )
    }
}