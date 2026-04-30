@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.variable.VariableType
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Slack Range function symbol: `y = max(lb - x, 0) + max(x - ub, 0)`.
 *
 * Measures how far `x` deviates from the range `[lb, ub]`:
 * - If x < lb: y = lb - x (distance below lower bound)
 * - If x > ub: y = x - ub (distance above upper bound)
 * - If lb <= x <= ub: y = 0
 *
 * Decomposition:
 * - Create helper variables: `neg >= 0`, `pos >= 0`
 * - ConstraintF64: `lb <= x + neg - pos <= ub` (when `constraint = true`)
 * - Output: `y = pos + neg`
 *
 * @param x the input linear polynomial
 * @param lb lower bound (default 0)
 * @param ub upper bound (default 0)
 * @param constraint if true, register bounds constraints (default true); set false when model already enforces bounds
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class SlackRangeFunction<T : Field<T>>(
    val x: LinearPolynomial<T>,
    val lb: Flt64 = Flt64.zero,
    val ub: Flt64 = Flt64.zero,
    val constraint: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    private val negVar: AbstractVariableItem<*, *> by lazy { URealVar("${name}_neg") }
    private val posVar: AbstractVariableItem<*, *> by lazy { URealVar("${name}_pos") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(negVar, posVar)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    /**
     * The slack amount: `y = pos + neg`.
     * When x is within [lb, ub], both pos and neg are 0.
     */
    val result: LinearPolynomial<T> by lazy {
        LinearPolynomial(
            listOf(
                LinearMonomial(oneOf<T>(), posVar),
                LinearMonomial(oneOf<T>(), negVar)
            ),
            zeroOf<T>()
        )
    }

    /**
     * Linear polynomial representing the positive part: `pos` (excess above ub).
     */
    val pos: LinearPolynomial<T> by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<T>(), posVar)), zeroOf<T>())
    }

    /**
     * Linear polynomial representing the negative part: `neg` (deficit below lb).
     */
    val neg: LinearPolynomial<T> by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<T>(), negVar)), zeroOf<T>())
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        val xValue = x.evaluate(values) ?: return null
        val xDouble = xValue.asFlt64().toDouble()
        val lbDouble = lb.toDouble()
        val ubDouble = ub.toDouble()
        @Suppress("UNCHECKED_CAST")
        return if (xDouble < lbDouble) {
            Flt64(lbDouble - xDouble) as T
        } else if (xDouble > ubDouble) {
            Flt64(xDouble - ubDouble) as T
        } else {
            Flt64.zero as T
        }
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        // Add helper variables
        when (val result = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Skip constraint registration when constraint=false
        // (useful when model already enforces bounds or in fixed-value scenarios)
        if (!constraint) {
            return ok
        }

        val xPoly = x.asFlt64Poly()

        // ConstraintF64: x + neg - pos <= ub
        // => x + neg - pos <= ub
        val upperLhs = LinearPolynomial(
            xPoly.monomials + listOf(
                LinearMonomial(Flt64.one, negVar),
                LinearMonomial(-Flt64.one, posVar)
            ),
            xPoly.constant
        )
        val upperRhs = LinearPolynomial(emptyList(), ub)
        val upperConstraint = LinearInequality<Flt64>(upperLhs, upperRhs, Comparison.LE, "${name}_ub")
        when (val result = model.addConstraint(relation = upperConstraint, name = upperConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // ConstraintF64: x + neg - pos >= lb
        // => x + neg - pos >= lb
        val lowerLhs = LinearPolynomial(
            xPoly.monomials + listOf(
                LinearMonomial(Flt64.one, negVar),
                LinearMonomial(-Flt64.one, posVar)
            ),
            xPoly.constant
        )
        val lowerRhs = LinearPolynomial(emptyList(), lb)
        val lowerConstraint = LinearInequality<Flt64>(lowerLhs, lowerRhs, Comparison.GE, "${name}_lb")
        when (val result = model.addConstraint(relation = lowerConstraint, name = lowerConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return ok
    }

    companion object {
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            lb: Flt64 = Flt64.zero,
            ub: Flt64 = Flt64.zero,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): SlackRangeFunction<Flt64> = SlackRangeFunction(
            x = x,
            lb = lb,
            ub = ub,
            constraint = constraint,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            x: LinearMonomial<Flt64>,
            lb: Flt64 = Flt64.zero,
            ub: Flt64 = Flt64.zero,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): SlackRangeFunction<Flt64> = SlackRangeFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            lb = lb,
            ub = ub,
            constraint = constraint,
            name = name,
            displayName = displayName
        )

        @JvmStatic
        @JvmName("legacyWithVariableTypeAndPolynomialBounds")
        operator fun invoke(
            type: VariableType<*>,
            x: LinearPolynomial<Flt64>,
            lb: LinearPolynomial<Flt64>,
            ub: LinearPolynomial<Flt64>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter {
            val zero = Flt64.zero
            val shiftedX = subtract(x, lb)
            val shiftedUb = subtract(ub, lb)
            val mapped = SlackRangeFunction(
                x = shiftedX,
                lb = zero,
                ub = shiftedUb.constant,
                constraint = true,
                name = name,
                displayName = displayName
            )
            return LinearFunctionSymbolAdapter(mapped)
        }

        private fun subtract(
            lhs: LinearPolynomial<Flt64>,
            rhs: LinearPolynomial<Flt64>
        ): LinearPolynomial<Flt64> {
            val negatedRhs = rhs.monomials.map { monomial ->
                LinearMonomial(-monomial.coefficient, monomial.symbol)
            }
            return LinearPolynomial(
                lhs.monomials + negatedRhs,
                lhs.constant - rhs.constant
            )
        }
    }
}
