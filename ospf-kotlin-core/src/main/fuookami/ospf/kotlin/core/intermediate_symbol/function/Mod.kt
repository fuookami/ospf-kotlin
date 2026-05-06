@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
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
 * Modulo function: y = x mod d.
 *
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
        val xD = converter.fromValue(xVal).toDouble()
        val dD = converter.fromValue(d).toDouble()
        return converter.intoValue(Flt64(xD % dD))
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val mF = converter.fromValue(bigM)
        val dF = converter.fromValue(d)
        val xF = x.asFlt64Poly(converter)
        val allConstraints = mutableListOf<LinearInequality<Flt64>>()
        val xMonos = xF.monomials.map { LinearMonomial(it.coefficient, it.symbol) }

        // r = x - d*q
        allConstraints += LinearInequality<Flt64>(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, rVar),
                LinearMonomial(dF, qVar)
            ) + xMonos.map { LinearMonomial(-it.coefficient, it.symbol) },
                -xF.constant),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_mod_decompose")

        // r >= 0 (from URealVar)
        // r < d => r <= d - epsilon
        val eps = Flt64(NONZERO_TOLERANCE)
        allConstraints += LinearInequality<Flt64>(
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, rVar)), Flt64.zero),
            LinearPolynomial(emptyList(), dF - eps), Comparison.LE, "${name}_mod_r_ub")

        // result = r
        allConstraints += LinearInequality<Flt64>(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, resultVar),
                LinearMonomial(-Flt64.one, rVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_mod_result")

        addConstraints(model, allConstraints, converter)?.let { return it }
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

        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            d: Flt64,
            bigM: Flt64? = null,
            name: String = "mod",
            displayName: String? = null
        ): ModFunction<Flt64> = ModFunction(x, d, bigM, flt64Converter, name, displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomial")
        fun fromLinearPolynomial(
            x: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>,
            d: Flt64,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            ModFunction<Flt64>(
                x = x.toLinearPolynomial(),
                d = d,
                bigM = bigM,
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
        )
    }
}