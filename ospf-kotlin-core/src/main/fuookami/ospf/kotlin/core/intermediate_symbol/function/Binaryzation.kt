@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
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
 * Binaryzation function: converts a continuous variable to binary using Big-M.
 *
 * y = 1 if x > 0, y = 0 if x <= 0.
 */
class BinaryzationFunction<V>(
    val polynomial: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    override var name: String = "bin",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_bin")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val v = polynomial.evaluateWith(values) ?: return null
        return if (v gr converter.zero) converter.one else converter.zero
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
        val eps = converter.intoValue(Flt64(NONZERO_TOLERANCE))
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // x <= M*y
        val xMonos = polynomial.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        allConstraints += LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-bigM, resultVar), polynomial.constant),
            LinearPolynomial(emptyList(), zero), Comparison.LE, "${name}_bin_ub")

        // x >= epsilon*y
        allConstraints += LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-eps, resultVar), polynomial.constant),
            LinearPolynomial(emptyList(), zero), Comparison.GE, "${name}_bin_lb")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            polynomial: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): BinaryzationFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            BinaryzationFunction(polynomial, converter, bigM, name = name, displayName = displayName)

        operator fun invoke(
            polynomial: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): BinaryzationFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = BinaryzationFunction(polynomial, flt64Converter, bigM, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomial")
        fun fromLinearPolynomial(
            polynomial: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearFunctionSymbolAdapter(
            BinaryzationFunction(
                polynomial = polynomial.toLinearPolynomial(),
                bigM = bigM,
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
        )
    }
}
