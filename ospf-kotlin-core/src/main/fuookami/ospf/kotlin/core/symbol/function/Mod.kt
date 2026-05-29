/** 取模函数符号 / Modulo function symbol */
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
 * 取模函数符号 / Modulo function symbol
 *
 * 提供 [ModFunction]，实现 y = x mod d 的线性化建模。
 *
 * Provides [ModFunction] for linearized modeling of y = x mod d.
 */

/**
 * 取模函数：y = x mod d。
 * Modulo function: y = x mod d.
 *
 * y = x - d*q，其中 q = floor(x/d)。
 * y = x - d*q where q = floor(x/d).
 */
class ModFunction<V>(
    val x: LinearPolynomial<V>,
    val d: V,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "mod",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val qVar: AbstractVariableItem<*, *> = IntVar("${name}_q")
    val rVar: AbstractVariableItem<*, *> = URealVar("${name}_r")
    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_mod")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(qVar, rVar, resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xVal = x.evaluateWith(values) ?: return null
        val xFlt = converter.fromValue(xVal)
        val dFlt = converter.fromValue(d)
        return converter.intoValue(xFlt % dFlt)
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

        // r = x - d*q
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, rVar),
                LinearMonomial(d, qVar)
            ) + xMonos.map { LinearMonomial(-it.coefficient, it.symbol) },
                -x.constant),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_mod_decompose")

        // r >= 0 (from URealVar)
        // r < d => r <= d - epsilon
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(one, rVar)), zero),
            LinearPolynomial(emptyList(), d - eps), Comparison.LE, "${name}_mod_r_ub")

        // result = r
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, resultVar),
                LinearMonomial(-one, rVar)
            ), zero),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_mod_result")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            d: V,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "mod",
            displayName: String? = null
        ): ModFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            ModFunction(x, d, bigM, converter, name, displayName)
    }
}
