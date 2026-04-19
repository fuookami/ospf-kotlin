@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.core.variable.URealVar
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
 * Ceiling function symbol: `y = ceil(x / d)` where `x` is a LinearPolynomial.
 *
 * Decomposition:
 * - Create helper variables: `q` (IntVar for quotient) and `r` (URealVar for remainder, 0 <= r < d)
 * - ConstraintF64: `x = d * q - r`
 * - The ceiling result is `q`
 *
 * When `x = d*q - r` with `0 <= r < d`:
 * - If r = 0: x = d*q, so q = x/d = ceil(x/d)
 * - If r > 0: x < d*q, so q > x/d, and q = ceil(x/d)
 *
 * @param x the input linear polynomial
 * @param d the divisor (default 1)
 * @param epsilon small positive value to enforce strict upper bound on remainder (default 1e-6)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class CeilingFunction<T : Field<T>>(
    val x: LinearPolynomial<T>,
    val d: Flt64 = Flt64.one,
    val epsilon: Flt64 = Flt64(1e-6),
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    private val qVar: AbstractVariableItem<*, *> by lazy { IntVar("${name}_q") }
    private val rVar: AbstractVariableItem<*, *> by lazy { URealVar("${name}_r") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(qVar, rVar)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.variable.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    /**
     * Linear polynomial representing the quotient (ceiling result): `q`.
     * Exposed for framework reference (e.g. in objectives).
     */
    val q: LinearPolynomial<T> by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<T>(), qVar)), zeroOf<T>())
    }

    /**
     * Linear polynomial representing the remainder: `r`.
     * Exposed for framework reference.
     */
    val r: LinearPolynomial<T> by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<T>(), rVar)), zeroOf<T>())
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        val xValue = x.evaluate(values) ?: return null
        val doubleVal = xValue.asFlt64().toDouble()
        val dVal = d.toDouble()
        @Suppress("UNCHECKED_CAST")
        return Flt64(kotlin.math.ceil(doubleVal / dVal)) as T
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        // Add helper variables to the model
        when (val result = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val xPoly = x.asFlt64Poly()
        val dVal = d

        // ConstraintF64: x = d * q - r  =>  x eq (d*q - r)
        val dqPoly = LinearPolynomial(listOf(LinearMonomial(dVal, qVar)), Flt64.zero)
        val rPoly = LinearPolynomial(listOf(LinearMonomial(-Flt64.one, rVar)), Flt64.zero)
        val rhs = LinearPolynomial(dqPoly.monomials + rPoly.monomials, dqPoly.constant + rPoly.constant)
        val eqConstraint = MathLinearInequality(xPoly, rhs, Comparison.EQ, "${name}_div_eq")
        when (val result = model.addConstraint(relation = eqConstraint, name = eqConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Bound constraints on remainder: 0 <= r < d
        // r >= 0 (URealVar is already non-negative by default)
        val rLower = LinearPolynomial(listOf(LinearMonomial(Flt64.one, rVar)), Flt64.zero)
        val rLowerRhs = LinearPolynomial(emptyList(), Flt64.zero)
        val rLowerConstraint = MathLinearInequality(rLower, rLowerRhs, Comparison.GE, "${name}_r_ge_0")
        when (val result = model.addConstraint(relation = rLowerConstraint, name = rLowerConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // r <= d - epsilon  (to enforce r < d strictly)
        val rUpper = LinearPolynomial(listOf(LinearMonomial(Flt64.one, rVar)), Flt64.zero)
        val rUpperRhs = LinearPolynomial(emptyList(), dVal - epsilon)
        val rUpperConstraint = MathLinearInequality(rUpper, rUpperRhs, Comparison.LE, "${name}_r_lt_d")
        when (val result = model.addConstraint(relation = rUpperConstraint, name = rUpperConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return ok
    }

    companion object {
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            d: Flt64 = Flt64.one,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): CeilingFunction<Flt64> = CeilingFunction(
            x = x,
            d = d,
            epsilon = epsilon,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            x: LinearMonomial<Flt64>,
            d: Flt64 = Flt64.one,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): CeilingFunction<Flt64> = CeilingFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            d = d,
            epsilon = epsilon,
            name = name,
            displayName = displayName
        )
    }
}
