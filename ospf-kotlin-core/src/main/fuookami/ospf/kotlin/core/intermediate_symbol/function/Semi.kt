@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
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
 * Semi-continuous function symbol: `y = |x|` if active, or `y = 0` (using Big-M).
 *
 * Decomposition:
 * - Create helper variable `y` (URealVar for output) and `u` (BinVar for activation)
 * - ConstraintF64: `y >= x`        (lower bound: y covers x)
 * - ConstraintF64: `y <= x + M*u`  (upper bound via BigM)
 * - ConstraintF64: `y <= M*(1-u)`  (activation: if u=1, y must be 0; if u=0, y can be nonzero)
 *
 * When `u=0`: y >= x, y <= x, y <= M  => y = x (active, y takes the value of x)
 * When `u=1`: y >= x, y <= x+M, y <= 0 => y = 0 (inactive, y is forced to 0)
 *
 * @param x the input linear polynomial
 * @param m Big-M upper bound constant (default 1e6)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class SemiFunction<T : Field<T>>(
    val x: LinearPolynomial<T>,
    val m: Flt64 = Flt64(1e6),
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    private val yVar: AbstractVariableItem<*, *> by lazy { URealVar("${name}_y") }
    private val uVar: AbstractVariableItem<*, *> by lazy { BinVar("${name}_u") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(yVar, uVar)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    /**
     * Linear polynomial representing the output: `y`.
     * Exposed for framework reference (e.g. in objectives).
     */
    val y: LinearPolynomial<T> by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<T>(), yVar)), zeroOf<T>())
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        val xValue = x.evaluate(values) ?: return null
        val doubleVal = xValue.asFlt64().toDouble()
        // Semi-continuous: returns |x| if nonzero, else 0
        @Suppress("UNCHECKED_CAST")
        return Flt64(kotlin.math.abs(doubleVal)) as T
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        // Add helper variables to the model
        when (val result = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val xPoly = x.asFlt64Poly()
        val mVal = m

        // y polynomial
        val yPoly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, yVar)), Flt64.zero)

        // ConstraintF64 1: y >= x  =>  y - x >= 0  =>  y geq x
        val geqConstraint = LinearInequality<F64>(yPoly, xPoly, Comparison.GE, "${name}_geq_x")
        when (val result = model.addConstraint(relation = geqConstraint, name = geqConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // ConstraintF64 2: y <= x + M*u  =>  y - x - M*u <= 0
        val upperLhs = LinearPolynomial(
            yPoly.monomials + xPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(-mVal, uVar),
            yPoly.constant - xPoly.constant
        )
        val upperRhs = LinearPolynomial(emptyList(), Flt64.zero)
        val upperConstraint = LinearInequality<F64>(upperLhs, upperRhs, Comparison.LE, "${name}_upper_bound")
        when (val result = model.addConstraint(relation = upperConstraint, name = upperConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // ConstraintF64 3: y <= M*(1-u)  =>  y + M*u <= M
        val activationLhs = LinearPolynomial(
            listOf(
                LinearMonomial(Flt64.one, yVar),
                LinearMonomial(mVal, uVar)
            ),
            Flt64.zero
        )
        val activationRhs = LinearPolynomial(emptyList(), mVal)
        val activationConstraint = LinearInequality<F64>(activationLhs, activationRhs, Comparison.LE, "${name}_activation")
        when (val result = model.addConstraint(relation = activationConstraint, name = activationConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return ok
    }

    companion object {
        operator fun invoke(
            x: LinearPolynomial<F64>,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): SemiFunction<F64> = SemiFunction(
            x = x,
            m = m,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            x: LinearMonomial<F64>,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): SemiFunction<F64> = SemiFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            m = m,
            name = name,
            displayName = displayName
        )
    }
}
