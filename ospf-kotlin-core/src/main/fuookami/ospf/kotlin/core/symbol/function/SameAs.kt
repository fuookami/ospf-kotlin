/**
 * 相同满足函数符号 / Same-as function symbol
 *
 * 提供 [SameAsFunction]，当所有不等式满足状态一致时返回 1。
 *
 * Provides [SameAsFunction] that returns 1 when all inequalities have the same satisfaction status.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar

/**
 * SameAs function symbol: returns 1 if all inequalities have the same satisfaction status
 * (all true or all false), returns 0 otherwise.
 *
 * Constraint pattern:
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
class SameAsFunction<V>(
    val inequalities: List<LinearInequality<V>>,
    val constraint: Boolean = true,
    val epsilon: V,
    val m: V,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    init {
        require(inequalities.isNotEmpty()) { "SameAsFunction requires at least one inequality" }
    }

    private val n = inequalities.size

    // Binary flag per inequality: u[i] = 1 if inequality i is satisfied
    val satisfactionFlags: List<AbstractVariableItem<*, *>> =
        (0 until n).map { BinVar("${name}_u${it}") }

    // Output binary: y = 1 if all same, 0 otherwise
    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_same")

    // Diff variables for measurement mode (XOR between adjacent satisfaction flags)
    private val diffVars: List<AbstractVariableItem<*, *>> =
        if (!constraint && n > 1) (1 until n).map { BinVar("${name}_diff${it}") } else emptyList()

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + satisfactionFlags + diffVars

    override fun evaluate(values: Map<Symbol, V>): V? {
        val flags = mutableListOf<Boolean>()
        for (ineq in inequalities) {
            val lhsVal = ineq.lhs.evaluateWith(values) ?: return null
            val rhsVal = ineq.rhs.evaluateWith(values) ?: return null
            val diff = lhsVal - rhsVal
            val satisfied = when (ineq.comparison) {
                Comparison.LE -> !(diff gr epsilon)
                Comparison.LT -> diff ls epsilon
                Comparison.GE -> !(diff ls -epsilon)
                Comparison.GT -> diff gr -epsilon
                Comparison.EQ -> diff.abs() ls epsilon || diff.abs() eq epsilon
                Comparison.NE -> diff.abs() gr epsilon
            }
            flags += satisfied
        }
        val allSame = flags.all { it == flags[0] }
        return if (allSame) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val two = converter.intoValue(Flt64(2.0))
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Register each inequality with its satisfaction flag using simple indicator constraints
        for (i in inequalities.indices) {
            allConstraints += simpleIndicatorConstraints(
                inequalities[i], satisfactionFlags[i], m, epsilon, epsilon, "${name}_ineq_${i}")
        }

        // Link constraints: enforce all satisfaction flags are equal
        if (constraint) {
            // For constraint mode: force u[0] == u[1] == ... == u[n-1]
            // This is done by: u[0] - u[i] == 0 for i=1..n-1
            for (i in 1 until n) {
                val eqLhs = LinearPolynomial(
                    listOf(
                        LinearMonomial(one, satisfactionFlags[0]),
                        LinearMonomial(-one, satisfactionFlags[i])
                    ),
                    zero
                )
                val eqRhs = LinearPolynomial(emptyList(), zero)
                allConstraints += LinearInequality(
                    eqLhs, eqRhs, Comparison.EQ, "${name}_equal_${i}")
            }
            // result = u[0] (since all are equal)
            val resultLink = LinearPolynomial(
                listOf(
                    LinearMonomial(one, resultVar),
                    LinearMonomial(-one, satisfactionFlags[0])
                ),
                zero
            )
            allConstraints += LinearInequality(
                resultLink, LinearPolynomial(emptyList(), zero),
                Comparison.EQ, "${name}_result_link")
        } else {
            // Measurement mode: y = 1 iff all u[i] are equal
            if (n == 1) {
                // Single inequality: always "same" with itself
                allConstraints += LinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(one, resultVar)), zero),
                    LinearPolynomial(emptyList(), one), Comparison.EQ, "${name}_result_single")
            } else {
                for (i in 1 until n) {
                    val u0 = satisfactionFlags[0]
                    val ui = satisfactionFlags[i]
                    val diffVar = diffVars[i - 1]

                    // diff >= u[i] - u[0]  =>  diff - u[i] + u[0] >= 0
                    allConstraints += LinearInequality(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(one, diffVar),
                                LinearMonomial(-one, ui),
                                LinearMonomial(one, u0)
                            ),
                            zero
                        ),
                        LinearPolynomial(emptyList(), zero),
                        Comparison.GE, "${name}_diff_ge_${i}")

                    // diff >= u[0] - u[i]  =>  diff - u[0] + u[i] >= 0
                    allConstraints += LinearInequality(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(one, diffVar),
                                LinearMonomial(one, ui),
                                LinearMonomial(-one, u0)
                            ),
                            zero
                        ),
                        LinearPolynomial(emptyList(), zero),
                        Comparison.GE, "${name}_diff_le_${i}")

                    // diff <= u[i] + u[0]  =>  diff - u[i] - u[0] <= 0
                    allConstraints += LinearInequality(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(one, diffVar),
                                LinearMonomial(-one, ui),
                                LinearMonomial(-one, u0)
                            ),
                            zero
                        ),
                        LinearPolynomial(emptyList(), zero),
                        Comparison.LE, "${name}_diff_sum_ub_${i}")

                    // diff <= 2 - u[i] - u[0]  =>  diff + u[i] + u[0] <= 2
                    allConstraints += LinearInequality(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(one, diffVar),
                                LinearMonomial(one, ui),
                                LinearMonomial(one, u0)
                            ),
                            zero
                        ),
                        LinearPolynomial(emptyList(), two),
                        Comparison.LE, "${name}_diff_sum_lb_${i}")
                }

                // y = 1 - sum(diff_i)  =>  y + sum(diff_i) = 1
                val yPlusSumMonos = listOf(LinearMonomial(one, resultVar)) +
                    diffVars.map { LinearMonomial(one, it) }
                allConstraints += LinearInequality(
                    LinearPolynomial(yPlusSumMonos, zero),
                    LinearPolynomial(emptyList(), one),
                    Comparison.EQ, "${name}_result_sum")
            }
        }

        return addConstraints(model, allConstraints) ?: ok
    }
    companion object {
        operator fun <V> invoke(
            inequalities: List<LinearInequality<V>>,
            constraint: Boolean = true,
            epsilon: V,
            m: V,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): SameAsFunction<V> where V : RealNumber<V>, V : NumberField<V> = SameAsFunction(
            inequalities = inequalities,
            constraint = constraint,
            epsilon = epsilon,
            m = m,
            converter = converter,
            name = name,
            displayName = displayName
        )
    }
}
