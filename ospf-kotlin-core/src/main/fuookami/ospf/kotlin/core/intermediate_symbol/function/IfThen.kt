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
 * If-Then function: `y = then_poly if condition > 0, else y = 0`.
 *
 * Uses Big-M linearization:
 * - When condition indicator b = 1: y = then_poly
 * - When condition indicator b = 0: y = 0
 *
 * @param condition the condition linear polynomial
 * @param thenPoly the "then" linear polynomial (activated when condition > 0)
 * @param bigM Big-M bound (default 1e6)
 * @param tolerance zero tolerance (default 1e-6)
 * @param strictBoundary strict boundary value (default 0.5)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class IfThenFunction<V>(
    val condition: LinearPolynomial<V>,
    val thenPoly: LinearPolynomial<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "ifthen",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
    private val tolerance: V = tolerance ?: Flt64(NONZERO_TOLERANCE) as V
    private val strictBoundary: V = strictBoundary ?: Flt64(STRICT_BOUNDARY) as V

    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_ind")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_side")
    private val resultVar: AbstractVariableItem<*, *> by lazy { URealVar("${name}_y") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(indicatorVar, sideVar, resultVar)

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<V>(), resultVar)), zeroOf<V>())
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val condValue = condition.evaluateWith(values) ?: return null
        return if (condValue.asFlt64().toDouble() > 0.0) {
            thenPoly.evaluateWith(values) ?: return null
        } else {
            zeroOf<V>()
        }
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val mF = bigM.asFlt64()
        val thenF = thenPoly.asFlt64Poly()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicator for condition
        allConstraints += nonzeroIndicatorConstraints(condition, indicatorVar, sideVar, bigM, tolerance, strictBoundary, "${name}_cond")

        // y - thenPoly <= M*(1 - indicator)  =>  y - thenPoly + M*indicator <= M
        val yMono = LinearMonomial(Flt64.one, resultVar)
        val negThenMonos = thenF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(yMono) + negThenMonos + LinearMonomial(mF, indicatorVar), -thenF.constant),
            LinearPolynomial(emptyList(), mF),
            Comparison.LE, "${name}_then_ub"
        )

        // y - thenPoly >= -M*(1 - indicator)  =>  y - thenPoly - M*indicator >= -M
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(yMono) + negThenMonos + LinearMonomial(-mF, indicatorVar), -thenF.constant),
            LinearPolynomial(emptyList(), -mF),
            Comparison.GE, "${name}_then_lb"
        )

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            condition: LinearPolynomial<V>,
            thenPoly: LinearPolynomial<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): IfThenFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            IfThenFunction(condition, thenPoly, bigM, name = name, displayName = displayName)

        operator fun invoke(
            condition: LinearPolynomial<Flt64>,
            thenPoly: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfThenFunction<Flt64> = IfThenFunction(condition, thenPoly, bigM, name = name, displayName = displayName)

        operator fun invoke(
            condition: LinearMonomial<Flt64>,
            thenPoly: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfThenFunction<Flt64> = IfThenFunction(
            condition = LinearPolynomial(listOf(condition), Flt64.zero),
            thenPoly = thenPoly,
            bigM = bigM,
            name = name,
            displayName = displayName
        )

        /**
         * Factory: accept LinearConstraintInput for framework compatibility.
         * Extracts the condition polynomial from the constraint input's flatten data.
         * Defaults thenPoly to Flt64.one (binary indicator).
         */
        @JvmStatic
        @JvmName("fromConstraintInput")
        operator fun invoke(
            inequality: fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInput,
            thenPoly: LinearPolynomial<Flt64> = LinearPolynomial(emptyList(), Flt64.one),
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val conditionPoly = LinearPolynomial(
                inequality.flattenData.monomials.map { LinearMonomial(it.coefficient, it.symbol) },
                inequality.flattenData.constant
            )
            return LinearFunctionSymbolAdapter(
                IfThenFunction(conditionPoly, thenPoly, bigM, name = name, displayName = displayName)
            )
        }
    }
}
