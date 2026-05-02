@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Quadratic-to-linear bridge function.
 *
 * Takes a quadratic polynomial `input` and creates a continuous result variable `y`
 * such that `y = input`. This allows quadratic expressions to be used as inputs
 * to downstream linear function symbols.
 *
 * When registered with a quadratic model, adds the quadratic equality constraint
 * `input = y`. When registered with a linear-only model, only adds the result variable
 * (the quadratic constraint must be handled externally).
 *
 * @param input the quadratic polynomial expression
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class QuadraticLinearBridgeFunction<V>(
    val input: QuadraticPolynomial<Flt64>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_qy")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    val result: LinearPolynomial<V> by lazy {
        @Suppress("UNCHECKED_CAST")
        LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar)), Flt64.zero) as LinearPolynomial<V>
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        var sum = input.constant
        for (m in input.monomials) {
            val v1Flt = (values[m.symbol1] ?: return null).asFlt64()
            val term = if (m.symbol2 != null) {
                val v2Flt = (values[m.symbol2!!] ?: return null).asFlt64()
                m.coefficient * v1Flt * v2Flt
            } else {
                m.coefficient * v1Flt
            }
            sum += term
        }
        @Suppress("UNCHECKED_CAST")
        return sum as V
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is fuookami.ospf.kotlin.utils.functional.Ok -> {}
            is fuookami.ospf.kotlin.utils.functional.Failed -> return fuookami.ospf.kotlin.utils.functional.Failed(result.error)
            is fuookami.ospf.kotlin.utils.functional.Fatal -> return fuookami.ospf.kotlin.utils.functional.Fatal(result.errors)
        }

        if (model is AbstractQuadraticMetaModel<*>) {
            @Suppress("UNCHECKED_CAST")
            val qModel = model as AbstractQuadraticMetaModel<V>

            // input - result_var = 0
            val negResultMonomials = input.monomials + QuadraticMonomial.linear(
                -Flt64.one, resultVar
            )
            val lhs = QuadraticPolynomial(negResultMonomials, input.constant)
            val rhs = QuadraticPolynomial<Flt64>(constant = Flt64.zero)
            val constraint = QuadraticInequality(lhs, rhs, Comparison.EQ, name)
            when (val r = qModel.addConstraint(relation = constraint, group = null, name = name)) {
                is fuookami.ospf.kotlin.utils.functional.Ok -> {}
                is fuookami.ospf.kotlin.utils.functional.Failed -> return fuookami.ospf.kotlin.utils.functional.Failed(r.error)
                is fuookami.ospf.kotlin.utils.functional.Fatal -> return fuookami.ospf.kotlin.utils.functional.Fatal(r.errors)
            }
        }

        return ok
    }

    companion object {
        /**
         * Generic factory: creates a bridge from a Flt64 quadratic polynomial.
         */
        @JvmStatic
        @JvmName("of")
        operator fun <V> invoke(
            input: QuadraticPolynomial<Flt64>,
            name: String,
            displayName: String? = null
        ): QuadraticLinearBridgeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            QuadraticLinearBridgeFunction(input, name, displayName)

        /**
         * Flt64 factory.
         */
        operator fun invoke(
            input: QuadraticPolynomial<Flt64>,
            name: String,
            displayName: String? = null
        ): QuadraticLinearBridgeFunction<Flt64> =
            QuadraticLinearBridgeFunction(input, name, displayName)

        /**
         * Convenience: bridge from left * right (creates ProductFunction internally).
         */
        @JvmStatic
        @JvmName("ofProduct")
        operator fun <V> invoke(
            left: LinearPolynomial<Flt64>,
            right: LinearPolynomial<Flt64>,
            name: String,
            displayName: String? = null
        ): QuadraticLinearBridgeFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            val product = ProductFunction<V>(left, right, name = "${name}_prod")
            return QuadraticLinearBridgeFunction(product.toMathQuadraticPolynomial(), name, displayName)
        }

        /**
         * Convenience: bridge from left * right (Flt64).
         */
        @JvmStatic
        @JvmName("fromProduct")
        operator fun invoke(
            left: LinearPolynomial<Flt64>,
            right: LinearPolynomial<Flt64>,
            name: String,
            displayName: String? = null
        ): QuadraticLinearBridgeFunction<Flt64> {
            val product = ProductFunction<Flt64>(left, right, name = "${name}_prod")
            return QuadraticLinearBridgeFunction(product.toMathQuadraticPolynomial(), name, displayName)
        }

        /**
         * Convenience: bridge directly from an existing ProductFunction.
         */
        fun <V> fromProduct(
            product: ProductFunction<V>,
            name: String = product.name,
            displayName: String? = product.displayName
        ): QuadraticLinearBridgeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            QuadraticLinearBridgeFunction(product.toMathQuadraticPolynomial(), name, displayName)
    }
}
