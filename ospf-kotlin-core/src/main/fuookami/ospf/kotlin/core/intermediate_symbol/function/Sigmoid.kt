@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
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
 * Sigmoid/step function: y = 1 if condition > 0, else 0.
 *
 * Uses Big-M linearization with nonzero indicators.
 */
class SigmoidFunction<V>(
    val condition: LinearPolynomial<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "sigmoid",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
    private val tolerance: V = tolerance ?: Flt64(NONZERO_TOLERANCE) as V
    private val strictBoundary: V = strictBoundary ?: Flt64(STRICT_BOUNDARY) as V

    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_sig_ind")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_sig_side")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(indicatorVar, sideVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val condValue = condition.evaluateWith(values) ?: return null
        return if (condValue.asFlt64().toDouble() > 0.0) oneOf<V>() else zeroOf<V>()
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val mF = bigM.asFlt64()
        val tolF = tolerance.asFlt64()
        val sbF = strictBoundary.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicator: indicator = 1 iff condition != 0
        allConstraints += nonzeroIndicatorConstraints(condition.asFlt64Poly(), indicatorVar, sideVar, mF, tolF, sbF, "${name}_sig_nz")

        // indicator serves as the result: indicator = 1 when condition > 0

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    @Suppress("DEPRECATION")
    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val mF = bigM.asFlt64()
        val tolF = tolerance.asFlt64()
        val sbF = strictBoundary.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicator: indicator = 1 iff condition != 0
        allConstraints += nonzeroIndicatorConstraints(condition.asFlt64Poly(), indicatorVar, sideVar, mF, tolF, sbF, "${name}_sig_nz")

        // indicator serves as the result: indicator = 1 when condition > 0

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            condition: LinearPolynomial<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): SigmoidFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SigmoidFunction(condition, bigM, name = name, displayName = displayName)

        operator fun invoke(
            condition: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): SigmoidFunction<Flt64> = SigmoidFunction(condition, bigM, name = name, displayName = displayName)

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
                name = name,
                displayName = displayName
            )
        )
    }
}
