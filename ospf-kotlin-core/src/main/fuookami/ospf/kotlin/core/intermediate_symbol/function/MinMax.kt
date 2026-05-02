@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbolFlt64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * MinMax function: result = max(polynomials[0], polynomials[1], ...).
 *
 * Named "MinMax" because it computes the minimum of the maximum values
 * in optimization contexts. Delegates to MaxFunction internally.
 */
class MinMaxFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
    private val inner = MaxFunction(polynomials, bigM, name)

    val resultVar: AbstractVariableItem<*, *>
        get() = inner.resultVar
    val selectorVars: List<AbstractVariableItem<*, *>>
        get() = inner.selectorVars

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = inner.helperVariables

    override fun evaluate(values: Map<Symbol, V>): V? {
        return inner.evaluate(values)
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        return inner.register(model)
    }

    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): MinMaxFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MinMaxFunction(polynomials, bigM, name, displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): MinMaxFunction<Flt64> = MinMaxFunction(polynomials, bigM, name, displayName)

        /**
         * Factory: accept List<LinearIntermediateSymbol> for framework compatibility.
         */
        @JvmStatic
        @JvmName("fromSymbols")
        operator fun invoke(
            polynomials: List<LinearIntermediateSymbolFlt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            MinMaxFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}

/**
 * MaxMin function: result = min(polynomials[0], polynomials[1], ...).
 *
 * Named "MaxMin" because it computes the maximum of the minimum values
 * in optimization contexts. Delegates to MinFunction internally.
 */
class MaxMinFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
    private val inner = MinFunction(polynomials, bigM, name)

    val resultVar: AbstractVariableItem<*, *>
        get() = inner.resultVar
    val selectorVars: List<AbstractVariableItem<*, *>>
        get() = inner.selectorVars

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = inner.helperVariables

    override fun evaluate(values: Map<Symbol, V>): V? {
        return inner.evaluate(values)
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        return inner.register(model)
    }

    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): MaxMinFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaxMinFunction(polynomials, bigM, name, displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): MaxMinFunction<Flt64> = MaxMinFunction(polynomials, bigM, name, displayName)

        /**
         * Factory: accept List<LinearIntermediateSymbol> for framework compatibility.
         */
        @JvmStatic
        @JvmName("fromSymbols")
        operator fun invoke(
            polynomials: List<LinearIntermediateSymbolFlt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            MaxMinFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}