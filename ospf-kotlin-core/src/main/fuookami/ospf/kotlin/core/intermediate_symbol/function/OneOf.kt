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
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Represents a single branch in a OneOfFunction.
 *
 * @param polynomial the linear polynomial contributed by this branch when active
 * @param condition optional inequality that must be satisfied for this branch to be active;
 *   if null, this branch is always available (subject to the at-most-one constraint)
 * @param name human-readable name for this branch
 */
data class OneOfBranch<T : Field<T>>(
    val polynomial: LinearPolynomial<T>,
    val condition: MathLinearInequality? = null,
    val name: String
)

/**
 * OneOf function symbol: selects at most one branch from a list of (condition, polynomial) pairs.
 *
 * When a branch is active, its polynomial value contributes to the output.
 * When a branch is inactive, its contribution is zero.
 *
 * ConstraintF64 pattern:
 * - Each branch has a binary activation variable `u[i]`
 * - `sum(u[i]) <= 1` (at most one branch active)
 * - Each branch's polynomial is masked via MaskingFunction: z[i] = polynomial[i] * u[i]
 * - Output `y = sum(z[i])`
 *
 * @param branches list of branches to select from
 * @param m Big-M constant for masking constraints (default 1e6)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class OneOfFunction<T : Field<T>>(
    val branches: List<OneOfBranch<T>>,
    val m: Flt64 = Flt64(1e6),
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    init {
        require(branches.isNotEmpty()) { "OneOfFunction requires at least one branch" }
    }

    // Selection binary per branch: u[i] = 1 if branch i is active
    val selectionVars: List<AbstractVariableItem<*, *>> =
        (0 until branches.size).map { BinVar("${name}_u${it}") }

    // Masked output variable per branch: z[i] = polynomial[i] when u[i]=1, 0 otherwise
    val maskedVars: List<AbstractVariableItem<*, *>> =
        (0 until branches.size).map { URealVar("${name}_z${it}") }

    // Output variable: y = sum(z[i])
    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_result")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectionVars + maskedVars

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    /** Linear polynomial representing the output y. */
    val y: LinearPolynomial<T> by lazy {
        LinearPolynomial(listOf(LinearMonomial(oneOf<T>(), resultVar)), zeroOf<T>())
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        // Find which branch is active (selection var = 1)
        for (i in branches.indices) {
            val uVal = values[selectionVars[i]]?.asFlt64()?.toDouble() ?: return null
            if (uVal > 0.5) {
                // This branch is active
                return branches[i].polynomial.evaluate(values)
            }
        }
        // No branch active
        return zeroOf<T>()
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val r = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val allConstraints = mutableListOf<MathLinearInequality>()
        val mVal = m

        // For each branch: mask the polynomial using Big-M constraints
        // z[i] <= M * u[i]
        // z[i] >= -M * u[i]
        // z[i] - poly[i] <= M * (1 - u[i])
        // z[i] - poly[i] >= -M * (1 - u[i])
        for (i in branches.indices) {
            val zVar = maskedVars[i]
            val uVar = selectionVars[i]
            val branchPoly = branches[i].polynomial.asFlt64Poly()
            val branchName = branches[i].name

            val zMon = LinearMonomial(Flt64.one, zVar)

            // z[i] <= M * u[i]  =>  z[i] - M*u[i] <= 0
            allConstraints += MathLinearInequality(
                LinearPolynomial(
                    listOf(zMon, LinearMonomial(-mVal, uVar)),
                    Flt64.zero
                ),
                LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.LE, "${name}_mask_ub_${i}")

            // z[i] >= -M * u[i]  =>  z[i] + M*u[i] >= 0
            allConstraints += MathLinearInequality(
                LinearPolynomial(
                    listOf(zMon, LinearMonomial(mVal, uVar)),
                    Flt64.zero
                ),
                LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.GE, "${name}_mask_lb_${i}")

            // z[i] - poly[i] <= M * (1 - u[i])
            // => z[i] - poly[i] + M*u[i] <= M
            val diffUpMonos = listOf(zMon) +
                branchPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(mVal, uVar)
            val diffUpConst = branchPoly.constant.unaryMinus()
            allConstraints += MathLinearInequality(
                LinearPolynomial(diffUpMonos, diffUpConst),
                LinearPolynomial(emptyList(), mVal),
                Comparison.LE, "${name}_eq_ub_${i}")

            // z[i] - poly[i] >= -M * (1 - u[i])
            // => z[i] - poly[i] - M*u[i] >= -M
            val diffLbMonos = listOf(zMon) +
                branchPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(-mVal, uVar)
            val diffLbConst = branchPoly.constant.unaryMinus()
            allConstraints += MathLinearInequality(
                LinearPolynomial(diffLbMonos, diffLbConst),
                LinearPolynomial(emptyList(), -mVal),
                Comparison.GE, "${name}_eq_lb_${i}")
        }

        // sum(u[i]) <= 1  (at most one branch active)
        if (selectionVars.isNotEmpty()) {
            val sumMonos = selectionVars.map { LinearMonomial(Flt64.one, it) }
            allConstraints += MathLinearInequality(
                LinearPolynomial(sumMonos, Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.one),
                Comparison.LE, "${name}_at_most_one")
        }

        // y = sum(z[i])  =>  y - sum(z[i]) = 0
        val resultMonos = listOf(LinearMonomial(Flt64.one, resultVar)) +
            maskedVars.map { LinearMonomial(-Flt64.one, it) }
        allConstraints += MathLinearInequality(
            LinearPolynomial(resultMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.EQ, "${name}_result_eq")

        return addConstraints(model, allConstraints) ?: ok
    }

    companion object {
        @JvmName("fromBranches")
        operator fun invoke(
            branches: List<OneOfBranch<F64>>,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): OneOfFunction<F64> = OneOfFunction(
            branches = branches,
            m = m,
            name = name,
            displayName = displayName
        )

        /**
         * Convenience factory for creating a OneOfFunction from polynomials only
         * (no conditions, at-most-one selection).
         */
        @JvmName("fromPolynomials")
        operator fun invoke(
            polynomials: List<LinearPolynomial<F64>>,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): OneOfFunction<F64> {
            val branchList = polynomials.mapIndexed { i, poly ->
                OneOfBranch(poly, null, "${name}_branch_${i}")
            }
            return OneOfFunction(
                branches = branchList,
                m = m,
                name = name,
                displayName = displayName
            )
        }
    }
}
