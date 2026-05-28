/**
 * 蕴含-则函数符号 / If-Then function symbol
 *
 * 提供 [IfThenFunction]，实现条件-结果联动的线性化建模。
 *
 * Provides [IfThenFunction] for linearized modeling of conditional-consequent linkage.
 */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*

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
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "ifthen",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_ind")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_side")
    private val resultVar: AbstractVariableItem<*, *> by lazy { URealVar("${name}_y") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(indicatorVar, sideVar, resultVar)

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)
    }

    override val resultPolynomial: LinearPolynomial<V>
        get() = result

    override fun evaluate(values: Map<Symbol, V>): V? {
        val condValue = condition.evaluateWith(values) ?: return null
        return if (condValue gr converter.zero) {
            thenPoly.evaluateWith(values) ?: return null
        } else {
            converter.zero
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val bigMValue = bigM
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Nonzero indicator for condition
        allConstraints += nonzeroIndicatorConstraints(condition, indicatorVar, sideVar, bigM, tolerance, strictBoundary, "${name}_cond")

        // y - thenPoly <= M*(1 - indicator)  =>  y - thenPoly + M*indicator <= M
        val yMono = LinearMonomial(converter.one, resultVar)
        val negThenMonos = thenPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(yMono) + negThenMonos + LinearMonomial(bigMValue, indicatorVar), -thenPoly.constant),
            LinearPolynomial(emptyList(), bigMValue),
            Comparison.LE, "${name}_then_ub"
        )

        // y - thenPoly >= -M*(1 - indicator)  =>  y - thenPoly - M*indicator >= -M
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(yMono) + negThenMonos + LinearMonomial(-bigMValue, indicatorVar), -thenPoly.constant),
            LinearPolynomial(emptyList(), -bigMValue),
            Comparison.GE, "${name}_then_lb"
        )

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            condition: LinearPolynomial<V>,
            thenPoly: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): IfThenFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            IfThenFunction(condition, thenPoly, converter, bigM, name = name, displayName = displayName)

        /**
         * 约束输入工厂：从约束输入提取条件多项式，默认 then 多项式为一。
         * Constraint-input factory: extracts the condition polynomial and defaults thenPoly to one.
         */
        fun <V> from(
            inequality: LinearConstraintInput<V>,
            converter: IntoValue<V>,
            thenPoly: LinearPolynomial<V> = LinearPolynomial(emptyList(), converter.one),
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
                IfThenFunction(
                    condition = conditionPoly,
                    thenPoly = thenPoly,
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
