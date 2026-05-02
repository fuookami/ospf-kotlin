@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbolFlt64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.core.variable.UIntVar
import fuookami.ospf.kotlin.core.variable.VariableType
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

class SlackRangeFunction<V>(
    val x: LinearPolynomial<V>,
    val threshold: V,
    val type: VariableType<*> = UContinuous,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val upperVar: AbstractVariableItem<*, *> by lazy {
        if (type.isIntegerType) UIntVar("${name}_ub") else URealVar("${name}_ub")
    }
    private val lowerVar: AbstractVariableItem<*, *> by lazy {
        if (type.isIntegerType) UIntVar("${name}_lb") else URealVar("${name}_lb")
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(upperVar, lowerVar)

    val upper: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<V>(), upperVar)), zeroOf<V>())
    }
    val lower: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<V>(), lowerVar)), zeroOf<V>())
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val xFlt = xValue.asFlt64().toDouble()
        val threshFlt = threshold.asFlt64().toDouble()
        if (xFlt > threshFlt) {
            @Suppress("UNCHECKED_CAST")
            return Flt64(xFlt - threshFlt) as V
        } else {
            return zeroOf<V>()
        }
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val xPoly = x.asFlt64Poly()
        val threshPoly = LinearPolynomial(emptyList(), threshold.asFlt64())

        // upper >= x - threshold
        val lhsUpper = LinearPolynomial(
            xPoly.monomials + LinearMonomial(-Flt64.one, upperVar),
            xPoly.constant
        )
        val upperConstraint = LinearInequality<Flt64>(
            lhsUpper, threshPoly, Comparison.GE, "${name}_upper"
        )
        when (val result = model.addConstraint(relation = upperConstraint, name = upperConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // lower >= threshold - x
        val lhsLower = LinearPolynomial(
            xPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } + LinearMonomial(-Flt64.one, lowerVar),
            -xPoly.constant
        )
        val lowerConstraint = LinearInequality<Flt64>(
            lhsLower, LinearPolynomial(emptyList(), -threshold.asFlt64()), Comparison.GE, "${name}_lower"
        )
        when (val result = model.addConstraint(relation = lowerConstraint, name = lowerConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return ok
    }

    @Suppress("DEPRECATION")
    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val xPoly = x.asFlt64Poly()
        val threshPoly = LinearPolynomial(emptyList(), threshold.asFlt64())

        // upper >= x - threshold
        val lhsUpper = LinearPolynomial(
            xPoly.monomials + LinearMonomial(-Flt64.one, upperVar),
            xPoly.constant
        )
        val upperConstraint = LinearInequality<Flt64>(
            lhsUpper, threshPoly, Comparison.GE, "${name}_upper"
        )
        when (val result = model.addConstraint(relation = upperConstraint, name = upperConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // lower >= threshold - x
        val lhsLower = LinearPolynomial(
            xPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } + LinearMonomial(-Flt64.one, lowerVar),
            -xPoly.constant
        )
        val lowerConstraint = LinearInequality<Flt64>(
            lhsLower, LinearPolynomial(emptyList(), -threshold.asFlt64()), Comparison.GE, "${name}_lower"
        )
        when (val result = model.addConstraint(relation = lowerConstraint, name = lowerConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return ok
    }

    companion object {
        /**
         * Factory: accept LinearPolynomial<Flt64> for x and Flt64 threshold.
         */
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            threshold: Flt64,
            type: VariableType<*> = UContinuous,
            name: String,
            displayName: String? = null
        ): SlackRangeFunction<Flt64> = SlackRangeFunction(x, threshold, type, name, displayName)

        /**
         * Factory: accept LinearIntermediateSymbol for x and Flt64 threshold.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbolFlt64,
            threshold: Flt64,
            type: VariableType<*> = UContinuous,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(
                x = x.toLinearPolynomial(),
                threshold = threshold,
                type = type,
                name = name,
                displayName = displayName
            )
        )

        /**
         * Factory: accept LinearIntermediateSymbol for x and UInt64 threshold.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbolFlt64,
            threshold: UInt64,
            type: VariableType<*> = UContinuous,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(
                x = x.toLinearPolynomial(),
                threshold = threshold.toFlt64(),
                type = type,
                name = name,
                displayName = displayName
            )
        )

        /**
         * Factory: accept AbstractVariableItem for x and Flt64 threshold.
         */
        @JvmStatic
        operator fun invoke(
            x: AbstractVariableItem<*, *>,
            threshold: Flt64,
            type: VariableType<*> = UContinuous,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(
                x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
                threshold = threshold,
                type = type,
                name = name,
                displayName = displayName
            )
        )

        /**
         * Factory: accept AbstractVariableItem for x and UInt64 threshold.
         */
        @JvmStatic
        operator fun invoke(
            x: AbstractVariableItem<*, *>,
            threshold: UInt64,
            type: VariableType<*> = UContinuous,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(
                x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
                threshold = threshold.toFlt64(),
                type = type,
                name = name,
                displayName = displayName
            )
        )
    }
}