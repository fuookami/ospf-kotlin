@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64
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
 * If function: `y = 1 if condition > 0, else y = 0`.
 *
 * Uses Big-M linearization with a nonzero indicator.
 *
 * @param condition the condition linear polynomial
 * @param bigM Big-M bound (default 1e6)
 * @param tolerance zero tolerance (default 1e-6)
 * @param strictBoundary strict boundary value (default 0.5)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class IfFunction<V>(
    val condition: LinearPolynomial<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "if",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
    private val tolerance: V = tolerance ?: Flt64(NONZERO_TOLERANCE) as V
    private val strictBoundary: V = strictBoundary ?: Flt64(STRICT_BOUNDARY) as V

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_if")
    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_if_nz")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_if_side")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, indicatorVar, sideVar)

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<V>(), resultVar)), zeroOf<V>())
    }

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
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicator for condition
        allConstraints += nonzeroIndicatorConstraints(condition, indicatorVar, sideVar, bigM, tolerance, strictBoundary, "${name}_if_nz")

        // result = indicator (if condition > 0, result = 1)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(
                listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-Flt64.one, indicatorVar)),
                Flt64.zero
            ),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.EQ, "${name}_if_eq"
        )

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

        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicator for condition
        allConstraints += nonzeroIndicatorConstraints(condition, indicatorVar, sideVar, bigM, tolerance, strictBoundary, "${name}_if_nz")

        // result = indicator (if condition > 0, result = 1)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(
                listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-Flt64.one, indicatorVar)),
                Flt64.zero
            ),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.EQ, "${name}_if_eq"
        )

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            condition: LinearPolynomial<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): IfFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            IfFunction(condition, bigM, name = name, displayName = displayName)

        operator fun invoke(
            condition: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfFunction<Flt64> = IfFunction(condition, bigM, name = name, displayName = displayName)

        operator fun invoke(
            condition: LinearMonomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfFunction<Flt64> = IfFunction(
            condition = LinearPolynomial(listOf(condition), Flt64.zero),
            bigM = bigM,
            name = name,
            displayName = displayName
        )

        /**
         * Factory: accept LinearConstraintInput for framework compatibility.
         * Extracts the condition polynomial from the constraint input's flatten data.
         */
        @JvmStatic
        @JvmName("fromConstraintInput")
        operator fun invoke(
            inequality: fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInput,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val conditionPoly = LinearPolynomial(
                inequality.flattenData.monomials.map { LinearMonomial(it.coefficient, it.symbol) },
                inequality.flattenData.constant
            )
            return LinearFunctionSymbolAdapter(
                IfFunction(conditionPoly, bigM, name = name, displayName = displayName)
            )
        }
    }
}
