@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * IfInFunction: Returns 1 if x is in range [lowerBound, upperBound], else 0.
 *
 * Uses BigM formulation with two binary variables (lby for lower bound, uby for upper bound)
 * combined via an AndFunction.
 */
class IfInFunction<T : Field<T>>(
    val x: LinearPolynomial<T>,
    val lowerBound: LinearPolynomial<T>,
    val upperBound: LinearPolynomial<T>,
    val epsilon: Flt64 = Flt64(1e-6),
    val bigM: Flt64 = Flt64(BIG_M_DEFAULT),
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    val lby: AbstractVariableItem<*, *> = BinVar("${name}_lby")
    val uby: AbstractVariableItem<*, *> = BinVar("${name}_uby")

    private val andFunc: Flt64AndFunction by lazy {
        Flt64AndFunction(
            listOf(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, lby)), Flt64.zero),
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, uby)), Flt64.zero)
            ),
            bigM,
            "${name}_and"
        )
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(lby, uby) + andFunc.helperVariables

    override fun evaluate(values: Map<Symbol, T>): T? {
        val xVal = x.evaluate(values)?.asFlt64()?.toDouble() ?: return null
        val lbVal = lowerBound.evaluate(values)?.asFlt64()?.toDouble() ?: return null
        val ubVal = upperBound.evaluate(values)?.asFlt64()?.toDouble() ?: return null
        return if (xVal >= lbVal - NONZERO_TOLERANCE && xVal <= ubVal + NONZERO_TOLERANCE) {
            oneOf<T>()
        } else {
            zeroOf<T>()
        }
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionF64): Try {
        // Only register own auxiliary tokens; andFunc registers its own via andFunc.register()
        return tokens.add(listOf(lby, uby))
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        // Register own auxiliary tokens (lby, uby); andFunc registers its own via andFunc.register()
        when (val r = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val xFlt = x.asFlt64Poly()
        val lbFlt = lowerBound.asFlt64Poly()
        val ubFlt = upperBound.asFlt64Poly()
        val mD = bigM.toDouble()
        val epsD = epsilon.toDouble()

        val allConstraints = mutableListOf<MathLinearInequality>()

        // Lower bound: x >= lowerBound  (via lby)
        // When lby=1: lowerBound - x <= 0
        // When lby=0: x - lowerBound <= M - epsilon
        allConstraints += MathLinearInequality(
            LinearPolynomial(
                lbFlt.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
                    xFlt.monomials.map { LinearMonomial(it.coefficient.unaryMinus(), it.symbol) } +
                    LinearMonomial(Flt64(mD), lby),
                (lbFlt.constant - xFlt.constant) + Flt64(-mD)
            ),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.LE,
            "${name}_lb_check"
        )

        allConstraints += MathLinearInequality(
            LinearPolynomial(
                xFlt.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
                    lbFlt.monomials.map { LinearMonomial(it.coefficient.unaryMinus(), it.symbol) } +
                    LinearMonomial(Flt64(-(mD + epsD)), lby),
                xFlt.constant - lbFlt.constant + Flt64(epsD)
            ),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.LE,
            "${name}_lb_enforce"
        )

        // Upper bound: x <= upperBound  (via uby)
        // When uby=1: x - upperBound <= 0
        // When uby=0: upperBound - x <= M - epsilon
        allConstraints += MathLinearInequality(
            LinearPolynomial(
                xFlt.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
                    ubFlt.monomials.map { LinearMonomial(it.coefficient.unaryMinus(), it.symbol) } +
                    LinearMonomial(Flt64(mD), uby),
                xFlt.constant - ubFlt.constant - Flt64(mD)
            ),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.LE,
            "${name}_ub_check"
        )

        allConstraints += MathLinearInequality(
            LinearPolynomial(
                ubFlt.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
                    xFlt.monomials.map { LinearMonomial(it.coefficient.unaryMinus(), it.symbol) } +
                    LinearMonomial(Flt64(-(mD + epsD)), uby),
                ubFlt.constant - xFlt.constant + Flt64(epsD)
            ),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.LE,
            "${name}_ub_enforce"
        )

        when (val r = addConstraints(model, allConstraints)) {
            null, is Ok<*, *, *> -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        return andFunc.register(model)
    }

    companion object {
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            lowerBound: LinearPolynomial<Flt64>,
            upperBound: LinearPolynomial<Flt64>,
            epsilon: Flt64 = Flt64(1e-6),
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction<Flt64> = IfInFunction(
            x = x,
            lowerBound = lowerBound,
            upperBound = upperBound,
            epsilon = epsilon,
            bigM = bigM ?: Flt64(BIG_M_DEFAULT),
            name = name,
            displayName = displayName
        )
    }
}
