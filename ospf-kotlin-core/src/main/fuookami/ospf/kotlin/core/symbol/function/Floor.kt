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
 * 向下取整函数符号 / Floor function symbol
 *
 * 提供 [FloorFunction]，实现 y = floor(x) 的线性化建模。
 *
 * Provides [FloorFunction] for linearized modeling of y = floor(x).
 */

/**
 * Floor function: y = floor(x).
 *
 * Uses integer variable k = floor(x) with fractional binary variable b.
 * k <= x < k+1, b = x - k (0 or fractional), result = k.
 */
class FloorFunction<V>(
    val x: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    override var name: String = "floor",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val kVar: AbstractVariableItem<*, *> = IntVar("${name}_k")
    val bVar: AbstractVariableItem<*, *> = BinVar("${name}_b")
    val resultVar: AbstractVariableItem<*, *> = IntVar("${name}_floor")

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(kVar, bVar, resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xVal = x.evaluateWith(values) ?: return null
        return converter.intoValue(converter.fromValue(xVal).floor())
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
        val allConstraints = mutableListOf<LinearInequality<V>>()
        val xMonos = x.monomials.map { LinearMonomial(it.coefficient, it.symbol) }

        // k <= x
        allConstraints += LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-one, kVar), x.constant),
            LinearPolynomial(emptyList(), zero), Comparison.GE, "${name}_floor_lb")

        // k + 1 >= x => x <= k + 1
        allConstraints += LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-one, kVar), x.constant),
            LinearPolynomial(emptyList(), one), Comparison.LE, "${name}_floor_ub")

        // b = x - k => b + k - x = 0
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, bVar),
                LinearMonomial(one, kVar)
            ) + xMonos.map { LinearMonomial(-it.coefficient, it.symbol) },
                -x.constant),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_floor_decompose")

        // result = k
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, resultVar),
                LinearMonomial(-one, kVar)
            ), zero),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_floor_result")

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
        ): FloorFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            FloorFunction(x, converter, bigM, name = name, displayName = displayName)
    }
}
