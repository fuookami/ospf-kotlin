@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInputV
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

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
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_if")
    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_if_nz")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_if_side")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, indicatorVar, sideVar)

    override val resultPolynomial: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)
    }

    val result: LinearPolynomial<V> get() = resultPolynomial

    override fun evaluate(values: Map<Symbol, V>): V? {
        val condValue = condition.evaluateWith(values) ?: return null
        return if (condValue gr converter.zero) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    private fun buildConstraints(): List<LinearInequality<V>> {
        val zero = converter.zero
        val one = converter.one
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Nonzero indicator for condition
        allConstraints += nonzeroIndicatorConstraintsV(condition, indicatorVar, sideVar, bigM, tolerance, strictBoundary, "${name}_if_nz")

        // result = indicator (if condition > 0, result = 1)
        allConstraints += LinearInequality(
            LinearPolynomial(
                listOf(LinearMonomial(one, resultVar), LinearMonomial(-one, indicatorVar)),
                zero
            ),
            LinearPolynomial(emptyList(), zero),
            Comparison.EQ, "${name}_if_eq"
        )

        return allConstraints
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        addConstraints(model, buildConstraints())?.let { return it }
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

        /**
         * 类型化工厂：从约束输入提取条件多项式。
         * Typed factory: extracts the condition polynomial from the constraint input.
         */
        fun <V> typed(
            inequality: LinearConstraintInputV<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            tolerance: V? = null,
            strictBoundary: V? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> {
            val conditionPoly = LinearPolynomial(
                inequality.flattenData.monomials.map { LinearMonomial(it.coefficient, it.symbol) },
                inequality.flattenData.constant
            )
            return LinearFunctionSymbolAdapter(
                IfFunction(
                    condition = conditionPoly,
                    converter = converter,
                    bigM = bigM,
                    tolerance = tolerance,
                    strictBoundary = strictBoundary,
                    name = name,
                    displayName = displayName
                ),
                converter = converter
            )
        }
    }
}
