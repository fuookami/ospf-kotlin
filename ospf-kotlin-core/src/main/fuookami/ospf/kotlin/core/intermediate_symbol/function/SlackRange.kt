@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
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
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

class SlackRangeFunction<V>(
    val x: LinearPolynomial<V>,
    val threshold: V,
    val type: VariableType<*> = UContinuous,
    private val converter: IntoValue<V>,
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
        val unit = x.constant / x.constant
        LinearPolynomial(listOf(LinearMonomial(unit, upperVar)), x.constant - x.constant)
    }
    val lower: LinearPolynomial<V> by lazy {
        val unit = x.constant / x.constant
        LinearPolynomial(listOf(LinearMonomial(unit, lowerVar)), x.constant - x.constant)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val xFlt = converter.fromValue(xValue).toDouble()
        val threshFlt = converter.fromValue(threshold).toDouble()
        if (xFlt > threshFlt) {
            return converter.intoValue(Flt64(xFlt - threshFlt))
        } else {
            return converter.zero
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
        val xPoly = x.asFlt64Poly(converter)
        val threshPoly = LinearPolynomial(emptyList(), converter.fromValue(threshold))

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
            lhsLower, LinearPolynomial(emptyList(), -converter.fromValue(threshold)), Comparison.GE, "${name}_lower"
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
         * V-generic factory: accept LinearPolynomial<V> for x and V threshold.
         */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            threshold: V,
            type: VariableType<*> = UContinuous,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): SlackRangeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SlackRangeFunction(x, threshold, type, converter, name, displayName)

        /**
         * Factory: accept LinearPolynomial<Flt64> for x and Flt64 threshold.
         */
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            threshold: Flt64,
            type: VariableType<*> = UContinuous,
            name: String,
            displayName: String? = null
        ): SlackRangeFunction<Flt64> = SlackRangeFunction(x, threshold, type, flt64Converter, name, displayName)

        /**
         * Factory: accept LinearIntermediateSymbol for x and Flt64 threshold.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
            threshold: Flt64,
            type: VariableType<*> = UContinuous,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(
                x = x.toLinearPolynomial(),
                threshold = threshold,
                type = type,
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
        )

        /**
         * Factory: accept LinearIntermediateSymbol for x and UInt64 threshold.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
            threshold: UInt64,
            type: VariableType<*> = UContinuous,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(
                x = x.toLinearPolynomial(),
                threshold = threshold.toFlt64(),
                type = type,
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
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
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
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
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
        )
    }
}