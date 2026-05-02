@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IntVar
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
        return converter.intoValue(Flt64(kotlin.math.round(xVal.asFlt64().toDouble())))
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val mF = bigM.asFlt64()
        val xF = x.asFlt64Poly()
        val allConstraints = mutableListOf<Flt64LinearInequality>()
        val xMonos = xF.monomials.map { LinearMonomial(it.coefficient, it.symbol) }

        // k = floor(x), same as FloorFunction constraints
        // k <= x
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-Flt64.one, kVar), xF.constant),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_round_k_lb")

        // x < k + 1 => x <= k + 1 - eps
        val eps = Flt64(NONZERO_TOLERANCE)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-Flt64.one, kVar), xF.constant),
            LinearPolynomial(emptyList(), Flt64.one - eps), Comparison.LE, "${name}_round_k_ub")

        // b = x - k (fractional part)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, bVar),
                LinearMonomial(Flt64.one, kVar)
            ) + xMonos.map { LinearMonomial(-it.coefficient, it.symbol) },
                -xF.constant),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_round_decompose")

        // r = 1 if b >= 0.5 (round up)
        // b >= 0.5*r
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, bVar),
                LinearMonomial(-Flt64(0.5), rVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_round_r_lb")

        // b <= 0.5 + 0.5*(1-r) = 1 - 0.5*r => b + 0.5*r <= 1... wait
        // b <= 0.5 + (1-r)*0.5 + r*0 = 0.5 + 0.5 - 0.5*r = 1 - 0.5*r
        // Simplified: if b < 0.5 then r = 0, if b >= 0.5 then r = 1
        // b - 0.5*r <= 1 - r ... no.
        // b <= 0.5 + M*(1-r) => b + M*r <= M + 0.5
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, bVar),
                LinearMonomial(mF, rVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), mF + Flt64(0.5)), Comparison.LE, "${name}_round_r_ub")

        // b >= 0.5 - M*(1-r) => b - M*r >= 0.5 - M
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, bVar),
                LinearMonomial(-mF, rVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64(0.5) - mF), Comparison.GE, "${name}_round_r_lb2")

        // result = k + r
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, resultVar),
                LinearMonomial(-Flt64.one, kVar),
                LinearMonomial(-Flt64.one, rVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_round_result")

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

        val mF = bigM.asFlt64()
        val xF = x.asFlt64Poly()
        val allConstraints = mutableListOf<Flt64LinearInequality>()
        val xMonos = xF.monomials.map { LinearMonomial(it.coefficient, it.symbol) }

        // k = floor(x), same as FloorFunction constraints
        // k <= x
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-Flt64.one, kVar), xF.constant),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_round_k_lb")

        // x < k + 1 => x <= k + 1 - eps
        val eps = Flt64(NONZERO_TOLERANCE)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-Flt64.one, kVar), xF.constant),
            LinearPolynomial(emptyList(), Flt64.one - eps), Comparison.LE, "${name}_round_k_ub")

        // b = x - k (fractional part)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, bVar),
                LinearMonomial(Flt64.one, kVar)
            ) + xMonos.map { LinearMonomial(-it.coefficient, it.symbol) },
                -xF.constant),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_round_decompose")

        // r = 1 if b >= 0.5 (round up)
        // b >= 0.5*r
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, bVar),
                LinearMonomial(-Flt64(0.5), rVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_round_r_lb")

        // b <= 0.5 + 0.5*(1-r) = 1 - 0.5*r => b + 0.5*r <= 1... wait
        // b <= 0.5 + (1-r)*0.5 + r*0 = 0.5 + 0.5 - 0.5*r = 1 - 0.5*r
        // Simplified: if b < 0.5 then r = 0, if b >= 0.5 then r = 1
        // b - 0.5*r <= 1 - r ... no.
        // b <= 0.5 + M*(1-r) => b + M*r <= M + 0.5
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, bVar),
                LinearMonomial(mF, rVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), mF + Flt64(0.5)), Comparison.LE, "${name}_round_r_ub")

        // b >= 0.5 - M*(1-r) => b - M*r >= 0.5 - M
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, bVar),
                LinearMonomial(-mF, rVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64(0.5) - mF), Comparison.GE, "${name}_round_r_lb2")

        // result = k + r
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, resultVar),
                LinearMonomial(-Flt64.one, kVar),
                LinearMonomial(-Flt64.one, rVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_round_result")

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

        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): RoundingFunction<Flt64> = RoundingFunction(x, bigM, IntoValue.Flt64, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomial")
        fun fromLinearPolynomial(
            x: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            RoundingFunction<Flt64>(
                x = x.toLinearPolynomial(),
                bigM = bigM,
                converter = IntoValue.Flt64,
                name = name,
                displayName = displayName
            )
        )
    }
}
