@file:Suppress("unused")

package fuookami.ospf.kotlin.core.function

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.frontend.variable.BinVar
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
 * Inequality satisfaction indicator function.
 *
 * Given a linear expression and a comparison type, returns:
 * - 1 if the inequality is satisfied
 * - 0 if the inequality is violated
 *
 * Supported comparisons: LE (<=), GE (>=), EQ (==)
 *
 * For LE/GE: uses Big-M to link a binary flag to the slack variable.
 * For EQ: uses Big-M to check if |lhs| < epsilon.
 *
 * @param lhs the left-hand side linear polynomial (representing lhs <op> rhs)
 * @param rhs the right-hand side constant
 * @param sign the comparison type
 * @param m Big-M upper bound (default 1e6)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class InequalityFunction<T : Field<T>>(
    val lhs: LinearPolynomial<T>,
    val rhs: Flt64,
    val sign: Comparison,
    val m: Flt64 = Flt64(1e6),
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    private val flagVar: AbstractVariableItem<*, *> by lazy { BinVar("${name}_flag") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(flagVar)

    /**
     * The binary indicator: 1 if satisfied, 0 if violated.
     */
    val result: LinearPolynomial<T> by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<T>(), flagVar)), zeroOf<T>())
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        val lhsValue = lhs.evaluate(values) ?: return null
        val lhsDouble = lhsValue.asFlt64().toDouble()
        val rhsDouble = rhs.toDouble()
        val satisfied = when (sign) {
            Comparison.LE, Comparison.LT -> lhsDouble <= rhsDouble
            Comparison.GE, Comparison.GT -> lhsDouble >= rhsDouble
            Comparison.EQ -> kotlin.math.abs(lhsDouble - rhsDouble) < 1e-9
            Comparison.NE -> kotlin.math.abs(lhsDouble - rhsDouble) > 1e-9
        }
        @Suppress("UNCHECKED_CAST")
        return if (satisfied) Flt64.one else Flt64.zero as T
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        // Add binary flag variable
        when (val result = model.add(flagVar)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val lhsPoly = lhs.asFlt64Poly()
        val mVal = m
        val eps = Flt64(1e-6)

        when (sign) {
            Comparison.LE, Comparison.LT -> {
                // Constraint: lhs <= rhs + M*(1-flag)
                // => lhs - M*(1-flag) <= rhs
                // => lhs + M*flag <= rhs + M
                val leqLhs = LinearPolynomial(
                    lhsPoly.monomials + listOf(LinearMonomial(mVal, flagVar)),
                    lhsPoly.constant
                )
                val leqRhs = LinearPolynomial(emptyList(), rhs + mVal)
                val leqConstraint = MathLinearInequality(leqLhs, leqRhs, Comparison.LE, "${name}_satisfied")
                when (val result = model.addConstraint(relation = leqConstraint, name = leqConstraint.name)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                // Constraint: lhs >= rhs + epsilon - M*flag
                // => lhs - M*flag >= rhs + epsilon - M
                // This ensures flag=1 => lhs >= rhs + epsilon - M (no-op when flag=1)
                //                     flag=0 => lhs >= rhs + epsilon
                val geqLhs = LinearPolynomial(
                    lhsPoly.monomials + listOf(LinearMonomial(-mVal, flagVar)),
                    lhsPoly.constant
                )
                val geqRhs = LinearPolynomial(emptyList(), rhs + eps - mVal)
                val geqConstraint = MathLinearInequality(geqLhs, geqRhs, Comparison.GE, "${name}_violated")
                when (val result = model.addConstraint(relation = geqConstraint, name = geqConstraint.name)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }

            Comparison.GE, Comparison.GT -> {
                // Constraint: lhs >= rhs - M*(1-flag)
                // => lhs + M*flag >= rhs
                val geqLhs = LinearPolynomial(
                    lhsPoly.monomials + listOf(LinearMonomial(mVal, flagVar)),
                    lhsPoly.constant
                )
                val geqRhs = LinearPolynomial(emptyList(), rhs)
                val geqConstraint = MathLinearInequality(geqLhs, geqRhs, Comparison.GE, "${name}_satisfied")
                when (val result = model.addConstraint(relation = geqConstraint, name = geqConstraint.name)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                // Constraint: lhs <= rhs - epsilon + M*flag
                val leqLhs = LinearPolynomial(
                    lhsPoly.monomials + listOf(LinearMonomial(-mVal, flagVar)),
                    lhsPoly.constant
                )
                val leqRhs = LinearPolynomial(emptyList(), rhs - eps + mVal)
                val leqConstraint = MathLinearInequality(leqLhs, leqRhs, Comparison.LE, "${name}_violated")
                when (val result = model.addConstraint(relation = leqConstraint, name = leqConstraint.name)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }

            Comparison.EQ -> {
                // For equality, flag=1 means |lhs - rhs| < epsilon
                // lhs - rhs <= M*(1-flag) + epsilon
                // lhs - rhs >= -M*(1-flag) - epsilon
                val diffPoly = LinearPolynomial(lhsPoly.monomials, lhsPoly.constant - rhs)

                // diff <= M*(1-flag) + epsilon => diff - M*(1-flag) <= epsilon
                val upperLhs = LinearPolynomial(
                    diffPoly.monomials + listOf(LinearMonomial(mVal, flagVar)),
                    diffPoly.constant
                )
                val upperRhs = LinearPolynomial(emptyList(), mVal + eps)
                val upperConstraint = MathLinearInequality(upperLhs, upperRhs, Comparison.LE, "${name}_eq_upper")
                when (val result = model.addConstraint(relation = upperConstraint, name = upperConstraint.name)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                // diff >= -M*(1-flag) - epsilon => diff + M*(1-flag) >= -epsilon
                val lowerLhs = LinearPolynomial(
                    diffPoly.monomials + listOf(LinearMonomial(-mVal, flagVar)),
                    diffPoly.constant
                )
                val lowerRhs = LinearPolynomial(emptyList(), -mVal - eps)
                val lowerConstraint = MathLinearInequality(lowerLhs, lowerRhs, Comparison.GE, "${name}_eq_lower")
                when (val result = model.addConstraint(relation = lowerConstraint, name = lowerConstraint.name)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }

            Comparison.NE -> {
                return Failed(
                    fuookami.ospf.kotlin.utils.error.Err(
                        fuookami.ospf.kotlin.utils.error.ErrorCode.ApplicationFailed,
                        "InequalityFunction: NE comparison not supported for MIP encoding"
                    )
                )
            }
        }

        return ok
    }

    companion object {
        operator fun invoke(
            lhs: LinearPolynomial<Flt64>,
            rhs: Flt64,
            sign: Comparison,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): InequalityFunction<Flt64> = InequalityFunction(
            lhs = lhs,
            rhs = rhs,
            sign = sign,
            m = m,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            lhs: LinearMonomial<Flt64>,
            rhs: Flt64,
            sign: Comparison,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): InequalityFunction<Flt64> = InequalityFunction(
            lhs = LinearPolynomial(listOf(lhs), Flt64.zero),
            rhs = rhs,
            sign = sign,
            m = m,
            name = name,
            displayName = displayName
        )
    }
}
