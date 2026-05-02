@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IntVar
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
 * Modulo function: y = x mod d.
 *
 * y = x - d*q where q = floor(x/d).
 */
class ModFunction<V>(
    val x: LinearPolynomial<V>,
    val d: V,
    bigM: V? = null,
    override var name: String = "mod",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V

    val qVar: AbstractVariableItem<*, *> = IntVar("${name}_q")
    val rVar: AbstractVariableItem<*, *> = URealVar("${name}_r")
    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_mod")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(qVar, rVar, resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xVal = x.evaluateWith(values) ?: return null
        val xD = xVal.asFlt64().toDouble()
        val dD = d.asFlt64().toDouble()
        @Suppress("UNCHECKED_CAST")
        return Flt64(xD % dD) as V
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val mF = bigM.asFlt64()
        val dF = d.asFlt64()
        val xF = x.asFlt64Poly()
        val allConstraints = mutableListOf<Flt64LinearInequality>()
        val xMonos = xF.monomials.map { LinearMonomial(it.coefficient, it.symbol) }

        // r = x - d*q
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, rVar),
                LinearMonomial(dF, qVar)
            ) + xMonos.map { LinearMonomial(-it.coefficient, it.symbol) },
                -xF.constant),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_mod_decompose")

        // r >= 0 (from URealVar)
        // r < d => r <= d - epsilon
        val eps = Flt64(NONZERO_TOLERANCE)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, rVar)), Flt64.zero),
            LinearPolynomial(emptyList(), dF - eps), Comparison.LE, "${name}_mod_r_ub")

        // result = r
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, resultVar),
                LinearMonomial(-Flt64.one, rVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_mod_result")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            d: V,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): ModFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            ModFunction(x, d, bigM, name = name, displayName = displayName)

        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            d: Flt64,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): ModFunction<Flt64> = ModFunction(x, d, bigM, name = name, displayName = displayName)

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
                name = name,
                displayName = displayName
            )
        )
    }
}
