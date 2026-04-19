@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
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
class MinMaxFunction<T : Field<T>>(
    val polynomials: List<LinearPolynomial<T>>,
    bigM: T? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T
    private val inner = MaxFunction(polynomials, bigM, name)

    val resultVar: AbstractVariableItem<*, *>
        get() = inner.resultVar
    val selectorVars: List<AbstractVariableItem<*, *>>
        get() = inner.selectorVars

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = inner.helperVariables

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.variable.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        return inner.evaluate(values)
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        return inner.register(model)
    }

    companion object {
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
            polynomials: List<LinearIntermediateSymbol<*>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            MinMaxFunction(
                polynomials = polynomials.map { it.asMathLinearPolynomial() },
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
class MaxMinFunction<T : Field<T>>(
    val polynomials: List<LinearPolynomial<T>>,
    bigM: T? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T
    private val inner = MinFunction(polynomials, bigM, name)

    val resultVar: AbstractVariableItem<*, *>
        get() = inner.resultVar
    val selectorVars: List<AbstractVariableItem<*, *>>
        get() = inner.selectorVars

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = inner.helperVariables

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.variable.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        return inner.evaluate(values)
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        return inner.register(model)
    }

    companion object {
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
            polynomials: List<LinearIntermediateSymbol<*>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            MaxMinFunction(
                polynomials = polynomials.map { it.asMathLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}
