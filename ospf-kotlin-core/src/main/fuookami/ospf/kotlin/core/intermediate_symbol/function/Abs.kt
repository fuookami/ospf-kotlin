@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.eq
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
 * Absolute value function symbol: `y = |x|` where `x` is a LinearPolynomial.
 *
 * Decomposition:
 * - Create two non-negative variables: `pos >= 0`, `neg >= 0`
 * - ConstraintF64: `x = pos - neg`  (i.e. `x + neg = pos`)
 * - Output:     `y = pos + neg`  (which equals `|x|`)
 *
 * When `extract = true`, additional binary variable and Big-M constraints are added
 * to enforce that at most one of `pos`/`neg` is nonzero in a minimization objective:
 * - `pos + neg <= M`
 * - `pos <= M * b`
 * - `neg <= M * (1 - b)`
 *
 * @param x the input linear polynomial
 * @param extract whether to use binary variable for optimization (default true)
 * @param m Big-M upper bound for extract mode (default 1e6)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class AbsFunction<T : Field<T>>(
    val x: LinearPolynomial<T>,
    val extract: Boolean = true,
    val m: Flt64 = Flt64(1e6),
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    private val posVar: AbstractVariableItem<*, *> by lazy { URealVar("${name}_pos") }
    private val negVar: AbstractVariableItem<*, *> by lazy { URealVar("${name}_neg") }
    private val binVar: AbstractVariableItem<*, *>? by lazy {
        if (extract) BinVar("${name}_bin") else null
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOfNotNull(posVar, negVar, binVar)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    /**
     * Linear polynomial representing the positive part: `pos`.
     * Exposed for framework reference (e.g. in objectives).
     */
    val pos: LinearPolynomial<T>? by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<T>(), posVar)), zeroOf<T>())
    }

    /**
     * Linear polynomial representing the negative part: `neg`.
     * Exposed for framework reference (e.g. in objectives).
     */
    val neg: LinearPolynomial<T>? by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<T>(), negVar)), zeroOf<T>())
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        val xValue = x.evaluate(values) ?: return null
        val doubleVal = xValue.asFlt64().toDouble()
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

        // ConstraintF64: x + neg = pos   ->   x + neg - pos = 0
        val posPoly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, posVar)), Flt64.zero)
        val negPoly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, negVar)), Flt64.zero)

        // x + neg = pos  =>  x + neg - pos = 0  =>  x + neg eq pos
        val eqLhs = LinearPolynomial(xPoly.monomials + LinearMonomial(Flt64.one, negVar), xPoly.constant)
        val eqRhs = posPoly
        val eqConstraint = MathLinearInequality(eqLhs, eqRhs, Comparison.EQ, "${name}_decompose")
        when (val result = model.addConstraint(relation = eqConstraint, name = eqConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // If extract mode: add Big-M constraints with binary variable
        if (extract) {
            val bin = binVar!!
            val mVal = m

            // pos + neg <= M
            val sumPoly = LinearPolynomial(
                listOf(
                    LinearMonomial(Flt64.one, posVar),
                    LinearMonomial(Flt64.one, negVar)
                ),
                Flt64.zero
            )
            val rhsConst = LinearPolynomial(emptyList(), mVal)
            val leqConstraint = MathLinearInequality(sumPoly, rhsConst, Comparison.LE, "${name}_bigM_sum")
            when (val result = model.addConstraint(relation = leqConstraint, name = leqConstraint.name)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            // pos <= M * b   ->   pos - M*b <= 0
            val posLeqLhs = LinearPolynomial(
                listOf(
                    LinearMonomial(Flt64.one, posVar),
                    LinearMonomial(-mVal, bin)
                ),
                Flt64.zero
            )
            val posLeqRhs = LinearPolynomial(emptyList(), Flt64.zero)
            val posLeqConstraint = MathLinearInequality(posLeqLhs, posLeqRhs, Comparison.LE, "${name}_pos_bound")
            when (val result = model.addConstraint(relation = posLeqConstraint, name = posLeqConstraint.name)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            // neg <= M * (1 - b)   ->   neg + M*b <= M
            val negLeqLhs = LinearPolynomial(
                listOf(
                    LinearMonomial(Flt64.one, negVar),
                    LinearMonomial(mVal, bin)
                ),
                Flt64.zero
            )
            val negLeqRhs = LinearPolynomial(emptyList(), mVal)
            val negLeqConstraint = MathLinearInequality(negLeqLhs, negLeqRhs, Comparison.LE, "${name}_neg_bound")
            when (val result = model.addConstraint(relation = negLeqConstraint, name = negLeqConstraint.name)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        return ok
    }

    companion object {
        operator fun invoke(
            x: LinearPolynomial<F64>,
            extract: Boolean = true,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): AbsFunction<F64> = AbsFunction(
            x = x,
            extract = extract,
            m = m,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            x: LinearMonomial<F64>,
            extract: Boolean = true,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): AbsFunction<F64> = AbsFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            extract = extract,
            m = m,
            name = name,
            displayName = displayName
        )
    }
}
