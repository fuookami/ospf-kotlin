@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
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
 * SameAs function symbol: returns 1 if all inequalities have the same satisfaction status
 * (all true or all false), returns 0 otherwise.
 *
 * ConstraintF64 pattern:
 * - Each input inequality gets a binary flag `u[i]` (1 if satisfied, 0 if not)
 * - BigM constraints link each flag to its inequality
 * - In constraint mode: all flags are forced equal (all satisfied or all unsatisfied)
 * - In measurement mode: y measures whether all are the same
 *
 * @param inequalities list of linear inequalities to compare
 * @param constraint if true, force all inequalities to have same satisfaction; if false, measure only
 * @param epsilon minimum gap for relaxed inequality (default 1e-6)
 * @param m Big-M constant for indicator constraints (default 1e6)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class SameAsFunction<T : Field<T>>(
    val inequalities: List<LinearInequality<Flt64>>,
    val constraint: Boolean = true,
    val epsilon: Flt64 = Flt64(1e-6),
    val m: Flt64 = Flt64(1e6),
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    init {
        require(inequalities.isNotEmpty()) { "SameAsFunction requires at least one inequality" }
    }

    private val n = inequalities.size

    // Binary flag per inequality: u[i] = 1 if inequality i is satisfied
    val satisfactionFlags: List<AbstractVariableItem<*, *>> =
        (0 until n).map { BinVar("${name}_u${it}") }

    // Output binary: y = 1 if all same, 0 otherwise
    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_same")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + satisfactionFlags

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.variable.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    @Suppress("UNCHECKED_CAST")
    override fun evaluate(values: Map<Symbol, T>): T? {
        val flt64Values = values.mapValues { it.value.asFlt64() }
        val flags = mutableListOf<Boolean>()
        for (ineq in inequalities) {
            val lhsVal = ineq.lhs.evaluate(flt64Values) ?: return null
            val rhsVal = ineq.rhs.evaluate(flt64Values) ?: return null
            val diff = (lhsVal - rhsVal).toDouble()
            val satisfied = when (ineq.comparison) {
                Comparison.LE -> diff <= epsilon.toDouble()
                Comparison.GE -> diff >= -epsilon.toDouble()
                Comparison.EQ -> kotlin.math.abs(diff) <= epsilon.toDouble()
                else -> false
            }
            flags += satisfied
        }
        val allSame = flags.all { it == flags[0] }
        return if (allSame) oneOf<T>() else zeroOf<T>()
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val r = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val allConstraints = mutableListOf<LinearInequality<Flt64>>()

        // Register each inequality with its satisfaction flag using simple indicator constraints
        for (i in inequalities.indices) {
            allConstraints += simpleIndicatorConstraints(
                inequalities[i], satisfactionFlags[i], m, "${name}_ineq_${i}")
        }

        // Link constraints: enforce all satisfaction flags are equal
        if (constraint) {
            // For constraint mode: force u[0] == u[1] == ... == u[n-1]
            // This is done by: u[0] - u[i] == 0 for i=1..n-1
            for (i in 1 until n) {
                val eqLhs = LinearPolynomial(
                    listOf(
                        LinearMonomial(Flt64.one, satisfactionFlags[0]),
                        LinearMonomial(-Flt64.one, satisfactionFlags[i])
                    ),
                    Flt64.zero
                )
                val eqRhs = LinearPolynomial(emptyList(), Flt64.zero)
                allConstraints += LinearInequality<Flt64>(
                    eqLhs, eqRhs, Comparison.EQ, "${name}_equal_${i}")
            }
            // result = u[0] (since all are equal)
            val resultLink = LinearPolynomial(
                listOf(
                    LinearMonomial(Flt64.one, resultVar),
                    LinearMonomial(-Flt64.one, satisfactionFlags[0])
                ),
                Flt64.zero
            )
            allConstraints += LinearInequality<Flt64>(
                resultLink, LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.EQ, "${name}_result_link")
        } else {
            // Measurement mode: y = 1 iff all u[i] are equal
            // diff_i = |u[i] - u[0]| for i=1..n-1
            // y = 1 - sum(diff_i)
            //
            // Linearization: diff_i >= u[i] - u[0], diff_i >= u[0] - u[i]
            // diff_i <= u[i] + u[0], diff_i <= 2 - u[i] - u[0]
            // y = 1 - sum(diff_i), and since y is binary, this forces y=1 only when all diff_i=0

            if (n == 1) {
                // Single inequality: always "same" with itself
                allConstraints += LinearInequality<Flt64>(
                    LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar)), Flt64.zero),
                    LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ, "${name}_result_single")
            } else {
                // For each i=1..n-1: constrain u[i] == u[0] via a diff variable
                // Since u[i] and u[0] are binary, diff_i = u[i] XOR u[0]
                // diff_i >= u[i] - u[0], diff_i >= u[0] - u[i]
                // diff_i <= u[i] + u[0], diff_i <= 2 - u[i] - u[0]
                // Then y = 1 - sum(diff_i)
                // y is binary: y=1 only when sum=0 (all equal), y=0 otherwise

                val diffVars = (1 until n).map { BinVar("${name}_diff${it}") }
                // Register diff variables
                when (val r2 = model.add(diffVars)) {
                    is Ok -> {}
                    is Failed -> return Failed(r2.error)
                    is Fatal -> return Fatal(r2.errors)
                }

                for (i in 1 until n) {
                    val u0 = satisfactionFlags[0]
                    val ui = satisfactionFlags[i]
                    val diffVar = diffVars[i - 1]

                    // diff >= u[i] - u[0]  =>  diff - u[i] + u[0] >= 0
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(Flt64.one, diffVar),
                                LinearMonomial(-Flt64.one, ui),
                                LinearMonomial(Flt64.one, u0)
                            ),
                            Flt64.zero
                        ),
                        LinearPolynomial(emptyList(), Flt64.zero),
                        Comparison.GE, "${name}_diff_ge_${i}")

                    // diff >= u[0] - u[i]  =>  diff - u[0] + u[i] >= 0
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(Flt64.one, diffVar),
                                LinearMonomial(Flt64.one, ui),
                                LinearMonomial(-Flt64.one, u0)
                            ),
                            Flt64.zero
                        ),
                        LinearPolynomial(emptyList(), Flt64.zero),
                        Comparison.GE, "${name}_diff_le_${i}")

                    // diff <= u[i] + u[0]  =>  diff - u[i] - u[0] <= 0
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(Flt64.one, diffVar),
                                LinearMonomial(-Flt64.one, ui),
                                LinearMonomial(-Flt64.one, u0)
                            ),
                            Flt64.zero
                        ),
                        LinearPolynomial(emptyList(), Flt64.zero),
                        Comparison.LE, "${name}_diff_sum_ub_${i}")

                    // diff <= 2 - u[i] - u[0]  =>  diff + u[i] + u[0] <= 2
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(Flt64.one, diffVar),
                                LinearMonomial(Flt64.one, ui),
                                LinearMonomial(Flt64.one, u0)
                            ),
                            Flt64.zero
                        ),
                        LinearPolynomial(emptyList(), Flt64(2.0)),
                        Comparison.LE, "${name}_diff_sum_lb_${i}")
                }

                // y = 1 - sum(diff_i)  =>  y + sum(diff_i) = 1
                val yPlusSumMonos = listOf(LinearMonomial(Flt64.one, resultVar)) +
                    diffVars.map { LinearMonomial(Flt64.one, it) }
                allConstraints += LinearInequality<Flt64>(
                    LinearPolynomial(yPlusSumMonos, Flt64.zero),
                    LinearPolynomial(emptyList(), Flt64.one),
                    Comparison.EQ, "${name}_result_sum")
            }
        }

        return addConstraints(model, allConstraints) ?: ok
    }

    companion object {
        operator fun invoke(
            inequalities: List<LinearInequality<Flt64>>,
            constraint: Boolean = true,
            epsilon: Flt64 = Flt64(1e-6),
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): SameAsFunction<Flt64> = SameAsFunction(
            inequalities = inequalities,
            constraint = constraint,
            epsilon = epsilon,
            m = m,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            inequalities: List<LinearInequality<Flt64>>,
            name: String,
            displayName: String? = null
        ): SameAsFunction<Flt64> = SameAsFunction(
            inequalities = inequalities,
            constraint = true,
            epsilon = Flt64(1e-6),
            m = Flt64(1e6),
            name = name,
            displayName = displayName
        )
    }
}
