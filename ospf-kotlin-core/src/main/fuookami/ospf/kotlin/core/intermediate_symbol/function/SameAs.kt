@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * SameAs function symbol: returns 1 if all inequalities have the same satisfaction status
 * (all true or all false), returns 0 otherwise.
 *
 * ConstraintFlt64 pattern:
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
        val epsDouble = converter.fromValue(epsilon).toDouble()
        for (ineq in inequalities) {
            val lhsVal = ineq.lhs.evaluateWith(values) ?: return null
            val rhsVal = ineq.rhs.evaluateWith(values) ?: return null
            val diff = converter.fromValue((lhsVal - rhsVal)).toDouble()
            val satisfied = when (ineq.comparison) {
                Comparison.LE -> diff <= epsDouble
                Comparison.GE -> diff >= -epsDouble
                Comparison.EQ -> kotlin.math.abs(diff) <= epsDouble
                else -> false
            }
            flags += satisfied
        }
        val allSame = flags.all { it == flags[0] }
        val unit = inequalities.first().lhs.constant / inequalities.first().lhs.constant
        val zero = unit - unit
        return if (allSame) unit else zero
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val allConstraints = mutableListOf<LinearInequality<Flt64>>()

        // Register each inequality with its satisfaction flag using simple indicator constraints
        for (i in inequalities.indices) {
            allConstraints += simpleIndicatorConstraints(
                inequalities[i], satisfactionFlags[i], m, epsilon, epsilon, converter, "${name}_ineq_${i}")
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
            if (n == 1) {
                // Single inequality: always "same" with itself
                allConstraints += LinearInequality<Flt64>(
                    LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar)), Flt64.zero),
                    LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ, "${name}_result_single")
            } else {
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
            converter = IntoValue.Flt64,
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
            converter = IntoValue.Flt64,
            name = name,
            displayName = displayName
        )
    }
}
