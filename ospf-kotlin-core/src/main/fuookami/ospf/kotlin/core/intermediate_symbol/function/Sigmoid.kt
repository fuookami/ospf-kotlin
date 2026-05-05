@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * Sigmoid/step function: y = 1 if condition > 0, else 0.
 *
 * Uses Big-M linearization with nonzero indicators.
 */
class SigmoidFunction<V>(
    val condition: LinearPolynomial<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "sigmoid",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_sig_ind")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_sig_side")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(indicatorVar, sideVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val condValue = condition.evaluateWith(values) ?: return null
        return if (converter.fromValue(condValue).toDouble() > 0.0) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<Flt64>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<Flt64>): Try {
        val mF = converter.fromValue(bigM)
        val tolF = converter.fromValue(tolerance)
        val sbF = converter.fromValue(strictBoundary)
        val allConstraints = mutableListOf<LinearInequality<Flt64>>()

        // Nonzero indicator: indicator = 1 iff condition != 0
        allConstraints += nonzeroIndicatorConstraints(condition.asFlt64Poly(converter), indicatorVar, sideVar, mF, tolF, sbF, "${name}_sig_nz")

        // indicator serves as the result: indicator = 1 when condition > 0

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            condition: LinearPolynomial<V>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "sigmoid",
            displayName: String? = null
        ): SigmoidFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SigmoidFunction(condition, bigM, converter = converter, name = name, displayName = displayName)

        operator fun invoke(
            condition: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String = "sigmoid",
            displayName: String? = null
        ): SigmoidFunction<Flt64> = SigmoidFunction(condition, bigM, converter = flt64Converter, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomial")
        fun fromLinearPolynomial(
            condition: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SigmoidFunction<Flt64>(
                condition = condition.toLinearPolynomial(),
                bigM = bigM,
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
        )
    }
}
