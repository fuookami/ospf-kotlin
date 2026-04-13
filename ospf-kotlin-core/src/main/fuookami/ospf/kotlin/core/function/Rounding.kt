@file:Suppress("unused")

package fuookami.ospf.kotlin.core.function

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.frontend.variable.BinVar
import fuookami.ospf.kotlin.core.frontend.variable.IntVar
import fuookami.ospf.kotlin.core.frontend.variable.URealVar
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
 * Rounding function symbol: `y = round(x / d)` where `x` is a LinearPolynomial.
 *
 * Decomposition:
 * - Create helper variables: `q` (IntVar for quotient) and `r` (URealVar for remainder, -d/2 <= r < d/2)
 * - Constraint: `x = d * q + r`
 * - The round result is `q`
 *
 * Note: Since URealVar is non-negative, we use two remainder variables or shift the
 * remainder range. Here we use `r >= 0` with `r < d` and interpret:
 * - If r < d/2: q = floor(x/d) = round(x/d)
 * - If r >= d/2: q = floor(x/d) but we need q+1 for rounding
 *
 * A simpler approach: use `x = d*q + r` with `0 <= r < d`, then the rounding
 * is `q` when `r < d/2` and `q+1` when `r >= d/2`. For MILP, we can add a binary
 * variable to handle the rounding threshold.
 *
 * @param x the input linear polynomial
 * @param d the divisor (default 1)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class RoundingFunction<T : Field<T>>(
    val x: LinearPolynomial<T>,
    val d: Flt64 = Flt64.one,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    private val qVar: AbstractVariableItem<*, *> by lazy { IntVar("${name}_q") }
    private val rVar: AbstractVariableItem<*, *> by lazy { URealVar("${name}_r") }
    private val bVar: AbstractVariableItem<*, *> by lazy { BinVar("${name}_b") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(qVar, rVar, bVar)

    /**
     * Linear polynomial representing the quotient (round result): `q + b`.
     * Exposed for framework reference (e.g. in objectives).
     * When r < d/2, b=0 so result=q. When r >= d/2, b=1 so result=q+1.
     */
    val result: LinearPolynomial<T> by lazy {
        LinearPolynomial(
            listOf(
                LinearMonomial(oneOf<T>(), qVar),
                LinearMonomial(oneOf<T>(), bVar)
            ),
            zeroOf<T>()
        )
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
        return Flt64(kotlin.math.round(doubleVal / dVal)) as T
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        // Add helper variables to the model
        val varsToAdd = listOf(qVar, rVar, bVar)
        if (varsToAdd.isNotEmpty()) {
            when (val result = model.add(varsToAdd)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        val xPoly = x.asFlt64Poly()
        val dVal = d
        val halfD = dVal / Flt64(2.0)
        val mVal = Flt64(1e6)

        // Constraint: x = d * q + r  =>  x eq (d*q + r)
        val dqPoly = LinearPolynomial(listOf(LinearMonomial(dVal, qVar)), Flt64.zero)
        val rPoly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, rVar)), Flt64.zero)
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

        // r <= d (upper bound)
        val rUpper = LinearPolynomial(listOf(LinearMonomial(Flt64.one, rVar)), Flt64.zero)
        val rUpperRhs = LinearPolynomial(emptyList(), dVal)
        val rUpperConstraint = MathLinearInequality(rUpper, rUpperRhs, Comparison.LE, "${name}_r_le_d")
        when (val result = model.addConstraint(relation = rUpperConstraint, name = rUpperConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Rounding logic: b=1 when r >= d/2, b=0 when r < d/2
        // Use Big-M to link b with the rounding threshold:
        // r >= d/2 - M*(1-b)   => if b=1: r >= d/2; if b=0: r >= d/2 - M (no constraint)
        // r <= d/2 + M*b - eps  => if b=0: r <= d/2 - eps; if b=1: r <= d/2 + M (no constraint)

        // r >= d/2 - M*(1-b)  =>  r + M*(1-b) >= d/2  =>  r - M*b >= d/2 - M
        val rGeHalfLhs = LinearPolynomial(
            listOf(
                LinearMonomial(Flt64.one, rVar),
                LinearMonomial(-mVal, bVar)
            ),
            Flt64.zero
        )
        val rGeHalfRhs = LinearPolynomial(emptyList(), halfD - mVal)
        val rGeHalfConstraint = MathLinearInequality(rGeHalfLhs, rGeHalfRhs, Comparison.GE, "${name}_r_ge_half_d")
        when (val result = model.addConstraint(relation = rGeHalfConstraint, name = rGeHalfConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // r <= d/2 + M*b - eps  =>  r - M*b <= d/2 - eps
        val rLeHalfLhs = LinearPolynomial(
            listOf(
                LinearMonomial(Flt64.one, rVar),
                LinearMonomial(-mVal, bVar)
            ),
            Flt64.zero
        )
        val rLeHalfRhs = LinearPolynomial(emptyList(), halfD - Flt64(1e-6))
        val rLeHalfConstraint = MathLinearInequality(rLeHalfLhs, rLeHalfRhs, Comparison.LE, "${name}_r_le_half_d")
        when (val result = model.addConstraint(relation = rLeHalfConstraint, name = rLeHalfConstraint.name)) {
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
            name: String,
            displayName: String? = null
        ): RoundingFunction<Flt64> = RoundingFunction(
            x = x,
            d = d,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            x: LinearMonomial<Flt64>,
            d: Flt64 = Flt64.one,
            name: String,
            displayName: String? = null
        ): RoundingFunction<Flt64> = RoundingFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            d = d,
            name = name,
            displayName = displayName
        )
    }
}
