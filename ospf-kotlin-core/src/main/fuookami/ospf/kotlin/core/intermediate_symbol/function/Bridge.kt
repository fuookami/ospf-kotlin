@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
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
 * Bridge function: activates a variable when a condition polynomial is nonzero.
 *
 * When condition > 0: result = value polynomial.
 * When condition <= 0: result = 0.
 */
class BridgeFunction<V>(
    val condition: LinearPolynomial<V>,
    val value: LinearPolynomial<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "bridge",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
    private val tolerance: V = tolerance ?: Flt64(NONZERO_TOLERANCE) as V
    private val strictBoundary: V = strictBoundary ?: Flt64(STRICT_BOUNDARY) as V

    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_bridge_ind")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_bridge_side")
    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_bridge_result")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(indicatorVar, sideVar, resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val condValue = condition.evaluateWith(values) ?: return null
        if (condValue.asFlt64().toDouble() <= 0.0) return zeroOf<V>()
        return value.evaluateWith(values) ?: return null
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val mF = bigM.asFlt64()
        val tolF = tolerance.asFlt64()
        val sbF = strictBoundary.asFlt64()
        val condF = condition.asFlt64Poly()
        val valF = value.asFlt64Poly()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicator for condition
        allConstraints += nonzeroIndicatorConstraints(condF, indicatorVar, sideVar, mF, tolF, sbF, "${name}_bridge_cond")

        // result <= value + M*(1-indicator)
        val valMonos = valF.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar)) +
                valMonos.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(mF, indicatorVar),
                -valF.constant),
            LinearPolynomial(emptyList(), mF), Comparison.LE, "${name}_bridge_val_ub")

        // result >= value - M*(1-indicator)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar)) +
                valMonos.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(-mF, indicatorVar),
                -valF.constant),
            LinearPolynomial(emptyList(), -mF), Comparison.GE, "${name}_bridge_val_lb")

        // result <= M*indicator
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, resultVar),
                LinearMonomial(-mF, indicatorVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${name}_bridge_act")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            condition: LinearPolynomial<V>,
            value: LinearPolynomial<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): BridgeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            BridgeFunction(condition, value, bigM, name = name, displayName = displayName)

        operator fun invoke(
            condition: LinearPolynomial<Flt64>,
            value: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): BridgeFunction<Flt64> = BridgeFunction(condition, value, bigM, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomials")
        fun fromLinearPolynomials(
            condition: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>,
            value: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            BridgeFunction<Flt64>(
                condition = condition.toLinearPolynomial(),
                value = value.toLinearPolynomial(),
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}
