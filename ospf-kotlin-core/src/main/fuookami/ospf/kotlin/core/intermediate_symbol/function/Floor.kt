@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IntVar
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

private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

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
        return converter.intoValue(Flt64(kotlin.math.floor(converter.fromValue(xVal).toDouble())))
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
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

        operator fun invoke(
            x: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): FloorFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = FloorFunction(x, flt64Converter, bigM, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomial")
        fun fromLinearPolynomial(
            x: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearFunctionSymbolAdapter(
            FloorFunction(
                x = x.toLinearPolynomial(),
                bigM = bigM,
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
        )
    }
}
