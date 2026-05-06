@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.solver.value.IntoValue
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
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "if",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_if")
    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_if_nz")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_if_side")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, indicatorVar, sideVar)

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val condValue = condition.evaluateWith(values) ?: return null
        return if (converter.fromValue(condValue).toDouble() > 0.0) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    private fun buildConstraints(): List<LinearInequality<Flt64>> {
        val allConstraints = mutableListOf<LinearInequality<Flt64>>()

        // Nonzero indicator for condition
        allConstraints += nonzeroIndicatorConstraints(condition, indicatorVar, sideVar, bigM, tolerance, strictBoundary, converter, "${name}_if_nz")

        // result = indicator (if condition > 0, result = 1)
        allConstraints += LinearInequality<Flt64>(
            LinearPolynomial(
                listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-Flt64.one, indicatorVar)),
                Flt64.zero
            ),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.EQ, "${name}_if_eq"
        )

        return allConstraints
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        addConstraints(model, buildConstraints(), converter)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            condition: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): IfFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            IfFunction(condition, converter, bigM, name = name, displayName = displayName)

        operator fun invoke(
            condition: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfFunction<Flt64> = IfFunction(condition, flt64Converter, bigM, name = name, displayName = displayName)

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
                IfFunction(conditionPoly, flt64Converter, bigM, name = name, displayName = displayName),
            converter = flt64Converter
        
            )
        }
    }
}
