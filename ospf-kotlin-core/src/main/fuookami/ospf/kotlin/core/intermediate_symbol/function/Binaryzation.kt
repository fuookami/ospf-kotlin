@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

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
 * Binaryzation function: converts a continuous variable to binary using Big-M.
 *
 * y = 1 if x > 0, y = 0 if x <= 0.
 */
class BinaryzationFunction<V>(
    val polynomial: LinearPolynomial<V>,
    bigM: V? = null,
    override var name: String = "bin",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_bin")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val v = polynomial.evaluateWith(values) ?: return null
        return if (v.asFlt64().toDouble() > 0.0) oneOf<V>() else zeroOf<V>()
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val mF = bigM.asFlt64()
        val polyF = polynomial.asFlt64Poly()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // x <= M*y
        val xMonos = polyF.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-mF, resultVar), polyF.constant),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${name}_bin_ub")

        // x >= epsilon*y
        val eps = Flt64(NONZERO_TOLERANCE)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-eps, resultVar), polyF.constant),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_bin_lb")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomial: LinearPolynomial<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): BinaryzationFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            BinaryzationFunction(polynomial, bigM, name = name, displayName = displayName)

        operator fun invoke(
            polynomial: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): BinaryzationFunction<Flt64> = BinaryzationFunction(polynomial, bigM, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomial")
        fun fromLinearPolynomial(
            polynomial: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            BinaryzationFunction<Flt64>(
                polynomial = polynomial.toLinearPolynomial(),
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}
