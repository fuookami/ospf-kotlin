/**
 * 四舍五入函数符号 / Rounding function symbol
 *
 * 提供 [RoundingFunction]，实现 y = round(x) 的线性化建模。
 *
 * Provides [RoundingFunction] for linearized modeling of y = round(x).
 */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.token.AddableTokenCollection

/**
 * Rounding function: y = round(x).
 *
 * Uses integer variable k and binary r to handle the 0.5 case.
 */
class RoundingFunction<V>(
    val x: LinearPolynomial<V>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "round",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val kVar: AbstractVariableItem<*, *> = IntVar("${name}_k")
    val rVar: AbstractVariableItem<*, *> = BinVar("${name}_r")
    val bVar: AbstractVariableItem<*, *> = BinVar("${name}_b")
    val resultVar: AbstractVariableItem<*, *> = IntVar("${name}_round")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(kVar, rVar, bVar, resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xVal = x.evaluateWith(values) ?: return null
        return converter.intoValue(converter.fromValue(xVal).round())
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val half = converter.intoValue(Flt64(0.5))
        val eps = converter.intoValue(Flt64(NONZERO_TOLERANCE))
        val bigMValue = bigM
        val allConstraints = mutableListOf<LinearInequality<V>>()
        val xMonos = x.monomials.map { LinearMonomial(it.coefficient, it.symbol) }

        // k = floor(x), same as FloorFunction constraints
        // k <= x
        allConstraints += LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-one, kVar), x.constant),
            LinearPolynomial(emptyList(), zero), Comparison.GE, "${name}_round_k_lb")

        // x < k + 1 => x <= k + 1 - eps
        allConstraints += LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-one, kVar), x.constant),
            LinearPolynomial(emptyList(), one - eps), Comparison.LE, "${name}_round_k_ub")

        // b = x - k (fractional part)
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, bVar),
                LinearMonomial(one, kVar)
            ) + xMonos.map { LinearMonomial(-it.coefficient, it.symbol) },
                -x.constant),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_round_decompose")

        // r = 1 if b >= 0.5 (round up)
        // b >= 0.5*r
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, bVar),
                LinearMonomial(-half, rVar)
            ), zero),
            LinearPolynomial(emptyList(), zero), Comparison.GE, "${name}_round_r_lb")

        // b <= 0.5 + 0.5*(1-r) = 1 - 0.5*r => b + 0.5*r <= 1... wait
        // b <= 0.5 + (1-r)*0.5 + r*0 = 0.5 + 0.5 - 0.5*r = 1 - 0.5*r
        // Simplified: if b < 0.5 then r = 0, if b >= 0.5 then r = 1
        // b - 0.5*r <= 1 - r ... no.
        // b <= 0.5 + M*(1-r) => b + M*r <= M + 0.5
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, bVar),
                LinearMonomial(bigMValue, rVar)
            ), zero),
            LinearPolynomial(emptyList(), bigMValue + half), Comparison.LE, "${name}_round_r_ub")

        // b >= 0.5 - M*(1-r) => b - M*r >= 0.5 - M
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, bVar),
                LinearMonomial(-bigMValue, rVar)
            ), zero),
            LinearPolynomial(emptyList(), half - bigMValue), Comparison.GE, "${name}_round_r_lb2")

        // result = k + r
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, resultVar),
                LinearMonomial(-one, kVar),
                LinearMonomial(-one, rVar)
            ), zero),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_round_result")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): RoundingFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            RoundingFunction(x, bigM, converter, name = name, displayName = displayName)
    }
}
