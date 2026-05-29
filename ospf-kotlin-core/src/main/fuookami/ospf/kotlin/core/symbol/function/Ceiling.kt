/** 向上取整函数符号 / Ceiling function symbol */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 向上取整函数符号 / Ceiling function symbol
 *
 * 提供 [CeilingFunction]，实现 y = ceil(x) 的线性化建模。
 *
 * Provides [CeilingFunction] for linearized modeling of y = ceil(x).
 */

/**
 * 向上取整函数：y = ceil(x)。
 * Ceiling function: y = ceil(x).
 *
 * 使用整数变量 k = ceil(x) 和小数二值变量 b。
 * Uses integer variable k = ceil(x) with fractional binary variable b.
 * k - 1 < x <= k, b = x - floor(x), k = floor(x) + b.
 */
class CeilingFunction<V>(
    val x: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    override var name: String = "ceil",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val kVar: AbstractVariableItem<*, *> = IntVar("${name}_k")
    val bVar: AbstractVariableItem<*, *> = BinVar("${name}_b")
    val resultVar: AbstractVariableItem<*, *> = IntVar("${name}_ceil")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(kVar, bVar, resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xVal = x.evaluateWith(values) ?: return null
        return converter.intoValue(converter.fromValue(xVal).ceil())
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
        val eps = converter.intoValue(Flt64(NONZERO_TOLERANCE))
        val allConstraints = mutableListOf<LinearInequality<V>>()
        val xMonos = x.monomials.map { LinearMonomial(it.coefficient, it.symbol) }

        // x <= k
        allConstraints += LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-one, kVar), x.constant),
            LinearPolynomial(emptyList(), zero), Comparison.LE, "${name}_ceil_ub")

        // x > k - 1 => x >= k - 1 + epsilon
        allConstraints += LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-one, kVar), x.constant),
            LinearPolynomial(emptyList(), one - eps), Comparison.GE, "${name}_ceil_lb")

        // b = x - floor(x) => k = x + 1 - b => b - k + x = -1 + ... simplified:
        // k = x + b, so k - x = b
        // k - x - b = 0
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, kVar),
                LinearMonomial(-one, bVar)
            ) + xMonos.map { LinearMonomial(-it.coefficient, it.symbol) },
                -x.constant),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_ceil_decompose")

        // result = k
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, resultVar),
                LinearMonomial(-one, kVar)
            ), zero),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_ceil_result")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): CeilingFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            CeilingFunction(x, converter, bigM, name = name, displayName = displayName)
    }
}
